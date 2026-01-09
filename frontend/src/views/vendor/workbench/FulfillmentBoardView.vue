<template>
  <PageSection title="履约任务墙" description="按状态卡片化展示全部订单，快速进入聊天/凭证抽屉完成发货与退租。">
    <template #actions>
      <a-space>
        <a-switch v-model:checked="manualReviewOnly" @change="reload" />
        <span>仅查看预警</span>
        <a-button @click="reload" :loading="loading">刷新</a-button>
      </a-space>
    </template>
    <div class="filter-row">
      <a-segmented v-model:value="statusFilter" :options="statusOptions" @change="reload" />
    </div>
    <div v-if="loading && !orders.length">
      <a-skeleton active :paragraph="{ rows: 4 }" />
    </div>
    <div v-else-if="orders.length" class="order-grid">
      <VendorOrderCard v-for="item in orders" :key="item.id" :order="item" @open="openDetail" />
    </div>
    <DataStateBlock v-else title="暂无订单" description="当前没有履约中的订单，等待新订单进入。" />
    <div class="load-more" v-if="orders.length">
      <a-button v-if="hasMore" :loading="loading" @click="loadMore">加载更多</a-button>
      <span v-else class="text-muted">已经到底啦</span>
    </div>
  </PageSection>

  <FulfillmentDetailSheet
    :open="detail.open"
    :vendor-id="vendorId"
    :order-id="detail.orderId"
    @close="closeDetail"
    @refresh-list="reload"
  />
</template>

<script lang="ts" setup>
// 履约中台：厂商处理发货、续租/退租/买断审批、取证与纠纷响应。
import { computed, reactive, ref, watch } from 'vue';
import PageSection from '../../../components/layout/PageSection.vue';
import DataStateBlock from '../../../components/feedback/DataStateBlock.vue';
import VendorOrderCard from '../../../components/vendor/VendorOrderCard.vue';
import FulfillmentDetailSheet from './FulfillmentDetailSheet.vue';
import { useVendorContext } from '../../../composables/useVendorContext';
import { listOrders, type OrderStatus, type RentalOrderSummary } from '../../../services/orderService';
import { friendlyErrorMessage } from '../../../utils/error';
import { message } from 'ant-design-vue';

const { vendorId } = useVendorContext();

const orders = ref<RentalOrderSummary[]>([]);
const loading = ref(false);
const statusFilter = ref<OrderStatus | 'ALL'>('ALL');
const manualReviewOnly = ref(false);
const pagination = reactive({ page: 1, size: 6, totalElements: 0, totalPages: 1 });
const detail = reactive<{ open: boolean; orderId: string | null }>({ open: false, orderId: null });

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '待发货', value: 'AWAITING_SHIPMENT' },
  { label: '履约中', value: 'IN_LEASE' },
  { label: '退租审批', value: 'RETURN_REQUESTED' },
  { label: '退租中', value: 'RETURN_IN_PROGRESS' },
  { label: '买断申请', value: 'BUYOUT_REQUESTED' }
];

const hasMore = computed(() => pagination.page < pagination.totalPages);

const fetchOrders = async (reset = false) => {
  if (!vendorId.value) {
    return;
  }
  loading.value = true;
  try {
    if (reset) {
      pagination.page = 1;
      orders.value = [];
    }
    const response = await listOrders({
      vendorId: vendorId.value,
      status: statusFilter.value === 'ALL' ? undefined : statusFilter.value,
      manualReviewOnly: manualReviewOnly.value || undefined,
      page: pagination.page,
      size: pagination.size
    });
    pagination.totalElements = response.totalElements;
    pagination.totalPages = response.totalPages || 1;
    orders.value = reset ? response.content : [...orders.value, ...response.content];
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载订单失败'));
  } finally {
    loading.value = false;
  }
};

const reload = () => fetchOrders(true);

const loadMore = () => {
  if (!hasMore.value || loading.value) {
    return;
  }
  pagination.page += 1;
  fetchOrders(false);
};

const openDetail = (orderId: string) => {
  detail.orderId = orderId;
  detail.open = true;
};

const closeDetail = () => {
  detail.open = false;
};

watch(
  () => vendorId.value,
  (id) => {
    if (id) {
      fetchOrders(true);
    }
  },
  { immediate: true }
);
</script>

<style scoped>
.filter-row {
  margin-bottom: var(--space-4);
  overflow-x: auto;
}

.filter-row :deep(.ant-segmented) {
  max-width: 100%;
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--space-4);
}

.load-more {
  margin-top: var(--space-4);
  text-align: center;
}
</style>
