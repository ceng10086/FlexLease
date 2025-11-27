<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>订单监控</h2>
        <p class="page-header__meta">灵活按用户、厂商或状态过滤订单，也可查看全量记录。</p>
      </div>
    </div>

    <a-card>
      <a-form
        :layout="formLayout"
        :model="filters"
        :class="['filter-form', { 'filter-form--stacked': isMobile }]"
        @submit.prevent
      >
        <a-form-item label="用户 ID">
          <a-input
            v-model:value="filters.userId"
            placeholder="可选"
            :disabled="disableUserFilter"
            :style="{ width: isMobile ? '100%' : '240px' }"
          />
        </a-form-item>
        <a-form-item label="厂商 ID">
          <a-input
            v-model:value="filters.vendorId"
            placeholder="可选"
            :disabled="disableVendorFilter"
            :style="{ width: isMobile ? '100%' : '240px' }"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-select
            v-model:value="filters.status"
            allow-clear
            :style="{ width: isMobile ? '100%' : '200px' }"
          >
            <a-select-option v-for="status in orderStatusOptions" :key="status" :value="status">
              {{ status }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item class="filter-form__actions">
          <a-space :wrap="isMobile">
            <a-button type="primary" :loading="loading" @click="loadOrders">查询</a-button>
            <a-button @click="resetFilters">重置</a-button>
          </a-space>
        </a-form-item>
      </a-form>

      <a-table
        :data-source="orders"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        :scroll="tableScroll"
        size="middle"
        @change="handleTableChange"
      >
        <a-table-column title="订单号" data-index="orderNo" key="orderNo" />
        <a-table-column title="状态" key="status">
          <template #default="{ record }">
            <a-tag>{{ record.status }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="用户" key="userId">
          <template #default="{ record }">{{ record.userId.slice(0, 8) }}</template>
        </a-table-column>
        <a-table-column title="厂商" key="vendorId">
          <template #default="{ record }">{{ record.vendorId.slice(0, 8) }}</template>
        </a-table-column>
        <a-table-column title="金额" key="totalAmount">
          <template #default="{ record }">¥{{ formatCurrency(record.totalAmount) }}</template>
        </a-table-column>
        <a-table-column title="创建时间" key="createdAt">
          <template #default="{ record }">{{ formatDate(record.createdAt) }}</template>
        </a-table-column>
        <a-table-column title="操作" key="actions">
          <template #default="{ record }">
            <a-button size="small" @click="openDetail(record.id)">详情</a-button>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <a-drawer
      v-model:open="detailDrawer.open"
      title="订单详情"
      :width="detailDrawerWidth"
      destroy-on-close
    >
      <template v-if="detailDrawer.loading">
        <a-spin />
      </template>
      <template v-else-if="detailDrawer.order">
        <a-descriptions title="基础信息" :column="descriptionsColumn" bordered size="small">
          <a-descriptions-item label="订单号">{{ detailDrawer.order.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="状态">{{ detailDrawer.order.status }}</a-descriptions-item>
          <a-descriptions-item label="用户">{{ detailDrawer.order.userId }}</a-descriptions-item>
          <a-descriptions-item label="厂商">{{ detailDrawer.order.vendorId }}</a-descriptions-item>
          <a-descriptions-item label="押金">¥{{ formatCurrency(resolveOrderDeposit(detailDrawer.order)) }}</a-descriptions-item>
          <a-descriptions-item label="租金">¥{{ formatCurrency(resolveOrderRent(detailDrawer.order)) }}</a-descriptions-item>
          <a-descriptions-item label="总金额">¥{{ formatCurrency(resolveOrderTotal(detailDrawer.order)) }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDate(detailDrawer.order.createdAt) }}</a-descriptions-item>
          <a-descriptions-item label="承运方" v-if="detailDrawer.order.shippingCarrier">{{ detailDrawer.order.shippingCarrier }}</a-descriptions-item>
          <a-descriptions-item label="运单号" v-if="detailDrawer.order.shippingTrackingNo">{{ detailDrawer.order.shippingTrackingNo }}</a-descriptions-item>
          <a-descriptions-item label="客户备注" :span="2" v-if="detailDrawer.order.customerRemark">
            {{ detailDrawer.order.customerRemark }}
          </a-descriptions-item>
        </a-descriptions>
        <a-space style="margin: 12px 0;">
          <a-button type="link" @click="contractDrawerOpen = true">查看电子合同</a-button>
        </a-space>
        <a-divider />
        <h4>租赁明细</h4>
        <a-table
          :data-source="detailDrawer.order.items"
          row-key="id"
          :pagination="false"
          size="small"
        >
          <a-table-column title="商品" data-index="productName" key="product" />
          <a-table-column title="SKU" data-index="skuCode" key="sku" />
          <a-table-column title="数量" data-index="quantity" key="quantity" />
          <a-table-column title="月租金" key="rent">
            <template #default="{ record }">¥{{ formatCurrency(resolveItemRent(record)) }}</template>
          </a-table-column>
          <a-table-column title="押金" key="deposit">
            <template #default="{ record }">¥{{ formatCurrency(resolveItemDeposit(record)) }}</template>
          </a-table-column>
        </a-table>

        <a-divider />
        <div class="admin-actions">
          <h4>管理员操作</h4>
          <a-form layout="vertical">
            <a-form-item label="关闭原因">
              <a-textarea v-model:value="forceCloseForm.reason" :rows="3" placeholder="可选" />
            </a-form-item>
            <a-button
              type="primary"
              danger
              :loading="forceCloseForm.loading"
              :disabled="!canForceClose"
              @click="handleForceClose"
            >强制关闭订单</a-button>
          </a-form>
        </div>

        <a-divider />
        <h4>纠纷与仲裁</h4>
        <a-empty v-if="!disputes.length" description="暂无纠纷" />
        <div v-else class="dispute-list">
          <div class="dispute-item" v-for="item in disputes" :key="item.id">
            <div class="dispute-item__header">
              <a-tag :color="disputeStatusColor(item.status)">{{ disputeStatusLabel(item.status) }}</a-tag>
              <span>发起人：{{ disputeActorLabel(item.initiatorRole) }}</span>
              <span>创建时间：{{ formatDate(item.createdAt) }}</span>
            </div>
            <p class="dispute-item__reason">
              <strong>诉求：</strong>{{ resolutionLabel(item.initiatorOption) }} | {{ item.initiatorReason }}
            </p>
            <p v-if="item.respondentOption" class="dispute-item__line">
              对方方案：{{ resolutionLabel(item.respondentOption) }}
              <span v-if="item.respondentRemark">（{{ item.respondentRemark }}）</span>
            </p>
            <p v-if="item.adminDecisionOption" class="dispute-item__line">
              平台裁决：{{ resolutionLabel(item.adminDecisionOption) }}
              <span v-if="item.adminDecisionRemark">（{{ item.adminDecisionRemark }}）</span>
            </p>
            <div class="dispute-meta">
              <span v-if="item.escalatedAt && item.status === 'PENDING_ADMIN'">
                升级于：{{ formatDate(item.escalatedAt) }}
              </span>
              <span v-if="item.userCreditDelta">
                信用变动：{{ item.userCreditDelta }} 分
              </span>
            </div>
            <div class="dispute-actions">
              <a-button
                size="small"
                type="primary"
                @click="openAdminDisputeModal(item)"
                :disabled="item.status === 'CLOSED'"
              >
                平台裁决
              </a-button>
            </div>
          </div>
        </div>

        <a-divider />
        <h4>操作记录</h4>
        <a-empty v-if="!detailDrawer.order.events?.length" description="暂无记录" />
        <a-timeline v-else>
          <a-timeline-item v-for="event in detailDrawer.order.events" :key="event.id">
            <div class="timeline-item">
              <strong>{{ event.eventType }}</strong>
              <span>{{ formatDate(event.createdAt) }}</span>
              <p v-if="event.description">{{ event.description }}</p>
            </div>
          </a-timeline-item>
        </a-timeline>
      </template>
      <template v-else>
        <a-empty description="未找到订单详情" />
      </template>
    </a-drawer>
    <OrderContractDrawer
      v-if="detailDrawer.order"
      v-model:open="contractDrawerOpen"
      :order-id="detailDrawer.order.id"
      :allow-sign="false"
    />
    <a-modal
      v-model:open="adminDisputeModal.open"
      title="纠纷裁决"
      ok-text="保存裁决"
      cancel-text="取消"
      :confirm-loading="adminDisputeModal.loading"
      @ok="handleAdminResolveDispute"
      @cancel="handleAdminCloseDisputeModal"
    >
      <a-form layout="vertical">
        <a-form-item label="裁决方案">
          <a-select v-model:value="adminDisputeModal.decision">
            <a-select-option value="REDELIVER">重新发货/补发</a-select-option>
            <a-select-option value="PARTIAL_REFUND">部分退款继续租赁</a-select-option>
            <a-select-option value="RETURN_WITH_DEPOSIT_DEDUCTION">退租并扣押金</a-select-option>
            <a-select-option value="DISCOUNTED_BUYOUT">优惠买断</a-select-option>
            <a-select-option value="CUSTOM">自定义方案</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="信用扣分（可为负数）">
          <a-input-number
            v-model:value="adminDisputeModal.creditDelta"
            :step="1"
            :min="-50"
            :max="50"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="adminDisputeModal.remark" :rows="3" placeholder="说明裁决原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import {
  listAdminOrders,
  fetchOrder,
  forceCloseOrder,
  resolveOrderDispute,
  type RentalOrderSummary,
  type OrderStatus,
  type RentalOrderDetail,
  type OrderDispute,
  type DisputeResolutionOption
} from '../../services/orderService';
import {
  resolveItemDeposit,
  resolveItemRent,
  rentalOrderDeposit,
  rentalOrderRent,
  rentalOrderTotal
} from '../../utils/orderAmounts';
import OrderContractDrawer from '../../components/orders/OrderContractDrawer.vue';
import { useViewport } from '../../composables/useViewport';

const orderStatusOptions: OrderStatus[] = [
  'PENDING_PAYMENT',
  'AWAITING_SHIPMENT',
  'IN_LEASE',
  'RETURN_REQUESTED',
  'RETURN_IN_PROGRESS',
  'COMPLETED',
  'BUYOUT_REQUESTED',
  'BUYOUT_COMPLETED',
  'CANCELLED',
  'EXCEPTION_CLOSED'
];

const { isMobile, width: viewportWidth } = useViewport();
const formLayout = computed(() => (isMobile.value ? 'vertical' : 'inline'));
const tableScroll = computed(() => (isMobile.value ? { x: 900 } : undefined));
const descriptionsColumn = computed(() => (isMobile.value ? 1 : 2));
const detailDrawerWidth = computed(() => {
  if (!isMobile.value) {
    return 720;
  }
  const base = viewportWidth.value || 360;
  return Math.min(Math.max(base - 32, 320), 720);
});

const filters = reactive<{ userId?: string; vendorId?: string; status?: OrderStatus }>({});
const loading = ref(false);
const orders = ref<RentalOrderSummary[]>([]);
const pagination = reactive({ current: 1, pageSize: 10, total: 0 });
const disableUserFilter = computed(() => Boolean(filters.vendorId));
const disableVendorFilter = computed(() => Boolean(filters.userId));

const detailDrawer = reactive<{ open: boolean; loading: boolean; order: RentalOrderDetail | null }>(
  {
    open: false,
    loading: false,
    order: null
  }
);

const forceCloseForm = reactive({ reason: '', loading: false });
const contractDrawerOpen = ref(false);
const adminDisputeModal = reactive({
  open: false,
  disputeId: null as string | null,
  decision: 'REDELIVER' as DisputeResolutionOption,
  creditDelta: 0,
  remark: '',
  loading: false
});

const formatCurrency = (value: number) => value.toFixed(2);
const formatDate = (value: string) => new Date(value).toLocaleString();

const canForceClose = computed(() => {
  if (!detailDrawer.order) {
    return false;
  }
  return !['COMPLETED', 'CANCELLED', 'BUYOUT_COMPLETED', 'EXCEPTION_CLOSED'].includes(detailDrawer.order.status);
});

const disputes = computed(() => detailDrawer.order?.disputes ?? []);

const resolveOrderDeposit = (detail: RentalOrderDetail): number => rentalOrderDeposit(detail);
const resolveOrderRent = (detail: RentalOrderDetail): number => rentalOrderRent(detail);
const resolveOrderTotal = (detail: RentalOrderDetail): number => rentalOrderTotal(detail);

const resolutionLabel = (option?: DisputeResolutionOption | null) =>
  option ? {
    REDELIVER: '重新发货/补发',
    PARTIAL_REFUND: '部分退款继续租赁',
    RETURN_WITH_DEPOSIT_DEDUCTION: '退租并扣押金',
    DISCOUNTED_BUYOUT: '优惠买断',
    CUSTOM: '自定义方案'
  }[option] ?? option : '未填写';

const disputeStatusLabel = (status: OrderDispute['status']) => {
  switch (status) {
    case 'OPEN':
      return '协商中';
    case 'PENDING_ADMIN':
      return '待平台处理';
    case 'RESOLVED':
      return '双方一致';
    case 'CLOSED':
      return '已结案';
    default:
      return status;
  }
};

const disputeStatusColor = (status: OrderDispute['status']) => {
  switch (status) {
    case 'OPEN':
      return 'orange';
    case 'PENDING_ADMIN':
      return 'blue';
    case 'RESOLVED':
      return 'green';
    case 'CLOSED':
      return 'red';
    default:
      return 'default';
  }
};

const disputeActorLabel = (role?: string | null) => {
  if (role === 'USER') return '消费者';
  if (role === 'VENDOR') return '厂商';
  if (role === 'ADMIN' || role === 'INTERNAL') return '平台';
  return '系统';
};

const loadOrders = async () => {
  if (filters.userId && filters.vendorId) {
    message.warning('请仅填写用户 ID 或厂商 ID 中的一项');
    return;
  }
  loading.value = true;
  try {
    const result = await listAdminOrders({
      userId: filters.userId || undefined,
      vendorId: filters.vendorId || undefined,
      status: filters.status,
      page: pagination.current,
      size: pagination.pageSize
    });
    orders.value = result.content;
    pagination.total = result.totalElements;
  } catch (error) {
    console.error('Failed to load orders', error);
    message.error('加载订单失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (pager: { current?: number; pageSize?: number }) => {
  pagination.current = pager.current ?? pagination.current;
  pagination.pageSize = pager.pageSize ?? pagination.pageSize;
  loadOrders();
};

const resetFilters = () => {
  filters.userId = undefined;
  filters.vendorId = undefined;
  filters.status = undefined;
  pagination.current = 1;
  loadOrders();
};

const openDetail = async (orderId: string) => {
  detailDrawer.open = true;
  detailDrawer.loading = true;
  try {
    detailDrawer.order = await fetchOrder(orderId);
    forceCloseForm.reason = '';
  } catch (error) {
    console.error('Failed to fetch order detail', error);
    message.error('加载订单详情失败');
    detailDrawer.order = null;
  } finally {
    detailDrawer.loading = false;
  }
};

const handleForceClose = async () => {
  if (!detailDrawer.order) {
    return;
  }
  if (!canForceClose.value) {
    message.warning('订单已处于终态');
    return;
  }
  forceCloseForm.loading = true;
  try {
    await forceCloseOrder(detailDrawer.order.id, {
      reason: forceCloseForm.reason || undefined
    });
    message.success('订单已强制关闭');
    await loadOrders();
    await openDetail(detailDrawer.order.id);
  } catch (error) {
    console.error('Force close order failed', error);
    message.error('强制关闭失败，请稍后再试');
  } finally {
    forceCloseForm.loading = false;
  }
};

const openAdminDisputeModal = (dispute: OrderDispute) => {
  adminDisputeModal.disputeId = dispute.id;
  adminDisputeModal.decision = dispute.adminDecisionOption ?? dispute.initiatorOption;
  adminDisputeModal.creditDelta = dispute.userCreditDelta ?? 0;
  adminDisputeModal.remark = dispute.adminDecisionRemark ?? '';
  adminDisputeModal.open = true;
};

const handleAdminResolveDispute = async () => {
  if (!detailDrawer.order || !adminDisputeModal.disputeId) {
    return;
  }
  adminDisputeModal.loading = true;
  try {
    await resolveOrderDispute(detailDrawer.order.id, adminDisputeModal.disputeId, {
      decision: adminDisputeModal.decision,
      penalizeUserDelta: adminDisputeModal.creditDelta,
      remark: adminDisputeModal.remark?.trim() || undefined
    });
    message.success('纠纷已结案');
    adminDisputeModal.open = false;
    await openDetail(detailDrawer.order.id);
  } catch (error) {
    console.error('纠纷裁决失败', error);
    message.error('纠纷裁决失败，请稍后重试');
  } finally {
    adminDisputeModal.loading = false;
  }
};

const handleAdminCloseDisputeModal = () => {
  adminDisputeModal.open = false;
  adminDisputeModal.disputeId = null;
};

loadOrders();

watch(
  () => detailDrawer.open,
  (open) => {
    if (!open) {
      contractDrawerOpen.value = false;
    }
  }
);
</script>

<style scoped>
.filter-form {
  margin-bottom: 16px;
}

.filter-form--stacked :deep(.ant-form-item) {
  width: 100%;
}

.filter-form__actions {
  margin-left: auto;
}

.filter-form__actions .ant-space {
  justify-content: flex-end;
}

.info-alert {
  margin-bottom: 16px;
}

.dispute-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dispute-item {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  background: #f8fafc;
}

.dispute-item__header {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #475569;
  align-items: center;
}

.dispute-item__reason {
  margin: 8px 0;
  color: #0f172a;
}

.dispute-item__line {
  margin: 4px 0;
  color: #334155;
}

.dispute-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
}

.dispute-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

@media (max-width: 768px) {
  .filter-form__actions {
    width: 100%;
  }

  .filter-form__actions .ant-space {
    width: 100%;
  }
}
</style>
