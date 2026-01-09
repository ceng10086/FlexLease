<template>
  <div class="quick-grid">
    <article v-for="entry in entries" :key="entry.key" class="quick-entry">
      <div class="quick-entry__icon" v-if="entry.icon">
        <component :is="entry.icon" />
      </div>
      <div class="quick-entry__body">
        <h3>{{ entry.title }}</h3>
        <p>{{ entry.description }}</p>
      </div>
      <a-button type="primary" size="small" ghost @click="$emit('select', entry)">
        {{ entry.ctaLabel }}
      </a-button>
    </article>
  </div>
</template>

<script lang="ts" setup>
// 快捷入口网格：根据角色装配可点击的功能入口卡片（跳转到对应路由）。
export type QuickEntry = {
  key: string;
  title: string;
  description: string;
  ctaLabel: string;
  icon?: any;
};

defineProps<{
  entries: QuickEntry[];
}>();

defineEmits<{
  (e: 'select', entry: QuickEntry): void;
}>();
</script>

<style scoped>
.quick-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--space-4);
}

.quick-entry {
  padding: var(--space-4);
  border-radius: var(--radius-card);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.quick-entry__icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(37, 99, 235, 0.08);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 18px;
}

.quick-entry__body {
  flex: 1;
}

.quick-entry__body h3 {
  margin: 0 0 4px;
  font-size: 16px;
}

.quick-entry__body p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}
</style>
