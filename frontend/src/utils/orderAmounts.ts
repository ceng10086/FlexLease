import type { CartItem } from '../services/cartService';
import type { RentalOrderDetail, RentalOrderItem } from '../services/orderService';
import { parsePlanSnapshot, resolveBuyout, resolveDeposit, resolveRent } from './planSnapshot';

type PriceCarrier = {
  unitDepositAmount?: number | null;
  unitRentAmount?: number | null;
  buyoutPrice?: number | null;
  planSnapshot?: string | null;
  quantity?: number;
};

type OrderAmountSource<T extends PriceCarrier> = {
  depositAmount?: number | null;
  rentAmount?: number | null;
  totalAmount?: number | null;
  buyoutAmount?: number | null;
  items: Array<T & { quantity: number }>;
};

const snapshotOf = (item: PriceCarrier) => parsePlanSnapshot(item.planSnapshot ?? null);

const safeMult = (value: number, quantity?: number) => {
  if (!quantity || Number.isNaN(quantity)) {
    return value;
  }
  return value * quantity;
};

const safeNumber = (value?: number | null) =>
  typeof value === 'number' && Number.isFinite(value) ? value : 0;

export const resolveItemDeposit = (item: PriceCarrier): number => {
  const snapshot = snapshotOf(item);
  return safeNumber(resolveDeposit(item.unitDepositAmount, snapshot));
};

export const resolveItemRent = (item: PriceCarrier): number => {
  const snapshot = snapshotOf(item);
  return safeNumber(resolveRent(item.unitRentAmount, snapshot));
};

export const resolveItemBuyout = (item: PriceCarrier): number => {
  const snapshot = snapshotOf(item);
  return safeNumber(resolveBuyout(item.buyoutPrice, snapshot));
};

export const resolveOrderDeposit = <T extends PriceCarrier>(order: OrderAmountSource<T>): number => {
  if (typeof order.depositAmount === 'number') {
    return safeNumber(order.depositAmount);
  }
  return order.items.reduce(
    (sum, item) => sum + safeMult(resolveItemDeposit(item), item.quantity),
    0
  );
};

export const resolveOrderRent = <T extends PriceCarrier>(order: OrderAmountSource<T>): number => {
  if (typeof order.rentAmount === 'number') {
    return safeNumber(order.rentAmount);
  }
  return order.items.reduce(
    (sum, item) => sum + safeMult(resolveItemRent(item), item.quantity),
    0
  );
};

export const resolveOrderBuyout = <T extends PriceCarrier>(order: OrderAmountSource<T>): number => {
  if (typeof order.buyoutAmount === 'number') {
    return safeNumber(order.buyoutAmount);
  }
  return order.items.reduce(
    (sum, item) => sum + safeMult(resolveItemBuyout(item), item.quantity),
    0
  );
};

export const resolveOrderTotal = <T extends PriceCarrier>(order: OrderAmountSource<T>): number => {
  if (typeof order.totalAmount === 'number') {
    return safeNumber(order.totalAmount);
  }
  return resolveOrderDeposit(order) + resolveOrderRent(order) + resolveOrderBuyout(order);
};

export const cartItemDeposit = (item: CartItem) => resolveItemDeposit(item);
export const cartItemRent = (item: CartItem) => resolveItemRent(item);

export const rentalOrderDeposit = (order: RentalOrderDetail) =>
  resolveOrderDeposit<RentalOrderItem>({ ...order, items: order.items });
export const rentalOrderRent = (order: RentalOrderDetail) =>
  resolveOrderRent<RentalOrderItem>({ ...order, items: order.items });
export const rentalOrderTotal = (order: RentalOrderDetail) =>
  resolveOrderTotal<RentalOrderItem>({ ...order, items: order.items });
