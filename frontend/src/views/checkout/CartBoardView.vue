<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="购物车 · 三端统一交互"
        eyebrow="Cart"
        :description="`已选 ${selectedCount} / ${items.length} 条，信用减押会在结算页展示。`"
      >
        <template #actions>
          <a-space>
            <a-checkbox :checked="allSelected" :disabled="!items.length" @change="toggleSelectAll">
              全选
            </a-checkbox>
            <a-button :loading="loading" @click="loadCart">刷新</a-button>
            <a-popconfirm
              title="确认清空购物车？"
              :disabled="!items.length"
              @confirm="handleClear"
            >
              <a-button danger :disabled="!items.length" :loading="clearing">清空</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </PageHeader>
    </template>

    <PageSection title="待下单条目" description="卡片展示方案、押金与租金，随时调整数量。">
      <a-skeleton v-if="loading" active :paragraph="{ rows: 6 }" />
      <template v-else>
        <div v-if="items.length" class="cart-grid">
          <CartItemCard
            v-for="item in items"
            :key="item.id"
            :item="item"
            :selected="selectedIds.has(item.id)"
            :updating="updatingItemId === item.id"
            @toggle="toggleSelection"
            @quantity="(quantity) => handleQuantityChange(item, quantity)"
            @remove="() => handleRemove(item)"
          />
        </div>
        <DataStateBlock
          v-else
          type="empty"
          title="购物车为空"
          description="瀑布流选品后可随时加入购物车。"
        >
          <a-button type="primary" @click="goCatalog">去逛逛</a-button>
        </DataStateBlock>
      </template>
    </PageSection>

    <StickyActionBar
      :price="summaryTotal"
      label="合计（押金 + 月租）"
      price-suffix="（元，买断金额下单后确认）"
      cta-label="生成订单"
      secondary-label="继续逛逛"
      :disabled="selectedItems.length === 0"
      :busy="actionBusy"
      @primary="handleCheckout"
      @secondary="goCatalog"
    />

    <a-drawer
      :open="previewVisible"
      placement="bottom"
      height="auto"
      :closable="false"
      class="cart-preview"
      @close="closePreview"
      :mask-closable="false"
    >
      <div class="cart-preview__header">
        <div>
          <p class="eyebrow">订单试算</p>
          <h3>信用权益已应用</h3>
        </div>
        <a-button type="text" @click="closePreview">关闭</a-button>
      </div>
      <div class="cart-preview__totals">
        <div>
          <span>押金</span>
          <strong>¥{{ formatCurrency(previewData?.depositAmount ?? 0) }}</strong>
        </div>
        <div>
          <span>租金</span>
          <strong>¥{{ formatCurrency(previewData?.rentAmount ?? 0) }}</strong>
        </div>
        <div class="cart-preview__total">
          <span>应付总额</span>
          <strong>¥{{ formatCurrency(previewData?.totalAmount ?? 0) }}</strong>
        </div>
        <small class="muted">
          原始押金 ¥{{ formatCurrency(previewData?.originalDepositAmount ?? 0) }} ·
          {{ depositAdjustmentText }}
        </small>
      </div>
      <div v-if="previewCredit" class="cart-preview__credit">
        <div class="cart-preview__credit-header">
          <span>信用等级</span>
          <a-tag :color="creditColor(previewCredit.creditTier)">
            {{ creditLabel(previewCredit.creditTier) }} · {{ previewCredit.creditScore }} 分
          </a-tag>
        </div>
        <a-alert
          v-if="previewCredit.requiresManualReview"
          type="warning"
          show-icon
          message="信用预警：提交后需平台人工复核"
        />
      </div>
      <div class="cart-preview__items">
        <div v-for="item in selectedItems" :key="item.id" class="cart-preview__item">
          <div class="cart-preview__item-main">
            <strong>{{ item.productName }}</strong>
            <span class="muted">×{{ item.quantity }}</span>
          </div>
          <div class="cart-preview__item-sub">
            <span>{{ planSummary(item) }}</span>
            <span>SKU {{ item.skuCode || '—' }}</span>
          </div>
          <div class="cart-preview__item-sub">
            <span>押金 ¥{{ formatCurrency(cartItemDeposit(item) ?? 0) }}</span>
            <span>月租 ¥{{ formatCurrency(cartItemRent(item) ?? 0) }}</span>
          </div>
        </div>
      </div>
      <div class="cart-preview__actions">
        <a-button block size="large" @click="closePreview">返回调整</a-button>
        <a-button
          type="primary"
          block
          size="large"
          :loading="submitting"
          @click="confirmCheckout"
        >
          确认创建订单
        </a-button>
      </div>
    </a-drawer>
  </PageShell>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import PageSection from '../../components/layout/PageSection.vue';
import CartItemCard from '../../components/catalog/CartItemCard.vue';
import StickyActionBar from '../../components/catalog/StickyActionBar.vue';
import DataStateBlock from '../../components/feedback/DataStateBlock.vue';
import {
  fetchCartItems,
  updateCartItem,
  removeCartItem,
  clearCart,
  type CartItem
} from '../../services/cartService';
import {
  createOrder,
  previewOrder,
  type OrderPreviewResponse,
  type RentalOrderDetail
} from '../../services/orderService';
import { cartItemDeposit, cartItemRent } from '../../utils/orderAmounts';
import { useAuthStore } from '../../stores/auth';
import { autoCompleteInitialPayment } from '../../utils/autoPayment';
import { generateIdempotencyKey } from '../../utils/idempotency';
import { creditTierColor, creditTierLabel } from '../../types/credit';
import { parsePlanSnapshot } from '../../utils/planSnapshot';

const auth = useAuthStore();
const router = useRouter();

const loading = ref(false);
const clearing = ref(false);
const submitting = ref(false);
const previewing = ref(false);
const previewVisible = ref(false);
const previewData = ref<OrderPreviewResponse | null>(null);
const previewSelectionKey = ref('');
const updatingItemId = ref<string | null>(null);

const items = ref<CartItem[]>([]);
const selectedIds = ref<Set<string>>(new Set());

const requireUserId = () => {
  const id = auth.user?.id;
  if (!id) {
    message.error('请先登录账号');
    throw new Error('未登录');
  }
  return id;
};

const loadCart = async () => {
  const userId = requireUserId();
  loading.value = true;
  try {
    const data = await fetchCartItems(userId);
    items.value = data;
    const existing = new Set(
      Array.from(selectedIds.value).filter((id) => data.some((item) => item.id === id))
    );
    if (!existing.size && data.length) {
      data.forEach((item) => existing.add(item.id));
    }
    selectedIds.value = existing;
  } catch (error) {
    console.error('加载购物车失败', error);
    message.error('加载购物车失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const toggleSelection = (id: string) => {
  const next = new Set(selectedIds.value);
  if (next.has(id)) {
    next.delete(id);
  } else {
    next.add(id);
  }
  selectedIds.value = next;
};

const toggleSelectAll = () => {
  if (selectedIds.value.size === items.value.length) {
    selectedIds.value = new Set();
  } else {
    selectedIds.value = new Set(items.value.map((item) => item.id));
  }
};

const selectedItems = computed(() =>
  items.value.filter((item) => selectedIds.value.has(item.id))
);

const selectedCount = computed(() => selectedItems.value.length);
const allSelected = computed(
  () => items.value.length > 0 && selectedIds.value.size === items.value.length
);

const selectionFingerprint = computed(() =>
  selectedItems.value
    .map((item) => `${item.id}:${item.quantity}`)
    .sort()
    .join('|')
);

const previewCredit = computed(() => previewData.value?.creditSnapshot ?? null);
const depositAdjustmentText = computed(() => {
  if (!previewCredit.value) {
    return '押金按标准比例计算';
  }
  const rate = previewCredit.value.depositAdjustmentRate ?? 1;
  if (Math.abs(rate - 1) < 0.001) {
    return '押金维持原价';
  }
  const percent = Math.abs(rate - 1) * 100;
  return rate < 1 ? `减免 ${percent.toFixed(0)}%` : `上浮 ${percent.toFixed(0)}%`;
});

const actionBusy = computed(() => submitting.value || previewing.value);

const totalDeposit = computed(() =>
  selectedItems.value.reduce((sum, item) => sum + cartItemDeposit(item) * item.quantity, 0)
);

const totalRent = computed(() =>
  selectedItems.value.reduce((sum, item) => sum + cartItemRent(item) * item.quantity, 0)
);

const summaryTotal = computed(() => totalDeposit.value + totalRent.value);

const handleQuantityChange = async (item: CartItem, quantity: number) => {
  if (quantity < 1 || Number.isNaN(quantity)) {
    message.warning('数量至少为 1');
    return;
  }
  const userId = requireUserId();
  updatingItemId.value = item.id;
  try {
    await updateCartItem(item.id, { userId, quantity });
    await loadCart();
  } catch (error) {
    console.error('更新数量失败', error);
    message.error('更新数量失败，请稍后再试');
  } finally {
    updatingItemId.value = null;
  }
};

const handleRemove = async (item: CartItem) => {
  const userId = requireUserId();
  updatingItemId.value = item.id;
  try {
    await removeCartItem(item.id, userId);
    const next = new Set(selectedIds.value);
    next.delete(item.id);
    selectedIds.value = next;
    await loadCart();
    message.success('已移除该条目');
  } catch (error) {
    console.error('移除购物车条目失败', error);
    message.error('移除失败，请稍后再试');
  } finally {
    updatingItemId.value = null;
  }
};

const handleClear = async () => {
  if (!items.value.length) {
    return;
  }
  const userId = requireUserId();
  clearing.value = true;
  try {
    await clearCart(userId);
    selectedIds.value = new Set();
    await loadCart();
    message.success('购物车已清空');
  } catch (error) {
    console.error('清空购物车失败', error);
    message.error('清空失败，请稍后再试');
  } finally {
    clearing.value = false;
  }
};

const goCatalog = () => {
  router.push({ name: 'catalog-feed' });
};

const buildPreviewPayload = (userId: string, vendorId: string) => ({
  userId,
  vendorId,
  items: selectedItems.value.map((item) => ({
    productId: item.productId,
    skuId: item.skuId,
    planId: item.planId ?? null,
    productName: item.productName,
    skuCode: item.skuCode ?? null,
    planSnapshot: item.planSnapshot ?? null,
    quantity: item.quantity,
    unitRentAmount: item.unitRentAmount,
    unitDepositAmount: item.unitDepositAmount,
    buyoutPrice: item.buyoutPrice ?? null
  }))
});

const planSummary = (item: CartItem) => {
  const snapshot = parsePlanSnapshot(item.planSnapshot);
  if (!snapshot) {
    return '租赁方案已锁定';
  }
  const type = snapshot.planType ?? '租赁方案';
  return snapshot.termMonths ? `${type} · ${snapshot.termMonths}个月` : type;
};

const vendorSelection = () => Array.from(new Set(selectedItems.value.map((item) => item.vendorId)));

const handleCheckout = async () => {
  const userId = requireUserId();
  if (!selectedItems.value.length) {
    message.warning('请选择至少一个条目');
    return;
  }
  const vendorIds = vendorSelection();
  if (vendorIds.length > 1) {
    message.warning('仅支持同一厂商的商品一起下单');
    return;
  }
  if (previewData.value && previewSelectionKey.value === selectionFingerprint.value) {
    previewVisible.value = true;
    return;
  }
  previewing.value = true;
  try {
    previewData.value = await previewOrder(buildPreviewPayload(userId, vendorIds[0]));
    previewSelectionKey.value = selectionFingerprint.value;
    previewVisible.value = true;
  } catch (error) {
    console.error('订单试算失败', error);
    message.error('订单试算失败，请稍后重试');
  } finally {
    previewing.value = false;
  }
};

const confirmCheckout = async () => {
  const userId = requireUserId();
  if (previewSelectionKey.value !== selectionFingerprint.value) {
    message.warning('购物车已调整，请重新试算订单');
    previewVisible.value = false;
    return;
  }
  const vendorIds = vendorSelection();
  if (vendorIds.length !== 1) {
    message.warning('请确保仅选择一个厂商的商品');
    return;
  }
  submitting.value = true;
  try {
    const idempotencyKey = generateIdempotencyKey();
    const order = await createOrder(
      {
        userId,
        vendorId: vendorIds[0],
        items: [],
        cartItemIds: Array.from(selectedIds.value)
      },
      { idempotencyKey }
    );
    await handleAutoPayment(order, userId);
    selectedIds.value = new Set();
    await loadCart();
    previewVisible.value = false;
    previewData.value = null;
    previewSelectionKey.value = '';
    router.push({ name: 'orders' });
  } catch (error: any) {
    console.error('创建订单失败', error);
    const msg = error?.response?.data?.message ?? '创建订单失败，请稍后再试';
    message.error(msg);
  } finally {
    submitting.value = false;
  }
};

const closePreview = () => {
  previewVisible.value = false;
};

const handleAutoPayment = async (order: RentalOrderDetail, userId: string) => {
  try {
    if (order.requiresManualReview) {
      message.info('订单已提交，待人工审核后由平台发起支付');
      return;
    }
    const deposit = order.depositAmount ?? 0;
    const rent = order.rentAmount ?? 0;
    const buyout = order.buyoutAmount ?? 0;
    const total = order.totalAmount ?? deposit + rent + buyout;
    const paymentResult = await autoCompleteInitialPayment({
      orderId: order.id,
      vendorId: order.vendorId,
      userId,
      amount: total,
      depositAmount: deposit,
      rentAmount: rent,
      buyoutAmount: buyout
    });
    if (paymentResult.succeeded) {
      message.success('订单创建并自动完成支付');
    } else if (paymentResult.transactionId) {
      message.info('订单创建成功，支付单已生成，待自动确认');
    } else {
      message.success('订单创建成功');
    }
  } catch (error) {
    console.error('自动支付失败', error);
    message.warning('订单已创建，但自动支付失败，请在订单详情完成付款');
  }
};

onMounted(() => {
  loadCart();
});

watch(selectionFingerprint, (fingerprint) => {
  if (fingerprint !== previewSelectionKey.value) {
    previewSelectionKey.value = '';
    previewData.value = null;
    previewVisible.value = false;
  }
});

const creditColor = creditTierColor;
const creditLabel = creditTierLabel;
const formatCurrency = (value: number) => value.toFixed(2);
</script>

<style scoped>
.cart-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--space-4);
}

.cart-preview :deep(.ant-drawer-body) {
  padding: var(--space-5) var(--space-4);
}

.cart-preview__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.cart-preview__header h3 {
  margin: 0;
  font-size: var(--font-size-title-lg);
}

.eyebrow {
  color: var(--color-text-secondary);
  font-size: var(--font-size-caption);
  margin-bottom: var(--space-1);
}

.cart-preview__totals {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.cart-preview__totals div {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cart-preview__total strong {
  font-size: var(--font-size-title-lg);
  color: var(--color-primary);
}

.muted {
  color: var(--color-text-secondary);
}

.cart-preview__credit {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  padding: var(--space-3);
  background: var(--color-surface-muted);
  margin-bottom: var(--space-4);
}

.cart-preview__credit-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
}

.cart-preview__items {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.cart-preview__item {
  border-bottom: 1px solid var(--color-border);
  padding-bottom: var(--space-2);
}

.cart-preview__item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.cart-preview__item-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cart-preview__item-sub {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-caption);
  color: var(--color-text-secondary);
}

.cart-preview__actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-3);
}

@media (max-width: 640px) {
  .cart-preview__actions {
    grid-template-columns: 1fr;
  }
}
</style>
