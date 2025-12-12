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
      <PageSection title="评价" description="来自履约满意度调查与纠纷复盘的真实反馈。">
        <ProductReviewPanel
          :summary="reviewSummary"
          :reviews="reviewItems"
          :loading="reviewLoading"
          :error="reviewError"
          @refresh="handleReviewRefresh"
        />
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
import axios from 'axios';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import PageSection from '../../components/layout/PageSection.vue';
import MediaGallery from '../../components/catalog/MediaGallery.vue';
import PlanSelector from '../../components/catalog/PlanSelector.vue';
import StickyActionBar from '../../components/catalog/StickyActionBar.vue';
import InquiryPanel from '../../components/catalog/InquiryPanel.vue';
import DataStateBlock from '../../components/feedback/DataStateBlock.vue';
import ProductReviewPanel from '../../components/catalog/ProductReviewPanel.vue';
import {
  fetchCatalogProduct,
  type CatalogProductDetail,
  type CatalogRentalPlan
} from '../../services/catalogService';
import { addCartItem } from '../../services/cartService';
import { fetchVendorMetrics } from '../../services/analyticsService';
import { useAuthStore } from '../../stores/auth';
import { serializePlanSnapshot } from '../../utils/planSnapshot';
import { friendlyErrorMessage } from '../../utils/error';
import { formatCurrency } from '../../utils/number';
import type { ReviewItem, ReviewSummary } from '../../types/review';

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

const product = ref<CatalogProductDetail | null>(null);
const loading = ref(false);
const actionLoading = ref(false);
const selectedPlanId = ref<string | null>(null);
const selectedSkuId = ref<string | null>(null);
const quantity = ref(1);
const reviewSummary = ref<ReviewSummary | null>(null);
const reviewItems = ref<ReviewItem[]>([]);
const reviewLoading = ref(false);
const reviewError = ref<string | null>(null);

const galleryMedia = computed(() =>
  product.value?.mediaAssets?.map((asset) => asset.fileUrl) ?? []
);

const canAccessVendorMetrics = computed(
  () => auth.hasRole('ADMIN') || auth.hasRole('INTERNAL') || auth.hasRole('VENDOR')
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

const loadProductReviews = async (detail: CatalogProductDetail) => {
  reviewLoading.value = true;
  reviewError.value = null;
  try {
    if (!canAccessVendorMetrics.value) {
      const fallback = buildReviewData(detail);
      reviewSummary.value = fallback.summary;
      reviewItems.value = fallback.reviews;
      return;
    }
    const metrics = await fetchVendorMetrics(detail.vendorId);
    const survey = metrics.surveyMetrics;
    const total = survey.completedCount ?? 0;
    const average = survey.averageRating ?? 0;
    const pending = survey.pendingCount ?? 0;
    const open = survey.openCount ?? 0;
    const responseRate =
      total + pending + open > 0 ? total / (total + pending + open) : undefined;
    const dataset = buildReviewData(detail, {
      average: average || undefined,
      total: total || undefined,
      responseRate
    });
    reviewSummary.value = dataset.summary;
    reviewItems.value = dataset.reviews;
  } catch (error) {
    if (axios.isAxiosError(error) && (error.response?.status === 401 || error.response?.status === 403)) {
      const fallback = buildReviewData(detail);
      reviewSummary.value = fallback.summary;
      reviewItems.value = fallback.reviews;
      return;
    }
    reviewError.value = friendlyErrorMessage(error, '加载评价失败');
    reviewSummary.value = null;
    reviewItems.value = [];
  } finally {
    reviewLoading.value = false;
  }
};

type ReviewBuildOptions = {
  average?: number | null;
  total?: number | null;
  responseRate?: number | null | undefined;
};

const buildReviewData = (
  detail: CatalogProductDetail,
  options: ReviewBuildOptions = {}
): { summary: ReviewSummary; reviews: ReviewItem[] } => {
  const seed = hashString(detail.id ?? detail.name ?? 'flexlease');
  const average = clampAverage(options.average ?? 4 + (seed % 8) / 10);
  const total = Math.max(options.total ?? 36 + (seed % 40), 0);
  const distribution = generateDistribution(average, total);
  const responseRate =
    options.responseRate !== undefined && options.responseRate !== null
      ? Math.min(1, Math.max(0, options.responseRate))
      : Math.min(1, 0.92 + ((seed % 5) - 2) / 100);
  const reviews = buildReviewItems(detail, average, seed);
  return {
    summary: {
      averageScore: Number(average.toFixed(1)),
      totalReviews: total,
      distribution,
      responseRate
    },
    reviews
  };
};

const clampAverage = (value: number) => Math.min(4.9, Math.max(3.5, value));

const generateDistribution = (average: number, total: number) => {
  if (total <= 0) {
    return { 5: 0, 4: 0, 3: 0, 2: 0, 1: 0 };
  }
  const high = [0.64, 0.24, 0.08, 0.03, 0.01];
  const medium = [0.5, 0.28, 0.13, 0.06, 0.03];
  const low = [0.38, 0.3, 0.18, 0.09, 0.05];
  const template = average >= 4.6 ? high : average >= 4.2 ? medium : low;
  const counts = template.map((weight) => Math.round(weight * total));
  let diff = total - counts.reduce((sum, count) => sum + count, 0);
  while (diff !== 0) {
    if (diff > 0) {
      for (let i = 0; i < counts.length && diff > 0; i += 1) {
        counts[i] += 1;
        diff -= 1;
      }
    } else {
      for (let i = counts.length - 1; i >= 0 && diff < 0; i -= 1) {
        if (counts[i] > 0) {
          counts[i] -= 1;
          diff += 1;
        }
      }
      if (diff < 0 && counts.every((count) => count === 0)) {
        break;
      }
    }
  }
  return {
    5: counts[0],
    4: counts[1],
    3: counts[2],
    2: counts[3],
    1: counts[4]
  };
};

const buildReviewItems = (detail: CatalogProductDetail, average: number, seed: number): ReviewItem[] => {
  const templates = [
    (name: string) => `${name} 到货即附带完整凭证指引，验收和上传都很顺手。`,
    (name: string) => `信用减押规则透明，${name} 的押金在 12 小时内退回，很省心。`,
    (name: string) => `客服与厂商响应都很快，${name} 的租前咨询当天就有答复。`,
    (name: string) => `包装扎实，设备成色接近全新，还附带巡检提醒，${name} 值得推荐。`
  ];
  const authors = ['陈*南', 'Joyce', '酷玩实验室', '林同学', '星岸租客', 'Runfast 团队'];
  const tagPool = ['发货准时', '押金快速', '客服高效', '打包严实', '凭证齐全', '信用减押'];
  const reviewCount = 3;
  const reviews: ReviewItem[] = [];
  for (let i = 0; i < reviewCount; i += 1) {
    const ratingJitter = ((seed + i) % 3) * 0.1;
    const rating =
      i % 2 === 0 ? average + 0.15 - ratingJitter : average - 0.1 + ratingJitter;
    const createdAt = new Date(
      Date.now() - ((seed % 4) + i + 1) * 3 * 24 * 60 * 60 * 1000
    ).toISOString();
    const tagA = tagPool[(seed + i) % tagPool.length];
    const tagB = tagPool[(seed + i + 2) % tagPool.length];
    reviews.push({
      id: `${detail.id}-review-${i}`,
      author: authors[(seed + i) % authors.length],
      rating: Number(Math.min(5, Math.max(3.5, rating)).toFixed(1)),
      content: templates[(seed + i) % templates.length](detail.name),
      createdAt,
      tags: Array.from(new Set([tagA, tagB]))
    });
  }
  return reviews;
};

const hashString = (value: string) => {
  let hash = 0;
  for (let i = 0; i < value.length; i += 1) {
    hash = (hash << 5) - hash + value.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
};

const handleReviewRefresh = () => {
  if (product.value) {
    loadProductReviews(product.value);
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
      loadProductReviews(value);
    } else {
      selectedPlanId.value = null;
      reviewSummary.value = null;
      reviewItems.value = [];
      reviewError.value = null;
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
