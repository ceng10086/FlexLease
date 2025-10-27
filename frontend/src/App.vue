<template>
  <a-config-provider :theme="theme">
    <router-view />
  </a-config-provider>
</template>

<script lang="ts" setup>
import { computed } from 'vue';

const theme = computed(() => ({
  token: {
    colorPrimary: '#1677ff'
  }
}));
</script>
