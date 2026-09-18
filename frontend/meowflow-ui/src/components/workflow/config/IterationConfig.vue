<template>
  <div class="iteration-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="数据源" required>
        <el-input v-model="source" placeholder="{{upstream.items}}" />
        <div class="tip">上游节点输出的数组变量路径,例 <code>&#123;&#123;llm.results&#125;&#125;</code></div>
      </el-form-item>

      <el-form-item label="迭代变量" required>
        <el-input v-model="iterator" placeholder="item" />
        <div class="tip">在子流程内部使用 <code>&#123;&#123;item&#125;&#125;</code> 引用当前元素</div>
      </el-form-item>

      <el-form-item label="变量">
        <VariablePicker inline :groups="variableGroups" @insert="insertAtSource" />
      </el-form-item>

      <el-form-item label="输出模式">
        <el-radio-group v-model="outputType">
          <el-radio-button value="array">数组</el-radio-button>
          <el-radio-button value="flatten">扁平合并</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="并行执行">
        <el-switch v-model="parallel" />
        <span class="tip">子节点并发执行 (实验性)</span>
      </el-form-item>

      <el-form-item label="错误处理">
        <el-radio-group v-model="errorMode">
          <el-radio-button value="abort">终止</el-radio-button>
          <el-radio-button value="skip">跳过错误项</el-radio-button>
          <el-radio-button value="continue">继续(错误计数)</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="输出变量">
        <el-input v-model="outputName" placeholder="results" />
      </el-form-item>

      <el-form-item label="子流程节点">
        <el-select
          v-model="subgraphNodeIds"
          multiple
          filterable
          style="width: 100%"
          placeholder="选择画布节点"
        >
          <el-option
            v-for="n in availableNodes"
            :key="n.id"
            :label="`${n.name} (${n.id})`"
            :value="n.id"
          />
        </el-select>
        <div class="tip">选择的节点按当前顺序组成迭代子流程</div>
      </el-form-item>

      <el-form-item label="子流程画布">
        <el-button @click="subgraphEditorOpen = true">
          <i class="fa-solid fa-diagram-project"></i><span style="margin-left:4px">编辑子流程</span>
        </el-button>
        <div class="tip">用独立画布拖拽编排迭代内部的节点和连线</div>
      </el-form-item>
    </el-form>

    <IterationSubgraphEditor
      v-model="subgraphEditorOpen"
      :subgraph="subgraphModel"
      @update:subgraph="onSubgraphUpdate"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import VariablePicker, { type VariableGroup } from './VariablePicker.vue';
import IterationSubgraphEditor from '@/components/workflow/IterationSubgraphEditor.vue';
import { useWorkflowStore } from '@/stores/workflow';

const props = defineProps<{
  modelValue: Record<string, any>;
  variableGroups?: VariableGroup[];
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

const subgraphEditorOpen = ref(false);
const store = useWorkflowStore();

const availableNodes = computed(() =>
  (store.current?.nodes ?? []).filter((n) => n.type !== 'flow.iteration'),
);

function patch(extra: Record<string, any>) {
  emit('update:modelValue', { ...props.modelValue, ...extra });
}

const source = computed({
  get: () => props.modelValue?.source ?? '',
  set: (v: string) => patch({ source: v }),
});

const iterator = computed({
  get: () => props.modelValue?.iterator ?? 'item',
  set: (v: string) => patch({ iterator: v }),
});

const outputType = computed({
  get: () => props.modelValue?.outputType ?? 'array',
  set: (v: string) => patch({ outputType: v }),
});

const parallel = computed({
  get: () => props.modelValue?.parallel ?? false,
  set: (v: boolean) => patch({ parallel: v }),
});

const errorMode = computed({
  get: () => props.modelValue?.errorMode ?? 'abort',
  set: (v: string) => patch({ errorMode: v }),
});

const outputName = computed({
  get: () => props.modelValue?.outputName ?? 'results',
  set: (v: string) => patch({ outputName: v }),
});

const subgraphNodeIds = computed({
  get: () => props.modelValue?.subgraphNodes ?? [],
  set: (v: string[]) => patch({ subgraphNodes: v }),
});

const subgraphModel = computed(() => props.modelValue?.subgraph ?? { nodes: [], edges: [] });

function onSubgraphUpdate(value: { nodes: any[]; edges: any[] }) {
  patch({ subgraph: value });
}

function insertAtSource(path: string) {
  source.value = `{{${path}}}`;
}
</script>

<style scoped>
.tip {
  font-size: 11px;
  color: var(--text-tertiary);
  margin-top: 4px;
}
</style>
