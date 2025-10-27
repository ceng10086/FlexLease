<template>
  <div class="page-wrapper">
    <a-card title="订单协同" :bordered="false">
      <a-table :columns="columns" :data-source="orders" :loading="loading" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'customer'">
            <div class="customer">{{ record.customerName }}</div>
            <div class="mode">{{ record.mode }}</div>
          </template>
          <template v-else-if="column.key === 'value'">
            {{ formatCurrency(record.value) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link">发货</a-button>
              <a-button type="link">审批</a-button>
              <a-button type="link">备注</a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { TableColumnType } from 'ant-design-vue';
import { formatCurrency } from '@/utils/formatters';
import { fetchVendorOrders } from '@/api/vendors';
import { useAuthStore } from '@/stores/auth';
import type { VendorOrderSummary } from '@/types';
import { sampleVendorOrders } from '@/utils/sampleData';

const auth = useAuthStore();

const columns: TableColumnType<VendorOrderSummary>[] = [
  { title: '订单号', dataIndex: 'id', key: 'id' },
  { title: '客户', dataIndex: 'customer', key: 'customer' },
  { title: '商品', dataIndex: 'productName', key: 'productName' },
  { title: '发货时间', dataIndex: 'shipBy', key: 'shipBy' },
  { title: '订单金额', dataIndex: 'value', key: 'value' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' }
];

const orders = ref<VendorOrderSummary[]>([]);
const loading = ref(false);

function statusColor(status: string) {
  if (status.includes('待')) return 'orange';
  if (status.includes('完成')) return 'green';
  return 'blue';
}

async function loadOrders() {
  loading.value = true;
  try {
    const vendorId = auth.profile?.id ?? '';
    orders.value = await fetchVendorOrders(vendorId);
  } catch (error) {
    console.error(error);
    orders.value = sampleVendorOrders;
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

.customer {
  font-weight: 600;
}

.mode {
  color: var(--app-muted);
  font-size: 12px;
}
</style>
