<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="沟通中心"
        eyebrow="Vendor Chat"
        description="集中查看需要回复的订单消息，直接打开履约抽屉进行聊天、凭证或审批。"
      >
        <template #actions>
          <a-button size="small" :loading="loading" @click="refresh">刷新</a-button>
        </template>
      </PageHeader>
    </template>

    <template v-if="vendorReady">
      <PageSection>
        <div class="conversation-summary">
          <span>当前需跟进 {{ pagination.total }} 单</span>
          <span>最近刷新：{{ lastUpdatedLabel }}</span>
        </div>

        <a-list
          :data-source="orders"
          :loading="loading"
          bordered
          :locale="{ emptyText: '暂无待沟通订单' }"
          :pagination="false"
        >
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta
                :title="`订单号：${item.orderNo}`"
                :description="`状态：${orderStatusLabel(item.status)} · 创建于 ${formatDate(item.createdAt)}`"
              />
              <a-tag :color="orderStatusColor(item.status)">{{ orderStatusLabel(item.status) }}</a-tag>
              <a-button type="link" @click="openDrawer(item.id)">打开聊天</a-button>
            </a-list-item>
          </template>
        </a-list>

        <div class="conversation-pagination" v-if="pagination.total > pagination.size">
          <a-pagination
            :current="pagination.page"
            :total="pagination.total"
            :page-size="pagination.size"
            :show-size-changer="true"
            :page-size-options="['10', '20', '30', '50']"
            @change="handlePageChange"
            @showSizeChange="handlePageSizeChange"
          />
        </div>
      </PageSection>
    </template>
    <DataStateBlock
      v-else
      title="尚未绑定厂商身份"
      description="退出重新登录或点击下方按钮同步，会刷新登录态以获取 vendorId。"
    >
      <a-button type="primary" :loading="syncingVendor" @click="refreshVendorContext()">刷新账号信息</a-button>
    </DataStateBlock>

    <FulfillmentDetailSheet
      :open="detail.open"
      :order-id="detail.orderId"
      :vendor-id="vendorId"
      @close="closeDrawer"
      @refresh-list="refresh"
    />
  </PageShell>
</template>

<script lang="ts" setup>
// 厂商沟通中心：汇总需要跟进的订单，快速进入聊天/取证/审批抽屉。
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import PageSection from '../../components/layout/PageSection.vue';
import DataStateBlock from '../../components/feedback/DataStateBlock.vue';
import { useVendorContext } from '../../composables/useVendorContext';
import FulfillmentDetailSheet from './workbench/FulfillmentDetailSheet.vue';
import { listOrders, type RentalOrderSummary } from '../../services/orderService';
import { orderStatusLabel, orderStatusColor } from '../../utils/orderStatus';
import { friendlyErrorMessage } from '../../utils/error';

const { vendorId, vendorReady, refreshVendorContext, syncingVendor } = useVendorContext();

const orders = ref<RentalOrderSummary[]>([]);
const loading = ref(false);
const pagination = reactive({ page: 1, size: 20, total: 0 });
const lastUpdated = ref<number | null>(null);
const detail = reactive<{ open: boolean; orderId: string | null }>({ open: false, orderId: null });
const refreshInterval = 30_000;
let intervalId: ReturnType<typeof setInterval> | null = null;

const lastUpdatedLabel = computed(() => {
  if (!lastUpdated.value) {
    return '尚未刷新';
  }
  return new Date(lastUpdated.value).toLocaleTimeString();
});

const formatDate = (value: string) => {
  return new Date(value).toLocaleString();
};

const loadOrders = async (page = pagination.page, size = pagination.size) => {
  if (!vendorId.value) {
    orders.value = [];
    pagination.page = 1;
    pagination.total = 0;
    return;
  }
  loading.value = true;
  try {
    const response = await listOrders({
      vendorId: vendorId.value,
      page,
      size
    });
    orders.value = response.content;
    pagination.page = response.page ?? page;
    pagination.size = response.size ?? size;
    pagination.total = response.totalElements ?? response.content.length;
    lastUpdated.value = Date.now();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载订单失败'));
  } finally {
    loading.value = false;
  }
};

const refresh = () => {
  pagination.page = 1;
  loadOrders(1, pagination.size);
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  loadOrders(page, pagination.size);
};

const handlePageSizeChange = (_current: number, size: number) => {
  pagination.page = 1;
  pagination.size = size;
  loadOrders(1, size);
};

const startPolling = () => {
  stopPolling();
  intervalId = setInterval(() => {
    loadOrders(pagination.page, pagination.size);
  }, refreshInterval);
};

const stopPolling = () => {
  if (intervalId) {
    clearInterval(intervalId);
    intervalId = null;
  }
};

const openDrawer = (orderId: string) => {
  detail.orderId = orderId;
  detail.open = true;
};

const closeDrawer = () => {
  detail.open = false;
  detail.orderId = null;
};

watch(
  () => vendorId.value,
  (id) => {
    if (id) {
      refresh();
      startPolling();
    } else {
      stopPolling();
      orders.value = [];
      pagination.page = 1;
      pagination.total = 0;
      lastUpdated.value = null;
    }
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  stopPolling();
});
</script>

<style scoped>
.conversation-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
  color: var(--color-text-secondary);
  flex-wrap: wrap;
  row-gap: 4px;
}

.conversation-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-3);
}

@media (max-width: 640px) {
  .conversation-pagination {
    justify-content: center;
  }
}
</style>
