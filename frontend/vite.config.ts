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
    '/api/v1/customers': {
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
      target: 'http://localhost:9002',
      changeOrigin: true,
      secure: false,
      router: (req) => {
        const url = req?.url ?? '';
        const vendorProductOrInquiryPattern = /^\/api\/v1\/vendors\/[^/]+\/(products|inquiries)(\/|$)/;
        return vendorProductOrInquiryPattern.test(url) ? 'http://localhost:9003' : 'http://localhost:9002';
      }
    },
    '/api/v1/cart': {
      target: 'http://localhost:9004',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/orders': {
      target: 'http://localhost:9004',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/admin/orders': {
      target: 'http://localhost:9004',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/analytics': {
      target: 'http://localhost:9004',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/proofs': {
      target: 'http://localhost:9004',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/proof-policy': {
      target: 'http://localhost:9004',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/payments': {
      target: 'http://localhost:9005',
      changeOrigin: true,
      secure: false
    },
    '/api/v1/notifications': {
      target: 'http://localhost:9006',
      changeOrigin: true,
      secure: false
    },
    '/media': {
      target: 'http://localhost:9003',
      changeOrigin: true,
      secure: false
    },
    '/proofs': {
      target: 'http://localhost:9004',
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
        },
        '/proofs': {
          target: env.VITE_API_PROXY,
          changeOrigin: true,
          secure: false
        },
        '/media': {
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
      sourcemap: configEnv.mode !== 'production',
      rollupOptions: {
        output: {
          manualChunks: {
            vue: ['vue', 'vue-router', 'pinia'],
            antd: ['ant-design-vue', '@ant-design/icons-vue'],
            utility: ['axios', 'dayjs']
          }
        }
      },
      // 将 Ant Design 单独拆包，避免 chunk 体积告警干扰其它模块
      chunkSizeWarningLimit: 1600
    }
  };
});
