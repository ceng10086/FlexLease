import axios, { AxiosHeaders } from 'axios';
import type { AxiosError, AxiosResponse, InternalAxiosRequestConfig } from 'axios';

type AuthHandlers = {
  getToken: () => string | null;
  clearSession: () => void;
};

let authHandlers: AuthHandlers = {
  getToken: () => null,
  clearSession: () => undefined
};

export const configureApiAuth = (handlers: AuthHandlers) => {
  authHandlers = handlers;
};

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1'
});

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
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

api.interceptors.response.use(
  (response: AxiosResponse) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      authHandlers.clearSession();
    }
    return Promise.reject(error);
  }
);

export default api;
