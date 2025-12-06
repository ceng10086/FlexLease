<template>
  <form class="proof-uploader" @submit.prevent="handleUpload">
    <a-radio-group v-model:value="form.proofType">
      <a-radio-button value="SHIPMENT">发货</a-radio-button>
      <a-radio-button value="RECEIVE">收货</a-radio-button>
      <a-radio-button value="RETURN">退租</a-radio-button>
      <a-radio-button value="INSPECTION">巡检</a-radio-button>
      <a-radio-button value="OTHER">其他</a-radio-button>
    </a-radio-group>
    <a-input v-model:value="form.description" placeholder="补充说明（选填）" />
    <input ref="fileInput" type="file" @change="handleFileChange" />
    <a-button type="primary" :loading="uploading" :disabled="!file" @click="handleUpload">
      上传凭证
    </a-button>
  </form>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';
import type { OrderProofType } from '../../services/orderService';

const emit = defineEmits<{
  (e: 'upload', payload: { proofType: OrderProofType; description?: string; file: File }): void;
}>();

const form = reactive<{ proofType: OrderProofType; description?: string }>({
  proofType: 'SHIPMENT'
});
const file = ref<File | null>(null);
const uploading = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  file.value = target.files?.[0] ?? null;
};

const handleUpload = () => {
  if (!file.value) {
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
</style>
