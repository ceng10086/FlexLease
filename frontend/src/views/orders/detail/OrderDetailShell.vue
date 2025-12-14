<template>
  <PageShell>
    <template #header>
      <div class="detail-header">
        <div>
          <p class="detail-header__eyebrow">订单号 {{ order?.orderNo ?? '加载中' }}</p>
          <h1>{{ headerTitle }}</h1>
          <OrderProgressPill v-if="order" :status="order.status" />
        </div>
        <div class="detail-header__meta">
          <div>
            <span>应付总额</span>
            <strong>¥{{ displayCurrency(order?.totalAmount) }}</strong>
          </div>
          <div>
            <span>押金</span>
            <strong>¥{{ displayCurrency(order?.depositAmount) }}</strong>
          </div>
        </div>
      </div>
      <a-tabs :active-key="activeTab" @change="handleTabChange">
        <a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.label" />
      </a-tabs>
    </template>
    <router-view v-if="order" />
    <DataStateBlock
      v-else-if="!loading"
      type="error"
      title="未找到订单"
      description="请返回订单列表重试。"
    />
    <div v-else class="detail-loading">
      <a-spin tip="加载订单..." />
    </div>
  </PageShell>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import PageShell from '../../../components/layout/PageShell.vue';
import OrderProgressPill from '../../../components/orders/OrderProgressPill.vue';
import DataStateBlock from '../../../components/feedback/DataStateBlock.vue';
import { fetchOrder, type RentalOrderDetail } from '../../../services/orderService';
import { friendlyErrorMessage } from '../../../utils/error';
import { message } from 'ant-design-vue';
import { provideOrderDetail } from '../../../composables/useOrderDetail';
import { formatCurrency } from '../../../utils/number';

const route = useRoute();
const router = useRouter();
const order = ref<RentalOrderDetail | null>(null);
const loading = ref(false);

const tabs = [
  { key: 'order-overview', label: '概览' },
  { key: 'order-chat', label: '聊天' },
  { key: 'order-proofs', label: '凭证' },
  { key: 'order-timeline', label: '时间线' }
];

const activeTab = computed(() => route.name as string);
const loadOrder = async () => {
  const orderId = route.params.orderId as string | undefined;
  if (!orderId) {
    order.value = null;
    return;
  }
  loading.value = true;
  try {
    order.value = await fetchOrder(orderId);
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载订单失败'));
    order.value = null;
  } finally {
    loading.value = false;
  }
};

watch(
  () => route.params.orderId,
  () => loadOrder(),
  { immediate: true }
);

const headerTitle = computed(() => {
  if (!order.value) {
    return '订单详情';
  }
  return `厂商 ${order.value.vendorId}`;
});

const handleTabChange = (key: string) => {
  if (activeTab.value === key) {
    return;
  }
  router.push({ name: key, params: { orderId: route.params.orderId } });
};

provideOrderDetail({
  order: () => order.value,
  loading: () => loading.value,
  refresh: loadOrder,
  updateOrder: (detail) => {
    order.value = detail;
  }
});

const displayCurrency = (value: number | null | undefined) =>
  value == null ? '--' : formatCurrency(value);
</script>

<style scoped>
.detail-header {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
}

.detail-header__eyebrow {
  margin: 0;
  text-transform: uppercase;
  font-size: 11px;
  letter-spacing: 0.08em;
  color: var(--color-text-secondary);
}

.detail-header__meta {
  display: flex;
  gap: var(--space-4);
}

.detail-header__meta strong {
  display: block;
  font-size: var(--font-size-title-lg);
  color: var(--color-primary);
  line-height: 1.4;
}

.detail-header h1 {
  line-height: 1.4;
}

.detail-loading {
  display: flex;
  justify-content: center;
  padding: var(--space-5);
}
</style>
