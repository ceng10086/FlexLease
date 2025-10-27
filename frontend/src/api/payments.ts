import http from '@/api/http';
import type { ApiResponseEnvelope, PaymentRecord, SettlementRecord } from '@/types';
import { samplePaymentRecords, sampleSettlements } from '@/utils/sampleData';

interface PaymentApiModel {
  id: string;
  orderId: string;
  amount: number;
  method: string;
  status: string;
  createdAt: string;
}

interface SettlementApiModel {
  id: string;
  vendorName: string;
  period: string;
  orderCount: number;
  grossAmount: number;
  refundAmount: number;
  netAmount: number;
  status: string;
}

export async function fetchPaymentRecords(orderId?: string): Promise<PaymentRecord[]> {
  const enableRemote = import.meta.env.VITE_ENABLE_PAYMENT_API === 'true';
  if (!orderId || !enableRemote) {
    return orderId
      ? samplePaymentRecords.filter((record) => record.orderId === orderId)
      : samplePaymentRecords;
  }
  try {
    const { data } = await http.get<ApiResponseEnvelope<PaymentApiModel[]>>(`/api/v1/orders/${orderId}/payments`);
    if (!data.success) {
      throw new Error(data.message ?? '无法获取支付记录');
    }
    return data.data.map((item) => ({
      id: item.id,
      orderId: item.orderId,
      amount: item.amount,
      method: item.method,
      status: item.status,
      createdAt: item.createdAt
    }));
  } catch (error) {
    console.warn('使用示例支付记录数据', error);
    return samplePaymentRecords.filter((record) => record.orderId === orderId);
  }
}

export async function fetchSettlements(vendorId?: string): Promise<SettlementRecord[]> {
  try {
    const { data } = await http.get<ApiResponseEnvelope<SettlementApiModel[]>>('/api/v1/payments/settlements', {
      params: { vendorId }
    });
    if (!data.success) {
      throw new Error(data.message ?? '无法获取结算记录');
    }
    return data.data.map((item) => ({
      id: item.id,
      vendorName: item.vendorName,
      period: item.period,
      orderCount: item.orderCount,
      grossAmount: item.grossAmount,
      refundAmount: item.refundAmount,
      netAmount: item.netAmount,
      status: item.status
    }));
  } catch (error) {
    console.warn('使用示例结算数据', error);
    return vendorId ? sampleSettlements.filter((item) => item.vendorName.includes(vendorId)) : sampleSettlements;
  }
}
