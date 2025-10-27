<template>
  <div class="page-wrapper">
    <a-card title="厂商入驻管理" :bordered="false">
      <a-space style="margin-bottom: 16px;">
        <a-button type="primary">批量审批</a-button>
        <a-button>导出列表</a-button>
      </a-space>
      <a-table :columns="columns" :data-source="applications" :loading="loading" row-key="id">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="statusColor(record.status)">{{ record.status }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a-button type="link">查看资料</a-button>
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
import { fetchVendorApplications } from '@/api/vendors';
import type { VendorApplicationSummary } from '@/types';
import { sampleVendorApplications } from '@/utils/sampleData';

const columns: TableColumnType<VendorApplicationSummary>[] = [
  { title: '企业名称', dataIndex: 'companyName', key: 'companyName' },
  { title: '联系方式', dataIndex: 'contact', key: 'contact' },
  { title: '提交时间', dataIndex: 'submittedAt', key: 'submittedAt' },
  { title: '状态', dataIndex: 'status', key: 'status' },
  { title: '备注', dataIndex: 'remark', key: 'remark' },
  { title: '操作', key: 'action' }
];

const applications = ref<VendorApplicationSummary[]>([]);
const loading = ref(false);

function statusColor(status: string) {
  if (status.includes('待')) return 'orange';
  if (status.includes('补充')) return 'purple';
  if (status.includes('通过')) return 'green';
  return 'blue';
}

onMounted(async () => {
  loading.value = true;
  try {
    applications.value = await fetchVendorApplications();
  } catch (error) {
    console.error(error);
    applications.value = sampleVendorApplications;
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
