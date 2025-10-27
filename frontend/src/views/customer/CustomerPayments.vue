<template>
  <div class="page-wrapper">
    <a-card :bordered="false" title="支付与账单">
      <a-space style="margin-bottom: 16px;">
        <span>选择订单：</span>
        <a-select v-model:value="selectedOrderId" style="width: 240px;" placeholder="请选择订单" @change="loadPayments">
          <a-select-option v-for="order in orders" :key="order.id" :value="order.id">
            {{ order.productName }} · {{ order.mode }}
          </a-select-option>
        </a-select>
      </a-space>
      <a-table :columns="columns" :data-source="payments" :loading="loading" row-key="id" :pagination="false">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'amount'">
            {{ formatCurrency(record.amount) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { TableColumnType } from 'ant-design-vue';
import { fetchOrdersByUser } from '@/api/orders';
import { fetchPaymentRecords } from '@/api/payments';
import { useAuthStore } from '@/stores/auth';
import { formatCurrency } from '@/utils/formatters';
import type { PaymentRecord, RentalOrderSummary } from '@/types';
import { sampleCustomerOrders } from '@/utils/sampleData';

const auth = useAuthStore();

const orders = ref<RentalOrderSummary[]>([]);
const payments = ref<PaymentRecord[]>([]);
const selectedOrderId = ref<string | undefined>();
const loading = ref(false);

const columns: TableColumnType<PaymentRecord>[] = [
  { title: '支付单号', dataIndex: 'id', key: 'id' },
  { title: '支付金额', dataIndex: 'amount', key: 'amount' },
  { title: '支付方式', dataIndex: 'method', key: 'method' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '时间', dataIndex: 'createdAt', key: 'createdAt' }
];

function statusColor(status: string) {
  if (status.includes('成功') || status.includes('已')) return 'green';
  if (status.includes('待')) return 'orange';
  return 'blue';
}

async function loadOrders() {
  if (auth.profile?.id) {
    const response = await fetchOrdersByUser(auth.profile.id);
    orders.value = response.content;
  } else {
    orders.value = sampleCustomerOrders;
  }
  selectedOrderId.value = orders.value[0]?.id;
  await loadPayments();
}

async function loadPayments() {
  if (!selectedOrderId.value) {
    payments.value = [];
    return;
  }
  loading.value = true;
  try {
    payments.value = await fetchPaymentRecords(selectedOrderId.value);
  } finally {
    loading.value = false;
  }
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
</style>
