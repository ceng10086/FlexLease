<template>
  <div class="page-container notification-center">
    <div class="page-header">
      <div>
        <h2>通知中心</h2>
        <p class="page-header__meta">
          查看平台推送的站内通知，及时掌握订单与账户动态。
          <span v-if="lastLoadedAt" class="page-header__meta-updated">
            最近刷新：{{ formatDate(lastLoadedAt) }}
          </span>
        </p>
      </div>
      <div class="notification-filters">
        <a-space :wrap="true">
          <a-select
            v-model:value="filters.status"
            allow-clear
            placeholder="全部状态"
            style="width: 150px"
            @change="loadNotifications"
          >
            <a-select-option value="PENDING">待发送</a-select-option>
            <a-select-option value="SENT">已发送</a-select-option>
            <a-select-option value="FAILED">发送失败</a-select-option>
          </a-select>
          <a-select
            v-model:value="filters.channel"
            allow-clear
            placeholder="全部渠道"
            style="width: 150px"
            @change="loadNotifications"
          >
            <a-select-option value="IN_APP">站内信</a-select-option>
            <a-select-option value="EMAIL">邮件</a-select-option>
            <a-select-option value="SMS">短信</a-select-option>
          </a-select>
          <a-select
            v-model:value="filters.contextType"
            allow-clear
            placeholder="全部业务"
            style="width: 150px"
            @change="loadNotifications"
          >
            <a-select-option
              v-for="option in contextOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </a-select-option>
          </a-select>
          <a-input-search
            v-model:value="filters.keyword"
            placeholder="搜索标题/内容/收件人"
            allow-clear
            style="width: 220px"
          />
          <a-button type="primary" :loading="loading" @click="loadNotifications">刷新</a-button>
        </a-space>
      </div>
    </div>

    <a-card class="notification-stats-card" size="small">
      <a-row :gutter="[16, 16]">
        <a-col v-for="card in statusCards" :key="card.status" :xs="24" :md="8">
          <a-statistic :title="card.label" :value="statusSummary[card.status]" />
        </a-col>
      </a-row>
      <div class="notification-channel-summary">
        <span class="notification-channel-summary__label">渠道统计：</span>
        <a-space :wrap="true">
          <a-tag v-for="channel in channelCards" :key="channel.key" :color="channel.color">
            {{ channel.label }} · {{ channelSummary[channel.key] }}
          </a-tag>
        </a-space>
      </div>
    </a-card>

    <a-card class="notification-list-card" :loading="loading">
      <template v-if="filteredNotifications.length">
        <div class="notification-list">
          <div v-for="item in filteredNotifications" :key="item.id" class="notification-item">
            <div class="notification-item__header">
              <div class="notification-item__title">
                <span>{{ item.subject || '系统通知' }}</span>
                <a-tag v-if="item.templateCode" bordered="false" color="default">
                  模板：{{ item.templateCode }}
                </a-tag>
              </div>
              <div class="notification-item__status">
                <a-tag :color="statusColor(item.status)">{{ statusLabel(item.status) }}</a-tag>
                <a-tag>{{ channelLabel(item.channel) }}</a-tag>
                <a-tag v-if="item.contextType" color="orange">{{ contextLabel(item.contextType) }}</a-tag>
              </div>
            </div>
            <div class="notification-item__meta">
              <span>收件人：{{ item.recipient }}</span>
              <span>创建：{{ formatDate(item.createdAt) }}</span>
              <span v-if="item.sentAt">发送：{{ formatDate(item.sentAt) }}</span>
            </div>
            <p v-if="item.content" class="notification-item__content">{{ item.content }}</p>
            <a-alert
              v-if="item.status === 'FAILED' && item.errorMessage"
              type="error"
              show-icon
              message="发送失败"
              :description="item.errorMessage"
            />
          </div>
        </div>
      </template>
      <a-empty v-else :description="emptyDescription" />
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import {
  listNotificationLogs,
  type NotificationChannel,
  type NotificationLog,
  type NotificationStatus
} from '../../services/notificationService';

const loading = ref(false);
const notifications = ref<NotificationLog[]>([]);
const lastLoadedAt = ref<string | null>(null);
const filters = reactive<{
  status?: NotificationStatus;
  channel?: NotificationChannel;
  contextType?: string;
  keyword: string;
}>({
  keyword: ''
});

const statusCards = [
  { status: 'SENT' as NotificationStatus, label: '已发送' },
  { status: 'PENDING' as NotificationStatus, label: '待发送' },
  { status: 'FAILED' as NotificationStatus, label: '发送失败' }
];

const channelCards = [
  { key: 'IN_APP' as NotificationChannel, label: '站内信', color: 'blue' },
  { key: 'EMAIL' as NotificationChannel, label: '邮件', color: 'green' },
  { key: 'SMS' as NotificationChannel, label: '短信', color: 'purple' }
];

const contextOptions = [
  { value: 'DISPUTE', label: '纠纷通知' }
];

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

const channelLabel = (channel: NotificationChannel) => {
  switch (channel) {
    case 'EMAIL':
      return '邮件';
    case 'SMS':
      return '短信';
    default:
      return '站内信';
  }
};

const contextLabel = (context?: string | null) => {
  if (!context) {
    return '通用通知';
  }
  switch (context) {
    case 'DISPUTE':
      return '纠纷通知';
    default:
      return context;
  }
};

const filteredNotifications = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase();
  return notifications.value.filter((item) => {
    const matchStatus = filters.status ? item.status === filters.status : true;
    const matchChannel = filters.channel ? item.channel === filters.channel : true;
    const matchContext = filters.contextType ? item.contextType === filters.contextType : true;
    if (!keyword) {
      return matchStatus && matchChannel && matchContext;
    }
    const haystack = [item.subject, item.content, item.recipient, item.templateCode]
      .filter(Boolean)
      .map((value) => String(value).toLowerCase());
    const matchKeyword = haystack.some((value) => value.includes(keyword));
    return matchStatus && matchChannel && matchContext && matchKeyword;
  });
});

const statusSummary = computed<Record<NotificationStatus, number>>(() => {
  const base: Record<NotificationStatus, number> = {
    SENT: 0,
    PENDING: 0,
    FAILED: 0
  };
  return filteredNotifications.value.reduce((acc, item) => {
    acc[item.status] += 1;
    return acc;
  }, { ...base });
});

const channelSummary = computed<Record<NotificationChannel, number>>(() => {
  const base: Record<NotificationChannel, number> = {
    IN_APP: 0,
    EMAIL: 0,
    SMS: 0
  };
  return filteredNotifications.value.reduce((acc, item) => {
    acc[item.channel] += 1;
    return acc;
  }, { ...base });
});

const emptyDescription = computed(() => {
  if (filters.keyword) {
    return '没有匹配的通知，换个关键词试试';
  }
  if (filters.status || filters.channel || filters.contextType) {
    return '当前筛选条件下暂无通知';
  }
  return '暂无通知记录';
});

const formatDate = (value?: string | null) =>
  value ? new Date(value).toLocaleString() : '-';

const loadNotifications = async () => {
  loading.value = true;
  try {
    notifications.value = await listNotificationLogs({
      status: filters.status,
      channel: filters.channel,
      contextType: filters.contextType
    });
    lastLoadedAt.value = new Date().toISOString();
  } catch (error) {
    console.error('加载通知失败', error);
    message.error('加载通知失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

loadNotifications();
</script>

<style scoped>
.notification-center {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notification-filters {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

.page-header__meta-updated {
  margin-left: 8px;
  font-size: 12px;
  color: #0f172a;
}

.notification-stats-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-channel-summary {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.notification-channel-summary__label {
  color: #64748b;
  font-size: 13px;
}

.notification-list-card :deep(.ant-card-body) {
  padding: 0 16px 16px;
}

.notification-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 16px;
}

.notification-item {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notification-item__header {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.notification-item__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}

.notification-item__status {
  display: flex;
  align-items: center;
  gap: 8px;
}

.notification-item__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
}

.notification-item__content {
  margin: 0;
  color: #0f172a;
  line-height: 1.6;
}

@media (max-width: 768px) {
  .notification-filters {
    width: 100%;
    justify-content: flex-start;
  }

  .notification-list-card :deep(.ant-card-body) {
    padding: 0 8px 12px;
  }

  .notification-item {
    padding: 12px;
  }
}
</style>
