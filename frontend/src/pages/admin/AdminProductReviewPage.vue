<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>商品审核</h2>
        <p class="page-header__meta">查看厂商提交的商品与租赁方案，控制上线质量。</p>
      </div>
      <a-space>
        <a-select
          v-model:value="filters.status"
          style="width: 180px"
          @change="loadProducts"
        >
          <a-select-option value="PENDING_REVIEW">待审核</a-select-option>
          <a-select-option value="ACTIVE">已上架</a-select-option>
          <a-select-option value="REJECTED">已驳回</a-select-option>
        </a-select>
        <a-button type="primary" @click="loadProducts" :loading="loading">刷新</a-button>
      </a-space>
    </div>

    <a-card>
      <a-table
        :data-source="products"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        @change="handleTableChange"
      >
        <a-table-column title="商品名称" data-index="name" key="name" />
        <a-table-column title="厂商" key="vendor">
          <template #default="{ record }">
            {{ record.vendorId.slice(0, 8) }}
          </template>
        </a-table-column>
        <a-table-column title="状态" key="status">
          <template #default="{ record }">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="提交时间" key="submittedAt">
          <template #default="{ record }">{{ formatDate(record.submittedAt) }}</template>
        </a-table-column>
        <a-table-column title="操作" key="actions">
          <template #default="{ record }">
            <a-space>
              <a-button size="small" @click="openDetail(record)">详情</a-button>
              <a-button
                size="small"
                type="primary"
                v-if="record.status === 'PENDING_REVIEW'"
                @click="approve(record)">
                通过
              </a-button>
              <a-button
                size="small"
                danger
                v-if="record.status === 'PENDING_REVIEW'"
                @click="openReject(record)">
                驳回
              </a-button>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <a-drawer
      v-model:open="detailDrawer.open"
      title="商品详情"
      :width="720"
      destroy-on-close
    >
      <template v-if="detailDrawer.loading">
        <a-spin />
      </template>
      <template v-else-if="detailDrawer.product">
        <div class="detail">
          <h3>{{ detailDrawer.product.name }}</h3>
          <p class="detail__meta">分类：{{ detailDrawer.product.categoryCode }}</p>
          <p class="detail__meta">状态：{{ statusLabel(detailDrawer.product.status) }}</p>
          <p class="detail__meta" v-if="detailDrawer.product.description">{{ detailDrawer.product.description }}</p>
        </div>
        <a-divider />
        <h4>租赁方案</h4>
        <a-empty v-if="!detailDrawer.product.rentalPlans?.length" description="暂无租赁方案" />
        <a-collapse v-else>
          <a-collapse-panel
            v-for="plan in detailDrawer.product.rentalPlans"
            :key="plan.id"
            :header="planHeader(plan)"
          >
            <p class="detail__meta">押金：¥{{ formatCurrency(plan.depositAmount) }}</p>
            <p class="detail__meta">月租金：¥{{ formatCurrency(plan.rentAmountMonthly) }}</p>
            <p class="detail__meta" v-if="plan.buyoutPrice">买断价：¥{{ formatCurrency(plan.buyoutPrice) }}</p>
            <h5>SKU</h5>
            <a-table :data-source="plan.skus" :pagination="false" size="small" row-key="id">
              <a-table-column title="SKU" data-index="skuCode" key="sku" />
              <a-table-column title="可用库存" key="stock">
                <template #default="{ record }">
                  {{ record.stockAvailable }}/{{ record.stockTotal }}
                </template>
              </a-table-column>
              <a-table-column title="状态" data-index="status" key="status" />
            </a-table>
          </a-collapse-panel>
        </a-collapse>
      </template>
      <template v-else>
        <a-empty description="未找到商品详情" />
      </template>
    </a-drawer>

    <a-modal
      v-model:open="rejectModal.open"
      title="驳回商品"
      :confirm-loading="rejectModal.submitting"
      @ok="handleReject"
    >
      <a-form layout="vertical">
        <a-form-item label="驳回原因">
          <a-textarea v-model:value="rejectModal.remark" :rows="4" placeholder="请输入驳回原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import {
  listAdminProducts,
  approveProduct,
  rejectProduct,
  fetchVendorProduct,
  type ProductSummary,
  type ProductDetail,
  type ProductStatus
} from '../../services/productService';

const loading = ref(false);
const products = ref<ProductSummary[]>([]);

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0
});

const filters = reactive<{ status: ProductStatus }>({ status: 'PENDING_REVIEW' });

const detailDrawer = reactive<{ open: boolean; loading: boolean; product: ProductDetail | null }>(
  {
    open: false,
    loading: false,
    product: null
  }
);

const rejectModal = reactive<{ open: boolean; submitting: boolean; product: ProductSummary | null; remark: string }>(
  {
    open: false,
    submitting: false,
    product: null,
    remark: ''
  }
);

const statusLabel = (status: ProductStatus) => {
  switch (status) {
    case 'ACTIVE':
      return '已上架';
    case 'PENDING_REVIEW':
      return '待审核';
    case 'REJECTED':
      return '已驳回';
    case 'INACTIVE':
      return '已下架';
    default:
      return '草稿';
  }
};

const statusColor = (status: ProductStatus) => {
  switch (status) {
    case 'ACTIVE':
      return 'green';
    case 'PENDING_REVIEW':
      return 'blue';
    case 'REJECTED':
      return 'red';
    default:
      return 'orange';
  }
};

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '-');
const formatCurrency = (value?: number | null) => value?.toFixed(2) ?? '0.00';

const loadProducts = async () => {
  loading.value = true;
  try {
    const result = await listAdminProducts({
      status: filters.status,
      page: pagination.current,
      size: pagination.pageSize
    });
    products.value = result.content;
    pagination.total = result.totalElements;
  } catch (error) {
    console.error('Failed to load products', error);
    message.error('加载商品失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (pager: { current?: number; pageSize?: number }) => {
  pagination.current = pager.current ?? pagination.current;
  pagination.pageSize = pager.pageSize ?? pagination.pageSize;
  loadProducts();
};

const openDetail = async (record: ProductSummary) => {
  detailDrawer.open = true;
  detailDrawer.loading = true;
  try {
    detailDrawer.product = await fetchVendorProduct(record.vendorId, record.id);
  } catch (error) {
    console.error('Failed to fetch product detail', error);
    message.error('加载商品详情失败');
    detailDrawer.product = null;
  } finally {
    detailDrawer.loading = false;
  }
};

const approve = async (record: ProductSummary) => {
  try {
    await approveProduct(record.id, { remark: '审核通过' });
    message.success('已通过商品审核');
    detailDrawer.open = false;
    await loadProducts();
  } catch (error: any) {
    console.error('Approve product failed', error);
    message.error(error?.response?.data?.message ?? '操作失败');
  }
};

const openReject = (record: ProductSummary) => {
  rejectModal.product = record;
  rejectModal.remark = '';
  rejectModal.open = true;
};

const handleReject = async () => {
  if (!rejectModal.product) {
    return;
  }
  rejectModal.submitting = true;
  try {
    await rejectProduct(rejectModal.product.id, {
      remark: rejectModal.remark
    });
    message.success('已驳回商品');
    rejectModal.open = false;
    detailDrawer.open = false;
    await loadProducts();
  } catch (error: any) {
    console.error('Reject product failed', error);
    message.error(error?.response?.data?.message ?? '操作失败');
  } finally {
    rejectModal.submitting = false;
  }
};

const planHeader = (plan: ProductDetail['rentalPlans'][number]) => {
  return `${plan.planType} · ${plan.termMonths} 个月 · ${plan.status}`;
};

loadProducts();
</script>

<style scoped>
.detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail__meta {
  color: #64748b;
  margin: 0;
}
</style>
