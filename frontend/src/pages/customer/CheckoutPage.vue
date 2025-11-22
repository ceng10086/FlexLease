<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>确认订单</h2>
        <p class="page-header__meta">核对租赁商品信息，确认后提交订单。</p>
      </div>
      <a-button type="default" @click="goBack">返回</a-button>
    </div>

    <a-card class="checkout-steps-card" :bordered="false">
      <a-steps :current="1" responsive size="small">
        <a-step title="选择商品" description="在目录中挑选商品与方案" />
        <a-step title="确认订单" description="核对租期与费用，提交订单" />
        <a-step title="完成支付" description="支付押金/租金并等待发货" />
      </a-steps>
      <div class="page-guidance checkout-guidance">
        <div class="page-guidance__title">操作提示</div>
        <div class="page-guidance__content">
          <ul>
            <li>如需调整租期或 SKU，请回到商品详情修改后重新进入此页面。</li>
            <li>提交订单后平台将自动尝试完成首付款，失败时可前往订单详情补款。</li>
            <li>请确保收货信息正确，避免因误填导致物流延误。</li>
          </ul>
        </div>
      </div>
    </a-card>

    <a-row :gutter="24">
      <a-col :xs="24" :lg="14">
        <a-card title="租赁信息">
          <template v-if="product && currentPlan && currentSku">
            <a-descriptions :column="1" bordered size="small">
              <a-descriptions-item label="商品">{{ product.name }}</a-descriptions-item>
              <a-descriptions-item label="方案">{{ currentPlan.planType }} · {{ currentPlan.termMonths }} 个月</a-descriptions-item>
              <a-descriptions-item label="SKU">{{ currentSku.skuCode }}</a-descriptions-item>
              <a-descriptions-item label="数量">{{ form.quantity }}</a-descriptions-item>
              <a-descriptions-item label="租期">
                {{ form.leaseStart ? form.leaseStart.format('YYYY-MM-DD') : '立即' }} ~
                {{ form.leaseEnd?.format('YYYY-MM-DD') ?? '按方案计算' }}
              </a-descriptions-item>
            </a-descriptions>
          </template>
          <a-empty v-else description="未加载到商品信息" />
        </a-card>
        <a-card title="付款人信息" class="mt-16">
          <a-form layout="vertical">
            <a-form-item label="备注">
              <a-textarea v-model:value="form.remark" :rows="3" placeholder="给商家的附言（选填）" />
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="10">
        <a-card title="支付概要">
          <div class="summary-item">
            <span>押金合计</span>
            <strong>¥{{ formatCurrency(preview?.depositAmount ?? 0) }}</strong>
          </div>
          <div class="summary-item">
            <span>租金合计</span>
            <strong>¥{{ formatCurrency(preview?.rentAmount ?? 0) }}</strong>
          </div>
          <div class="summary-item summary-item--total">
            <span>应付总额</span>
            <strong>¥{{ formatCurrency(preview?.totalAmount ?? 0) }}</strong>
          </div>
          <div v-if="preview?.creditSnapshot" class="credit-summary">
            <div class="credit-summary__header">
              <span>信用等级</span>
              <a-tag :color="creditColor(preview.creditSnapshot?.creditTier)">
                {{ creditLabel(preview.creditSnapshot?.creditTier) }} ·
                {{ preview.creditSnapshot?.creditScore ?? '--' }} 分
              </a-tag>
            </div>
            <div class="credit-summary__details">
              <div>
                <div class="credit-summary__label">押金原价</div>
                <strong>¥{{ formatCurrency(preview.originalDepositAmount ?? 0) }}</strong>
              </div>
              <div>
                <div class="credit-summary__label">信用调整</div>
                <span>{{ depositAdjustmentText }}</span>
              </div>
            </div>
            <a-alert
              v-if="preview.creditSnapshot?.requiresManualReview"
              type="warning"
              show-icon
              message="信用预警：订单提交后需平台人工审核"
            />
          </div>
          <a-space direction="vertical" style="width: 100%">
            <a-button
              type="default"
              block
              :loading="loading.preview"
              :disabled="!orderReady"
              @click="handlePreview"
            >试算费用</a-button>
            <a-button
              type="primary"
              block
              size="large"
              :loading="loading.create"
              :disabled="!orderReady"
              @click="handleCreate"
            >提交订单</a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs, { type Dayjs } from 'dayjs';
import { useAuthStore } from '../../stores/auth';
import { fetchCatalogProduct, type CatalogProductDetail } from '../../services/catalogService';
import {
  previewOrder,
  createOrder,
  type OrderPreviewResponse,
  type RentalOrderDetail
} from '../../services/orderService';
import { serializePlanSnapshot } from '../../utils/planSnapshot';
import { rentalOrderDeposit, rentalOrderRent, rentalOrderTotal } from '../../utils/orderAmounts';
import { autoCompleteInitialPayment } from '../../utils/autoPayment';
import { friendlyErrorMessage } from '../../utils/error';
import { creditTierColor, creditTierLabel } from '../../types/credit';

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
}>(
  {
    quantity: Number(route.query.quantity ?? 1) || 1,
    leaseStart: route.query.leaseStart ? dayjs(route.query.leaseStart as string) : undefined,
    leaseEnd: route.query.leaseEnd ? dayjs(route.query.leaseEnd as string) : undefined,
    remark: ''
  }
);

const currentPlanId = computed(() => route.query.planId as string | undefined);
const currentSkuId = computed(() => route.query.skuId as string | undefined);

const currentPlan = computed(() => product.value?.rentalPlans.find((plan) => plan.id === currentPlanId.value));
const currentSku = computed(() => currentPlan.value?.skus.find((sku) => sku.id === currentSkuId.value));
const orderReady = computed(() => Boolean(product.value && currentPlan.value && currentSku.value && product.value.vendorId));

const formatCurrency = (value: number) => value.toFixed(2);
const creditColor = (tier?: OrderPreviewResponse['creditSnapshot']['creditTier'] | null) =>
  creditTierColor(tier ?? null);
const creditLabel = (tier?: OrderPreviewResponse['creditSnapshot']['creditTier'] | null) =>
  creditTierLabel(tier ?? null);
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

const loadProduct = async () => {
  const productId = route.query.productId as string | undefined;
  if (!productId) {
    message.error('缺少商品信息');
    router.replace({ name: 'catalog' });
    return;
  }
  try {
    product.value = await fetchCatalogProduct(productId);
  } catch (error) {
    console.error('加载商品失败', error);
    message.error(friendlyErrorMessage(error, '加载商品失败，请返回目录重试'));
    router.replace({ name: 'catalog' });
  }
};

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
        buyoutPrice: currentPlan.value.buyoutPrice ?? null
      }),
      quantity: form.quantity,
      unitRentAmount: currentPlan.value.rentAmountMonthly,
      unitDepositAmount: currentPlan.value.depositAmount,
      buyoutPrice: currentPlan.value.buyoutPrice
    }
  ];
};

const handlePreview = async () => {
  if (!auth.user?.id) {
    message.error('请先登录');
    router.replace({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }
  if (!orderReady.value) {
    message.warning('商品信息正在加载，请稍后再试');
    return;
  }
  loading.preview = true;
  try {
    preview.value = await previewOrder({
      userId: auth.user.id,
      vendorId: product.value!.vendorId,
      planType: currentPlan.value?.planType,
      leaseStartAt: form.leaseStart?.toISOString(),
      leaseEndAt: form.leaseEnd?.toISOString(),
      items: buildOrderItems()
    });
  } catch (error) {
    console.error('订单试算失败', error);
    message.error(friendlyErrorMessage(error, '订单试算失败'));
  } finally {
    loading.preview = false;
  }
};

const handleCreate = async () => {
  if (!auth.user?.id) {
    message.error('请先登录');
    router.replace({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }
  if (!orderReady.value) {
    message.warning('商品信息正在加载，请稍后再试');
    return;
  }
  loading.create = true;
  try {
    const order = await createOrder({
      userId: auth.user.id,
      vendorId: product.value!.vendorId,
      planType: currentPlan.value?.planType,
      leaseStartAt: form.leaseStart?.toISOString(),
      leaseEndAt: form.leaseEnd?.toISOString(),
      items: buildOrderItems()
    });
    await handleAutoPayment(order);
    router.replace({ name: 'orders' });
  } catch (error) {
    console.error('创建订单失败', error);
    message.error(friendlyErrorMessage(error, '创建订单失败'));
  } finally {
    loading.create = false;
  }
};

const handleAutoPayment = async (order: RentalOrderDetail) => {
  try {
    const deposit = rentalOrderDeposit(order);
    const rent = rentalOrderRent(order);
    const buyout = order.buyoutAmount ?? 0;
    const total = rentalOrderTotal(order);
    const paid = await autoCompleteInitialPayment({
      orderId: order.id,
      vendorId: order.vendorId,
      userId: auth.user!.id,
      amount: total,
      depositAmount: deposit,
      rentAmount: rent,
      buyoutAmount: buyout
    });
    if (paid) {
      message.success('订单创建并自动完成支付');
    } else {
      message.success('订单创建成功');
    }
  } catch (error) {
    console.error('自动支付失败', error);
    message.warning('订单已创建，但自动支付失败，请在订单详情中完成付款');
  }
};

const goBack = () => {
  router.back();
};

loadProduct();
</script>

<style scoped>
.summary-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
}

.summary-item--total {
  font-size: 16px;
  font-weight: 600;
}

.mt-16 {
  margin-top: 16px;
}

.checkout-steps-card {
  padding: 20px 24px;
  border-radius: 24px;
}

.checkout-guidance ul {
  padding-left: 18px;
  margin: 8px 0 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #475569;
  font-size: 13px;
}

.credit-summary {
  margin-top: 12px;
  padding: 14px;
  background: #f8fafc;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.credit-summary__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #475569;
}

.credit-summary__details {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #0f172a;
}

.credit-summary__label {
  font-size: 12px;
  color: #94a3b8;
}
</style>
