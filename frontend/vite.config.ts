import { defineConfig, loadEnv } from 'vite';
import type { ConfigEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig((configEnv: ConfigEnv) => {
  const env = loadEnv(configEnv.mode, process.cwd(), '');
  return {
    plugins: [vue()],
    server: {
      port: Number(env.VITE_DEV_PORT ?? 5173),
      proxy: env.VITE_API_PROXY
        ? {
            '/api': {
              target: env.VITE_API_PROXY,
              changeOrigin: true,
              secure: false
            }
          }
        : undefined
    },
    build: {
      outDir: 'dist',
      sourcemap: configEnv.mode !== 'production'
    }
  };
});
