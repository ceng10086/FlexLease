<template>
  <div class="dashboard">
    <header class="dashboard__header">
      <div class="dashboard__brand">
        <span class="logo">FlexLease</span>
        <span class="divider" />
        <span class="section">控制台</span>
      </div>
      <div class="dashboard__actions">
        <a-dropdown>
          <a-button type="text">
            <template #icon>
              <a-avatar size="small">{{ initials }}</a-avatar>
            </template>
            {{ auth.user?.username || '未登录' }}
          </a-button>
          <template #overlay>
            <a-menu>
              <a-menu-item key="profile"><span>角色：{{ auth.user?.roles?.join(', ') || '未知' }}</span></a-menu-item>
              <a-menu-item key="logout" @click="handleLogout">退出登录</a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </header>

    <main class="dashboard__content">
      <a-row :gutter="16">
        <a-col :xs="24" :md="12" :lg="8">
          <a-card title="我的信息" bordered>
            <a-descriptions :column="1" size="small">
              <a-descriptions-item label="用户 ID">{{ auth.user?.id }}</a-descriptions-item>
              <a-descriptions-item label="用户名">{{ auth.user?.username }}</a-descriptions-item>
              <a-descriptions-item label="角色">{{ auth.user?.roles?.join(' / ') }}</a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>
        <a-col :xs="24" :md="12" :lg="16">
          <a-card title="快速导航" bordered>
            <a-steps :current="0" size="small" direction="vertical">
              <a-step title="厂商入驻管理" description="审核、查看厂商资料，管理共享租赁伙伴。" />
              <a-step title="商品与租赁配置" description="配置租赁方案、库存与商品上下架。" />
              <a-step title="订单与履约" description="处理订单履约、续租、退租与售后。" />
            </a-steps>
            <div class="dashboard__shortcuts">
              <a-space>
                <a-button type="primary" @click="goProducts" v-if="isVendor">管理我的商品</a-button>
                <a-button @click="goAdminProducts" v-if="isAdmin">审核商品</a-button>
              </a-space>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </main>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const router = useRouter();

onMounted(async () => {
  if (!auth.user && auth.token) {
    try {
      auth.user = await auth.fetchProfile();
    } catch (error) {
      console.error('加载用户信息失败', error);
      auth.clearSession();
      router.replace('/login');
    }
  }
});

const initials = computed(() => {
  if (!auth.user?.username) return 'U';
  const [first] = auth.user.username.split('');
  return first?.toUpperCase() ?? 'U';
});

const isAdmin = computed(() => auth.user?.roles?.includes('ADMIN'));
const isVendor = computed(() => auth.user?.roles?.includes('VENDOR'));

const goProducts = () => {
  router.push({ name: 'products' });
};

const goAdminProducts = () => {
  router.push({ name: 'admin-products' });
};

const handleLogout = () => {
  auth.logout();
  router.replace('/login');
};
</script>

<style scoped>
.dashboard {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.dashboard__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 64px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.08);
  position: sticky;
  top: 0;
  z-index: 10;
}

.dashboard__brand {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 18px;
  font-weight: 600;
}

.logo {
  color: #1677ff;
}

.divider {
  width: 1px;
  height: 24px;
  background: #d9d9d9;
}

.dashboard__content {
  flex: 1;
  padding: 24px;
}

.dashboard__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dashboard__shortcuts {
  margin-top: 24px;
}
</style>
