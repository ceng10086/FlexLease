<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>订单履约</h2>
        <p class="page-header__meta">查看并处理租赁订单，完成发货、退租审批等关键动作。</p>
      </div>
      <a-space>
        <a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 200px" @change="loadOrders">
          <a-select-option v-for="status in vendorStatuses" :key="status" :value="status">{{ status }}</a-select-option>
        </a-select>
        <a-button type="primary" @click="loadOrders" :loading="loading">刷新</a-button>
      </a-space>
    </div>

    <a-card>
      <a-table :data-source="orders" :loading="loading" row-key="id" :pagination="pagination" @change="handleTableChange">
        <a-table-column title="订单号" data-index="orderNo" key="orderNo" />
        <a-table-column title="状态" key="status">
          <template #default="{ record }"><a-tag>{{ record.status }}</a-tag></template>
        </a-table-column>
        <a-table-column title="金额" key="totalAmount">
          <template #default="{ record }">¥{{ formatCurrency(record.totalAmount) }}</template>
        </a-table-column>
        <a-table-column title="创建时间" key="createdAt">
          <template #default="{ record }">{{ formatDate(record.createdAt) }}</template>
        </a-table-column>
        <a-table-column title="操作" key="actions">
          <template #default="{ record }">
            <a-button size="small" @click="openDetail(record.id)">处理</a-button>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detail.open" :width="840" title="订单详情" destroy-on-close>
      <a-spin :spinning="detail.loading">
        <template v-if="detail.order">
          <a-descriptions title="基础信息" :column="2" bordered size="small">
            <a-descriptions-item label="订单号">{{ detail.order.orderNo }}</a-descriptions-item>
            <a-descriptions-item label="状态">{{ detail.order.status }}</a-descriptions-item>
            <a-descriptions-item label="用户">{{ detail.order.userId }}</a-descriptions-item>
            <a-descriptions-item label="总金额">¥{{ formatCurrency(detail.order.totalAmount) }}</a-descriptions-item>
            <a-descriptions-item label="押金">¥{{ formatCurrency(detail.order.depositAmount) }}</a-descriptions-item>
            <a-descriptions-item label="租金">¥{{ formatCurrency(detail.order.rentAmount) }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatDate(detail.order.createdAt) }}</a-descriptions-item>
          </a-descriptions>

          <a-divider />
          <h4>明细</h4>
          <a-table :data-source="detail.order.items" :pagination="false" size="small" row-key="id">
            <a-table-column title="商品" data-index="productName" key="product" />
            <a-table-column title="SKU" data-index="skuCode" key="sku" />
            <a-table-column title="数量" data-index="quantity" key="quantity" />
            <a-table-column title="月租金" key="rent"><template #default="{ record }">¥{{ formatCurrency(record.unitRentAmount) }}</template></a-table-column>
          </a-table>

          <a-divider />
          <div class="detail__actions">
            <div class="action-box">
              <h5>发货</h5>
              <a-form layout="vertical">
                <a-form-item label="承运方" required><a-input v-model:value="shipForm.carrier" placeholder="如 SF" /></a-form-item>
                <a-form-item label="运单号" required><a-input v-model:value="shipForm.trackingNumber" placeholder="物流单号" /></a-form-item>
                <a-button type="primary" :loading="shipForm.loading" @click="handleShip">提交发货</a-button>
              </a-form>
            </div>
            <div class="action-box">
              <h5>退租审批</h5>
              <a-form layout="vertical">
                <a-form-item label="审批意见">
                  <a-textarea v-model:value="returnForm.remark" :rows="3" placeholder="可选" />
                </a-form-item>
                <a-space>
                  <a-button type="primary" danger :loading="returnForm.loading" @click="handleReturnDecision(false)">拒绝退租</a-button>
                  <a-button type="primary" :loading="returnForm.loading" @click="handleReturnDecision(true)">确认退租</a-button>
                </a-space>
              </a-form>
            </div>
          </div>

          <a-divider />
          <h5>操作记录</h5>
          <a-empty v-if="!detail.order.events?.length" description="暂无记录" />
          <a-timeline v-else>
            <a-timeline-item v-for="event in detail.order.events" :key="event.id">
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
      </a-spin>
    </a-drawer>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../../stores/auth';
import {
  listOrders,
  fetchOrder,
  shipOrder,
  decideOrderReturn,
  type OrderStatus,
  type RentalOrderSummary,
  type RentalOrderDetail
} from '../../services/orderService';

const vendorStatuses: OrderStatus[] = [
  'PENDING_PAYMENT',
  'AWAITING_SHIPMENT',
  'IN_LEASE',
  'RETURN_REQUESTED',
  'RETURN_IN_PROGRESS',
  'COMPLETED',
  'BUYOUT_REQUESTED',
  'BUYOUT_COMPLETED',
  'CANCELLED'
];

const auth = useAuthStore();
const vendorId = () => {
  const id = auth.vendorId ?? auth.user?.id;
  if (!id) {
    throw new Error('未获取到厂商账号');
  }
  return id;
};

const loading = ref(false);
const orders = ref<RentalOrderSummary[]>([]);
const filters = reactive<{ status?: OrderStatus }>({});
const pagination = reactive({ current: 1, pageSize: 10, total: 0 });

const detail = reactive<{ open: boolean; loading: boolean; order: RentalOrderDetail | null }>(
  {
    open: false,
    loading: false,
    order: null
  }
);

const shipForm = reactive({ carrier: '', trackingNumber: '', loading: false });
const returnForm = reactive({ remark: '', loading: false });

const formatCurrency = (value: number) => value.toFixed(2);
const formatDate = (value: string) => new Date(value).toLocaleString();

const loadOrders = async () => {
  loading.value = true;
  try {
    const result = await listOrders({
      vendorId: vendorId(),
      status: filters.status,
      page: pagination.current,
      size: pagination.pageSize
    });
    orders.value = result.content;
    pagination.total = result.totalElements;
  } catch (error) {
    console.error('加载订单失败', error);
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

const openDetail = async (orderId: string) => {
  detail.open = true;
  detail.loading = true;
  try {
    detail.order = await fetchOrder(orderId);
    shipForm.carrier = detail.order?.shippingCarrier ?? '';
    shipForm.trackingNumber = detail.order?.shippingTrackingNo ?? '';
  } catch (error) {
    console.error('加载订单详情失败', error);
    message.error('加载详情失败');
    detail.order = null;
  } finally {
    detail.loading = false;
  }
};

const refreshDetail = async () => {
  if (detail.order?.id) {
    await openDetail(detail.order.id);
  }
};

const handleShip = async () => {
  if (!detail.order) {
    return;
  }
  if (!shipForm.carrier || !shipForm.trackingNumber) {
    message.warning('请填写承运方与运单号');
    return;
  }
  shipForm.loading = true;
  try {
    await shipOrder(detail.order.id, {
      vendorId: vendorId(),
      carrier: shipForm.carrier,
      trackingNumber: shipForm.trackingNumber
    });
    message.success('发货信息已提交');
    await refreshDetail();
    await loadOrders();
  } catch (error) {
    console.error('发货失败', error);
    message.error('发货失败，请稍后重试');
  } finally {
    shipForm.loading = false;
  }
};

const handleReturnDecision = async (approve: boolean) => {
  if (!detail.order) {
    return;
  }
  returnForm.loading = true;
  try {
    await decideOrderReturn(detail.order.id, {
      vendorId: vendorId(),
      approve,
      remark: returnForm.remark || (approve ? '同意退租' : '拒绝退租')
    });
    message.success(approve ? '已确认退租' : '已驳回退租');
    await refreshDetail();
    await loadOrders();
  } catch (error) {
    console.error('退租审批失败', error);
    message.error('退租审批失败，请稍后重试');
  } finally {
    returnForm.loading = false;
  }
};

loadOrders();
</script>

<style scoped>
.detail__actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.action-box {
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
</style>
