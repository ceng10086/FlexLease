<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>通知中心</h2>
        <p class="page-header__meta">查看平台推送的站内通知，及时掌握订单与账户动态。</p>
      </div>
      <a-space>
        <a-select
          v-model:value="filters.status"
          allow-clear
          placeholder="全部状态"
          style="width: 200px"
          @change="loadNotifications"
        >
          <a-select-option value="PENDING">待发送</a-select-option>
          <a-select-option value="SENT">已发送</a-select-option>
          <a-select-option value="FAILED">发送失败</a-select-option>
        </a-select>
        <a-button type="primary" :loading="loading" @click="loadNotifications">刷新</a-button>
      </a-space>
    </div>

    <a-card>
      <template v-if="notifications.length">
        <a-timeline>
          <a-timeline-item v-for="item in notifications" :key="item.id">
            <div class="timeline-item">
              <div class="timeline-item__header">
                <span class="timeline-item__subject">{{ item.subject || '系统通知' }}</span>
                <a-tag :color="statusColor(item.status)">{{ item.status }}</a-tag>
              </div>
              <div class="timeline-item__meta">
                <span>{{ formatDate(item.createdAt) }}</span>
                <span class="timeline-item__channel">渠道：{{ item.channel }}</span>
              </div>
              <p class="timeline-item__content" v-if="item.content">{{ item.content }}</p>
            </div>
          </a-timeline-item>
        </a-timeline>
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
  type NotificationLog,
  type NotificationStatus
} from '../../services/notificationService';

const loading = ref(false);
const notifications = ref<NotificationLog[]>([]);
const filters = reactive<{ status?: NotificationStatus }>({});

const emptyDescription = computed(() =>
  filters.status ? '当前状态下暂无通知' : '暂无通知记录'
);

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

const formatDate = (value: string) => new Date(value).toLocaleString();

const loadNotifications = async () => {
  loading.value = true;
  try {
    notifications.value = await listNotificationLogs(filters.status);
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
.timeline-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.timeline-item__header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.timeline-item__subject {
  font-weight: 600;
}

.timeline-item__meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
}

.timeline-item__channel {
  font-style: italic;
}

.timeline-item__content {
  margin: 0;
  color: #1f2937;
  line-height: 1.5;
}
</style>
