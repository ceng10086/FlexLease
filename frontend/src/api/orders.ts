import http from '@/api/http';
import type { ApiResponseEnvelope, PagedResult, RentalOrderSummary } from '@/types';
import { sampleCustomerOrders, sampleVendorOrders } from '@/utils/sampleData';

interface OrderTimelineApiItem {
  step: string;
  timestamp: string;
  status: 'COMPLETED' | 'PENDING' | 'WARNING';
}

interface RentalOrderApiModel {
  id: string;
  productName: string;
  vendorName: string;
  planName: string;
  rentalMode: string;
  status: string;
  startedAt: string;
  endedAt: string;
  amountDue: number;
  nextAction?: string;
  timeline?: OrderTimelineApiItem[];
}

interface PagedOrderResponse {
  content: RentalOrderApiModel[];
  totalElements: number;
  size: number;
  number: number;
}

function mapOrder(item: RentalOrderApiModel): RentalOrderSummary {
  return {
    id: item.id,
    productName: item.productName,
    vendorName: item.vendorName,
    planName: item.planName,
    mode: item.rentalMode,
    status: item.status,
    startDate: item.startedAt,
    endDate: item.endedAt,
    amountDue: item.amountDue,
    nextAction: item.nextAction,
    timeline:
      item.timeline?.map((step) => ({
        label: step.step,
        time: step.timestamp,
        status:
          step.status === 'COMPLETED'
            ? 'completed'
            : step.status === 'WARNING'
            ? 'warning'
            : 'pending'
      })) ?? []
  };
}

export async function fetchOrdersByUser(userId: string, page = 1, size = 10): Promise<PagedResult<RentalOrderSummary>> {
  try {
    const { data } = await http.get<ApiResponseEnvelope<PagedOrderResponse>>('/api/v1/orders', {
      params: { userId, page, size }
    });
    if (!data.success) {
      throw new Error(data.message ?? '无法获取订单');
    }
    return {
      content: data.data.content.map(mapOrder),
      total: data.data.totalElements,
      page: data.data.number + 1,
      size: data.data.size
    };
  } catch (error) {
    console.warn('使用示例用户订单数据', error);
    return {
      content: sampleCustomerOrders,
      total: sampleCustomerOrders.length,
      page: 1,
      size: sampleCustomerOrders.length
    };
  }
}

export async function fetchOrdersByVendor(vendorId: string, page = 1, size = 10): Promise<PagedResult<RentalOrderSummary>> {
  try {
    const { data } = await http.get<ApiResponseEnvelope<PagedOrderResponse>>('/api/v1/orders', {
      params: { vendorId, page, size }
    });
    if (!data.success) {
      throw new Error(data.message ?? '无法获取订单');
    }
    return {
      content: data.data.content.map(mapOrder),
      total: data.data.totalElements,
      page: data.data.number + 1,
      size: data.data.size
    };
  } catch (error) {
    console.warn('使用示例厂商订单数据', error);
    return {
      content: sampleVendorOrders.map((order) => ({
        id: order.id,
        productName: order.productName,
        vendorName: '我方',
        planName: order.mode,
        mode: order.mode,
        status: order.status,
        startDate: order.shipBy,
        endDate: order.shipBy,
        nextAction: order.nextStep,
        amountDue: order.value,
        timeline: []
      })),
      total: sampleVendorOrders.length,
      page: 1,
      size: sampleVendorOrders.length
    };
  }
}
