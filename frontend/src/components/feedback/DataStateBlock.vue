<template>
  <div class="data-state" :class="`data-state--${type}`">
    <div class="data-state__icon">
      <slot name="icon">
        <component :is="iconName" />
      </slot>
    </div>
    <div class="data-state__content">
      <h3>{{ title }}</h3>
      <p v-if="description">{{ description }}</p>
      <slot />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { LoadingOutlined, InboxOutlined, WarningOutlined } from '@ant-design/icons-vue';

const props = withDefaults(
  defineProps<{
    type?: 'loading' | 'empty' | 'error';
    title: string;
    description?: string;
  }>(),
  {
    type: 'empty'
  }
);

const iconName = computed(() => {
  switch (props.type) {
    case 'loading':
      return LoadingOutlined;
    case 'error':
      return WarningOutlined;
    default:
      return InboxOutlined;
  }
});
</script>

<style scoped>
.data-state {
  border: 1px dashed rgba(148, 163, 184, 0.5);
  border-radius: var(--radius-card);
  padding: var(--space-4);
  text-align: center;
  color: var(--color-text-secondary);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  align-items: center;
  justify-content: center;
  min-height: 180px;
}

.data-state__icon {
  font-size: 32px;
  color: var(--color-primary);
}

.data-state__content > h3 {
  margin: var(--space-2) 0 0;
  font-weight: 600;
  color: var(--color-text);
}

.data-state__content > p {
  margin: var(--space-2) 0 0;
}
</style>
