<template>
  <div v-if="vendorReady" class="page-container">
    <div class="page-header">
      <div>
        <h2>运营指标</h2>
        <p class="page-header__meta">实时查看 GMV、活跃订单等核心指标，辅助库存与策略决策。</p>
      </div>
      <a-button type="default" @click="loadMetrics(true)" :loading="loading">刷新</a-button>
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

  <div v-else class="page-container">
    <a-result status="warning" title="尚未获取厂商身份">
      <template #subTitle>
        请先重新同步账户或退出后重新登录，以查看厂商指标。
      </template>
      <template #extra>
        <a-space>
          <a-button type="primary" :loading="syncingVendor" @click="refreshAccount">重新同步</a-button>
        </a-space>
      </template>
    </a-result>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import { useVendorContext } from '../../composables/useVendorContext';
import { fetchVendorMetrics, type VendorMetrics } from '../../services/analyticsService';

const loading = ref(false);
const metrics = ref<VendorMetrics | null>(null);
const {
  vendorId: currentVendorId,
  vendorReady,
  refreshVendorContext,
  syncingVendor
} = useVendorContext();

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

const loadMetrics = async (notify = false) => {
  const vendorId = currentVendorId.value;
  if (!vendorId) {
    metrics.value = null;
    if (notify) {
      message.warning('缺少厂商身份，请重新登录后重试');
    }
    return;
  }
  loading.value = true;
  try {
    metrics.value = await fetchVendorMetrics(vendorId);
  } catch (error) {
    console.error('加载厂商指标失败', error);
    message.error('加载指标失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const refreshAccount = async () => {
  await refreshVendorContext();
  if (currentVendorId.value) {
    await loadMetrics();
  }
};

watch(
  vendorReady,
  (ready) => {
    if (ready) {
      loadMetrics();
    } else {
      metrics.value = null;
    }
  },
  { immediate: true }
);
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
