<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="确认订单"
        eyebrow="Checkout"
        description="核对方案与信用快照后提交订单，系统会自动尝试首付款。"
      />
    </template>

    <div class="checkout-grid">
      <div class="checkout-main">
        <PageSection title="租赁信息">
          <div v-if="product && currentPlan && currentSku" class="summary-list">
            <div class="summary-row">
              <span>商品</span>
              <strong>{{ product.name }}</strong>
            </div>
            <div class="summary-row">
              <span>方案</span>
              <strong>{{ currentPlan.planType }} · {{ currentPlan.termMonths }} 个月</strong>
            </div>
            <div class="summary-row">
              <span>SKU</span>
              <strong>{{ currentSku.skuCode }}</strong>
            </div>
            <div class="summary-row">
              <span>数量</span>
              <strong>{{ form.quantity }}</strong>
            </div>
            <div class="summary-row">
              <span>租期</span>
              <strong>{{ leaseText }}</strong>
            </div>
          </div>
          <DataStateBlock
            v-else
            type="loading"
            title="正在加载商品信息"
            description="如长时间未响应，请返回目录重新选择。"
          />
        </PageSection>
        <PageSection title="备注">
          <a-form layout="vertical">
            <a-form-item label="给厂商的附言">
              <a-textarea v-model:value="form.remark" :rows="3" placeholder="例如发货时间、配送注意事项（选填）" />
            </a-form-item>
          </a-form>
        </PageSection>
      </div>
      <div class="checkout-side">
        <PageSection title="费用概要">
          <div class="payment-summary">
            <div>
              <span>押金</span>
              <strong>¥{{ formatCurrency(preview?.depositAmount ?? 0) }}</strong>
            </div>
            <div>
              <span>租金</span>
              <strong>¥{{ formatCurrency(preview?.rentAmount ?? 0) }}</strong>
            </div>
            <div class="payment-summary__total">
              <span>应付总额</span>
              <strong>¥{{ formatCurrency(preview?.totalAmount ?? 0) }}</strong>
            </div>
          </div>
          <div v-if="preview?.creditSnapshot" class="credit-card">
            <div class="credit-card__header">
              <span>信用等级</span>
              <a-tag :color="creditColor(preview.creditSnapshot.creditTier)">
                {{ creditLabel(preview.creditSnapshot.creditTier) }} ·
                {{ preview.creditSnapshot.creditScore }} 分
              </a-tag>
            </div>
            <p>押金原价 ¥{{ formatCurrency(preview.originalDepositAmount) }}，{{ depositAdjustmentText }}</p>
            <a-alert
              v-if="preview.creditSnapshot.requiresManualReview"
              type="warning"
              show-icon
              message="信用预警：提交后需平台人工复核"
            />
          </div>
          <a-space direction="vertical" style="width: 100%">
            <a-button block :loading="loading.preview" :disabled="!orderReady" @click="handlePreview">
              重新试算
            </a-button>
            <a-button
              type="primary"
              block
              size="large"
              :disabled="!orderReady"
              :loading="loading.create"
              @click="handleCreate"
            >
              提交订单
            </a-button>
          </a-space>
        </PageSection>
      </div>
    </div>
  </PageShell>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs, { type Dayjs } from 'dayjs';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import PageSection from '../../components/layout/PageSection.vue';
import DataStateBlock from '../../components/feedback/DataStateBlock.vue';
import { useAuthStore } from '../../stores/auth';
import { fetchCatalogProduct, type CatalogProductDetail } from '../../services/catalogService';
import {
  previewOrder,
  createOrder,
  type OrderPreviewResponse,
  type RentalOrderDetail
} from '../../services/orderService';
import { serializePlanSnapshot } from '../../utils/planSnapshot';
import { autoCompleteInitialPayment } from '../../utils/autoPayment';
import { friendlyErrorMessage } from '../../utils/error';
import { creditTierColor, creditTierLabel } from '../../types/credit';
import { generateIdempotencyKey } from '../../utils/idempotency';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const product = ref<CatalogProductDetail | null>(null);
const preview = ref<OrderPreviewResponse | null>(null);
const loading = reactive({ preview: false, create: false });

const form = reactive<{
  quantity: number;
  leaseStart?: Dayjs;
  leaseEnd?: Dayjs;
  remark?: string;
}>({
  quantity: Number(route.query.quantity ?? 1) || 1,
  leaseStart: route.query.leaseStart ? dayjs(route.query.leaseStart as string) : undefined,
  leaseEnd: route.query.leaseEnd ? dayjs(route.query.leaseEnd as string) : undefined,
  remark: ''
});

const cartItemIds = computed(() => {
  const ids = route.query.cartItemIds as string | undefined;
  return ids ? ids.split(',').filter(Boolean) : undefined;
});

const planId = computed(() => route.query.planId as string | undefined);
const skuId = computed(() => route.query.skuId as string | undefined);

const leaseText = computed(() => {
  if (!form.leaseStart || !form.leaseEnd) {
    return '立即生效 · 按方案约定';
  }
  return `${form.leaseStart.format('YYYY-MM-DD')} ~ ${form.leaseEnd.format('YYYY-MM-DD')}`;
});

const loadProduct = async () => {
  const productId = route.query.productId as string | undefined;
  if (!productId) {
    message.error('缺少商品信息');
    router.replace({ name: 'catalog-feed' });
    return;
  }
  try {
    product.value = await fetchCatalogProduct(productId);
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载商品失败，请返回目录重试'));
    router.replace({ name: 'catalog-feed' });
  }
};

const currentPlan = computed(() =>
  product.value?.rentalPlans.find((plan) => plan.id === planId.value) ?? null
);
const currentSku = computed(() => currentPlan.value?.skus.find((sku) => sku.id === skuId.value) ?? null);
const orderReady = computed(() => Boolean(product.value && (cartItemIds.value || (currentPlan.value && currentSku.value))));

const formatCurrency = (value: number) => value.toFixed(2);
const creditColor = creditTierColor;
const creditLabel = creditTierLabel;
const depositAdjustmentText = computed(() => {
  if (!preview.value?.creditSnapshot) {
    return '按标准押金计算';
  }
  const rate = preview.value.creditSnapshot.depositAdjustmentRate ?? 1;
  if (Math.abs(rate - 1) < 0.001) {
    return '押金维持原价';
  }
  const percent = Math.abs(rate - 1) * 100;
  return rate < 1 ? `减免 ${percent.toFixed(0)}%` : `上浮 ${percent.toFixed(0)}%`;
});

const buildOrderItems = () => {
  if (!product.value || !currentPlan.value || !currentSku.value) {
    throw new Error('订单信息不完整');
  }
  return [
    {
      productId: product.value.id,
      skuId: currentSku.value.id,
      planId: currentPlan.value.id,
      productName: product.value.name,
      skuCode: currentSku.value.skuCode,
      planSnapshot: serializePlanSnapshot({
        planId: currentPlan.value.id,
        planType: currentPlan.value.planType,
        termMonths: currentPlan.value.termMonths,
        depositAmount: currentPlan.value.depositAmount,
        rentAmountMonthly: currentPlan.value.rentAmountMonthly,
        buyoutPrice: currentPlan.value.buyoutPrice ?? undefined
      }),
      quantity: form.quantity,
      unitRentAmount: currentPlan.value.rentAmountMonthly,
      unitDepositAmount: currentPlan.value.depositAmount,
      buyoutPrice: currentPlan.value.buyoutPrice ?? null
    }
  ];
};

const handlePreview = async () => {
  if (!orderReady.value || !auth.user) {
    return;
  }
  loading.preview = true;
  try {
    preview.value = await previewOrder({
      userId: auth.user.id,
      vendorId: product.value!.vendorId,
      planType: currentPlan.value?.planType,
      items: buildOrderItems()
    });
  } catch (error) {
    message.error(friendlyErrorMessage(error, '试算失败，请稍后重试'));
  } finally {
    loading.preview = false;
  }
};

const handleCreate = async () => {
  if (!orderReady.value || !auth.user) {
    return;
  }
  loading.create = true;
  try {
    const idempotencyKey = generateIdempotencyKey('order');
    const payload = {
      userId: auth.user.id,
      vendorId: product.value!.vendorId,
      planType: currentPlan.value?.planType,
      leaseStartAt: form.leaseStart?.toISOString(),
      leaseEndAt: form.leaseEnd?.toISOString(),
      remark: form.remark,
      items: cartItemIds.value ? [] : buildOrderItems(),
      cartItemIds: cartItemIds.value
    };
    const order = await createOrder(payload as any, { idempotencyKey });
    message.success('订单创建成功');
    try {
      const result = await autoCompleteInitialPayment({
        orderId: order.id,
        vendorId: order.vendorId,
        userId: order.userId,
        amount: order.totalAmount,
        depositAmount: order.depositAmount,
        rentAmount: order.rentAmount,
        buyoutAmount: order.buyoutAmount ?? undefined,
        description: '订单首付款'
      });
      if (result.succeeded) {
        message.success('首付款已自动完成');
      } else {
        message.info('已生成支付流水，请在订单详情查看进度');
      }
    } catch (paymentError) {
      message.warning(friendlyErrorMessage(paymentError, '自动支付未完成，请前往订单详情补缴'));
    }
    router.replace({ name: 'order-overview', params: { orderId: order.id } });
  } catch (error) {
    message.error(friendlyErrorMessage(error, '创建订单失败'));
  } finally {
    loading.create = false;
  }
};

loadProduct().then(() => handlePreview());
</script>

<style scoped>
.checkout-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--space-5);
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  gap: var(--space-3);
  font-weight: 500;
}

.payment-summary {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.payment-summary div {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.payment-summary__total strong {
  font-size: var(--font-size-title-lg);
  color: var(--color-primary);
}

.credit-card {
  padding: var(--space-3);
  border-radius: var(--radius-card);
  background: var(--color-surface-muted);
  border: 1px solid var(--color-border);
  margin-bottom: var(--space-3);
}

.credit-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

@media (max-width: 900px) {
  .checkout-grid {
    grid-template-columns: 1fr;
  }
}
</style>
