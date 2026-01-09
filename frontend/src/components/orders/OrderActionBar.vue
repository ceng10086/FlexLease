<template>
  <div class="action-bar">
    <a-button
      v-for="action in actions"
      :key="action.key"
      :type="action.primary ? 'primary' : 'default'"
      :ghost="action.ghost"
      size="large"
      @click="$emit(action.event)"
    >
      {{ action.label }}
    </a-button>
  </div>
</template>

<script lang="ts" setup>
// 订单操作栏：根据角色与状态装配可执行动作（支付/发货/续租/退租/买断等）。
import { computed } from 'vue';
import type { RentalOrderDetail } from '../../services/orderService';

const props = defineProps<{
  order: RentalOrderDetail;
}>();

const actions = computed(() => {
  const list: Array<{ key: string; label: string; primary?: boolean; ghost?: boolean; event: string }> = [];
  switch (props.order.status) {
    case 'PENDING_PAYMENT':
      list.push(
        { key: 'pay', label: '立即支付', primary: true, event: 'pay' },
        { key: 'cancel', label: '取消订单', event: 'cancel' }
      );
      break;
    case 'AWAITING_SHIPMENT':
      list.push({ key: 'chat', label: '联系厂商', event: 'chat', ghost: true });
      break;
    case 'AWAITING_RECEIPT':
      list.push(
        { key: 'confirm', label: '确认收货', primary: true, event: 'confirm' },
        { key: 'chat', label: '联系厂商', event: 'chat', ghost: true }
      );
      break;
    case 'IN_LEASE':
      list.push(
        { key: 'extend', label: '续租', event: 'extend' },
        { key: 'return', label: '申请退租', event: 'return' },
        { key: 'buyout', label: '申请买断', event: 'buyout' }
      );
      break;
    default:
      list.push({ key: 'chat', label: '查看聊天', event: 'chat' });
      break;
  }
  return list;
});

defineEmits<{
  (e: 'pay'): void;
  (e: 'cancel'): void;
  (e: 'confirm'): void;
  (e: 'extend'): void;
  (e: 'return'): void;
  (e: 'buyout'): void;
  (e: 'chat'): void;
}>();
</script>

<style scoped>
.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  justify-content: flex-end;
}
</style>
