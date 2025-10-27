<template>
  <a-layout class="dashboard-layout">
    <a-layout-sider v-model:collapsed="collapsed" collapsible breakpoint="lg" class="dashboard-sider">
      <div class="logo">FlexLease</div>
      <SidebarMenu :items="menuItems" />
    </a-layout-sider>
    <a-layout>
      <a-layout-header class="dashboard-header">
        <div class="header-left">
          <a-typography-title :level="5" class="header-title">{{ pageTitle }}</a-typography-title>
        </div>
        <div class="header-right">
          <a-space align="center">
            <a-avatar size="small" style="background-color: #1677ff">{{ usernameInitial }}</a-avatar>
            <span class="username">{{ username }}</span>
            <RoleBadge :role="activeRole" />
            <a-dropdown v-if="roles.length > 1">
              <a-button type="text">
                切换视角
                <DownOutlined />
              </a-button>
              <template #overlay>
                <a-menu @click="onRoleSelect">
                  <a-menu-item v-for="role in roles" :key="role">{{ roleLabel(role) }}</a-menu-item>
                </a-menu>
              </template>
            </a-dropdown>
            <a-button type="text" @click="handleLogout">
              <template #icon><LogoutOutlined /></template>
              退出
            </a-button>
          </a-space>
        </div>
      </a-layout-header>
      <a-layout-content class="dashboard-content">
        <router-view />
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { computed, h, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import {
  DashboardOutlined,
  AppstoreOutlined,
  ShoppingCartOutlined,
  PayCircleOutlined,
  TeamOutlined,
  AreaChartOutlined,
  AuditOutlined,
  WalletOutlined,
  DeploymentUnitOutlined,
  LogoutOutlined,
  DownOutlined
} from '@ant-design/icons-vue';
import SidebarMenu, { type SidebarItem } from '@/components/layout/SidebarMenu.vue';
import RoleBadge from '@/components/common/RoleBadge.vue';
import { useAuthStore } from '@/stores/auth';
import type { UserRole } from '@/types';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();
const { activeRole, roles, defaultRoute } = storeToRefs(auth);

const collapsed = ref(false);

const username = computed(() => auth.profile?.username ?? '访客');
const usernameInitial = computed(() => username.value.slice(0, 1).toUpperCase());

const pageTitle = computed(() => route.meta.title ?? '智能租赁工作台');

const navDefinitions: Record<UserRole, SidebarItem[]> = {
  CUSTOMER: [
    { key: 'customer-overview', label: '租赁概览', path: '/customer/overview', icon: () => h(DashboardOutlined) },
    { key: 'customer-catalog', label: '商品目录', path: '/customer/catalog', icon: () => h(AppstoreOutlined) },
    { key: 'customer-orders', label: '我的订单', path: '/customer/orders', icon: () => h(ShoppingCartOutlined) },
    { key: 'customer-payments', label: '支付与账单', path: '/customer/payments', icon: () => h(PayCircleOutlined) }
  ],
  VENDOR: [
    { key: 'vendor-overview', label: '运营总览', path: '/vendor/overview', icon: () => h(DashboardOutlined) },
    { key: 'vendor-products', label: '商品管理', path: '/vendor/products', icon: () => h(AppstoreOutlined) },
    { key: 'vendor-orders', label: '订单协同', path: '/vendor/orders', icon: () => h(TeamOutlined) },
    { key: 'vendor-analytics', label: '运营分析', path: '/vendor/analytics', icon: () => h(AreaChartOutlined) }
  ],
  ADMIN: [
    { key: 'admin-overview', label: '平台概览', path: '/admin/overview', icon: () => h(DashboardOutlined) },
    { key: 'admin-products', label: '商品审核', path: '/admin/products', icon: () => h(AuditOutlined) },
    { key: 'admin-settlements', label: '资金结算', path: '/admin/settlements', icon: () => h(WalletOutlined) },
    { key: 'admin-vendors', label: '厂商入驻', path: '/admin/vendors', icon: () => h(DeploymentUnitOutlined) }
  ]
};

const menuItems = computed(() => {
  const role = activeRole.value ?? roles.value[0] ?? 'CUSTOMER';
  return navDefinitions[role];
});

function roleLabel(role: UserRole): string {
  switch (role) {
    case 'CUSTOMER':
      return 'C 端用户视角';
    case 'VENDOR':
      return 'B 端厂商视角';
    case 'ADMIN':
      return '平台管理员视角';
    default:
      return role;
  }
}

function onRoleSelect({ key }: { key: string }) {
  auth.setActiveRole(key as UserRole);
  router.push(defaultRoute.value);
}

function handleLogout() {
  auth.logout();
  router.push('/login');
}
</script>

<style scoped>
.dashboard-layout {
  min-height: 100vh;
}

.dashboard-sider {
  background: #0f172a;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 20px;
  color: #ffffff;
  letter-spacing: 0.5px;
}

.dashboard-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #ffffff;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.08);
}

.header-title {
  margin: 0;
}

.header-right {
  color: #1f2937;
}

.username {
  font-weight: 500;
}

.dashboard-content {
  padding: 24px;
  min-height: calc(100vh - 64px);
}
</style>
