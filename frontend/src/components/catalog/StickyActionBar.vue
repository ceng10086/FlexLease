<template>
  <div class="sticky-bar">
    <div class="sticky-bar__price">
      <p class="sticky-bar__label">{{ label }}</p>
      <strong>¥{{ price.toFixed(0) }}</strong>
      <span>{{ priceSuffix }}</span>
    </div>
    <div class="sticky-bar__actions">
      <a-button v-if="secondaryLabel" :disabled="disabled" @click="$emit('secondary')">
        {{ secondaryLabel }}
      </a-button>
      <a-button
        type="primary"
        size="large"
        :loading="busy"
        :disabled="disabled"
        @click="$emit('primary')"
      >
        {{ ctaLabel }}
      </a-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
// 吸底操作栏：在移动端/长页面提供“加入购物车/去下单”等主要行动按钮。
withDefaults(
  defineProps<{
    price: number;
    label?: string;
    ctaLabel: string;
    secondaryLabel?: string;
    disabled?: boolean;
    busy?: boolean;
    priceSuffix?: string;
  }>(),
  {
    price: 0,
    label: '每月租金',
    ctaLabel: '立即租赁',
    priceSuffix: '/ 月（方案预估）'
  }
);

defineEmits<{
  (e: 'primary'): void;
  (e: 'secondary'): void;
}>();
</script>

<style scoped>
.sticky-bar {
  position: sticky;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding: var(--space-4);
  border-radius: var(--radius-card);
  backdrop-filter: blur(12px);
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: 0 -8px 30px rgba(15, 23, 42, 0.1);
}

.sticky-bar__price {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.sticky-bar__label {
  margin: 0;
  font-size: var(--font-size-caption);
  color: var(--color-text-secondary);
}

.sticky-bar__price strong {
  font-size: 26px;
  color: var(--color-primary);
}

.sticky-bar__actions {
  display: flex;
  gap: var(--space-2);
}

@media (max-width: 640px) {
  .sticky-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .sticky-bar__actions {
    width: 100%;
  }

  .sticky-bar__actions :deep(.ant-btn) {
    flex: 1;
  }
}
</style>
