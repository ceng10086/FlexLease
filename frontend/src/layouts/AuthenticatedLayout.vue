<template>
  <div class="auth-layout">
    <div v-if="auth.initializing" class="auth-layout__loading">
      <a-spin size="large" tip="正在加载会话" />
    </div>
    <a-layout v-else class="auth-layout__shell">
      <a-layout-sider
        v-model:collapsed="collapsed"
        collapsible
        :width="240"
        breakpoint="lg"
        class="auth-layout__sider"
      >
        <div class="auth-layout__logo">FlexLease</div>
        <a-menu
          mode="inline"
          :selectedKeys="selectedKeys"
          :openKeys="openKeys"
          @openChange="handleOpenChange"
          @select="handleSelect"
        >
          <template v-for="item in menuItems" :key="item.key">
            <a-sub-menu v-if="item.children && item.children.length" :key="item.key">
              <template #title>
                <component v-if="item.icon && iconMap[item.icon]" :is="iconMap[item.icon]" />
                <span>{{ item.label }}</span>
              </template>
              <a-menu-item
                v-for="child in item.children"
                :key="child.key"
              >
                <component v-if="child.icon && iconMap[child.icon]" :is="iconMap[child.icon]" />
                <span>{{ child.label }}</span>
              </a-menu-item>
            </a-sub-menu>
            <a-menu-item v-else :key="item.key">
              <component v-if="item.icon && iconMap[item.icon]" :is="iconMap[item.icon]" />
              <span>{{ item.label }}</span>
            </a-menu-item>
          </template>
        </a-menu>
      </a-layout-sider>
      <a-layout>
        <a-layout-header class="auth-layout__header">
          <div class="auth-layout__breadcrumbs">
            <a-breadcrumb>
              <a-breadcrumb-item v-for="crumb in breadcrumbs" :key="crumb.key">
                {{ crumb.label }}
              </a-breadcrumb-item>
            </a-breadcrumb>
            <h1 class="auth-layout__title">{{ activeTitle }}</h1>
          </div>
          <div class="auth-layout__actions">
            <a-space align="center">
              <a-tag v-for="role in auth.user?.roles ?? []" :key="role" color="blue">{{ role }}</a-tag>
              <a-dropdown trigger="['click']">
                <a-button type="text" class="auth-layout__user-button">
                  <a-avatar size="small" class="auth-layout__avatar">{{ initials }}</a-avatar>
                  <span class="auth-layout__username">{{ auth.user?.username }}</span>
                </a-button>
                <template #overlay>
                  <a-menu>
                    <a-menu-item key="profile">
                      <span>账号：{{ auth.user?.username }}</span>
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="logout" @click="handleLogout">退出登录</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </div>
        </a-layout-header>
        <a-layout-content class="auth-layout__content">
          <router-view />
        </a-layout-content>
      </a-layout>
    </a-layout>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  DashboardOutlined,
  AppstoreOutlined,
  ShoppingOutlined,
  ShoppingCartOutlined,
  BellOutlined,
  ControlOutlined,
  TeamOutlined,
  InboxOutlined,
  ProfileOutlined,
  ToolOutlined,
  ShopOutlined,
  FileAddOutlined,
  ContainerOutlined,
  SendOutlined,
  FundOutlined,
  TransactionOutlined
} from '@ant-design/icons-vue';
import { useAuthStore } from '../stores/auth';
import { resolveMenuForRoles, findNavItem, findNavPath, type NavItem } from '../router/menu';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const collapsed = ref(false);
const openKeys = ref<string[]>([]);

const iconMap: Record<string, any> = {
  DashboardOutlined,
  AppstoreOutlined,
  ShoppingCartOutlined,
  ShoppingOutlined,
  BellOutlined,
  ControlOutlined,
  TeamOutlined,
  InboxOutlined,
  ProfileOutlined,
  ToolOutlined,
  ShopOutlined,
  FileAddOutlined,
  ContainerOutlined,
  SendOutlined,
  FundOutlined,
  TransactionOutlined
};

const menuItems = computed(() => resolveMenuForRoles(auth.user?.roles ?? []));

const keyPathMap = computed(() => {
  const map = new Map<string, string>();
  const traverse = (items: NavItem[]) => {
    items.forEach((item) => {
      if (item.path) {
        map.set(item.key, item.path);
      }
      if (item.children) {
        traverse(item.children);
      }
    });
  };
  traverse(menuItems.value);
  return map;
});

const selectedKeys = computed(() => {
  const key = (route.meta.navKey as string | undefined) ?? 'overview';
  return [key];
});

const syncOpenKeys = () => {
  const navKey = route.meta.navKey as string | undefined;
  if (!navKey || collapsed.value) {
    openKeys.value = [];
    return;
  }
  const path = findNavPath(navKey, menuItems.value);
  if (path && path.length > 1) {
    openKeys.value = path.slice(0, -1);
  } else {
    openKeys.value = [];
  }
};

watch(
  () => route.meta.navKey,
  () => {
    syncOpenKeys();
  }
);

watch(collapsed, () => {
  syncOpenKeys();
});

const breadcrumbs = computed(() => {
  const navKey = (route.meta.navKey as string | undefined) ?? 'overview';
  const pathKeys = findNavPath(navKey, menuItems.value) ?? ['overview'];
  return pathKeys
    .map((key) => findNavItem(key, menuItems.value))
    .filter((item): item is NavItem => Boolean(item))
    .map((item) => ({ key: item.key, label: item.label }));
});

const activeTitle = computed(() => breadcrumbs.value.at(-1)?.label ?? '平台总览');

const initials = computed(() => {
  const username = auth.user?.username ?? '';
  if (!username) {
    return 'U';
  }
  return username.slice(0, 1).toUpperCase();
});

const handleOpenChange = (keys: string[]) => {
  openKeys.value = keys;
};

const handleSelect = ({ key }: { key: string }) => {
  const target = keyPathMap.value.get(key);
  if (target && target !== route.fullPath) {
    router.push(target);
  }
};

const handleLogout = () => {
  auth.logout();
  router.replace({ name: 'login' });
};

onMounted(async () => {
  if (!auth.user && auth.token) {
    await auth.bootstrap();
  }
  syncOpenKeys();
});
</script>

<style scoped>
.auth-layout__loading {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.auth-layout__shell {
  min-height: 100vh;
}

.auth-layout__sider {
  background: #0b1a37;
}

.auth-layout__logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.auth-layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  height: 64px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
}

.auth-layout__breadcrumbs {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.auth-layout__title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.auth-layout__actions {
  display: flex;
  align-items: center;
}

.auth-layout__user-button {
  display: flex;
  align-items: center;
  gap: 8px;
}

.auth-layout__username {
  font-weight: 500;
}

.auth-layout__avatar {
  background: #1677ff;
  color: #fff;
}

.auth-layout__content {
  padding: 24px;
  min-height: calc(100vh - 64px);
  background: #f5f7fa;
  overflow: auto;
}
</style>
