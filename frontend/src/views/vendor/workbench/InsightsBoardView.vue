<template>
  <div class="insights-grid">
    <PageSection title="运营指标" description="GMV、活跃订单与退租积压一目了然。">
      <div class="stat-grid">
        <div class="stat-card">
          <p>累计 GMV</p>
          <strong>¥{{ formatCurrency(metrics?.totalGmv ?? 0) }}</strong>
        </div>
        <div class="stat-card">
          <p>活跃订单</p>
          <strong>{{ metrics?.activeOrders ?? 0 }}</strong>
        </div>
        <div class="stat-card">
          <p>待退租</p>
          <strong>{{ metrics?.pendingReturns ?? 0 }}</strong>
        </div>
        <div class="stat-card">
          <p>信用档位</p>
          <strong>{{ vendorProfile?.commissionProfile?.creditTier ?? '--' }}</strong>
        </div>
      </div>
    </PageSection>
    <PageSection title="7 日趋势">
      <TrendChart :data="metrics?.recentTrend ?? []" />
    </PageSection>
    <PageSection title="方案构成">
      <PlanBreakdownCard :data="metrics?.planBreakdown ?? []" />
    </PageSection>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue';
import PageSection from '../../../components/layout/PageSection.vue';
import TrendChart from '../../../components/analytics/TrendChart.vue';
import PlanBreakdownCard from '../../../components/analytics/PlanBreakdownCard.vue';
import { fetchVendorMetrics, type VendorMetrics } from '../../../services/analyticsService';
import { useVendorContext } from '../../../composables/useVendorContext';
import { useVendorWorkbench } from '../../../composables/useVendorWorkbench';
import { message } from 'ant-design-vue';
import { friendlyErrorMessage } from '../../../utils/error';
import { formatCurrency } from '../../../utils/number';

const { vendorId } = useVendorContext();
const { vendor: vendorProfile } = useVendorWorkbench();
const metrics = ref<VendorMetrics | null>(null);

const loadMetrics = async () => {
  if (!vendorId.value) {
    metrics.value = null;
    return;
  }
  try {
    metrics.value = await fetchVendorMetrics(vendorId.value);
  } catch (error) {
    metrics.value = null;
    message.error(friendlyErrorMessage(error, '加载指标失败'));
  }
};

watch(
  () => vendorId.value,
  (id) => {
    if (id) {
      loadMetrics();
    }
  },
  { immediate: true }
);
</script>

<style scoped>
.insights-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--space-4);
}

.stat-card {
  padding: var(--space-4);
  border-radius: var(--radius-card);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: var(--color-surface);
}

.stat-card p {
  margin: 0;
  color: var(--color-text-secondary);
}

.stat-card strong {
  font-size: var(--font-size-title-lg);
}
</style>
