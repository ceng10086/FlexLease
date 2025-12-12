<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="订单时间线"
        eyebrow="Orders"
        description="卡片式订单墙，按状态快速筛选并进入聊天/凭证子页。"
      >
        <template #actions>
          <div class="status-tabs">
            <a-segmented
              v-model:value="activeStatus"
              :options="statusOptions"
              @change="reload"
            />
          </div>
        </template>
      </PageHeader>
    </template>
    <PageSection>
      <div class="order-grid">
        <OrderCard v-for="order in orders" :key="order.id" :order="order" @open="openOrder" />
      </div>
      <div class="order-footer">
        <a-button v-if="hasMore" :loading="loading" @click="loadMore">查看更多</a-button>
        <span v-else class="text-muted">没有更多订单了</span>
      </div>
    </PageSection>
  </PageShell>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import PageSection from '../../components/layout/PageSection.vue';
import OrderCard from '../../components/orders/OrderCard.vue';
import { useAuthStore } from '../../stores/auth';
import { listOrders, type OrderStatus, type RentalOrderSummary } from '../../services/orderService';
import { orderStatusLabel } from '../../utils/orderStatus';
import { friendlyErrorMessage } from '../../utils/error';

const auth = useAuthStore();
const router = useRouter();

const orders = ref<RentalOrderSummary[]>([]);
const pagination = reactive({ page: 1, size: 6, total: 0 });
const activeStatus = ref<OrderStatus | 'ALL'>('ALL');
const loading = ref(false);

const statusOrder: OrderStatus[] = [
  'PENDING_PAYMENT',
  'AWAITING_SHIPMENT',
  'AWAITING_RECEIPT',
  'IN_LEASE',
  'RETURN_REQUESTED',
  'RETURN_IN_PROGRESS',
  'BUYOUT_REQUESTED',
  'BUYOUT_COMPLETED',
  'COMPLETED',
  'CANCELLED',
  'EXCEPTION_CLOSED'
];

const statusOptions = [
  { label: '全部', value: 'ALL' },
  ...statusOrder.map((status) => ({
    label: orderStatusLabel(status),
    value: status
  }))
];

const hasMore = computed(() => orders.value.length < pagination.total);

const fetchOrders = async (reset = false) => {
  if (!auth.user) {
    return;
  }
  loading.value = true;
  try {
    if (reset) {
      pagination.page = 1;
      orders.value = [];
    }
    const response = await listOrders({
      userId: auth.user.id,
      status: activeStatus.value === 'ALL' ? undefined : (activeStatus.value as OrderStatus),
      page: pagination.page,
      size: pagination.size
    });
    pagination.total = response.totalElements;
    orders.value = reset ? response.content : [...orders.value, ...response.content];
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载订单失败'));
  } finally {
    loading.value = false;
  }
};

const loadMore = async () => {
  if (loading.value || !hasMore.value) {
    return;
  }
  pagination.page += 1;
  await fetchOrders(false);
};

const reload = () => {
  fetchOrders(true);
};

const openOrder = (orderId: string) => {
  router.push({ name: 'order-overview', params: { orderId } });
};

fetchOrders(true);
</script>

<style scoped>
.order-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--space-4);
}

.order-footer {
  text-align: center;
  margin-top: var(--space-4);
}

.status-tabs {
  max-width: 100%;
  overflow-x: auto;
  padding-bottom: 4px;
}
</style>
