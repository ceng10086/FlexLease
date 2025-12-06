<template>
  <div class="page-header-root" :class="borderless && 'page-header-root--borderless'">
    <div class="page-header__titles">
      <p v-if="eyebrow" class="page-header__eyebrow">{{ eyebrow }}</p>
      <h1 class="page-header__title">{{ title }}</h1>
      <p v-if="description" class="page-header__description">{{ description }}</p>
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<script lang="ts" setup>
defineProps<{
  title: string;
  description?: string;
  eyebrow?: string;
  borderless?: boolean;
}>();
</script>

<style scoped>
.page-header-root {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.page-header-root--borderless {
  border: none;
  box-shadow: none;
}

.page-header__titles {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.page-header__eyebrow {
  margin: 0;
  font-size: var(--font-size-caption);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
}

.page-header__title {
  margin: 0;
  font-size: var(--font-size-title-xl);
  line-height: 1.35;
  font-weight: 600;
}

.page-header__description {
  margin: 0;
  color: var(--color-text-secondary);
}

.page-header__actions {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

@media (max-width: 640px) {
  .page-header-root {
    flex-direction: column;
  }

  .page-header__actions {
    width: 100%;
    justify-content: flex-start;
  }
}
</style>
