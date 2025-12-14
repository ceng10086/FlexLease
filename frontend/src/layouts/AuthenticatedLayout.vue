<template>
  <div class="auth-layout">
    <div v-if="auth.initializing" class="auth-layout__loading">
      <a-spin size="large" tip="正在加载会话" />
    </div>
    <a-layout v-else class="auth-layout__shell">
      <a-layout-sider
        v-model:collapsed="collapsed"
        collapsible
        :width="256"
        :collapsedWidth="siderCollapsedWidth"
        breakpoint="lg"
        class="auth-layout__sider"
      >
        <div class="auth-layout__logo">FlexLease</div>
        <a-menu
          mode="inline"
          :selectedKeys="selectedKeys"
          :openKeys="openKeys"
          @openChange="handleOpenChange"
        >
          <template v-for="section in menuSections" :key="section.key">
            <a-menu-item-group>
              <template #title>
                <div class="auth-layout__section-label">{{ section.label }}</div>
              </template>
              <template v-for="module in section.modules" :key="module.key">
                <a-sub-menu v-if="module.children && module.children.length">
                  <template #title>
                    <component v-if="module.icon && iconMap[module.icon]" :is="iconMap[module.icon]" />
                    <span>{{ module.label }}</span>
                  </template>
                  <a-menu-item
                    v-for="child in module.children"
                    :key="child.key"
                    @click="() => navigateTo(child.key)"
                  >
                    <component v-if="child.icon && iconMap[child.icon]" :is="iconMap[child.icon]" />
                    <span>{{ child.label }}</span>
                  </a-menu-item>
                </a-sub-menu>
                <a-menu-item v-else @click="() => navigateTo(module.key)">
                  <component v-if="module.icon && iconMap[module.icon]" :is="iconMap[module.icon]" />
                  <span>{{ module.label }}</span>
                </a-menu-item>
              </template>
            </a-menu-item-group>
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
              <p class="auth-layout__eyebrow">{{ activeEyebrow }}</p>
              <h1 class="auth-layout__title">{{ activeTitle }}</h1>
            </div>
          </div>
          <div class="auth-layout__actions">
            <a-tag v-for="role in auth.user?.roles ?? []" :key="role" color="processing">{{ role }}</a-tag>
            <a-dropdown trigger="['click']">
              <a-button type="text" class="auth-layout__user-button">
                <a-avatar size="small" class="auth-layout__avatar">{{ initials }}</a-avatar>
                <span class="auth-layout__username">{{ auth.user?.username }}</span>
              </a-button>
              <template #overlay>
                <a-menu @click="handleUserMenuClick">
                  <a-menu-item key="profile">个人资料</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="logout">退出登录</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
          </div>
        </a-layout-header>
        <a-layout-content class="auth-layout__content">
          <router-view />
        </a-layout-content>
      </a-layout>
      <nav class="bottom-tab-bar mobile-only">
        <button
          v-for="tab in bottomTabs"
          :key="tab.key"
          class="bottom-tab"
          :class="{ 'bottom-tab--active': selectedKeys.includes(tab.key) }"
          @click="() => router.push(tab.path)"
        >
          <component :is="iconMap[tab.icon]" />
          <span>{{ tab.label }}</span>
        </button>
      </nav>
    </a-layout>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
  DashboardOutlined,
  AppstoreOutlined,
  BellOutlined,
  ControlOutlined,
  TeamOutlined,
  InboxOutlined,
  ProfileOutlined,
  ShopOutlined,
  FileAddOutlined,
  ContainerOutlined,
  SendOutlined,
  FundOutlined,
  TransactionOutlined,
  CreditCardOutlined,
  ScheduleOutlined,
  MessageOutlined,
  UserOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined
} from '@ant-design/icons-vue';
import { useAuthStore } from '../stores/auth';
import {
  resolveMenuForRoles,
  flattenNavItems,
  findNavItem,
  findNavPath,
  resolveMobileTabs,
  type NavSection
} from '../router/menu';
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
  BellOutlined,
  ControlOutlined,
  TeamOutlined,
  InboxOutlined,
  ProfileOutlined,
  ShopOutlined,
  FileAddOutlined,
  ContainerOutlined,
  SendOutlined,
  FundOutlined,
  TransactionOutlined,
  CreditCardOutlined,
  ScheduleOutlined,
  MessageOutlined,
  UserOutlined
};

const menuSections = computed<NavSection[]>(() => resolveMenuForRoles(auth.user?.roles ?? []));
const flatMenu = computed(() => flattenNavItems(menuSections.value));
const bottomTabs = computed(() => resolveMobileTabs(auth.user?.roles ?? []));

const selectedKeys = computed(() => {
  const key = (route.meta.navKey as string | undefined) ?? 'dashboard-home';
  return [key];
});

const syncOpenKeys = () => {
  const navKey = route.meta.navKey as string | undefined;
  if (!navKey || collapsed.value) {
    openKeys.value = [];
    return;
  }
  const path = findNavPath(navKey, menuSections.value);
  if (path && path.length > 1) {
    openKeys.value = path.slice(1, -1);
  } else {
    openKeys.value = [];
  }
};

watch(
  () => route.meta.navKey,
  () => syncOpenKeys()
);

watch(collapsed, () => {
  syncOpenKeys();
});

watch(
  () => isMobile.value,
  (mobile) => {
    if (mobile) {
      collapsed.value = true;
    }
  },
  { immediate: true }
);

const breadcrumbs = computed(() => {
  const navKey = (route.meta.navKey as string | undefined) ?? 'dashboard-home';
  const pathKeys = findNavPath(navKey, menuSections.value) ?? [];
  const crumbs: Array<{ key: string; label: string }> = [];
  if (pathKeys.length) {
    const section = menuSections.value.find((section) => section.key === pathKeys[0]);
    if (section) {
      crumbs.push({ key: section.key, label: section.label });
    }
  }
  pathKeys.slice(1).forEach((key) => {
    const leaf = findNavItem(key, menuSections.value);
    if (leaf) {
      crumbs.push({ key: leaf.key, label: leaf.label });
    }
  });
  return crumbs;
});

const activeTitle = computed(() => breadcrumbs.value.at(-1)?.label ?? 'FlexLease');
const activeEyebrow = computed(() => breadcrumbs.value.at(0)?.label ?? '工作台');

const initials = computed(() => {
  const username = auth.user?.username ?? '';
  return username ? username.slice(0, 1).toUpperCase() : 'U';
});

const handleOpenChange = (keys: string[]) => {
  openKeys.value = keys;
};

const handleUserMenuClick = ({ key }: { key: string }) => {
  if (key === 'profile') {
    router.push({ name: 'profile' });
  } else if (key === 'logout') {
    auth.logout();
    router.replace({ name: 'login' });
  }
};

const toggleSider = () => {
  collapsed.value = !collapsed.value;
};

const navigateTo = (key: string) => {
  const target = flatMenu.value.find((item) => item.key === key)?.path;
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

.auth-layout__section-label {
  font-size: 11px;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.6);
}

.auth-layout__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  background: #fff;
  min-height: 76px;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.08);
  overflow: visible;
}

.auth-layout__breadcrumbs {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.auth-layout__eyebrow {
  margin: 0;
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
}

.auth-layout__title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  line-height: 1.4;
}

.auth-layout__actions {
  display: flex;
  align-items: center;
  gap: 12px;
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
  min-height: calc(100vh - 76px);
  background: var(--color-surface-muted);
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
  background: rgba(255, 255, 255, 0.12);
}

.bottom-tab-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  display: flex;
  justify-content: space-around;
  padding: 8px 12px;
  box-shadow: 0 -6px 20px rgba(15, 23, 42, 0.08);
  z-index: 10;
}

.bottom-tab {
  flex: 1;
  border: none;
  background: transparent;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: var(--color-text-secondary);
  font-size: 12px;
}

.bottom-tab--active {
  color: var(--color-primary);
  font-weight: 600;
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
    flex-wrap: wrap;
  }

  .auth-layout__content {
    min-height: calc(100vh - 64px);
  }
}
</style>
