<template>
  <section class="stat-widget" :class="`stat-widget--${variant}`">
    <div v-if="$slots.icon" class="stat-widget__icon">
      <slot name="icon" />
    </div>
    <div class="stat-widget__body">
      <p class="stat-widget__label">{{ label }}</p>
      <p class="stat-widget__value">{{ value }}</p>
      <p v-if="description" class="stat-widget__description">{{ description }}</p>
      <div v-if="trendLabel || trendValue" class="stat-widget__trend">
        <span>{{ trendLabel }}</span>
        <strong>{{ trendValue }}</strong>
      </div>
    </div>
  </section>
</template>

<script lang="ts" setup>
// 指标卡片：展示单个统计值（支持图标/趋势/副标题），用于驾驶舱与洞察页。
withDefaults(
  defineProps<{
    label: string;
    value: string;
    description?: string;
    trendLabel?: string;
    trendValue?: string;
    variant?: 'default' | 'accent' | 'warning';
  }>(),
  {
    variant: 'default'
  }
);
</script>

<style scoped>
.stat-widget {
  display: flex;
  gap: var(--space-4);
  padding: var(--space-4);
  border-radius: var(--radius-card);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  min-height: 120px;
}

.stat-widget__icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: rgba(37, 99, 235, 0.08);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  font-size: 22px;
}

.stat-widget--accent .stat-widget__icon {
  background: rgba(255, 138, 0, 0.08);
  color: var(--color-accent);
}

.stat-widget--warning .stat-widget__icon {
  background: rgba(244, 63, 94, 0.08);
  color: var(--color-danger);
}

.stat-widget__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-widget__label {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.stat-widget__value {
  margin: 0;
  font-size: 26px;
  font-weight: 600;
  color: var(--color-text);
}

.stat-widget__description {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.stat-widget__trend {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.stat-widget__trend strong {
  font-weight: 600;
  color: var(--color-primary);
}
</style>
