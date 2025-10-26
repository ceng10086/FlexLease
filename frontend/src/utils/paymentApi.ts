import api from './api';

export type PaymentScene = 'DEPOSIT' | 'RENT' | 'BUYOUT' | 'PENALTY';
export type PaymentChannel = 'MOCK' | 'ALIPAY' | 'WECHAT' | 'BANK_TRANSFER';
export type PaymentStatus = 'PENDING' | 'SUCCEEDED' | 'FAILED';

export type PaymentSplitPayload = {
  splitType: string;
  amount: number;
  beneficiary: string;
};

export type PaymentInitPayload = {
  userId: string;
  vendorId: string;
  scene: PaymentScene;
  channel: PaymentChannel;
  amount: number;
  description?: string;
  splits?: PaymentSplitPayload[];
};

export type PaymentConfirmPayload = {
  channelTransactionNo?: string;
  paidAt?: string;
};

export type PaymentCallbackPayload = {
  status: PaymentStatus;
  channelTransactionNo?: string;
  paidAt?: string;
  failureReason?: string;
};

export type PaymentRefundPayload = {
  amount: number;
  reason?: string;
};

export type PaymentSettlementResponse = {
  vendorId: string;
  totalAmount: number;
  depositAmount: number;
  rentAmount: number;
  buyoutAmount: number;
  penaltyAmount: number;
  refundedAmount: number;
  netAmount: number;
  lastPaidAt?: string | null;
  count: number;
};

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export const initPayment = async (orderId: string, payload: PaymentInitPayload) => {
  const response = await api.post<ApiResponse<any>>(`/payments/${orderId}/init`, payload);
  return response.data.data;
};

export const getPayment = async (transactionId: string) => {
  const response = await api.get<ApiResponse<any>>(`/payments/${transactionId}`);
  return response.data.data;
};

export const confirmPayment = async (transactionId: string, payload: PaymentConfirmPayload) => {
  const response = await api.post<ApiResponse<any>>(`/payments/${transactionId}/confirm`, payload);
  return response.data.data;
};

export const handlePaymentCallback = async (
  transactionId: string,
  payload: PaymentCallbackPayload
) => {
  const response = await api.post<ApiResponse<any>>(`/payments/${transactionId}/callback`, payload);
  return response.data.data;
};

export const refundPayment = async (transactionId: string, payload: PaymentRefundPayload) => {
  const response = await api.post<ApiResponse<any>>(`/payments/${transactionId}/refund`, payload);
  return response.data.data;
};

export const listSettlements = async (params: {
  vendorId?: string;
  from?: string;
  to?: string;
  refundFrom?: string;
  refundTo?: string;
}) => {
  const response = await api.get<ApiResponse<PaymentSettlementResponse[]>>('/payments/settlements', {
    params
  });
  return response.data.data;
};
