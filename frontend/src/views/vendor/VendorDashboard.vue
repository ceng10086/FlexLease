<template>
  <div class="page-wrapper">
    <a-row :gutter="16" class="metrics-row">
      <a-col v-for="metric in metrics" :key="metric.label" :xs="24" :sm="12" :md="8">
        <MetricCard :title="metric.label" :value="metric.value" :trend="metric.trend" />
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="12">
        <a-card title="履约中订单" :bordered="false">
          <a-list :data-source="orders" :loading="loading" item-layout="horizontal">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta
                  :title="item.productName"
                  :description="`${item.customerName} · ${item.mode}`"
                />
                <template #actions>
                  <a-tag :color="statusColor(item.status)">{{ item.status }}</a-tag>
                  <span class="next-step">{{ item.nextStep }}</span>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12">
        <a-card title="商品运营状态" :bordered="false">
          <a-table :columns="productColumns" :data-source="products" row-key="id" :pagination="false" size="small" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { TableColumnType } from 'ant-design-vue';
import MetricCard from '@/components/common/MetricCard.vue';
import { fetchVendorMetrics, fetchVendorOrders, fetchVendorProducts } from '@/api/vendors';
import { useAuthStore } from '@/stores/auth';
import type { VendorMetric, VendorOrderSummary, VendorProductSummary } from '@/types';
import { sampleVendorMetrics, sampleVendorOrders, sampleVendorProducts } from '@/utils/sampleData';

const auth = useAuthStore();

const metrics = ref<VendorMetric[]>([]);
const orders = ref<VendorOrderSummary[]>([]);
const products = ref<VendorProductSummary[]>([]);
const loading = ref(false);

const productColumns: TableColumnType<VendorProductSummary>[] = [
  { title: '商品', dataIndex: 'name', key: 'name' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  {
    title: '租赁模式',
    dataIndex: 'rentalModes',
    key: 'rentalModes',
    customRender: ({ record }) => record.rentalModes.join(' / ')
  },
  {
    title: '库存使用',
    key: 'inventory',
    customRender: ({ record }) => `${record.leased}/${record.totalInventory}`
  },
  { title: '待处理订单', dataIndex: 'pendingOrders', key: 'pendingOrders' }
];

function statusColor(status: string) {
  if (status.includes('待')) return 'orange';
  if (status.includes('完成')) return 'green';
  return 'blue';
}

async function loadData() {
  loading.value = true;
  try {
    const vendorId = auth.profile?.id ?? '';
    metrics.value = await fetchVendorMetrics(vendorId);
    orders.value = await fetchVendorOrders(vendorId);
    products.value = await fetchVendorProducts(vendorId);
  } catch (error) {
    console.error(error);
    metrics.value = sampleVendorMetrics;
    orders.value = sampleVendorOrders;
    products.value = sampleVendorProducts;
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.metrics-row {
  margin-bottom: 8px;
}

.next-step {
  color: var(--app-muted);
  font-size: 12px;
}
</style>
