<template>
  <PageSection title="聊天">
    <OrderChatPanel :messages="chatEvents" :sending="sending" @send="handleSend" />
  </PageSection>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue';
import PageSection from '../../../components/layout/PageSection.vue';
import OrderChatPanel from '../../../components/chat/OrderChatPanel.vue';
import { useOrderDetail } from '../../../composables/useOrderDetail';
import { useAuthStore } from '../../../stores/auth';
import { postOrderMessage, uploadOrderProof } from '../../../services/orderService';
import { friendlyErrorMessage } from '../../../utils/error';
import { message } from 'ant-design-vue';
import type { ChatSendPayload } from '../../../types/chat';

const { order: getOrder, updateOrder, refresh } = useOrderDetail();
const auth = useAuthStore();
const sending = ref(false);

const chatEvents = computed(() => {
  const target = getOrder()?.events ?? [];
  return target.filter((event) => {
    const type = event.eventType?.toUpperCase() ?? '';
    return type.includes('MESSAGE') || type === 'COMMUNICATION_NOTE';
  });
});

const handleSend = async (payload: ChatSendPayload) => {
  if (!auth.user || !getOrder()) {
    return;
  }
  const trimmed = payload.content.trim();
  const hasAttachments = payload.attachments.length > 0;
  if (!trimmed && !hasAttachments) {
    return;
  }
  sending.value = true;
  try {
    const orderId = getOrder()!.id;
    const actorId = auth.user.id;
    let attachmentSummary = '';
    if (hasAttachments) {
      const uploads = await Promise.all(
        payload.attachments.map((file) =>
          uploadOrderProof(orderId, {
            actorId,
            proofType: 'OTHER',
            description: trimmed || file.name,
            file
          })
        )
      );
      await refresh();
      const attachmentLines = uploads.map((proof, index) => `• ${payload.attachments[index].name}: ${proof.fileUrl}`);
      attachmentSummary = [`已附加 ${uploads.length} 个文件`, ...attachmentLines].join('\n');
    }
    const finalMessage = [trimmed, attachmentSummary].filter((segment) => segment && segment.trim()).join('\n\n');
    if (!finalMessage) {
      sending.value = false;
      return;
    }
    const updated = await postOrderMessage(orderId, {
      actorId,
      message: finalMessage
    });
    updateOrder(updated);
    message.success('已发送');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '发送失败'));
  } finally {
    sending.value = false;
  }
};
</script>
