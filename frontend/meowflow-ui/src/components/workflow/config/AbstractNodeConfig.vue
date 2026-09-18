<template>
  <div class="abstract-config">
    <el-alert type="info" :closable="false" class="info-banner">
      <template #title>
        <strong>自定义节点</strong> — 通过参数定义 + 代码,自由定义节点行为
      </template>
      <div class="alert-extra">
        上游输入会以 <code>input</code> 对象形式注入;
        代码返回值作为 <code>output</code> 传递给下游。
      </div>
    </el-alert>

    <el-tabs v-model="activeTab" class="config-tabs">
      <!-- 参数定义 -->
      <el-tab-pane label="参数定义" name="params">
        <el-form label-width="100px" size="small">
          <el-form-item label="代码语言">
            <el-radio-group v-model="language">
              <el-radio-button value="javascript">JavaScript</el-radio-button>
              <el-radio-button value="python">Python</el-radio-button>
            </el-radio-group>
            <div class="tip">JS 由 Node.js 在执行器沙箱中运行;Python 由 Python subprocess 执行</div>
          </el-form-item>

          <el-form-item label="输入 Schema">
            <CodeEditor
              v-model="inputSchemaText"
              language="json"
              :rows="6"
              placeholder='{"text":"string","count":"number"}'
            />
            <div class="tip">JSON Schema 描述上游 <code>input</code> 对象的字段类型</div>
          </el-form-item>

          <el-form-item label="输出 Schema">
            <CodeEditor
              v-model="outputSchemaText"
              language="json"
              :rows="6"
              placeholder='{"result":"string"}'
            />
            <div class="tip">JSON Schema 描述代码返回值的字段类型</div>
          </el-form-item>

          <el-form-item label="超时(秒)">
            <el-input-number v-model="timeoutSeconds" :min="5" :max="300" />
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 执行逻辑 -->
      <el-tab-pane label="执行代码" name="code">
        <el-form label-width="100px" size="small">
          <el-form-item label="代码" required>
            <CodeEditor
              v-model="source"
              :language="language === 'python' ? 'python' : 'javascript'"
              :rows="14"
              :placeholder="placeholder"
            />
          </el-form-item>

          <el-form-item label="变量">
            <VariablePicker inline :groups="variableGroups" @insert="insertToCode" />
          </el-form-item>

          <el-form-item label="可用内置">
            <ul class="builtin-list">
              <li><code>input</code> — 上游注入的对象</li>
              <li><code>console.log(...)</code> — 输出到执行日志</li>
              <li v-if="language === 'javascript'"><code>fetch(url)</code> — 内置 HTTP 客户端</li>
              <li v-if="language === 'python'"><code>requests</code> — 已加载</li>
              <li><code>return &#123;...&#125;</code> — 作为 output 返回</li>
            </ul>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <!-- 测试 -->
      <el-tab-pane label="测试运行" name="test">
        <el-form label-width="100px" size="small">
          <el-form-item label="输入示例">
            <CodeEditor
              v-model="testInputText"
              language="json"
              :rows="6"
              placeholder='{"text":"hello","count":3}'
            />
          </el-form-item>
          <el-form-item label="">
            <el-button type="primary" :loading="testing" @click="runTest">
              <i class="fa-solid fa-play"></i>
              <span style="margin-left:6px">执行测试</span>
            </el-button>
          </el-form-item>
          <el-form-item v-if="testResult" label="输出">
            <pre class="test-output">{{ testResult }}</pre>
          </el-form-item>
          <el-form-item v-if="testError" label="错误">
            <pre class="test-error">{{ testError }}</pre>
          </el-form-item>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import CodeEditor from '@/components/common/CodeEditor.vue';
import VariablePicker, { type VariableGroup } from './VariablePicker.vue';
import { ElMessage } from '@/utils/notify';

const props = defineProps<{
  modelValue: Record<string, any>;
  variableGroups?: VariableGroup[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

const activeTab = ref('code');
const testing = ref(false);
const testResult = ref('');
const testError = ref('');

function patch(extra: Record<string, any>) {
  emit('update:modelValue', { ...props.modelValue, ...extra });
}

const language = computed({
  get: () => props.modelValue?.language ?? 'javascript',
  set: (v: string) => patch({ language: v }),
});

const inputSchemaText = computed({
  get: () => toText(props.modelValue?.inputSchema),
  set: (v: string) => patch({ inputSchema: v }),
});

const outputSchemaText = computed({
  get: () => toText(props.modelValue?.outputSchema),
  set: (v: string) => patch({ outputSchema: v }),
});

const source = computed({
  get: () => props.modelValue?.source ?? '',
  set: (v: string) => patch({ source: v }),
});

const timeoutSeconds = computed({
  get: () => Number(props.modelValue?.timeoutSeconds ?? 60),
  set: (v: number) => patch({ timeoutSeconds: v }),
});

const testInputText = computed({
  get: () => toText(props.modelValue?.testInput, { text: 'hello', count: 3 }),
  set: (v: string) => patch({ testInput: v }),
});

function toText(v: any, fallback: any = {}) {
  if (typeof v === 'string') return v;
  if (v) return JSON.stringify(v, null, 2);
  return JSON.stringify(fallback, null, 2);
}

const placeholder = computed(() => {
  if (language.value === 'python') {
    return 'import json\ninput = json.loads(input_json)\nresult = {"result": input["text"][:input["count"]]}\nprint(json.dumps(result))';
  }
  return '// input 是上游注入的对象\nconst result = { result: input.text };\nreturn result;';
});

function insertToCode(path: string) {
  if (language.value === 'python') {
    source.value = (source.value ?? '') + `\n# {{${path}}}\n`;
  } else {
    source.value = (source.value ?? '') + `\n// {{${path}}}\n`;
  }
}

async function runTest() {
  testing.value = true;
  testResult.value = '';
  testError.value = '';
  try {
    // 此处应调用后端执行测试接口,目前前端模拟
    const input = JSON.parse(testInputText.value || '{}');
    // 模拟执行
    if (language.value === 'javascript') {
      // eslint-disable-next-line no-new-func
      const fn = new Function('input', source.value);
      const out = fn(input);
      testResult.value = JSON.stringify(out, null, 2);
    } else {
      testResult.value = '(请在后端执行 Python 代码测试,前端模拟仅支持 JS)';
    }
    ElMessage.success('执行成功');
  } catch (e: any) {
    testError.value = e?.message || String(e);
    ElMessage.error('执行失败');
  } finally {
    testing.value = false;
  }
}
</script>

<style scoped>
.abstract-config { padding: 4px 0; }
.info-banner {
  margin-bottom: 12px;
}
.alert-extra {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 4px;
}
.alert-extra code {
  background: var(--bg-secondary);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: ui-monospace, monospace;
  font-size: 11px;
}
.config-tabs { margin-top: 0; }
.tip {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
.builtin-list {
  margin: 0;
  padding-left: 20px;
  font-size: 12px;
  color: var(--text-secondary);
}
.builtin-list li { margin: 2px 0; }
.builtin-list code {
  font-family: ui-monospace, monospace;
  background: var(--bg-secondary);
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 11px;
}
.test-output, .test-error {
  background: var(--bg-secondary);
  border-radius: var(--radius-sm);
  padding: 10px;
  font-family: ui-monospace, monospace;
  font-size: 12px;
  white-space: pre-wrap;
  max-height: 300px;
  overflow: auto;
  margin: 0;
}
.test-error {
  background: var(--danger-bg);
  color: var(--danger);
}
</style>
