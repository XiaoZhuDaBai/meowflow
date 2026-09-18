import type { NodeCategory } from './workflow';

export type NodeClassification = 'input' | 'processing' | 'output' | 'logic';

export interface NodeParamSchema {
  key: string;
  label: string;
  type: 'string' | 'text' | 'number' | 'boolean' | 'select' | 'model' | 'code' | 'json';
  required?: boolean;
  default?: any;
  options?: Array<{ label: string; value: string }>;
  placeholder?: string;
  description?: string;
}

export interface NodeOutputSchema {
  key: string;
  label: string;
  type: 'string' | 'number' | 'boolean' | 'array' | 'object' | 'any';
  description?: string;
}

export interface NodeInputSchema {
  key: string;
  label: string;
  type: 'string' | 'number' | 'boolean' | 'array' | 'object' | 'any';
  required?: boolean;
  description?: string;
}

export interface NodeDefinition {
  type: string;
  category: NodeCategory;
  classification: NodeClassification;
  name: string;
  icon: string;
  color: string;
  description: string;
  params: NodeParamSchema[];
  inputs?: NodeInputSchema[];
  outputs?: NodeOutputSchema[];
  author?: string;
  version?: string;
  sort?: number;
  checkValid?: (config: Record<string, any>) => { isValid: boolean; error?: string };
}

export interface NodeCatalogGroup {
  category: NodeCategory;
  label: string;
  icon: string;
  color: string;
  items: NodeDefinition[];
}

export interface BlockClassification {
  key: string;
  label: string;
  tabsKey: string;
}

export const BLOCK_CLASSIFICATIONS: BlockClassification[] = [
  { key: '-', label: '全部', tabsKey: 'all' },
  { key: 'input', label: '输入', tabsKey: 'input' },
  { key: 'processing', label: '处理', tabsKey: 'processing' },
  { key: 'logic', label: '逻辑', tabsKey: 'logic' },
  { key: 'output', label: '输出', tabsKey: 'output' },
];
