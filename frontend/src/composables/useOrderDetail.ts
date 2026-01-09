/**
 * 订单详情上下文（provide/inject）：
 * - 由 OrderDetailShell 提供 order/loading/refresh 等能力
 * - 子页面（overview/chat/proofs/timeline）通过 useOrderDetail 统一读取
 */
import { inject, provide } from 'vue';
import type { RentalOrderDetail } from '../services/orderService';

export type OrderDetailContext = {
  order: () => RentalOrderDetail | null;
  loading: () => boolean;
  refresh: () => Promise<void>;
  updateOrder: (detail: RentalOrderDetail) => void;
};

const ORDER_DETAIL_KEY = Symbol('order-detail');

export const provideOrderDetail = (context: OrderDetailContext) => {
  provide(ORDER_DETAIL_KEY, context);
};

export const useOrderDetail = (): OrderDetailContext => {
  const ctx = inject<OrderDetailContext | null>(ORDER_DETAIL_KEY, null);
  if (!ctx) {
    throw new Error('useOrderDetail must be used inside OrderDetailShell');
  }
  return ctx;
};
