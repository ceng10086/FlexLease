<template>
  <div class="timeline">
    <div class="timeline__controls" v-if="segmentedOptions.length > 1 || canToggle">
      <a-segmented
        v-if="segmentedOptions.length > 1"
        v-model:value="filterValue"
        :options="segmentedOptions"
        size="small"
      />
      <a-button type="link" size="small" v-if="canToggle" @click="toggleCollapsed">
        {{ collapsed ? '展开全部' : '收起' }}
      </a-button>
    </div>

    <div v-if="displayedEvents.length" class="timeline__list">
      <div v-for="event in displayedEvents" :key="event.id" class="timeline__item">
        <div class="timeline__dot" />
        <div class="timeline__content">
          <div class="timeline__meta">
            <span class="timeline__actor">{{ actorRoleLabel(event) }}</span>
            <small>{{ formatTime(event.createdAt) }}</small>
          </div>
          <p class="timeline__title">{{ event.eventType }}</p>
          <p class="timeline__desc">{{ event.description }}</p>
        </div>
      </div>
    </div>
    <p v-else class="timeline__empty">暂无相关记录</p>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue';
import type { OrderEvent } from '../../services/orderService';

const ROLE_LABELS: Record<string, string> = {
  USER: '消费者',
  VENDOR: '厂商',
  ADMIN: '管理员',
  ARBITRATOR: '仲裁管理',
  REVIEW_PANEL: '复核组',
  INTERNAL: '平台',
  SYSTEM: '系统',
};

const props = defineProps<{
  events: OrderEvent[];
}>();

const filterValue = ref<string>('ALL');
const collapsed = ref(true);
const COLLAPSE_THRESHOLD = 6;

const resolveActorKey = (event: OrderEvent): string => {
  return event.actorRole ?? 'SYSTEM';
};

const actorRoleLabel = (event: OrderEvent): string => {
  const key = resolveActorKey(event);
  return ROLE_LABELS[key] ?? key;
};

const segmentedOptions = computed(() => {
  const roles = Array.from(new Set(props.events.map((event) => resolveActorKey(event))));
  if (!roles.length) {
    return [{ label: '全部', value: 'ALL' }];
  }
  const options = roles.map((role) => ({
    label: ROLE_LABELS[role] ?? role,
    value: role
  }));
  return [{ label: '全部', value: 'ALL' }, ...options];
});

const filteredEvents = computed(() => {
  if (filterValue.value === 'ALL') {
    return props.events;
  }
  return props.events.filter((event) => resolveActorKey(event) === filterValue.value);
});

const canToggle = computed(() => filteredEvents.value.length > COLLAPSE_THRESHOLD);

const displayedEvents = computed(() => {
  if (collapsed.value && canToggle.value) {
    return filteredEvents.value.slice(0, COLLAPSE_THRESHOLD);
  }
  return filteredEvents.value;
});

const toggleCollapsed = () => {
  collapsed.value = !collapsed.value;
};

const formatTime = (value: string) => new Date(value).toLocaleString();

watch(
  () => props.events,
  () => {
    const hasCurrentFilter =
      filterValue.value === 'ALL' ||
      segmentedOptions.value.some((option) => option.value === filterValue.value);
    if (!hasCurrentFilter) {
      filterValue.value = 'ALL';
    }
    collapsed.value = true;
  }
);
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

.timeline__controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
  gap: var(--space-3);
}

.timeline__list {
  display: flex;
  flex-direction: column;
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

.timeline__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-text-secondary);
}

.timeline__actor {
  font-weight: 600;
  font-size: 13px;
}

.timeline__title {
  margin: var(--space-1) 0 0;
  font-weight: 600;
}

.timeline__desc {
  margin: var(--space-1) 0;
  color: var(--color-text-secondary);
}

.timeline__empty {
  color: var(--color-text-secondary);
  margin: 0;
}

@media (max-width: 640px) {
  .timeline {
    padding-left: 18px;
  }

  .timeline::before {
    left: 6px;
  }

  .timeline__controls {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
