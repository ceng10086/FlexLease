<template>
  <div class="page-wrapper">
    <a-card title="我的租赁订单" :bordered="false">
      <a-table :columns="columns" :data-source="orders" :loading="loading" row-key="id" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'product'">
            <div class="table-product">
              <div class="product-name">{{ record.productName }}</div>
              <div class="product-plan">{{ record.planName }}</div>
            </div>
          </template>
          <template v-else-if="column.key === 'amount'">
            {{ formatCurrency(record.amountDue) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link" @click="viewDetail(record)">查看进度</a-button>
              <a-button type="link">续租</a-button>
              <a-button type="link">退租</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="drawerVisible" :title="selectedOrder?.productName" width="520">
      <template v-if="selectedOrder">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="租赁方案">{{ selectedOrder.planName }}</a-descriptions-item>
          <a-descriptions-item label="租赁模式">{{ selectedOrder.mode }}</a-descriptions-item>
          <a-descriptions-item label="订单状态">{{ selectedOrder.status }}</a-descriptions-item>
          <a-descriptions-item label="起止时间">{{ selectedOrder.startDate }} 至 {{ selectedOrder.endDate }}</a-descriptions-item>
          <a-descriptions-item label="应付金额">{{ formatCurrency(selectedOrder.amountDue) }}</a-descriptions-item>
        </a-descriptions>
        <a-divider>履约进度</a-divider>
        <a-steps direction="vertical">
          <a-step
            v-for="step in selectedOrder.timeline"
            :key="step.label"
            :title="step.label"
            :description="step.time"
            :status="step.status === 'completed' ? 'finish' : step.status === 'warning' ? 'error' : 'wait'"
          />
        </a-steps>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { TableColumnType } from 'ant-design-vue';
import { useAuthStore } from '@/stores/auth';
import { fetchOrdersByUser } from '@/api/orders';
import { formatCurrency } from '@/utils/formatters';
import type { RentalOrderSummary } from '@/types';
import { sampleCustomerOrders } from '@/utils/sampleData';

const auth = useAuthStore();

const columns: TableColumnType<RentalOrderSummary>[] = [
  { title: '商品信息', dataIndex: 'product', key: 'product' },
  { title: '租赁模式', dataIndex: 'mode', key: 'mode' },
  { title: '租期', dataIndex: 'period', key: 'period', customRender: ({ record }) => `${record.startDate} 至 ${record.endDate}` },
  { title: '应付金额', dataIndex: 'amountDue', key: 'amount' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' }
];

const orders = ref<RentalOrderSummary[]>([]);
const loading = ref(false);
const drawerVisible = ref(false);
const selectedOrder = ref<RentalOrderSummary | null>(null);

function statusColor(status: string) {
  if (status.includes('待')) return 'orange';
  if (status.includes('完成')) return 'green';
  return 'blue';
}

async function loadOrders() {
  loading.value = true;
  try {
    if (auth.profile?.id) {
      const response = await fetchOrdersByUser(auth.profile.id);
      orders.value = response.content;
    } else {
      orders.value = sampleCustomerOrders;
    }
  } finally {
    loading.value = false;
  }
}

function viewDetail(order: RentalOrderSummary) {
  selectedOrder.value = order;
  drawerVisible.value = true;
}

onMounted(() => {
  loadOrders();
});
</script>

<style scoped>
.page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.table-product {
  display: flex;
  flex-direction: column;
}

.product-name {
  font-weight: 600;
}

.product-plan {
  color: var(--app-muted);
  font-size: 12px;
}
</style>
