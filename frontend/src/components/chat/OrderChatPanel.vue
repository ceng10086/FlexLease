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
      <div class="composer-toolbar">
        <a-upload
          v-if="allowAttachments"
          :before-upload="handleAttachmentSelect"
          :multiple="true"
          :show-upload-list="false"
          accept="image/*,video/*,.pdf,.zip,.doc,.docx"
        >
          <a-button size="small" type="text" class="composer-toolbar__btn">
            <PaperClipOutlined />
            <span>附件</span>
          </a-button>
        </a-upload>
        <a-button
          v-if="quickPhraseList.length"
          size="small"
          type="text"
          class="composer-toolbar__btn"
          @click="togglePhrases"
        >
          <ThunderboltOutlined />
          <span>快捷短语</span>
        </a-button>
      </div>
      <div v-if="attachedFiles.length" class="attachment-list">
        <div v-for="file in attachedFiles" :key="file.id" class="attachment-chip">
          <div>
            <strong>{{ file.name }}</strong>
            <span class="attachment-chip__size">{{ formatSize(file.size) }}</span>
          </div>
          <a-button type="link" size="small" @click="removeAttachment(file.id)">移除</a-button>
        </div>
      </div>
      <div v-if="showPhrases && quickPhraseList.length" class="chat-panel__phrases">
        <button
          v-for="phrase in quickPhraseList"
          :key="phrase"
          type="button"
          class="phrase-chip"
          @click="insertPhrase(phrase)"
        >
          {{ phrase }}
        </button>
      </div>
      <div class="composer-input-row">
        <a-textarea
          v-model:value="draft"
          auto-size
          placeholder="输入要发送的内容…"
          :disabled="sending"
          @pressEnter="handlePressEnter"
        />
        <a-button type="primary" :loading="sending" :disabled="!canSend || sending" @click="handleSend">
          发送
        </a-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, nextTick, ref, watch } from 'vue';
import { PaperClipOutlined, ThunderboltOutlined } from '@ant-design/icons-vue';
import { message, type UploadProps } from 'ant-design-vue';
import type { OrderEvent } from '../../services/orderService';
import type { ChatSendPayload } from '../../types/chat';

type PendingAttachment = {
  id: string;
  file: File;
  name: string;
  size: number;
};

const defaultPhrases = [
  '已收到您的消息，我们会尽快处理。',
  '已安排发货，稍后同步物流信息。',
  '烦请按凭证指引补充资料，谢谢配合。'
];

const props = withDefaults(
  defineProps<{
    messages: OrderEvent[];
    sending?: boolean;
    selfRole?: string;
    quickPhrases?: string[];
    allowAttachments?: boolean;
  }>(),
  {
    selfRole: 'USER',
    quickPhrases: () => [],
    allowAttachments: true
  }
);

const emit = defineEmits<{
  (e: 'send', payload: ChatSendPayload): void;
}>();

const draft = ref('');
const attachedFiles = ref<PendingAttachment[]>([]);
const showPhrases = ref(false);
const messagesContainer = ref<HTMLDivElement | null>(null);

const quickPhraseList = computed(() =>
  props.quickPhrases.length ? props.quickPhrases : defaultPhrases
);
const trimmedDraft = computed(() => draft.value.trim());
const canSend = computed(() => Boolean(trimmedDraft.value) || attachedFiles.value.length > 0);

const handleAttachmentSelect: UploadProps['beforeUpload'] = (file) => {
  if (attachedFiles.value.length >= 5) {
    message.warning('一次最多选择 5 个附件');
    return false;
  }
  const raw = (file as any).originFileObj ?? file;
  if (raw) {
    attachedFiles.value = [
      ...attachedFiles.value,
      {
        id: `${Date.now()}-${raw.name}`,
        file: raw as File,
        name: raw.name,
        size: raw.size ?? 0
      }
    ];
  }
  return false;
};

const removeAttachment = (id: string) => {
  attachedFiles.value = attachedFiles.value.filter((item) => item.id !== id);
};

const togglePhrases = () => {
  showPhrases.value = !showPhrases.value;
};

const insertPhrase = (phrase: string) => {
  draft.value = draft.value ? `${draft.value}\n${phrase}` : phrase;
};

const formatSize = (bytes: number) => {
  if (!bytes) {
    return '0KB';
  }
  if (bytes < 1024) {
    return `${bytes}B`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)}KB`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)}MB`;
};

const handleSend = () => {
  if (!canSend.value || props.sending) {
    return;
  }
  const payload: ChatSendPayload = {
    content: trimmedDraft.value,
    attachments: attachedFiles.value.map((item) => item.file)
  };
  emit('send', payload);
  draft.value = '';
  attachedFiles.value = [];
  showPhrases.value = false;
};

const handlePressEnter = (event: KeyboardEvent) => {
  if (event.shiftKey) {
    return;
  }
  event.preventDefault();
  handleSend();
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
  flex-direction: column;
  gap: var(--space-3);
}

.composer-toolbar {
  display: flex;
  gap: var(--space-2);
}

.composer-toolbar__btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--color-text-secondary);
}

.attachment-list {
  background: var(--color-surface-muted);
  border-radius: var(--radius-card);
  padding: var(--space-2);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.attachment-chip {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
}

.attachment-chip__size {
  margin-left: var(--space-2);
  color: var(--color-text-secondary);
  font-size: 12px;
}

.chat-panel__phrases {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.phrase-chip {
  border: 1px solid var(--color-border);
  border-radius: 999px;
  padding: 2px 12px;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
  color: var(--color-text-secondary);
}

.phrase-chip:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.composer-input-row {
  display: flex;
  gap: var(--space-2);
}

.composer-input-row .ant-input-textarea {
  flex: 1;
}
</style>
