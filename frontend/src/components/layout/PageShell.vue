<template>
  <section class="page-shell-root" :class="[compact && 'page-shell-root--compact']">
    <header v-if="$slots.header" class="page-shell__header">
      <slot name="header" />
    </header>
    <div class="page-shell__body">
      <slot />
    </div>
  </section>
</template>

<script lang="ts" setup>
defineProps<{
  compact?: boolean;
}>();
</script>

<style scoped>
.page-shell-root {
  margin: 0 auto;
  max-width: var(--layout-max-width);
  padding: var(--layout-padding);
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.page-shell-root--compact {
  padding-top: var(--space-4);
}

.page-shell__header {
  background: linear-gradient(120deg, rgba(37, 99, 235, 0.12), rgba(99, 102, 241, 0.08));
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: 20px;
  padding: var(--space-5);
  box-shadow: var(--shadow-card);
}

.page-shell__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}
</style>
