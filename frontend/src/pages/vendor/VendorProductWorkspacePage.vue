<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>商品与租赁方案</h2>
        <p class="page-header__meta">创建并维护租赁商品、定价方案与库存，提交审核上线。</p>
      </div>
      <a-button type="primary" @click="openCreateModal">新建商品</a-button>
    </div>

    <a-card>
      <a-table
        :data-source="products"
        :loading="loading"
        :pagination="false"
        row-key="id"
      >
        <a-table-column title="名称" data-index="name" key="name" />
        <a-table-column title="分类" data-index="categoryCode" key="category" />
        <a-table-column title="状态" key="status">
          <template #default="{ record }">
            <a-tag :color="statusColor(record.status)">{{ statusLabel(record.status) }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="创建时间" key="createdAt">
          <template #default="{ record }">{{ formatDate(record.createdAt) }}</template>
        </a-table-column>
        <a-table-column title="操作" key="actions">
          <template #default="{ record }">
            <a-space size="small">
              <a-button size="small" @click="openDetail(record.id)">查看</a-button>
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
        <a-form-item label="商品名称" required><a-input v-model:value="modal.form.name" placeholder="请输入商品名称" /></a-form-item>
        <a-form-item label="分类编码" required><a-input v-model:value="modal.form.categoryCode" placeholder="例如 OFFICE" /></a-form-item>
        <a-form-item label="封面图 URL"><a-input v-model:value="modal.form.coverImageUrl" placeholder="可选" /></a-form-item>
        <a-form-item label="商品描述"><a-textarea v-model:value="modal.form.description" :rows="4" placeholder="可选" /></a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detail.open" :width="860" title="商品详情" destroy-on-close>
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
            <p v-if="!detail.product.rentalPlans?.length" class="detail__empty">暂未配置租赁方案</p>
            <a-collapse v-else>
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
                  <span>状态：{{ plan.status }}</span>
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
                <a-table :data-source="plan.skus" :pagination="false" size="small" row-key="id" class="sku-table">
                  <a-table-column title="SKU 编码" data-index="skuCode" key="sku" />
                  <a-table-column title="库存" key="stock">
                    <template #default="{ record }">{{ record.stockAvailable }}/{{ record.stockTotal }}</template>
                  </a-table-column>
                  <a-table-column title="状态" data-index="status" key="status" />
                  <a-table-column title="操作" key="actions">
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

    <a-modal v-model:open="planModal.open" title="新增租赁方案" :confirm-loading="planModal.submitting" @ok="handleCreatePlan" destroy-on-close>
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
        <a-row v-if="planModal.form.allowExtend" :gutter="12">
          <a-col :span="12">
            <a-form-item label="续租单位"><a-input v-model:value="planModal.form.extensionUnit" placeholder="例如 MONTH" /></a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="续租价格"><a-input-number v-model:value="planModal.form.extensionPrice" :min="0" :step="50" style="width: 100%" /></a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>

    <a-modal v-model:open="skuModal.open" :title="skuModal.editing ? '编辑 SKU' : '新增 SKU'" :confirm-loading="skuModal.submitting" @ok="handleSaveSku" destroy-on-close>
      <a-form layout="vertical">
        <a-form-item label="SKU 编码" required>
          <a-input v-model:value="skuModal.form.skuCode" placeholder="请输入 SKU 编码" :disabled="skuModal.editing" />
        </a-form-item>
        <a-form-item label="总库存" required>
          <a-input-number v-model:value="skuModal.form.stockTotal" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="可用库存">
          <a-input-number v-model:value="skuModal.form.stockAvailable" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="skuModal.form.status">
            <a-select-option value="ACTIVE">启用</a-select-option>
            <a-select-option value="INACTIVE">停用</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal v-model:open="inventoryModal.open" title="调整库存" :confirm-loading="inventoryModal.submitting" @ok="handleAdjustInventory" destroy-on-close>
      <a-form layout="vertical">
        <a-form-item label="调整类型" required>
          <a-select v-model:value="inventoryModal.form.changeType">
            <a-select-option value="INBOUND">入库</a-select-option>
            <a-select-option value="OUTBOUND">出库</a-select-option>
            <a-select-option value="RESERVE">预占</a-select-option>
            <a-select-option value="RELEASE">释放预占</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="数量" required>
          <a-input-number v-model:value="inventoryModal.form.quantity" :min="1" style="width: 100%" />
        </a-form-item>
        <a-form-item label="参考单号">
          <a-input v-model:value="inventoryModal.form.referenceId" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../../stores/auth';
import {
  listVendorProducts,
  createVendorProduct,
  submitVendorProduct,
  fetchVendorProduct,
  createRentalPlan,
  activateRentalPlan,
  deactivateRentalPlan,
  createSku,
  updateSku,
  adjustInventory,
  type ProductSummary,
  type ProductDetail,
  type RentalPlan,
  type RentalSku,
  type ProductStatus
} from '../../services/productService';

const auth = useAuthStore();
const vendorId = () => {
  if (!auth.user?.id) {
    throw new Error('未获取到厂商账号信息');
  }
  return auth.user.id;
};

const loading = ref(false);
const products = ref<ProductSummary[]>([]);
const pagination = reactive({ page: 1, size: 10, total: 0 });

const modal = reactive({
  open: false,
  submitting: false,
  form: {
    name: '',
    categoryCode: '',
    description: '',
    coverImageUrl: ''
  }
});

const detail = reactive<{ open: boolean; loading: boolean; product: ProductDetail | null }>(
  {
    open: false,
    loading: false,
    product: null
  }
);

const planModal = reactive({
  open: false,
  submitting: false,
  form: {
    planType: 'STANDARD',
    termMonths: 12,
    depositAmount: 0,
    rentAmountMonthly: 0,
    buyoutPrice: undefined as number | undefined,
    allowExtend: false,
    extensionUnit: '',
    extensionPrice: undefined as number | undefined
  }
});

const skuModal = reactive({
  open: false,
  submitting: false,
  editing: false,
  planId: '',
  skuId: '',
  form: {
    skuCode: '',
    stockTotal: 0,
    stockAvailable: 0,
    status: 'ACTIVE'
  }
});

const inventoryModal = reactive({
  open: false,
  submitting: false,
  planId: '',
  skuId: '',
  form: {
    changeType: 'INBOUND',
    quantity: 1,
    referenceId: ''
  }
});

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

const formatDate = (value: string) => new Date(value).toLocaleString();
const formatCurrency = (value?: number | null) => (value ?? 0).toFixed(2);

const loadProducts = async () => {
  loading.value = true;
  try {
    const result = await listVendorProducts(vendorId(), { page: pagination.page, size: pagination.size });
    products.value = result.content;
    pagination.total = result.totalElements;
  } catch (error) {
    console.error('加载商品失败', error);
    message.error('加载商品失败，请稍后重试');
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
  modal.submitting = false;
  Object.assign(modal.form, { name: '', categoryCode: '', description: '', coverImageUrl: '' });
};

const handleCreate = async () => {
  if (!modal.form.name || !modal.form.categoryCode) {
    message.warning('请填写必填字段');
    return;
  }
  modal.submitting = true;
  try {
    await createVendorProduct(vendorId(), { ...modal.form });
    message.success('商品已创建');
    modal.open = false;
    loadProducts();
  } catch (error) {
    console.error('创建商品失败', error);
    message.error('创建失败，请稍后重试');
  } finally {
    modal.submitting = false;
  }
};

const openDetail = async (productId: string) => {
  detail.open = true;
  detail.loading = true;
  try {
    detail.product = await fetchVendorProduct(vendorId(), productId);
  } catch (error) {
    console.error('加载商品详情失败', error);
    message.error('加载详情失败');
    detail.product = null;
  } finally {
    detail.loading = false;
  }
};

const refreshDetail = async () => {
  if (!detail.product) {
    return;
  }
  await openDetail(detail.product.id);
};

const handleSubmit = async (productId: string) => {
  try {
    await submitVendorProduct(vendorId(), productId);
    message.success('商品已提交审核');
    loadProducts();
    if (detail.product?.id === productId) {
      refreshDetail();
    }
  } catch (error) {
    console.error('提交审核失败', error);
    message.error('提交审核失败，请稍后重试');
  }
};

const openPlanModal = () => {
  if (!detail.product) {
    message.warning('请先选择商品');
    return;
  }
  planModal.open = true;
  planModal.submitting = false;
  Object.assign(planModal.form, {
    planType: 'STANDARD',
    termMonths: 12,
    depositAmount: 0,
    rentAmountMonthly: 0,
    buyoutPrice: undefined,
    allowExtend: false,
    extensionUnit: '',
    extensionPrice: undefined
  });
};

const handleCreatePlan = async () => {
  if (!detail.product) {
    return;
  }
  planModal.submitting = true;
  try {
    await createRentalPlan(vendorId(), detail.product.id, { ...planModal.form });
    message.success('租赁方案已创建');
    planModal.open = false;
    refreshDetail();
  } catch (error) {
    console.error('创建租赁方案失败', error);
    message.error('创建方案失败，请稍后重试');
  } finally {
    planModal.submitting = false;
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
    refreshDetail();
  } catch (error) {
    console.error('切换方案状态失败', error);
    message.error('操作失败，请稍后重试');
  }
};

const openSkuModal = (planId: string, sku?: RentalSku) => {
  if (!detail.product) {
    return;
  }
  skuModal.open = true;
  skuModal.planId = planId;
  skuModal.skuId = sku?.id ?? '';
  skuModal.editing = Boolean(sku);
  skuModal.submitting = false;
  Object.assign(skuModal.form, {
    skuCode: sku?.skuCode ?? '',
    stockTotal: sku?.stockTotal ?? 0,
    stockAvailable: sku?.stockAvailable ?? 0,
    status: sku?.status ?? 'ACTIVE'
  });
};

const handleSaveSku = async () => {
  if (!detail.product) {
    return;
  }
  if (!skuModal.form.skuCode) {
    message.warning('请填写 SKU 编码');
    return;
  }
  skuModal.submitting = true;
  try {
    if (skuModal.editing) {
      await updateSku(vendorId(), detail.product.id, skuModal.planId, skuModal.skuId, {
        ...skuModal.form
      });
      message.success('SKU 已更新');
    } else {
      await createSku(vendorId(), detail.product.id, skuModal.planId, { ...skuModal.form });
      message.success('SKU 已创建');
    }
    skuModal.open = false;
    refreshDetail();
  } catch (error) {
    console.error('保存 SKU 失败', error);
    message.error('保存失败，请稍后重试');
  } finally {
    skuModal.submitting = false;
  }
};

const openInventoryModal = (planId: string, sku: RentalSku) => {
  inventoryModal.open = true;
  inventoryModal.planId = planId;
  inventoryModal.skuId = sku.id;
  inventoryModal.submitting = false;
  Object.assign(inventoryModal.form, {
    changeType: 'INBOUND',
    quantity: 1,
    referenceId: ''
  });
};

const handleAdjustInventory = async () => {
  if (!detail.product) {
    return;
  }
  inventoryModal.submitting = true;
  try {
    await adjustInventory(
      vendorId(),
      detail.product.id,
      inventoryModal.planId,
      inventoryModal.skuId,
      inventoryModal.form
    );
    message.success('库存已调整');
    inventoryModal.open = false;
    refreshDetail();
  } catch (error) {
    console.error('调整库存失败', error);
    message.error('库存调整失败，请稍后重试');
  } finally {
    inventoryModal.submitting = false;
  }
};

const planHeader = (plan: RentalPlan) => `${plan.planType} · ${plan.termMonths} 个月`;

loadProducts();
</script>

<style scoped>
.page__pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.detail__section {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.detail__section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.detail__meta {
  margin: 0;
  color: #64748b;
}

.detail__desc {
  margin: 0;
  color: #1f2937;
}

.detail__empty {
  color: #94a3b8;
  margin: 0;
}

.plan__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #475569;
}

.plan__actions {
  margin: 12px 0;
}

.sku-table {
  margin-top: 12px;
}
</style>
