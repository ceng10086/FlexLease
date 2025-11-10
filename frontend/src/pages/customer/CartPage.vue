<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>我的购物车</h2>
        <p class="page-header__meta">管理已选商品，支持批量生成租赁订单。</p>
      </div>
      <a-space>
        <a-button @click="loadCart" :loading="loading">刷新</a-button>
        <a-popconfirm title="确认清空购物车？" @confirm="handleClear" :disabled="items.length === 0">
          <a-button danger :disabled="items.length === 0" :loading="clearing">清空</a-button>
        </a-popconfirm>
      </a-space>
    </div>

    <a-card>
      <a-table
        :loading="loading"
        :data-source="items"
        row-key="id"
        :row-selection="rowSelection"
        :pagination="false"
      >
        <a-table-column title="商品" key="product" data-index="productName" />
        <a-table-column title="SKU" key="sku" data-index="skuCode" />
        <a-table-column title="数量" key="quantity">
          <template #default="{ record }">
            <a-input-number
              :min="1"
              :value="record.quantity"
              :disabled="updating"
              @change="(value) => handleQuantityChange(record, Number(value))"
            />
          </template>
        </a-table-column>
        <a-table-column title="月租金" key="rent">
          <template #default="{ record }">¥{{ formatCurrency(resolveUnitRent(record)) }}</template>
        </a-table-column>
        <a-table-column title="押金" key="deposit">
          <template #default="{ record }">¥{{ formatCurrency(resolveUnitDeposit(record)) }}</template>
        </a-table-column>
        <a-table-column title="操作" key="actions">
          <template #default="{ record }">
            <a-popconfirm title="确定删除该条目？" @confirm="() => handleRemove(record)">
              <a-button type="link" danger>删除</a-button>
            </a-popconfirm>
          </template>
        </a-table-column>
      </a-table>

      <div class="cart-footer" v-if="items.length > 0">
        <div class="cart-summary">
          <p>已选择 {{ selectedItems.length }} 件商品</p>
          <p>押金小计：¥{{ formatCurrency(totalDeposit) }}</p>
          <p>租金小计：¥{{ formatCurrency(totalRent) }}</p>
        </div>
        <a-space>
          <a-button type="default" @click="goCatalog">继续选购</a-button>
          <a-button
            type="primary"
            :disabled="selectedItems.length === 0"
            :loading="submitting"
            @click="handleCheckout"
          >生成订单</a-button>
        </a-space>
      </div>
      <a-empty v-else description="购物车为空，去商品目录看看吧" />
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../../stores/auth';
import {
  fetchCartItems,
  updateCartItem,
  removeCartItem,
  clearCart,
  type CartItem
} from '../../services/cartService';
import { createOrder, type RentalOrderDetail } from '../../services/orderService';
import { parsePlanSnapshot, resolveDeposit, resolveRent } from '../../utils/planSnapshot';
import { autoCompleteInitialPayment } from '../../utils/autoPayment';

const router = useRouter();
const auth = useAuthStore();

const loading = ref(false);
const updating = ref(false);
const clearing = ref(false);
const submitting = ref(false);

const items = ref<CartItem[]>([]);
const selectedRowKeys = ref<string[]>([]);

const rowSelection = reactive({
  selectedRowKeys: selectedRowKeys.value as (string | number)[],
  onChange: (keys: (string | number)[]) => {
    selectedRowKeys.value = keys.map((key) => String(key));
  }
});

watch(selectedRowKeys, (keys) => {
  rowSelection.selectedRowKeys = keys;
});

const selectedItems = computed(() => items.value.filter((item) => selectedRowKeys.value.includes(item.id)));

const resolveUnitDeposit = (item: CartItem): number => {
  const snapshot = parsePlanSnapshot(item.planSnapshot);
  return resolveDeposit(item.unitDepositAmount, snapshot) ?? 0;
};

const resolveUnitRent = (item: CartItem): number => {
  const snapshot = parsePlanSnapshot(item.planSnapshot);
  return resolveRent(item.unitRentAmount, snapshot) ?? 0;
};

const totalDeposit = computed(() =>
  selectedItems.value.reduce((acc, item) => acc + resolveUnitDeposit(item) * item.quantity, 0)
);
const totalRent = computed(() =>
  selectedItems.value.reduce((acc, item) => acc + resolveUnitRent(item) * item.quantity, 0)
);

const formatCurrency = (value: number) => value.toFixed(2);

const requireUserId = () => {
  const userId = auth.user?.id;
  if (!userId) {
    message.error('请先登录账号');
    return null;
  }
  return userId;
};

const loadCart = async () => {
  const userId = requireUserId();
  if (!userId) {
    return;
  }
  loading.value = true;
  try {
    items.value = await fetchCartItems(userId);
    if (items.value.length === 0) {
      selectedRowKeys.value = [];
    } else {
      const reservedKeys = selectedRowKeys.value.filter((key) => items.value.some((item) => item.id === key));
      selectedRowKeys.value = reservedKeys;
    }
  } catch (error) {
    console.error('加载购物车失败', error);
    message.error('加载购物车失败，请稍后再试');
  } finally {
    loading.value = false;
  }
};

const handleQuantityChange = async (item: CartItem, quantity: number) => {
  if (!quantity || quantity < 1) {
    message.warning('数量至少为 1');
    return;
  }
  const userId = requireUserId();
  if (!userId) {
    return;
  }
  updating.value = true;
  try {
    await updateCartItem(item.id, { userId, quantity });
    await loadCart();
  } catch (error) {
    console.error('更新数量失败', error);
    message.error('更新数量失败，请稍后重试');
  } finally {
    updating.value = false;
  }
};

const handleRemove = async (item: CartItem) => {
  const userId = requireUserId();
  if (!userId) {
    return;
  }
  try {
    await removeCartItem(item.id, userId);
    message.success('已删除');
    await loadCart();
  } catch (error) {
    console.error('删除购物车条目失败', error);
    message.error('删除失败，请稍后再试');
  }
};

const handleClear = async () => {
  const userId = requireUserId();
  if (!userId) {
    return;
  }
  clearing.value = true;
  try {
    await clearCart(userId);
    message.success('购物车已清空');
    await loadCart();
  } catch (error) {
    console.error('清空购物车失败', error);
    message.error('清空失败，请稍后再试');
  } finally {
    clearing.value = false;
  }
};

const handleCheckout = async () => {
  const userId = requireUserId();
  if (!userId) {
    return;
  }
  if (selectedItems.value.length === 0) {
    message.warning('请选择至少一件商品');
    return;
  }
  const vendorIds = new Set(selectedItems.value.map((item) => item.vendorId));
  if (vendorIds.size > 1) {
    message.warning('仅支持同一厂商的商品一起下单');
    return;
  }
  submitting.value = true;
  try {
    const [vendorId] = Array.from(vendorIds);
    const order = await createOrder({
      userId,
      vendorId,
      items: [],
      cartItemIds: [...selectedRowKeys.value]
    });
    await handleAutoPayment(order, userId);
    selectedRowKeys.value = [];
    await loadCart();
    router.push({ name: 'orders' });
  } catch (error: any) {
    console.error('创建订单失败', error);
    const msg = error?.response?.data?.message ?? '创建订单失败，请稍后再试';
    message.error(msg);
  } finally {
    submitting.value = false;
  }
};

const handleAutoPayment = async (order: RentalOrderDetail, userId: string) => {
  try {
    const deposit = order.depositAmount ?? 0;
    const rent = order.rentAmount ?? 0;
    const buyout = order.buyoutAmount ?? 0;
    const total = order.totalAmount ?? deposit + rent + buyout;
    const paid = await autoCompleteInitialPayment({
      orderId: order.id,
      vendorId: order.vendorId,
      userId,
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
    message.warning('订单已创建，但自动支付失败，请在订单详情完成付款');
  }
};

const goCatalog = () => {
  router.push({ name: 'catalog' });
};

loadCart();
</script>

<style scoped>
.cart-footer {
  margin-top: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.cart-summary {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 14px;
}
</style>
