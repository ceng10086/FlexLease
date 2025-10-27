<template>
  <div class="login-container">
    <div class="login-hero">
      <h1>FlexLease 智能化共享租赁平台</h1>
      <p>
        面向 B2C 租赁场景的统一运营中台，帮助平台管理员、入驻厂商与终端用户协同完成商品配置、订单履约、支付结算与风控管理。
      </p>
      <div class="login-highlights">
        <div class="highlight-card">
          <span class="highlight-title">多角色协同</span>
          <span class="highlight-desc">管理员、厂商、用户角色全流程联动</span>
        </div>
        <div class="highlight-card">
          <span class="highlight-title">多租赁模式</span>
          <span class="highlight-desc">支持先租后买、以租代售、共享租赁</span>
        </div>
        <div class="highlight-card">
          <span class="highlight-title">智能风控</span>
          <span class="highlight-desc">订单、支付与风控数据一体化分析</span>
        </div>
      </div>
    </div>
    <div class="login-panel">
      <a-card title="登录到平台" :bordered="false" class="login-card">
        <a-form layout="vertical" :model="form" @submit.prevent="handleSubmit">
          <a-form-item label="用户名">
            <a-input v-model:value="form.username" size="large" placeholder="请输入用户名" />
          </a-form-item>
          <a-form-item label="密码">
            <a-input-password v-model:value="form.password" size="large" placeholder="请输入密码" />
          </a-form-item>
          <a-form-item label="角色偏好">
            <a-select v-model:value="form.roleHint" size="large">
              <a-select-option value="CUSTOMER">C 端用户</a-select-option>
              <a-select-option value="VENDOR">B 端厂商</a-select-option>
              <a-select-option value="ADMIN">平台管理员</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" html-type="submit" size="large" block :loading="loading">
              使用账号登录
            </a-button>
          </a-form-item>
        </a-form>
        <a-divider>或快速体验</a-divider>
        <a-space direction="vertical" style="width: 100%">
          <a-button block size="large" @click="simulate('CUSTOMER')">体验 C 端用户视角</a-button>
          <a-button block size="large" @click="simulate('VENDOR')">体验 B 端厂商视角</a-button>
          <a-button block size="large" @click="simulate('ADMIN')">体验平台管理员视角</a-button>
        </a-space>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter, useRoute } from 'vue-router';
import { message } from 'ant-design-vue';
import { useAuthStore } from '@/stores/auth';
import type { UserRole } from '@/types';

const auth = useAuthStore();
const router = useRouter();
const route = useRoute();

const { defaultRoute } = storeToRefs(auth);

const form = reactive({
  username: '',
  password: '',
  roleHint: 'CUSTOMER' as UserRole
});

const loading = computed(() => auth.loading);

async function handleSubmit() {
  if (!form.username || !form.password) {
    message.warning('请输入用户名和密码');
    return;
  }
  try {
    await auth.login({ username: form.username, password: form.password, roleHint: form.roleHint });
    const redirect = (route.query.redirect as string) || defaultRoute.value;
    router.push(redirect);
  } catch (error) {
    message.error('登录失败，请检查账号信息或选择体验模式');
    console.error(error);
  }
}

function simulate(role: UserRole) {
  auth.simulate(role);
  router.push(defaultRoute.value);
}
</script>

<style scoped>
.login-container {
  display: grid;
  grid-template-columns: 2fr 1fr;
  min-height: 100vh;
}

.login-hero {
  padding: 96px 80px;
  background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 100%);
  color: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-hero h1 {
  font-size: 40px;
  margin-bottom: 24px;
}

.login-hero p {
  font-size: 18px;
  line-height: 1.6;
  margin-bottom: 40px;
  max-width: 520px;
}

.login-highlights {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.highlight-card {
  background: rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 20px;
  backdrop-filter: blur(6px);
}

.highlight-title {
  font-weight: 600;
  display: block;
  margin-bottom: 8px;
}

.highlight-desc {
  font-size: 14px;
  opacity: 0.85;
}

.login-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px;
}

.login-card {
  width: 420px;
  box-shadow: 0 30px 60px rgba(15, 23, 42, 0.15);
  border-radius: 18px;
}

@media (max-width: 1024px) {
  .login-container {
    grid-template-columns: 1fr;
  }

  .login-hero {
    padding: 64px 32px;
  }

  .login-panel {
    padding: 32px;
  }
}
</style>
