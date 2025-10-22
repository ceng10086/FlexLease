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

    <a-drawer
      v-model:open="detail.open"
      :width="720"
      title="商品详情"
      destroy-on-close
    >
      <a-spin :spinning="detail.loading">
        <template v-if="detail.product">
          <div class="detail__section">
            <h3>{{ detail.product.name }}</h3>
            <p class="detail__meta">分类：{{ detail.product.categoryCode }}</p>
            <p class="detail__meta">状态：{{ statusLabel(detail.product.status) }}</p>
            <p v-if="detail.product.description" class="detail__desc">{{ detail.product.description }}</p>
          </div>

          <div class="detail__section">
            <div class="detail__section-header">
              <h4>租赁方案</h4>
              <a-button type="dashed" size="small" @click="openPlanModal">新增方案</a-button>
            </div>
            <p v-if="!detail.product.rentalPlans?.length" class="detail__empty">暂未配置方案</p>
            <a-collapse v-else :accordion="false">
              <a-collapse-panel
                v-for="plan in detail.product.rentalPlans"
                :key="plan.id"
                :header="planHeader(plan)"
              >
                <div class="plan__meta">
                  <span>租期：{{ plan.termMonths }} 个月</span>
                  <span>押金：¥{{ formatCurrency(plan.depositAmount) }}</span>
                  <span>月租金：¥{{ formatCurrency(plan.rentAmountMonthly) }}</span>
                  <span v-if="plan.buyoutPrice">买断价：¥{{ formatCurrency(plan.buyoutPrice) }}</span>
                </div>
                <div class="plan__actions">
                  <a-space size="small">
                    <a-button size="small" type="primary" @click="openSkuModal(plan.id)">新增 SKU</a-button>
                    <a-button
                      size="small"
                      :type="plan.status === 'ACTIVE' ? 'default' : 'primary'"
                      @click="togglePlanStatus(plan)"
                    >
                      {{ plan.status === 'ACTIVE' ? '停用' : '启用' }}
                    </a-button>
                  </a-space>
                </div>
                <a-table
                  :data-source="plan.skus || []"
                  :pagination="false"
                  size="small"
                  row-key="id"
                  class="sku-table"
                >
                  <a-table-column key="sku" title="SKU 编码" data-index="skuCode" />
                  <a-table-column key="stock" title="库存">
                    <template #default="{ record }">
                      {{ record.stockAvailable }}/{{ record.stockTotal }}
                    </template>
                  </a-table-column>
                  <a-table-column key="status" title="状态" data-index="status" />
                  <a-table-column key="actions" title="操作">
                    <template #default="{ record }">
                      <a-space size="small">
                        <a-button size="small" @click="openSkuModal(plan.id, record)">编辑</a-button>
                        <a-button size="small" @click="openInventoryModal(plan.id, record)">调整库存</a-button>
                      </a-space>
                    </template>
                  </a-table-column>
                </a-table>
              </a-collapse-panel>
            </a-collapse>
          </div>
        </template>
        <template v-else>
          <p class="detail__empty">未找到商品详情</p>
        </template>
      </a-spin>
    </a-drawer>

    <a-modal
      v-model:open="planModal.open"
      title="新增租赁方案"
      :confirm-loading="planModal.submitting"
      @ok="handleCreatePlan"
      destroy-on-close
    >
      <a-form layout="vertical">
        <a-form-item label="方案类型" required>
          <a-select v-model:value="planModal.form.planType">
            <a-select-option value="STANDARD">标准方案</a-select-option>
            <a-select-option value="RENT_TO_OWN">先租后买</a-select-option>
            <a-select-option value="LEASE_TO_SALE">租售结合</a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="租期（月）" required>
              <a-input-number v-model:value="planModal.form.termMonths" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="押金" required>
              <a-input-number v-model:value="planModal.form.depositAmount" :min="0" :step="100" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="月租金" required>
              <a-input-number v-model:value="planModal.form.rentAmountMonthly" :min="0" :step="50" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="买断价">
              <a-input-number v-model:value="planModal.form.buyoutPrice" :min="0" :step="100" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="允许续租">
          <a-switch v-model:checked="planModal.form.allowExtend" />
        </a-form-item>
        <a-row :gutter="12" v-if="planModal.form.allowExtend">
          <a-col :span="12">
            <a-form-item label="续租单位">
              <a-input v-model:value="planModal.form.extensionUnit" placeholder="例如 MONTH" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="续租价格">
              <a-input-number v-model:value="planModal.form.extensionPrice" :min="0" :step="50" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="skuModal.open"
      :title="skuModal.editing ? '编辑 SKU' : '新增 SKU'"
      :confirm-loading="skuModal.submitting"
      @ok="handleSaveSku"
      destroy-on-close
    >
      <a-form layout="vertical">
        <a-form-item label="SKU 编码" required>
          <a-input v-model:value="skuModal.form.skuCode" placeholder="请输入 SKU 编码" :disabled="skuModal.editing" />
        </a-form-item>
        <a-form-item label="SKU 属性 (JSON)" tooltip="例如 { &quot;color&quot;: &quot;白色&quot; }">
          <a-textarea v-model:value="skuModal.form.attributesText" :rows="4" placeholder="可选，JSON 格式" />
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="库存总量" required>
              <a-input-number v-model:value="skuModal.form.stockTotal" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="可用库存">
              <a-input-number v-model:value="skuModal.form.stockAvailable" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="状态">
          <a-select v-model:value="skuModal.form.status">
            <a-select-option value="ACTIVE">启用</a-select-option>
            <a-select-option value="INACTIVE">停用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="inventoryModal.open"
      title="调整库存"
      :confirm-loading="inventoryModal.submitting"
      @ok="handleAdjustInventory"
      destroy-on-close
    >
      <a-form layout="vertical">
        <a-form-item label="调整类型" required>
          <a-select v-model:value="inventoryModal.form.changeType">
            <a-select-option value="INBOUND">入库</a-select-option>
            <a-select-option value="OUTBOUND">出库</a-select-option>
            <a-select-option value="RESERVE">预占</a-select-option>
            <a-select-option value="RELEASE">释放</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数量" required>
          <a-input-number v-model:value="inventoryModal.form.quantity" :min="1" style="width: 100%" />
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
import type {
  InventoryAdjustPayload,
  ProductDetail,
  ProductPayload,
  ProductSummary,
  RentalPlan,
  RentalPlanPayload,
  RentalSku,
  SkuPayload
} from '../utils/productApi';
import {
  activateRentalPlan,
  adjustSkuInventory,
  createRentalPlan,
  createSku,
  createVendorProduct,
  getVendorProduct,
  listVendorProducts,
  submitVendorProduct,
  updateSku,
  deactivateRentalPlan
} from '../utils/productApi';

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

const detail = reactive({
  open: false,
  loading: false,
  product: null as ProductDetail | null
});

const planModal = reactive({
  open: false,
  submitting: false,
  form: {
    planType: 'STANDARD' as RentalPlanPayload['planType'],
    termMonths: 12,
    depositAmount: 0,
    rentAmountMonthly: 0,
    buyoutPrice: null as number | null,
    allowExtend: false,
    extensionUnit: 'MONTH',
    extensionPrice: null as number | null
  }
});

const skuModal = reactive({
  open: false,
  submitting: false,
  editing: false,
  planId: '' as string,
  skuId: '' as string,
  form: {
    skuCode: '',
    attributesText: '',
    stockTotal: 0,
    stockAvailable: null as number | null,
    status: 'ACTIVE' as SkuPayload['status']
  }
});

const inventoryModal = reactive({
  open: false,
  submitting: false,
  planId: '' as string,
  skuId: '' as string,
  form: {
    changeType: 'INBOUND' as InventoryAdjustPayload['changeType'],
    quantity: 1
  }
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
    message.error(error?.response?.data?.message ?? '提交审核失败');
  }
};

const viewDetail = (productId: string) => {
  detail.open = true;
  fetchProductDetail(productId);
};

const fetchProductDetail = async (productId: string) => {
  try {
    detail.loading = true;
    detail.product = await getVendorProduct(vendorId(), productId);
  } catch (error) {
    console.error('加载商品详情失败', error);
    message.error('加载详情失败，请稍后重试');
    detail.product = null;
  } finally {
    detail.loading = false;
  }
};

const openPlanModal = () => {
  if (!detail.product) {
    message.warning('请先选择商品');
    return;
  }
  planModal.form = {
    planType: 'STANDARD',
    termMonths: 12,
    depositAmount: 0,
    rentAmountMonthly: 0,
    buyoutPrice: null,
    allowExtend: false,
    extensionUnit: 'MONTH',
    extensionPrice: null
  };
  planModal.open = true;
};

const handleCreatePlan = async () => {
  if (!detail.product) {
    return;
  }
  try {
    planModal.submitting = true;
    await createRentalPlan(vendorId(), detail.product.id, {
      ...planModal.form,
      buyoutPrice: planModal.form.buyoutPrice ?? null,
      extensionPrice: planModal.form.allowExtend ? planModal.form.extensionPrice ?? null : null,
      extensionUnit: planModal.form.allowExtend ? planModal.form.extensionUnit ?? null : null
    });
    message.success('租赁方案已创建');
    planModal.open = false;
    fetchProductDetail(detail.product.id);
  } catch (error: any) {
    console.error('创建租赁方案失败', error);
    message.error(error?.response?.data?.message ?? '创建失败，请稍后重试');
  } finally {
    planModal.submitting = false;
  }
};

const openSkuModal = (planId: string, sku?: RentalSku) => {
  if (!detail.product) {
    return;
  }
  skuModal.planId = planId;
  skuModal.open = true;
  if (sku) {
    skuModal.editing = true;
    skuModal.skuId = sku.id;
    skuModal.form = {
      skuCode: sku.skuCode,
      attributesText: JSON.stringify(sku.attributes ?? {}, null, 2),
      stockTotal: sku.stockTotal,
      stockAvailable: sku.stockAvailable,
      status: sku.status
    };
  } else {
    skuModal.editing = false;
    skuModal.skuId = '';
    skuModal.form = {
      skuCode: '',
      attributesText: '',
      stockTotal: 0,
      stockAvailable: null,
      status: 'ACTIVE'
    };
  }
};

const parseAttributes = (raw: string) => {
  if (!raw || !raw.trim()) {
    return undefined;
  }
  try {
    return JSON.parse(raw);
  } catch (error) {
    throw new Error('SKU 属性需为合法 JSON');
  }
};

const handleSaveSku = async () => {
  if (!detail.product) {
    return;
  }
  if (!skuModal.form.skuCode) {
    message.warning('请填写 SKU 编码');
    return;
  }
  try {
    skuModal.submitting = true;
    const payload: SkuPayload = {
      skuCode: skuModal.form.skuCode,
      stockTotal: skuModal.form.stockTotal,
      stockAvailable: skuModal.form.stockAvailable ?? undefined,
      status: skuModal.form.status,
      attributes: parseAttributes(skuModal.form.attributesText)
    };
    if (skuModal.editing) {
      await updateSku(vendorId(), detail.product.id, skuModal.planId, skuModal.skuId, payload);
      message.success('SKU 已更新');
    } else {
      await createSku(vendorId(), detail.product.id, skuModal.planId, payload);
      message.success('SKU 已创建');
    }
    skuModal.open = false;
    fetchProductDetail(detail.product.id);
  } catch (error: any) {
    console.error('保存 SKU 失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '保存失败，请稍后重试');
  } finally {
    skuModal.submitting = false;
  }
};

const openInventoryModal = (planId: string, sku: RentalSku) => {
  if (!detail.product) {
    return;
  }
  inventoryModal.planId = planId;
  inventoryModal.skuId = sku.id;
  inventoryModal.form = {
    changeType: 'INBOUND',
    quantity: 1
  };
  inventoryModal.open = true;
};

const handleAdjustInventory = async () => {
  if (!detail.product) {
    return;
  }
  try {
    inventoryModal.submitting = true;
    await adjustSkuInventory(vendorId(), detail.product.id, inventoryModal.planId, inventoryModal.skuId, {
      ...inventoryModal.form
    });
    message.success('库存已调整');
    inventoryModal.open = false;
    fetchProductDetail(detail.product.id);
  } catch (error: any) {
    console.error('库存调整失败', error);
    message.error(error?.response?.data?.message ?? '库存调整失败');
  } finally {
    inventoryModal.submitting = false;
  }
};

const togglePlanStatus = async (plan: RentalPlan) => {
  if (!detail.product) {
    return;
  }
  try {
    if (plan.status === 'ACTIVE') {
      await deactivateRentalPlan(vendorId(), detail.product.id, plan.id);
      message.success('方案已停用');
    } else {
      await activateRentalPlan(vendorId(), detail.product.id, plan.id);
      message.success('方案已启用');
    }
    fetchProductDetail(detail.product.id);
  } catch (error: any) {
    console.error('更新方案状态失败', error);
    message.error(error?.response?.data?.message ?? '操作失败，请稍后重试');
  }
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

const planHeader = (plan: RentalPlan) => {
  const status = plan.status === 'ACTIVE' ? '已启用' : plan.status === 'INACTIVE' ? '已停用' : '草稿';
  return `${plan.planType} · ${plan.termMonths}个月 · ${status}`;
};

const formatCurrency = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '0.00';
  }
  return Number(value).toFixed(2);
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

.detail__section {
  margin-bottom: 16px;
}

.detail__meta {
  color: #4b5563;
  margin: 4px 0;
}

.detail__desc {
  margin-top: 8px;
  white-space: pre-wrap;
}

.detail__empty {
  color: #9ca3af;
}

.detail__section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.plan__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
  color: #6b7280;
}

.plan__actions {
  margin-bottom: 12px;
}

.sku-table {
  margin-bottom: 8px;
}
</style>
