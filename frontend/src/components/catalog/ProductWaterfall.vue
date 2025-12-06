<template>
  <div class="waterfall">
    <ProductCardLarge
      v-for="item in items"
      :key="item.id"
      :product="item"
      @select="$emit('select', $event)"
    />
  </div>
  <div v-if="loading" class="waterfall__skeleton">
    <div v-for="n in 4" :key="n" class="waterfall__placeholder"></div>
  </div>
  <DataStateBlock
    v-else-if="!items.length"
    type="empty"
    title="暂无在租商品"
    description="调整筛选条件或稍后再试。"
  />
</template>

<script lang="ts" setup>
import type { CatalogProductSummary } from '../../services/catalogService';
import ProductCardLarge from './ProductCardLarge.vue';
import DataStateBlock from '../feedback/DataStateBlock.vue';

defineProps<{
  items: CatalogProductSummary[];
  loading?: boolean;
}>();

defineEmits<{
  (e: 'select', productId: string): void;
}>();
</script>

<style scoped>
.waterfall {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--space-4);
}

.waterfall__skeleton {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--space-4);
}

.waterfall__placeholder {
  height: 280px;
  border-radius: var(--radius-card);
  background: linear-gradient(
    120deg,
    rgba(148, 163, 184, 0.3),
    rgba(148, 163, 184, 0.15),
    rgba(148, 163, 184, 0.3)
  );
  animation: shimmer 1.2s linear infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
