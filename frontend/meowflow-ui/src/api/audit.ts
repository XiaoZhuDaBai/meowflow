import http from './http';
import { serviceUrl } from './endpoints';

export interface AuditLog {
  id: number | string;
  username?: string;
  module?: string;
  method?: string;
  requestMethod?: string;
  requestUrl?: string;
  requestParams?: string;
  responseResult?: string;
  ip?: string;
  costTime?: number;
  status?: string;
  errorMsg?: string;
  operateTime?: string;
}

export interface AuditLogQuery {
  username?: string;
  module?: string;
  status?: string;
  startTime?: string;
  endTime?: string;
  pageNum?: number;
  pageSize?: number;
}

export const auditApi = {
  page(params: AuditLogQuery): Promise<{ records: AuditLog[]; total: number; current: number; size: number }> {
    return http
      .get(serviceUrl('user', '/api/v1/audit-logs'), { params })
      .then((r: any) => r.data);
  },

  getById(id: number | string): Promise<AuditLog> {
    return http
      .get(serviceUrl('user', `/api/v1/audit-logs/${id}`))
      .then((r: any) => r.data);
  },
};
