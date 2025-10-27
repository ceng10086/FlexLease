import axios, { AxiosError, AxiosHeaders } from 'axios';
import type { InternalAxiosRequestConfig, AxiosResponse } from 'axios';

type AuthHandlers = {
  getToken: () => string | null;
  onUnauthorized: () => void;
};

let authHandlers: AuthHandlers = {
  getToken: () => null,
  onUnauthorized: () => undefined
};

export const configureHttpAuth = (handlers: AuthHandlers) => {
  authHandlers = handlers;
};

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
});

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
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      authHandlers.onUnauthorized();
    }
    return Promise.reject(error);
  }
);

export default http;
