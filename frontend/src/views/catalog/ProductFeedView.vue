<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="逛逛精选"
        description="按闲鱼式瀑布流体验重绘目录，关注押金/租金组合与热门方案。"
        eyebrow="Catalog"
      >
        <template #actions>
          <a-input-search
            v-model:value="filters.keyword"
            placeholder="搜索品类或厂商"
            allow-clear
            @search="reload"
          />
        </template>
      </PageHeader>
    </template>
    <PageSection>
      <CatalogFilterBar
        :model-value="filters"
        @update:modelValue="handleFilterUpdate"
        @change="handleFiltersChange"
      />
      <ProductWaterfall :items="products" :loading="loading" @select="goDetail" />
      <div class="feed-footer">
        <a-button v-if="hasMore" :loading="loading" @click="loadMore">加载更多</a-button>
        <span v-else class="text-muted">已经到底啦</span>
      </div>
    </PageSection>
  </PageShell>
</template>

<script lang="ts" setup>
import { reactive, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import PageSection from '../../components/layout/PageSection.vue';
import ProductWaterfall from '../../components/catalog/ProductWaterfall.vue';
import CatalogFilterBar from '../../components/catalog/CatalogFilterBar.vue';
import { listCatalogProducts, type CatalogProductSummary } from '../../services/catalogService';
import { friendlyErrorMessage } from '../../utils/error';

type RentSort = 'RENT_ASC' | 'RENT_DESC' | null;

type CatalogFeedFilters = {
  keyword?: string;
  categoryCode?: string | null;
  planType?: string | null;
  depositRange?: [number | null, number | null];
  rentSort?: RentSort;
};

const router = useRouter();
const products = ref<CatalogProductSummary[]>([]);
const loading = ref(false);
const pagination = reactive({ page: 1, size: 12, total: 0 });
const filters = reactive<CatalogFeedFilters>({
  keyword: '',
  categoryCode: null,
  planType: null,
  depositRange: [null, null],
  rentSort: null
});

const hasMore = computed(() => products.value.length < pagination.total);

const fetchProducts = async (reset = false) => {
  loading.value = true;
  try {
    if (reset) {
      pagination.page = 1;
      products.value = [];
    }
    const response = await listCatalogProducts({
      keyword: filters.keyword || undefined,
      categoryCode: filters.categoryCode || undefined,
      planType: filters.planType || undefined,
      minDeposit: filters.depositRange?.[0] ?? undefined,
      maxDeposit: filters.depositRange?.[1] ?? undefined,
      rentSort: filters.rentSort || undefined,
      page: pagination.page,
      size: pagination.size
    });
    pagination.total = response.totalElements;
    if (reset) {
      products.value = response.content;
    } else {
      products.value = [...products.value, ...response.content];
    }
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载商品失败，请稍后重试'));
  } finally {
    loading.value = false;
  }
};

const loadMore = async () => {
  if (loading.value || !hasMore.value) {
    return;
  }
  pagination.page += 1;
  await fetchProducts(false);
};

const reload = () => {
  fetchProducts(true);
};

const handleFilterUpdate = (next: CatalogFeedFilters) => {
  Object.assign(filters, next);
};

const handleFiltersChange = () => {
  reload();
};

const goDetail = (productId: string) => {
  router.push({ name: 'catalog-product', params: { productId } });
};

fetchProducts(true);
</script>

<style scoped>
.feed-footer {
  display: flex;
  justify-content: center;
  margin-top: var(--space-4);
}
</style>
