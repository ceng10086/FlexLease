<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="订单监控"
        description="按用户/厂商/状态过滤平台订单，快速进入抽屉查看聊天、凭证与纠纷。"
        eyebrow="Admin"
      />
    </template>
    <PageSection title="筛选">
      <a-form layout="vertical" class="filter-grid">
        <a-form-item label="用户 ID">
          <a-input v-model:value="filters.userId" placeholder="可选" />
        </a-form-item>
        <a-form-item label="厂商 ID">
          <a-input v-model:value="filters.vendorId" placeholder="可选" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="filters.status" allow-clear>
            <a-select-option v-for="option in statusOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="仅人工审核">
          <a-switch v-model:checked="filters.manualReviewOnly" />
        </a-form-item>
        <a-form-item>
          <a-space>
            <a-button type="primary" :loading="loading" @click="reload">查询</a-button>
            <a-button @click="resetFilters">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>
      <div v-if="loading && !orders.length">
        <a-skeleton active :paragraph="{ rows: 4 }" />
      </div>
      <div v-else-if="orders.length" class="order-grid">
        <div v-for="order in orders" :key="order.id" class="admin-card">
          <header>
            <div>
              <span class="order-id">{{ order.orderNo }}</span>
              <a-tag :color="statusColor(order.status)">{{ statusLabel(order.status) }}</a-tag>
            </div>
            <a-tag v-if="order.requiresManualReview" color="orange">需人工</a-tag>
          </header>
          <p>用户：{{ order.userId.slice(0, 8) }}</p>
          <p>厂商：{{ order.vendorId.slice(0, 8) }}</p>
          <p>金额：¥{{ order.totalAmount.toFixed(2) }}</p>
          <div class="card-footer">
            <a-button type="link" @click="openDetail(order.id)">查看</a-button>
          </div>
        </div>
      </div>
      <DataStateBlock v-else title="暂无订单" description="调整筛选条件或稍后重试。" />
      <div class="load-more" v-if="orders.length">
        <a-button v-if="hasMore" :loading="loading" @click="loadMore">加载更多</a-button>
        <span v-else class="text-muted">没有更多了</span>
      </div>
    </PageSection>
  </PageShell>

  <AdminOrderDetailDrawer :open="detail.open" :order-id="detail.orderId" @close="closeDetail" @refresh="reload" />
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import PageSection from '../../components/layout/PageSection.vue';
import DataStateBlock from '../../components/feedback/DataStateBlock.vue';
import AdminOrderDetailDrawer from './admin/AdminOrderDetailDrawer.vue';
import { listAdminOrders, type OrderStatus, type RentalOrderSummary } from '../../services/orderService';
import { orderStatusLabel, orderStatusColor } from '../../utils/orderStatus';
import { message } from 'ant-design-vue';
import { friendlyErrorMessage } from '../../utils/error';

const orders = ref<RentalOrderSummary[]>([]);
const loading = ref(false);
const pagination = reactive({ page: 1, size: 8, total: 0 });
const filters = reactive<{ userId?: string; vendorId?: string; status?: OrderStatus | null; manualReviewOnly: boolean }>(
  { userId: '', vendorId: '', status: null, manualReviewOnly: false }
);
const detail = reactive<{ open: boolean; orderId: string | null }>({ open: false, orderId: null });

const statusOptions = [
  { label: '待支付', value: 'PENDING_PAYMENT' },
  { label: '待发货', value: 'AWAITING_SHIPMENT' },
  { label: '待收货', value: 'AWAITING_RECEIPT' },
  { label: '履约中', value: 'IN_LEASE' },
  { label: '退租审批', value: 'RETURN_REQUESTED' },
  { label: '退租中', value: 'RETURN_IN_PROGRESS' },
  { label: '买断申请', value: 'BUYOUT_REQUESTED' },
  { label: '买断完成', value: 'BUYOUT_COMPLETED' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
  { label: '异常关闭', value: 'EXCEPTION_CLOSED' }
];

const hasMore = computed(() => orders.value.length < pagination.total);

const fetchOrders = async (append = false) => {
  loading.value = true;
  try {
    const response = await listAdminOrders({
      userId: filters.userId || undefined,
      vendorId: filters.vendorId || undefined,
      status: filters.status || undefined,
      manualReviewOnly: filters.manualReviewOnly,
      page: pagination.page,
      size: pagination.size
    });
    pagination.total = response.totalElements;
    orders.value = append ? [...orders.value, ...response.content] : response.content;
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载订单失败'));
  } finally {
    loading.value = false;
  }
};

const reload = () => {
  pagination.page = 1;
  fetchOrders(false);
};

const loadMore = () => {
  if (!hasMore.value || loading.value) {
    return;
  }
  pagination.page += 1;
  fetchOrders(true);
};

const resetFilters = () => {
  filters.userId = '';
  filters.vendorId = '';
  filters.status = null;
  filters.manualReviewOnly = false;
  reload();
};

const openDetail = (orderId: string) => {
  detail.orderId = orderId;
  detail.open = true;
};

const closeDetail = () => {
  detail.open = false;
};

const statusLabel = (status: OrderStatus) => orderStatusLabel(status);
const statusColor = (status: OrderStatus) => orderStatusColor(status);

fetchOrders();
</script>

<style scoped>
.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: var(--space-3);
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--space-4);
  margin-top: var(--space-4);
}

.admin-card {
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: var(--radius-card);
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.admin-card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-id {
  font-weight: 600;
  margin-right: var(--space-2);
}

.card-footer {
  text-align: right;
}

.load-more {
  margin-top: var(--space-4);
  text-align: center;
}
</style>
