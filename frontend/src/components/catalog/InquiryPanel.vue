<template>
  <div class="inquiry-stack">
    <a-form layout="vertical" @submit.prevent="handleSubmit">
      <a-form-item label="称呼">
        <a-input v-model:value="form.contactName" placeholder="如何称呼您" />
      </a-form-item>
      <a-form-item label="联系方式">
        <a-input v-model:value="form.contactMethod" placeholder="手机号 / 邮箱" />
      </a-form-item>
      <a-form-item label="咨询内容">
        <a-textarea
          v-model:value="form.message"
          :rows="4"
          placeholder="填写租赁需求、交付时间或更多问题，厂商将在 72 小时内回复"
        />
      </a-form-item>
      <a-space style="width: 100%" direction="vertical">
        <a-button type="primary" block :loading="submitting" :disabled="!form.message" @click="handleSubmit">
          发送咨询
        </a-button>
        <p class="inquiry-hint">咨询有效期 72 小时，回复将同步到通知中心及聊天面板。</p>
      </a-space>
    </a-form>

    <a-divider orientation="left">我的咨询</a-divider>
    <a-spin :spinning="historyLoading">
      <a-empty v-if="!auth.isAuthenticated" description="登录后可查看咨询记录" />
      <a-empty v-else-if="!history.length" description="暂无咨询记录" />
      <div v-else class="history-list">
        <a-card v-for="item in history" :key="item.id" size="small" class="history-card">
          <template #title>
            <div class="history-title">
              <a-tag :color="inquiryStatusColor(item.status)">{{ inquiryStatusLabel(item.status) }}</a-tag>
              <span class="history-time">{{ formatDate(item.createdAt) }}</span>
            </div>
          </template>
          <p class="history-message">{{ item.message }}</p>
          <div v-if="item.reply" class="history-reply">
            <p class="history-reply-label">厂商回复</p>
            <p class="history-reply-text">{{ item.reply }}</p>
          </div>
          <p v-else class="history-pending">
            {{ item.status === 'EXPIRED' ? '咨询已过期' : `等待回复，截止 ${formatDate(item.expiresAt)}` }}
          </p>
        </a-card>
      </div>
    </a-spin>
  </div>
</template>

<script lang="ts" setup>
// 下单前咨询面板：消费者提交问题并查看 72 小时窗口内的咨询记录与回复。
import { onMounted, reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import { useRoute, useRouter } from 'vue-router';
import {
  listMyProductInquiries,
  submitProductInquiry,
  type ProductInquiry,
  type ProductInquiryPayload,
  type ProductInquiryStatus
} from '../../services/catalogService';
import { friendlyErrorMessage } from '../../utils/error';
import { useAuthStore } from '../../stores/auth';

const props = defineProps<{
  productId: string;
}>();

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const form = reactive<ProductInquiryPayload>({
  contactName: '',
  contactMethod: '',
  message: ''
});

const submitting = ref(false);
const history = ref<ProductInquiry[]>([]);
const historyLoading = ref(false);

const ensureAuthenticated = () => {
  if (auth.isAuthenticated) {
    return true;
  }
  message.info('登录后才能发送咨询');
  router.push({ name: 'login', query: { redirect: route.fullPath } });
  return false;
};

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '--');

const inquiryStatusLabel = (status: ProductInquiryStatus) => {
  switch (status) {
    case 'OPEN':
      return '待回复';
    case 'RESPONDED':
      return '已回复';
    case 'EXPIRED':
      return '已过期';
    default:
      return status;
  }
};

const inquiryStatusColor = (status: ProductInquiryStatus) => {
  switch (status) {
    case 'OPEN':
      return 'orange';
    case 'RESPONDED':
      return 'green';
    case 'EXPIRED':
      return 'default';
    default:
      return 'default';
  }
};

const loadHistory = async () => {
  if (!auth.isAuthenticated) {
    history.value = [];
    return;
  }
  historyLoading.value = true;
  try {
    history.value = await listMyProductInquiries(props.productId);
  } catch (error) {
    console.error(error);
    history.value = [];
  } finally {
    historyLoading.value = false;
  }
};

const handleSubmit = async () => {
  if (!form.message) {
    message.warning('请填写咨询内容');
    return;
  }
  if (!ensureAuthenticated()) {
    return;
  }
  submitting.value = true;
  try {
    await submitProductInquiry(props.productId, form);
    message.success('咨询已发送，稍后留意通知');
    form.message = '';
    await loadHistory();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '提交咨询失败，请稍后重试'));
  } finally {
    submitting.value = false;
  }
};

onMounted(loadHistory);
watch(
  () => auth.isAuthenticated,
  () => loadHistory()
);
</script>

<style scoped>
.inquiry-stack {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.inquiry-hint {
  margin: 0;
  font-size: var(--font-size-caption);
  color: var(--color-text-secondary);
  text-align: center;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-card {
  border-radius: 12px;
}

.history-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.history-time {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.history-message {
  margin: 0;
  white-space: pre-wrap;
}

.history-reply {
  margin-top: 8px;
  padding: 8px 10px;
  background: var(--color-surface-muted);
  border-radius: 10px;
}

.history-reply-label {
  margin: 0 0 4px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.history-reply-text {
  margin: 0;
  white-space: pre-wrap;
}

.history-pending {
  margin: 8px 0 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
