<template>
  <a-drawer
    :open="open"
    :width="drawerWidth"
    :height="drawerHeight"
    :placement="drawerPlacement"
    :destroyOnClose="true"
    :title="product?.name ?? '商品详情'"
    @close="emit('close')"
  >
    <a-spin :spinning="Boolean(loading)">
      <template v-if="product">
        <a-tabs v-model:activeKey="activeTab">
          <a-tab-pane key="basic" tab="基础资料">
            <a-form layout="vertical">
              <a-form-item label="商品名称" required>
                <a-input v-model:value="basicForm.name" placeholder="请输入商品名称" />
              </a-form-item>
              <a-form-item label="分类编码" required>
                <a-select v-model:value="basicForm.categoryCode" placeholder="请选择分类">
                  <a-select-option
                    v-for="option in categoryOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}（{{ option.value }}）
                  </a-select-option>
                </a-select>
              </a-form-item>
              <a-form-item label="封面图 URL">
                <a-input v-model:value="basicForm.coverImageUrl" placeholder="https://example" />
              </a-form-item>
              <a-form-item label="描述">
                <a-textarea v-model:value="basicForm.description" :rows="4" />
              </a-form-item>
            </a-form>
            <div class="drawer-actions">
              <a-space>
                <a-button type="primary" :loading="saving" @click="handleSaveBasic">保存</a-button>
                <a-button
                  :disabled="!canSubmit"
                  :loading="actionLoading"
                  @click="handleSubmitReview"
                >
                  提交审核
                </a-button>
                <a-button
                  :disabled="!canPublish"
                  :loading="actionLoading"
                  @click="handleTogglePublish"
                >
                  {{ publishLabel }}
                </a-button>
              </a-space>
            </div>
          </a-tab-pane>
          <a-tab-pane key="plans" tab="方案 & SKU">
            <div class="plan-toolbar">
              <div>
                共 {{ product.rentalPlans?.length ?? 0 }} 个方案
              </div>
              <a-button size="small" type="dashed" @click="planModal.open = true">新增方案</a-button>
            </div>
            <div v-if="!product.rentalPlans?.length" class="plan-empty">
              暂无方案，请先新增方案再配置 SKU。
            </div>
            <div class="plan-grid" v-else>
              <div v-for="plan in product.rentalPlans" :key="plan.id" class="plan-card">
                <div class="plan-card__header">
                  <div>
                    <strong>{{ planTitle(plan.planType) }}</strong>
                    <p>{{ plan.termMonths }} 个月 · 押金 ¥{{ formatCurrency(plan.depositAmount) }}</p>
                  </div>
                  <a-tag :color="plan.status === 'ACTIVE' ? 'success' : 'warning'">{{ plan.status }}</a-tag>
                </div>
                <p class="plan-card__price">月租金 ¥{{ formatCurrency(plan.rentAmountMonthly) }}</p>
                <div class="plan-card__actions">
                  <a-button size="small" @click="openSkuModal(plan.id)">新增 SKU</a-button>
                  <a-button size="small" type="text" @click="togglePlan(plan)">
                    {{ plan.status === 'ACTIVE' ? '停用' : '启用' }}
                  </a-button>
                </div>
                <a-table
                  :data-source="plan.skus"
                  :pagination="false"
                  size="small"
                  row-key="id"
                >
                  <a-table-column title="SKU" data-index="skuCode" key="sku" />
                  <a-table-column title="库存" key="inventory">
                    <template #default="{ record }">
                      {{ record.stockAvailable }}/{{ record.stockTotal }}
                    </template>
                  </a-table-column>
                  <a-table-column title="状态" data-index="status" key="status" />
                  <a-table-column title="操作" key="actions">
                    <template #default="{ record }">
                      <a-space size="small">
                        <a-button size="small" @click="openSkuModal(plan.id, record)">编辑</a-button>
                        <a-button size="small" @click="openInventoryModal(plan.id, record)">
                          调整库存
                        </a-button>
                      </a-space>
                    </template>
                  </a-table-column>
                </a-table>
              </div>
            </div>
          </a-tab-pane>
        </a-tabs>
      </template>
      <template v-else>
        <DataStateBlock title="请选择商品" description="在左侧商品卡片中选择需要配置的条目" />
      </template>
    </a-spin>

    <a-modal
      v-model:open="planModal.open"
      title="新增租赁方案"
      :confirm-loading="planModal.loading"
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
            <a-form-item label="押金 (¥)" required>
              <a-input-number v-model:value="planModal.form.depositAmount" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="月租金 (¥)" required>
              <a-input-number v-model:value="planModal.form.rentAmountMonthly" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="买断价 (¥)">
              <a-input-number v-model:value="planModal.form.buyoutPrice" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="允许续租">
          <a-switch v-model:checked="planModal.form.allowExtend" />
        </a-form-item>
        <template v-if="planModal.form.allowExtend">
          <a-row :gutter="12">
            <a-col :span="12">
              <a-form-item label="续租单位">
                <a-input v-model:value="planModal.form.extensionUnit" placeholder="如 MONTH" />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item label="续租价格 (¥)">
                <a-input-number v-model:value="planModal.form.extensionPrice" :min="0" style="width: 100%" />
              </a-form-item>
            </a-col>
          </a-row>
        </template>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="skuModal.open"
      :title="skuModal.editing ? '编辑 SKU' : '新增 SKU'"
      :confirm-loading="skuModal.loading"
      @ok="handleSaveSku"
      destroy-on-close
    >
      <a-form layout="vertical">
        <a-form-item label="SKU 编码" required>
          <a-input v-model:value="skuModal.form.skuCode" :disabled="skuModal.editing" />
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

    <a-modal
      v-model:open="inventoryModal.open"
      title="调整库存"
      :confirm-loading="inventoryModal.loading"
      @ok="handleAdjustInventory"
      destroy-on-close
    >
      <a-form layout="vertical">
        <a-form-item label="类型" required>
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
        <a-form-item label="参考单号">
          <a-input v-model:value="inventoryModal.form.referenceId" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-drawer>
</template>

<script lang="ts" setup>
// 商品详情抽屉：编辑商品基础信息、媒体与方案/SKU，并支持提交审核。
import { computed, reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import DataStateBlock from '../../../components/feedback/DataStateBlock.vue';
import { useViewport } from '../../../composables/useViewport';
import {
  updateVendorProduct,
  submitVendorProduct,
  toggleProductShelf,
  createRentalPlan,
  activateRentalPlan,
  deactivateRentalPlan,
  createSku,
  updateSku,
  adjustInventory,
  type ProductDetail,
  type RentalPlan,
  type RentalSku
} from '../../../services/productService';
import { friendlyErrorMessage } from '../../../utils/error';
import { formatCurrency } from '../../../utils/number';
import { PRODUCT_CATEGORY_OPTIONS, ensureCategoryOption } from '../../../utils/productCategories';

const props = defineProps<{
  open: boolean;
  vendorId: string | null;
  product: ProductDetail | null;
  loading?: boolean;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'refresh'): void;
  (e: 'refresh-list'): void;
}>();

const { width: viewportWidth, isMobile } = useViewport();
const drawerWidth = computed(() => {
  if (isMobile.value) {
    return '100%';
  }
  const margin = 64;
  const desired = Math.min(960, viewportWidth.value - margin);
  return Math.max(640, desired);
});
const drawerPlacement = computed(() => (isMobile.value ? 'bottom' : 'right'));
const drawerHeight = computed(() => (isMobile.value ? '100%' : undefined));

const activeTab = ref('basic');
const saving = ref(false);
const actionLoading = ref(false);

const basicForm = reactive({
  name: '',
  categoryCode: '',
  coverImageUrl: '',
  description: ''
});

const categoryOptions = computed(() =>
  ensureCategoryOption(
    PRODUCT_CATEGORY_OPTIONS.map((item) => ({ label: item.label, value: item.value })),
    basicForm.categoryCode
  )
);

watch(
  () => props.product,
  (product) => {
    if (!product) {
      basicForm.name = '';
      basicForm.categoryCode = '';
      basicForm.coverImageUrl = '';
      basicForm.description = '';
      return;
    }
    basicForm.name = product.name;
    basicForm.categoryCode = product.categoryCode;
    basicForm.coverImageUrl = product.coverImageUrl ?? '';
    basicForm.description = product.description ?? '';
  },
  { immediate: true }
);

const canSubmit = computed(
  () =>
    Boolean(
      props.product && ['DRAFT', 'REJECTED'].includes(props.product.status)
    )
);
const canPublish = computed(() =>
  Boolean(props.product && ['ACTIVE', 'INACTIVE'].includes(props.product.status))
);
const publishLabel = computed(() => (props.product?.status === 'ACTIVE' ? '下线' : '上线'));

const ensureContext = () => {
  if (!props.vendorId || !props.product) {
    message.warning('请确保已选择商品');
    return false;
  }
  return true;
};

const handleSaveBasic = async () => {
  if (!ensureContext()) {
    return;
  }
  saving.value = true;
  try {
    await updateVendorProduct(props.vendorId!, props.product!.id, {
      name: basicForm.name,
      categoryCode: basicForm.categoryCode,
      coverImageUrl: basicForm.coverImageUrl || undefined,
      description: basicForm.description || undefined
    });
    message.success('已保存商品资料');
    emit('refresh');
    emit('refresh-list');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '保存失败'));
  } finally {
    saving.value = false;
  }
};

const handleSubmitReview = async () => {
  if (!ensureContext() || !canSubmit.value) {
    return;
  }
  actionLoading.value = true;
  try {
    await submitVendorProduct(props.vendorId!, props.product!.id);
    message.success('已提交审核');
    emit('refresh');
    emit('refresh-list');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '提交失败'));
  } finally {
    actionLoading.value = false;
  }
};

const handleTogglePublish = async () => {
  if (!ensureContext() || !canPublish.value) {
    return;
  }
  actionLoading.value = true;
  try {
    const publish = props.product!.status !== 'ACTIVE';
    await toggleProductShelf(props.vendorId!, props.product!.id, publish);
    message.success(publish ? '已上线商品' : '已下线商品');
    emit('refresh');
    emit('refresh-list');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '操作失败'));
  } finally {
    actionLoading.value = false;
  }
};

const planModal = reactive({
  open: false,
  loading: false,
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

const resetPlanForm = () => {
  planModal.form.planType = 'STANDARD';
  planModal.form.termMonths = 12;
  planModal.form.depositAmount = 0;
  planModal.form.rentAmountMonthly = 0;
  planModal.form.buyoutPrice = undefined;
  planModal.form.allowExtend = false;
  planModal.form.extensionUnit = '';
  planModal.form.extensionPrice = undefined;
};

const handleCreatePlan = async () => {
  if (!ensureContext()) {
    return;
  }
  planModal.loading = true;
  try {
    await createRentalPlan(props.vendorId!, props.product!.id, {
      planType: planModal.form.planType as any,
      termMonths: planModal.form.termMonths,
      depositAmount: planModal.form.depositAmount,
      rentAmountMonthly: planModal.form.rentAmountMonthly,
      buyoutPrice: planModal.form.buyoutPrice,
      allowExtend: planModal.form.allowExtend,
      extensionUnit: planModal.form.allowExtend ? planModal.form.extensionUnit : undefined,
      extensionPrice: planModal.form.allowExtend ? planModal.form.extensionPrice : undefined
    });
    message.success('已新增方案');
    planModal.open = false;
    resetPlanForm();
    emit('refresh');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '创建方案失败'));
  } finally {
    planModal.loading = false;
  }
};

const togglePlan = async (plan: RentalPlan) => {
  if (!ensureContext()) {
    return;
  }
  actionLoading.value = true;
  try {
    if (plan.status === 'ACTIVE') {
      await deactivateRentalPlan(props.vendorId!, props.product!.id, plan.id);
      message.success('方案已停用');
    } else {
      await activateRentalPlan(props.vendorId!, props.product!.id, plan.id);
      message.success('方案已启用');
    }
    emit('refresh');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '切换方案状态失败'));
  } finally {
    actionLoading.value = false;
  }
};

const skuModal = reactive({
  open: false,
  loading: false,
  planId: '',
  editing: false,
  skuId: '',
  form: {
    skuCode: '',
    stockTotal: 0,
    stockAvailable: undefined as number | undefined,
    status: 'ACTIVE'
  }
});

const openSkuModal = (planId: string, sku?: RentalSku) => {
  skuModal.planId = planId;
  skuModal.open = true;
  skuModal.editing = Boolean(sku);
  if (sku) {
    skuModal.skuId = sku.id;
    skuModal.form.skuCode = sku.skuCode;
    skuModal.form.stockTotal = sku.stockTotal;
    skuModal.form.stockAvailable = sku.stockAvailable;
    skuModal.form.status = sku.status;
  } else {
    skuModal.skuId = '';
    skuModal.form.skuCode = '';
    skuModal.form.stockTotal = 0;
    skuModal.form.stockAvailable = undefined;
    skuModal.form.status = 'ACTIVE';
  }
};

const handleSaveSku = async () => {
  if (!ensureContext() || !skuModal.planId) {
    return;
  }
  skuModal.loading = true;
  try {
    if (skuModal.editing) {
      await updateSku(props.vendorId!, props.product!.id, skuModal.planId, skuModal.skuId, {
        skuCode: skuModal.form.skuCode,
        stockTotal: skuModal.form.stockTotal,
        stockAvailable: skuModal.form.stockAvailable,
        status: skuModal.form.status as any
      });
      message.success('已更新 SKU');
    } else {
      await createSku(props.vendorId!, props.product!.id, skuModal.planId, {
        skuCode: skuModal.form.skuCode,
        stockTotal: skuModal.form.stockTotal,
        stockAvailable: skuModal.form.stockAvailable,
        status: skuModal.form.status as any
      });
      message.success('已新增 SKU');
    }
    skuModal.open = false;
    emit('refresh');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '保存 SKU 失败'));
  } finally {
    skuModal.loading = false;
  }
};

const inventoryModal = reactive({
  open: false,
  loading: false,
  planId: '',
  skuId: '',
  form: {
    changeType: 'INBOUND',
    quantity: 1,
    referenceId: ''
  }
});

const openInventoryModal = (planId: string, sku: RentalSku) => {
  inventoryModal.planId = planId;
  inventoryModal.skuId = sku.id;
  inventoryModal.form.changeType = 'INBOUND';
  inventoryModal.form.quantity = 1;
  inventoryModal.form.referenceId = '';
  inventoryModal.open = true;
};

const handleAdjustInventory = async () => {
  if (!ensureContext() || !inventoryModal.planId || !inventoryModal.skuId) {
    return;
  }
  inventoryModal.loading = true;
  try {
    await adjustInventory(
      props.vendorId!,
      props.product!.id,
      inventoryModal.planId,
      inventoryModal.skuId,
      {
        changeType: inventoryModal.form.changeType as any,
        quantity: inventoryModal.form.quantity,
        referenceId: inventoryModal.form.referenceId || undefined
      }
    );
    message.success('库存已更新');
    inventoryModal.open = false;
    emit('refresh');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '调整库存失败'));
  } finally {
    inventoryModal.loading = false;
  }
};

const planTitle = (type: string) => {
  switch (type) {
    case 'RENT_TO_OWN':
      return '先租后买';
    case 'LEASE_TO_SALE':
      return '租售结合';
    default:
      return '标准方案';
  }
};
</script>

<style scoped>
.drawer-actions {
  margin-top: var(--space-4);
  display: flex;
  justify-content: flex-end;
}

.plan-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
}

.plan-empty {
  padding: var(--space-4);
  border: 1px dashed rgba(148, 163, 184, 0.5);
  border-radius: var(--radius-card);
  text-align: center;
  color: var(--color-text-secondary);
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--space-4);
}

.plan-card {
  border: 1px solid rgba(148, 163, 184, 0.4);
  border-radius: var(--radius-card);
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.plan-card__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--space-3);
}

.plan-card__header p {
  margin: var(--space-1) 0 0;
  color: var(--color-text-secondary);
}

.plan-card__price {
  margin: 0;
  font-size: var(--font-size-title-md);
  font-weight: 600;
}

.plan-card__actions {
  display: flex;
  gap: var(--space-2);
}
</style>
