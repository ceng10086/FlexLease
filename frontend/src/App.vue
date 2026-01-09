<template>
  <a-config-provider :theme="theme">
    <router-view />
  </a-config-provider>
</template>

<script lang="ts" setup>
import { computed } from 'vue';

// 全局 UI 主题（Ant Design Vue token），用于保持各角色工作台视觉一致。
const theme = computed(() => ({
  token: {
    colorPrimary: '#2563eb',
    colorInfo: '#2563eb',
    colorSuccess: '#10b981',
    colorWarning: '#ff8a00',
    colorError: '#f43f5e',
    colorBgLayout: '#f7f7f5',
    colorBgContainer: '#ffffff',
    fontFamily: `var(--font-stack)`,
    fontSize: 14,
    borderRadius: 10,
    borderRadiusLG: 16,
    controlHeightLG: 44
  }
}));
</script>
