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
        :collapsedWidth="siderCollapsedWidth"
        breakpoint="lg"
        class="auth-layout__sider"
      >
        <div class="auth-layout__logo">FlexLease</div>
        <a-menu
          theme="dark"
          mode="inline"
          :selectedKeys="selectedKeys"
          :openKeys="openKeys"
          @openChange="handleOpenChange"
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
                @click="() => navigateTo(child.key)"
              >
                <component v-if="child.icon && iconMap[child.icon]" :is="iconMap[child.icon]" />
                <span>{{ child.label }}</span>
              </a-menu-item>
            </a-sub-menu>
            <a-menu-item v-else :key="item.key" @click="() => navigateTo(item.key)">
              <component v-if="item.icon && iconMap[item.icon]" :is="iconMap[item.icon]" />
              <span>{{ item.label }}</span>
            </a-menu-item>
          </template>
        </a-menu>
      </a-layout-sider>
      <a-layout>
        <a-layout-header class="auth-layout__header">
          <div class="auth-layout__header-left">
            <a-button
              v-if="isMobile"
              class="auth-layout__menu-toggle"
              type="text"
              @click="toggleSider"
            >
              <template #icon>
                <component :is="collapsed ? MenuUnfoldOutlined : MenuFoldOutlined" />
              </template>
            </a-button>
            <div class="auth-layout__breadcrumbs">
              <h1 class="auth-layout__title">{{ activeTitle }}</h1>
            </div>
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
                  <a-menu @click="handleUserMenuClick">
                    <a-menu-item key="profile">
                      <span>个人资料</span>
                    </a-menu-item>
                    <a-menu-divider />
                    <a-menu-item key="logout">退出登录</a-menu-item>
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
  TransactionOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined
} from '@ant-design/icons-vue';
import { useAuthStore } from '../stores/auth';
import { resolveMenuForRoles, findNavItem, findNavPath, type NavItem } from '../router/menu';
import { useViewport } from '../composables/useViewport';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const { isMobile } = useViewport();

const collapsed = ref(false);
const openKeys = ref<string[]>([]);
const siderCollapsedWidth = computed(() => (isMobile.value ? 0 : 80));

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

watch(
  isMobile,
  (mobile) => {
    if (mobile) {
      collapsed.value = true;
    }
  },
  { immediate: true }
);

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

const handleUserMenuClick = ({ key }: { key: string }) => {
  if (key === 'profile') {
    router.push({ name: 'profile' });
  } else if (key === 'logout') {
    handleLogout();
  }
};

const handleLogout = () => {
  auth.logout();
  router.replace({ name: 'login' });
};

const toggleSider = () => {
  collapsed.value = !collapsed.value;
};

const navigateTo = (key: string) => {
  const target = keyPathMap.value.get(key);
  if (!target || target === route.fullPath) {
    return;
  }
  router.push(target).catch(() => undefined);
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

.auth-layout__header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.auth-layout__menu-toggle {
  width: 40px;
  height: 40px;
}

:deep(.auth-layout__sider .ant-layout-sider-children) {
  display: flex;
  flex-direction: column;
  height: 100%;
}

:deep(.auth-layout__sider .ant-menu) {
  background: transparent;
  color: #e2e8f0;
  border-inline-end: none;
}

:deep(.auth-layout__sider .ant-menu-item),
:deep(.auth-layout__sider .ant-menu-submenu-title) {
  height: auto;
  line-height: 1.4;
  padding-top: 10px;
  padding-bottom: 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: normal;
}

:deep(.auth-layout__sider .ant-menu .ant-menu-title-content) {
  flex: 1;
}

:deep(.auth-layout__sider .ant-menu-item-selected) {
  background: rgba(255, 255, 255, 0.1);
}

@media (max-width: 768px) {
  .auth-layout__header {
    flex-direction: column;
    align-items: flex-start;
    height: auto;
    padding: 12px 16px;
    gap: 12px;
  }

  .auth-layout__actions {
    width: 100%;
    justify-content: space-between;
  }

  .auth-layout__content {
    padding: 16px;
  }

  .auth-layout__title {
    font-size: 16px;
  }
}
</style>
