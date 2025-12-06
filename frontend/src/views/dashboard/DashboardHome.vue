<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="FlexLease 驾驶舱"
        eyebrow="Dashboard"
        description="统一查看指标、信用与沟通提醒，快速跳转到不同角色的关键入口。"
      >
        <template #actions>
          <a-tag v-for="role in auth.user?.roles ?? []" :key="role" color="processing">
            {{ role }}
          </a-tag>
        </template>
      </PageHeader>
    </template>

    <PageSection title="快捷入口" description="围绕角色装配下单、履约、审核等卡片式入口。">
      <QuickEntryGrid :entries="quickEntries" @select="handleEntrySelect" />
    </PageSection>

    <PageSection
      v-if="canViewPlatform"
      title="平台指标"
      description="管理员可实时掌握 GMV、订单状态与信用态势。"
    >
      <a-skeleton :loading="platformLoading" active>
        <template #default>
          <div v-if="platformMetrics" class="stat-grid">
            <StatWidget
              v-for="stat in platformStats"
              :key="stat.label"
              v-bind="stat"
            >
              <template #icon>
                <component :is="stat.icon" />
              </template>
            </StatWidget>
          </div>
          <div class="dashboard-panels" v-if="platformMetrics">
            <div class="dashboard-panel">
              <h4>7 日趋势</h4>
              <TrendChart :data="platformMetrics.recentTrend" />
            </div>
            <div class="dashboard-panel">
              <h4>租赁模式构成</h4>
              <PlanBreakdownCard :data="platformMetrics.planBreakdown" />
            </div>
          </div>
          <div class="dashboard-panels">
            <div class="dashboard-panel">
              <h4>信用分布</h4>
              <div v-if="creditDistribution.length" class="credit-tags">
                <a-tag
                  v-for="entry in creditDistribution"
                  :key="entry.tier"
                  :color="entry.color"
                >
                  {{ entry.label }} · {{ entry.count }}
                </a-tag>
              </div>
              <DataStateBlock
                v-else
                type="empty"
                title="暂无信用统计"
                description="待有订单后自动累积。"
              />
            </div>
            <div class="dashboard-panel">
              <h4>纠纷态势</h4>
              <div v-if="platformMetrics?.disputeMetrics" class="dispute-grid">
                <div class="dispute-block" v-for="item in disputeStats(platformMetrics.disputeMetrics)" :key="item.label">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
              <DataStateBlock
                v-else
                type="empty"
                title="暂无纠纷数据"
                description="当订单进入纠纷后会在此累计。"
              />
            </div>
          </div>
        </template>
      </a-skeleton>
      <DataStateBlock
        v-if="!platformLoading && !platformMetrics"
        type="empty"
        title="暂未获取到平台指标"
        description="请稍后刷新，或确保账号具备管理员权限。"
      />
    </PageSection>

    <PageSection
      v-if="vendorMetrics"
      title="厂商履约"
      description="结合 SLA 得分、纠纷与结算情况，评估当前厂商健康度。"
    >
      <div class="stat-grid">
        <StatWidget
          v-for="stat in vendorStats"
          :key="stat.label"
          v-bind="stat"
        >
          <template #icon>
            <component :is="stat.icon" />
          </template>
        </StatWidget>
      </div>
      <div class="dashboard-panels">
        <div class="dashboard-panel">
          <h4>我的趋势</h4>
          <TrendChart :data="vendorMetrics.recentTrend" />
        </div>
        <div class="dashboard-panel">
          <h4>订单状态</h4>
          <div class="status-pills">
            <span v-for="entry in statusEntries(vendorMetrics.ordersByStatus)" :key="entry.status">
              {{ entry.label }} · {{ entry.count }}
            </span>
          </div>
        </div>
      </div>
    </PageSection>

    <PageSection title="公告与提醒" description="统一收敛站内公告、信用提示等消息。">
      <div class="dashboard-panels">
        <div class="dashboard-panel">
          <h4>平台公告</h4>
          <AnnouncementRail :items="announcementItems" />
        </div>
        <div class="dashboard-panel">
          <h4>最近站内信</h4>
          <div v-if="recentNotifications.length" class="notification-raised-list">
            <div v-for="item in recentNotifications" :key="item.id" class="notification-pill">
              <span>{{ item.subject || '系统通知' }}</span>
              <small>{{ formatDate(item.createdAt) }}</small>
            </div>
          </div>
          <DataStateBlock
            v-else
            type="empty"
            title="暂无站内提醒"
            description="当订单、纠纷或信用有更新时会在此出现。"
          />
        </div>
      </div>
    </PageSection>
  </PageShell>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import {
  DashboardOutlined,
  AppstoreOutlined,
  ScheduleOutlined,
  MessageOutlined,
  ShopOutlined,
  SendOutlined,
  TransactionOutlined,
  TeamOutlined,
  ProfileOutlined
} from '@ant-design/icons-vue';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import PageSection from '../../components/layout/PageSection.vue';
import StatWidget from '../../components/dashboard/StatWidget.vue';
import QuickEntryGrid, { type QuickEntry } from '../../components/dashboard/QuickEntryGrid.vue';
import AnnouncementRail from '../../components/dashboard/AnnouncementRail.vue';
import TrendChart from '../../components/analytics/TrendChart.vue';
import PlanBreakdownCard from '../../components/analytics/PlanBreakdownCard.vue';
import DataStateBlock from '../../components/feedback/DataStateBlock.vue';
import { useAuthStore } from '../../stores/auth';
import { useQuery } from '../../composables/useQuery';
import {
  fetchDashboardMetrics,
  fetchVendorMetrics,
  type DashboardMetrics,
  type OrderStatusBreakdown
} from '../../services/analyticsService';
import { listNotificationLogs, type NotificationLog } from '../../services/notificationService';
import { creditTierLabel, creditTierColor } from '../../types/credit';

const auth = useAuthStore();
const router = useRouter();
const vendorId = computed(() => auth.vendorId ?? null);

const canViewPlatform = computed(() => auth.hasRole('ADMIN'));
const hasVendor = computed(() => Boolean(vendorId.value));

const {
  data: platformData,
  loading: platformLoading
} = useQuery<DashboardMetrics>(
  'dashboard-metrics',
  () => fetchDashboardMetrics(),
  {
    enabled: canViewPlatform,
    immediate: false
  }
);

const { data: vendorData } = useQuery<DashboardMetrics>('vendor-metrics', () => {
  if (!vendorId.value) {
    throw new Error('缺少厂商身份');
  }
  return fetchVendorMetrics(vendorId.value);
}, {
  enabled: hasVendor,
  immediate: false
});

const { data: announcementData } = useQuery<NotificationLog[]>(
  'dashboard-announcements',
  () => listNotificationLogs({ contextType: 'ANNOUNCEMENT' })
);

const { data: notificationData } = useQuery<NotificationLog[]>(
  'dashboard-notifications',
  () => listNotificationLogs({}),
  { cache: false }
);

const platformMetrics = computed(() => platformData.value);
const vendorMetrics = computed(() => vendorData.value);

const platformStats = computed(() => {
  if (!platformMetrics.value) {
    return [];
  }
  return [
    {
      label: '总订单',
      value: platformMetrics.value.totalOrders.toLocaleString(),
      description: '累计创建的租赁订单',
      icon: DashboardOutlined
    },
    {
      label: '活跃订单',
      value: platformMetrics.value.activeOrders.toLocaleString(),
      description: '待发货/履约/退租',
      icon: ScheduleOutlined
    },
    {
      label: 'GMV (¥)',
      value: platformMetrics.value.totalGmv.toLocaleString('zh-CN', { minimumFractionDigits: 2 }),
      description: '含押金/租金/买断',
      icon: TransactionOutlined
    },
    {
      label: '租赁中',
      value: platformMetrics.value.inLeaseCount.toString(),
      description: '当前设备正在租赁',
      icon: ShopOutlined
    },
    {
      label: '待退租',
      value: platformMetrics.value.pendingReturns.toString(),
      description: '需要催促归还',
      icon: SendOutlined
    }
  ];
});

const vendorStats = computed(() => {
  if (!vendorMetrics.value) {
    return [];
  }
  return [
    {
      label: '我的 GMV (¥)',
      value: vendorMetrics.value.totalGmv.toLocaleString('zh-CN', { minimumFractionDigits: 2 }),
      description: '含押金/租金/买断',
      icon: TransactionOutlined
    },
    {
      label: '活跃订单',
      value: vendorMetrics.value.activeOrders.toString(),
      description: '待发货/履约/退租',
      icon: ScheduleOutlined
    },
    {
      label: '纠纷（协商中）',
      value: vendorMetrics.value.disputeMetrics?.openCount?.toString() ?? '0',
      description: '及时跟进沟通',
      icon: MessageOutlined
    }
  ];
});

const creditDistribution = computed(() => {
  if (!platformMetrics.value?.creditMetrics) {
    return [];
  }
  const entries = platformMetrics.value.creditMetrics.tierDistribution ?? {};
  return Object.entries(entries).map(([tier, count]) => ({
    tier,
    count,
    label: creditTierLabel(tier as any),
    color: creditTierColor(tier as any)
  }));
});

const announcementItems = computed(() => (announcementData.value ?? []).slice(0, 5));
const recentNotifications = computed(() => (notificationData.value ?? []).slice(0, 5));

const formatDate = (value?: string | null) => {
  if (!value) {
    return '--';
  }
  return new Date(value).toLocaleString();
};

const quickEntries = computed<QuickEntry[]>(() => {
  const entries: QuickEntry[] = [];
  if (auth.hasRole('USER')) {
    entries.push(
      {
        key: 'catalog',
        title: '逛逛精选',
        description: '两列瀑布流，快速选品并查看押金策略。',
        ctaLabel: '打开目录',
        icon: AppstoreOutlined
      },
      {
        key: 'orders',
        title: '订单与聊天',
        description: '查看卡片式订单墙，直接切换聊天/凭证/时间线。',
        ctaLabel: '进入订单',
        icon: ScheduleOutlined
      },
      {
        key: 'notifications',
        title: '通知中心',
        description: '按渠道统计信用提醒、纠纷倒计时。',
        ctaLabel: '查看通知',
        icon: MessageOutlined
      }
    );
  }
  if (auth.hasRole('VENDOR')) {
    entries.push(
      {
        key: 'vendor-products',
        title: '商品工作台',
        description: '集中维护方案、SKU、库存与咨询回复。',
        ctaLabel: '管理商品',
        icon: ShopOutlined
      },
      {
        key: 'vendor-fulfillment',
        title: '履约中台',
        description: '统一操作发货、续租/退租审批与凭证上传。',
        ctaLabel: '进入履约',
        icon: SendOutlined
      },
      {
        key: 'vendor-settlement',
        title: '结算对账',
        description: '查看押金/租金拆分，关注抽成与净入账。',
        ctaLabel: '查看结算',
        icon: TransactionOutlined
      }
    );
  }
  if (auth.hasRole('ADMIN')) {
    entries.push(
      {
        key: 'admin-review',
        title: '审核大厅',
        description: '管理厂商/商品审核，减少来回跳转。',
        ctaLabel: '打开审核',
        icon: TeamOutlined
      },
      {
        key: 'admin-orders',
        title: '订单监控',
        description: '分段 Tab + 抽屉，快速定位人工复核。',
        ctaLabel: '监控订单',
        icon: ProfileOutlined
      }
    );
  }
  return entries;
});

const handleEntrySelect = (entry: QuickEntry) => {
  switch (entry.key) {
    case 'catalog':
      router.push({ name: 'catalog-feed' });
      break;
    case 'orders':
      router.push({ name: 'orders' });
      break;
    case 'notifications':
      router.push({ name: 'notifications' });
      break;
    case 'vendor-products':
      router.push({ name: 'vendor-workbench-products' });
      break;
    case 'vendor-fulfillment':
      router.push({ name: 'vendor-workbench-fulfillment' });
      break;
    case 'vendor-settlement':
      router.push({ name: 'vendor-workbench-settlement' });
      break;
    case 'admin-review':
      router.push({ name: 'admin-review' });
      break;
    case 'admin-orders':
      router.push({ name: 'admin-orders' });
      break;
    default:
      router.push({ path: '/app/dashboard' });
  }
};

const statusEntries = (breakdown: OrderStatusBreakdown) =>
  Object.entries(breakdown ?? {}).map(([status, count]) => ({
    status,
    label: status,
    count
  }));

const disputeStats = (metrics: DashboardMetrics['disputeMetrics']) => [
  { label: '协商中', value: metrics?.openCount ?? 0 },
  { label: '待平台', value: metrics?.pendingAdminCount ?? 0 },
  { label: '已结案', value: metrics?.resolvedCount ?? 0 }
];
</script>

<style scoped>
.dashboard-panels {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--space-4);
  margin-top: var(--space-4);
}

.dashboard-panel {
  padding: var(--space-4);
  border-radius: var(--radius-card);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
}

.dashboard-panel h4 {
  margin-top: 0;
  margin-bottom: var(--space-3);
}

.credit-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.dispute-grid {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.dispute-block {
  flex: 1;
  min-width: 120px;
  background: rgba(148, 163, 184, 0.12);
  border-radius: 12px;
  padding: var(--space-3);
}

.dispute-block span {
  display: block;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.dispute-block strong {
  font-size: 20px;
  font-weight: 600;
}

.notification-raised-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.notification-pill {
  padding: var(--space-3);
  border-radius: 12px;
  background: rgba(37, 99, 235, 0.06);
  border: 1px solid rgba(37, 99, 235, 0.12);
  display: flex;
  justify-content: space-between;
}

.status-pills {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  color: var(--color-text-secondary);
}
</style>
