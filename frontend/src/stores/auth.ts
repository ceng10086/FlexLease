import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { message } from 'ant-design-vue';
import { configureHttpAuth } from '../services/http';
import {
  login as loginApi,
  registerCustomer,
  registerVendor,
  fetchCurrentUser,
  type LoginPayload,
  type RegisterPayload,
  type AuthSession
} from '../services/authService';

const TOKEN_KEY = 'flexlease_token';

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '');
  const user = ref<AuthSession['user'] | null>(null);
  const initializing = ref(true);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const bootstrapCompleted = ref(false);

  const isAuthenticated = computed(() => Boolean(token.value));
  const roles = computed(() => new Set(user.value?.roles ?? []));

  const clearSession = () => {
    token.value = '';
    user.value = null;
    error.value = null;
    localStorage.removeItem(TOKEN_KEY);
  };

  configureHttpAuth({
    getToken: () => (token.value ? token.value : null),
    onUnauthorized: () => {
      const shouldNotify = token.value && bootstrapCompleted.value;
      if (shouldNotify) {
        message.warning('登录状态已过期，请重新登录');
      }
      clearSession();
    }
  });

  const bootstrap = async () => {
    if (!token.value) {
      initializing.value = false;
      bootstrapCompleted.value = true;
      return;
    }
    try {
      user.value = await fetchCurrentUser();
    } catch (err) {
      console.warn('Bootstrap auth failed', err);
      clearSession();
    } finally {
      initializing.value = false;
      bootstrapCompleted.value = true;
    }
  };

  const login = async (payload: LoginPayload) => {
    loading.value = true;
    error.value = null;
    try {
      const session = await loginApi(payload);
      token.value = session.accessToken;
      localStorage.setItem(TOKEN_KEY, session.accessToken);
      user.value = await fetchCurrentUser();
      message.success('登录成功');
    } catch (err) {
      const msg = err instanceof Error ? err.message : '登录失败';
      error.value = msg;
      message.error(msg);
      throw err;
    } finally {
      loading.value = false;
      initializing.value = false;
      bootstrapCompleted.value = true;
    }
  };

  const register = async (role: 'USER' | 'VENDOR', payload: RegisterPayload) => {
    loading.value = true;
    try {
      if (role === 'USER') {
        await registerCustomer(payload);
      } else {
        await registerVendor(payload);
      }
      message.success('注册成功，请使用新账号登录');
    } catch (err) {
      const msg = err instanceof Error ? err.message : '注册失败';
      message.error(msg);
      throw err;
    } finally {
      loading.value = false;
    }
  };

  const logout = () => {
    clearSession();
    bootstrapCompleted.value = true;
    message.success('已退出登录');
  };

  const hasRole = (role: string) => roles.value.has(role);

  return {
    token,
    user,
    initializing,
    bootstrapCompleted,
    loading,
    error,
    isAuthenticated,
    roles,
    hasRole,
    bootstrap,
    login,
    register,
    logout,
    clearSession
  };
});
