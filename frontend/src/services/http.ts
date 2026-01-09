/**
 * Axios 统一封装：
 * - 自动注入 `Authorization: Bearer <token>`
 * - 401 时尝试用 refreshToken 续期（并发场景用 refreshPromise 合并请求）
 * - refresh 失败或重复 401 时触发 onUnauthorized（清理会话并提示）
 */
import axios, { AxiosError, AxiosHeaders } from 'axios';
import type { InternalAxiosRequestConfig, AxiosResponse } from 'axios';

type AuthHandlers = {
  getToken: () => string | null;
  getRefreshToken?: () => string | null;
  refreshTokens?: (refreshToken: string) => Promise<string | null>;
  onUnauthorized: () => void;
};

let authHandlers: AuthHandlers = {
  getToken: () => null,
  getRefreshToken: () => null,
  refreshTokens: async () => null,
  onUnauthorized: () => undefined
};

export const configureHttpAuth = (handlers: AuthHandlers) => {
  authHandlers = handlers;
};

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
});

let refreshPromise: Promise<string | null> | null = null;

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = authHandlers.getToken();
  if (token) {
    const headers = config.headers instanceof AxiosHeaders
      ? config.headers
      : new AxiosHeaders(config.headers);
    headers.set('Authorization', `Bearer ${token}`);
    config.headers = headers;
  }
  return config;
});

http.interceptors.response.use(
  (response: AxiosResponse) => response,
  async (error: AxiosError) => {
    const { response } = error;
    const originalRequest = error.config as (InternalAxiosRequestConfig & {
      _retry?: boolean;
      _skipAuthRefresh?: boolean;
    }) | undefined;

    // 约定：对 refresh 接口本身跳过自动刷新，避免递归死循环
    if (response?.status === 401 && originalRequest && !originalRequest._skipAuthRefresh) {
      if (originalRequest._retry) {
        authHandlers.onUnauthorized();
        return Promise.reject(error);
      }

      const currentRefreshToken = authHandlers.getRefreshToken?.() ?? null;
      const refreshTokens = authHandlers.refreshTokens;

      if (!currentRefreshToken || !refreshTokens) {
        authHandlers.onUnauthorized();
        return Promise.reject(error);
      }

      if (!refreshPromise) {
        // 同一时间只允许一个 refresh 请求；其余 401 等待该 Promise 结束后复用结果
        refreshPromise = refreshTokens(currentRefreshToken).finally(() => {
          refreshPromise = null;
        });
      }

      try {
        const newAccessToken = await refreshPromise;
        if (newAccessToken) {
          originalRequest._retry = true;
          const headers = originalRequest.headers instanceof AxiosHeaders
            ? originalRequest.headers
            : new AxiosHeaders(originalRequest.headers);
          headers.set('Authorization', `Bearer ${newAccessToken}`);
          originalRequest.headers = headers;
          return http(originalRequest);
        }
      } catch (refreshError) {
        authHandlers.onUnauthorized();
        return Promise.reject(refreshError);
      }

      authHandlers.onUnauthorized();
    }

    return Promise.reject(error);
  }
);

export default http;
