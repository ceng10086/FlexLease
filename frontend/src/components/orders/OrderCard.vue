<template>
  <article class="order-card">
    <header class="order-card__header">
      <div>
        <span class="order-card__no">订单号：{{ order.orderNo }}</span>
        <a-tag :color="statusColor">{{ statusLabel }}</a-tag>
      </div>
      <small>{{ new Date(order.createdAt).toLocaleString() }}</small>
    </header>
    <div class="order-card__body">
      <div>
        <p class="order-card__vendor">厂商：{{ order.vendorId }}</p>
        <p class="order-card__meta">状态：{{ statusLabel }}</p>
      </div>
      <div class="order-card__amount">
        <span>应付</span>
        <strong>¥{{ order.totalAmount.toFixed(2) }}</strong>
      </div>
    </div>
    <footer class="order-card__footer">
      <a-button type="link" @click="$emit('open', order.id)">查看详情</a-button>
    </footer>
  </article>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type { RentalOrderSummary } from '../../services/orderService';
import { orderStatusLabel, orderStatusColor } from '../../utils/orderStatus';

const props = defineProps<{
  order: RentalOrderSummary;
}>();

defineEmits<{
  (e: 'open', orderId: string): void;
}>();

const statusLabel = computed(() => orderStatusLabel(props.order.status));
const statusColor = computed(() => orderStatusColor(props.order.status));
</script>

<style scoped>
.order-card {
  padding: var(--space-4);
  border-radius: var(--radius-card);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.order-card__header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: var(--space-2);
}

.order-card__no {
  font-weight: 600;
}

.order-card__body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-card__vendor,
.order-card__meta {
  margin: 0;
  color: var(--color-text-secondary);
}

.order-card__amount {
  text-align: right;
}

.order-card__amount strong {
  font-size: var(--font-size-title-lg);
  color: var(--color-primary);
  line-height: 1.4;
}

.order-card__footer {
  text-align: right;
}
</style>
