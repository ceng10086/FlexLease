<template>
  <div v-if="vendorReady" class="page-container">
    <div class="page-header">
      <div>
        <h2>结算中心</h2>
        <p class="page-header__meta">查看押金、租金与退款的结算情况，支撑财务对账。</p>
      </div>
      <a-button type="primary" @click="loadSettlements(true)" :loading="loading">刷新</a-button>
    </div>

    <a-card>
      <a-form layout="inline" class="filter-form" @submit.prevent>
        <a-form-item label="开始日期">
          <a-date-picker v-model:value="filters.from" format="YYYY-MM-DD" />
        </a-form-item>
        <a-form-item label="结束日期">
          <a-date-picker v-model:value="filters.to" format="YYYY-MM-DD" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="loadSettlements(true)" :loading="loading">查询</a-button>
        </a-form-item>
      </a-form>

      <a-table :data-source="records" :loading="loading" row-key="vendorId" :pagination="false">
        <a-table-column title="厂商" data-index="vendorId" key="vendorId" />
        <a-table-column title="总金额" key="totalAmount">
          <template #default="{ record }">¥{{ formatCurrency(record.totalAmount) }}</template>
        </a-table-column>
        <a-table-column title="押金" key="depositAmount">
          <template #default="{ record }">¥{{ formatCurrency(record.depositAmount) }}</template>
        </a-table-column>
        <a-table-column title="租金" key="rentAmount">
          <template #default="{ record }">¥{{ formatCurrency(record.rentAmount) }}</template>
        </a-table-column>
        <a-table-column title="退款" key="refundedAmount">
          <template #default="{ record }">¥{{ formatCurrency(record.refundedAmount) }}</template>
        </a-table-column>
        <a-table-column title="净入账" key="netAmount">
          <template #default="{ record }">¥{{ formatCurrency(record.netAmount) }}</template>
        </a-table-column>
        <a-table-column title="最后付款时间" key="lastPaidAt">
          <template #default="{ record }">{{ record.lastPaidAt ? formatDate(record.lastPaidAt) : '-' }}</template>
        </a-table-column>
        <a-table-column title="笔数" key="transactionCount">
          <template #default="{ record }">{{ record.transactionCount }}</template>
        </a-table-column>
      </a-table>
    </a-card>
  </div>

  <div v-else class="page-container">
    <a-result status="warning" title="尚未获取厂商身份">
      <template #subTitle>
        请先重新同步账户或退出后重新登录，以查看结算数据。
      </template>
      <template #extra>
        <a-space>
          <a-button type="primary" :loading="syncingVendor" @click="refreshAccount">重新同步</a-button>
        </a-space>
      </template>
    </a-result>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import dayjs, { type Dayjs } from 'dayjs';
import { useVendorContext } from '../../composables/useVendorContext';
import { listSettlements, type PaymentSettlementResponse } from '../../services/paymentService';

const loading = ref(false);
const records = ref<PaymentSettlementResponse[]>([]);
const {
  vendorId: currentVendorId,
  vendorReady,
  refreshVendorContext,
  syncingVendor
} = useVendorContext();

const filters = reactive<{ from?: Dayjs; to?: Dayjs }>({});

const formatCurrency = (value: number) => value.toFixed(2);
const formatDate = (value: string) => new Date(value).toLocaleString();

const loadSettlements = async (notify = false) => {
  const vendorId = currentVendorId.value;
  if (!vendorId) {
    records.value = [];
    if (notify) {
      message.warning('缺少厂商身份，请重新登录后重试');
    }
    return;
  }
  loading.value = true;
  try {
    records.value = await listSettlements({
      vendorId,
      from: filters.from ? dayjs(filters.from).startOf('day').toISOString() : undefined,
      to: filters.to ? dayjs(filters.to).endOf('day').toISOString() : undefined
    });
  } catch (error) {
    console.error('加载结算数据失败', error);
    message.error('加载结算失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const refreshAccount = async () => {
  await refreshVendorContext();
  if (currentVendorId.value) {
    await loadSettlements();
  }
};

watch(
  vendorReady,
  (ready) => {
    if (ready) {
      loadSettlements();
    } else {
      records.value = [];
    }
  },
  { immediate: true }
);
</script>

<style scoped>
.filter-form {
  margin-bottom: 16px;
}
</style>
