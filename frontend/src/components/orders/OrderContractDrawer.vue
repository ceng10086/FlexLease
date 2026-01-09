<template>
  <a-drawer
    :open="open"
    :width="drawerWidth"
    title="电子合同"
    :closable="true"
    @close="handleClose"
  >
    <template #extra>
      <a-space align="center">
        <a-tag :color="statusColor">{{ statusLabel }}</a-tag>
        <a-button type="link" size="small" :loading="loading" @click="loadContract">刷新</a-button>
      </a-space>
    </template>
    <a-spin :spinning="loading">
      <template v-if="contract">
        <a-descriptions :column="1" size="small" bordered>
          <a-descriptions-item label="合同编号">{{ contract.contractNumber }}</a-descriptions-item>
          <a-descriptions-item label="生成时间">{{ formatDate(contract.generatedAt) }}</a-descriptions-item>
          <a-descriptions-item label="签署人" v-if="contract.signedBy">
            {{ contract.signature || contract.signedBy }}
          </a-descriptions-item>
          <a-descriptions-item label="签署时间" v-if="contract.signedAt">
            {{ formatDate(contract.signedAt) }}
          </a-descriptions-item>
        </a-descriptions>
        <a-divider />
        <div class="contract-content">
          <a-typography-paragraph v-for="(line, index) in contentLines" :key="index">
            {{ line }}
          </a-typography-paragraph>
        </div>
        <div v-if="showSignForm" class="sign-form">
          <a-alert
            message="未签署"
            description="请确认合同内容后输入签名，签名成功后将自动回写订单。"
            type="warning"
            show-icon
          />
          <a-form layout="vertical">
            <a-form-item label="签名">
              <a-input
                v-model:value="signature"
                placeholder="请输入签名"
                :maxlength="60"
              />
            </a-form-item>
            <a-space>
              <a-button type="primary" :loading="signing" @click="handleSign">确认签署</a-button>
              <a-button @click="resetSignature">清空</a-button>
            </a-space>
          </a-form>
        </div>
        <a-result
          v-else-if="contract.status === 'SIGNED'"
          status="success"
          title="合同已完成签署"
          :sub-title="contract.signature ? `签名：${contract.signature}` : undefined"
        />
      </template>
      <a-empty v-else description="暂无合同内容" />
    </a-spin>
  </a-drawer>
</template>

<script lang="ts" setup>
// 合同抽屉：在订单详情中展示合同内容，并提供刷新/签署等相关操作入口。
import { computed, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import {
  fetchOrderContract,
  signOrderContract,
  type OrderContract
} from '../../services/orderService';
import { useViewport } from '../../composables/useViewport';

const props = defineProps<{
  orderId: string;
  open: boolean;
  allowSign?: boolean;
  userId?: string | null;
  defaultSignature?: string;
}>();

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void;
  (e: 'signed', contract: OrderContract): void;
}>();

const { isMobile, width: viewportWidth } = useViewport();
const drawerWidth = computed(() => {
  if (!isMobile.value) {
    return 720;
  }
  const base = viewportWidth.value || 360;
  return Math.min(Math.max(base - 32, 320), 720);
});

const loading = ref(false);
const signing = ref(false);
const contract = ref<OrderContract | null>(null);
const signature = ref('');

const allowSign = computed(() => props.allowSign ?? true);
const showSignForm = computed(
  () => allowSign.value && contract.value?.status === 'DRAFT'
);

const statusColor = computed(() => (contract.value?.status === 'SIGNED' ? 'green' : 'blue'));
const statusLabel = computed(() =>
  contract.value?.status === 'SIGNED' ? '已签署' : '待签署'
);

const contentLines = computed(() =>
  contract.value?.content ? contract.value.content.split(/\n+/).filter(Boolean) : []
);

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '-');

const resetSignature = () => {
  signature.value = props.defaultSignature ?? '';
};

const loadContract = async () => {
  if (!props.orderId) {
    return;
  }
  loading.value = true;
  try {
    contract.value = await fetchOrderContract(props.orderId);
    if (contract.value.status === 'SIGNED') {
      signature.value = contract.value.signature ?? '';
    } else if (!signature.value) {
      resetSignature();
    }
  } catch (error) {
    console.error('加载合同失败', error);
    message.error('加载合同失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const handleSign = async () => {
  if (!props.userId) {
    message.error('缺少用户信息，无法签署合同');
    return;
  }
  if (!signature.value.trim()) {
    message.warning('请先输入签名');
    return;
  }
  signing.value = true;
  try {
    const result = await signOrderContract(props.orderId, {
      userId: props.userId,
      signature: signature.value.trim()
    });
    contract.value = result;
    emit('signed', result);
    message.success('合同签署成功');
  } catch (error) {
    console.error('签署合同失败', error);
    message.error('签署合同失败，请稍后重试');
  } finally {
    signing.value = false;
  }
};

const handleClose = () => {
  emit('update:open', false);
};

watch(
  () => props.open,
  (open) => {
    if (open) {
      loadContract();
    } else {
      signing.value = false;
    }
  }
);
</script>

<style scoped>
.contract-content {
  max-height: 360px;
  overflow: auto;
  padding-right: 8px;
  margin-bottom: 16px;
  line-height: 1.6;
  color: #1f2937;
}

.sign-form {
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
</style>
