<template>
  <div class="page-container" v-if="product">
    <div class="page-header">
      <div>
        <h2>{{ product.name }}</h2>
        <p class="page-header__meta">分类：{{ product.categoryCode }}</p>
      </div>
      <a-button type="default" @click="goBack">返回目录</a-button>
    </div>

    <a-row :gutter="24">
      <a-col :xs="24" :lg="14">
        <a-card title="租赁方案">
          <a-radio-group v-model:value="selectedPlanId" class="plan-group">
            <a-radio
              v-for="plan in product.rentalPlans"
              :key="plan.id"
              :value="plan.id"
            >
              <div class="plan-card">
                <div class="plan-card__title">{{ plan.planType }} · {{ plan.termMonths }} 个月</div>
                <div class="plan-card__meta">押金：¥{{ formatCurrency(plan.depositAmount) }}</div>
                <div class="plan-card__meta">月租金：¥{{ formatCurrency(plan.rentAmountMonthly) }}</div>
                <div class="plan-card__meta" v-if="plan.buyoutPrice">买断价：¥{{ formatCurrency(plan.buyoutPrice) }}</div>
              </div>
            </a-radio>
          </a-radio-group>
        </a-card>

        <a-card title="商品描述" class="mt-16">
          <p v-if="product.description">{{ product.description }}</p>
          <a-empty v-else description="暂无描述" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="10">
        <a-card title="选择配置">
          <a-form layout="vertical">
            <a-form-item label="SKU">
              <a-select v-model:value="selectedSkuId" placeholder="选择库存">
                <a-select-option
                  v-for="sku in currentPlan?.skus ?? []"
                  :key="sku.id"
                  :value="sku.id"
                  :disabled="sku.stockAvailable <= 0"
                >
                  {{ sku.skuCode }} · 库存 {{ sku.stockAvailable }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="数量">
              <a-input-number v-model:value="form.quantity" :min="1" :max="currentSku?.stockAvailable ?? 99" style="width: 100%" />
            </a-form-item>
            <a-row :gutter="12">
              <a-col :span="12">
                <a-form-item label="起租日期">
                  <a-date-picker v-model:value="form.leaseStart" format="YYYY-MM-DD" style="width: 100%" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="预计归还">
                  <a-date-picker v-model:value="form.leaseEnd" format="YYYY-MM-DD" style="width: 100%" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-space direction="vertical" style="width: 100%">
              <a-button type="primary" block size="large" :disabled="!canCheckout" @click="goCheckout">
                去结算
              </a-button>
              <a-button block :disabled="!canCheckout" :loading="addingToCart" @click="handleAddToCart">
                加入购物车
              </a-button>
            </a-space>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
  <div v-else class="page-container">
    <a-card :loading="loading">正在加载商品信息...</a-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import dayjs, { type Dayjs } from 'dayjs';
import { fetchCatalogProduct, type CatalogProductDetail } from '../../services/catalogService';
import { addCartItem } from '../../services/cartService';
import { useAuthStore } from '../../stores/auth';

const route = useRoute();
const router = useRouter();
const productId = route.params.productId as string;
const auth = useAuthStore();

const loading = ref(false);
const product = ref<CatalogProductDetail | null>(null);
const selectedPlanId = ref<string | undefined>();
const selectedSkuId = ref<string | undefined>();

const form = reactive<{ quantity: number; leaseStart?: Dayjs; leaseEnd?: Dayjs }>({ quantity: 1 });
const addingToCart = ref(false);

const currentPlan = computed(() => product.value?.rentalPlans.find((plan) => plan.id === selectedPlanId.value));
const currentSku = computed(() => currentPlan.value?.skus.find((sku) => sku.id === selectedSkuId.value));

const canCheckout = computed(() => currentPlan.value && currentSku.value && form.quantity > 0);

const formatCurrency = (value: number) => value.toFixed(2);

const loadProduct = async () => {
  loading.value = true;
  try {
    product.value = await fetchCatalogProduct(productId);
    selectedPlanId.value = product.value.rentalPlans[0]?.id;
    selectedSkuId.value = product.value.rentalPlans[0]?.skus[0]?.id;
  } catch (error) {
    console.error('加载商品详情失败', error);
    message.error('加载商品详情失败，请返回重试');
  } finally {
    loading.value = false;
  }
};

const goCheckout = () => {
  if (!canCheckout.value || !product.value || !currentPlan.value || !currentSku.value) {
    message.warning('请完善配置后再结算');
    return;
  }
  router.push({
    name: 'checkout',
    query: {
      productId: product.value.id,
      planId: currentPlan.value.id,
      skuId: currentSku.value.id,
      quantity: form.quantity.toString(),
      leaseStart: form.leaseStart ? dayjs(form.leaseStart).format('YYYY-MM-DD') : undefined,
      leaseEnd: form.leaseEnd ? dayjs(form.leaseEnd).format('YYYY-MM-DD') : undefined
    }
  });
};

const handleAddToCart = async () => {
  if (!auth.user?.id) {
    message.error('请先登录');
    router.replace({ name: 'login', query: { redirect: route.fullPath } });
    return;
  }
  if (!canCheckout.value || !product.value || !currentPlan.value || !currentSku.value) {
    message.warning('请先选择租赁方案和库存');
    return;
  }
  addingToCart.value = true;
  try {
    await addCartItem({
      userId: auth.user.id,
      vendorId: product.value.vendorId,
      productId: product.value.id,
      skuId: currentSku.value.id,
      planId: currentPlan.value.id,
      productName: product.value.name,
      skuCode: currentSku.value.skuCode,
      planSnapshot: JSON.stringify({ termMonths: currentPlan.value.termMonths }),
      quantity: form.quantity,
      unitRentAmount: currentPlan.value.rentAmountMonthly,
      unitDepositAmount: currentPlan.value.depositAmount,
      buyoutPrice: currentPlan.value.buyoutPrice ?? null
    });
    message.success('已加入购物车');
  } catch (error) {
    console.error('加入购物车失败', error);
    message.error('加入购物车失败，请稍后重试');
  } finally {
    addingToCart.value = false;
  }
};

const goBack = () => {
  router.push({ name: 'catalog' });
};

watch(selectedPlanId, (planId) => {
  if (!planId || planId === currentPlan.value?.id) {
    return;
  }
  const plan = product.value?.rentalPlans.find((item) => item.id === planId);
  selectedSkuId.value = plan?.skus[0]?.id;
});

loadProduct();
</script>

<style scoped>
.plan-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.plan-card {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.plan-card__title {
  font-weight: 600;
}

.plan-card__meta {
  color: #64748b;
}

.mt-16 {
  margin-top: 16px;
}
</style>
