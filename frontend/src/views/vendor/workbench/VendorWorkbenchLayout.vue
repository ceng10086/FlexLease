<template>
  <PageShell>
    <template #header>
      <template v-if="vendorReady">
        <PageHeader
          title="厂商一体化工作台"
          description="统一入口串联商品、履约、指标与结算，聚焦当周 SLA 与抽成动态。"
          eyebrow="Vendor"
        >
          <template #actions>
            <a-button :loading="vendorLoadingState" @click="refreshVendorProfile">刷新资料</a-button>
            <a-button type="primary" ghost :loading="syncing" @click="syncAccount">同步账号</a-button>
          </template>
        </PageHeader>
        <div class="workbench-meta" v-if="vendorProfile">
          <div class="meta-card">
            <p>主体</p>
            <strong>{{ vendorProfile.companyName }}</strong>
            <span>状态：{{ vendorProfile.status }}</span>
          </div>
          <div class="meta-card">
            <p>抽成 (当前)</p>
            <strong>
              {{ formatPercent(vendorProfile.commissionProfile?.commissionRate) }}
            </strong>
            <span>行业：{{ vendorProfile.commissionProfile?.industryCategory ?? '—' }}</span>
          </div>
          <div class="meta-card">
            <p>信用档位</p>
            <strong>{{ vendorProfile.commissionProfile?.creditTier ?? '—' }}</strong>
            <span>SLA：{{ vendorProfile.commissionProfile?.slaScore ?? '--' }}</span>
          </div>
        </div>
        <a-skeleton v-else active :paragraph="{ rows: 2 }" />
        <a-tabs :active-key="activeTab" class="workbench-tabs" @change="handleTabChange">
          <a-tab-pane v-for="tab in tabs" :key="tab.key" :tab="tab.label" />
        </a-tabs>
      </template>
      <DataStateBlock
        v-else
        title="尚未绑定厂商身份"
        description="退出重新登录或点击下方按钮同步，会刷新登录态以获取 vendorId。"
      >
        <a-button type="primary" :loading="syncing" @click="syncAccount">刷新账号信息</a-button>
      </DataStateBlock>
    </template>
    <router-view v-if="vendorReady" />
  </PageShell>
</template>

<script lang="ts" setup>
// 厂商工作台布局：统一提供厂商档案与刷新能力，并承载子看板路由。
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import PageShell from '../../../components/layout/PageShell.vue';
import PageHeader from '../../../components/layout/PageHeader.vue';
import DataStateBlock from '../../../components/feedback/DataStateBlock.vue';
import { useVendorContext } from '../../../composables/useVendorContext';
import { provideVendorWorkbench } from '../../../composables/useVendorWorkbench';
import { fetchVendor, type Vendor } from '../../../services/vendorService';
import { friendlyErrorMessage } from '../../../utils/error';

const router = useRouter();
const route = useRoute();
const { vendorId, vendorReady, refreshVendorContext } = useVendorContext();

const vendorProfile = ref<Vendor | null>(null);
const vendorLoading = ref(false);
const syncing = ref(false);

const tabs = [
  { key: 'vendor-workbench-products', label: '商品看板' },
  { key: 'vendor-workbench-fulfillment', label: '履约中台' },
  { key: 'vendor-workbench-insights', label: '指标洞察' },
  { key: 'vendor-workbench-settlement', label: '结算中心' }
];

const activeTab = computed(() => (route.name as string) ?? tabs[0].key);

const loadVendorProfile = async () => {
  if (!vendorId.value) {
    vendorProfile.value = null;
    return;
  }
  vendorLoading.value = true;
  try {
    vendorProfile.value = await fetchVendor(vendorId.value);
  } catch (error) {
    vendorProfile.value = null;
    message.error(friendlyErrorMessage(error, '加载厂商资料失败'));
  } finally {
    vendorLoading.value = false;
  }
};

watch(
  () => vendorId.value,
  () => {
    if (vendorId.value) {
      loadVendorProfile();
    } else {
      vendorProfile.value = null;
    }
  },
  { immediate: true }
);

const handleTabChange = (key: string) => {
  if (key === activeTab.value) {
    return;
  }
  router.push({ name: key });
};

const refreshVendorProfile = () => loadVendorProfile();

const syncAccount = async () => {
  if (syncing.value) {
    return;
  }
  syncing.value = true;
  try {
    await refreshVendorContext({ notify: true });
  } finally {
    syncing.value = false;
  }
};

const vendorLoadingState = computed(() => vendorLoading.value);

const formatPercent = (value?: number | null) => {
  if (typeof value !== 'number') {
    return '--';
  }
  return `${(value * 100).toFixed(1)}%`;
};

provideVendorWorkbench({
  vendor: vendorProfile,
  vendorLoading,
  refreshVendorProfile: loadVendorProfile
});
</script>

<style scoped>
.workbench-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-4);
  margin: var(--space-4) 0;
}

.meta-card {
  padding: var(--space-4);
  border-radius: var(--radius-card);
  border: 1px solid rgba(148, 163, 184, 0.35);
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.6), #fff);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.meta-card p {
  margin: 0;
  font-size: var(--font-size-caption);
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
}

.meta-card strong {
  font-size: var(--font-size-title-lg);
  line-height: 1.4;
}

.meta-card span {
  color: var(--color-text-secondary);
}

.workbench-tabs {
  background: var(--color-surface);
  padding: 0 var(--space-4);
  border-radius: var(--radius-card);
  border: 1px solid rgba(148, 163, 184, 0.3);
  overflow-x: auto;
}

.workbench-tabs :deep(.ant-tabs-nav) {
  min-height: 46px;
}

.workbench-tabs :deep(.ant-tabs-tab) {
  padding: 12px 16px;
  line-height: 1.5;
}

.workbench-tabs :deep(.ant-tabs-tab-btn) {
  line-height: 1.5;
  overflow: visible;
}

@media (max-width: 640px) {
  .workbench-meta {
    grid-template-columns: 1fr;
  }
}
</style>
