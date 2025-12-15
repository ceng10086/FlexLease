<template>
  <form class="proof-uploader" @submit.prevent="processQueue">
    <a-alert
      v-if="isDisabled && disabledMessage"
      type="info"
      show-icon
      :message="disabledMessage"
      class="proof-uploader__alert"
    />
    <div class="proof-uploader__form">
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
    </div>
    <div
      class="proof-uploader__picker"
      :class="{ 'proof-uploader__picker--dragging': isDragging }"
      @dragenter.prevent="handleDragEnter"
      @dragover.prevent="handleDragOver"
      @dragleave.prevent="handleDragLeave"
      @drop.prevent="handleDrop"
    >
      <div class="picker-copy">
        <strong>选择需要上传的图片 / 视频</strong>
        <span>支持批量选择，系统会在后台自动打水印。</span>
      </div>
      <a-button type="dashed" html-type="button" :disabled="isDisabled" @click="triggerFileSelect">
        添加文件
      </a-button>
      <input
        ref="fileInput"
        type="file"
        multiple
        class="proof-uploader__file-input"
        :disabled="isDisabled"
        accept="image/*,video/*"
        @change="handleFileChange"
      />
    </div>
    <div v-if="queue.length" class="proof-uploader__queue">
      <div v-for="item in queue" :key="item.id" class="upload-item">
        <div class="upload-item__meta">
          <div>
            <strong>{{ item.name }}</strong>
            <p>{{ formatSize(item.size) }}</p>
          </div>
          <a-tag :color="statusColor(item.status)">{{ statusLabel(item.status) }}</a-tag>
        </div>
        <a-progress
          :percent="item.progress"
          size="small"
          :status="item.status === 'error' ? 'exception' : item.status === 'success' ? 'success' : 'active'"
        />
        <p v-if="item.error" class="upload-item__error">{{ item.error }}</p>
        <div class="upload-item__actions">
          <a-button
            v-if="item.status === 'pending'"
            type="link"
            size="small"
            :disabled="processing"
            @click="removeFromQueue(item.id)"
          >
            移除
          </a-button>
          <a-button
            v-else-if="item.status === 'error'"
            type="link"
            size="small"
            :loading="processing"
            @click="retryUpload(item.id)"
          >
            重试
          </a-button>
        </div>
      </div>
    </div>
    <div class="proof-uploader__actions">
      <a-button v-if="queue.length" size="small" :disabled="processing" @click="clearQueue">
        清空列表
      </a-button>
      <a-button type="primary" html-type="submit" :loading="processing" :disabled="isDisabled || !canUpload">
        上传全部
      </a-button>
    </div>
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

type UploadPayload = { proofType: OrderProofType; description?: string; file: File };
type UploadStatus = 'pending' | 'uploading' | 'success' | 'error';
type UploadQueueItem = {
  id: number;
  file: File;
  name: string;
  size: number;
  status: UploadStatus;
  progress: number;
  error: string | null;
};

const props = defineProps<{
  allowedTypes?: OrderProofType[];
  disabled?: boolean;
  disabledReason?: string | null;
  uploadHandler: (payload: UploadPayload) => Promise<void> | void;
}>();

const resolvedTypes = computed<OrderProofType[]>(() =>
  props.allowedTypes && props.allowedTypes.length ? props.allowedTypes : DEFAULT_TYPES
);
const isDisabled = computed(() => props.disabled ?? false);
const disabledMessage = computed(() => (isDisabled.value ? props.disabledReason ?? null : null));

const form = reactive<{ proofType: OrderProofType; description?: string }>({
  proofType: resolvedTypes.value[0] ?? 'OTHER'
});

const queue = ref<UploadQueueItem[]>([]);
const processing = ref(false);
const fileInput = ref<HTMLInputElement | null>(null);
const isDragging = ref(false);
let fileSeed = 0;

const proofTypeOptions = computed(() =>
  resolvedTypes.value.map((type) => ({
    value: type,
    label: PROOF_TYPE_LABELS[type] ?? type
  }))
);

const canUpload = computed(() =>
  queue.value.some((item) => item.status === 'pending' || item.status === 'error')
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

const formatSize = (size: number) => {
  if (size >= 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }
  if (size >= 1024) {
    return `${(size / 1024).toFixed(1)} KB`;
  }
  return `${size} B`;
};

const statusLabel = (status: UploadStatus) => {
  switch (status) {
    case 'uploading':
      return '上传中';
    case 'success':
      return '已完成';
    case 'error':
      return '失败';
    default:
      return '待上传';
  }
};

const statusColor = (status: UploadStatus) => {
  switch (status) {
    case 'success':
      return 'success';
    case 'error':
      return 'error';
    case 'uploading':
      return 'blue';
    default:
      return 'default';
  }
};

const triggerFileSelect = () => {
  if (isDisabled.value) {
    return;
  }
  const input = fileInput.value;
  if (!input) {
    return;
  }
  const showPicker = (input as unknown as { showPicker?: () => void }).showPicker;
  if (typeof showPicker === 'function') {
    showPicker.call(input);
    return;
  }
  input.click();
};

const appendFilesToQueue = (files: File[]) => {
  const filtered = files.filter((file) => file.size > 0);
  if (!filtered.length) {
    return;
  }
  const nextItems = filtered.map<UploadQueueItem>((file) => ({
    id: ++fileSeed,
    file,
    name: file.name,
    size: file.size,
    status: 'pending',
    progress: 0,
    error: null
  }));
  queue.value = [...queue.value, ...nextItems];
};

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const files = Array.from(target.files ?? []);
  if (!files.length) {
    return;
  }
  appendFilesToQueue(files);
  if (target) {
    target.value = '';
  }
};

const handleDragEnter = () => {
  if (isDisabled.value) {
    return;
  }
  isDragging.value = true;
};

const handleDragOver = () => {
  if (isDisabled.value) {
    return;
  }
  isDragging.value = true;
};

const handleDragLeave = () => {
  isDragging.value = false;
};

const handleDrop = (event: DragEvent) => {
  if (isDisabled.value) {
    return;
  }
  isDragging.value = false;
  const files = Array.from(event.dataTransfer?.files ?? []);
  appendFilesToQueue(files);
};

const removeFromQueue = (id: number) => {
  queue.value = queue.value.filter((item) => item.id !== id);
};

const clearQueue = () => {
  if (processing.value) {
    return;
  }
  queue.value = [];
};

const extractError = (error: unknown) => {
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string') {
    return error;
  }
  return '上传失败，请稍后再试';
};

const uploadItem = async (item: UploadQueueItem) => {
  item.status = 'uploading';
  item.progress = 15;
  item.error = null;
  try {
    await Promise.resolve(
      props.uploadHandler({
        proofType: form.proofType,
        description: form.description,
        file: item.file
      })
    );
    item.progress = 100;
    item.status = 'success';
  } catch (error) {
    item.progress = 0;
    item.status = 'error';
    item.error = extractError(error);
  }
};

const processQueue = async () => {
  if (isDisabled.value || !canUpload.value) {
    return;
  }
  processing.value = true;
  try {
    for (const item of queue.value) {
      if (item.status === 'pending' || item.status === 'error') {
        await uploadItem(item);
      }
    }
  } finally {
    processing.value = false;
  }
};

const retryUpload = async (id: number) => {
  if (processing.value) {
    return;
  }
  const target = queue.value.find((item) => item.id === id);
  if (!target) {
    return;
  }
  processing.value = true;
  try {
    await uploadItem(target);
  } finally {
    processing.value = false;
  }
};
</script>

<style scoped>
.proof-uploader {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  background: var(--color-surface, #fff);
  padding: var(--space-4);
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 16px;
}

.proof-uploader__alert {
  margin-bottom: var(--space-2);
}

.proof-uploader__form {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.proof-uploader__picker {
  border: 1px dashed var(--color-border, #e2e8f0);
  border-radius: 12px;
  padding: var(--space-3);
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-3);
  background: var(--color-surface-muted, #f7f7f5);
}

.proof-uploader__picker--dragging {
  border-color: var(--color-primary, #2563eb);
  background: rgba(37, 99, 235, 0.08);
}

.proof-uploader__file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.picker-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: var(--color-text-secondary, #64748b);
}

.proof-uploader__queue {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.upload-item {
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 12px;
  padding: var(--space-3);
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.08);
}

.upload-item__meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
}

.upload-item__meta p {
  margin: 4px 0 0;
  color: var(--color-text-secondary, #64748b);
}

.upload-item__actions {
  display: flex;
  justify-content: flex-end;
}

.upload-item__error {
  color: var(--color-danger, #f43f5e);
  margin: 4px 0;
}

.proof-uploader__actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
}

@media (max-width: 640px) {
  .proof-uploader {
    padding: var(--space-3);
  }

  .proof-uploader__picker {
    flex-direction: column;
    align-items: flex-start;
  }

  .proof-uploader__actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
