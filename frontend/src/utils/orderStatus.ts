/**
 * 订单状态展示映射：
 * - 订单状态 -> 中文文案/标签色（Ant Design Vue Tag 语义色）
 */
import type { OrderStatus } from '../services/orderService';

export const orderStatusLabel = (status: OrderStatus): string => {
  switch (status) {
    case 'PENDING_PAYMENT':
      return '待支付';
    case 'AWAITING_SHIPMENT':
      return '待发货';
    case 'AWAITING_RECEIPT':
      return '待收货';
    case 'IN_LEASE':
      return '履约中';
    case 'RETURN_REQUESTED':
      return '退租审批';
    case 'RETURN_IN_PROGRESS':
      return '退租中';
    case 'BUYOUT_REQUESTED':
      return '买断审批';
    case 'BUYOUT_COMPLETED':
      return '买断完成';
    case 'COMPLETED':
      return '已完成';
    case 'CANCELLED':
      return '已取消';
    case 'EXCEPTION_CLOSED':
      return '异常关闭';
    default:
      return status;
  }
};

export const orderStatusColor = (status: OrderStatus): string => {
  switch (status) {
    case 'PENDING_PAYMENT':
      return 'warning';
    case 'AWAITING_SHIPMENT':
    case 'AWAITING_RECEIPT':
      return 'processing';
    case 'IN_LEASE':
      return 'success';
    case 'RETURN_REQUESTED':
    case 'RETURN_IN_PROGRESS':
    case 'BUYOUT_REQUESTED':
      return 'purple';
    case 'COMPLETED':
    case 'BUYOUT_COMPLETED':
      return 'green';
    default:
      return 'default';
  }
};
