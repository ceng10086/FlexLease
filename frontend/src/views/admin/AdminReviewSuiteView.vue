<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="审核大厅"
        description="厂商与商品审核集中在同一视图，左侧决策主体，右侧审核上架。"
        eyebrow="Admin"
      />
    </template>
    <div class="split-grid">
      <PageSection title="厂商入驻">
        <div class="section-toolbar">
          <a-segmented v-model:value="vendorFilter" :options="vendorFilterOptions" @change="loadVendorApps" />
        </div>
        <div v-if="vendorLoading">
          <a-skeleton active :paragraph="{ rows: 3 }" />
        </div>
        <div v-else-if="vendorApps.length" class="card-column">
          <div v-for="app in vendorApps" :key="app.id" class="review-card" @click="openVendorDrawer(app)">
            <h3>{{ app.companyName }}</h3>
            <p>{{ app.contactName }} · {{ app.contactPhone }}</p>
            <p>统一社会信用代码：{{ app.unifiedSocialCode }}</p>
            <a-tag :color="app.status === 'APPROVED' ? 'green' : app.status === 'REJECTED' ? 'red' : 'processing'">{{ statusMap[app.status] }}</a-tag>
          </div>
        </div>
        <DataStateBlock v-else title="暂无申请" description="当前没有需要处理的入驻申请。" />
      </PageSection>

      <PageSection title="商品审核">
        <div class="section-toolbar">
          <a-segmented v-model:value="productFilter" :options="productFilterOptions" @change="loadProducts" />
        </div>
        <div v-if="productLoading">
          <a-skeleton active :paragraph="{ rows: 3 }" />
        </div>
        <div v-else-if="products.length" class="card-column">
          <div v-for="product in products" :key="product.id" class="review-card" @click="openProductDrawer(product)">
            <h3>{{ product.name }}</h3>
            <p>厂商：{{ product.vendorId.slice(0, 8) }}</p>
            <p>分类：{{ product.categoryCode }}</p>
            <a-tag :color="product.status === 'ACTIVE' ? 'green' : product.status === 'PENDING_REVIEW' ? 'processing' : 'orange'">{{ product.status }}</a-tag>
          </div>
        </div>
        <DataStateBlock v-else title="暂无商品待审核" description="待审核商品将自动展示在此列表。" />
      </PageSection>
    </div>
  </PageShell>

  <a-drawer
    v-model:open="vendorDrawer.open"
    title="厂商申请"
    :width="vendorDrawerWidth"
    destroy-on-close
  >
    <template v-if="vendorDrawer.current">
      <p>公司：{{ vendorDrawer.current.companyName }}</p>
      <p>联系人：{{ vendorDrawer.current.contactName }} · {{ vendorDrawer.current.contactPhone }}</p>
      <p>邮箱：{{ vendorDrawer.current.contactEmail || '—' }}</p>
      <p>地址：{{ vendorDrawer.current.province }} {{ vendorDrawer.current.city }} {{ vendorDrawer.current.address }}</p>
      <a-textarea v-model:value="vendorDrawer.remark" :rows="3" placeholder="审核备注" />
      <div class="drawer-footer">
        <a-space>
          <a-button @click="handleVendorDecision(false)">驳回</a-button>
          <a-button type="primary" @click="handleVendorDecision(true)">通过</a-button>
        </a-space>
      </div>
    </template>
  </a-drawer>

  <a-drawer
    v-model:open="productDrawer.open"
    title="商品审核"
    :width="productDrawerWidth"
    destroy-on-close
  >
    <template v-if="productDrawer.detail">
      <h3>{{ productDrawer.detail.name }}</h3>
      <p>厂商：{{ productDrawer.detail.vendorId }}</p>
      <p>说明：{{ productDrawer.detail.description || '—' }}</p>
      <div class="plan-list">
        <div v-for="plan in productDrawer.detail.rentalPlans" :key="plan.id" class="plan-row">
          <strong>{{ plan.planType }}</strong>
          <span>{{ plan.termMonths }} 个月</span>
          <span>押金 ¥{{ formatCurrency(plan.depositAmount) }}</span>
          <span>月租 ¥{{ formatCurrency(plan.rentAmountMonthly) }}</span>
        </div>
      </div>
      <a-textarea v-model:value="productDrawer.remark" :rows="3" placeholder="审核备注" />
      <div class="drawer-footer">
        <a-space>
          <a-button @click="handleProductDecision(false)">驳回</a-button>
          <a-button type="primary" @click="handleProductDecision(true)">通过</a-button>
        </a-space>
      </div>
    </template>
    <DataStateBlock v-else title="加载中" />
  </a-drawer>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue';
import { useViewport } from '../../composables/useViewport';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import PageSection from '../../components/layout/PageSection.vue';
import DataStateBlock from '../../components/feedback/DataStateBlock.vue';
import {
  listVendorApplications,
  approveVendorApplication,
  rejectVendorApplication,
  type VendorApplication
} from '../../services/vendorService';
import {
  listAdminProducts,
  fetchVendorProduct,
  approveProduct,
  rejectProduct,
  type ProductSummary,
  type ProductDetail,
  type ProductStatus
} from '../../services/productService';
import { friendlyErrorMessage } from '../../utils/error';
import { formatCurrency } from '../../utils/number';
import { message } from 'ant-design-vue';

const vendorFilter = ref<'SUBMITTED' | 'APPROVED' | 'REJECTED'>('SUBMITTED');
const vendorFilterOptions = [
  { label: '待审核', value: 'SUBMITTED' },
  { label: '已通过', value: 'APPROVED' },
  { label: '被驳回', value: 'REJECTED' }
];
const statusMap = {
  SUBMITTED: '待审核',
  APPROVED: '已通过',
  REJECTED: '已驳回'
};
const vendorApps = ref<VendorApplication[]>([]);
const vendorLoading = ref(false);

const loadVendorApps = async () => {
  vendorLoading.value = true;
  try {
    vendorApps.value = await listVendorApplications(vendorFilter.value);
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载厂商申请失败'));
  } finally {
    vendorLoading.value = false;
  }
};

const vendorDrawer = reactive<{ open: boolean; current: VendorApplication | null; remark: string; approving: boolean }>(
  { open: false, current: null, remark: '', approving: false }
);

const openVendorDrawer = (app: VendorApplication) => {
  vendorDrawer.current = app;
  vendorDrawer.remark = '';
  vendorDrawer.open = true;
};

const handleVendorDecision = async (approve: boolean) => {
  if (!vendorDrawer.current) {
    return;
  }
  vendorDrawer.approving = true;
  try {
    if (approve) {
      await approveVendorApplication(vendorDrawer.current.id, { remark: vendorDrawer.remark });
      message.success('已通过申请');
    } else {
      await rejectVendorApplication(vendorDrawer.current.id, { remark: vendorDrawer.remark });
      message.success('已驳回申请');
    }
    vendorDrawer.open = false;
    loadVendorApps();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '提交失败'));
  } finally {
    vendorDrawer.approving = false;
  }
};

const productFilter = ref<ProductStatus | 'PENDING_REVIEW'>('PENDING_REVIEW');
const productFilterOptions = [
  { label: '待审核', value: 'PENDING_REVIEW' },
  { label: '已上线', value: 'ACTIVE' },
  { label: '已下线', value: 'INACTIVE' }
];
const products = ref<ProductSummary[]>([]);
const productLoading = ref(false);

const loadProducts = async () => {
  productLoading.value = true;
  try {
    products.value = (await listAdminProducts({ status: productFilter.value === 'PENDING_REVIEW' ? 'PENDING_REVIEW' : productFilter.value })).content;
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载商品失败'));
  } finally {
    productLoading.value = false;
  }
};

const productDrawer = reactive<{ open: boolean; summary: ProductSummary | null; detail: ProductDetail | null; remark: string; submitting: boolean }>(
  { open: false, summary: null, detail: null, remark: '', submitting: false }
);

const openProductDrawer = async (product: ProductSummary) => {
  productDrawer.open = true;
  productDrawer.summary = product;
  productDrawer.detail = null;
  productDrawer.remark = '';
  try {
    productDrawer.detail = await fetchVendorProduct(product.vendorId, product.id);
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载商品失败'));
  }
};

const handleProductDecision = async (approve: boolean) => {
  if (!productDrawer.summary) {
    return;
  }
  productDrawer.submitting = true;
  try {
    if (approve) {
      await approveProduct(productDrawer.summary.id, { remark: productDrawer.remark });
      message.success('已通过商品');
    } else {
      await rejectProduct(productDrawer.summary.id, { remark: productDrawer.remark });
      message.success('已驳回商品');
    }
    productDrawer.open = false;
    loadProducts();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '操作失败'));
  } finally {
    productDrawer.submitting = false;
  }
};

const { width: viewportWidth, isMobile } = useViewport();

const vendorDrawerWidth = computed(() => {
  if (isMobile.value) return '100%';
  return Math.min(520, viewportWidth.value - 32);
});

const productDrawerWidth = computed(() => {
  if (isMobile.value) return '100%';
  return Math.min(720, viewportWidth.value - 32);
});

loadVendorApps();
loadProducts();
</script>

<style scoped>
.split-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--space-4);
}

.section-toolbar {
  margin-bottom: var(--space-3);
  overflow-x: auto;
}

.section-toolbar :deep(.ant-segmented) {
  max-width: 100%;
}

.card-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.review-card {
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: var(--radius-card);
  padding: var(--space-4);
  cursor: pointer;
  transition: transform 0.2s ease;
}

.review-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-card);
}

.drawer-footer {
  margin-top: var(--space-4);
  display: flex;
  justify-content: flex-end;
}

.plan-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.plan-row {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
}
</style>
