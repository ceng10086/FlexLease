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

    <a-row v-if="metrics" :gutter="16" class="mt-16">
      <a-col :xs="24" :lg="16">
        <a-card title="7 日 GMV 与订单趋势" :loading="loading">
          <TrendChart :data="trendPoints" />
          <div v-if="trendDelta" class="trend-summary">
            <div class="trend-summary__item">
              <span class="trend-summary__label">GMV 环比</span>
              <span :class="['trend-summary__value', deltaClass(trendDelta.gmv.direction)]">
                ¥{{ formatCurrency(trendDelta.gmv.diff) }}
                <small>{{ formatPercent(trendDelta.gmv.rate) }}</small>
              </span>
            </div>
            <div class="trend-summary__item">
              <span class="trend-summary__label">订单环比</span>
              <span :class="['trend-summary__value', deltaClass(trendDelta.orders.direction)]">
                {{ formatSigned(trendDelta.orders.diff) }} 单
                <small>{{ formatPercent(trendDelta.orders.rate) }}</small>
              </span>
            </div>
          </div>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="8">
        <a-card title="租赁模式构成" :loading="loading">
          <PlanBreakdownCard :data="planBreakdown" />
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
        请先退出当前账号并重新登录后，再访问厂商指标。
      </template>
    </a-result>
  </div>
</template>

<script lang="ts" setup>
import { computed, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import { useVendorContext } from '../../composables/useVendorContext';
import { fetchVendorMetrics, type VendorMetrics } from '../../services/analyticsService';
import TrendChart from '../../components/analytics/TrendChart.vue';
import PlanBreakdownCard from '../../components/analytics/PlanBreakdownCard.vue';

const loading = ref(false);
const metrics = ref<VendorMetrics | null>(null);
const {
  vendorId: currentVendorId,
  vendorReady
} = useVendorContext();

const formatCurrency = (value: number) =>
  value.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

const planBreakdown = computed(() => metrics.value?.planBreakdown ?? []);
const trendPoints = computed(() => metrics.value?.recentTrend ?? []);
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

type Direction = 'up' | 'down' | 'flat';

type MetricDelta = {
  diff: number;
  rate: number | null;
  direction: Direction;
};

type TrendDelta = {
  gmv: MetricDelta;
  orders: MetricDelta;
};

const deltaClass = (direction: Direction) => {
  if (direction === 'up') {
    return 'trend-summary__value--up';
  }
  if (direction === 'down') {
    return 'trend-summary__value--down';
  }
  return '';
};

const formatPercent = (value: number | null) => {
  if (value === null || Number.isNaN(value)) {
    return '—';
  }
  const prefix = value > 0 ? '+' : '';
  return `${prefix}${value.toFixed(1)}%`;
};

const formatSigned = (value: number) => {
  if (value > 0) {
    return `+${value}`;
  }
  return value.toString();
};

const computeMetricDelta = (latest: number, previous: number): MetricDelta => {
  const diff = Number((latest - previous).toFixed(2));
  const rate = previous === 0 ? null : Number(((diff / previous) * 100).toFixed(1));
  let direction: Direction = 'flat';
  if (diff > 0) {
    direction = 'up';
  } else if (diff < 0) {
    direction = 'down';
  }
  return { diff, rate, direction };
};

const trendDelta = computed<TrendDelta | null>(() => {
  if (!trendPoints.value.length || trendPoints.value.length < 2) {
    return null;
  }
  const latest = trendPoints.value[trendPoints.value.length - 1];
  const previous = trendPoints.value[trendPoints.value.length - 2];
  return {
    gmv: computeMetricDelta(latest.gmv ?? 0, previous.gmv ?? 0),
    orders: computeMetricDelta(latest.orders ?? 0, previous.orders ?? 0)
  };
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

.trend-summary {
  margin-top: 16px;
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
}

.trend-summary__item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.trend-summary__label {
  color: #94a3b8;
  font-size: 12px;
}

.trend-summary__value {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.trend-summary__value--up {
  color: #16a34a;
}

.trend-summary__value--down {
  color: #dc2626;
}

.trend-summary__value small {
  margin-left: 8px;
  font-weight: 400;
  color: #94a3b8;
}
</style>
