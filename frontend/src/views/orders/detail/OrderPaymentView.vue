<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="支付订单"
        eyebrow="Payment"
        description="支持支付宝/微信（演示版：二维码为模拟内容）。"
      />
    </template>

    <PageSection title="支付信息">
      <a-skeleton v-if="loading.order" active :paragraph="{ rows: 6 }" />

      <template v-else>
        <DataStateBlock
          v-if="!order"
          type="empty"
          title="订单不存在"
          description="请返回订单墙重新进入。"
        >
          <a-button type="primary" @click="goOrders">返回订单墙</a-button>
        </DataStateBlock>

        <template v-else>
          <a-alert
            v-if="contractNotSigned"
            type="warning"
            show-icon
            message="请先签署合同"
            description="合同签署是支付前置步骤，请先完成签署。"
            style="margin-bottom: var(--space-4)"
          />

          <a-button
            v-if="contractNotSigned"
            type="primary"
            style="margin-bottom: var(--space-4)"
            @click="goContract"
          >
            去签署合同
          </a-button>

          <a-descriptions :column="1" size="small" bordered>
            <a-descriptions-item label="订单号">{{ order.orderNo }}</a-descriptions-item>
            <a-descriptions-item label="应付金额">¥{{ order.totalAmount.toFixed(2) }}</a-descriptions-item>
            <a-descriptions-item label="押金 / 租金">
              ¥{{ order.depositAmount.toFixed(2) }} / ¥{{ order.rentAmount.toFixed(2) }}
            </a-descriptions-item>
            <a-descriptions-item label="当前状态">{{ order.status }}</a-descriptions-item>
          </a-descriptions>

          <a-divider />

          <a-tabs v-model:activeKey="activeChannel" :animated="false">
            <a-tab-pane key="ALIPAY" tab="支付宝">
              <div class="pay-grid">
                <MockQrCode :value="`FLEXLEASE:ALIPAY:${order.id}`" :size="176" />
                <div class="pay-meta">
                  <h3 style="margin: 0">支付宝扫码支付</h3>
                  <p class="muted">模拟二维码，仅用于页面演示，不代表真实支付。</p>
                  <a-button
                    type="primary"
                    :disabled="contractNotSigned"
                    :loading="paying"
                    @click="handlePay"
                  >
                    我已完成支付（模拟）
                  </a-button>
                </div>
              </div>
            </a-tab-pane>
            <a-tab-pane key="WECHAT" tab="微信支付">
              <div class="pay-grid">
                <MockQrCode :value="`FLEXLEASE:WECHAT:${order.id}`" :size="176" />
                <div class="pay-meta">
                  <h3 style="margin: 0">微信扫码支付</h3>
                  <p class="muted">模拟二维码，仅用于页面演示，不代表真实支付。</p>
                  <a-button
                    type="primary"
                    :disabled="contractNotSigned"
                    :loading="paying"
                    @click="handlePay"
                  >
                    我已完成支付（模拟）
                  </a-button>
                </div>
              </div>
            </a-tab-pane>
          </a-tabs>
        </template>
      </template>
    </PageSection>
  </PageShell>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import PageShell from '../../../components/layout/PageShell.vue';
import PageHeader from '../../../components/layout/PageHeader.vue';
import PageSection from '../../../components/layout/PageSection.vue';
import DataStateBlock from '../../../components/feedback/DataStateBlock.vue';
import MockQrCode from '../../../components/payment/MockQrCode.vue';
import { useAuthStore } from '../../../stores/auth';
import { fetchOrder, fetchOrderContract, type RentalOrderDetail, type OrderContract } from '../../../services/orderService';
import { initPayment, type PaymentChannel } from '../../../services/paymentService';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const orderId = computed(() => route.params.orderId as string);

const loading = ref({ order: false, contract: false });
const paying = ref(false);

const order = ref<RentalOrderDetail | null>(null);
const contract = ref<OrderContract | null>(null);

const activeChannel = ref<PaymentChannel>('ALIPAY');

const contractNotSigned = computed(() => contract.value?.status !== 'SIGNED');

const loadOrder = async () => {
  loading.value.order = true;
  try {
    order.value = await fetchOrder(orderId.value);
  } catch (error) {
    console.error('加载订单失败', error);
    order.value = null;
  } finally {
    loading.value.order = false;
  }
};

const loadContract = async () => {
  loading.value.contract = true;
  try {
    contract.value = await fetchOrderContract(orderId.value);
  } catch (error) {
    console.error('加载合同失败', error);
    contract.value = null;
  } finally {
    loading.value.contract = false;
  }
};

const goOrders = () => {
  router.push({ name: 'orders' });
};

const goContract = () => {
  router.push({ name: 'order-contract', params: { orderId: orderId.value } });
};


const handlePay = async () => {
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  if (!order.value) {
    message.error('订单不存在');
    return;
  }
  if (contractNotSigned.value) {
    message.warning('请先完成合同签署');
    goContract();
    return;
  }

  paying.value = true;
  try {
    const tx = await initPayment(order.value.id, {
      userId: order.value.userId,
      vendorId: order.value.vendorId,
      scene: order.value.buyoutAmount ? 'BUYOUT' : order.value.rentAmount ? 'RENT' : 'DEPOSIT',
      channel: activeChannel.value,
      amount: order.value.totalAmount,
      description: `订单支付（${activeChannel.value}，演示）`
    });

    if (tx?.status === 'SUCCEEDED') {
      message.success('支付已完成（模拟）');
    } else {
      message.info('已生成支付单（演示），请在订单详情查看状态');
    }

    router.push({ name: 'orders' });
  } catch (error) {
    console.error('发起支付失败', error);
    message.error('发起支付失败，请稍后重试');
  } finally {
    paying.value = false;
  }
};

loadOrder();
loadContract();
</script>

<style scoped>
.pay-grid {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: var(--space-5);
  align-items: center;
}

.pay-meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.muted {
  color: var(--color-text-secondary);
  margin: 0;
}

@media (max-width: 900px) {
  .pay-grid {
    grid-template-columns: 1fr;
  }
}
</style>
