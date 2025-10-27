import { defineStore } from 'pinia';
import { computed, ref } from 'vue';
import { message } from 'ant-design-vue';
import { fetchProfile, login as loginApi } from '@/api/auth';
import type { UserProfile, UserRole } from '@/types';

const TOKEN_STORAGE_KEY = 'flexlease/token';
const ROLE_STORAGE_KEY = 'flexlease/role';

const storage = typeof window !== 'undefined' ? window.localStorage : undefined;

function persist(key: string, value: string | null) {
  if (!storage) return;
  if (value === null) {
    storage.removeItem(key);
  } else {
    storage.setItem(key, value);
  }
}

function read(key: string): string | null {
  if (!storage) return null;
  return storage.getItem(key);
}

function simulateProfile(role: UserRole, username: string): UserProfile {
  const id = `offline-${role}-${Math.random().toString(36).slice(2, 10)}`;
  const aliasMap: Record<UserRole, string> = {
    CUSTOMER: '体验用户',
    VENDOR: '体验厂商',
    ADMIN: '体验管理员'
  };
  return {
    id,
    username: username || aliasMap[role],
    roles: [role],
    lastLoginAt: new Date().toISOString()
  };
}

function resolveDefaultRoute(role?: UserRole | null): string {
  switch (role) {
    case 'VENDOR':
      return '/vendor/overview';
    case 'ADMIN':
      return '/admin/overview';
    case 'CUSTOMER':
    default:
      return '/customer/overview';
  }
}

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(read(TOKEN_STORAGE_KEY));
  const profile = ref<UserProfile | null>(null);
  const activeRole = ref<UserRole | null>((read(ROLE_STORAGE_KEY) as UserRole | null) ?? null);
  const initialized = ref(false);
  const loading = ref(false);

  const isAuthenticated = computed(() => Boolean(token.value && profile.value));
  const roles = computed<UserRole[]>(() => profile.value?.roles ?? []);
  const defaultRoute = computed(() => resolveDefaultRoute(activeRole.value ?? roles.value[0] ?? 'CUSTOMER'));

  function getDefaultRoute(role?: UserRole | null) {
    return resolveDefaultRoute(role ?? activeRole.value ?? roles.value[0] ?? 'CUSTOMER');
  }

  function hasRole(role: UserRole): boolean {
    return roles.value.includes(role);
  }

  function setActiveRole(role: UserRole) {
    if (profile.value && !profile.value.roles.includes(role)) {
      profile.value = { ...profile.value, roles: [...profile.value.roles, role] };
    }
    activeRole.value = role;
    persist(ROLE_STORAGE_KEY, role);
  }

  async function bootstrap() {
    if (initialized.value) return;
    if (token.value) {
      try {
        const data = await fetchProfile();
        profile.value = data;
        if (data.roles.length > 0) {
          const resolved = data.roles.includes(activeRole.value as UserRole)
            ? (activeRole.value as UserRole)
            : data.roles[0];
          setActiveRole(resolved);
        }
      } catch (error) {
        console.warn('无法加载会话信息，已退出登录', error);
        clearSession();
      }
    }
    initialized.value = true;
  }

  async function login(payload: { username: string; password: string; roleHint?: UserRole }) {
    loading.value = true;
    try {
      const { accessToken } = await loginApi({ username: payload.username, password: payload.password });
      token.value = accessToken;
      persist(TOKEN_STORAGE_KEY, accessToken);
      const data = await fetchProfile();
      profile.value = data;
      const candidate = payload.roleHint && data.roles.includes(payload.roleHint)
        ? payload.roleHint
        : data.roles[0] ?? null;
      if (candidate) {
        setActiveRole(candidate);
      }
      message.success('登录成功');
    } catch (error) {
      if (payload.roleHint) {
        token.value = `offline-${payload.roleHint}`;
        profile.value = simulateProfile(payload.roleHint, payload.username);
        setActiveRole(payload.roleHint);
        persist(TOKEN_STORAGE_KEY, token.value);
        message.success('已进入模拟体验模式');
      } else {
        throw error;
      }
    } finally {
      loading.value = false;
    }
  }

  function simulate(role: UserRole) {
    token.value = `offline-${role}`;
    profile.value = simulateProfile(role, '');
    setActiveRole(role);
    persist(TOKEN_STORAGE_KEY, token.value);
    message.info('已进入模拟体验模式');
  }

  function clearSession() {
    token.value = null;
    profile.value = null;
    activeRole.value = null;
    persist(TOKEN_STORAGE_KEY, null);
    persist(ROLE_STORAGE_KEY, null);
  }

  function logout() {
    clearSession();
  }

  return {
    token,
    profile,
    activeRole,
    initialized,
    loading,
    isAuthenticated,
    roles,
    defaultRoute,
    getDefaultRoute,
    hasRole,
    setActiveRole,
    bootstrap,
    login,
    simulate,
    logout
  };
});
