<template>
  <a-config-provider :theme="theme">
    <div class="app-shell">
      <div v-if="auth.initializing" class="app-loading">
        <a-spin size="large" tip="正在加载会话" />
      </div>
      <router-view v-else />
    </div>
  </a-config-provider>
</template>

<script lang="ts" setup>
import { computed, onMounted } from 'vue';
import { useAuthStore } from './stores/auth';

const theme = computed(() => ({
  token: {
    colorPrimary: '#1677ff'
  }
}));

const auth = useAuthStore();

onMounted(async () => {
  if (auth.initializing) {
    await auth.bootstrap();
  }
});
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-loading {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}
</style>
