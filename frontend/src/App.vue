<template>
  <a-config-provider :theme="theme">
    <div class="app-shell">
      <router-view />
    </div>
  </a-config-provider>
</template>

<script lang="ts" setup>
import { computed, onMounted } from 'vue';
import { useAuthStore } from '@/stores/auth';

const auth = useAuthStore();

const theme = computed(() => ({
  token: {
    colorPrimary: '#1677ff',
    borderRadius: 6
  }
}));

onMounted(() => {
  if (!auth.initialized) {
    auth.bootstrap();
  }
});
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background: var(--app-background);
  color: var(--app-text-color);
}
</style>
