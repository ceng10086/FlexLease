<template>
  <article class="cart-card" :class="{ 'cart-card--selected': selected }">
    <header class="cart-card__header">
      <a-checkbox :checked="selected" @change="$emit('toggle', item.id)" />
      <div class="cart-card__title">
        <h3>{{ item.productName }}</h3>
        <p>{{ planDescription }}</p>
      </div>
      <a-button type="text" danger size="small" @click="$emit('remove', item)">
        删除
      </a-button>
    </header>
    <div class="cart-card__meta">
      <span>SKU：{{ item.skuCode ?? '未命名 SKU' }}</span>
      <span>ID：{{ item.id.slice(0, 8) }} · 厂商 {{ item.vendorId }}</span>
    </div>
    <div class="cart-card__pricing">
      <div>
        <label>月租金</label>
        <strong>¥{{ (rent * item.quantity).toFixed(2) }}</strong>
        <small>¥{{ rent.toFixed(2) }}/件</small>
      </div>
      <div>
        <label>押金</label>
        <strong>¥{{ (deposit * item.quantity).toFixed(2) }}</strong>
        <small>¥{{ deposit.toFixed(2) }}/件</small>
      </div>
      <div>
        <label>买断价</label>
        <strong>{{ buyout > 0 ? `¥${(buyout * item.quantity).toFixed(2)}` : '业务审批' }}</strong>
        <small>{{ buyout > 0 ? `¥${buyout.toFixed(2)}/件` : '详见详情页' }}</small>
      </div>
      <div class="cart-card__quantity">
        <label>数量</label>
        <a-input-number
          :value="item.quantity"
          :min="1"
          size="small"
          :disabled="updating"
          @change="(value) => $emit('quantity', Number(value ?? item.quantity))"
        />
      </div>
    </div>
  </article>
</template>

<script lang="ts" setup>
// 购物车条目卡片：展示商品/方案摘要，并支持数量调整与删除。
import { computed } from 'vue';
import type { CartItem } from '../../services/cartService';
import { cartItemDeposit, cartItemRent } from '../../utils/orderAmounts';
import { parsePlanSnapshot } from '../../utils/planSnapshot';

const props = defineProps<{
  item: CartItem;
  selected: boolean;
  updating?: boolean;
}>();

defineEmits<{
  (e: 'toggle', id: string): void;
  (e: 'quantity', quantity: number): void;
  (e: 'remove', item: CartItem): void;
}>();

const rent = computed(() => cartItemRent(props.item));
const deposit = computed(() => cartItemDeposit(props.item));
const snapshot = computed(() => parsePlanSnapshot(props.item.planSnapshot));
const buyout = computed(() => snapshot.value?.buyoutPrice ?? props.item.buyoutPrice ?? 0);

const planDescription = computed(() => {
  const planType = snapshot.value?.planType ?? '标准方案';
  const term = snapshot.value?.termMonths ? `${snapshot.value.termMonths} 个月` : '灵活租期';
  return `${planType} · ${term}`;
});
</script>

<style scoped>
.cart-card {
  border-radius: var(--radius-card);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.cart-card--selected {
  border-color: rgba(37, 99, 235, 0.4);
  box-shadow: 0 20px 40px rgba(37, 99, 235, 0.15);
}

.cart-card__header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
}

.cart-card__title h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.cart-card__title p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 13px;
}

.cart-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  font-size: 12px;
  color: var(--color-text-secondary);
}

.cart-card__pricing {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: var(--space-3);
  align-items: center;
}

.cart-card__pricing label {
  display: block;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.cart-card__pricing strong {
  display: block;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.cart-card__pricing small {
  color: var(--color-text-secondary);
}

.cart-card__quantity {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}
</style>
