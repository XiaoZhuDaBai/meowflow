<template>
  <div class="tnl">
    <div v-if="parsed.error" class="warn-block">
      <i class="fa-solid fa-triangle-exclamation"></i>
      <span>workflowJson 解析失败：{{ parsed.error }}</span>
    </div>

    <div v-else-if="!parsed.nodes.length" class="empty-block">
      <i class="fa-solid fa-circle-info"></i>
      <span>该模板暂未提供 workflowJson</span>
    </div>

    <div v-else class="tnl-grid">
      <!-- 左侧：流程总览统计 / 入口 -->
      <aside class="tnl-side">
        <div class="summary-card">
          <div class="sc-title">
            <i class="fa-solid fa-chart-pie"></i>
            <span>工作流画像</span>
          </div>
          <ul class="sc-list">
            <li v-for="s in categorySummary" :key="s.category">
              <span class="dot" :style="{ background: s.color }"></span>
              <span class="label">{{ s.label }}</span>
              <span class="count">{{ s.count }}</span>
            </li>
          </ul>
        </div>

        <div class="quick-tips">
          <div class="qt-title"><i class="fa-solid fa-lightbulb"></i> 什么是这些节点？</div>
          <p>下方每个卡片对应一个节点。点击展开可看到完整参数（已被脱敏处理过的密钥字段除外）。</p>
        </div>
      </aside>

      <!-- 右侧：节点卡片列表 -->
      <ol class="tnl-list">
        <li
          v-for="(item, idx) in enriched"
          :key="item.node.id || idx"
          class="tnl-item"
          :class="{ expanded: expandedId === item.node.id, [`cat-${item.meta.category}`]: true }"
        >
          <!-- 折叠态的标题栏 -->
          <button
            class="tnl-head"
            :aria-expanded="expandedId === item.node.id"
            @click="toggle(item.node.id)"
          >
            <!-- 序号 -->
            <span class="step">{{ idx + 1 }}</span>
            <!-- 图标块 -->
            <span class="icon" :style="{ background: item.meta.color }">
              <i :class="['fa-solid', item.meta.icon]" />
            </span>
            <!-- 主体信息 -->
            <span class="main">
              <span class="name-row">
                <span class="name">{{ item.node.name }}</span>
                <span class="cat-pill" :style="{ background: item.meta.categorySoft, color: item.meta.categoryColor }">
                  {{ item.meta.categoryLabel }}
                </span>
              </span>
              <span class="type-row">
                <code class="type-tag">{{ item.node.type }}</code>
                <span class="param-count">
                  <i class="fa-solid fa-sliders"></i>
                  {{ item.configCount }} 项配置
                </span>
                <span v-if="item.meta.inputs.length" class="io-hint">
                  <i class="fa-solid fa-arrow-right-arrow-left"></i>
                  {{ item.meta.inputs.length }} 入 / {{ item.meta.outputs.length }} 出
                </span>
              </span>
            </span>
            <!-- 关键参数 chip -->
            <span class="key-params" v-if="item.keyParams.length">
              <span
                v-for="p in item.keyParams"
                :key="p.key"
                class="kp"
                :title="p.display"
              >
                <span class="kp-label">{{ p.label }}</span>
                <span class="kp-value">{{ p.display }}</span>
              </span>
            </span>
            <i class="fa-solid fa-chevron-down tnl-toggle" />
          </button>

          <!-- 展开内容 -->
          <div v-if="expandedId === item.node.id" class="tnl-body">
            <!-- 描述 -->
            <p v-if="item.node.description || item.meta.description" class="tnl-desc">
              <i class="fa-solid fa-circle-info"></i>
              {{ item.node.description || item.meta.description }}
            </p>

            <!-- 出入参 -->
            <div v-if="item.meta.inputs.length || item.meta.outputs.length" class="tnl-io">
              <div v-if="item.meta.inputs.length" class="io-block">
                <div class="io-title"><i class="fa-solid fa-signs-post"></i> 入参</div>
                <ul class="io-list">
                  <li v-for="(p, i) in item.meta.inputs" :key="`in-${i}`">
                    <code class="io-key">{{ p.key }}</code>
                    <span class="io-label">{{ p.label }}</span>
                    <span class="io-type">{{ p.type }}</span>
                  </li>
                </ul>
              </div>
              <div v-if="item.meta.outputs.length" class="io-block">
                <div class="io-title"><i class="fa-solid fa-paper-plane"></i> 出参</div>
                <ul class="io-list">
                  <li v-for="(p, i) in item.meta.outputs" :key="`out-${i}`">
                    <code class="io-key">{{ p.key }}</code>
                    <span class="io-label">{{ p.label }}</span>
                    <span class="io-type">{{ p.type }}</span>
                  </li>
                </ul>
              </div>
            </div>

            <!-- 完整配置 -->
            <div v-if="item.configTable.length" class="tnl-config">
              <div class="cfg-title">
                <i class="fa-solid fa-gear"></i> 完整配置
                <span class="cfg-meta">敏感字段已脱敏</span>
              </div>
              <ul class="cfg-list">
                <li
                  v-for="c in item.configTable"
                  :key="c.key"
                  :class="{ sensitive: c.sensitive }"
                >
                  <span class="cfg-key">
                    {{ c.label }}
                    <span class="cfg-key-id">({{ c.key }})</span>
                  </span>
                  <span class="cfg-value">
                    <i v-if="c.sensitive" class="fa-solid fa-shield-halved" title="已脱敏"></i>
                    <span :class="{ masked: c.sensitive }">{{ c.display }}</span>
                  </span>
                </li>
              </ul>
            </div>

            <!-- 入口/出口连线 -->
            <div v-if="item.upstream.length || item.downstream.length" class="tnl-edges">
              <div v-if="item.upstream.length" class="edge-row">
                <span class="edge-tag up"><i class="fa-solid fa-turn-up"></i> 入线</span>
                <span class="edge-list">
                  <span v-for="up in item.upstream" :key="up.id" class="edge-node">
                    {{ up.name }}
                  </span>
                </span>
              </div>
              <div v-if="item.downstream.length" class="edge-row">
                <span class="edge-tag down"><i class="fa-solid fa-turn-down"></i> 出线</span>
                <span class="edge-list">
                  <span v-for="down in item.downstream" :key="down.id" class="edge-node">
                    {{ down.name }}
                    <span v-if="down.label" class="edge-label">{{ down.label }}</span>
                  </span>
                </span>
              </div>
            </div>
          </div>
        </li>
      </ol>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { ParsedWorkflowContent, ParsedWorkflowNode } from '@/api/template';
import {
  getNodeVisualMeta,
  buildConfigTable,
  buildKeyParamChips,
} from '@/utils/nodeVisualization';

const props = defineProps<{
  parsed: ParsedWorkflowContent;
}>();

const expandedId = ref<string | null>(null);

function toggle(id: string) {
  expandedId.value = expandedId.value === id ? null : id;
}

interface Enriched {
  node: ParsedWorkflowNode;
  meta: ReturnType<typeof getNodeVisualMeta>;
  configTable: ReturnType<typeof buildConfigTable>;
  keyParams: ReturnType<typeof buildKeyParamChips>;
  configCount: number;
  upstream: { id: string; name: string }[];
  downstream: { id: string; name: string; label?: string }[];
}

const enriched = computed<Enriched[]>(() => {
  if (!props.parsed?.nodes?.length) return [];
  const map = new Map<string, ParsedWorkflowNode>();
  for (const n of props.parsed.nodes) map.set(n.id, n);
  return props.parsed.nodes.map((n) => {
    const meta = getNodeVisualMeta(n.type, n.name);
    const config = (n as any).config && typeof (n as any).config === 'object' ? (n as any).config : {};
    const configTable = buildConfigTable(config);
    const keyParams = buildKeyParamChips(config, 3);
    const upstream: { id: string; name: string }[] = [];
    const downstream: { id: string; name: string; label?: string }[] = [];
    for (const e of props.parsed.edges || []) {
      if (e.target === n.id) {
        const src = map.get(e.source);
        if (src) upstream.push({ id: src.id, name: src.name || src.id });
      }
      if (e.source === n.id) {
        const tgt = map.get(e.target);
        if (tgt) downstream.push({ id: tgt.id, name: tgt.name || tgt.id, label: e.label });
      }
    }
    return {
      node: n,
      meta,
      configTable,
      keyParams,
      configCount: configTable.length,
      upstream,
      downstream,
    };
  });
});

const categorySummary = computed(() => {
  const counts = new Map<string, number>();
  for (const item of enriched.value) {
    counts.set(item.meta.category, (counts.get(item.meta.category) || 0) + 1);
  }
  const order: Array<ReturnType<typeof getNodeVisualMeta>['category']> = ['trigger', 'ai', 'flow', 'tool', 'notify', 'transform', 'end', 'data', 'default'];
  return order
    .map((cat) => {
      const count = counts.get(cat) || 0;
      if (!count) return null;
      const meta = getNodeVisualMeta(undefined);
      // 直接借 theme 色
      const themeMap: Record<string, { label: string; color: string }> = {
        trigger: { label: '触发器', color: '#8b5cf6' },
        ai: { label: 'AI', color: '#3b82f6' },
        flow: { label: '流程控制', color: '#10b981' },
        tool: { label: '工具', color: '#f59e0b' },
        notify: { label: '通知', color: '#ec4899' },
        transform: { label: '转换', color: '#6366f1' },
        end: { label: '结束', color: '#64748b' },
        data: { label: '数据', color: '#06b6d4' },
        default: { label: '其他', color: '#94a3b8' },
      };
      return { category: cat, label: themeMap[cat].label, count, color: themeMap[cat].color };
    })
    .filter(Boolean) as Array<{ category: string; label: string; count: number; color: string }>;
});
</script>

<style scoped>
.tnl { padding: 4px 0; }
.tnl-grid {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 18px;
}
@media (max-width: 768px) {
  .tnl-grid { grid-template-columns: 1fr; }
}

/* 左侧 */
.tnl-side { display: flex; flex-direction: column; gap: 12px; }

.summary-card {
  background: linear-gradient(135deg, #fafbff 0%, #f5f7fb 100%);
  border: 1px solid var(--border-light);
  border-radius: 12px;
  padding: 14px 16px;
}
.sc-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 10px;
}
.sc-title i { color: var(--primary); }
.sc-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.sc-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--text-secondary);
}
.sc-list li .dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
}
.sc-list li .label { flex: 1; }
.sc-list li .count {
  font-weight: 600;
  color: var(--text-primary);
  background: #fff;
  padding: 0 8px;
  border-radius: 999px;
  border: 1px solid var(--border-light);
}

.quick-tips {
  background: var(--info-bg);
  border: 1px solid #bfdbfe;
  border-radius: 10px;
  padding: 12px 14px;
  font-size: 12px;
  color: var(--text-secondary);
  line-height: 1.6;
}
.qt-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 600;
  color: #1d4ed8;
  margin-bottom: 6px;
}

/* 右侧 */
.tnl-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.tnl-item {
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--border-light);
  overflow: hidden;
  transition: all 0.2s ease;
}
.tnl-item:hover { box-shadow: 0 4px 14px rgba(15, 23, 42, 0.04); }
.tnl-item.expanded { box-shadow: 0 4px 18px rgba(15, 23, 42, 0.06); }

.tnl-head {
  width: 100%;
  background: transparent;
  border: 0;
  text-align: left;
  display: grid;
  grid-template-columns: 28px 40px 1fr auto 16px;
  gap: 12px;
  align-items: center;
  padding: 12px 14px;
  cursor: pointer;
}

.step {
  font-size: 10.5px;
  font-weight: 700;
  color: var(--text-secondary);
  background: var(--bg-tertiary);
  text-align: center;
  padding: 4px 0;
  border-radius: 6px;
}

.icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.1);
  position: relative;
}
.icon::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 10px;
  background: linear-gradient(180deg, rgba(255,255,255,0.18), transparent 60%);
  pointer-events: none;
}
.icon i { font-size: 16px; }

.main { min-width: 0; display: flex; flex-direction: column; gap: 4px; }
.name-row { display: flex; align-items: center; gap: 8px; min-width: 0; }
.name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 280px;
}
.cat-pill {
  font-size: 10.5px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
  letter-spacing: 0.3px;
  white-space: nowrap;
}
.type-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.type-tag {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 10.5px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  padding: 1px 6px;
  border-radius: 4px;
}
.param-count, .io-hint {
  font-size: 11px;
  color: var(--text-tertiary);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.param-count i, .io-hint i { font-size: 10px; }

.key-params {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.kp {
  display: inline-flex;
  flex-direction: column;
  background: var(--bg-secondary);
  border: 1px solid var(--border-light);
  border-radius: 8px;
  padding: 4px 8px;
  max-width: 160px;
}
.kp-label { font-size: 9.5px; color: var(--text-tertiary); }
.kp-value {
  font-size: 11.5px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  color: var(--text-primary);
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 144px;
}

.tnl-toggle {
  color: var(--text-tertiary);
  font-size: 12px;
  transition: transform 0.2s;
}
.tnl-item.expanded .tnl-toggle { transform: rotate(180deg); }

/* 展开 body */
.tnl-body {
  padding: 6px 18px 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  border-top: 1px dashed var(--border-light);
  margin-top: -1px;
  background: linear-gradient(180deg, #fafbff00 0%, #fafbff 70%);
}

.tnl-desc {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
  background: var(--bg-secondary);
  padding: 10px 14px;
  border-radius: 8px;
  line-height: 1.6;
}
.tnl-desc i { color: var(--primary); margin-top: 3px; }

.tnl-io {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
@media (max-width: 720px) {
  .tnl-io { grid-template-columns: 1fr; }
}
.io-block {
  background: var(--bg-secondary);
  border-radius: 8px;
  padding: 12px 14px;
  border: 1px solid var(--border-light);
}
.io-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.io-title i { color: var(--primary); font-size: 10.5px; }
.io-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.io-list li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  padding: 4px 0;
  border-bottom: 1px dashed var(--border-light);
}
.io-list li:last-child { border-bottom: none; }
.io-key {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 10.5px;
  background: #fff;
  color: var(--primary);
  padding: 1px 6px;
  border-radius: 4px;
  border: 1px solid var(--border-light);
}
.io-label {
  color: var(--text-primary);
  flex: 1;
}
.io-type {
  font-size: 10.5px;
  color: var(--text-tertiary);
  background: var(--bg-tertiary);
  padding: 1px 6px;
  border-radius: 4px;
}

/* 配置列表 */
.tnl-config {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: 10px;
  padding: 12px 14px;
}
.cfg-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 8px;
}
.cfg-title i { color: var(--primary); }
.cfg-meta {
  font-size: 10.5px;
  font-weight: 400;
  color: var(--warning);
  background: var(--warning-bg);
  padding: 1px 8px;
  border-radius: 999px;
}
.cfg-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0;
}
.cfg-list li {
  display: grid;
  grid-template-columns: 1fr 2fr;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px dashed var(--border-light);
  align-items: baseline;
  font-size: 12.5px;
}
.cfg-list li:last-child { border-bottom: none; }
.cfg-list li.sensitive .cfg-value { color: var(--warning); }
.cfg-list li.sensitive .masked { font-family: ui-monospace, monospace; letter-spacing: 2px; }
.cfg-key {
  color: var(--text-secondary);
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.cfg-key-id {
  font-size: 10.5px;
  color: var(--text-tertiary);
  font-family: ui-monospace, monospace;
}
.cfg-value {
  color: var(--text-primary);
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  word-break: break-all;
  display: flex;
  align-items: center;
  gap: 6px;
}
.cfg-value .fa-shield-halved { color: var(--warning); font-size: 11px; }

/* 连线 summary */
.tnl-edges {
  background: var(--bg-secondary);
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border: 1px solid var(--border-light);
}
.edge-row {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 12px;
}
.edge-tag {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}
.edge-tag.up { background: var(--primary-bg); color: var(--primary); }
.edge-tag.down { background: #ecfeff; color: #0e7490; }
.edge-list { display: flex; gap: 6px; flex-wrap: wrap; }
.edge-node {
  font-size: 11.5px;
  background: #fff;
  padding: 2px 10px;
  border-radius: 999px;
  border: 1px solid var(--border-light);
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.edge-label {
  font-size: 10.5px;
  color: var(--text-tertiary);
  font-style: italic;
}

.warn-block, .empty-block {
  padding: 24px;
  text-align: center;
  color: var(--text-tertiary);
  background: var(--bg-secondary);
  border-radius: var(--radius-md);
  font-size: 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}
.warn-block {
  background: var(--warning-bg);
  color: var(--warning);
}
</style>
