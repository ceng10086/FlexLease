<template>
  <div class="timeline">
    <div v-for="event in events" :key="event.id" class="timeline__item">
      <div class="timeline__dot" />
      <div class="timeline__content">
        <p class="timeline__title">{{ event.eventType }}</p>
        <p class="timeline__desc">{{ event.description }}</p>
        <small>{{ new Date(event.createdAt).toLocaleString() }}</small>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import type { OrderEvent } from '../../services/orderService';

defineProps<{
  events: OrderEvent[];
}>();
</script>

<style scoped>
.timeline {
  position: relative;
  padding-left: 24px;
}

.timeline::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 0;
  bottom: 0;
  width: 2px;
  background: var(--color-border);
}

.timeline__item {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.timeline__dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--color-primary);
  position: relative;
  top: 6px;
}

.timeline__title {
  margin: 0;
  font-weight: 600;
}

.timeline__desc {
  margin: var(--space-1) 0;
  color: var(--color-text-secondary);
}
</style>
