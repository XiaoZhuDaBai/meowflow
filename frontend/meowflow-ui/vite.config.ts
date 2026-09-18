import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import react from '@vitejs/plugin-react';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
import path from 'node:path';
import { rewriteElementPlusImports } from './vite-plugins/rewrite-ep-imports';

export default defineConfig({
  plugins: [
    vue(),
    react(),
    AutoImport({
      imports: ['vue', 'vue-router', '@vueuse/core'],
      resolvers: [ElementPlusResolver()],
      dts: 'auto-imports.d.ts',
    }),
    Components({
      resolvers: [ElementPlusResolver()],
      dts: 'components.d.ts',
    }),
    // Runs after Components; rewrites Element Plus bare imports to component entries.
    rewriteElementPlusImports(),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    host: '127.0.0.1',
    strictPort: false,
    proxy: Object.fromEntries(
      ['user', 'workflow', 'executor', 'template', 'monitor', 'infra'].map((prefix) => [
        `^/${prefix}(?=/|$)`,
        {
          target: 'http://localhost:8080',
          changeOrigin: true,
          secure: false,
        },
      ]),
    ),
  },
  build: {
    target: 'es2020',
    chunkSizeWarningLimit: 2000,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules/element-plus/')) return 'element-plus';
          if (id.includes('node_modules/echarts/')) return 'echarts';
          if (id.includes('node_modules/reactflow/')) return 'reactflow';
          if (id.includes('node_modules/react/')) return 'react';
          if (id.includes('node_modules/vue/') || id.includes('node_modules/vue-router/') || id.includes('node_modules/pinia/')) return 'vue';
          return undefined;
        },
      },
    },
  },
  optimizeDeps: {
    include: ['reactflow', 'element-plus', 'echarts', 'axios', 'dayjs'],
  },
});



