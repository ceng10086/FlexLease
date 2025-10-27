import axios from 'axios';
import type { AxiosError } from 'axios';
import { message } from 'ant-design-vue';
import type { ApiResponseEnvelope } from '@/types';
import { useAuthStore } from '@/stores/auth';

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 10000
});

http.interceptors.request.use((config) => {
  const auth = useAuthStore();
  if (auth.token) {
    config.headers = config.headers ?? {};
    config.headers.Authorization = `Bearer ${auth.token}`;
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiResponseEnvelope<unknown>>) => {
    const auth = useAuthStore();
    if (error.response?.status === 401) {
      message.error('登录状态已过期，请重新登录');
      auth.logout();
    } else if (error.response?.data?.message) {
      message.error(error.response.data.message);
    }
    return Promise.reject(error);
  }
);

export default http;
