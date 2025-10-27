<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>平台总览</h2>
        <p class="page-header__meta">查看租赁平台核心指标与最近活动，快速掌握业务态势。</p>
      </div>
      <a-space>
        <a-tag v-for="role in auth.user?.roles ?? []" :key="role" color="blue">{{ role }}</a-tag>
      </a-space>
    </div>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="16">
        <a-card title="平台运营指标" :loading="state.loadingMetrics">
          <template v-if="state.metrics">
            <a-row :gutter="16">
              <a-col :xs="12" :md="8">
                <a-statistic title="总订单" :value="state.metrics.totalOrders" />
              </a-col>
              <a-col :xs="12" :md="8">
                <a-statistic title="活跃订单" :value="state.metrics.activeOrders" />
              </a-col>
              <a-col :xs="12" :md="8">
                <a-statistic title="租赁中" :value="state.metrics.inLeaseCount" />
              </a-col>
              <a-col :xs="12" :md="8">
                <a-statistic title="待退租" :value="state.metrics.pendingReturns" />
              </a-col>
              <a-col :xs="24" :md="8">
                <a-statistic title="GMV (¥)" :value="formatCurrency(state.metrics.totalGmv)" />
              </a-col>
            </a-row>
            <div class="status-breakdown" v-if="statusEntries.length">
              <span class="status-breakdown__title">订单状态分布：</span>
              <a-space wrap>
                <a-tag v-for="item in statusEntries" :key="item.status">{{ item.status }}：{{ item.count }}</a-tag>
              </a-space>
            </div>
          </template>
          <a-empty v-else description="暂无数据" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="系统公告">
          <template v-if="state.notifications.length">
            <a-timeline>
              <a-timeline-item v-for="item in state.notifications" :key="item.id">
                <div class="timeline-item">
                  <div class="timeline-item__title">{{ item.subject }}</div>
                  <div class="timeline-item__meta">{{ formatDate(item.createdAt) }}</div>
                </div>
              </a-timeline-item>
            </a-timeline>
          </template>
          <a-empty v-else description="暂无通知" />
        </a-card>
      </a-col>
    </a-row>

    <a-row v-if="isVendor" :gutter="16">
      <a-col :span="24">
        <a-card title="我的厂商指标" :loading="state.loadingVendorMetrics">
          <template v-if="state.vendorMetrics">
            <a-row :gutter="16">
              <a-col :xs="12" :md="6">
                <a-statistic title="总订单" :value="state.vendorMetrics.totalOrders" />
              </a-col>
              <a-col :xs="12" :md="6">
                <a-statistic title="活跃订单" :value="state.vendorMetrics.activeOrders" />
              </a-col>
              <a-col :xs="12" :md="6">
                <a-statistic title="在租中" :value="state.vendorMetrics.inLeaseCount" />
              </a-col>
              <a-col :xs="12" :md="6">
                <a-statistic title="厂商 GMV (¥)" :value="formatCurrency(state.vendorMetrics.totalGmv)" />
              </a-col>
            </a-row>
            <a-divider class="section-divider" />
            <a-progress
              v-for="item in vendorStatusEntries"
              :key="item.status"
              :percent="statusPercent(item.count, state.vendorMetrics.totalOrders)"
              :format="() => `${item.status} · ${item.count}`"
            />
          </template>
          <a-empty v-else description="暂无厂商数据" />
        </a-card>
      </a-col>
    </a-row>

    <a-row v-if="isCustomer" :gutter="16">
      <a-col :span="12">
        <a-card title="最新订单">
          <template v-if="state.latestOrders.length">
            <a-list :data-source="state.latestOrders" :renderItem="renderOrder" />
          </template>
          <a-empty v-else description="暂无订单" />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="常用入口">
          <a-space direction="vertical" style="width: 100%">
            <a-button type="primary" block @click="goCatalog">浏览商品目录</a-button>
            <a-button block @click="goOrders">查看我的订单</a-button>
            <a-button block @click="goNotifications">通知中心</a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { computed, h, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../../stores/auth';
import { fetchDashboardMetrics, fetchVendorMetrics } from '../../services/analyticsService';
import { listOrders } from '../../services/orderService';
import { listNotificationLogs, type NotificationLog } from '../../services/notificationService';
import type { RentalOrderSummary } from '../../services/orderService';

const auth = useAuthStore();
const router = useRouter();

const state = reactive({
  loadingMetrics: false,
  loadingVendorMetrics: false,
  metrics: null as Awaited<ReturnType<typeof fetchDashboardMetrics>> | null,
  vendorMetrics: null as Awaited<ReturnType<typeof fetchVendorMetrics>> | null,
  latestOrders: [] as RentalOrderSummary[],
  notifications: [] as NotificationLog[]
});

const isVendor = computed(() => auth.hasRole('VENDOR'));
const isAdmin = computed(() => auth.hasRole('ADMIN'));
const isCustomer = computed(() => auth.hasRole('USER'));

const statusEntries = computed(() => {
  if (!state.metrics?.ordersByStatus) {
    return [] as Array<{ status: string; count: number }>;
  }
  return Object.entries(state.metrics.ordersByStatus)
    .map(([status, count]) => ({ status, count }))
    .sort((a, b) => b.count - a.count);
});

const vendorStatusEntries = computed(() => {
  if (!state.vendorMetrics?.ordersByStatus) {
    return [] as Array<{ status: string; count: number }>;
  }
  return Object.entries(state.vendorMetrics.ordersByStatus)
    .map(([status, count]) => ({ status, count }))
    .sort((a, b) => b.count - a.count);
});

const formatCurrency = (value: number) =>
  value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const formatDate = (value: string) => new Date(value).toLocaleString();

const statusPercent = (count: number, total: number) => {
  if (!total) {
    return 0;
  }
  return Number(((count / total) * 100).toFixed(2));
};

const renderOrder = (order: RentalOrderSummary) =>
  h(
    'a-list-item',
    { key: order.id },
    {
      default: () => [
        h('div', { class: 'order-item' }, [
          h('div', { class: 'order-item__title' }, `订单号：${order.orderNo}`),
          h('div', { class: 'order-item__meta' }, `状态：${order.status}`),
          h('div', { class: 'order-item__meta' }, `金额：¥${formatCurrency(order.totalAmount)}`)
        ])
      ]
    }
  );

const goCatalog = () => router.push({ name: 'catalog' });
const goOrders = () => router.push({ name: 'orders' });
const goNotifications = () => router.push({ name: 'notifications' });

const loadMetrics = async () => {
  state.loadingMetrics = true;
  try {
    state.metrics = await fetchDashboardMetrics();
  } catch (error) {
    console.error('Failed to load dashboard metrics', error);
  } finally {
    state.loadingMetrics = false;
  }
};

const loadVendorMetrics = async () => {
  if (!isVendor.value || !auth.user?.id) {
    return;
  }
  state.loadingVendorMetrics = true;
  try {
    state.vendorMetrics = await fetchVendorMetrics(auth.user.id);
  } catch (error) {
    console.error('Failed to load vendor metrics', error);
  } finally {
    state.loadingVendorMetrics = false;
  }
};

const loadCustomerSnapshot = async () => {
  if (!isCustomer.value || !auth.user?.id) {
    return;
  }
  try {
    const orders = await listOrders({ userId: auth.user.id, size: 5, page: 1 });
    state.latestOrders = orders.content;
  } catch (error) {
    console.error('Failed to load orders', error);
  }
};

const loadNotifications = async () => {
  try {
    const logs = await listNotificationLogs();
    state.notifications = logs.slice(0, 5);
  } catch (error) {
    console.error('Failed to load notifications', error);
  }
};

onMounted(async () => {
  await loadMetrics();
  await Promise.all([loadVendorMetrics(), loadCustomerSnapshot(), loadNotifications()]);
});
</script>

<style scoped>
.status-breakdown {
  margin-top: 24px;
}

.status-breakdown__title {
  font-weight: 600;
  margin-right: 8px;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.timeline-item__title {
  font-weight: 600;
}

.timeline-item__meta {
  color: #64748b;
  font-size: 12px;
}

.order-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.order-item__title {
  font-weight: 600;
}

.order-item__meta {
  color: #64748b;
  font-size: 12px;
}
</style>
