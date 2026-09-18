/**
 * 前端用户类型
 *
 * 与后端 UserDTO 对齐：
 * - 字段名统一：后端 nickName / 前端 nickname（统一用 nickname）
 * - id: 后端 Long（JSON 数字），前端统一为 string
 * - roles: 后端 Set<String> → 前端 string[]
 * - permissions: 后端 Set<String> → 前端 string[]
 */

/** 用户信息（Pinia store 单一真相） */
export interface UserInfo {
  id: string;
  username: string;
  /** 后端 nickName 统一为 nickname */
  nickname: string;
  email?: string;
  avatar?: string;
  role?: string;
  /** 角色 code 列表（从后端 roles 列表取第一个作为 role） */
  roles?: string[];
  /** 权限字符串列表（从后端 permissions 集合获取） */
  permissions?: string[];
  /** 当前所属组织 id（个人组织） */
  orgId?: number;
  /** 用户所拥有的所有组织 id 集合（个人 + 加入的团队） */
  orgIds?: number[];
  organization?: { id: string; name: string };
}

/** 硬编码权限列表（仅用于 Mock 或无权限 API 的兜底） */
export const GRANTED_PERMISSIONS = [
  'workflow:view',
  'workflow:edit',
  'workflow:run',
  'workflow:publish',
  'template:view',
  'template:use',
  'log:view',
  'dashboard:view',
  'profile:view',
  'profile:edit',
  'team:view',
  'team:manage',
  'settings:view',
  'settings:edit',
];
