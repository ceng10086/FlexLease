<template>
  <PageShell>
    <template #header>
      <PageHeader
        :title="product?.name ?? '加载中...'"
        eyebrow="Product"
        :description="product?.description ?? '厂商将在下单前提供完整履约指引。'"
      >
        <template #actions>
          <a-tag color="processing">{{ product?.categoryCode ?? '品类识别中' }}</a-tag>
        </template>
      </PageHeader>
    </template>

    <div v-if="loading" class="detail-loading">
      <a-skeleton active :paragraph="{ rows: 6 }" />
    </div>
    <div v-else-if="!product" class="detail-empty">
      <DataStateBlock type="error" title="未找到该商品" description="请返回目录重试。" />
    </div>
    <div v-else class="detail-grid">
      <PageSection title="媒体">
        <MediaGallery :cover="product.coverImageUrl" :media="galleryMedia" />
      </PageSection>
      <PageSection title="方案选择">
        <PlanSelector v-model="selectedPlanId" :plans="product.rentalPlans" />
        <div class="sku-grid">
          <h4>SKU / 库存</h4>
          <a-radio-group
            v-model:value="selectedSkuId"
            button-style="solid"
            class="sku-radio-group"
          >
            <a-radio-button
              v-for="sku in selectedPlan?.skus ?? []"
              :key="sku.id"
              :value="sku.id"
              :disabled="sku.stockAvailable <= 0"
            >
              {{ sku.skuCode }}（余 {{ sku.stockAvailable }}）
            </a-radio-button>
          </a-radio-group>
          <div class="quantity-row">
            <span>数量</span>
            <a-input-number
              v-model:value="quantity"
              :min="1"
              :max="selectedSku?.stockAvailable ?? 99"
            />
          </div>
        </div>
        <div class="plan-stats">
          <div class="stat-card">
            <div class="stat-card__label">月租金</div>
            <div class="stat-card__value">¥{{ displayCurrency(selectedPlan?.rentAmountMonthly) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-card__label">押金</div>
            <div class="stat-card__value">¥{{ displayCurrency(selectedPlan?.depositAmount) }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-card__label">租期</div>
            <div class="stat-card__value">{{ selectedPlan?.termMonths ?? '--' }} 个月</div>
          </div>
          <div class="stat-card">
            <div class="stat-card__label">买断价</div>
            <div class="stat-card__value">
              {{ selectedPlan?.buyoutPrice != null ? `¥${displayCurrency(selectedPlan.buyoutPrice)}` : '支持业务审批' }}
            </div>
          </div>
        </div>
      </PageSection>
      <PageSection title="方案亮点">
        <ul class="plan-highlights">
          <li>信用好的用户将自动触发押金减免，Checkout 页面会展示信用快照。</li>
          <li>所有履约节点可上传凭证并与厂商聊天，系统同步站内信提醒。</li>
          <li>支持续租 / 退租 / 买断全流程，各阶段费用透明可查。</li>
        </ul>
      </PageSection>
      <PageSection title="下单前咨询" description="72 小时内回复，消息会同步通知中心。">
        <InquiryPanel :product-id="product.id" />
      </PageSection>

      <StickyActionBar
        :price="(selectedPlan?.rentAmountMonthly ?? 0) * quantity"
        :disabled="!selectReady"
        :busy="actionLoading"
        cta-label="立即租赁"
        secondary-label="加入购物车"
        @primary="handleCheckout"
        @secondary="handleAddToCart"
      />
    </div>
  </PageShell>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import PageSection from '../../components/layout/PageSection.vue';
import MediaGallery from '../../components/catalog/MediaGallery.vue';
import PlanSelector from '../../components/catalog/PlanSelector.vue';
import StickyActionBar from '../../components/catalog/StickyActionBar.vue';
import InquiryPanel from '../../components/catalog/InquiryPanel.vue';
import DataStateBlock from '../../components/feedback/DataStateBlock.vue';
import {
  fetchCatalogProduct,
  type CatalogProductDetail,
  type CatalogRentalPlan
} from '../../services/catalogService';
import { addCartItem } from '../../services/cartService';
import { useAuthStore } from '../../stores/auth';
import { serializePlanSnapshot } from '../../utils/planSnapshot';
import { friendlyErrorMessage } from '../../utils/error';
import { formatCurrency } from '../../utils/number';

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

const product = ref<CatalogProductDetail | null>(null);
const loading = ref(false);
const actionLoading = ref(false);
const selectedPlanId = ref<string | null>(null);
const selectedSkuId = ref<string | null>(null);
const quantity = ref(1);

const galleryMedia = computed(() =>
  product.value?.mediaAssets?.map((asset) => asset.fileUrl) ?? []
);

const loadProduct = async () => {
  const productId = route.params.productId as string | undefined;
  if (!productId) {
    product.value = null;
    return;
  }
  loading.value = true;
  try {
    product.value = await fetchCatalogProduct(productId);
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载商品失败'));
    product.value = null;
  } finally {
    loading.value = false;
  }
};

const displayCurrency = (value: number | null | undefined) =>
  value == null ? '--' : formatCurrency(value);

watch(
  () => route.params.productId,
  () => loadProduct(),
  { immediate: true }
);

watch(
  () => product.value,
  (value) => {
    if (value) {
      const planFromRoute = route.query.planId as string | undefined;
      const matchedPlan = planFromRoute
        ? value.rentalPlans.find((plan) => plan.id === planFromRoute)
        : undefined;
      selectedPlanId.value = matchedPlan?.id ?? value.rentalPlans[0]?.id ?? null;
    } else {
      selectedPlanId.value = null;
    }
  }
);

const selectedPlan = computed<CatalogRentalPlan | null>(() =>
  product.value?.rentalPlans.find((plan) => plan.id === selectedPlanId.value) ?? null
);

watch(
  () => selectedPlan.value,
  (plan) => {
    const skuFromRoute = route.query.skuId as string | undefined;
    const matchedSku = skuFromRoute ? plan?.skus.find((sku) => sku.id === skuFromRoute) : null;
    selectedSkuId.value = matchedSku?.id ?? plan?.skus[0]?.id ?? null;
  }
);

const selectedSku = computed(() =>
  selectedPlan.value?.skus.find((sku) => sku.id === selectedSkuId.value) ?? null
);

const selectReady = computed(() => Boolean(product.value && selectedPlan.value && selectedSku.value));

const handleAddToCart = async () => {
  if (!auth.user) {
    message.info('请先登录后再加入购物车');
    router.push({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }
  if (!selectReady.value) {
    message.warning('请选择完整方案和 SKU');
    return;
  }
  actionLoading.value = true;
  try {
    await addCartItem({
      userId: auth.user.id,
      vendorId: product.value!.vendorId,
      productId: product.value!.id,
      skuId: selectedSku.value!.id,
      planId: selectedPlan.value!.id,
      productName: product.value!.name,
      skuCode: selectedSku.value!.skuCode,
      planSnapshot: serializePlanSnapshot({
        planId: selectedPlan.value!.id,
        planType: selectedPlan.value!.planType,
        termMonths: selectedPlan.value!.termMonths,
        depositAmount: selectedPlan.value!.depositAmount,
        rentAmountMonthly: selectedPlan.value!.rentAmountMonthly,
        buyoutPrice: selectedPlan.value!.buyoutPrice ?? undefined
      }),
      quantity: quantity.value,
      unitRentAmount: selectedPlan.value!.rentAmountMonthly,
      unitDepositAmount: selectedPlan.value!.depositAmount,
      buyoutPrice: selectedPlan.value!.buyoutPrice ?? null
    });
    message.success('已加入购物车，可统一结算');
    router.push({ name: 'cart' });
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加入购物车失败'));
  } finally {
    actionLoading.value = false;
  }
};

const handleCheckout = () => {
  if (!selectReady.value) {
    message.warning('请选择完整方案和 SKU');
    return;
  }
  router.push({
    name: 'checkout',
    query: {
      productId: product.value!.id,
      planId: selectedPlan.value!.id,
      skuId: selectedSku.value!.id,
      quantity: quantity.value.toString()
    }
  });
};
</script>

<style scoped>
.detail-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.detail-loading,
.detail-empty {
  padding: var(--space-5);
}

.sku-grid {
  margin-top: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.sku-radio-group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.quantity-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.plan-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--space-3);
  margin-top: var(--space-4);
}

.plan-highlights {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text-secondary);
}
</style>
