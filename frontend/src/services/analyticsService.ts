import type { CreditTier } from '../types/credit';
/**
 * 运营指标 API（order-service /analytics/**）：
 * - 平台驾驶舱与厂商洞察所需的聚合统计
 */
import http from './http';

export type OrderStatusBreakdown = Record<string, number>;

export type TrendPoint = {
  date: string;
  orders: number;
  gmv: number;
};

export type PlanBreakdownEntry = {
  planType: string;
  orders: number;
  gmv: number;
};

export type CreditMetrics = {
  averageScore: number;
  tierDistribution: Record<CreditTier, number>;
};

export type DisputeMetrics = {
  openCount: number;
  pendingAdminCount: number;
  resolvedCount: number;
  averageResolutionHours: number;
};

export type SurveyMetrics = {
  pendingCount: number;
  openCount: number;
  completedCount: number;
  averageRating: number;
};

export type DashboardMetrics = {
  totalOrders: number;
  activeOrders: number;
  totalGmv: number;
  inLeaseCount: number;
  pendingReturns: number;
  ordersByStatus: OrderStatusBreakdown;
  recentTrend: TrendPoint[];
  planBreakdown: PlanBreakdownEntry[];
  creditMetrics: CreditMetrics;
  disputeMetrics: DisputeMetrics;
  surveyMetrics: SurveyMetrics;
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
