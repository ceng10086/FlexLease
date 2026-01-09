<template>
  <div class="plan-selector" role="tablist">
    <button
      v-for="plan in plans"
      :key="plan.id"
      class="plan-selector__option"
      :class="{ 'plan-selector__option--active': plan.id === modelValue }"
      type="button"
      @click="$emit('update:modelValue', plan.id)"
    >
      <div class="plan-selector__badge">{{ plan.planType }}</div>
      <div class="plan-selector__price">
        <strong>¥{{ plan.rentAmountMonthly.toFixed(0) }}</strong>
        <span>/月 · {{ plan.termMonths }} 个月</span>
      </div>
      <p class="plan-selector__meta">
        押金 ¥{{ plan.depositAmount.toFixed(0) }}
        <span v-if="plan.buyoutPrice"> · 买断 ¥{{ plan.buyoutPrice?.toFixed(0) }}</span>
      </p>
    </button>
  </div>
</template>

<script lang="ts" setup>
// 方案选择器：在商品详情/下单前选择租赁方案、租期与对应定价快照。
import type { CatalogRentalPlan } from '../../services/catalogService';

defineProps<{
  plans: CatalogRentalPlan[];
  modelValue?: string;
}>();

defineEmits<{
  (e: 'update:modelValue', value: string): void;
}>();
</script>

<style scoped>
.plan-selector {
  display: flex;
  gap: var(--space-3);
  overflow-x: auto;
  padding-bottom: var(--space-2);
}

.plan-selector__option {
  min-width: 220px;
  border-radius: var(--radius-card);
  padding: var(--space-3);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  cursor: pointer;
  text-align: left;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.plan-selector__option--active {
  border-color: var(--color-primary);
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.2);
}

.plan-selector__badge {
  font-size: var(--font-size-caption);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--color-text-secondary);
}

.plan-selector__price {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  margin: var(--space-2) 0;
}

.plan-selector__price strong {
  font-size: var(--font-size-title-lg);
  color: var(--color-primary);
}

.plan-selector__meta {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-caption);
}
</style>
