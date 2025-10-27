<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>商品目录</h2>
        <p class="page-header__meta">浏览可租赁商品，按分类或关键字筛选适合你的方案。</p>
      </div>
      <a-space>
        <a-input-search
          v-model:value="filters.keyword"
          placeholder="搜索商品"
          style="width: 260px"
          @search="loadProducts"
          allow-clear
        />
        <a-button type="primary" @click="loadProducts" :loading="loading">搜索</a-button>
      </a-space>
    </div>

    <a-row :gutter="16">
      <a-col v-for="item in products" :key="item.id" :xs="24" :sm="12" :lg="8">
        <a-card hoverable class="product-card" @click="goDetail(item.id)">
          <div class="product-card__cover" :style="coverStyle(item.coverImageUrl)">
            <span v-if="!item.coverImageUrl">{{ (item.name ?? '?').slice(0, 2) }}</span>
          </div>
          <div class="product-card__body">
            <h3>{{ item.name ?? '未知商品' }}</h3>
            <p class="product-card__meta">分类：{{ item.categoryCode ?? '-' }}</p>
            <div class="product-card__tags">
              <a-tag
                v-for="plan in item.rentalPlans.slice(0, 2)"
                :key="plan.id"
              >
                {{ plan.planType }} · ¥{{ formatCurrency(plan.rentAmountMonthly) }}/月
              </a-tag>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>

    <div class="page__pagination">
      <a-pagination
        :current="pagination.page"
        :page-size="pagination.size"
        :total="pagination.total"
        show-size-changer
        @change="handlePageChange"
        @showSizeChange="handleSizeChange"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { listCatalogProducts, type CatalogProductSummary } from '../../services/catalogService';

const router = useRouter();
const loading = ref(false);
const products = ref<CatalogProductSummary[]>([]);
const pagination = reactive({ page: 1, size: 9, total: 0 });
const filters = reactive<{ keyword?: string }>({});

const formatCurrency = (value: number) => value.toFixed(2);

const coverStyle = (url?: string | null) => ({
  backgroundImage: url ? `url(${url})` : 'none'
});

const loadProducts = async () => {
  loading.value = true;
  try {
    const result = await listCatalogProducts({
      keyword: filters.keyword || undefined,
      page: pagination.page,
      size: pagination.size
    });
    products.value = result.content;
    pagination.total = result.totalElements;
  } catch (error) {
    console.error('加载商品目录失败', error);
    message.error('加载商品目录失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  loadProducts();
};

const handleSizeChange = (_: number, size: number) => {
  pagination.size = size;
  pagination.page = 1;
  loadProducts();
};

const goDetail = (productId: string) => {
  router.push({ name: 'catalog-product', params: { productId } });
};

loadProducts();
</script>

<style scoped>
.product-card {
  margin-bottom: 16px;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.product-card:hover {
  transform: translateY(-4px);
}

.product-card__cover {
  height: 180px;
  border-radius: 12px;
  background: linear-gradient(135deg, #1677ff33, #9333ea33);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1f2937;
  font-size: 28px;
  font-weight: 600;
}

.product-card__body {
  margin-top: 16px;
}

.product-card__body h3 {
  margin: 0 0 8px;
}

.product-card__meta {
  margin: 0;
  color: #64748b;
}

.product-card__tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.page__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
