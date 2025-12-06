<template>
  <div class="auth-page">
    <div class="auth-page__intro">
      <h1>FlexLease 智能共享租赁平台</h1>
      <p>登录控制台以管理厂商入驻、商品租赁与全流程履约。</p>
    </div>
    <a-card class="auth-card" title="账号登录">
      <a-form layout="vertical" :model="form" @finish="handleSubmit">
        <a-form-item
          label="用户名"
          name="username"
          :rules="[{ required: true, message: '请输入用户名' }]"
        >
          <a-input
            v-model:value="form.username"
            size="large"
            placeholder="邮箱或手机号"
            autocomplete="username"
          />
        </a-form-item>
        <a-form-item
          label="密码"
          name="password"
          :rules="[{ required: true, message: '请输入密码' }]"
        >
          <a-input-password
            v-model:value="form.password"
            size="large"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </a-form-item>
        <a-form-item>
          <a-button
            type="primary"
            html-type="submit"
            size="large"
            block
            :loading="auth.loading"
          >
            登录
          </a-button>
        </a-form-item>
        <a-alert
          v-if="auth.error"
          :message="auth.error"
          type="error"
          show-icon
        />
        <div class="auth-card__footer">
          <span>还没有账号？</span>
          <a @click.prevent="goRegister">立即注册</a>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { reactive, watchEffect } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '../../stores/auth';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const form = reactive({
  username: '',
  password: ''
});

const targetPath = () => (route.query.redirect as string) || '/app/dashboard';

const handleSubmit = async () => {
  try {
    await auth.login({ ...form });
    router.replace(targetPath());
  } catch (error) {
    console.error('Login failed', error);
  }
};

const goRegister = () => {
  router.push({ name: 'register', query: { role: 'USER' } });
};

watchEffect(() => {
  if (!auth.initializing && auth.isAuthenticated) {
    router.replace(targetPath());
  }
});
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 48px;
  padding: 24px;
  background: linear-gradient(120deg, #0f172a, #1d4ed8 40%, #3b82f6 100%);
  color: #fff;
}

.auth-page__intro {
  max-width: 420px;
}

.auth-page__intro h1 {
  font-size: 36px;
  margin-bottom: 16px;
  line-height: 1.2;
}

.auth-page__intro p {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.6;
}

.auth-card {
  width: 360px;
  border-radius: 16px;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.28);
}

.auth-card__footer {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  font-size: 12px;
}

.auth-card__footer a {
  color: #1677ff;
}

@media (max-width: 960px) {
  .auth-page {
    flex-direction: column;
    text-align: center;
  }

  .auth-card {
    width: 100%;
    max-width: 360px;
  }
}
</style>
