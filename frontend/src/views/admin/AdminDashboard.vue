<template>
  <div class="page-wrapper">
    <a-row :gutter="16" class="metrics-row">
      <a-col v-for="metric in metrics" :key="metric.title" :xs="24" :sm="12" :md="8">
        <MetricCard :title="metric.title" :value="metric.value" :trend="metric.trend" :trend-label="metric.trendLabel" />
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="12">
        <a-card title="待审核商品" :bordered="false">
          <a-table :columns="productColumns" :data-source="reviews" :pagination="false" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12">
        <a-card title="资金结算概览" :bordered="false">
          <a-table :columns="settlementColumns" :data-source="settlements" :pagination="false" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'gross'">
                {{ formatCurrency(record.grossAmount) }}
              </template>
              <template v-else-if="column.key === 'net'">
                {{ formatCurrency(record.netAmount) }}
              </template>
              <template v-else-if="column.key === 'status'">
                <a-tag :color="record.status === '已结算' ? 'green' : 'blue'">{{ record.status }}</a-tag>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import type { TableColumnType } from 'ant-design-vue';
import MetricCard from '@/components/common/MetricCard.vue';
import { fetchDashboardMetrics } from '@/api/analytics';
import { fetchSettlements } from '@/api/payments';
import { fetchProductReviews } from '@/api/admin';
import type { AdminProductReview, DashboardMetric, SettlementRecord } from '@/types';
import { sampleAdminReviews, sampleSettlements } from '@/utils/sampleData';
import { formatCurrency } from '@/utils/formatters';

const metrics = ref<DashboardMetric[]>([]);
const reviews = ref<AdminProductReview[]>(sampleAdminReviews);
const settlements = ref<SettlementRecord[]>(sampleSettlements);

const productColumns: TableColumnType<AdminProductReview>[] = [
  { title: '厂商', dataIndex: 'vendorName', key: 'vendorName' },
  { title: '商品', dataIndex: 'productName', key: 'productName' },
  { title: '租赁模式', dataIndex: 'rentalModes', key: 'rentalModes', customRender: ({ record }) => record.rentalModes.join(' / ') },
  { title: '提交时间', dataIndex: 'submittedAt', key: 'submittedAt' },
  { title: '状态', dataIndex: 'status', key: 'status' }
];

const settlementColumns: TableColumnType<SettlementRecord>[] = [
  { title: '厂商', dataIndex: 'vendorName', key: 'vendorName' },
  { title: '周期', dataIndex: 'period', key: 'period' },
  { title: '订单数', dataIndex: 'orderCount', key: 'orderCount' },
  { title: '总金额', dataIndex: 'grossAmount', key: 'gross' },
  { title: '净入账', dataIndex: 'netAmount', key: 'net' },
  { title: '状态', dataIndex: 'status', key: 'status' }
];

function statusColor(status: string) {
  if (status.includes('待')) return 'orange';
  if (status.includes('复审')) return 'purple';
  if (status.includes('已')) return 'green';
  return 'blue';
}

onMounted(async () => {
  metrics.value = await fetchDashboardMetrics();
  try {
    settlements.value = await fetchSettlements();
    reviews.value = await fetchProductReviews('PENDING');
  } catch (error) {
    console.error(error);
  }
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
</style>
