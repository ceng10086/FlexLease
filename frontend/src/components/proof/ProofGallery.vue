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
            <img :src="proof.fileUrl" alt="proof" />
          </template>
          <a-card-meta :description="new Date(proof.uploadedAt).toLocaleString()" />
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

img {
  width: 100%;
  height: 140px;
  object-fit: cover;
}
</style>
