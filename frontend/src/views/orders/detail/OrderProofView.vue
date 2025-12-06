<template>
  <div class="proof-view">
    <PageSection title="凭证墙">
      <ProofGallery :proofs="order?.proofs ?? []" @preview="handlePreview" />
    </PageSection>
    <PageSection title="上传新凭证">
      <ProofUploader @upload="handleUpload" />
    </PageSection>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { useOrderDetail } from '../../../composables/useOrderDetail';
import PageSection from '../../../components/layout/PageSection.vue';
import ProofGallery from '../../../components/proof/ProofGallery.vue';
import ProofUploader from '../../../components/proof/ProofUploader.vue';
import { uploadOrderProof } from '../../../services/orderService';
import { useAuthStore } from '../../../stores/auth';
import { message } from 'ant-design-vue';
import { friendlyErrorMessage } from '../../../utils/error';

const { order: getOrder, refresh } = useOrderDetail();
const order = computed(() => getOrder());
const auth = useAuthStore();

const handlePreview = (proof: { fileUrl: string }) => {
  window.open(proof.fileUrl, '_blank');
};

const handleUpload = async (payload: { proofType: string; description?: string; file: File }) => {
  if (!order.value || !auth.user) {
    return;
  }
  try {
    await uploadOrderProof(order.value!.id, {
      actorId: auth.user.id,
      proofType: payload.proofType as any,
      description: payload.description,
      file: payload.file
    });
    message.success('已上传凭证');
    await refresh();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '上传失败'));
  }
};
</script>

<style scoped>
.proof-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
</style>
