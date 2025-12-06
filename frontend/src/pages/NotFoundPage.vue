<template>
  <div class="not-found">
    <a-result status="404" title="页面不存在" sub-title="抱歉，您访问的页面不存在或者已被删除。">
      <template #extra>
        <a-space>
          <a-button type="primary" @click="goHome">返回工作台</a-button>
          <a-button @click="goLogin">重新登录</a-button>
        </a-space>
      </template>
    </a-result>
  </div>
</template>

<script lang="ts" setup>
import { useRouter } from 'vue-router';

const router = useRouter();

const goHome = () => {
  router.replace({ name: 'dashboard' });
};

const goLogin = () => {
  router.replace({ name: 'login' });
};
</script>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 24px;
}
</style>
