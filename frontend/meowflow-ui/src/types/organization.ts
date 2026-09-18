export interface OrgTree {
  id: string
  parentId: string
  name: string
  code?: string
  leader?: string
  phone?: string
  email?: string
  sort?: number
  status?: string
  children?: OrgTree[]
}

export interface OrgCreateRequest {
  name: string
  parentId?: string
  code?: string
  leader?: string
  phone?: string
  email?: string
  sort?: number
  remark?: string
}
