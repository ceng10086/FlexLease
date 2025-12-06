<template>
  <div class="chat-panel">
    <div class="chat-panel__messages" ref="messagesContainer">
      <div v-if="!messages.length" class="chat-panel__empty">暂无对话</div>
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="chat-bubble"
        :class="msg.actorRole === selfRole ? 'chat-bubble--self' : 'chat-bubble--peer'"
      >
        <div class="chat-bubble__body">{{ msg.description }}</div>
        <small>{{ new Date(msg.createdAt).toLocaleString() }}</small>
      </div>
    </div>
    <div class="chat-panel__composer">
      <a-textarea
        v-model:value="draft"
        auto-size
        placeholder="输入要发送的内容…"
        @pressEnter.prevent="handleSend"
      />
      <a-button type="primary" :loading="sending" :disabled="!draft" @click="handleSend">
        发送
      </a-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, watch, nextTick } from 'vue';
import type { OrderEvent } from '../../services/orderService';

const props = withDefaults(
  defineProps<{
    messages: OrderEvent[];
    sending?: boolean;
    selfRole?: string;
  }>(),
  {
    selfRole: 'USER'
  }
);

const emit = defineEmits<{
  (e: 'send', content: string): void;
}>();

const draft = ref('');
const messagesContainer = ref<HTMLDivElement | null>(null);

const handleSend = () => {
  if (!draft.value.trim()) {
    return;
  }
  emit('send', draft.value.trim());
  draft.value = '';
};

watch(
  () => props.messages.length,
  async () => {
    await nextTick();
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
    }
  }
);
</script>

<style scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-surface);
  height: 100%;
}

.chat-panel__messages {
  flex: 1;
  padding: var(--space-4);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.chat-panel__empty {
  text-align: center;
  color: var(--color-text-secondary);
}

.chat-bubble {
  max-width: 80%;
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.chat-bubble__body {
  padding: var(--space-3);
  border-radius: 16px;
}

.chat-bubble--self {
  align-self: flex-end;
}

.chat-bubble--self .chat-bubble__body {
  background: var(--color-primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-bubble--peer {
  align-self: flex-start;
}

.chat-bubble--peer .chat-bubble__body {
  background: var(--color-surface-muted);
  border-bottom-left-radius: 4px;
}

.chat-panel__composer {
  border-top: 1px solid var(--color-border);
  padding: var(--space-3);
  display: flex;
  gap: var(--space-2);
}
</style>
