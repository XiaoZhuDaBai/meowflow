export type ApiService = 'user' | 'workflow' | 'template' | 'monitor' | 'infra';

export const isMockEnabled = import.meta.env.VITE_USE_MOCK === 'true';

const servicePrefix: Record<ApiService, string> = {
  user: '/user',
  workflow: '/workflow',
  template: '/template',
  monitor: '/monitor',
  infra: '/infra',
};

/**
 * Build a gateway URL while keeping the in-browser mock route table stable.
 * Backend controllers include their own /api prefix after the gateway strips
 * the service segment (for example /workflow/api/workflow).
 */
export function serviceUrl(
  service: ApiService,
  backendPath: string,
  mockPath = backendPath,
): string {
  if (isMockEnabled) return normalizePath(mockPath);
  return `${servicePrefix[service]}${normalizePath(backendPath)}`;
}

function normalizePath(path: string): string {
  return path.startsWith('/') ? path : `/${path}`;
}
