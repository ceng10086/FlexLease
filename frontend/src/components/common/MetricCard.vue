<template>
  <a-card :bordered="false" class="metric-card">
    <div class="metric-title">{{ title }}</div>
    <div class="metric-value">{{ value }}</div>
    <div v-if="trend !== undefined" class="metric-trend">
      <a-tag :color="trendColor">{{ trendPrefix }}{{ Math.abs(trend).toFixed(1) }}% {{ trendLabel }}</a-tag>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { trendColor as resolveTrendColor } from '@/utils/formatters';

const props = defineProps<{
  title: string;
  value: string | number;
  trend?: number;
  trendLabel?: string;
}>();

const trendColor = computed(() => resolveTrendColor(props.trend));
const trendPrefix = computed(() => (props.trend ?? 0) >= 0 ? '+' : '-');
</script>

<style scoped>
.metric-card {
  border-radius: var(--app-border-radius);
  box-shadow: 0 6px 24px rgba(15, 23, 42, 0.06);
}

.metric-title {
  font-size: 15px;
  color: var(--app-muted);
}

.metric-value {
  font-size: 28px;
  font-weight: 600;
  margin: 12px 0;
}

.metric-trend {
  font-size: 12px;
}
</style>
