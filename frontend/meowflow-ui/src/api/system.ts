import http from './http';
import { serviceUrl } from './endpoints';

/**
 * 系统设置 API(调用 meowflow-infra 后端)
 *
 * 工作流节点配置会通过这里的接口加载模型/集成列表,
 * 节点存储的是 ID(modelId/integrationId),而非密钥。
 */

export interface AiModelOption {
  id: number | string;
  name: string;
  provider: string;
  modelKey?: string;
  baseUrl?: string;
  apiKeyMasked?: string;
  enabled: boolean;
  isDefault?: boolean;
  capabilities?: string;
}

export interface IntegrationOption {
  id: number | string;
  type: 'dingtalk' | 'wxwork' | 'feishu' | 'email' | 'sms';
  name: string;
  enabled: boolean;
  webhookUrl?: string;
  secret?: string;
  accessKeyId?: string;
  accessKeySecret?: string;
  customConfig?: string;
  retryTimes?: number;
  timeoutSeconds?: number;
  description?: string;
}

let _models: AiModelOption[] = [];
let _integrations: IntegrationOption[] = [];
let _modelsLoadedAt = 0;
let _integrationsLoadedAt = 0;
const CACHE_TTL_MS = 5 * 60 * 1000; // 5 分钟

function isCacheFresh(ts: number) {
  return ts > 0 && Date.now() - ts < CACHE_TTL_MS;
}

export const systemApi = {
  /**
   * 列出所有模型(系统设置页)
   * @param params.scope 'personal' | 'team' — 决定从哪个 orgId 拉取配置
   */
  listAllModels(params?: { scope?: 'personal' | 'team' }): Promise<AiModelOption[]> {
    return http
      .get(serviceUrl('infra', '/api/infra/model'), { params })
      .then((r: any) => {
        _models = r.data ?? [];
        _modelsLoadedAt = Date.now();
        return _models;
      });
  },

  /**
   * 列出启用的模型(节点下拉框,带缓存)
   */
  enabledModels(force = false): Promise<AiModelOption[]> {
    if (!force && isCacheFresh(_modelsLoadedAt) && _models.length) {
      return Promise.resolve(_models.filter((m) => m.enabled));
    }
    return http
      .get(serviceUrl('infra', '/api/infra/model/enabled'))
      .then((r: any) => {
        const list = (r.data ?? []) as AiModelOption[];
        _models = list;
        _modelsLoadedAt = Date.now();
        return list;
      });
  },

  createModel(payload: Partial<AiModelOption>): Promise<AiModelOption> {
    return http
      .post(serviceUrl('infra', '/api/infra/model'), payload)
      .then((r: any) => {
        _modelsLoadedAt = 0; // invalidate cache
        return r.data;
      });
  },

  updateModel(id: number | string, payload: Partial<AiModelOption>): Promise<AiModelOption> {
    return http
      .put(serviceUrl('infra', `/api/infra/model/${id}`), payload)
      .then((r: any) => {
        _modelsLoadedAt = 0;
        return r.data;
      });
  },

  deleteModel(id: number | string): Promise<void> {
    return http
      .delete(serviceUrl('infra', `/api/infra/model/${id}`))
      .then(() => {
        _modelsLoadedAt = 0;
      });
  },

  testModel(id: number | string): Promise<{ success: boolean; message: string }> {
    return http
      .post(serviceUrl('infra', `/api/infra/model/${id}/test`))
      .then((r: any) => r.data);
  },

  setDefaultModel(id: number | string): Promise<void> {
    return http
      .put(serviceUrl('infra', `/api/infra/model/${id}/default`))
      .then(() => {
        _modelsLoadedAt = 0;
      });
  },

  /**
   * 列出所有集成(系统设置页)
   * @param params.scope 'personal' | 'team' — 决定从哪个 orgId 拉取配置
   */
  listAllIntegrations(params?: { scope?: 'personal' | 'team' }): Promise<IntegrationOption[]> {
    return http
      .get(serviceUrl('infra', '/api/infra/integration'), { params })
      .then((r: any) => {
        _integrations = r.data ?? [];
        _integrationsLoadedAt = Date.now();
        return _integrations;
      });
  },

  /**
   * 列出启用的集成(节点下拉框,带缓存)
   */
  enabledIntegrations(force = false): Promise<IntegrationOption[]> {
    if (!force && isCacheFresh(_integrationsLoadedAt) && _integrations.length) {
      return Promise.resolve(_integrations.filter((i) => i.enabled));
    }
    return http
      .get(serviceUrl('infra', '/api/infra/integration/enabled'))
      .then((r: any) => {
        const list = (r.data ?? []) as IntegrationOption[];
        _integrations = list;
        _integrationsLoadedAt = Date.now();
        return list;
      });
  },

  createIntegration(payload: Partial<IntegrationOption>): Promise<IntegrationOption> {
    return http
      .post(serviceUrl('infra', '/api/infra/integration'), payload)
      .then((r: any) => {
        _integrationsLoadedAt = 0;
        return r.data;
      });
  },

  updateIntegration(id: number | string, payload: Partial<IntegrationOption>): Promise<IntegrationOption> {
    return http
      .put(serviceUrl('infra', `/api/infra/integration/${id}`), payload)
      .then((r: any) => {
        _integrationsLoadedAt = 0;
        return r.data;
      });
  },

  deleteIntegration(id: number | string): Promise<void> {
    return http
      .delete(serviceUrl('infra', `/api/infra/integration/${id}`))
      .then(() => {
        _integrationsLoadedAt = 0;
      });
  },

  testIntegration(id: number | string): Promise<{ success: boolean; message: string }> {
    return http
      .post(serviceUrl('infra', `/api/infra/integration/${id}/test`))
      .then((r: any) => r.data);
  },

  /**
   * 兼容旧 API 方法名,避免破坏现有 mock 流程
   * @deprecated 使用 listAllModels
   */
  llmModels(): Promise<AiModelOption[]> {
    return this.listAllModels();
  },

  /**
   * @deprecated 使用 updateModel
   */
  updateLlmModel(id: string, payload: Partial<AiModelOption>) {
    return this.updateModel(id, payload);
  },

  /**
   * @deprecated 使用 listAllIntegrations
   */
  integrations(): Promise<IntegrationOption[]> {
    return this.listAllIntegrations();
  },

  /**
   * @deprecated 使用 updateIntegration
   */
  updateIntegrationCompat(id: string, payload: Partial<IntegrationOption>) {
    return this.updateIntegration(id, payload);
  },

  /**
   * 节点目录(前端目前用 mock,保留此接口)
   */
  nodeCatalog(): Promise<any[]> {
    return Promise.resolve([]);
  },

  /**
   * 清空缓存(供测试或登出时使用)
   */
  clearCache() {
    _models = [];
    _integrations = [];
    _modelsLoadedAt = 0;
    _integrationsLoadedAt = 0;
  },
};
