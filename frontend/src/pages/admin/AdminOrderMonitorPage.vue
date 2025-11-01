<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>订单监控</h2>
        <p class="page-header__meta">灵活按用户、厂商或状态过滤订单，也可查看全量记录。</p>
      </div>
    </div>

    <a-card>
      <a-form layout="inline" :model="filters" class="filter-form" @submit.prevent>
        <a-form-item label="用户 ID">
          <a-input v-model:value="filters.userId" placeholder="可选" style="width: 240px" />
        </a-form-item>
        <a-form-item label="厂商 ID">
          <a-input v-model:value="filters.vendorId" placeholder="可选" style="width: 240px" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="filters.status" allow-clear style="width: 200px">
            <a-select-option v-for="status in orderStatusOptions" :key="status" :value="status">
              {{ status }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-space>
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
      :width="720"
      destroy-on-close
    >
      <template v-if="detailDrawer.loading">
        <a-spin />
      </template>
      <template v-else-if="detailDrawer.order">
        <a-descriptions title="基础信息" :column="2" bordered size="small">
          <a-descriptions-item label="订单号">{{ detailDrawer.order.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="状态">{{ detailDrawer.order.status }}</a-descriptions-item>
          <a-descriptions-item label="用户">{{ detailDrawer.order.userId }}</a-descriptions-item>
          <a-descriptions-item label="厂商">{{ detailDrawer.order.vendorId }}</a-descriptions-item>
          <a-descriptions-item label="押金">¥{{ formatCurrency(detailDrawer.order.depositAmount) }}</a-descriptions-item>
          <a-descriptions-item label="租金">¥{{ formatCurrency(detailDrawer.order.rentAmount) }}</a-descriptions-item>
          <a-descriptions-item label="总金额">¥{{ formatCurrency(detailDrawer.order.totalAmount) }}</a-descriptions-item>
          <a-descriptions-item label="创建时间">{{ formatDate(detailDrawer.order.createdAt) }}</a-descriptions-item>
        </a-descriptions>
        <a-divider />
        <h4>租赁明细</h4>
        <a-table
          :data-source="detailDrawer.order.orderItems"
          row-key="id"
          :pagination="false"
          size="small"
        >
          <a-table-column title="商品" data-index="productName" key="product" />
          <a-table-column title="SKU" data-index="skuCode" key="sku" />
          <a-table-column title="数量" data-index="quantity" key="quantity" />
          <a-table-column title="月租金" key="rent">
            <template #default="{ record }">¥{{ formatCurrency(record.unitRentAmount) }}</template>
          </a-table-column>
          <a-table-column title="押金" key="deposit">
            <template #default="{ record }">¥{{ formatCurrency(record.unitDepositAmount) }}</template>
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
      </template>
      <template v-else>
        <a-empty description="未找到订单详情" />
      </template>
    </a-drawer>
  </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import {
  listAdminOrders,
  fetchOrder,
  forceCloseOrder,
  type RentalOrderSummary,
  type OrderStatus
} from '../../services/orderService';

const orderStatusOptions: OrderStatus[] = [
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

const filters = reactive<{ userId?: string; vendorId?: string; status?: OrderStatus }>({});
const loading = ref(false);
const orders = ref<RentalOrderSummary[]>([]);
const pagination = reactive({ current: 1, pageSize: 10, total: 0 });

const detailDrawer = reactive<{ open: boolean; loading: boolean; order: any }>(
  {
    open: false,
    loading: false,
    order: null
  }
);

const forceCloseForm = reactive({ reason: '', loading: false });


const formatCurrency = (value: number) => value.toFixed(2);
const formatDate = (value: string) => new Date(value).toLocaleString();

const canForceClose = computed(() => {
  if (!detailDrawer.order) {
    return false;
  }
  return !['COMPLETED', 'CANCELLED', 'BUYOUT_COMPLETED'].includes(detailDrawer.order.status);
});

const loadOrders = async () => {
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

loadOrders();
</script>

<style scoped>
.filter-form {
  margin-bottom: 16px;
}

.info-alert {
  margin-bottom: 16px;
}
</style>
