import type { OrgTree } from '@/types/organization'

export const MOCK_ORGS: OrgTree[] = [
  {
    id: 'org-001',
    name: '示例科技有限公司',
    parentId: '0',
    code: 'MFC',
    leader: '小猪大白',
    status: '1',
    children: [
      {
        id: 'org-002',
        name: '技术研发部',
        parentId: 'org-001',
        code: 'TECH',
        leader: '王工程师',
        status: '1',
      },
      {
        id: 'org-003',
        name: '产品运营部',
        parentId: 'org-001',
        code: 'PROD',
        leader: '孙产品',
        status: '1',
      },
      {
        id: 'org-004',
        name: '客户服务部',
        parentId: 'org-001',
        code: 'CS',
        leader: '陈客服',
        status: '1',
      },
    ],
  },
]

// 内存中的组织列表（支持增删改）
let orgList: OrgTree[] = JSON.parse(JSON.stringify(MOCK_ORGS))

export function getMockOrgs(): OrgTree[] {
  return orgList
}

export function addMockOrg(org: OrgTree): void {
  orgList.push(org)
}

export function updateMockOrg(id: string, updates: Partial<OrgTree>): OrgTree | null {
  const idx = orgList.findIndex((o) => o.id === id)
  if (idx !== -1) {
    orgList[idx] = { ...orgList[idx], ...updates }
    return orgList[idx]
  }
  return null
}

export function deleteMockOrg(id: string): boolean {
  const idx = orgList.findIndex((o) => o.id === id)
  if (idx !== -1) {
    orgList.splice(idx, 1)
    return true
  }
  return false
}

export function resetMockOrgs(): void {
  orgList = JSON.parse(JSON.stringify(MOCK_ORGS))
}
