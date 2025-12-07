<template>
  <div class="catalog-filter-bar">
    <div class="chip-row">
      <span class="chip-row__label">类目</span>
      <div class="chip-row__group">
        <button
          v-for="option in categoryOptions"
          :key="option.label"
          type="button"
          class="chip"
          :class="{ 'chip--active': isActive('categoryCode', option.value) }"
          @click="toggleValue('categoryCode', option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </div>
    <div class="chip-row">
      <span class="chip-row__label">租赁模式</span>
      <div class="chip-row__group">
        <button
          v-for="option in planTypeOptions"
          :key="option.label"
          type="button"
          class="chip"
          :class="{ 'chip--active': isActive('planType', option.value) }"
          @click="toggleValue('planType', option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </div>
    <a-button size="small" type="text" class="sheet-trigger" @click="sheetOpen = true">
      <template #icon>
        <FilterOutlined />
      </template>
      更多筛选
    </a-button>
  </div>

  <a-drawer
    placement="bottom"
    height="65vh"
    :open="sheetOpen"
    :closable="false"
    title="精细筛选"
    @close="sheetOpen = false"
  >
    <a-form layout="vertical">
      <a-form-item label="押金区间 (¥)">
        <div class="drawer-range">
          <a-input-number
            v-model:value="sheetForm.minDeposit"
            :min="0"
            :precision="0"
            placeholder="最低"
          />
          <span class="range-divider">~</span>
          <a-input-number
            v-model:value="sheetForm.maxDeposit"
            :min="0"
            :precision="0"
            placeholder="最高"
          />
        </div>
      </a-form-item>
      <a-form-item label="月租排序">
        <a-segmented
          v-model:value="sheetForm.rentSort"
          :options="rentSortOptions"
        />
      </a-form-item>
    </a-form>
    <div class="drawer-actions">
      <a-button @click="resetAdvanced">清空筛选</a-button>
      <a-button type="primary" @click="applyAdvanced">应用筛选</a-button>
    </div>
  </a-drawer>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue';
import { FilterOutlined } from '@ant-design/icons-vue';

type RentSort = 'RENT_ASC' | 'RENT_DESC' | null;

type CatalogFilterState = {
  keyword?: string;
  categoryCode?: string | null;
  planType?: string | null;
  depositRange?: [number | null, number | null];
  rentSort?: RentSort;
};

const DEFAULT_FILTERS: CatalogFilterState = {
  keyword: '',
  categoryCode: null,
  planType: null,
  depositRange: [null, null],
  rentSort: null
};

const props = defineProps<{ modelValue: CatalogFilterState }>();
const emit = defineEmits<{
  (e: 'update:modelValue', value: CatalogFilterState): void;
  (e: 'change', value: CatalogFilterState): void;
}>();

const sheetOpen = ref(false);

const mergedValue = computed<CatalogFilterState>(() => ({
  ...DEFAULT_FILTERS,
  ...(props.modelValue ?? {})
}));

const sheetForm = reactive<{ minDeposit: number | null; maxDeposit: number | null; rentSort: RentSort }>(
  {
    minDeposit: null,
    maxDeposit: null,
    rentSort: null
  }
);

watch(
  mergedValue,
  (value) => {
    sheetForm.minDeposit = value.depositRange?.[0] ?? null;
    sheetForm.maxDeposit = value.depositRange?.[1] ?? null;
    sheetForm.rentSort = value.rentSort ?? null;
  },
  { immediate: true }
);

const categoryOptions: Array<{ label: string; value: string | null }> = [
  { label: '全部', value: null },
  { label: '3C 数码', value: 'ELECTRONICS' },
  { label: '智能家电', value: 'HOME_APPLIANCE' },
  { label: '影音娱乐', value: 'ENTERTAINMENT' },
  { label: '户外出行', value: 'OUTDOOR' }
];

const planTypeOptions: Array<{ label: string; value: string | null }> = [
  { label: '全部', value: null },
  { label: '标准租赁', value: 'STANDARD' },
  { label: '先租后买', value: 'RENT_TO_OWN' },
  { label: '以租代售', value: 'LEASE_TO_SALE' }
];

const rentSortOptions = [
  { label: '默认', value: null },
  { label: '租金由低到高', value: 'RENT_ASC' },
  { label: '租金由高到低', value: 'RENT_DESC' }
];

const updateValue = (patch: Partial<CatalogFilterState>) => {
  const next = {
    ...mergedValue.value,
    ...patch
  };
  emit('update:modelValue', next);
  emit('change', next);
};

const isActive = (field: 'categoryCode' | 'planType', value: string | null) => {
  return (mergedValue.value[field] ?? null) === value;
};

const toggleValue = (field: 'categoryCode' | 'planType', value: string | null) => {
  const current = mergedValue.value[field] ?? null;
  updateValue({ [field]: current === value ? null : value } as Partial<CatalogFilterState>);
};

const applyAdvanced = () => {
  updateValue({
    depositRange: [sheetForm.minDeposit ?? null, sheetForm.maxDeposit ?? null],
    rentSort: sheetForm.rentSort ?? null
  });
  sheetOpen.value = false;
};

const resetAdvanced = () => {
  sheetForm.minDeposit = null;
  sheetForm.maxDeposit = null;
  sheetForm.rentSort = null;
  updateValue({ depositRange: [null, null], rentSort: null });
};
</script>

<style scoped>
.catalog-filter-bar {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-3);
  background: var(--color-surface-muted);
  border-radius: var(--radius-card);
}

.chip-row {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.chip-row__label {
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
}

.chip-row__group {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.chip {
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: 4px 14px;
  background: #fff;
  font-size: 13px;
  color: var(--color-text-secondary);
  transition: all 0.15s ease;
}

.chip--active {
  border-color: var(--color-primary);
  color: var(--color-primary);
  background: rgba(37, 99, 235, 0.08);
}

.sheet-trigger {
  align-self: flex-start;
  padding-left: 0;
  color: var(--color-primary);
}

.drawer-range {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.range-divider {
  color: var(--color-text-secondary);
}

.drawer-actions {
  margin-top: var(--space-4);
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}
</style>
