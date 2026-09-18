import type { UserInfo } from '@/types/user';

export const MOCK_USER: UserInfo = {
  id: 'u-001',
  username: 'li.ops',
  nickname: '小猪大白',
  email: 'li.ops@meowflow.com',
  role: 'admin',
  roles: ['admin'],
  organization: { id: 'org-001', name: '示例科技有限公司' },
  orgId: 1,
  orgIds: [1, 2],
  permissions: [
    'dashboard',
    'workflow',
    'workflow:list',
    'workflow:editor',
    'template',
    'log',
    'statistics',
    'user',
    'team',
    'system',
    'system:model',
    'system:integration',
    'system:alert',
  ],
};