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
      <a-row :gutter="16" class="dashboard__metrics">
        <a-col :xs="24" :lg="16">
          <a-card title="平台运营指标" bordered>
            <a-spin :spinning="analytics.loading">
              <template v-if="analytics.error">
                <a-alert type="error" :message="analytics.error" show-icon />
              </template>
              <template v-else>
                <div v-if="dashboardMetrics" class="metrics-grid">
                  <a-row :gutter="16">
                    <a-col :xs="12" :md="8">
                      <a-statistic title="总订单" :value="dashboardMetrics.totalOrders" />
                    </a-col>
                    <a-col :xs="12" :md="8">
                      <a-statistic title="活跃订单" :value="dashboardMetrics.activeOrders" />
                    </a-col>
                    <a-col :xs="12" :md="8">
                      <a-statistic title="租赁中" :value="dashboardMetrics.inLeaseCount" />
                    </a-col>
                    <a-col :xs="12" :md="8">
                      <a-statistic title="待退租" :value="dashboardMetrics.pendingReturns" />
                    </a-col>
                    <a-col :xs="24" :md="8">
                      <a-statistic title="GMV (¥)" :value="formatCurrency(dashboardMetrics.totalGmv)" />
                    </a-col>
                  </a-row>
                  <div class="status-breakdown" v-if="statusEntries.length">
                    <span class="status-breakdown__title">状态分布：</span>
                    <a-space wrap>
                      <a-tag v-for="entry in statusEntries" :key="entry.status">
                        {{ entry.status }}：{{ entry.count }}
                      </a-tag>
                    </a-space>
                  </div>
                </div>
                <a-empty v-else description="暂无运营数据" />
              </template>
            </a-spin>
          </a-card>
        </a-col>
        <a-col :xs="24" :lg="8">
          <a-card title="重要提醒" bordered>
            <a-empty v-if="!statusEntries.length" description="暂无需要关注的订单" />
            <ul v-else class="reminder-list">
              <li v-for="entry in statusEntries" :key="entry.status">
                <span class="reminder-list__status">{{ entry.status }}</span>
                <span class="reminder-list__count">{{ entry.count }}</span>
              </li>
            </ul>
          </a-card>
        </a-col>
      </a-row>

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
import { useAnalyticsStore } from '../stores/analytics';
import { storeToRefs } from 'pinia';

const auth = useAuthStore();
const router = useRouter();
const analytics = useAnalyticsStore();
const { metrics: dashboardMetrics, statusEntries } = storeToRefs(analytics);

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

  if (!analytics.metrics?.value) {
    await analytics.loadDashboard();
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

const formatCurrency = (value: number) => {
  return value.toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
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

.dashboard__metrics {
  margin-bottom: 24px;
}

.dashboard__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.dashboard__shortcuts {
  margin-top: 24px;
}

.metrics-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.status-breakdown {
  margin-top: 16px;
}

.status-breakdown__title {
  font-weight: 600;
  margin-right: 8px;
}

.reminder-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reminder-list__status {
  font-weight: 600;
  margin-right: 8px;
}

.reminder-list__count {
  color: #1677ff;
}
</style>
