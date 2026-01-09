<template>
  <section class="page-section" :class="variantClass">
    <header v-if="title || $slots.actions" class="page-section__header">
      <div>
        <p v-if="eyebrow" class="page-section__eyebrow">{{ eyebrow }}</p>
        <h2 v-if="title" class="page-section__title">{{ title }}</h2>
        <p v-if="description" class="page-section__description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="page-section__actions">
        <slot name="actions" />
      </div>
    </header>
    <div class="page-section__body">
      <slot />
    </div>
  </section>
</template>

<script lang="ts" setup>
// 页面分区：带标题/描述与 actions slot 的容器，用于驾驶舱与各看板分块展示。
import { computed } from 'vue';

const props = defineProps<{
  title?: string;
  description?: string;
  eyebrow?: string;
  flush?: boolean;
  transparent?: boolean;
}>();

const variantClass = computed(() => ({
  'page-section--flush': props.flush,
  'page-section--transparent': props.transparent
}));
</script>

<style scoped>
.page-section {
  background: var(--color-surface);
  border-radius: var(--radius-card);
  padding: var(--space-5);
  border: 1px solid rgba(148, 163, 184, 0.35);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.page-section--flush {
  padding: 0;
}

.page-section--transparent {
  background: transparent;
  border: none;
  box-shadow: none;
  padding: 0;
}

.page-section__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.page-section__eyebrow {
  margin: 0;
  text-transform: uppercase;
  font-size: var(--font-size-caption);
  letter-spacing: 0.06em;
  color: var(--color-text-secondary);
}

.page-section__title {
  margin: 0;
  font-size: var(--font-size-title-lg);
  font-weight: 600;
  line-height: 1.4;
}

.page-section__description {
  margin: 0;
  color: var(--color-text-secondary);
}

.page-section__actions {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}
</style>
