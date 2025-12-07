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
import { postOrderMessage } from '../../../services/orderService';
import { friendlyErrorMessage } from '../../../utils/error';
import { message } from 'ant-design-vue';

const { order: getOrder, updateOrder } = useOrderDetail();
const auth = useAuthStore();
const sending = ref(false);

const chatEvents = computed(() => {
  const target = getOrder()?.events ?? [];
  return target.filter((event) => {
    const type = event.eventType?.toUpperCase() ?? '';
    return type.includes('MESSAGE') || type === 'COMMUNICATION_NOTE';
  });
});

const handleSend = async (content: string) => {
  if (!auth.user || !getOrder()) {
    return;
  }
  sending.value = true;
  try {
    const updated = await postOrderMessage(getOrder()!.id, {
      actorId: auth.user.id,
      message: content
    });
    updateOrder(updated);
  } catch (error) {
    message.error(friendlyErrorMessage(error, '发送失败'));
  } finally {
    sending.value = false;
  }
};
</script>
