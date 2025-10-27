import http from './http';

export type OrderStatusBreakdown = Record<string, number>;

export type DashboardMetrics = {
  totalOrders: number;
  activeOrders: number;
  totalGmv: number;
  inLeaseCount: number;
  pendingReturns: number;
  ordersByStatus: OrderStatusBreakdown;
};

export type VendorMetrics = DashboardMetrics & {
  vendorId: string;
};

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export const fetchDashboardMetrics = async (): Promise<DashboardMetrics> => {
  const response = await http.get<ApiResponse<DashboardMetrics>>('/analytics/dashboard');
  if (!response.data.data) {
    throw new Error('未能获取平台指标');
  }
  return response.data.data;
};

export const fetchVendorMetrics = async (vendorId: string): Promise<VendorMetrics> => {
  const response = await http.get<ApiResponse<VendorMetrics>>(`/analytics/vendor/${vendorId}`);
  if (!response.data.data) {
    throw new Error('未能获取厂商指标');
  }
  return response.data.data;
};
