<template>
  <div class="vendor-board">
    <PageSection title="商品集合" description="卡片化视图展示全部商品，轻松切换状态与关键字过滤。">
      <template #actions>
        <a-button @click="reloadProducts" :loading="loading">刷新</a-button>
        <a-button type="primary" @click="createDrawer = true">新建商品</a-button>
      </template>
      <div class="board-filters">
        <a-segmented v-model:value="statusFilter" :options="statusOptions" @change="reloadProducts" />
        <a-input-search
          v-model:value="keyword"
          allow-clear
          placeholder="按名称/类目搜索"
          @search="reloadProducts"
        />
      </div>
      <div v-if="loading && !products.length">
        <a-skeleton active :paragraph="{ rows: 4 }" />
      </div>
      <div v-else-if="products.length" class="card-grid">
        <ProductCard
          v-for="item in products"
          :key="item.id"
          :product="item"
          @open="openDetail"
          @submit="submitProduct"
        />
      </div>
      <DataStateBlock v-else title="暂无商品" description="创建首个商品，提交审核后即可上线。" />
      <div class="load-more" v-if="products.length">
        <a-button v-if="hasMore" :loading="loading" @click="loadMore">加载更多</a-button>
        <span v-else class="text-muted">已展示全部商品</span>
      </div>
    </PageSection>

    <PageSection title="咨询收件箱" description="72 小时内回复确保曝光权益，支持按状态筛选。">
      <div class="inquiry-toolbar">
        <a-segmented v-model:value="inquiryFilter" :options="inquiryOptions" @change="loadInquiries" />
        <a-button type="link" @click="loadInquiries" :loading="inquiryLoading">刷新</a-button>
      </div>
      <div v-if="inquiryLoading">
        <a-skeleton active :paragraph="{ rows: 3 }" />
      </div>
      <div v-else-if="inquiries.length" class="inquiry-list">
        <div v-for="item in inquiries" :key="item.id" class="inquiry-item">
          <div class="inquiry-item__header">
            <strong>{{ item.contactName || '匿名用户' }}</strong>
            <span class="status-pill">{{ inquiryStatusLabel(item.status) }}</span>
          </div>
          <p class="inquiry-item__message">{{ item.message }}</p>
          <p class="inquiry-item__meta">
            {{ item.contactMethod || '未留联系方式' }} · 过期时间 {{ formatDate(item.expiresAt) }}
          </p>
          <template v-if="item.status === 'OPEN'">
            <a-textarea v-model:value="replyDrafts[item.id]" :rows="2" placeholder="输入回复" />
            <div class="inquiry-item__actions">
              <a-button
                type="primary"
                :loading="replyingId === item.id"
                :disabled="!replyDrafts[item.id]"
                @click="handleReply(item.id)"
              >
                发送回复
              </a-button>
            </div>
          </template>
          <template v-else-if="item.reply">
            <div class="inquiry-item__reply">已回复：{{ item.reply }}</div>
          </template>
        </div>
      </div>
      <DataStateBlock v-else title="暂无咨询" description="当用户提交问题后会自动出现在此处。" />
    </PageSection>
  </div>

  <ProductDetailDrawer
    :open="detail.open"
    :vendor-id="vendorId"
    :product="detail.product"
    :loading="detail.loading"
    @close="closeDetail"
    @refresh="loadSelectedProduct"
    @refresh-list="reloadProducts"
  />

  <a-drawer v-model:open="createDrawer" title="新建商品" :width="520" destroy-on-close>
    <a-form layout="vertical">
      <a-form-item label="商品名称" required>
        <a-input v-model:value="createForm.name" placeholder="请输入名称" />
      </a-form-item>
      <a-form-item label="分类编码" required>
        <a-input v-model:value="createForm.categoryCode" placeholder="如 OFFICE" />
      </a-form-item>
      <a-form-item label="封面图">
        <div class="cover-upload">
          <img v-if="coverState.fileUrl" :src="coverState.fileUrl" alt="cover" />
          <a-upload
            :show-upload-list="false"
            :beforeUpload="handleCoverUpload"
            accept="image/*"
          >
            <a-button :loading="coverState.uploading">上传图片</a-button>
          </a-upload>
          <a-button
            v-if="coverState.fileUrl"
            type="link"
            danger
            size="small"
            @click="removeCover"
          >
            移除
          </a-button>
        </div>
      </a-form-item>
      <a-form-item label="描述">
        <a-textarea v-model:value="createForm.description" :rows="4" />
      </a-form-item>
    </a-form>
    <div class="drawer-footer">
      <a-space>
        <a-button @click="createDrawer = false">取消</a-button>
        <a-button type="primary" :loading="createSubmitting" @click="handleCreate">创建商品</a-button>
      </a-space>
    </div>
  </a-drawer>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue';
import { message, Upload } from 'ant-design-vue';
import type { UploadProps } from 'ant-design-vue';
import PageSection from '../../../components/layout/PageSection.vue';
import DataStateBlock from '../../../components/feedback/DataStateBlock.vue';
import ProductCard from '../../../components/vendor/ProductCard.vue';
import ProductDetailDrawer from './ProductDetailDrawer.vue';
import { useVendorContext } from '../../../composables/useVendorContext';
import {
  createVendorProduct,
  fetchVendorProduct,
  listVendorProducts,
  submitVendorProduct,
  uploadProductCover,
  deleteUploadedProductCover,
  type ProductDetail,
  type ProductStatus,
  type ProductSummary
} from '../../../services/productService';
import {
  listVendorInquiries,
  replyVendorInquiry,
  type ProductInquiry
} from '../../../services/vendorService';
import { friendlyErrorMessage } from '../../../utils/error';

const { vendorId } = useVendorContext();

const products = ref<ProductSummary[]>([]);
const loading = ref(false);
const keyword = ref('');
const statusFilter = ref<ProductStatus | 'ALL'>('ALL');
const pagination = reactive({ page: 1, size: 6, total: 0 });

const detail = reactive<{ open: boolean; loading: boolean; product: ProductDetail | null; productId: string | null }>(
  {
    open: false,
    loading: false,
    product: null,
    productId: null
  }
);

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '草稿', value: 'DRAFT' },
  { label: '审核中', value: 'PENDING_REVIEW' },
  { label: '已上线', value: 'ACTIVE' },
  { label: '已下线', value: 'INACTIVE' },
  { label: '被驳回', value: 'REJECTED' }
];

const hasMore = computed(() => products.value.length < pagination.total);

const fetchProducts = async (reset = false) => {
  if (!vendorId.value) {
    return;
  }
  loading.value = true;
  try {
    if (reset) {
      pagination.page = 1;
    }
    const response = await listVendorProducts(vendorId.value, {
      keyword: keyword.value || undefined,
      status: statusFilter.value === 'ALL' ? undefined : statusFilter.value,
      page: pagination.page,
      size: pagination.size
    });
    pagination.total = response.totalElements;
    products.value = reset ? response.content : [...products.value, ...response.content];
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载商品失败'));
  } finally {
    loading.value = false;
  }
};

const reloadProducts = () => fetchProducts(true);

const loadMore = () => {
  if (!hasMore.value || loading.value) {
    return;
  }
  pagination.page += 1;
  fetchProducts(false);
};

const openDetail = (productId: string) => {
  detail.open = true;
  detail.product = null;
  detail.productId = productId;
  loadSelectedProduct();
};

const closeDetail = () => {
  detail.open = false;
};

const loadSelectedProduct = async () => {
  if (!vendorId.value || !detail.productId) {
    return;
  }
  detail.loading = true;
  try {
    detail.product = await fetchVendorProduct(vendorId.value, detail.productId);
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载商品详情失败'));
  } finally {
    detail.loading = false;
  }
};

const submitProduct = async (productId: string) => {
  if (!vendorId.value) {
    return;
  }
  try {
    await submitVendorProduct(vendorId.value, productId);
    message.success('已提交审核');
    reloadProducts();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '提交审核失败'));
  }
};

const inquiries = ref<ProductInquiry[]>([]);
const inquiryLoading = ref(false);
const inquiryFilter = ref<ProductInquiry['status'] | 'ALL'>('OPEN');
const inquiryOptions = [
  { label: '开放', value: 'OPEN' },
  { label: '已回复', value: 'RESPONDED' },
  { label: '已过期', value: 'EXPIRED' },
  { label: '全部', value: 'ALL' }
];

const formatDate = (value: string) => new Date(value).toLocaleString();
const inquiryStatusLabel = (status: ProductInquiry['status']) => {
  switch (status) {
    case 'RESPONDED':
      return '已回复';
    case 'EXPIRED':
      return '已过期';
    default:
      return '待回复';
  }
};

const loadInquiries = async () => {
  if (!vendorId.value) {
    return;
  }
  inquiryLoading.value = true;
  try {
    inquiries.value = await listVendorInquiries(vendorId.value, {
      status: inquiryFilter.value === 'ALL' ? undefined : inquiryFilter.value
    });
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载咨询失败'));
  } finally {
    inquiryLoading.value = false;
  }
};

const replyDrafts = reactive<Record<string, string>>({});
const replyingId = ref<string | null>(null);

const handleReply = async (inquiryId: string) => {
  if (!vendorId.value || !replyDrafts[inquiryId]) {
    return;
  }
  replyingId.value = inquiryId;
  try {
    await replyVendorInquiry(vendorId.value, inquiryId, replyDrafts[inquiryId]);
    message.success('回复成功');
    replyDrafts[inquiryId] = '';
    loadInquiries();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '回复失败'));
  } finally {
    replyingId.value = null;
  }
};

const createDrawer = ref(false);
const createSubmitting = ref(false);
const createForm = reactive({
  name: '',
  categoryCode: '',
  coverImageUrl: '',
  description: ''
});

const coverState = reactive<{ uploading: boolean; fileUrl: string; fileName: string }>(
  {
    uploading: false,
    fileUrl: '',
    fileName: ''
  }
);

const handleCoverUpload: UploadProps['beforeUpload'] = async (file) => {
  if (!vendorId.value) {
    message.warning('缺少厂商身份');
    return Upload.LIST_IGNORE;
  }
  coverState.uploading = true;
  try {
    const asset = await uploadProductCover(vendorId.value, file);
    coverState.fileUrl = asset.fileUrl;
    coverState.fileName = asset.fileName;
    createForm.coverImageUrl = asset.fileUrl;
    message.success('封面上传成功');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '上传失败'));
  } finally {
    coverState.uploading = false;
  }
  return Upload.LIST_IGNORE;
};

const removeCover = async () => {
  if (!vendorId.value || !coverState.fileName) {
    coverState.fileUrl = '';
    createForm.coverImageUrl = '';
    return;
  }
  try {
    await deleteUploadedProductCover(vendorId.value, coverState.fileName);
  } catch (error) {
    console.warn('删除封面失败', error);
  } finally {
    coverState.fileName = '';
    coverState.fileUrl = '';
    createForm.coverImageUrl = '';
  }
};

const resetCreateForm = () => {
  createForm.name = '';
  createForm.categoryCode = '';
  createForm.coverImageUrl = '';
  createForm.description = '';
  coverState.fileName = '';
  coverState.fileUrl = '';
};

const handleCreate = async () => {
  if (!vendorId.value) {
    return;
  }
  if (!createForm.name || !createForm.categoryCode) {
    message.warning('请填写必填项');
    return;
  }
  createSubmitting.value = true;
  try {
    const product = await createVendorProduct(vendorId.value, {
      name: createForm.name,
      categoryCode: createForm.categoryCode,
      description: createForm.description || undefined,
      coverImageUrl: createForm.coverImageUrl || undefined
    });
    message.success('创建成功');
    createDrawer.value = false;
    resetCreateForm();
    reloadProducts();
    openDetail(product.id);
  } catch (error) {
    message.error(friendlyErrorMessage(error, '创建失败'));
  } finally {
    createSubmitting.value = false;
  }
};

watch(
  () => vendorId.value,
  (id) => {
    if (id) {
      fetchProducts(true);
      loadInquiries();
    }
  },
  { immediate: true }
);
</script>

<style scoped>
.vendor-board {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--space-5);
}

.board-filters {
  display: flex;
  gap: var(--space-3);
  flex-wrap: wrap;
  margin-bottom: var(--space-4);
  align-items: center;
}

.board-filters :deep(.ant-segmented) {
  max-width: 100%;
  flex-shrink: 1;
  min-width: 0;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: var(--space-4);
}

.load-more {
  margin-top: var(--space-4);
  text-align: center;
}

.inquiry-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
  flex-wrap: wrap;
  gap: var(--space-2);
}

.inquiry-toolbar :deep(.ant-segmented) {
  max-width: 100%;
  flex-shrink: 1;
  min-width: 0;
}

.inquiry-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.inquiry-item {
  border: 1px solid rgba(148, 163, 184, 0.25);
  border-radius: var(--radius-card);
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.inquiry-item__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-pill {
  background: var(--color-primary-muted);
  color: var(--color-primary);
  padding: 2px 8px;
  border-radius: var(--radius-pill);
  font-size: var(--font-size-caption);
}

.inquiry-item__message {
  margin: 0;
  font-weight: 500;
}

.inquiry-item__meta {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-caption);
}

.inquiry-item__reply {
  color: var(--color-text-secondary);
  font-size: var(--font-size-caption);
}

.inquiry-item__actions {
  display: flex;
  justify-content: flex-end;
}

.cover-upload {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.cover-upload img {
  width: 100%;
  border-radius: var(--radius-card);
  border: 1px solid var(--color-border);
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-4);
}

@media (max-width: 960px) {
  .vendor-board {
    grid-template-columns: 1fr;
  }
}
</style>
