<template>
  <div class="announcement-rail">
    <div v-if="items.length" class="announcement-rail__list">
      <article v-for="item in items" :key="item.id" class="announcement">
        <div class="announcement__head">
          <span class="announcement__channel">站内信</span>
          <small>{{ formatDate(item.createdAt) }}</small>
        </div>
        <h4>{{ item.subject || '系统通知' }}</h4>
        <p v-if="item.content">{{ item.content }}</p>
      </article>
    </div>
    <DataStateBlock
      v-else
      type="empty"
      title="暂无公告"
      description="平台运营公告将展示在此处。"
    />
  </div>
</template>

<script lang="ts" setup>
// 公告/提醒列表：展示固定的演示公告与操作提示，便于演示“平台信息流”。
import type { NotificationLog } from '../../services/notificationService';
import DataStateBlock from '../feedback/DataStateBlock.vue';

defineProps<{
  items: NotificationLog[];
}>();

const formatDate = (value?: string | null) => {
  if (!value) {
    return '--';
  }
  return new Date(value).toLocaleString();
};
</script>

<style scoped>
.announcement-rail__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.announcement {
  padding: var(--space-3);
  border-radius: var(--radius-card);
  background: rgba(37, 99, 235, 0.05);
  border: 1px solid rgba(37, 99, 235, 0.12);
}

.announcement__head {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.announcement h4 {
  margin: 4px 0;
}

.announcement p {
  margin: 0;
  color: var(--color-text-secondary);
}

.announcement__channel {
  font-weight: 600;
  color: var(--color-primary);
}
</style>
