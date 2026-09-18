import dayjs from 'dayjs';
import 'dayjs/locale/zh-cn';

dayjs.locale('zh-cn');

export { dayjs };

export function formatDate(input: string | number | Date | undefined | null, fmt = 'YYYY-MM-DD HH:mm'): string {
  if (!input) return '-';
  return dayjs(input).format(fmt);
}

export function formatRelativeTime(input: string | number | Date | undefined | null): string {
  if (!input) return '-';
  const target = dayjs(input);
  const diffSec = dayjs().diff(target, 'second');
  if (diffSec < 60) return `${diffSec} 秒前`;
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)} 分钟前`;
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)} 小时前`;
  if (diffSec < 86400 * 7) return `${Math.floor(diffSec / 86400)} 天前`;
  return target.format('YYYY-MM-DD');
}

export function formatDuration(ms: number | undefined | null): string {
  if (ms === undefined || ms === null || isNaN(ms)) return '-';
  if (ms < 1000) return `${Math.round(ms)} ms`;
  if (ms < 60_000) return `${(ms / 1000).toFixed(2)} 秒`;
  const minutes = Math.floor(ms / 60_000);
  const seconds = Math.floor((ms % 60_000) / 1000);
  return `${minutes} 分 ${seconds} 秒`;
}

export function formatCost(value: number | undefined | null): string {
  if (value === undefined || value === null) return '-';
  if (value < 1) return `¥ ${value.toFixed(3)}`;
  return `¥ ${value.toFixed(2)}`;
}

export function formatNumber(value: number | undefined | null): string {
  if (value === undefined || value === null) return '-';
  return value.toLocaleString('zh-CN');
}

export function formatPercent(value: number | undefined | null, digits = 1): string {
  if (value === undefined || value === null) return '-';
  return `${value.toFixed(digits)}%`;
}
