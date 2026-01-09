<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="签署电子合同"
        eyebrow="Contract"
        description="请确认合同内容后签署，签署完成将进入支付页面。"
      />
    </template>

    <PageSection title="合同内容">
      <a-skeleton v-if="loading" active :paragraph="{ rows: 8 }" />

      <template v-else>
        <DataStateBlock
          v-if="!contract"
          type="empty"
          title="暂无合同"
          description="请稍后刷新，或返回订单详情查看。"
        >
          <a-space>
            <a-button :loading="loading" @click="loadContract">刷新</a-button>
            <a-button type="primary" @click="goOrders">返回订单墙</a-button>
          </a-space>
        </DataStateBlock>

        <template v-else>
          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="合同编号">{{ contract.contractNumber }}</a-descriptions-item>
            <a-descriptions-item label="状态">
              <a-tag :color="contract.status === 'SIGNED' ? 'green' : 'blue'">
                {{ contract.status === 'SIGNED' ? '已签署' : '待签署' }}
              </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="生成时间">{{ formatDate(contract.generatedAt) }}</a-descriptions-item>
            <a-descriptions-item v-if="contract.signedAt" label="签署时间">
              {{ formatDate(contract.signedAt) }}
            </a-descriptions-item>
          </a-descriptions>

          <a-divider />

          <div class="contract-content">
            <a-typography-paragraph v-for="(line, index) in contentLines" :key="index">
              {{ line }}
            </a-typography-paragraph>
          </div>

          <a-divider />

          <a-alert
            v-if="contract.status !== 'SIGNED'"
            type="info"
            show-icon
            message="提示"
            description="合同签署是支付前置步骤（演示版：输入任意签名即可）。"
          />

          <a-result
            v-else
            status="success"
            title="合同已完成签署"
            :sub-title="contract.signature ? `签名：${contract.signature}` : undefined"
          />

          <a-form
            v-if="contract.status !== 'SIGNED'"
            layout="vertical"
            style="margin-top: var(--space-4)"
          >
            <a-form-item label="签名">
              <a-input v-model:value="signature" placeholder="请输入签名" :maxlength="60" />
            </a-form-item>
            <a-space>
              <a-button type="primary" :loading="signing" @click="handleSign">
                签署并进入支付
              </a-button>
              <a-button @click="resetSignature">清空</a-button>
            </a-space>
          </a-form>

          <a-button
            v-if="contract.status === 'SIGNED'"
            type="primary"
            style="margin-top: var(--space-4)"
            @click="goPayment"
          >
            进入支付
          </a-button>
        </template>
      </template>
    </PageSection>
  </PageShell>
</template>

<script lang="ts" setup>
// 合同签署页：展示电子合同草稿并提交签名完成签署。
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import PageShell from '../../../components/layout/PageShell.vue';
import PageHeader from '../../../components/layout/PageHeader.vue';
import PageSection from '../../../components/layout/PageSection.vue';
import DataStateBlock from '../../../components/feedback/DataStateBlock.vue';
import { useAuthStore } from '../../../stores/auth';
import { fetchOrderContract, signOrderContract, type OrderContract } from '../../../services/orderService';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const orderId = computed(() => route.params.orderId as string);

const loading = ref(false);
const signing = ref(false);
const contract = ref<OrderContract | null>(null);
const signature = ref(auth.user?.username ?? '');

const contentLines = computed(() =>
  contract.value?.content ? contract.value.content.split(/\n+/).filter(Boolean) : []
);

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '-');

const loadContract = async () => {
  if (!orderId.value) {
    return;
  }
  loading.value = true;
  try {
    contract.value = await fetchOrderContract(orderId.value);
    if (contract.value.status === 'SIGNED') {
      signature.value = contract.value.signature ?? signature.value;
    }
  } catch (error) {
    console.error('加载合同失败', error);
    message.error('加载合同失败，请稍后重试');
    contract.value = null;
  } finally {
    loading.value = false;
  }
};

const resetSignature = () => {
  signature.value = auth.user?.username ?? '';
};

const goOrders = () => {
  router.push({ name: 'orders' });
};

const goPayment = () => {
  router.push({ name: 'order-payment', params: { orderId: orderId.value } });
};

const handleSign = async () => {
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  if (!signature.value.trim()) {
    message.warning('请先输入签名');
    return;
  }

  signing.value = true;
  try {
    const result = await signOrderContract(orderId.value, {
      userId: auth.user.id,
      signature: signature.value.trim()
    });
    contract.value = result;
    message.success('合同签署成功');
    goPayment();
  } catch (error) {
    console.error('签署合同失败', error);
    message.error('签署合同失败，请稍后重试');
  } finally {
    signing.value = false;
  }
};

loadContract();
</script>

<style scoped>
.contract-content {
  max-height: 520px;
  overflow: auto;
  padding-right: 8px;
  line-height: 1.7;
}
</style>
