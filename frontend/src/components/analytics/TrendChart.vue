<template>
  <div v-if="hasData" class="trend-chart">
    <svg :viewBox="viewBox" preserveAspectRatio="none" role="img" aria-label="运营趋势图">
      <polyline
        v-if="orderPolyline"
        :points="orderPolyline"
        class="trend-chart__line trend-chart__line--orders"
        fill="none"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
      <polyline
        v-if="gmvPolyline"
        :points="gmvPolyline"
        class="trend-chart__line trend-chart__line--gmv"
        fill="none"
        stroke-linecap="round"
        stroke-linejoin="round"
      />
    </svg>
    <div class="trend-chart__legend">
      <span><span class="trend-chart__dot trend-chart__dot--gmv" /> GMV</span>
      <span><span class="trend-chart__dot trend-chart__dot--orders" /> 订单数</span>
    </div>
    <div class="trend-chart__axis">
      <span v-for="label in axisLabels" :key="label">{{ label }}</span>
    </div>
  </div>
  <div v-else class="trend-chart__empty">暂无趋势数据</div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type { TrendPoint } from '../../services/analyticsService';

// 轻量 SVG 趋势图：同时绘制 GMV 与订单数两条折线，用于驾驶舱的 7 日趋势展示。
const props = defineProps<{
  data: TrendPoint[];
  height?: number;
}>();

const normalizedData = computed(() => props.data ?? []);
const chartHeight = computed(() => props.height ?? 160);
const hasData = computed(() => normalizedData.value.length > 0);
const baseWidth = 240;
const width = computed(() => {
  if (!hasData.value) {
    return baseWidth;
  }
  if (normalizedData.value.length <= 1) {
    return baseWidth;
  }
  return Math.max((normalizedData.value.length - 1) * 80, baseWidth);
});
const padding = 12;
const usableHeight = computed(() => chartHeight.value - padding * 2);
const viewBox = computed(() => `0 0 ${width.value} ${chartHeight.value}`);

const buildPolyline = (key: 'orders' | 'gmv') => {
  if (!hasData.value) {
    return '';
  }
  const maxValue = Math.max(...normalizedData.value.map((item) => item[key] ?? 0), 0);
  const dataLength = normalizedData.value.length;
  const horizontalGap = dataLength <= 1 ? 0 : (width.value - padding * 2) / (dataLength - 1);
  return normalizedData.value
    .map((item, index) => {
      const rawValue = item[key] ?? 0;
      const ratio = maxValue === 0 ? 0 : rawValue / maxValue;
      const x =
        dataLength === 1 ? width.value / 2 : padding + Number((index * horizontalGap).toFixed(2));
      const y = Number((chartHeight.value - padding - ratio * usableHeight.value).toFixed(2));
      return `${x},${y}`;
    })
    .join(' ');
};

const gmvPolyline = computed(() => buildPolyline('gmv'));
const orderPolyline = computed(() => buildPolyline('orders'));
const axisLabels = computed(() =>
  normalizedData.value.map((item) => {
    if (!item.date) {
      return '';
    }
    return item.date.slice(5);
  })
);
</script>

<style scoped>
.trend-chart {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

svg {
  width: 100%;
  height: 160px;
  background: linear-gradient(180deg, rgba(226, 232, 240, 0.45), rgba(255, 255, 255, 0));
  border-radius: 8px;
}

.trend-chart__line {
  stroke-width: 3px;
}

.trend-chart__line--gmv {
  stroke: #5b8ff9;
}

.trend-chart__line--orders {
  stroke: #5ad8a6;
}

.trend-chart__legend {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #475569;
}

.trend-chart__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  display: inline-block;
  margin-right: 6px;
}

.trend-chart__dot--gmv {
  background-color: #5b8ff9;
}

.trend-chart__dot--orders {
  background-color: #5ad8a6;
}

.trend-chart__axis {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #94a3b8;
}

.trend-chart__empty {
  padding: 32px 0;
  text-align: center;
  color: #94a3b8;
  border: 1px dashed #cbd5f5;
  border-radius: 6px;
  background-color: #f8fafc;
}
</style>
