import { defineConfig, loadEnv } from 'vite';
import type { ConfigEnv, ProxyOptions } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig((configEnv: ConfigEnv) => {
  const env = loadEnv(configEnv.mode, process.cwd(), '');
  const defaultProxy: Record<string, ProxyOptions> = {
    '/api/v1/auth': {
      target: 'http://localhost:9001',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/internal': {
      target: 'http://localhost:9001',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/vendors/applications': {
      target: 'http://localhost:9002',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/users': {
      target: 'http://localhost:9002',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/admin/products': {
      target: 'http://localhost:9003',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/products': {
      target: 'http://localhost:9003',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/catalog': {
      target: 'http://localhost:9003',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/vendors': {
      target: 'http://localhost:9003',
      changeOrigin: true,
      secure: false
    }
  } as const;

  const proxyConfig = env.VITE_API_PROXY
    ? {
        '/api': {
          target: env.VITE_API_PROXY,
          changeOrigin: true,
          secure: false
        }
      }
    : defaultProxy;

  return {
    plugins: [vue()],
    server: {
      port: Number(env.VITE_DEV_PORT ?? 5173),
      proxy: proxyConfig
    },
    build: {
      outDir: 'dist',
      sourcemap: configEnv.mode !== 'production'
    }
  };
});
