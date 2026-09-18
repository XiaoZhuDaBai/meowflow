import type { Plugin } from 'vite';

/**
 * Diagnostic: counts every rewrite the rewrite-ep-imports plugin applies
 * across the build. Used only for verification - remove after.
 */
export function debugRewriteEp(): Plugin {
  const subpathFiles = new Map<string, number>();
  const bareFiles = new Map<string, number>();
  const originalBareCounts = new Map<string, number>();

  return {
    name: 'debug-rewrite-ep',
    enforce: 'pre',
    transform(code, id) {
      if (!/\.(vue|ts|tsx|js|jsx|mjs|cjs)(\?.*)?$/.test(id)) return null;
      if (id.includes('node_modules')) return null;

      // Count bare 'element-plus' imports in original code (pre-rewrite)
      const bareMatches = code.match(/\bfrom\s+(['"])element-plus(?:\/es)?\1/g) || [];
      if (bareMatches.length) originalBareCounts.set(id, bareMatches.length);

      return null;
    },
    buildEnd() {
      console.log('\n[debug-rewrite-ep] Original bare `from "element-plus"` import count:');
      const sorted = [...originalBareCounts.entries()].sort((a, b) => b[1] - a[1]);
      for (const [id, count] of sorted) {
        const short = id.replace(/^.*[\\/](src|node_modules)[\\/]/, '$1/');
        console.log(`  ${count.toString().padStart(3)} imports  ${short}`);
      }
      console.log(`[debug-rewrite-ep] Total: ${sorted.length} files, ${[...originalBareCounts.values()].reduce((a, b) => a + b, 0)} bare imports`);
    },
  };
}
