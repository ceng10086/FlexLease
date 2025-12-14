<template>
  <div class="product-card surface-card">
    <div class="product-card__header">
      <div>
        <p class="product-card__category">{{ product.categoryCode }}</p>
        <h3>{{ product.name }}</h3>
      </div>
      <a-tag :color="statusColor">{{ statusLabel }}</a-tag>
    </div>
    <p class="product-card__desc">创建于 {{ formatDate(product.createdAt) }}</p>
    <div class="product-card__footer">
      <a-button type="primary" block @click="emit('open', product.id)">配置详情</a-button>
      <a-button
        block
        :disabled="!canSubmit"
        @click="emit('submit', product.id)"
      >
        提交审核
      </a-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { ProductSummary } from '../../services/productService';

import { computed } from 'vue';

const props = defineProps<{
  product: ProductSummary;
}>();

const emit = defineEmits<{
  (e: 'open', productId: string): void;
  (e: 'submit', productId: string): void;
}>();

const STATUS_COLOR: Record<ProductSummary['status'], string> = {
  DRAFT: 'default',
  PENDING_REVIEW: 'processing',
  ACTIVE: 'success',
  INACTIVE: 'warning',
  REJECTED: 'error'
};

const STATUS_LABEL: Record<ProductSummary['status'], string> = {
  DRAFT: '草稿',
  PENDING_REVIEW: '审核中',
  ACTIVE: '已上线',
  INACTIVE: '已下线',
  REJECTED: '被驳回'
};

const statusColor = computed(() => STATUS_COLOR[props.product.status]);
const statusLabel = computed(() => STATUS_LABEL[props.product.status]);

const canSubmit = computed(
  () => props.product.status === 'DRAFT' || props.product.status === 'REJECTED'
);

const formatDate = (value: string) => new Date(value).toLocaleDateString();
</script>

<style scoped>
.product-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
}

.product-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  align-items: flex-start;
}

.product-card__category {
  margin: 0;
  font-size: var(--font-size-caption);
  color: var(--color-text-secondary);
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.product-card__header h3 {
  margin: var(--space-1) 0 0;
  font-size: var(--font-size-title-md);
  line-height: 1.4;
}

.product-card__desc {
  margin: 0;
  color: var(--color-text-secondary);
}

.product-card__footer {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
</style>
