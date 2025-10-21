import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { message } from 'ant-design-vue';
import api from '../utils/api';

export type UserSummary = {
  id: string;
  username: string;
  roles: string[];
  lastLoginAt?: string | null;
};

export type LoginPayload = {
  username: string;
  password: string;
};

const TOKEN_KEY = 'flexlease_token';

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '');
  const user = ref<UserSummary | null>(null);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const initializing = ref(true);

  const isAuthenticated = computed(() => Boolean(token.value));

  const clearSession = () => {
    token.value = '';
    user.value = null;
    error.value = null;
    localStorage.removeItem(TOKEN_KEY);
  };

  const fetchProfile = async (): Promise<UserSummary> => {
    const response = await api.get('/auth/me');
    if (!response.data?.data) {
      throw new Error('未能获取用户信息');
    }
    return response.data.data as UserSummary;
  };

  const bootstrap = async () => {
    if (!token.value) {
      initializing.value = false;
      return;
    }
    try {
      user.value = await fetchProfile();
    } catch (err) {
      console.warn('Bootstrap auth failed', err);
      clearSession();
    } finally {
      initializing.value = false;
    }
  };

  const login = async (payload: LoginPayload) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await api.post('/auth/token', payload);
      const accessToken = response.data?.data?.accessToken as string | undefined;
      if (!accessToken) {
        throw new Error('登录失败，请稍后重试');
      }
      token.value = accessToken;
      localStorage.setItem(TOKEN_KEY, accessToken);
      user.value = await fetchProfile();
      message.success('登录成功');
    } catch (err: unknown) {
      const errorMessage = err instanceof Error ? err.message : '登录失败';
      error.value = errorMessage;
      message.error(errorMessage);
      throw err;
    } finally {
      loading.value = false;
      initializing.value = false;
    }
  };

  const logout = () => {
    clearSession();
    message.success('已退出登录');
  };

  return {
    token,
    user,
    loading,
    error,
    initializing,
    isAuthenticated,
    bootstrap,
    login,
    fetchProfile,
    logout,
    clearSession
  };
});
