<template>
  <a-config-provider :theme="theme">
    <router-view />
  </a-config-provider>
</template>

<script lang="ts" setup>
import { computed } from 'vue';

const theme = computed(() => ({
  token: {
    colorPrimary: '#2563eb',
    colorInfo: '#2563eb',
    colorSuccess: '#16a34a',
    colorWarning: '#f97316',
    colorError: '#dc2626',
    colorBgLayout: '#eef2ff',
    colorBgContainer: '#ffffff',
    fontFamily: `'Inter', 'Segoe UI', -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Arial, sans-serif`,
    borderRadius: 10,
    borderRadiusLG: 16,
    fontSize: 14,
    controlHeightLG: 44
  }
}));
</script>
