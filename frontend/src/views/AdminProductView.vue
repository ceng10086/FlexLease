<template>
  <div class="page">
    <header class="page__header">
      <div>
        <h2>商品审核</h2>
        <p class="page__subtitle">审批厂商提交的商品，控制上线状态。</p>
      </div>
      <a-space>
        <a-button @click="loadProducts">刷新</a-button>
      </a-space>
    </header>

    <a-card bordered>
      <a-table :data-source="products" :loading="loading" row-key="id" :pagination="false">
        <a-table-column key="name" title="商品" data-index="name" />
        <a-table-column key="vendor" title="厂商">
          <template #default="{ record }">
            {{ record.vendorId.slice(0, 8) }}
          </template>
        </a-table-column>
        <a-table-column key="submittedAt" title="提交时间">
          <template #default="{ record }">
            {{ formatDate(record.submittedAt) }}
          </template>
        </a-table-column>
        <a-table-column key="actions" title="操作">
          <template #default="{ record }">
            <a-space>
              <a-button size="small" type="primary" @click="handleApprove(record.id)">通过</a-button>
              <a-button size="small" danger @click="openReject(record.id)">驳回</a-button>
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

    <a-modal
      v-model:open="rejectModal.open"
      title="驳回商品"
      :confirm-loading="rejectModal.submitting"
      @ok="handleReject"
    >
      <a-form layout="vertical">
        <a-form-item label="驳回原因">
          <a-textarea v-model:value="rejectModal.remark" placeholder="请输入驳回原因" :rows="4" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../stores/auth';
import type { ProductSummary } from '../utils/productApi';
import { approveProduct, listAdminProducts, rejectProduct } from '../utils/productApi';

const auth = useAuthStore();
const products = ref<ProductSummary[]>([]);
const loading = ref(false);
const pagination = reactive({ page: 1, size: 10, total: 0 });

const rejectModal = reactive({
  open: false,
  submitting: false,
  productId: '' as string,
  remark: ''
});

const reviewerId = () => {
  if (!auth.user?.id) {
    throw new Error('未获取到当前用户 ID');
  }
  return auth.user.id;
};

const loadProducts = async () => {
  try {
    loading.value = true;
    const result = await listAdminProducts({
      status: 'PENDING_REVIEW',
      page: pagination.page,
      size: pagination.size
    });
    products.value = result.content;
    pagination.total = result.totalElements;
  } catch (error) {
    console.error('加载待审核商品失败', error);
    message.error('加载失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const handleApprove = async (productId: string) => {
  try {
    await approveProduct(productId, { reviewerId: reviewerId(), remark: '审核通过' });
    message.success('已通过审核');
    loadProducts();
  } catch (error) {
    console.error('审核通过失败', error);
    message.error('操作失败，请稍后重试');
  }
};

const openReject = (productId: string) => {
  rejectModal.productId = productId;
  rejectModal.remark = '';
  rejectModal.open = true;
};

const handleReject = async () => {
  if (!rejectModal.productId) {
    return;
  }
  try {
    rejectModal.submitting = true;
    await rejectProduct(rejectModal.productId, {
      reviewerId: reviewerId(),
      remark: rejectModal.remark
    });
    message.success('已驳回');
    rejectModal.open = false;
    loadProducts();
  } catch (error) {
    console.error('驳回商品失败', error);
    message.error('操作失败，请稍后重试');
  } finally {
    rejectModal.submitting = false;
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
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page__subtitle {
  color: #6b7280;
  margin-top: 4px;
}

.page__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
