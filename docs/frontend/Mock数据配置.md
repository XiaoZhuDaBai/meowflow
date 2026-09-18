# 喵流前端 Mock 数据完整配置

> 基于已定义的 API 接口生成完整 Mock 数据
> 版本：v1.0 | 更新日期：2026-07-10

---

## 一、Mock 环境配置

### 1.1 安装依赖

```bash
pnpm add -D vite-plugin-mock better-mock @types/better-mock mockjs
```

### 1.2 Vite 配置

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { viteMockServe } from 'vite-plugin-mock';
import path from 'path';

export default defineConfig({
  plugins: [
    vue(),
    viteMockServe({
      mockPath: './src/api/mock',
      enable: true,
      watchFiles: true,
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3000,
  },
});
```

### 1.3 环境变量

```bash
# .env.development
VITE_API_BASE_URL=/api
VITE_USE_MOCK=true
VITE_MOCK_DELAY=300
```

### 1.4 Mock 插件配置

```typescript
// src/plugins/mock.ts
import { registerMock } from 'vite-plugin-mock';

export function setupMock() {
  if (import.meta.env.VITE_USE_MOCK !== 'true') {
    return;
  }

  // 动态导入所有 mock 文件
  const mockModules = import.meta.glob('./mock/*.ts', { eager: true });

  Object.values(mockModules).forEach((module: any) => {
    if (module.default && Array.isArray(module.default)) {
      registerMock(module.default);
    }
  });
}
```

---

## 二、Mock 工具函数

### 2.1 基础工具

```typescript
// src/api/mock/utils.ts
import Mock from 'better-mock';
import type { WorkflowStatus, ExecutionStatus, NodeType, NodeCategory } from '@/types/node';

/**
 * 生成随机 ID
 */
export function mockId(): string {
  return Mock.mock('@id');
}

/**
 * 生成随机日期
 */
export function mockDate(options?: {
  start?: Date;
  end?: Date;
  format?: string;
}): string {
  return Mock.mock({
    'date|1': [
      () => Mock.Random.datetime('yyyy-MM-dd HH:mm:ss'),
      () => Mock.Random.datetime('yyyy-MM-dd'),
      () => Mock.Random.date(),
    ],
  }).date;
}

/**
 * 生成相对时间
 */
export function mockRelativeTime(): string {
  const times = [
    () => '刚刚',
    () => `${Mock.mock('@integer(1, 59)')} 分钟前`,
    () => `${Mock.mock('@integer(1, 23)')} 小时前`,
    () => `今天 ${Mock.mock('@time("HH:mm")')}`,
    () => `昨天 ${Mock.mock('@time("HH:mm")')}`,
    () => `${Mock.mock('@integer(1, 6)')} 天前`,
    () => `${Mock.mock('@integer(1, 4)')} 周前`,
  ];
  return Mock.mock({
    'value|1': times,
  }).value;
}

/**
 * 生成随机分类
 */
export function mockCategory(): string {
  return Mock.mock({
    'value|1': [
      'customer-service',
      'hr',
      'operation',
      'content',
      'admin',
      'data',
    ],
  }).value;
}

/**
 * 生成随机状态
 */
export function mockWorkflowStatus(): WorkflowStatus {
  return Mock.mock({
    'value|1': ['draft', 'running', 'stopped', 'archived'],
  }).value;
}

export function mockExecutionStatus(): ExecutionStatus {
  return Mock.mock({
    'value|1': ['running', 'success', 'failed', 'cancelled', 'waiting'],
  }).value;
}

/**
 * 生成随机节点类型
 */
export function mockNodeType(): NodeType {
  const types: NodeType[] = [
    'webhook', 'schedule', 'llm', 'classify', 'condition',
    'http', 'variable', 'dingtalk', 'email'
  ];
  return Mock.mock({
    'value|1': types,
  }).value;
}

export function mockNodeCategory(): NodeCategory {
  const categories: NodeCategory[] = ['trigger', 'ai', 'flow', 'tool', 'notify'];
  return Mock.mock({
    'value|1': categories,
  }).value;
}

/**
 * 生成随机颜色
 */
export function mockColor(): string {
  return Mock.mock('@color');
}

/**
 * 生成随机图标
 */
export function mockIcon(): string {
  const icons = [
    'play', 'clock', 'brain', 'tags', 'code-branch',
    'globe', 'database', 'bell', 'envelope'
  ];
  return Mock.mock({
    'value|1': icons,
  }).value;
}

/**
 * 生成分页数据
 */
export function mockPaginatedData<T>(data: T[], page: number = 1, pageSize: number = 10) {
  const start = (page - 1) * pageSize;
  const end = start + pageSize;
  return {
    items: data.slice(start, end),
    total: data.length,
    page,
    pageSize,
    totalPages: Math.ceil(data.length / pageSize),
  };
}

/**
 * 随机延迟
 */
export function mockDelay(min: number = 100, max: number = 500): number {
  return Mock.mock(`@integer(${min}, ${max})`);
}

/**
 * 生成 API 响应
 */
export function mockResponse<T>(data: T): {
  code: number;
  message: string;
  data: T;
  timestamp: number;
  traceId: string;
} {
  return {
    code: 200,
    message: 'success',
    data,
    timestamp: Date.now(),
    traceId: `trace-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`,
  };
}
```

### 2.2 数据生成器

```typescript
// src/api/mock/generators.ts
import Mock from 'better-mock';
import type { Workflow, WorkflowNode, WorkflowEdge, WorkflowListItem } from '@/types/workflow';
import type { Execution, ExecutionLog, NodeExecution } from '@/types/execution';

/**
 * 生成工作流统计
 */
export function generateWorkflowStats() {
  return {
    executions: Mock.mock('@integer(0, 10000)'),
    successRate: Mock.mock('@float(90, 100, 2)'),
    avgDuration: Mock.mock('@integer(100, 5000)'),
    totalCost: Mock.mock('@float(0, 1000, 2)'),
    todayExecutions: Mock.mock('@integer(0, 500)'),
    todayCost: Mock.mock('@float(0, 100, 2)'),
  };
}

/**
 * 生成工作流节点
 */
export function generateWorkflowNode(overrides?: Partial<WorkflowNode>): WorkflowNode {
  const types = ['webhook', 'schedule', 'llm', 'classify', 'extract', 'condition', 'http', 'dingtalk'];
  const categories = ['trigger', 'trigger', 'ai', 'ai', 'ai', 'flow', 'tool', 'notify'];

  const typeIndex = Mock.mock('@integer(0, 7)');

  return {
    id: Mock.mock('@id'),
    type: types[typeIndex] as any,
    category: categories[typeIndex] as any,
    title: Mock.mock('@cword(3, 8)'),
    position: {
      x: Mock.mock('@integer(100, 600)'),
      y: Mock.mock('@integer(100, 800)'),
    },
    config: generateNodeConfig(types[typeIndex]),
    input: { variables: [] },
    output: { variables: [] },
    status: 'idle',
    ...overrides,
  };
}

/**
 * 生成节点配置
 */
export function generateNodeConfig(type: string): Record<string, any> {
  switch (type) {
    case 'llm':
      return {
        model: Mock.mock({
          'value|1': ['gpt-4o', 'gpt-4o-mini', 'claude-3', 'qwen-plus'],
        }).value,
        temperature: 0.7,
        maxTokens: 1000,
        systemPrompt: Mock.mock('@cparagraph(2)'),
        userTemplate: '{{input.text}}',
      };
    case 'classify':
      return {
        model: 'gpt-4o',
        categories: [
          { id: '1', name: '咨询', description: '产品咨询类问题' },
          { id: '2', name: '投诉', description: '投诉建议类问题' },
          { id: '3', name: '售后', description: '售后问题' },
        ],
        systemPrompt: '你是一个客服问题分类助手',
      };
    case 'http':
      return {
        method: Mock.mock({ 'value|1': ['GET', 'POST'] }).value,
        url: Mock.mock('@url'),
        headers: {},
        body: { type: 'json', content: '{}' },
        auth: { type: 'none' },
      };
    case 'webhook':
      return {
        method: 'POST',
        path: `/webhook/${Mock.mock('@string("lower", 8)')}`,
        auth: { type: 'bearer', token: Mock.mock('@guid') },
      };
    case 'schedule':
      return {
        cron: '0 */5 * * * *',
        timezone: 'Asia/Shanghai',
        enabled: true,
      };
    case 'dingtalk':
      return {
        webhook: 'https://oapi.dingtalk.com/robot/send?access_token=xxx',
        msgType: 'text',
        content: {
          text: '工作流执行通知',
        },
      };
    case 'email':
      return {
        to: ['user@example.com'],
        subject: '工作流执行结果',
        body: '执行完成，请查看详情',
        isHtml: false,
      };
    case 'condition':
      return {
        branches: [
          { id: '1', label: '高优先级', condition: '{{priority}} == "high"' },
          { id: '2', label: '普通', condition: '{{priority}} == "normal"' },
        ],
      };
    default:
      return {};
  }
}

/**
 * 生成工作流边
 */
export function generateWorkflowEdge(): WorkflowEdge {
  return {
    id: Mock.mock('@id'),
    source: Mock.mock('@id'),
    target: Mock.mock('@id'),
  };
}

/**
 * 生成完整工作流
 */
export function generateWorkflow(overrides?: Partial<Workflow>): Workflow {
  const nodeCount = Mock.mock('@integer(3, 8)');
  const nodes: WorkflowNode[] = [];

  // 生成节点
  for (let i = 0; i < nodeCount; i++) {
    const node = generateWorkflowNode({
      position: { x: 400, y: 100 + i * 150 },
    });
    nodes.push(node);
  }

  // 生成边（简单线性连接）
  const edges: WorkflowEdge[] = [];
  for (let i = 0; i < nodes.length - 1; i++) {
    edges.push({
      id: Mock.mock('@id'),
      source: nodes[i].id,
      target: nodes[i + 1].id,
    });
  }

  const categories = ['customer-service', 'hr', 'operation', 'content', 'admin', 'data'];
  const statusList = ['draft', 'running', 'stopped'];

  return {
    id: Mock.mock('@id'),
    name: Mock.mock('@ctitle(4, 12)'),
    description: Mock.mock('@csentence(20, 50)'),
    category: Mock.mock({ 'value|1': categories }).value,
    status: Mock.mock({ 'value|1': statusList }).value,
    version: 1,
    nodes,
    edges,
    createdBy: Mock.mock('@id'),
    createdAt: Mock.mock('@datetime'),
    updatedAt: Mock.mock('@datetime'),
    publishedAt: undefined,
    stats: generateWorkflowStats(),
    ...overrides,
  };
}

/**
 * 生成工作流列表项
 */
export function generateWorkflowListItem(): WorkflowListItem {
  const categories = ['customer-service', 'hr', 'operation', 'content', 'admin', 'data'];

  return {
    id: Mock.mock('@id'),
    name: Mock.mock('@ctitle(4, 12)'),
    description: Mock.mock('@csentence(10, 30)'),
    category: Mock.mock({ 'value|1': categories }).value,
    status: mockWorkflowStatus(),
    version: Mock.mock('@integer(1, 5)'),
    createdAt: Mock.mock('@datetime'),
    updatedAt: Mock.mock('@datetime'),
    stats: generateWorkflowStats(),
  };
}

/**
 * 生成执行记录
 */
export function generateExecution(workflowId: string): Execution {
  const statuses: Execution['status'][] = ['success', 'success', 'success', 'failed', 'running'];

  return {
    id: Mock.mock('@id'),
    workflowId,
    workflowName: Mock.mock('@ctitle(4, 10)'),
    status: Mock.mock({ 'value|1': statuses }).value,
    trigger: Mock.mock({ 'value|1': ['manual', 'webhook', 'schedule'] }).value,
    startTime: Mock.mock('@datetime'),
    endTime: Mock.mock('@datetime'),
    duration: Mock.mock('@integer(500, 10000)'),
    cost: Mock.mock('@float(0, 10, 2)'),
    nodes: generateNodeExecutions(),
    error: undefined,
  };
}

/**
 * 生成节点执行记录
 */
export function generateNodeExecutions(): NodeExecution[] {
  const types = ['llm', 'condition', 'http', 'dingtalk'];
  const statuses: NodeExecution['status'][] = ['success', 'success', 'failed', 'running'];
  const count = Mock.mock('@integer(2, 5)');

  return Array.from({ length: count }, (_, i) => ({
    nodeId: Mock.mock('@id'),
    nodeName: `${Mock.mock('@cword(2, 4)')}`,
    nodeType: Mock.mock({ 'value|1': types }).value,
    status: i === count - 1 ? 'running' : Mock.mock({ 'value|1': statuses }).value,
    startTime: Mock.mock('@datetime'),
    endTime: Mock.mock('@datetime'),
    duration: Mock.mock('@integer(100, 2000)'),
    input: { text: '测试输入' },
    output: { result: '处理结果' },
  }));
}

/**
 * 生成执行日志
 */
export function generateExecutionLog(): ExecutionLog {
  const levels: ExecutionLog['level'][] = ['info', 'success', 'success', 'success', 'warning', 'error'];
  const messages = [
    '工作流开始执行',
    'Webhook 触发器：接收到数据',
    'AI 问题分类：开始处理',
    'AI 问题分类：完成',
    '条件判断：高优先级 → 人工处理',
    'VIP 人工处理：等待人工处理中',
    'HTTP 请求：发送成功',
    '钉钉通知：发送成功',
  ];

  return {
    id: Mock.mock('@id'),
    timestamp: Mock.mock('@datetime'),
    level: Mock.mock({ 'value|1': levels }).value,
    nodeId: Mock.mock('@id'),
    nodeName: Mock.mock('@cword(2, 4)'),
    message: Mock.mock({ 'value|1': messages }).value,
    details: {},
  };
}

/**
 * 模拟工作流数据
 */
export function mockWorkflowData() {
  const workflowList = Mock.mock({
    'items|20': [generateWorkflowListItem()],
    total: 50,
    page: 1,
    pageSize: 20,
    totalPages: 3,
  });

  const workflowDetail = generateWorkflow();

  const executionLogs = Mock.mock({
    'items|50': [generateExecutionLog()],
  });

  return {
    workflowList,
    workflowDetail,
    executionLogs,
  };
}
```

---

## 三、工作流 API Mock

### 3.1 工作流 CRUD

```typescript
// src/api/mock/workflow.ts
import Mock from 'better-mock';
import { mockResponse, mockDelay, mockRelativeTime, mockPaginatedData } from './utils';
import {
  generateWorkflow,
  generateWorkflowListItem,
  generateWorkflowStats,
} from './generators';

// 存储工作流数据（模拟数据库）
let workflowDatabase: ReturnType<typeof generateWorkflow>[] = [];
let workflowListDatabase: ReturnType<typeof generateWorkflowListItem>[] = [];

// 初始化数据
function initWorkflowData() {
  if (workflowListDatabase.length > 0) return;

  // 生成初始工作流列表
  const categories = ['customer-service', 'hr', 'operation', 'content', 'admin', 'data'];
  const statuses = ['draft', 'running', 'running', 'running', 'stopped'];

  workflowListDatabase = Mock.mock({
    [`list|${30}`]: [
      {
        id: '@id',
        name: () => Mock.Random.ctitle(4, 12),
        description: () => Mock.Random.csentence(10, 30),
        category: () => Mock.Random.pick(categories),
        status: () => Mock.Random.pick(statuses),
        version: () => Mock.Random.integer(1, 5),
        createdAt: () => Mock.Random.datetime(),
        updatedAt: () => Mock.Random.datetime(),
        stats: generateWorkflowStats(),
      },
    ],
  }).list;
}

initWorkflowData();

export default [
  // ==================== 工作流列表 ====================
  {
    url: '/api/workflows',
    method: 'get',
    response: ({ query }: any) => {
      const { page = 1, pageSize = 20, category, status, keyword } = query || {};

      let result = [...workflowListDatabase];

      // 筛选
      if (category) {
        result = result.filter((w) => w.category === category);
      }
      if (status) {
        result = result.filter((w) => w.status === status);
      }
      if (keyword) {
        const kw = keyword.toLowerCase();
        result = result.filter(
          (w) =>
            w.name.toLowerCase().includes(kw) ||
            w.description?.toLowerCase().includes(kw)
        );
      }

      return mockResponse(mockPaginatedData(result, page, pageSize));
    },
    timeout: mockDelay(),
  },

  // ==================== 所有工作流（不分页） ====================
  {
    url: '/api/workflows/all',
    method: 'get',
    response: () => {
      return mockResponse(workflowListDatabase);
    },
  },

  // ==================== 工作流详情 ====================
  {
    url: '/api/workflows/:id',
    method: 'get',
    response: ({ params }: any) => {
      const workflow = workflowListDatabase.find((w) => w.id === params.id);

      if (!workflow) {
        return {
          code: 404,
          message: '工作流不存在',
          data: null,
        };
      }

      return mockResponse(generateWorkflow({ id: workflow.id, name: workflow.name }));
    },
  },

  // ==================== 创建工作流 ====================
  {
    url: '/api/workflows',
    method: 'post',
    response: ({ body }: any) => {
      const newWorkflow = generateWorkflow({
        id: Mock.mock('@id'),
        name: body.name,
        description: body.description,
        category: body.category,
        status: 'draft',
        version: 1,
        nodes: [
          generateWorkflow({}).nodes[0],
        ],
        edges: [],
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      });

      workflowListDatabase.unshift({
        id: newWorkflow.id,
        name: newWorkflow.name,
        description: newWorkflow.description,
        category: newWorkflow.category,
        status: 'draft',
        version: 1,
        createdAt: newWorkflow.createdAt,
        updatedAt: newWorkflow.updatedAt,
        stats: generateWorkflowStats(),
      });

      return mockResponse(newWorkflow);
    },
    timeout: mockDelay(),
  },

  // ==================== 更新工作流 ====================
  {
    url: '/api/workflows/:id',
    method: 'put',
    response: ({ params, body }: any) => {
      const index = workflowListDatabase.findIndex((w) => w.id === params.id);

      if (index === -1) {
        return {
          code: 404,
          message: '工作流不存在',
        };
      }

      const updated = {
        ...workflowListDatabase[index],
        ...body,
        updatedAt: new Date().toISOString(),
      };

      workflowListDatabase[index] = updated;

      return mockResponse(generateWorkflow({ ...updated, nodes: body.nodes || [], edges: body.edges || [] }));
    },
    timeout: mockDelay(),
  },

  // ==================== 删除工作流 ====================
  {
    url: '/api/workflows/:id',
    method: 'delete',
    response: ({ params }: any) => {
      const index = workflowListDatabase.findIndex((w) => w.id === params.id);

      if (index === -1) {
        return {
          code: 404,
          message: '工作流不存在',
        };
      }

      workflowListDatabase.splice(index, 1);

      return mockResponse(null);
    },
  },

  // ==================== 发布工作流 ====================
  {
    url: '/api/workflows/:id/publish',
    method: 'post',
    response: ({ params }: any) => {
      const index = workflowListDatabase.findIndex((w) => w.id === params.id);

      if (index !== -1) {
        workflowListDatabase[index].status = 'running';
      }

      return mockResponse({ success: true });
    },
    timeout: mockDelay(200, 800),
  },

  // ==================== 执行工作流 ====================
  {
    url: '/api/workflows/:id/execute',
    method: 'post',
    response: ({ params }: any) => {
      const execution = {
        id: Mock.mock('@id'),
        workflowId: params.id,
        workflowName: workflowListDatabase.find((w) => w.id === params.id)?.name || '未命名',
        status: 'running',
        trigger: 'manual',
        startTime: new Date().toISOString(),
        duration: 0,
        cost: 0,
        nodes: [],
      };

      return mockResponse(execution);
    },
    timeout: mockDelay(500, 1500),
  },

  // ==================== 停止执行 ====================
  {
    url: '/api/workflows/:id/stop',
    method: 'post',
    response: () => {
      return mockResponse({ success: true });
    },
  },

  // ==================== 获取收藏 ====================
  {
    url: '/api/workflows/favorites',
    method: 'get',
    response: () => {
      const favorites = workflowListDatabase.slice(0, 5);
      return mockResponse(favorites);
    },
  },

  // ==================== 复制工作流 ====================
  {
    url: '/api/workflows/:id/duplicate',
    method: 'post',
    response: ({ params, body }: any) => {
      const original = workflowListDatabase.find((w) => w.id === params.id);

      if (!original) {
        return { code: 404, message: '工作流不存在' };
      }

      const duplicated = generateWorkflow({
        name: body?.newName || `${original.name} (副本)`,
        description: original.description,
        category: original.category,
      });

      workflowListDatabase.unshift({
        ...duplicated,
        id: duplicated.id,
        name: duplicated.name,
      });

      return mockResponse(duplicated);
    },
  },
];
```

### 3.2 执行日志 Mock

```typescript
// src/api/mock/execution.ts
import Mock from 'better-mock';
import { mockResponse, mockDelay } from './utils';
import { generateExecution, generateExecutionLog } from './generators';

// 执行记录存储
let executionDatabase: ReturnType<typeof generateExecution>[] = [];

function initExecutionData(workflowId: string) {
  if (executionDatabase.length > 0) return;

  executionDatabase = Mock.mock({
    [`list|${50}`]: [generateExecution(workflowId)],
  }).list;
}

export default [
  // ==================== 执行记录列表 ====================
  {
    url: '/api/executions',
    method: 'get',
    response: ({ query }: any) => {
      const { workflowId, status, page = 1, pageSize = 20 } = query || {};

      let result = [...executionDatabase];

      if (workflowId) {
        result = result.filter((e) => e.workflowId === workflowId);
      }
      if (status) {
        result = result.filter((e) => e.status === status);
      }

      const start = (page - 1) * pageSize;
      const items = result.slice(start, start + pageSize);

      return mockResponse({
        items,
        total: result.length,
        page,
        pageSize,
        totalPages: Math.ceil(result.length / pageSize),
      });
    },
    timeout: mockDelay(),
  },

  // ==================== 执行详情 ====================
  {
    url: '/api/executions/:id',
    method: 'get',
    response: ({ params }: any) => {
      const execution = executionDatabase.find((e) => e.id === params.id);

      if (!execution) {
        return {
          code: 404,
          message: '执行记录不存在',
        };
      }

      return mockResponse(execution);
    },
  },

  // ==================== 执行日志 ====================
  {
    url: '/api/executions/:executionId/logs',
    method: 'get',
    response: ({ params, query }: any) => {
      const logs = Mock.mock({
        [`items|${30}`]: [
          {
            id: '@id',
            timestamp: () => Mock.Random.datetime('HH:mm:ss'),
            'level|1': ['info', 'success', 'success', 'warning'],
            nodeId: '@id',
            nodeName: () => Mock.Random.cword(2, 4),
            message: () => Mock.Random.pick([
              '工作流开始执行',
              'Webhook 触发器：接收到数据',
              'AI 问题分类：开始处理',
              'AI 问题分类：完成',
              '条件判断：高优先级',
              'VIP 人工处理：等待处理中',
              'HTTP 请求：发送成功',
              '钉钉通知：发送成功',
              '工作流执行完成',
            ]),
            details: {},
          },
        ],
      }).items;

      return mockResponse(logs);
    },
  },

  // ==================== 重新执行 ====================
  {
    url: '/api/executions/:id/retry',
    method: 'post',
    response: ({ params }: any) => {
      const newExecution = generateExecution(params.id);
      newExecution.status = 'running';
      newExecution.startTime = new Date().toISOString();

      return mockResponse(newExecution);
    },
    timeout: mockDelay(500, 1000),
  },

  // ==================== 取消执行 ====================
  {
    url: '/api/executions/:id/cancel',
    method: 'post',
    response: () => {
      return mockResponse({ success: true });
    },
  },
];
```

---

## 四、模板 API Mock

```typescript
// src/api/mock/template.ts
import Mock from 'better-mock';
import { mockResponse, mockDelay } from './utils';
import { generateWorkflow } from './generators';

const TEMPLATE_CATEGORIES = [
  { name: '客服', count: 12 },
  { name: 'HR', count: 8 },
  { name: '运营', count: 15 },
  { name: '内容', count: 10 },
  { name: '行政', count: 6 },
  { name: '数据', count: 9 },
];

const TEMPLATE_TAGS = [
  'AI', '自动化', '智能', '批量', '定时',
  'Webhook', '审批', '通知', '报表', '采集'
];

export default [
  // ==================== 模板列表 ====================
  {
    url: '/api/templates',
    method: 'get',
    response: ({ query }: any) => {
      const { category, keyword, sort = 'popular', page = 1, pageSize = 12 } = query || {};

      const templates = Mock.mock({
        [`items|${30}`]: [
          {
            id: '@id',
            name: () => Mock.Random.ctitle(6, 14),
            description: () => Mock.Random.csentence(15, 40),
            'category|1': ['customer-service', 'hr', 'operation', 'content'],
            'tags|2-4': [() => Mock.Random.pick(TEMPLATE_TAGS)],
            coverImage: () => Mock.Random.image('280x160', Mock.Random.color(), Mock.Random.cword(2)),
            author: () => Mock.Random.cname(),
            usageCount: () => Mock.Random.integer(100, 50000),
            rating: () => Mock.Random.float(4, 5, 1, 1),
            workflow: () => generateWorkflow(),
            createdAt: () => Mock.Random.datetime(),
            updatedAt: () => Mock.Random.datetime(),
            isOfficial: () => Mock.Random.boolean(),
          },
        ],
      }).items;

      // 筛选
      let result = [...templates];

      if (category) {
        result = result.filter((t) => t.category === category);
      }

      if (keyword) {
        const kw = keyword.toLowerCase();
        result = result.filter(
          (t) =>
            t.name.toLowerCase().includes(kw) ||
            t.description.toLowerCase().includes(kw)
        );
      }

      // 排序
      if (sort === 'popular') {
        result.sort((a, b) => b.usageCount - a.usageCount);
      } else if (sort === 'rating') {
        result.sort((a, b) => b.rating - a.rating);
      }

      return mockResponse({
        items: result.slice((page - 1) * pageSize, page * pageSize),
        total: result.length,
      });
    },
    timeout: mockDelay(),
  },

  // ==================== 模板详情 ====================
  {
    url: '/api/templates/:id',
    method: 'get',
    response: () => {
      const template = {
        id: Mock.mock('@id'),
        name: Mock.Random.ctitle(6, 14),
        description: Mock.Random.csentence(15, 40),
        category: 'customer-service',
        tags: [Mock.Random.pick(TEMPLATE_TAGS), 'AI'],
        coverImage: Mock.Random.image('280x160', Mock.Random.color(), 'Template'),
        author: Mock.Random.cname(),
        usageCount: Mock.Random.integer(1000, 50000),
        rating: Mock.Random.float(4, 5, 1, 1),
        workflow: generateWorkflow(),
        createdAt: Mock.Random.datetime(),
        updatedAt: Mock.Random.datetime(),
        isOfficial: true,
      };

      return mockResponse(template);
    },
  },

  // ==================== 使用模板创建 ====================
  {
    url: '/api/templates/:id/use',
    method: 'post',
    response: ({ body }: any) => {
      const workflow = generateWorkflow({
        name: body?.name || '新建工作流',
        status: 'draft',
      });

      return mockResponse(workflow);
    },
    timeout: mockDelay(500, 1000),
  },

  // ==================== 官方模板 ====================
  {
    url: '/api/templates/official',
    method: 'get',
    response: () => {
      const templates = Mock.mock({
        'items|6': [
          {
            id: '@id',
            name: () => Mock.Random.ctitle(6, 14),
            description: () => Mock.Random.csentence(15, 40),
            category: 'customer-service',
            'tags|2': ['AI', '自动化'],
            coverImage: () => Mock.Random.image('280x160', Mock.Random.color(), 'Official'),
            usageCount: () => Mock.Random.integer(5000, 50000),
            rating: () => Mock.Random.float(4.5, 5, 1, 1),
            isOfficial: true,
          },
        ],
      }).items;

      return mockResponse(templates);
    },
  },

  // ==================== 热门模板 ====================
  {
    url: '/api/templates/popular',
    method: 'get',
    response: ({ query }: any) => {
      const limit = query?.limit || 10;

      const templates = Mock.mock({
        [`items|${limit}`]: [
          {
            id: '@id',
            name: () => Mock.Random.ctitle(6, 14),
            description: () => Mock.Random.csentence(15, 40),
            'category|1': ['customer-service', 'hr', 'operation'],
            'tags|2': ['AI', '自动化'],
            coverImage: () => Mock.Random.image('280x160', Mock.Random.color()),
            usageCount: () => Mock.Random.integer(5000, 100000),
            rating: () => Mock.Random.float(4, 5, 1, 1),
          },
        ],
      }).items;

      templates.sort((a, b) => b.usageCount - a.usageCount);

      return mockResponse(templates);
    },
  },

  // ==================== 最新模板 ====================
  {
    url: '/api/templates/recent',
    method: 'get',
    response: ({ query }: any) => {
      const limit = query?.limit || 10;

      return mockResponse(
        Mock.mock({
          [`items|${limit}`]: [
            {
              id: '@id',
              name: () => Mock.Random.ctitle(6, 14),
              description: () => Mock.Random.csentence(15, 40),
              category: 'operation',
              'tags|2': ['AI', '智能'],
              coverImage: () => Mock.Random.image('280x160', Mock.Random.color()),
              createdAt: () => Mock.Random.datetime(),
              rating: () => Mock.Random.float(4, 5, 1, 1),
            },
          ],
        }).items
      );
    },
  },

  // ==================== 分类列表 ====================
  {
    url: '/api/templates/categories',
    method: 'get',
    response: () => {
      return mockResponse(TEMPLATE_CATEGORIES);
    },
  },

  // ==================== 标签列表 ====================
  {
    url: '/api/templates/tags',
    method: 'get',
    response: () => {
      return mockResponse(TEMPLATE_TAGS);
    },
  },
];
```

---

## 五、统计 API Mock

```typescript
// src/api/mock/statistics.ts
import Mock from 'better-mock';
import { mockResponse, mockDelay } from './utils';

export default [
  // ==================== 看板数据 ====================
  {
    url: '/api/statistics/dashboard',
    method: 'get',
    response: () => {
      const today = {
        executions: Mock.Random.integer(500, 2000),
        successRate: Mock.Random.float(95, 99.9, 2, 2),
        avgDuration: Mock.Random.integer(500, 2000),
        cost: Mock.Random.float(10, 100, 2, 2),
      };

      // 生成7天趋势数据
      const trend = Array.from({ length: 7 }, (_, i) => {
        const date = new Date();
        date.setDate(date.getDate() - (6 - i));

        return {
          date: date.toISOString().split('T')[0],
          executions: Mock.Random.integer(500, 2000),
          successRate: Mock.Random.float(95, 99.9, 2, 2),
          cost: Mock.Random.float(10, 100, 2, 2),
        };
      });

      // Top 工作流
      const topWorkflows = Mock.mock({
        'items|5': [
          {
            id: '@id',
            name: () => Mock.Random.ctitle(4, 8),
            executions: () => Mock.Random.integer(1000, 10000),
          },
        ],
      }).items;

      return mockResponse({ today, trend, topWorkflows });
    },
    timeout: mockDelay(),
  },

  // ==================== 执行统计 ====================
  {
    url: '/api/statistics/executions',
    method: 'get',
    response: ({ query }: any) => {
      const { granularity = 'day' } = query || {};

      let days = 7;
      if (granularity === 'hour') days = 24;
      if (granularity === 'week') days = 12;
      if (granularity === 'month') days = 30;

      const data = Array.from({ length: days }, (_, i) => {
        const date = new Date();
        if (granularity === 'hour') {
          date.setHours(date.getHours() - (23 - i));
        } else {
          date.setDate(date.getDate() - (days - 1 - i));
        }

        return {
          date: granularity === 'hour'
            ? date.toISOString().split('T')[1].slice(0, 5)
            : date.toISOString().split('T')[0],
          executions: Mock.Random.integer(100, 1000),
          successRate: Mock.Random.float(90, 100, 2, 2),
          avgDuration: Mock.Random.integer(500, 3000),
        };
      });

      return mockResponse(data);
    },
  },

  // ==================== 成本统计 ====================
  {
    url: '/api/statistics/costs',
    method: 'get',
    response: () => {
      const total = Mock.Random.float(1000, 10000, 2, 2);

      const byWorkflow = Mock.mock({
        'items|5': [
          {
            workflowId: '@id',
            workflowName: () => Mock.Random.ctitle(4, 8),
            cost: () => Mock.Random.float(100, 1000, 2, 2),
            percentage: () => Mock.Random.float(10, 40, 1, 1),
          },
        ],
      }).items;

      const byModel = Mock.mock({
        'items|3': [
          {
            model: () => Mock.Random.pick(['GPT-4o', 'Claude 3.5', '通义千问']),
            calls: () => Mock.Random.integer(1000, 10000),
            cost: () => Mock.Random.float(100, 1000, 2, 2),
          },
        ],
      }).items;

      const trend = Array.from({ length: 7 }, (_, i) => {
        const date = new Date();
        date.setDate(date.getDate() - (6 - i));

        return {
          date: date.toISOString().split('T')[0],
          cost: Mock.Random.float(50, 200, 2, 2),
        };
      });

      return mockResponse({ total, byWorkflow, byModel, trend });
    },
  },

  // ==================== 导出统计 ====================
  {
    url: '/api/statistics/export',
    method: 'get',
    response: () => {
      // 返回一个空的 Blob（实际会是 CSV/Excel）
      return new Blob(['模拟导出数据'], { type: 'text/csv' });
    },
  },
];
```

---

## 六、认证 API Mock

```typescript
// src/api/mock/auth.ts
import Mock from 'better-mock';
import { mockResponse, mockDelay } from './utils';

export default [
  // ==================== 登录 ====================
  {
    url: '/api/auth/login',
    method: 'post',
    response: ({ body }: any) => {
      const { username, password } = body || {};

      if (!username || !password) {
        return {
          code: 400,
          message: '用户名和密码不能为空',
        };
      }

      return mockResponse({
        accessToken: Mock.mock('@guid'),
        refreshToken: Mock.mock('@guid'),
        expiresIn: 7200,
        user: {
          id: Mock.mock('@id'),
          username: username,
          email: `${username}@example.com`,
          avatar: Mock.Random.image('80x80', Mock.Random.color(), 'User'),
          role: username === 'admin' ? 'admin' : 'user',
          organization: {
            id: Mock.mock('@id'),
            name: '示例公司',
          },
        },
      });
    },
    timeout: mockDelay(300, 800),
  },

  // ==================== 注册 ====================
  {
    url: '/api/auth/register',
    method: 'post',
    response: ({ body }: any) => {
      return mockResponse({
        accessToken: Mock.mock('@guid'),
        refreshToken: Mock.mock('@guid'),
        expiresIn: 7200,
        user: {
          id: Mock.mock('@id'),
          username: body?.username,
          email: body?.email,
          avatar: undefined,
          role: 'user',
        },
      });
    },
    timeout: mockDelay(500, 1000),
  },

  // ==================== 获取当前用户 ====================
  {
    url: '/api/auth/me',
    method: 'get',
    response: () => {
      return mockResponse({
        id: Mock.mock('@id'),
        username: 'demo_user',
        email: 'demo@example.com',
        avatar: Mock.Random.image('80x80', '#6366f1', 'Demo'),
        role: 'user',
        organization: {
          id: Mock.mock('@id'),
          name: '示例公司',
        },
      });
    },
  },

  // ==================== 更新用户信息 ====================
  {
    url: '/api/auth/me',
    method: 'put',
    response: ({ body }: any) => {
      return mockResponse({
        id: Mock.mock('@id'),
        username: body?.username || 'demo_user',
        email: body?.email || 'demo@example.com',
        avatar: body?.avatar,
        role: 'user',
      });
    },
    timeout: mockDelay(200, 500),
  },

  // ==================== 刷新 Token ====================
  {
    url: '/api/auth/refresh',
    method: 'post',
    response: () => {
      return mockResponse({
        accessToken: Mock.mock('@guid'),
      });
    },
  },

  // ==================== 登出 ====================
  {
    url: '/api/auth/logout',
    method: 'post',
    response: () => {
      return mockResponse(null);
    },
  },

  // ==================== 修改密码 ====================
  {
    url: '/api/auth/change-password',
    method: 'post',
    response: () => {
      return mockResponse({ success: true });
    },
    timeout: mockDelay(300, 600),
  },
];
```

---

## 七、Mock 入口文件

```typescript
// src/api/mock/index.ts
// 导出所有 Mock 模块

export { default as workflowMock } from './workflow';
export { default as executionMock } from './execution';
export { default as templateMock } from './template';
export { default as statisticsMock } from './statistics';
export { default as authMock } from './auth';
```

### Vite 配置中使用

```typescript
// vite.config.ts
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { viteMockServe } from 'vite-plugin-mock';
import path from 'path';

export default defineConfig({
  plugins: [
    vue(),
    viteMockServe({
      mockPath: './src/api/mock',
      enable: import.meta.env.VITE_USE_MOCK === 'true',
      watchFiles: true,
      // 忽略这些文件
      ignore: ['**/*.ts', '**/*.md', '**/*.json'],
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
});
```

### 在开发环境自动启用

```typescript
// src/main.ts
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import App from './App.vue';
import router from './router';

// Mock（仅开发环境）
if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true') {
  import('./api/mock').then(({ workflowMock, executionMock, templateMock, statisticsMock, authMock }) => {
    console.log('Mock 数据已启用');
  });
}

const app = createApp(App);

app.use(createPinia());
app.use(router);
app.use(ElementPlus);

app.mount('#app');
```

---

## 八、Mock 数据使用示例

### 8.1 在组件中使用

```vue
<!-- src/views/workflow/WorkflowList.vue -->
<template>
  <div class="workflow-list">
    <el-spinner v-if="loading" />

    <div v-else class="workflow-grid">
      <WorkflowCard
        v-for="workflow in workflows"
        :key="workflow.id"
        :workflow="workflow"
        @click="handleClick(workflow)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { workflowApi } from '@/api/workflow';
import type { WorkflowListItem } from '@/types/workflow';

const loading = ref(true);
const workflows = ref<WorkflowListItem[]>([]);

onMounted(async () => {
  try {
    // 自动使用 Mock 数据（开发环境）
    const result = await workflowApi.getList({ page: 1, pageSize: 20 });
    workflows.value = result.items;
  } catch (error) {
    console.error('获取工作流列表失败', error);
  } finally {
    loading.value = false;
  }
});

function handleClick(workflow: WorkflowListItem) {
  // 跳转到编辑器
}
</script>
```

### 8.2 在测试中使用

```typescript
// src/api/__tests__/workflow.test.ts
import { workflowApi } from '../workflow';

// Mock 模拟
vi.mock('../request', () => ({
  default: {
    get: vi.fn().mockResolvedValue({
      code: 200,
      data: {
        items: [{ id: '1', name: '测试工作流' }],
        total: 1,
      },
    }),
    post: vi.fn().mockResolvedValue({
      code: 200,
      data: { id: '2', name: '新建工作流' },
    }),
  },
}));

describe('工作流 API', () => {
  it('获取工作流列表', async () => {
    const result = await workflowApi.getList({ page: 1, pageSize: 10 });
    expect(result.items).toHaveLength(1);
  });

  it('创建工作流', async () => {
    const result = await workflowApi.create({
      name: '新建工作流',
      category: 'customer-service',
    });
    expect(result.name).toBe('新建工作流');
  });
});
```

---

**文档版本**: v1.0
**更新日期**: 2026-07-10
