import http from './http';

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
  transactionCount: number;
};

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export const initPayment = async (orderId: string, payload: PaymentInitPayload) => {
  const response = await http.post<ApiResponse<any>>(`/payments/${orderId}/init`, payload);
  return response.data.data;
};

export const fetchPayment = async (transactionId: string) => {
  const response = await http.get<ApiResponse<any>>(`/payments/${transactionId}`);
  return response.data.data;
};

export const confirmPayment = async (
  transactionId: string,
  payload: PaymentConfirmPayload
) => {
  const response = await http.post<ApiResponse<any>>(`/payments/${transactionId}/confirm`, payload);
  return response.data.data;
};

export const refundPayment = async (
  transactionId: string,
  payload: PaymentRefundPayload
) => {
  const response = await http.post<ApiResponse<any>>(`/payments/${transactionId}/refund`, payload);
  return response.data.data;
};

export const listSettlements = async (params: {
  vendorId?: string;
  from?: string;
  to?: string;
  refundFrom?: string;
  refundTo?: string;
}) => {
  const response = await http.get<ApiResponse<PaymentSettlementResponse[]>>('/payments/settlements', {
    params
  });
  return response.data.data;
};
