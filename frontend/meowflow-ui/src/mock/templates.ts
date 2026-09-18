import type { Template } from '@/api/types';
import { STORAGE_KEYS } from '@/utils/constants';
import { getStoredJson, setStoredJson } from '@/utils/auth';
import { BUILTIN_TEMPLATES, type BuiltinTemplate } from './builtinTemplates';

/**
 * 缓存版本号。
 * 当 builtinTemplates.ts 中模板结构（节点/边数量、字段）发生不兼容变化时，
 * 提升此版本号以让 `loadTemplates()` 主动忽略旧的 localStorage 缓存。
 *
 * 历史：
 *  - v1:  初版（每个模板仅 1-2 节点）
 *  - v2:  全部 17 个模板重写（≥12 节点，含分支/条件/循环/异常边）
 *  - v3:  解决老 localStorage 中缓存了无 workflowJson 的旧模板 → 全部展示 fallback
 *  - v4:  删除 LEGACY_SEED（老板模板），使用模板真实 workflowJson 创建工作流
 */
const BUILTIN_CACHE_VERSION = 6;

/** 将内置模板统一转为前端 Template 形态，字段与后端 TemplateDTO 对齐 */
function builtinsToTemplates(items: BuiltinTemplate[]): Template[] {
  return items.map((t) => ({
    id: t.id,
    name: t.name,
    description: t.description,
    category: t.categoryName,
    tags: t.tags,
    coverEmoji: t.coverEmoji,
    author: t.author,
    usageCount: t.usageCount,
    rating: t.rating,
    isOfficial: t.isOfficial,
    workflowId: t.workflowId,
    createdAt: t.createdAt,
    likes: Math.floor(Math.random() * 200) + 10,
    isLiked: false,
    isFavorited: false,
    // 字段透传：market/详情页/SVG 预览会读取
    ...(t.workflowJson ? { workflowJson: t.workflowJson } : {}),
    ...(t.workflowGraph ? { workflowGraph: t.workflowGraph } : {}),
    ...(t.category ? { categoryId: t.category } : {}),
  } as Template));
}

export function loadBuiltinTemplates(): Template[] {
  return builtinsToTemplates(BUILTIN_TEMPLATES);
}

/**
 * 加载模板列表（mock 兼容）。
 *
 * 重要：localStorage 中缓存的旧版本数据可能没有 `workflowJson`，会导致：
 *   1. TemplatePreview 画布退化为 fallback 示例，所有模板长得一样
 *   2. 「节点 / 参数配置 / 连线」Tab 全部为空
 *
 * 为此我们引入两段校验：
 *   - 版本号 (`meowflow.templates.version`) 失效 → 重建
 *   - 即便版本号命中，若发现任意 builtin 模板的 `workflowJson` 缺失 → 重建
 */
export function loadTemplates(): Template[] {
  const versionStored = getStoredJson<number | null>(
    `${STORAGE_KEYS.TEMPLATES}.version`,
    null,
  );

  const builtins = builtinsToTemplates(BUILTIN_TEMPLATES);
  const builtinIds = new Set(builtins.map((t) => t.id));

  const stored = getStoredJson<Template[] | null>(STORAGE_KEYS.TEMPLATES, null);
  const storedValid =
    stored &&
    Array.isArray(stored) &&
    stored.length &&
    // 所有 builtin 模板都在缓存里
    builtins.every((b) => stored.some((s) => s.id === b.id)) &&
    // 缓存里的 builtin 模板必须带 workflowJson（兼容老缓存里没有这字段的版本）
    stored
      .filter((s) => builtinIds.has(s.id))
      .every((s) => typeof (s as any).workflowJson === 'string' && (s as any).workflowJson.length > 0);

  if (versionStored === BUILTIN_CACHE_VERSION && storedValid) {
    return stored as Template[];
  }

  // 缓存版本不匹配 / 缓存无效 → 用最新 builtin 重建
  const merged = [...builtins];
  setStoredJson(STORAGE_KEYS.TEMPLATES, merged);
  setStoredJson(`${STORAGE_KEYS.TEMPLATES}.version`, BUILTIN_CACHE_VERSION);
  return merged;
}
