import http from './http'
import { serviceUrl } from './endpoints'
import type { OrgTree, OrgCreateRequest } from '@/types/organization'

export const organizationApi = {
  /** GET /user/api/v1/orgs/tree — 获取组织树 */
  getOrgTree(): Promise<OrgTree[]> {
    return http.get<OrgTree>(serviceUrl('user', '/api/v1/orgs/tree')).then((r: any) => r.data)
  },

  /** POST /user/api/v1/orgs — 创建组织 */
  createOrg(payload: OrgCreateRequest): Promise<OrgTree> {
    return http.post<OrgTree>(serviceUrl('user', '/api/v1/orgs'), payload).then((r: any) => r.data)
  },

  /** GET /user/api/v1/orgs — 分页查询组织 */
  pageOrgs(params: {
    keyword?: string
    status?: string
    pageNum?: number
    pageSize?: number
  }): Promise<any> {
    return http.get(serviceUrl('user', '/api/v1/orgs'), { params }).then((r: any) => r.data)
  },

  /** PUT /user/api/v1/orgs/{id} — 更新组织 */
  updateOrg(id: string, payload: Partial<OrgCreateRequest>): Promise<OrgTree> {
    return http.put<OrgTree>(serviceUrl('user', `/api/v1/orgs/${id}`), payload).then((r: any) => r.data)
  },

  /** DELETE /user/api/v1/orgs/{id} — 删除组织 */
  deleteOrg(id: string): Promise<void> {
    return http.delete(serviceUrl('user', `/api/v1/orgs/${id}`)).then((r: any) => r.data)
  },

  /** GET /user/api/v1/orgs/{id} — 获取组织详情 */
  getOrgById(id: string): Promise<OrgTree> {
    return http.get<OrgTree>(serviceUrl('user', `/api/v1/orgs/${id}`)).then((r: any) => r.data)
  },
}
