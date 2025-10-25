import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { message } from 'ant-design-vue';
import type { DashboardMetrics, OrderStatusBreakdown } from '../utils/analyticsApi';
import { fetchDashboardMetrics } from '../utils/analyticsApi';

export const useAnalyticsStore = defineStore('analytics', () => {
  const loading = ref(false);
  const metrics = ref<DashboardMetrics | null>(null);
  const error = ref<string | null>(null);

  const statusEntries = computed(() => {
    if (!metrics.value) {
      return [] as Array<{ status: string; count: number }>;
    }
    const breakdown: OrderStatusBreakdown = metrics.value.ordersByStatus ?? {};
    return Object.entries(breakdown)
      .map(([status, count]) => ({ status, count }))
      .sort((a, b) => b.count - a.count);
  });

  const loadDashboard = async () => {
    if (loading.value) {
      return;
    }
    loading.value = true;
    error.value = null;
    try {
      metrics.value = await fetchDashboardMetrics();
    } catch (err) {
      const messageText = err instanceof Error ? err.message : '加载运营指标失败';
      error.value = messageText;
      message.error(messageText);
    } finally {
      loading.value = false;
    }
  };

  return {
    loading,
    metrics,
    error,
    statusEntries,
    loadDashboard
  };
});
