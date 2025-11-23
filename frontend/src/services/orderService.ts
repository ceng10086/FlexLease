import http from './http';
import type { CreditTier } from '../types/credit';

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

export type CreditSnapshot = {
  creditScore: number;
  creditTier: CreditTier;
  depositAdjustmentRate: number;
  requiresManualReview: boolean;
};

export type OrderPreviewResponse = {
  depositAmount: number;
  rentAmount: number;
  totalAmount: number;
  originalDepositAmount: number;
  creditSnapshot: CreditSnapshot;
};

export type CreateOrderPayload = OrderPreviewPayload & {
  cartItemIds?: string[];
};

export type RentalOrderSummary = {
  id: string;
  orderNo: string;
  status: OrderStatus;
  userId: string;
  vendorId: string;
  totalAmount: number;
  createdAt: string;
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

export type PagedResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type OrderForceClosePayload = {
  reason?: string;
};

export type OrderMessagePayload = {
  actorId: string;
  message: string;
};

export type UploadOrderProofPayload = {
  actorId: string;
  proofType: OrderProofType;
  description?: string;
  file: File;
};

export type RentalOrderItem = {
  id: string;
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

export type OrderEvent = {
  id: string;
  eventType: string;
  description: string;
  createdBy?: string | null;
  actorRole?: string | null;
  createdAt: string;
};

export type OrderExtension = {
  id: string;
  status: string;
  additionalMonths: number;
  requestedBy: string;
  requestedAt: string;
  decisionBy?: string | null;
  decisionAt?: string | null;
  remark?: string | null;
};

export type OrderReturn = {
  id: string;
  status: string;
  reason?: string | null;
  logisticsCompany?: string | null;
  trackingNumber?: string | null;
  requestedBy: string;
  requestedAt: string;
  decisionBy?: string | null;
  decisionAt?: string | null;
  remark?: string | null;
};

export type OrderProofType = 'SHIPMENT' | 'RECEIVE' | 'RETURN' | 'INSPECTION' | 'OTHER';

export type OrderProof = {
  id: string;
  proofType: OrderProofType;
  description?: string | null;
  fileUrl: string;
  contentType?: string | null;
  fileSize: number;
  uploadedBy: string;
  actorRole?: string | null;
  uploadedAt: string;
};

export type OrderContractStatus = 'DRAFT' | 'SIGNED';

export type OrderContract = {
  contractId: string;
  orderId: string;
  contractNumber: string;
  status: OrderContractStatus;
  content: string;
  signature?: string | null;
  signedBy?: string | null;
  generatedAt: string;
  signedAt?: string | null;
  updatedAt: string;
};

export type RentalOrderDetail = {
  id: string;
  orderNo: string;
  userId: string;
  vendorId: string;
  status: OrderStatus;
  planType?: string | null;
  totalAmount: number;
  depositAmount: number;
  originalDepositAmount: number;
  rentAmount: number;
  buyoutAmount?: number | null;
  creditScore: number;
  creditTier: CreditTier;
  depositAdjustmentRate: number;
  requiresManualReview: boolean;
  paymentTransactionId?: string | null;
  leaseStartAt?: string | null;
  leaseEndAt?: string | null;
  extensionCount: number;
  shippingCarrier?: string | null;
  shippingTrackingNo?: string | null;
  createdAt: string;
  updatedAt: string;
  items: RentalOrderItem[];
  events: OrderEvent[];
  extensions: OrderExtension[];
  returns: OrderReturn[];
  proofs: OrderProof[];
};

export const previewOrder = async (
  payload: OrderPreviewPayload
): Promise<OrderPreviewResponse> => {
  const response = await http.post<ApiResponse<OrderPreviewResponse>>('/orders/preview', payload);
  return response.data.data;
};

export const createOrder = async (payload: CreateOrderPayload): Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>('/orders', payload);
  return response.data.data;
};

export const fetchOrder = async (orderId: string): Promise<RentalOrderDetail> => {
  const response = await http.get<ApiResponse<RentalOrderDetail>>(`/orders/${orderId}`);
  return response.data.data;
};

export const listOrders = async (params: {
  userId?: string;
  vendorId?: string;
  status?: OrderStatus;
  page?: number;
  size?: number;
}) => {
  const response = await http.get<ApiResponse<PagedResponse<RentalOrderSummary>>>('/orders', {
    params
  });
  return response.data.data;
};

export const listAdminOrders = async (params: {
  userId?: string;
  vendorId?: string;
  status?: OrderStatus;
  page?: number;
  size?: number;
}) => {
  const response = await http.get<ApiResponse<PagedResponse<RentalOrderSummary>>>('/admin/orders', {
    params
  });
  return response.data.data;
};

export const confirmOrderPayment = async (
  orderId: string,
  payload: OrderPaymentPayload
) : Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>(`/orders/${orderId}/pay`, payload);
  return response.data.data;
};

export const cancelOrder = async (
  orderId: string,
  payload: { userId: string; reason?: string }
) : Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>(`/orders/${orderId}/cancel`, payload);
  return response.data.data;
};

export const shipOrder = async (
  orderId: string,
  payload: OrderShipmentPayload
) : Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>(`/orders/${orderId}/ship`, payload);
  return response.data.data;
};

export const confirmOrderReceive = async (
  orderId: string,
  payload: OrderActorPayload
) : Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>(`/orders/${orderId}/confirm-receive`, payload);
  return response.data.data;
};

export const applyOrderExtension = async (
  orderId: string,
  payload: OrderExtensionApplyPayload
) : Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>(`/orders/${orderId}/extend`, payload);
  return response.data.data;
};

export const decideOrderExtension = async (
  orderId: string,
  payload: OrderExtensionDecisionPayload
) : Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>(`/orders/${orderId}/extend/approve`, payload);
  return response.data.data;
};

export const applyOrderReturn = async (
  orderId: string,
  payload: OrderReturnApplyPayload
) : Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>(`/orders/${orderId}/return`, payload);
  return response.data.data;
};

export const decideOrderReturn = async (
  orderId: string,
  payload: OrderReturnDecisionPayload
) : Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>(`/orders/${orderId}/return/approve`, payload);
  return response.data.data;
};

export const applyOrderBuyout = async (
  orderId: string,
  payload: OrderBuyoutApplyPayload
) : Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>(`/orders/${orderId}/buyout`, payload);
  return response.data.data;
};

export const decideOrderBuyout = async (
  orderId: string,
  payload: OrderBuyoutDecisionPayload
) : Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>(`/orders/${orderId}/buyout/confirm`, payload);
  return response.data.data;
};

export const forceCloseOrder = async (
  orderId: string,
  payload: OrderForceClosePayload
) : Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>(`/admin/orders/${orderId}/force-close`, payload);
  return response.data.data;
};

export const postOrderMessage = async (
  orderId: string,
  payload: OrderMessagePayload
): Promise<RentalOrderDetail> => {
  const response = await http.post<ApiResponse<RentalOrderDetail>>(`/orders/${orderId}/messages`, payload);
  return response.data.data;
};

export const uploadOrderProof = async (
  orderId: string,
  payload: UploadOrderProofPayload
): Promise<OrderProof> => {
  const formData = new FormData();
  formData.append('actorId', payload.actorId);
  formData.append('proofType', payload.proofType);
  if (payload.description) {
    formData.append('description', payload.description);
  }
  formData.append('file', payload.file);
  const response = await http.post<ApiResponse<OrderProof>>(`/orders/${orderId}/proofs`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return response.data.data;
};

export const fetchOrderContract = async (orderId: string): Promise<OrderContract> => {
  const response = await http.get<ApiResponse<OrderContract>>(`/orders/${orderId}/contract`);
  return response.data.data;
};

export const signOrderContract = async (
  orderId: string,
  payload: { userId: string; signature: string }
): Promise<OrderContract> => {
  const response = await http.post<ApiResponse<OrderContract>>(`/orders/${orderId}/contract/sign`, payload);
  return response.data.data;
};
