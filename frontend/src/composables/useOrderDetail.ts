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
