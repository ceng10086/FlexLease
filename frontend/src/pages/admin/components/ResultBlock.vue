<template>
  <div class="result-block">
    <div class="result-block__header">
      <span class="result-block__title">{{ title }}</span>
      <a-button type="link" size="small" @click="copy">
        <template #icon>
          <CopyOutlined />
        </template>
        复制
      </a-button>
    </div>
    <pre>{{ content }}</pre>
  </div>
</template>

<script lang="ts" setup>
import { message } from 'ant-design-vue';
import CopyOutlined from '@ant-design/icons-vue/CopyOutlined';

const props = defineProps<{ title: string; content: string }>();

const copy = async () => {
  try {
    await navigator.clipboard.writeText(props.content);
    message.success('已复制到剪贴板');
  } catch (error) {
    console.error('复制失败', error);
    message.error('复制失败，请手动复制');
  }
};
</script>

<style scoped>
.result-block {
  margin-top: 16px;
  background: #0f172a0f;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
}

.result-block__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.result-block__title {
  font-weight: 600;
  color: #1f2937;
}

pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: 'Fira Code', 'Cascadia Code', Consolas, monospace;
  font-size: 12px;
  line-height: 1.5;
}
</style>
