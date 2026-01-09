<template>
  <div v-if="entries.length" class="plan-breakdown">
    <div v-for="entry in entries" :key="entry.planType" class="plan-breakdown__item">
      <div class="plan-breakdown__header">
        <span>{{ entry.label }}</span>
        <span>{{ entry.percent }}%</span>
      </div>
      <a-progress :percent="entry.percent" :stroke-color="entry.color" :show-info="false" />
      <div class="plan-breakdown__meta">
        <span>订单 {{ entry.orders }}</span>
        <span>GMV ¥{{ formatCurrency(entry.gmv) }}</span>
      </div>
    </div>
  </div>
  <div v-else class="plan-breakdown__empty">暂无模式数据</div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type { PlanBreakdownEntry } from '../../services/analyticsService';

// 驾驶舱/洞察页使用：把不同租赁模式的订单数占比可视化（进度条 + GMV）。
const PLAN_LABELS: Record<string, string> = {
  STANDARD: '标准方案',
  RENT_TO_OWN: '先租后买',
  LEASE_TO_SALE: '租售结合',
  UNKNOWN: '未分类'
};

const props = defineProps<{
  data: PlanBreakdownEntry[];
}>();

const totalOrders = computed(() =>
  (props.data ?? []).reduce((sum, entry) => sum + (entry.orders ?? 0), 0)
);

const colors = ['#5b8ff9', '#61d9a8', '#f6bd16', '#7262fd', '#ff9d4d'];

const entries = computed(() =>
  (props.data ?? []).map((entry, index) => {
    const percent =
      totalOrders.value === 0
        ? 0
        : Number((((entry.orders ?? 0) / totalOrders.value) * 100).toFixed(1));
    const label = PLAN_LABELS[entry.planType] ?? entry.planType ?? '未分类';
    return {
      ...entry,
      percent,
      label,
      color: colors[index % colors.length]
    };
  })
);

const formatCurrency = (value: number) =>
  (value ?? 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
</script>

<style scoped>
.plan-breakdown {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.plan-breakdown__item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.plan-breakdown__header {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
}

.plan-breakdown__meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #475569;
}

.plan-breakdown__empty {
  padding: 24px 0;
  text-align: center;
  color: #94a3b8;
  background-color: #f8fafc;
  border: 1px dashed #cbd5f5;
  border-radius: 6px;
}
</style>
