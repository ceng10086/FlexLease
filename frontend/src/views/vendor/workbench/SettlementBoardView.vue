<template>
  <PageSection title="结算中心" description="按支付/退款时间窗口查询押金、租金与平台抽成拆分。">
    <div class="filter-row">
      <a-range-picker v-model:value="range" @change="loadSettlements" />
      <a-button @click="loadSettlements" :loading="loading">查询</a-button>
    </div>
    <div v-if="loading && !settlements.length">
      <a-skeleton active :paragraph="{ rows: 3 }" />
    </div>
    <div v-else-if="settlements.length" class="settlement-grid">
      <div v-for="item in settlements" :key="item.vendorId + item.lastPaidAt" class="settlement-card">
        <div class="settlement-card__header">
          <strong>{{ item.lastPaidAt ? new Date(item.lastPaidAt).toLocaleString() : '批次' }}</strong>
          <span>流水：{{ item.transactionCount }}</span>
        </div>
        <p>总金额：¥{{ formatCurrency(item.totalAmount) }}</p>
        <p>押金：¥{{ formatCurrency(item.depositAmount) }} · 租金：¥{{ formatCurrency(item.rentAmount) }}</p>
        <p>平台抽成：¥{{ formatCurrency(item.platformCommissionAmount) }}</p>
        <p>已退款：¥{{ formatCurrency(item.refundedAmount) }}</p>
        <p class="settlement-card__net">净入账：¥{{ formatCurrency(item.netAmount) }}</p>
      </div>
    </div>
    <DataStateBlock v-else title="暂无结算数据" description="选择时间范围后可查询历史结算。" />
  </PageSection>
</template>

<script lang="ts" setup>
// 结算中心：展示押金/租金/退款/平台抽成等拆分统计与结算明细。
import { ref, watch } from 'vue';
import type { Dayjs } from 'dayjs';
import PageSection from '../../../components/layout/PageSection.vue';
import DataStateBlock from '../../../components/feedback/DataStateBlock.vue';
import { listSettlements, type PaymentSettlementResponse } from '../../../services/paymentService';
import { useVendorContext } from '../../../composables/useVendorContext';
import { message } from 'ant-design-vue';
import { friendlyErrorMessage } from '../../../utils/error';
import { formatCurrency } from '../../../utils/number';

const { vendorId } = useVendorContext();
const settlements = ref<PaymentSettlementResponse[]>([]);
const loading = ref(false);
const range = ref<[Dayjs, Dayjs] | null>(null);

const loadSettlements = async () => {
  if (!vendorId.value) {
    return;
  }
  loading.value = true;
  try {
    const [from, to] = range.value ?? [];
    settlements.value = await listSettlements({
      vendorId: vendorId.value,
      from: from ? from.toISOString() : undefined,
      to: to ? to.toISOString() : undefined
    });
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载结算失败'));
  } finally {
    loading.value = false;
  }
};

watch(
  () => vendorId.value,
  (id) => {
    if (id) {
      loadSettlements();
    }
  },
  { immediate: true }
);
</script>

<style scoped>
.filter-row {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
  margin-bottom: var(--space-4);
}

.settlement-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: var(--space-4);
}

.settlement-card {
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: var(--radius-card);
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.settlement-card__header {
  display: flex;
  justify-content: space-between;
}

.settlement-card__net {
  font-weight: 600;
  color: var(--color-primary);
}
</style>
