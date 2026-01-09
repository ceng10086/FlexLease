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
      v-if="showVendorSection"
      title="厂商履约"
      :description="vendorSectionDescription"
    >
      <template #actions v-if="vendorSelectionEnabled">
        <div class="vendor-switcher">
          <span>查看厂商</span>
          <a-select
            :value="selectedVendorId ?? undefined"
            :options="vendorSelectOptions"
            :loading="vendorDirectoryLoading"
            show-search
            allow-clear
            option-filter-prop="label"
            placeholder="选择厂商"
            style="min-width: 240px"
            @change="handleVendorChange"
            :filter-option="filterVendorOption"
          />
        </div>
      </template>
      <a-skeleton :loading="vendorLoading" active>
        <template #default>
          <template v-if="vendorMetrics">
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
          </template>
        </template>
      </a-skeleton>
      <DataStateBlock
        v-if="!vendorLoading && !vendorMetrics"
        type="empty"
        :title="vendorSelectionEnabled ? '请选择厂商' : '暂无厂商数据'"
        :description="vendorSelectionEnabled ? '选择厂商后即可查看 GMV、纠纷与状态分布。' : '当前账号未绑定厂商。'"
      />
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
import { computed, ref, watch } from 'vue';
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
import { listVendors, type Vendor, type PagedResponse } from '../../services/vendorService';
import { listNotificationLogs, type NotificationLog } from '../../services/notificationService';
import { creditTierLabel, creditTierColor } from '../../types/credit';

// 驾驶舱：按角色展示快捷入口、平台指标（管理员）与厂商指标（厂商/管理员）。
const auth = useAuthStore();
const router = useRouter();
const vendorId = computed(() => auth.vendorId ?? null);
const selectedVendorId = ref<string | null>(vendorId.value ?? null);

const canViewPlatform = computed(() => auth.hasRole('ADMIN'));
const showVendorSection = computed(() => auth.hasRole('VENDOR') || auth.hasRole('ADMIN'));
const vendorSelectionEnabled = computed(() => auth.hasRole('ADMIN'));

const {
  data: platformData,
  loading: platformLoading
} = useQuery<DashboardMetrics>('dashboard-metrics', () => fetchDashboardMetrics(), {
  enabled: canViewPlatform
});

watch(
  vendorId,
  (next) => {
    if (!auth.hasRole('ADMIN')) {
      selectedVendorId.value = next ?? null;
    }
  },
  { immediate: true }
);

const vendorMetricsKey = computed(
  () => `vendor-metrics-${selectedVendorId.value ?? 'none'}`
);

const { data: vendorData, loading: vendorLoading } = useQuery<DashboardMetrics>(
  () => vendorMetricsKey.value,
  () => fetchVendorMetrics(selectedVendorId.value as string),
  {
    enabled: computed(() => Boolean(selectedVendorId.value)),
    cache: false
  }
);

const {
  data: vendorDirectory,
  loading: vendorDirectoryLoading
} = useQuery<PagedResponse<Vendor>>(
  'dashboard-vendor-directory',
  () => listVendors({ page: 1, size: 40 }),
  { enabled: vendorSelectionEnabled }
);

const vendorList = computed(() => vendorDirectory.value?.content ?? []);
const vendorSelectOptions = computed(() =>
  vendorList.value.map((vendor) => ({
    label: vendor.companyName,
    value: vendor.id
  }))
);
const selectedVendor = computed(() =>
  vendorList.value.find((item) => item.id === selectedVendorId.value) ?? null
);
const vendorSectionDescription = computed(() => {
  if (selectedVendor.value) {
    return `当前查看：${selectedVendor.value.companyName} 的 GMV、纠纷与订单状态。`;
  }
  if (vendorSelectionEnabled.value) {
    return '选择任一厂商即可实时查看 SLA、纠纷与在途订单。';
  }
  return '结合 SLA 得分、纠纷与结算情况，评估当前厂商健康度。';
});

const handleVendorChange = (value: string | undefined) => {
  selectedVendorId.value = value ?? null;
};

const filterVendorOption = (input: string, option?: { label?: string }) => {
  if (!option?.label) {
    return false;
  }
  return option.label.toLowerCase().includes(input.toLowerCase());
};

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

.vendor-switcher {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.vendor-switcher span {
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>
