<template>
  <div class="login-page">
    <div class="hero">
      <h1>FlexLease 管理控制台</h1>
      <p>登录后即可管理厂商入驻与共享租赁业务。</p>
    </div>
    <a-card class="login-card" title="账号登录">
      <a-form layout="vertical" :model="form" @finish="handleSubmit">
        <a-form-item name="username" label="用户名" :rules="[{ required: true, message: '请输入用户名' }]"><a-input v-model:value="form.username" size="large" placeholder="邮箱或手机号" autocomplete="username" /></a-form-item>
        <a-form-item name="password" label="密码" :rules="[{ required: true, message: '请输入密码' }]"><a-input-password v-model:value="form.password" size="large" placeholder="请输入密码" autocomplete="current-password" /></a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="auth.loading">登录</a-button>
        </a-form-item>
        <a-alert v-if="auth.error" :message="auth.error" type="error" show-icon />
      </a-form>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { reactive, watchEffect } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const form = reactive({
  username: '',
  password: ''
});

watchEffect(() => {
  if (!auth.initializing && auth.isAuthenticated) {
    router.replace((route.query.redirect as string) || '/');
  }
});

const handleSubmit = async () => {
  try {
    await auth.login({ ...form });
    router.replace((route.query.redirect as string) || '/');
  } catch (error) {
    console.error('Login failed', error);
  }
};
</script>

<style scoped>
.login-page {
  display: flex;
  flex-direction: row;
  min-height: 100vh;
  align-items: center;
  justify-content: center;
  gap: 48px;
  padding: 24px;
  background: linear-gradient(120deg, #f0f5ff 0%, #fff 50%, #f5f9ff 100%);
}

.hero {
  max-width: 360px;
}

.hero h1 {
  font-size: 32px;
  margin-bottom: 16px;
}

.hero p {
  color: #59617d;
  line-height: 1.6;
}

.login-card {
  width: 360px;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.12);
  border-radius: 16px;
}

@media (max-width: 960px) {
  .login-page {
    flex-direction: column;
  }

  .hero {
    text-align: center;
  }
}
</style>
