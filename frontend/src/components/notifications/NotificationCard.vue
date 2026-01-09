<template>
  <article class="notification-card">
    <header class="notification-card__header">
      <div>
        <p class="notification-card__subject">{{ notification.subject || '系统通知' }}</p>
        <div class="notification-card__pills">
          <a-tag :color="statusColor(notification.status)">{{ statusLabel(notification.status) }}</a-tag>
          <a-tag v-if="notification.contextType" color="orange">{{ notification.contextType }}</a-tag>
        </div>
      </div>
      <small>{{ formatDate(notification.createdAt) }}</small>
    </header>
    <p v-if="notification.content" class="notification-card__content">
      {{ notification.content }}
    </p>
    <footer class="notification-card__footer">
      <span>收件人：{{ notification.recipient }}</span>
      <span v-if="notification.sentAt">发送于 {{ formatDate(notification.sentAt) }}</span>
    </footer>
    <a-alert
      v-if="notification.status === 'FAILED' && notification.errorMessage"
      type="error"
      show-icon
      :message="notification.errorMessage"
    />
  </article>
</template>

<script lang="ts" setup>
// 站内信卡片：统一通知的标题/摘要/标签与时间展示，供通知中心复用。
import type { NotificationLog, NotificationStatus } from '../../services/notificationService';

defineProps<{
  notification: NotificationLog;
}>();

const statusLabel = (status: NotificationStatus) => {
  switch (status) {
    case 'SENT':
      return '已发送';
    case 'FAILED':
      return '发送失败';
    default:
      return '待发送';
  }
};

const statusColor = (status: NotificationStatus) => {
  switch (status) {
    case 'SENT':
      return 'green';
    case 'FAILED':
      return 'red';
    default:
      return 'blue';
  }
};

const formatDate = (value?: string | null) => {
  if (!value) {
    return '--';
  }
  return new Date(value).toLocaleString();
};
</script>

<style scoped>
.notification-card {
  padding: var(--space-4);
  border-radius: var(--radius-card);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.notification-card__header {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
}

.notification-card__subject {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.notification-card__pills {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.notification-card__content {
  margin: 0;
  color: var(--color-text);
  line-height: 1.6;
}

.notification-card__footer {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-2);
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
