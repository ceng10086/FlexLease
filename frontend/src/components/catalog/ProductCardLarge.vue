<template>
  <article class="product-card" @click="$emit('select', product.id)">
    <div class="product-card__media" :style="coverStyle">
      <span v-if="!product.coverImageUrl">{{ initials }}</span>
    </div>
    <div class="product-card__body">
      <div class="product-card__category chip">{{ product.categoryCode }}</div>
      <h3>{{ product.name }}</h3>
      <p class="product-card__description">
        {{ product.description || '这款商品由认证厂商提供，支持标准租赁及先租后买。' }}
      </p>
      <div class="product-card__plans">
        <div v-for="plan in product.rentalPlans.slice(0, 3)" :key="plan.id" class="product-card__plan">
          <strong>¥{{ plan.rentAmountMonthly.toFixed(0) }}</strong>
          <span>/月 · {{ plan.planType }}</span>
        </div>
      </div>
    </div>
  </article>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type { CatalogProductSummary } from '../../services/catalogService';

const props = defineProps<{
  product: CatalogProductSummary;
}>();

defineEmits<{
  (e: 'select', productId: string): void;
}>();

const initials = computed(() => props.product.name?.slice(0, 2) ?? 'FL');

const coverStyle = computed(() => ({
  backgroundImage: props.product.coverImageUrl
    ? `linear-gradient(180deg, rgba(0,0,0,0), rgba(0,0,0,0.35)), url(${props.product.coverImageUrl})`
    : 'linear-gradient(135deg, rgba(37, 99, 235, 0.2), rgba(99, 102, 241, 0.2))'
}));
</script>

<style scoped>
.product-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  padding: var(--space-4);
  border-radius: var(--radius-card);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  min-height: 320px;
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-float);
}

.product-card__media {
  border-radius: var(--radius-card);
  background-size: cover;
  background-position: center;
  height: 180px;
  display: flex;
  align-items: flex-end;
  justify-content: flex-start;
  padding: var(--space-3);
  color: #fff;
  font-weight: 600;
  font-size: 20px;
}

.product-card__category {
  width: fit-content;
}

.product-card__body > h3 {
  margin: var(--space-2) 0;
}

.product-card__description {
  margin: 0;
  color: var(--color-text-secondary);
  min-height: 40px;
}

.product-card__plans {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.product-card__plan {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  font-size: var(--font-size-caption);
  color: var(--color-text-secondary);
}

.product-card__plan strong {
  font-size: var(--font-size-title-md);
  color: var(--color-primary);
}
</style>
