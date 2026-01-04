<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="通知中心 · 信用与纠纷同频"
        eyebrow="Notifications"
        :description="lastLoadedAt ? `最近刷新：${lastLoadedAt}` : '查看站内公告、信用提示与纠纷倒计时。'"
      >
        <template #actions>
          <a-button type="primary" :loading="loading" @click="refresh">刷新</a-button>
        </template>
      </PageHeader>
    </template>

    <PageSection title="筛选器" description="按状态、上下文快速收敛通知列表。">
      <div class="filter-grid">
        <a-select
          v-model:value="filters.status"
          allow-clear
          placeholder="状态"
          @change="refresh"
        >
          <a-select-option value="PENDING">待发送</a-select-option>
          <a-select-option value="SENT">已发送</a-select-option>
          <a-select-option value="FAILED">发送失败</a-select-option>
        </a-select>
        <a-select
          v-model:value="filters.contextType"
          allow-clear
          placeholder="业务场景"
          @change="refresh"
        >
          <a-select-option value="DISPUTE">纠纷通知</a-select-option>
          <a-select-option value="CREDIT">信用积分</a-select-option>
          <a-select-option value="ANNOUNCEMENT">公告</a-select-option>
        </a-select>
        <a-input-search
          v-model:value="filters.keyword"
          allow-clear
          placeholder="搜索标题/内容/收件人"
        />
      </div>
    </PageSection>

    <PageSection title="状态总览">
      <div class="summary-cards">
        <div v-for="card in statusCards" :key="card.key" class="summary-card">
          <p>{{ card.label }}</p>
          <strong>{{ statusSummary[card.key] ?? 0 }}</strong>
        </div>
      </div>
    </PageSection>

    <PageSection title="通知列表">
      <a-skeleton v-if="loading" active :paragraph="{ rows: 8 }" />
      <div v-else>
        <div v-if="filteredNotifications.length" class="notification-list">
          <NotificationCard
            v-for="notification in filteredNotifications"
            :key="notification.id"
            :notification="notification"
          />
        </div>
        <DataStateBlock
          v-else
          type="empty"
          title="暂无符合条件的通知"
          description="尝试清除筛选或稍后再试。"
        />
      </div>
    </PageSection>
  </PageShell>
</template>

<script lang="ts" setup>
import { computed, reactive } from 'vue';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import PageSection from '../../components/layout/PageSection.vue';
import NotificationCard from '../../components/notifications/NotificationCard.vue';
import DataStateBlock from '../../components/feedback/DataStateBlock.vue';
import {
  listNotificationLogs,
  type NotificationLog,
  type NotificationStatus
} from '../../services/notificationService';
import { useQuery } from '../../composables/useQuery';

const filters = reactive<{
  status?: NotificationStatus;
  contextType?: string;
  keyword: string;
}>({
  keyword: ''
});

const { data, loading, refresh } = useQuery<NotificationLog[]>(
  () =>
    `notification-${filters.status ?? 'all'}-${filters.contextType ?? 'all'}`,
  () =>
    listNotificationLogs({
      status: filters.status,
      contextType: filters.contextType
    })
);

const lastLoadedAt = computed(() => {
  if (!data.value || !data.value.length) {
    return null;
  }
  const latest = data.value.reduce((acc, item) => {
    if (!acc) {
      return item.createdAt;
    }
    return new Date(item.createdAt) > new Date(acc) ? item.createdAt : acc;
  }, data.value[0].createdAt);
  return new Date(latest).toLocaleString();
});

const keywordFilter = computed(() => filters.keyword.trim().toLowerCase());

const filteredNotifications = computed(() => {
  const keyword = keywordFilter.value;
  const records = data.value ?? [];
  if (!keyword) {
    return records;
  }
  return records.filter((item) => {
    const pool = [item.subject, item.content, item.recipient].join(' ').toLowerCase();
    return pool.includes(keyword);
  });
});

const statusCards = [
  { key: 'SENT' as NotificationStatus, label: '已发送' },
  { key: 'PENDING' as NotificationStatus, label: '待发送' },
  { key: 'FAILED' as NotificationStatus, label: '发送失败' }
];

const statusSummary = computed<Record<NotificationStatus, number>>(() => {
  const summary = { SENT: 0, PENDING: 0, FAILED: 0 } as Record<NotificationStatus, number>;
  (data.value ?? []).forEach((item) => {
    summary[item.status] += 1;
  });
  return summary;
});
</script>

<style scoped>
.filter-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-3);
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--space-3);
}

.summary-card {
  padding: var(--space-3);
  border-radius: var(--radius-card);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.summary-card--ghost {
  background: rgba(148, 163, 184, 0.08);
}

.summary-card p {
  margin: 0;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.summary-card strong {
  font-size: 24px;
  font-weight: 600;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
</style>
