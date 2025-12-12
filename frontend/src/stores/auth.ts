import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { message } from 'ant-design-vue';
import { configureHttpAuth } from '../services/http';
import { clearQueryCache, setQueryCacheScope } from '../composables/useQuery';
import {
  login as loginApi,
  registerCustomer,
  registerVendor,
  fetchCurrentUser,
  refreshAuthToken,
  type LoginPayload,
  type RegisterPayload,
  type AuthSession
} from '../services/authService';
import { friendlyErrorMessage } from '../utils/error';

const TOKEN_KEY = 'flexlease_token';
const REFRESH_TOKEN_KEY = 'flexlease_refresh_token';

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '');
  const refreshToken = ref<string>(localStorage.getItem(REFRESH_TOKEN_KEY) ?? '');
  const user = ref<AuthSession['user'] | null>(null);
  const initializing = ref(true);
  const loading = ref(false);
  const error = ref<string | null>(null);
  const bootstrapCompleted = ref(false);

  const isAuthenticated = computed(() => Boolean(token.value));
  const roles = computed(() => new Set(user.value?.roles ?? []));
  const vendorId = computed(() => user.value?.vendorId ?? null);

  const applySession = (session: AuthSession) => {
    token.value = session.accessToken;
    refreshToken.value = session.refreshToken;
    localStorage.setItem(TOKEN_KEY, session.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, session.refreshToken);
  };

  const clearSession = () => {
    clearQueryCache();
    setQueryCacheScope(null);
    token.value = '';
    refreshToken.value = '';
    user.value = null;
    error.value = null;
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
  };

  configureHttpAuth({
    getToken: () => (token.value ? token.value : null),
    getRefreshToken: () => (refreshToken.value ? refreshToken.value : null),
    refreshTokens: async (currentRefreshToken) => {
      try {
        const session = await refreshAuthToken(currentRefreshToken);
        applySession(session);
        return session.accessToken;
      } catch (err) {
        clearSession();
        throw err;
      }
    },
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
      setQueryCacheScope(null);
      return;
    }
    try {
      user.value = await fetchCurrentUser();
      setQueryCacheScope(user.value?.id ?? null);
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
      applySession(session);
      user.value = await fetchCurrentUser();
      setQueryCacheScope(user.value?.id ?? null);
      message.success('登录成功');
    } catch (err) {
      const msg = friendlyErrorMessage(err, '登录失败，请稍后重试');
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
      const msg = friendlyErrorMessage(err, '注册失败，请稍后重试');
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

  const refreshSession = async () => {
    if (!refreshToken.value) {
      throw new Error('当前没有可用的刷新令牌');
    }
    const session = await refreshAuthToken(refreshToken.value);
    applySession(session);
    return session.accessToken;
  };

  const hasRole = (role: string) => roles.value.has(role);

  return {
    token,
    refreshToken,
    user,
    initializing,
    bootstrapCompleted,
    loading,
    error,
    isAuthenticated,
    roles,
    hasRole,
    vendorId,
    bootstrap,
    login,
    register,
    logout,
    clearSession,
    refreshSession
  };
});
