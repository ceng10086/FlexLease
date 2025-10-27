<template>
  <div class="page-wrapper">
    <a-row :gutter="16">
      <a-col v-for="product in catalog" :key="product.id" :xs="24" :sm="12" :lg="8">
        <a-card class="product-card" :loading="loading">
          <template #cover>
            <img :src="product.cover" :alt="product.name" class="catalog-cover" />
          </template>
          <template #title>
            <div class="card-title">{{ product.name }}</div>
            <a-tag color="blue">{{ product.category }}</a-tag>
          </template>
          <div class="product-meta">
            <span class="price">月租 {{ formatCurrency(product.pricePerMonth) }}</span>
            <span class="deposit">押金 {{ formatCurrency(product.deposit) }}</span>
          </div>
          <div class="product-tags">
            <a-tag v-for="mode in product.modes" :key="mode" color="purple">{{ mode }}</a-tag>
          </div>
          <p class="product-desc">{{ product.description }}</p>
          <a-space>
            <a-button type="primary" @click="viewProduct(product)">查看方案</a-button>
            <a-button @click="initiateOrder(product)">快速下单</a-button>
          </a-space>
        </a-card>
      </a-col>
    </a-row>

    <a-drawer v-model:open="drawerVisible" :title="selectedProduct?.name" width="420">
      <template v-if="selectedProduct">
        <p class="drawer-section">租赁模式</p>
        <a-tag v-for="mode in selectedProduct.modes" :key="mode">{{ mode }}</a-tag>
        <p class="drawer-section">方案亮点</p>
        <a-list :data-source="selectedProduct.planHighlights" :split="false">
          <template #renderItem="{ item }">
            <a-list-item>{{ item }}</a-list-item>
          </template>
        </a-list>
        <p class="drawer-section">适配人群</p>
        <a-typography-paragraph>{{ selectedProduct.description }}</a-typography-paragraph>
      </template>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { message } from 'ant-design-vue';
import { fetchCatalog } from '@/api/catalog';
import { formatCurrency } from '@/utils/formatters';
import type { CatalogProduct } from '@/types';

const catalog = ref<CatalogProduct[]>([]);
const loading = ref(false);
const drawerVisible = ref(false);
const selectedProduct = ref<CatalogProduct | null>(null);

async function loadCatalog() {
  loading.value = true;
  try {
    const response = await fetchCatalog();
    catalog.value = response.content;
  } finally {
    loading.value = false;
  }
}

function viewProduct(product: CatalogProduct) {
  selectedProduct.value = product;
  drawerVisible.value = true;
}

function initiateOrder(product: CatalogProduct) {
  message.success(`${product.name} 已加入预下单流程`);
}

onMounted(() => {
  loadCatalog();
});
</script>

<style scoped>
.page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.product-card {
  border-radius: 18px;
  overflow: hidden;
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.08);
  min-height: 380px;
}

.catalog-cover {
  width: 100%;
  height: 160px;
  object-fit: cover;
}

.card-title {
  font-weight: 600;
  font-size: 18px;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 500;
}

.product-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.product-desc {
  min-height: 60px;
  color: var(--app-muted);
}

.drawer-section {
  font-weight: 600;
  margin-top: 16px;
}
</style>
