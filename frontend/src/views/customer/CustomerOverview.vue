<template>
  <div class="page-wrapper">
    <a-row :gutter="16" class="metrics-row">
      <a-col v-for="metric in metrics" :key="metric.title" :xs="24" :sm="12" :md="8">
        <MetricCard :title="metric.title" :value="metric.value" :trend="metric.trend" :trend-label="metric.trendLabel" />
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="16">
        <a-card title="在租订单跟进" :bordered="false">
          <a-timeline v-if="orders.length">
            <a-timeline-item
              v-for="order in orders"
              :key="order.id"
              :color="timelineColor(order)"
            >
              <div class="timeline-item">
                <div class="timeline-title">{{ order.productName }} · {{ order.planName }}</div>
                <div class="timeline-meta">
                  <span>状态：{{ order.status }}</span>
                  <span>下一步：{{ order.nextAction || '暂无待办' }}</span>
                </div>
              </div>
            </a-timeline-item>
          </a-timeline>
          <EmptyState v-else description="暂无在租订单，快去浏览商品吧" action-label="前往商品目录" @action="goCatalog" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="租赁提醒" :bordered="false">
          <a-list :data-source="reminders" :split="false">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :title="item.title" :description="item.description" />
                <template #actions>
                  <a-tag :color="item.level === 'warning' ? 'orange' : 'blue'">{{ item.levelLabel }}</a-tag>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import MetricCard from '@/components/common/MetricCard.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import { fetchDashboardMetrics } from '@/api/analytics';
import { fetchOrdersByUser } from '@/api/orders';
import { useAuthStore } from '@/stores/auth';
import type { DashboardMetric, RentalOrderSummary } from '@/types';
import { sampleCustomerOrders } from '@/utils/sampleData';

const auth = useAuthStore();
const router = useRouter();

const metrics = ref<DashboardMetric[]>([]);
const orders = ref<RentalOrderSummary[]>([]);

const reminders = computed(() =>
  orders.value.map((order) => ({
    title: `${order.productName} · ${order.mode}`,
    description: order.nextAction ?? `预计${order.endDate}结束，记得安排续租或退租`,
    level: order.status.includes('待') ? 'warning' : 'info',
    levelLabel: order.status.includes('待') ? '待处理' : '正常'
  }))
);

function timelineColor(order: RentalOrderSummary) {
  if (order.status.includes('待')) return 'orange';
  if (order.status.includes('异常')) return 'red';
  return 'blue';
}

async function loadData() {
  metrics.value = await fetchDashboardMetrics();
  if (auth.profile?.id) {
    const response = await fetchOrdersByUser(auth.profile.id);
    orders.value = response.content;
  } else {
    orders.value = sampleCustomerOrders;
  }
}

function goCatalog() {
  router.push('/customer/catalog');
}

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metrics-row {
  margin-bottom: 8px;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.timeline-title {
  font-weight: 600;
}

.timeline-meta {
  display: flex;
  flex-direction: column;
  color: var(--app-muted);
  font-size: 13px;
}
</style>
