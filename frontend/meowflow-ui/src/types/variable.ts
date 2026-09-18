import type { NodeCategory } from '@/types/workflow';

export interface Variable {
  name: string;
  type: 'string' | 'number' | 'boolean' | 'array' | 'object' | 'any';
  required?: boolean;
  default?: any;
  description?: string;
}

export interface VariableEdge {
  nodeId: string;
  varName: string;
  type: Variable['type'];
}

export interface VariableProvider {
  nodeId: string;
  nodeName: string;
  category: NodeCategory;
  variables: Variable[];
}
