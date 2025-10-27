<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>运营指标</h2>
        <p class="page-header__meta">实时查看 GMV、活跃订单等核心指标，辅助库存与策略决策。</p>
      </div>
      <a-button type="default" @click="loadMetrics" :loading="loading">刷新</a-button>
    </div>

    <a-row :gutter="16">
      <a-col :xs="24" :md="12" :lg="6">
        <a-card>
          <a-statistic title="总订单" :value="metrics?.totalOrders ?? 0" />
        </a-card>
      </a-col>
      <a-col :xs="24" :md="12" :lg="6">
        <a-card>
          <a-statistic title="活跃订单" :value="metrics?.activeOrders ?? 0" />
        </a-card>
      </a-col>
      <a-col :xs="24" :md="12" :lg="6">
        <a-card>
          <a-statistic title="在租中" :value="metrics?.inLeaseCount ?? 0" />
        </a-card>
      </a-col>
      <a-col :xs="24" :md="12" :lg="6">
        <a-card>
          <a-statistic title="GMV (¥)" :value="formatCurrency(metrics?.totalGmv ?? 0)" />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" class="mt-16">
      <a-col :xs="24" :lg="12">
        <a-card title="状态分布">
          <template v-if="statusEntries.length">
            <a-timeline>
              <a-timeline-item v-for="item in statusEntries" :key="item.status">
                <strong>{{ item.status }}</strong>
                <span> · {{ item.count }} 单</span>
              </a-timeline-item>
            </a-timeline>
          </template>
          <a-empty v-else description="暂无数据" />
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12">
        <a-card title="履约表现">
          <a-progress
            :percent="completionPercent"
            :format="() => `完成率 ${completionPercent}%`"
          />
          <p class="insight" v-if="metrics">近期待退租订单：{{ metrics.pendingReturns }} 单</p>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../../stores/auth';
import { fetchVendorMetrics, type VendorMetrics } from '../../services/analyticsService';

const auth = useAuthStore();
const loading = ref(false);
const metrics = ref<VendorMetrics | null>(null);

const formatCurrency = (value: number) =>
  value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const statusEntries = computed(() => {
  if (!metrics.value?.ordersByStatus) {
    return [] as Array<{ status: string; count: number }>;
  }
  return Object.entries(metrics.value.ordersByStatus)
    .map(([status, count]) => ({ status, count }))
    .sort((a, b) => b.count - a.count);
});

const completionPercent = computed(() => {
  if (!metrics.value) {
    return 0;
  }
  const completed = metrics.value.ordersByStatus?.COMPLETED ?? 0;
  const total = metrics.value.totalOrders || 1;
  return Number(((completed / total) * 100).toFixed(2));
});

const loadMetrics = async () => {
  if (!auth.user?.id) {
    message.error('未获取到厂商账号');
    return;
  }
  loading.value = true;
  try {
    metrics.value = await fetchVendorMetrics(auth.user.id);
  } catch (error) {
    console.error('加载厂商指标失败', error);
    message.error('加载指标失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

loadMetrics();
</script>

<style scoped>
.mt-16 {
  margin-top: 16px;
}

.insight {
  margin-top: 12px;
  color: #64748b;
}
</style>
