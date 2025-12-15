<template>
  <div class="proof-view">
    <PageSection title="凭证墙">
      <ProofGallery :proofs="order?.proofs ?? []" @preview="handlePreview" />
    </PageSection>
    <PageSection title="上传新凭证">
      <ProofUploader
        :allowed-types="consumerProofTypes"
        :disabled="!auth.user"
        :disabled-reason="!auth.user ? '请先登录后上传凭证，系统会将凭证与账号绑定。' : null"
        :upload-handler="handleUpload"
      />
    </PageSection>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import { useOrderDetail } from '../../../composables/useOrderDetail';
import PageSection from '../../../components/layout/PageSection.vue';
import ProofGallery from '../../../components/proof/ProofGallery.vue';
import ProofUploader from '../../../components/proof/ProofUploader.vue';
import { uploadOrderProof, type OrderProofType } from '../../../services/orderService';
import { openProofInNewTab } from '../../../services/proofService';
import { useAuthStore } from '../../../stores/auth';
import { message } from 'ant-design-vue';
import { friendlyErrorMessage } from '../../../utils/error';

const { order: getOrder, refresh } = useOrderDetail();
const order = computed(() => getOrder());
const auth = useAuthStore();
const consumerProofTypes: OrderProofType[] = ['RECEIVE', 'RETURN', 'INSPECTION', 'OTHER'];

const handlePreview = async (proof: { fileUrl: string }) => {
  try {
    await openProofInNewTab(proof.fileUrl);
  } catch (error) {
    message.error(friendlyErrorMessage(error, '无法打开预览'));
  }
};

const handleUpload = async (payload: { proofType: OrderProofType; description?: string; file: File }) => {
  if (!order.value) {
    throw new Error('订单不存在或尚未加载');
  }
  if (!auth.user) {
    throw new Error('请先登录后再上传凭证');
  }
  try {
    await uploadOrderProof(order.value!.id, {
      actorId: auth.user.id,
      proofType: payload.proofType,
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
