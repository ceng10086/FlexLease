<template>
  <div class="page-wrapper">
    <a-card title="商品管理" :bordered="false">
      <a-space style="margin-bottom: 16px;">
        <a-button type="primary">新建商品</a-button>
        <a-button>导出报表</a-button>
      </a-space>
      <a-table :columns="columns" :data-source="products" :loading="loading" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <div class="product-name">{{ record.name }}</div>
            <div class="product-modes">{{ record.rentalModes.join(' / ') }}</div>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'inventory'">
            {{ record.leased }}/{{ record.totalInventory }}
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link">编辑</a-button>
              <a-button type="link">上架</a-button>
              <a-button type="link">库存调整</a-button>
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
import { useAuthStore } from '@/stores/auth';
import { fetchVendorProducts } from '@/api/vendors';
import type { VendorProductSummary } from '@/types';
import { sampleVendorProducts } from '@/utils/sampleData';

const auth = useAuthStore();

const columns: TableColumnType<VendorProductSummary>[] = [
  { title: '商品', dataIndex: 'name', key: 'name' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '租赁模式', dataIndex: 'rentalModes', key: 'rentalModes' },
  { title: '库存使用', dataIndex: 'inventory', key: 'inventory' },
  { title: '待处理订单', dataIndex: 'pendingOrders', key: 'pendingOrders' },
  { title: '操作', key: 'action' }
];

const products = ref<VendorProductSummary[]>([]);
const loading = ref(false);

function statusColor(status: string) {
  if (status.includes('待')) return 'orange';
  if (status.includes('上架')) return 'green';
  return 'blue';
}

async function loadProducts() {
  loading.value = true;
  try {
    const vendorId = auth.profile?.id ?? '';
    products.value = await fetchVendorProducts(vendorId);
  } catch (error) {
    console.error(error);
    products.value = sampleVendorProducts;
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadProducts();
});
</script>

<style scoped>
.page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-name {
  font-weight: 600;
}

.product-modes {
  color: var(--app-muted);
  font-size: 12px;
}
</style>
