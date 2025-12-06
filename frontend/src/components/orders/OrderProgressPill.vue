<template>
  <span class="progress-pill" :class="`progress-pill--${color}`">{{ label }}</span>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type { OrderStatus } from '../../services/orderService';
import { orderStatusLabel, orderStatusColor } from '../../utils/orderStatus';

const props = defineProps<{
  status: OrderStatus;
}>();

const label = computed(() => orderStatusLabel(props.status));
const color = computed(() => orderStatusColor(props.status));
</script>

<style scoped>
.progress-pill {
  padding: 4px 12px;
  border-radius: 999px;
  font-weight: 600;
  font-size: var(--font-size-caption);
  background: var(--color-surface-muted);
  border: 1px solid var(--color-border);
}

.progress-pill--success {
  background: rgba(16, 185, 129, 0.1);
  border-color: rgba(16, 185, 129, 0.3);
  color: #0f9c68;
}

.progress-pill--processing {
  background: rgba(37, 99, 235, 0.1);
  border-color: rgba(37, 99, 235, 0.3);
  color: #1d4ed8;
}

.progress-pill--warning {
  background: rgba(249, 115, 22, 0.15);
  border-color: rgba(249, 115, 22, 0.3);
  color: #c05621;
}

.progress-pill--purple {
  background: rgba(129, 140, 248, 0.2);
  border-color: rgba(129, 140, 248, 0.3);
  color: #4338ca;
}
</style>
