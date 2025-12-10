<template>
  <div class="proof-gallery">
    <div v-for="group in groupedProofs" :key="group.type" class="proof-group">
      <h4>{{ group.label }}</h4>
      <div class="proof-grid">
        <a-card
          v-for="proof in group.items"
          :key="proof.id"
          size="small"
          :hoverable="true"
          @click="$emit('preview', proof)"
        >
          <template #cover>
            <img
              v-if="isImage(proof)"
              :src="proof.fileUrl"
              alt="proof"
              class="proof-cover"
            />
            <video
              v-else-if="isVideo(proof)"
              :src="proof.fileUrl"
              class="proof-cover proof-cover--video"
              controls
              preload="metadata"
            />
            <div v-else class="proof-cover proof-cover--file">
              <strong>{{ fileBadge(proof) }}</strong>
              <span>{{ formatSize(proof.fileSize) }}</span>
            </div>
          </template>
          <a-card-meta
            :title="proof.description || fileBadge(proof)"
            :description="new Date(proof.uploadedAt).toLocaleString()"
          />
        </a-card>
      </div>
    </div>
    <p v-if="!proofs.length" class="text-muted">暂无凭证。</p>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import type { OrderProof } from '../../services/orderService';

const props = defineProps<{
  proofs: OrderProof[];
}>();

defineEmits<{
  (e: 'preview', proof: OrderProof): void;
}>();

const TYPE_LABELS: Record<OrderProof['proofType'], string> = {
  SHIPMENT: '发货凭证',
  RECEIVE: '收货凭证',
  RETURN: '退租凭证',
  INSPECTION: '巡检',
  OTHER: '其他'
};

const groupedProofs = computed(() => {
  const map = new Map<OrderProof['proofType'], OrderProof[]>();
  props.proofs.forEach((proof) => {
    const bucket = map.get(proof.proofType) ?? [];
    bucket.push(proof);
    map.set(proof.proofType, bucket);
  });
  return Array.from(map.entries()).map(([type, items]) => ({
    type,
    label: TYPE_LABELS[type] ?? type,
    items
  }));
});

const isImage = (proof: OrderProof) => (proof.contentType ?? '').startsWith('image/');
const isVideo = (proof: OrderProof) => (proof.contentType ?? '').startsWith('video/');

const fileBadge = (proof: OrderProof) => {
  if (proof.contentType) {
    const [, subtype] = proof.contentType.split('/');
    if (subtype) {
      return subtype.toUpperCase();
    }
  }
  const match = proof.fileUrl.split('?')[0].split('.').pop();
  return match ? match.toUpperCase() : 'FILE';
};

const formatSize = (bytes: number) => {
  if (!bytes) {
    return '';
  }
  if (bytes < 1024) {
    return `${bytes}B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)}KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
};
</script>

<style scoped>
.proof-group {
  margin-bottom: var(--space-5);
}

.proof-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--space-3);
}

.proof-cover {
  width: 100%;
  height: 160px;
  object-fit: cover;
}

.proof-cover--video {
  background: #000;
}

.proof-cover--file {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--color-surface-muted);
  color: var(--color-text-secondary);
  gap: var(--space-1);
  font-size: 0.9rem;
}
</style>
