<template>
  <div class="page-wrapper">
    <a-card title="资金结算" :bordered="false">
      <a-table :columns="columns" :data-source="settlements" :loading="loading" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'gross'">
            {{ formatCurrency(record.grossAmount) }}
          </template>
          <template v-else-if="column.key === 'refund'">
            {{ formatCurrency(record.refundAmount) }}
          </template>
          <template v-else-if="column.key === 'net'">
            {{ formatCurrency(record.netAmount) }}
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="record.status === '已结算' ? 'green' : 'blue'">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link">查看明细</a-button>
              <a-button type="link">生成对账单</a-button>
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
import { fetchSettlements } from '@/api/payments';
import type { SettlementRecord } from '@/types';
import { sampleSettlements } from '@/utils/sampleData';
import { formatCurrency } from '@/utils/formatters';

const columns: TableColumnType<SettlementRecord>[] = [
  { title: '结算单号', dataIndex: 'id', key: 'id' },
  { title: '厂商', dataIndex: 'vendorName', key: 'vendorName' },
  { title: '结算周期', dataIndex: 'period', key: 'period' },
  { title: '订单数', dataIndex: 'orderCount', key: 'orderCount' },
  { title: '总计', dataIndex: 'grossAmount', key: 'gross' },
  { title: '退款', dataIndex: 'refundAmount', key: 'refund' },
  { title: '净入账', dataIndex: 'netAmount', key: 'net' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' }
];

const settlements = ref<SettlementRecord[]>([]);
const loading = ref(false);

onMounted(async () => {
  loading.value = true;
  try {
    settlements.value = await fetchSettlements();
  } catch (error) {
    console.error(error);
    settlements.value = sampleSettlements;
  } finally {
    loading.value = false;
  }
});
</script>

<style scoped>
.page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
