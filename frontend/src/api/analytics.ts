import http from '@/api/http';
import type { ApiResponseEnvelope, DashboardMetric } from '@/types';
import { sampleDashboardMetrics } from '@/utils/sampleData';

interface DashboardMetricApiModel {
  title: string;
  value: string;
  trend: number;
  trendLabel?: string;
}

export async function fetchDashboardMetrics(): Promise<DashboardMetric[]> {
  try {
    const { data } = await http.get<ApiResponseEnvelope<DashboardMetricApiModel[]>>('/api/v1/analytics/dashboard');
    if (!data.success) {
      throw new Error(data.message ?? '无法获取仪表盘数据');
    }
    return data.data.map((item) => ({
      title: item.title,
      value: item.value,
      trend: item.trend,
      trendLabel: item.trendLabel
    }));
  } catch (error) {
    console.warn('使用示例仪表盘数据', error);
    return sampleDashboardMetrics;
  }
}
