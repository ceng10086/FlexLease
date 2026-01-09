<template>
  <article class="vendor-order-card">
    <header>
      <div>
        <span class="order-id">{{ order.orderNo }}</span>
        <a-tag :color="statusColor">{{ statusLabel }}</a-tag>
      </div>
      <a-tag v-if="order.requiresManualReview" color="orange">需审核</a-tag>
    </header>
    <div class="card-body">
      <div>
        <p>用户：{{ order.userId.slice(0, 8) }}</p>
        <p>创建于 {{ new Date(order.createdAt).toLocaleString() }}</p>
      </div>
      <div class="amount">
        <small>应付</small>
        <strong>¥{{ order.totalAmount.toFixed(2) }}</strong>
      </div>
    </div>
    <footer>
      <a-button type="link" @click="$emit('open', order.id)">进入工作台</a-button>
    </footer>
  </article>
</template>

<script lang="ts" setup>
// 厂商订单卡片：在履约中台以卡片方式展示订单关键信息与状态提示。
import { computed } from 'vue';
import type { RentalOrderSummary } from '../../services/orderService';
import { orderStatusColor, orderStatusLabel } from '../../utils/orderStatus';

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
.vendor-order-card {
  padding: var(--space-4);
  border-radius: var(--radius-card);
  border: 1px solid rgba(148, 163, 184, 0.4);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-id {
  font-weight: 600;
  margin-right: var(--space-2);
}

.card-body {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.card-body p {
  margin: 0;
  color: var(--color-text-secondary);
}

.amount {
  text-align: right;
}

.amount strong {
  font-size: var(--font-size-title-lg);
  color: var(--color-primary);
  line-height: 1.4;
}

footer {
  text-align: right;
}
</style>
