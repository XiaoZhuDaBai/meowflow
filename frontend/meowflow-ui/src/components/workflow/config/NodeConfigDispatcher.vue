<template>
  <div class="node-config-dispatcher">
    <!-- 触发器 -->
    <WebhookTriggerConfig
      v-if="isType('trigger.webhook')"
      :model-value="configValue"
      :workflow-id="workflowId"
      @update:model-value="emitChange"
    />
    <CronConfig
      v-else-if="isType('trigger.cron')"
      :model-value="configValue"
      @update:model-value="emitChange"
    />

    <!-- AI 节点 -->
    <LLMConfig
      v-else-if="isType('ai.llm')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />
    <ClassifyConfig
      v-else-if="isType('ai.classify')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />
    <AgentConfig
      v-else-if="isType('ai.agent')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />
    <ParameterExtractorConfig
      v-else-if="isType('ai.parameter-extractor')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />
    <QuestionClassifierConfig
      v-else-if="isType('ai.question-classifier')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />

    <!-- 流程 -->
    <ConditionConfig
      v-else-if="isType('flow.condition')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />
    <IfElseConfig
      v-else-if="isType('flow.if-else')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />
    <IterationConfig
      v-else-if="isType('flow.iteration')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />
    <AggregationConfig
      v-else-if="isType('flow.aggregation')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />
    <TemplateTransformConfig
      v-else-if="isType('flow.template-transform')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />

    <!-- 工具 -->
    <HTTPConfig
      v-else-if="isType('tool.http')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />
    <ListOperatorConfig
      v-else-if="isType('tool.list-operator')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />
    <VariableAssignerConfig
      v-else-if="isType('tool.variable-assigner')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />
    <DocumentExtractorConfig
      v-else-if="isType('tool.document-extractor')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />
    <AbstractNodeConfig
      v-else-if="isType('tool.abstract')"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />

    <!-- 通知 -->
    <NotifyConfig
      v-else-if="platformNotifyTypes.has(node.type)"
      :model-value="configValue"
      :variable-groups="variableGroups"
      @update:model-value="emitChange"
    />

    <!-- 回退: 根据 def.params 生成通用表单 -->
    <GenericConfig
      v-else
      :params="def?.params ?? []"
      :model-value="configValue"
      @update:model-value="emitChange"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { WorkflowNode } from '@/types/workflow';
import type { NodeDefinition } from '@/types/node';
import { resolveNodeType } from '@/mock/nodes';

import WebhookTriggerConfig from './WebhookTriggerConfig.vue';
import CronConfig from './CronConfig.vue';

// AI
import LLMConfig from './LLMConfig.vue';
import ClassifyConfig from './ClassifyConfig.vue';
import AgentConfig from './AgentConfig.vue';
import ParameterExtractorConfig from './ParameterExtractorConfig.vue';
import QuestionClassifierConfig from './QuestionClassifierConfig.vue';

// Flow
import ConditionConfig from './ConditionConfig.vue';
import IfElseConfig from './IfElseConfig.vue';
import IterationConfig from './IterationConfig.vue';
import AggregationConfig from './AggregationConfig.vue';
import TemplateTransformConfig from './TemplateTransformConfig.vue';

// Tool
import HTTPConfig from './HTTPConfig.vue';
import ListOperatorConfig from './ListOperatorConfig.vue';
import VariableAssignerConfig from './VariableAssignerConfig.vue';
import DocumentExtractorConfig from './DocumentExtractorConfig.vue';
import AbstractNodeConfig from './AbstractNodeConfig.vue';

// Notify
import NotifyConfig from './NotifyConfig.vue';

// Fallback
import GenericConfig from './GenericConfig.vue';
import type { VariableGroup } from './VariablePicker.vue';

const props = defineProps<{
  node: WorkflowNode;
  def: NodeDefinition | null;
  configValue: Record<string, any>;
  variableGroups: VariableGroup[];
  workflowId?: string;
}>();

const emit = defineEmits<{
  (e: 'update', v: Record<string, any>): void;
}>();

const platformNotifyTypes = new Set([
  'notify.dingtalk',
  'notify.wxwork',
  'notify.feishu',
  'notify.email',
  'notify.sms',
]);

/**
 * 统一按「别名解析后」的类型做匹配：
 * builtinTemplates 等历史数据可能使用旧名（http.request / condition.if / code.transform / ...），
 * 通过 resolveNodeType 归一化到现役 NODE_CATALOG 的 type。
 */
const resolvedType = computed(() => resolveNodeType(props.node.type));

function isType(t: string) {
  return resolvedType.value === t;
}

function emitChange(v: Record<string, any>) {
  emit('update', v);
}
</script>
