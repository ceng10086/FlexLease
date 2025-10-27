<template>
  <div class="page-wrapper">
    <a-card title="商品审核" :bordered="false">
      <a-space style="margin-bottom: 16px;">
        <a-button type="primary">批量通过</a-button>
        <a-button>导出审核结果</a-button>
      </a-space>
      <a-table :columns="columns" :data-source="reviews" :loading="loading" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'modes'">
            {{ record.rentalModes.join(' / ') }}
          </template>
          <template v-else-if="column.key === 'risk'">
            <a-progress :percent="record.riskScore" :steps="10" stroke-color="#ff4d4f" :show-info="false" />
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link">通过</a-button>
              <a-button type="link">驳回</a-button>
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
import { fetchProductReviews } from '@/api/admin';
import type { AdminProductReview } from '@/types';
import { sampleAdminReviews } from '@/utils/sampleData';

const columns: TableColumnType<AdminProductReview>[] = [
  { title: '厂商', dataIndex: 'vendorName', key: 'vendorName' },
  { title: '商品', dataIndex: 'productName', key: 'productName' },
  { title: '租赁模式', dataIndex: 'rentalModes', key: 'modes' },
  { title: '提交时间', dataIndex: 'submittedAt', key: 'submittedAt' },
  { title: '风控评分', dataIndex: 'riskScore', key: 'risk' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '操作', key: 'action' }
];

const reviews = ref<AdminProductReview[]>([]);
const loading = ref(false);

function statusColor(status: string) {
  if (status.includes('待')) return 'orange';
  if (status.includes('复审')) return 'purple';
  if (status.includes('已')) return 'green';
  return 'blue';
}

onMounted(async () => {
  loading.value = true;
  try {
    reviews.value = await fetchProductReviews();
  } catch (error) {
    console.error(error);
    reviews.value = sampleAdminReviews;
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
