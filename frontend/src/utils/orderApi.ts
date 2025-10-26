import api from './api';

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'AWAITING_SHIPMENT'
  | 'IN_LEASE'
  | 'RETURN_REQUESTED'
  | 'RETURN_IN_PROGRESS'
  | 'COMPLETED'
  | 'BUYOUT_REQUESTED'
  | 'BUYOUT_COMPLETED'
  | 'CANCELLED';

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

type OrderItemPayload = {
  productId: string;
  skuId?: string | null;
  planId?: string | null;
  productName: string;
  skuCode?: string | null;
  planSnapshot?: string | null;
  quantity: number;
  unitRentAmount: number;
  unitDepositAmount: number;
  buyoutPrice?: number | null;
};

export type OrderPreviewPayload = {
  userId: string;
  vendorId: string;
  planType?: string | null;
  leaseStartAt?: string | null;
  leaseEndAt?: string | null;
  items: OrderItemPayload[];
};

export type OrderPreviewResponse = {
  depositAmount: number;
  rentAmount: number;
  totalAmount: number;
};

export type CreateOrderPayload = OrderPreviewPayload;

export type RentalOrderSummary = {
  id: string;
  orderNo: string;
  status: OrderStatus;
  userId: string;
  vendorId: string;
  totalAmount: number;
  createdAt: string;
};

export type PagedResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type OrderPaymentPayload = {
  userId: string;
  paymentReference: string;
  paidAmount: number;
};

export type OrderShipmentPayload = {
  vendorId: string;
  carrier: string;
  trackingNumber: string;
};

export type OrderActorPayload = {
  actorId: string;
};

export type OrderExtensionApplyPayload = {
  userId: string;
  additionalMonths: number;
  remark?: string;
};

export type OrderExtensionDecisionPayload = {
  vendorId: string;
  approve: boolean;
  remark?: string;
};

export type OrderReturnApplyPayload = {
  userId: string;
  reason?: string;
  logisticsCompany?: string;
  trackingNumber?: string;
};

export type OrderReturnDecisionPayload = {
  vendorId: string;
  approve: boolean;
  remark?: string;
};

export type OrderBuyoutApplyPayload = {
  userId: string;
  buyoutAmount?: number;
  remark?: string;
};

export type OrderBuyoutDecisionPayload = {
  vendorId: string;
  approve: boolean;
  remark?: string;
};

export const previewOrder = async (payload: OrderPreviewPayload): Promise<OrderPreviewResponse> => {
  const response = await api.post<ApiResponse<OrderPreviewResponse>>('/orders/preview', payload);
  return response.data.data;
};

export const createOrder = async (payload: CreateOrderPayload) => {
  const response = await api.post<ApiResponse<any>>('/orders', payload);
  return response.data.data;
};

export const getOrder = async (orderId: string) => {
  const response = await api.get<ApiResponse<any>>(`/orders/${orderId}`);
  return response.data.data;
};

export const listOrders = async (params: {
  userId?: string;
  vendorId?: string;
  status?: OrderStatus;
  page?: number;
  size?: number;
}) => {
  const response = await api.get<ApiResponse<PagedResponse<RentalOrderSummary>>>('/orders', {
    params
  });
  return response.data.data;
};

export const confirmOrderPayment = async (orderId: string, payload: OrderPaymentPayload) => {
  const response = await api.post<ApiResponse<any>>(`/orders/${orderId}/pay`, payload);
  return response.data.data;
};

export const cancelOrder = async (orderId: string, payload: { userId: string; reason?: string }) => {
  const response = await api.post<ApiResponse<any>>(`/orders/${orderId}/cancel`, payload);
  return response.data.data;
};

export const shipOrder = async (orderId: string, payload: OrderShipmentPayload) => {
  const response = await api.post<ApiResponse<any>>(`/orders/${orderId}/ship`, payload);
  return response.data.data;
};

export const confirmOrderReceive = async (orderId: string, payload: OrderActorPayload) => {
  const response = await api.post<ApiResponse<any>>(`/orders/${orderId}/confirm-receive`, payload);
  return response.data.data;
};

export const applyOrderExtension = async (orderId: string, payload: OrderExtensionApplyPayload) => {
  const response = await api.post<ApiResponse<any>>(`/orders/${orderId}/extend`, payload);
  return response.data.data;
};

export const decideOrderExtension = async (orderId: string, payload: OrderExtensionDecisionPayload) => {
  const response = await api.post<ApiResponse<any>>(`/orders/${orderId}/extend/approve`, payload);
  return response.data.data;
};

export const applyOrderReturn = async (orderId: string, payload: OrderReturnApplyPayload) => {
  const response = await api.post<ApiResponse<any>>(`/orders/${orderId}/return`, payload);
  return response.data.data;
};

export const decideOrderReturn = async (orderId: string, payload: OrderReturnDecisionPayload) => {
  const response = await api.post<ApiResponse<any>>(`/orders/${orderId}/return/approve`, payload);
  return response.data.data;
};

export const applyOrderBuyout = async (orderId: string, payload: OrderBuyoutApplyPayload) => {
  const response = await api.post<ApiResponse<any>>(`/orders/${orderId}/buyout`, payload);
  return response.data.data;
};

export const decideOrderBuyout = async (orderId: string, payload: OrderBuyoutDecisionPayload) => {
  const response = await api.post<ApiResponse<any>>(`/orders/${orderId}/buyout/confirm`, payload);
  return response.data.data;
};
