import type { TeamMember } from '@/api/team';

export const MOCK_TEAM_MEMBERS: TeamMember[] = [
  { id: '1', username: 'li.ops', nickname: '小猪大白', displayName: '小猪大白', role: 'admin', email: 'li.ops@meowflow.com', status: 'active', joinedAt: '2025-09-01', permissions: ['all'] },
  { id: '2', username: 'wang.dev', nickname: '王工程师', displayName: '王工程师', role: 'user', email: 'wang.dev@meowflow.com', status: 'active', joinedAt: '2025-09-12', permissions: ['workflow.edit'] },
  { id: '3', username: 'chen.cs', nickname: '陈客服', displayName: '陈客服', role: 'user', email: 'chen.cs@meowflow.com', status: 'active', joinedAt: '2025-10-10', permissions: ['workflow.view'] },
  { id: '4', username: 'liu.hr', nickname: '刘主管', displayName: '刘主管', role: 'admin', email: 'liu.hr@meowflow.com', status: 'active', joinedAt: '2025-08-22', permissions: ['all'] },
  { id: '5', username: 'sun.pm', nickname: '孙产品', displayName: '孙产品', role: 'user', email: 'sun.pm@meowflow.com', status: 'active', joinedAt: '2026-01-15', permissions: ['workflow.view'] },
  { id: '6', username: 'zhou', nickname: '周离职', displayName: '周离职', role: 'user', email: 'zhou@meowflow.com', status: 'disabled', joinedAt: '2025-06-03', permissions: [] },
];
