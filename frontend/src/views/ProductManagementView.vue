<template>
  <div class="page">
    <header class="page__header">
      <div>
        <h2>商品管理</h2>
        <p class="page__subtitle">配置共享租赁商品，并提交审核上线。</p>
      </div>
      <a-button type="primary" @click="openCreateModal">新建商品</a-button>
    </header>

    <a-card bordered>
      <a-table :data-source="products" :loading="loading" :pagination="false" row-key="id">
        <a-table-column key="name" title="名称" data-index="name" />
        <a-table-column key="category" title="分类" data-index="categoryCode" />
        <a-table-column key="status" title="状态">
          <template #default="{ record }">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column key="createdAt" title="创建时间">
          <template #default="{ record }">
            {{ formatDate(record.createdAt) }}
          </template>
        </a-table-column>
        <a-table-column key="actions" title="操作">
          <template #default="{ record }">
            <a-space size="small">
              <a-button size="small" @click="viewDetail(record.id)">查看</a-button>
              <a-button
                size="small"
                type="primary"
                :disabled="record.status === 'PENDING_REVIEW' || record.status === 'ACTIVE'"
                @click="handleSubmit(record.id)"
              >提交审核</a-button>
            </a-space>
          </template>
        </a-table-column>
      </a-table>

      <div class="page__pagination">
        <a-pagination
          :current="pagination.page"
          :page-size="pagination.size"
          :total="pagination.total"
          show-size-changer
          @change="handlePageChange"
          @showSizeChange="handlePageSizeChange"
        />
      </div>
    </a-card>

    <a-modal v-model:open="modal.open" title="新建商品" :confirm-loading="modal.submitting" @ok="handleCreate">
      <a-form layout="vertical">
        <a-form-item label="商品名称" required>
          <a-input v-model:value="modal.form.name" placeholder="请输入商品名称" />
        </a-form-item>
        <a-form-item label="分类编码" required>
          <a-input v-model:value="modal.form.categoryCode" placeholder="例如 OFFICE" />
        </a-form-item>
        <a-form-item label="封面图 URL">
          <a-input v-model:value="modal.form.coverImageUrl" placeholder="可选" />
        </a-form-item>
        <a-form-item label="商品描述">
          <a-textarea v-model:value="modal.form.description" :rows="4" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import type { ProductSummary, ProductPayload } from '../utils/productApi';
import { createVendorProduct, listVendorProducts, submitVendorProduct } from '../utils/productApi';

const auth = useAuthStore();
const router = useRouter();

const products = ref<ProductSummary[]>([]);
const loading = ref(false);
const pagination = reactive({ page: 1, size: 10, total: 0 });

const modal = reactive({
  open: false,
  submitting: false,
  form: {
    name: '',
    categoryCode: '',
    coverImageUrl: '',
    description: ''
  } as ProductPayload
});

const vendorId = () => {
  if (!auth.user?.id) {
    throw new Error('未获取到当前用户 ID');
  }
  return auth.user.id;
};

const loadProducts = async () => {
  try {
    loading.value = true;
    const result = await listVendorProducts(vendorId(), {
      page: pagination.page,
      size: pagination.size
    });
    products.value = result.content;
    pagination.total = result.totalElements;
  } catch (error) {
    console.error('加载商品失败', error);
    message.error('加载商品列表失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const handlePageChange = (page: number) => {
  pagination.page = page;
  loadProducts();
};

const handlePageSizeChange = (_: number, size: number) => {
  pagination.size = size;
  pagination.page = 1;
  loadProducts();
};

const openCreateModal = () => {
  modal.open = true;
  modal.form = {
    name: '',
    categoryCode: '',
    coverImageUrl: '',
    description: ''
  };
};

const handleCreate = async () => {
  if (!modal.form.name || !modal.form.categoryCode) {
    message.warning('请填写完整信息');
    return;
  }
  try {
    modal.submitting = true;
    await createVendorProduct(vendorId(), modal.form);
    message.success('创建成功');
    modal.open = false;
    loadProducts();
  } catch (error) {
    console.error('创建商品失败', error);
    message.error('创建失败，请稍后重试');
  } finally {
    modal.submitting = false;
  }
};

const handleSubmit = async (productId: string) => {
  try {
    await submitVendorProduct(vendorId(), productId);
    message.success('已提交审核');
    loadProducts();
  } catch (error) {
    console.error('提交审核失败', error);
    message.error('提交审核失败');
  }
};

const viewDetail = (productId: string) => {
  router.push({ name: 'dashboard', query: { focusProduct: productId } });
};

const statusLabel = (status: ProductSummary['status']) => {
  switch (status) {
    case 'DRAFT':
      return '草稿';
    case 'PENDING_REVIEW':
      return '待审核';
    case 'ACTIVE':
      return '已上架';
    case 'INACTIVE':
      return '已下架';
    case 'REJECTED':
      return '已驳回';
    default:
      return status;
  }
};

const statusColor = (status: ProductSummary['status']) => {
  switch (status) {
    case 'ACTIVE':
      return 'green';
    case 'PENDING_REVIEW':
      return 'blue';
    case 'REJECTED':
      return 'red';
    case 'INACTIVE':
      return 'orange';
    default:
      return 'default';
  }
};

const formatDate = (value?: string | null) => {
  if (!value) return '-';
  return new Date(value).toLocaleString();
};

onMounted(() => {
  loadProducts();
});
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
}

.page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page__subtitle {
  color: #6b7280;
  margin: 4px 0 0;
}

.page__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
