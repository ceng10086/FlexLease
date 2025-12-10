<template>
  <form class="proof-uploader" @submit.prevent="handleUpload">
    <a-alert
      v-if="isDisabled && disabledMessage"
      type="info"
      show-icon
      :message="disabledMessage"
      class="proof-uploader__alert"
    />
    <a-radio-group v-model:value="form.proofType" :disabled="isDisabled">
      <a-radio-button v-for="option in proofTypeOptions" :key="option.value" :value="option.value">
        {{ option.label }}
      </a-radio-button>
    </a-radio-group>
    <a-input
      v-model:value="form.description"
      placeholder="补充说明（选填）"
      :disabled="isDisabled"
    />
    <input ref="fileInput" type="file" :disabled="isDisabled" @change="handleFileChange" />
    <a-button
      type="primary"
      :loading="uploading"
      :disabled="isDisabled || !file"
      @click="handleUpload"
    >
      上传凭证
    </a-button>
  </form>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue';
import type { OrderProofType } from '../../services/orderService';

const PROOF_TYPE_LABELS: Record<OrderProofType, string> = {
  SHIPMENT: '发货',
  RECEIVE: '收货',
  RETURN: '退租',
  INSPECTION: '巡检',
  OTHER: '其他'
};

const DEFAULT_TYPES: OrderProofType[] = ['SHIPMENT', 'RECEIVE', 'RETURN', 'INSPECTION', 'OTHER'];

const props = defineProps<{
  allowedTypes?: OrderProofType[];
  disabled?: boolean;
  disabledReason?: string | null;
}>();

const emit = defineEmits<{
  (e: 'upload', payload: { proofType: OrderProofType; description?: string; file: File }): void;
}>();

const resolvedTypes = computed<OrderProofType[]>(() =>
  props.allowedTypes && props.allowedTypes.length ? props.allowedTypes : DEFAULT_TYPES
);
const isDisabled = computed(() => props.disabled ?? false);
const disabledMessage = computed(() => (isDisabled.value ? props.disabledReason ?? null : null));

const form = reactive<{ proofType: OrderProofType; description?: string }>({
  proofType: resolvedTypes.value[0] ?? 'OTHER'
});
const file = ref<File | null>(null);
const uploading = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);

const proofTypeOptions = computed(() =>
  resolvedTypes.value.map((type) => ({
    value: type,
    label: PROOF_TYPE_LABELS[type] ?? type
  }))
);

watch(
  resolvedTypes,
  (types) => {
    if (!types.length) {
      return;
    }
    if (!types.includes(form.proofType)) {
      form.proofType = types[0];
    }
  },
  { immediate: true }
);

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  file.value = target.files?.[0] ?? null;
};

const handleUpload = () => {
  if (!file.value || isDisabled.value) {
    return;
  }
  uploading.value = true;
  emit('upload', {
    proofType: form.proofType,
    description: form.description,
    file: file.value
  });
  uploading.value = false;
  file.value = null;
  if (fileInput.value) {
    fileInput.value.value = '';
  }
};
</script>

<style scoped>
.proof-uploader {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.proof-uploader__alert {
  margin-bottom: var(--space-2);
}
</style>
