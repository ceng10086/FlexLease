import http from '@/api/http';
import type {
  ApiResponseEnvelope,
  PagedResult,
  VendorApplicationSummary,
  VendorMetric,
  VendorOrderSummary,
  VendorProductSummary
} from '@/types';
import {
  sampleVendorApplications,
  sampleVendorMetrics,
  sampleVendorOrders,
  sampleVendorProducts
} from '@/utils/sampleData';

interface VendorProductApiModel {
  id: string;
  name: string;
  status: string;
  rentalModes: string[];
  totalInventory: number;
  leased: number;
  pendingOrders: number;
  updatedAt: string;
}

interface VendorOrderApiModel {
  id: string;
  customerName: string;
  productName: string;
  status: string;
  shipBy: string;
  nextStep: string;
  rentalMode: string;
  orderValue: number;
}

interface VendorMetricApiModel {
  label: string;
  value: string;
  trend: number;
}

interface VendorApplicationApiModel {
  id: string;
  companyName: string;
  contact: string;
  status: string;
  submittedAt: string;
  remark?: string;
}

export async function fetchVendorProducts(vendorId: string): Promise<VendorProductSummary[]> {
  try {
    const { data } = await http.get<ApiResponseEnvelope<PagedResult<VendorProductApiModel>>>(
      `/api/v1/vendors/${vendorId}/products`
    );
    if (!data.success) {
      throw new Error(data.message ?? '无法获取商品列表');
    }
    return data.data.content.map((item) => ({
      id: item.id,
      name: item.name,
      status: item.status,
      rentalModes: item.rentalModes,
      totalInventory: item.totalInventory,
      leased: item.leased,
      pendingOrders: item.pendingOrders,
      lastUpdated: item.updatedAt
    }));
  } catch (error) {
    console.warn('使用示例厂商商品数据', error);
    return sampleVendorProducts;
  }
}

export async function fetchVendorOrders(vendorId: string): Promise<VendorOrderSummary[]> {
  try {
    const { data } = await http.get<ApiResponseEnvelope<PagedResult<VendorOrderApiModel>>>(
      '/api/v1/orders',
      {
        params: { vendorId }
      }
    );
    if (!data.success) {
      throw new Error(data.message ?? '无法获取订单');
    }
    return data.data.content.map((item) => ({
      id: item.id,
      customerName: item.customerName,
      productName: item.productName,
      status: item.status,
      shipBy: item.shipBy,
      nextStep: item.nextStep,
      mode: item.rentalMode,
      value: item.orderValue
    }));
  } catch (error) {
    console.warn('使用示例厂商订单数据', error);
    return sampleVendorOrders;
  }
}

export async function fetchVendorMetrics(vendorId: string): Promise<VendorMetric[]> {
  try {
    const { data } = await http.get<ApiResponseEnvelope<VendorMetricApiModel[]>>(
      `/api/v1/analytics/vendor/${vendorId}`
    );
    if (!data.success) {
      throw new Error(data.message ?? '无法获取运营指标');
    }
    return data.data.map((item) => ({ label: item.label, value: item.value, trend: item.trend }));
  } catch (error) {
    console.warn('使用示例厂商指标数据', error);
    return sampleVendorMetrics;
  }
}

export async function fetchVendorApplications(status?: string): Promise<VendorApplicationSummary[]> {
  try {
    const { data } = await http.get<ApiResponseEnvelope<VendorApplicationApiModel[]>>(
      '/api/v1/vendors/applications',
      {
        params: { status }
      }
    );
    if (!data.success) {
      throw new Error(data.message ?? '无法获取入驻申请');
    }
    return data.data.map((item) => ({
      id: item.id,
      companyName: item.companyName,
      contact: item.contact,
      status: item.status,
      submittedAt: item.submittedAt,
      remark: item.remark
    }));
  } catch (error) {
    console.warn('使用示例入驻申请数据', error);
    return sampleVendorApplications;
  }
}
