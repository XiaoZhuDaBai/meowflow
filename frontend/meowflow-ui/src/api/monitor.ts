import http from './http';
import { serviceUrl } from './endpoints';

export interface AlertRule {
  id: number | string;
  code?: string;
  name: string;
  description?: string;
  metricName: string;
  conditionType?: string;
  thresholdValue?: number;
  timeWindowSeconds?: number;
  notificationChannels?: string;
  enabled?: boolean;
  severity?: string;
  createTime?: string;
}

export interface AlertRulePayload {
  name: string;
  description?: string;
  metricName: string;
  operator: string;
  threshold: number;
  duration?: number;
  alertChannel?: string;
  webhook?: string;
  status?: string;
  remark?: string;
}

export interface AlertRecord {
  id: number | string;
  ruleId?: number | string;
  ruleName?: string;
  metricType?: string;
  metricName?: string;
  currentValue?: number;
  threshold?: number;
  operator?: string;
  severity?: string;
  status: string;
  alertChannel?: string;
  alertMessage?: string;
  notifyStatus?: string;
  notifyCount?: number;
  notifyTime?: string;
  firedAt?: string;
  resolveTime?: string;
  resolveComment?: string;
  createTime?: string;
  /** 兼容旧接口字段 */
  alertRuleId?: number | string;
  title?: string;
  message?: string;
  triggerValue?: string;
  thresholdValue?: number;
  resolvedAt?: string;
  resolutionNote?: string;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
}

const base = '/api/monitor/alert';
const url = (path = '') => serviceUrl('monitor', `${base}${path}`);

function toNumber(value: unknown): number | undefined {
  if (value === null || value === undefined || value === '') return undefined;
  const n = Number(value);
  return Number.isFinite(n) ? n : undefined;
}

function normalizeSeverity(value: unknown): string | undefined {
  if (value === null || value === undefined || value === '') return undefined;
  if (typeof value === 'number') {
    return ({ 1: 'INFO', 2: 'WARNING', 3: 'ERROR', 4: 'CRITICAL' } as Record<number, string>)[value];
  }
  return String(value).toUpperCase();
}

function adaptAlertRecord(raw: any): AlertRecord {
  const value = raw?.currentValue ?? raw?.value ?? raw?.triggerValue;
  const threshold = raw?.threshold ?? raw?.thresholdValue;
  return {
    id: raw?.id,
    ruleId: raw?.ruleId ?? raw?.alertRuleId,
    ruleName: raw?.ruleName ?? raw?.title,
    metricType: raw?.metricType,
    metricName: raw?.metricName ?? raw?.metric,
    currentValue: toNumber(value),
    threshold: toNumber(threshold),
    operator: raw?.operator,
    severity: normalizeSeverity(raw?.severity),
    status: String(raw?.status ?? '').toUpperCase(),
    alertChannel: raw?.alertChannel,
    alertMessage: raw?.alertMessage ?? raw?.message,
    notifyStatus: raw?.notifyStatus,
    notifyCount: toNumber(raw?.notifyCount),
    notifyTime: raw?.notifyTime,
    firedAt: raw?.firedAt ?? raw?.triggeredAt,
    resolveTime: raw?.resolveTime ?? raw?.resolvedAt,
    resolveComment: raw?.resolveComment ?? raw?.resolutionNote,
    createTime: raw?.createTime,
    alertRuleId: raw?.alertRuleId ?? raw?.ruleId,
    title: raw?.title ?? raw?.ruleName,
    message: raw?.message ?? raw?.alertMessage,
    triggerValue: raw?.triggerValue ?? (value === undefined ? undefined : String(value)),
    thresholdValue: toNumber(threshold),
    resolvedAt: raw?.resolvedAt ?? raw?.resolveTime,
    resolutionNote: raw?.resolutionNote ?? raw?.resolveComment,
  };
}

export const monitorApi = {
  rules(params: { pageNum?: number; pageSize?: number } = {}): Promise<PageResult<AlertRule>> {
    return http.get(url('/rules'), { params }).then((r: any) => r.data);
  },

  createRule(payload: AlertRulePayload): Promise<AlertRule> {
    return http.post(url('/rule'), payload).then((r: any) => r.data);
  },

  updateRule(id: number | string, payload: AlertRulePayload): Promise<AlertRule> {
    return http.put(url(`/rule/${id}`), payload).then((r: any) => r.data);
  },

  deleteRule(id: number | string): Promise<void> {
    return http.delete(url(`/rule/${id}`)).then(() => undefined);
  },

  records(params: { status?: string; pageNum?: number; pageSize?: number } = {}): Promise<PageResult<AlertRecord>> {
    return http.get(url('/records'), { params }).then((r: any) => {
      const page = r.data ?? {};
      return {
        records: (page.records ?? []).map(adaptAlertRecord),
        total: Number(page.total ?? 0),
        current: Number(page.current ?? params.pageNum ?? 1),
        size: Number(page.size ?? params.pageSize ?? 20),
      };
    });
  },

  firing(): Promise<AlertRecord[]> {
    return http.get(url('/firing')).then((r: any) => (r.data ?? []).map(adaptAlertRecord));
  },

  stats(): Promise<{ firing: number }> {
    return http.get(url('/stats')).then((r: any) => r.data ?? { firing: 0 });
  },

  resolve(id: number | string, comment?: string): Promise<void> {
    return http.post(url(`/${id}/resolve`), { comment: comment ?? '已处理' }).then(() => undefined);
  },
};