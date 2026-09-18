<template>
  <div class="tcp">
    <div v-if="!groups.length" class="empty-block">
      <i class="fa-solid fa-circle-info"></i>
      <span>该模板暂未提供参数配置</span>
    </div>

    <div v-else class="tcp-content">
      <!-- 顶部筛选条 -->
      <div class="tcp-toolbar">
        <el-input
          v-model="keyword"
          size="small"
          placeholder="搜索节点名称 / 参数 key / value…"
          clearable
          class="tcp-search"
        >
          <template #prefix><i class="fa-solid fa-magnifying-glass" /></template>
        </el-input>

        <div class="cat-filter">
          <button
            v-for="c in categoryChips"
            :key="c.category"
            class="cf-chip"
            :class="{ active: activeCats.has(c.category) }"
            :style="{ '--c': c.color }"
            @click="toggleCat(c.category)"
          >
            <span class="dot" :style="{ background: c.color }" />
            {{ c.label }}
            <span class="c">{{ c.count }}</span>
          </button>
        </div>
      </div>

      <!-- 按节点分组展示 -->
      <div class="tcp-groups">
        <article
          v-for="g in visibleGroups"
          :key="g.nodeId"
          class="tcp-group"
          :style="{ '--accent': g.color }"
        >
          <header class="tcp-group-head">
            <span class="icon" :style="{ background: g.color }">
              <i :class="['fa-solid', g.icon]" />
            </span>
            <span class="name">{{ g.name }}</span>
            <span class="cat-pill" :style="{ background: g.categorySoft, color: g.color }">
              {{ g.categoryLabel }}
            </span>
            <span class="step">#{{ g.step }}</span>
            <span class="badge">{{ g.configs.length }} 项</span>
          </header>

          <table class="tcp-table">
            <thead>
              <tr>
                <th width="36%">参数</th>
                <th>当前值</th>
                <th width="14%">类型</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="c in g.configs"
                :key="c.key"
                :class="{ sensitive: c.sensitive }"
              >
                <td class="key-cell">
                  <span class="key">{{ c.label }}</span>
                  <span class="key-id">({{ c.key }})</span>
                </td>
                <td>
                  <span class="value" :class="{ masked: c.sensitive }">
                    <i v-if="c.sensitive" class="fa-solid fa-shield-halved" />
                    {{ c.display }}
                  </span>
                </td>
                <td>
                  <span class="type-chip">{{ inferType(c.value) }}</span>
                </td>
              </tr>
            </tbody>
          </table>
        </article>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import type { ParsedWorkflowContent } from '@/api/template';
import {
  getNodeVisualMeta,
  buildConfigTable,
  type ConfigDisplayItem,
} from '@/utils/nodeVisualization';

const props = defineProps<{
  parsed: ParsedWorkflowContent;
}>();

const keyword = ref('');
const activeCats = ref<Set<string>>(new Set()); // 空集 = 显示全部

interface Group {
  step: number;
  nodeId: string;
  name: string;
  icon: string;
  color: string;
  categoryLabel: string;
  categorySoft: string;
  category: string;
  configs: ConfigDisplayItem[];
}

const groups = computed<Group[]>(() => {
  if (!props.parsed?.nodes?.length) return [];
  return props.parsed.nodes.map((n, idx) => {
    const meta = getNodeVisualMeta(n.type, n.name);
    const config = (n as any).config && typeof (n as any).config === 'object' ? (n as any).config : {};
    const table = buildConfigTable(config);
    return {
      step: idx + 1,
      nodeId: n.id,
      name: n.name || n.id,
      icon: meta.icon,
      color: meta.color,
      categoryLabel: meta.categoryLabel,
      categorySoft: meta.categorySoft,
      category: meta.category,
      configs: table,
    };
  }).filter((g) => g.configs.length > 0);
});

const categoryChips = computed(() => {
  const map = new Map<string, { category: string; label: string; color: string; count: number }>();
  for (const g of groups.value) {
    const cur = map.get(g.category);
    if (cur) cur.count += g.configs.length;
    else map.set(g.category, {
      category: g.category,
      label: g.categoryLabel,
      color: g.color,
      count: g.configs.length,
    });
  }
  return Array.from(map.values()).sort((a, b) => b.count - a.count);
});

function toggleCat(cat: string) {
  const set = new Set(activeCats.value);
  if (set.has(cat)) set.delete(cat);
  else set.add(cat);
  activeCats.value = set;
}

const visibleGroups = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return groups.value
    .filter((g) => {
      if (activeCats.value.size && !activeCats.value.has(g.category)) return false;
      if (!kw) return true;
      if (g.name.toLowerCase().includes(kw)) return true;
      if (g.categoryLabel.toLowerCase().includes(kw)) return true;
      return g.configs.some(
        (c) =>
          c.label.toLowerCase().includes(kw) ||
          c.key.toLowerCase().includes(kw) ||
          String(c.value ?? '').toLowerCase().includes(kw),
      );
    })
    .map((g) => {
      if (!kw) return g;
      const filtered = g.configs.filter(
        (c) =>
          g.name.toLowerCase().includes(kw) ||
          g.categoryLabel.toLowerCase().includes(kw) ||
          c.label.toLowerCase().includes(kw) ||
          c.key.toLowerCase().includes(kw) ||
          String(c.value ?? '').toLowerCase().includes(kw),
      );
      return { ...g, configs: filtered };
    })
    .filter((g) => g.configs.length > 0);
});

function inferType(v: unknown): string {
  if (v === null) return 'null';
  if (v === undefined) return 'undefined';
  if (Array.isArray(v)) return 'array';
  return typeof v;
}
</script>

<style scoped>
.tcp { padding: 4px 0; }
.tcp-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.tcp-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}
.tcp-search { width: 280px; }

.cat-filter { display: flex; flex-wrap: wrap; gap: 6px; flex: 1; }
.cf-chip {
  --c: #6366f1;
  border: 1px solid var(--c);
  background: #fff;
  color: var(--c);
  padding: 4px 10px;
  border-radius: 999px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 500;
  transition: background 0.15s, color 0.15s;
}
.cf-chip .dot { width: 7px; height: 7px; border-radius: 999px; }
.cf-chip .c {
  font-size: 10.5px;
  background: rgba(255, 255, 255, 0.7);
  padding: 0 6px;
  border-radius: 999px;
}
.cf-chip.active {
  background: var(--c);
  color: #fff;
}
.cf-chip.active .c { background: rgba(255, 255, 255, 0.25); color: #fff; }

.tcp-groups { display: flex; flex-direction: column; gap: 14px; }

.tcp-group {
  --accent: #6366f1;
  background: #fff;
  border-radius: 12px;
  border: 1px solid var(--border-light);
  overflow: hidden;
  border-left: 3px solid var(--accent);
}

.tcp-group-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: linear-gradient(90deg, var(--accent, #6366f1)0c 0%, transparent 60%);
  border-bottom: 1px solid var(--border-light);
}

.tcp-group-head .icon {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 13px;
  box-shadow: 0 4px 8px rgba(15, 23, 42, 0.1);
}
.tcp-group-head .name { font-size: 14px; font-weight: 600; color: var(--text-primary); }
.tcp-group-head .cat-pill {
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 999px;
}
.tcp-group-head .step {
  font-family: ui-monospace, monospace;
  font-size: 11px;
  color: var(--text-tertiary);
}
.tcp-group-head .badge {
  margin-left: auto;
  font-size: 11px;
  color: var(--text-secondary);
  background: var(--bg-tertiary);
  padding: 2px 10px;
  border-radius: 999px;
}

.tcp-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.tcp-table thead th {
  text-align: left;
  font-size: 11px;
  font-weight: 600;
  color: var(--text-tertiary);
  text-transform: uppercase;
  letter-spacing: 0.5px;
  padding: 8px 16px;
  background: var(--bg-secondary);
  border-bottom: 1px solid var(--border-light);
}
.tcp-table tbody td {
  padding: 10px 16px;
  border-bottom: 1px dashed var(--border-light);
  vertical-align: top;
}
.tcp-table tbody tr:last-child td { border-bottom: none; }

.key-cell {
  display: flex;
  align-items: baseline;
  gap: 6px;
}
.key { color: var(--text-primary); font-weight: 500; }
.key-id {
  font-family: ui-monospace, monospace;
  font-size: 11px;
  color: var(--text-tertiary);
}
.value {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  word-break: break-all;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.value.masked { color: var(--warning); letter-spacing: 2px; }
.value .fa-shield-halved { color: var(--warning); font-size: 11px; }

.type-chip {
  display: inline-block;
  font-size: 11px;
  background: var(--bg-tertiary);
  color: var(--text-secondary);
  padding: 1px 8px;
  border-radius: 4px;
  font-family: ui-monospace, monospace;
  text-transform: lowercase;
}
tr.sensitive { background: rgba(245, 158, 11, 0.04); }

.empty-block {
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
</style>
