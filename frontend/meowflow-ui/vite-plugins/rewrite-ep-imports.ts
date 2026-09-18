import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { join } from 'node:path';
import type { Plugin } from 'vite';

const EP_PACKAGE = 'element-plus';
const EP_ES = `${EP_PACKAGE}/es`;

// 限制 clause 匹配在单行内，避免跨多行捕获后续 import 语句
const IMPORT_RE =
  /\bimport\s+(type\s+)?([^;\n]+?)\s+from\s+(['"])(element-plus(?:\/es)?)\3\s*;?/g;
const SIDE_EFFECT_BARE_RE = /\bimport\s+(['"])element-plus\1\s*;?/g;
const COMPONENT_ENTRY_RE = /^element-plus\/es\/components\/([^/]+)\/index\.mjs$/;
const MODULE_ID_RE = /\.(?:vue|[cm]?[jt]sx?)(?:\?|$)/;
const NODE_MODULES_RE = /[\\/]node_modules[\\/]/;
const EL_COMPONENT_RE = /^El[A-Z]/;

interface NamedImport {
  imported: string;
  local: string;
  isType: boolean;
}

interface ParsedClause {
  defaultName: string | null;
  namespaceName: string | null;
  named: NamedImport[];
}

function toKebab(name: string): string {
  const withoutPrefix = name.startsWith('El') && name[2] === name[2]?.toUpperCase()
    ? name.slice(2)
    : name;

  return withoutPrefix
    .replace(/([A-Z]+)([A-Z][a-z])/g, '$1-$2')
    .replace(/([a-z0-9])([A-Z])/g, '$1-$2')
    .toLowerCase();
}

function parseNamedImports(raw: string): NamedImport[] {
  return raw
    .split(',')
    .map((part) => part.trim())
    .filter(Boolean)
    .map((part) => {
      const isType = part.startsWith('type ');
      const normalized = isType ? part.slice(5).trim() : part;
      const [imported, local] = normalized.split(/\s+as\s+/);
      return {
        imported: imported.trim(),
        local: (local ?? imported).trim(),
        isType,
      };
    })
    .filter(({ imported }) => Boolean(imported));
}

function parseClause(raw: string): ParsedClause {
  const trimmed = raw.trim();
  const result: ParsedClause = {
    defaultName: null,
    namespaceName: null,
    named: [],
  };

  if (trimmed.startsWith('{')) {
    result.named = parseNamedImports(trimmed.slice(1, trimmed.lastIndexOf('}')));
    return result;
  }

  if (trimmed.startsWith('*')) {
    result.namespaceName = trimmed.match(/\*\s+as\s+([A-Za-z_$][\w$]*)/)?.[1] ?? null;
    return result;
  }

  const commaIdx = trimmed.indexOf(',');
  if (commaIdx === -1) {
    result.defaultName = trimmed;
    return result;
  }

  result.defaultName = trimmed.slice(0, commaIdx).trim();
  const rest = trimmed.slice(commaIdx + 1).trim();
  if (rest.startsWith('{')) {
    result.named = parseNamedImports(rest.slice(1, rest.lastIndexOf('}')));
  } else if (rest.startsWith('*')) {
    result.namespaceName = rest.match(/\*\s+as\s+([A-Za-z_$][\w$]*)/)?.[1] ?? null;
  }

  return result;
}

function getExportNames(source: string): string[] {
  const names: string[] = [];

  for (const match of source.matchAll(/export\s*\{([\s\S]*?)\}/g)) {
    const exportsList = match[1];
    if (!exportsList) continue;

    for (const rawExport of exportsList.split(',')) {
      const parts = rawExport.trim().split(/\s+as\s+/);
      const exportName = parts[parts.length - 1]?.trim();
      if (exportName && EL_COMPONENT_RE.test(exportName)) names.push(exportName);
    }
  }

  return names;
}

function createEntryResolver(componentsDir: string) {
  let entriesByExport: Map<string, string> | null = null;
  let redirectsByKebab: Map<string, string> | null = null;

  function scanEntries() {
    if (entriesByExport && redirectsByKebab) {
      return { entriesByExport, redirectsByKebab };
    }

    entriesByExport = new Map();
    redirectsByKebab = new Map();
    if (!existsSync(componentsDir)) return { entriesByExport, redirectsByKebab };

    for (const dirent of readdirSync(componentsDir, { withFileTypes: true })) {
      if (!dirent.isDirectory()) continue;

      const entryPath = join(componentsDir, dirent.name, 'index.mjs');
      if (!existsSync(entryPath)) continue;

      for (const exportName of getExportNames(readFileSync(entryPath, 'utf8'))) {
        const kebabName = toKebab(exportName);
        entriesByExport.set(exportName, dirent.name);
        if (kebabName !== dirent.name) redirectsByKebab.set(kebabName, dirent.name);
      }
    }

    return { entriesByExport, redirectsByKebab };
  }

  return {
    getImportDir(imported: string): string | null {
      const kebabName = toKebab(imported);
      const { entriesByExport } = scanEntries();
      if (entriesByExport.has(imported)) return kebabName;

      return existsSync(join(componentsDir, kebabName, 'index.mjs')) ? kebabName : null;
    },
    getActualDir(kebabName: string): string | null {
      return scanEntries().redirectsByKebab.get(kebabName) ?? null;
    },
  };
}

function stringifyNamedImport({ imported, local, isType }: NamedImport): string {
  const alias = imported === local ? imported : `${imported} as ${local}`;
  return isType ? `type ${alias}` : alias;
}

function normalizeToElementPlusEs(
  parsed: ParsedClause,
  quote: string,
  typePrefix: string | undefined,
): string[] {
  const lines: string[] = [];
  const importPrefix = `import ${typePrefix ?? ''}`;

  if (parsed.defaultName) {
    lines.push(`${importPrefix}${parsed.defaultName} from ${quote}${EP_ES}${quote};`);
  }

  if (parsed.namespaceName) {
    lines.push(`${importPrefix}* as ${parsed.namespaceName} from ${quote}${EP_ES}${quote};`);
  }

  if (parsed.named.length) {
    lines.push(`${importPrefix}{ ${parsed.named.map(stringifyNamedImport).join(', ')} } from ${quote}${EP_ES}${quote};`);
  }

  return lines;
}

export function rewriteElementPlusImports(): Plugin {
  let entryResolver = createEntryResolver('');

  return {
    name: 'rewrite-element-plus-imports',
    enforce: 'post',
    configResolved(config) {
      entryResolver = createEntryResolver(
        join(config.root, 'node_modules', EP_PACKAGE, 'es', 'components'),
      );
    },
    async resolveId(source, importer, options) {
      const match = source.match(COMPONENT_ENTRY_RE);
      if (!match) return null;

      const actualDir = entryResolver.getActualDir(match[1]);
      if (!actualDir) return null;

      const resolved = await this.resolve(
        `${EP_ES}/components/${actualDir}/index.mjs`,
        importer,
        { ...options, skipSelf: true },
      );

      return resolved?.id ?? null;
    },
    transform(code, id) {
      if (NODE_MODULES_RE.test(id) || !MODULE_ID_RE.test(id)) return null;
      if (!code.includes(EP_PACKAGE)) return null;

      let didChange = false;
      const out = code
        .replace(IMPORT_RE, (full, typePrefix, clause, quote, source) => {
          const parsed = parseClause(clause);

          if (typePrefix) {
            // import type 整体为类型导入，保留原样（esbuild 会剥离）
            return full;
          }

          const rewrittenByImportDir = new Map<string, NamedImport[]>();
          const preservedNamed: NamedImport[] = [];

          for (const named of parsed.named) {
            if (named.isType || !EL_COMPONENT_RE.test(named.imported)) {
              preservedNamed.push(named);
              continue;
            }

            const importDir = entryResolver.getImportDir(named.imported);
            if (!importDir) {
              preservedNamed.push(named);
              continue;
            }

            const entryImports = rewrittenByImportDir.get(importDir) ?? [];
            entryImports.push(named);
            rewrittenByImportDir.set(importDir, entryImports);
          }

          if (rewrittenByImportDir.size === 0 && source === EP_ES) return full;

          const lines: string[] = [];
          for (const [importDir, imports] of rewrittenByImportDir) {
            lines.push(
              `import { ${imports.map(stringifyNamedImport).join(', ')} } from ${quote}${EP_ES}/components/${importDir}/index.mjs${quote};`,
            );
          }

          parsed.named = preservedNamed;
          lines.push(...normalizeToElementPlusEs(parsed, quote, typePrefix));

          if (lines.length === 0) return full;

          didChange = true;
          return lines.join('\n');
        })
        .replace(SIDE_EFFECT_BARE_RE, (_full, quote) => {
          didChange = true;
          return `import ${quote}${EP_ES}${quote};`;
        });

      return didChange ? out : null;
    },
  };
}
