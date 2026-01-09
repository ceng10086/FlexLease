<template>
  <div class="auth-page">
    <div class="auth-page__intro">
      <h1>创建 FlexLease 账号</h1>
      <p>根据角色选择注册路径。厂商注册后需提交入驻申请方可上架商品。</p>
    </div>
    <a-card class="auth-card" title="用户注册">
      <a-form layout="vertical" :model="form" @finish="handleSubmit">
        <a-form-item label="注册类型">
          <a-segmented
            v-model:value="form.role"
            :options="roleOptions"
          />
        </a-form-item>
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
            placeholder="至少 8 位密码"
            autocomplete="new-password"
          />
        </a-form-item>
        <a-form-item
          label="确认密码"
          name="confirmPassword"
          :rules="[{ required: true, message: '请再次输入密码' }]"
        >
          <a-input-password
            v-model:value="form.confirmPassword"
            size="large"
            placeholder="请再次输入密码"
            autocomplete="new-password"
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
            注册
          </a-button>
        </a-form-item>
        <div class="auth-card__footer">
          <span>已有账号？</span>
          <a @click.prevent="goLogin">返回登录</a>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
// 注册页：支持消费者/厂商两种角色注册（role 由 query 参数决定）。
import { reactive } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../../stores/auth';

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const roleOptions = [
  { label: '消费者', value: 'USER' },
  { label: '厂商', value: 'VENDOR' }
];

const form = reactive({
  role: (route.query.role as 'USER' | 'VENDOR') || 'USER',
  username: '',
  password: '',
  confirmPassword: ''
});

const handleSubmit = async () => {
  if (!form.username || !form.password) {
    message.warning('请完整填写注册信息');
    return;
  }
  if (form.password !== form.confirmPassword) {
    message.warning('两次输入的密码不一致');
    return;
  }
  try {
    await auth.register(form.role, {
      username: form.username,
      password: form.password
    });
    router.replace({ name: 'login', query: { username: form.username } });
  } catch (error) {
    console.error('Register failed', error);
  }
};

const goLogin = () => {
  router.push({ name: 'login' });
};
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 48px;
  padding: 24px;
  background: radial-gradient(circle at top left, #38bdf8, #1e40af 45%, #0f172a 100%);
  color: #fff;
}

.auth-page__intro {
  max-width: 420px;
}

.auth-page__intro h1 {
  font-size: 34px;
  margin-bottom: 16px;
}

.auth-page__intro p {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.6;
}

.auth-card {
  width: 380px;
  border-radius: 18px;
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
