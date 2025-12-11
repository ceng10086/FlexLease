<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="资料与信用档案"
        eyebrow="Profile"
        description="留存最新联系方式，信用分将影响押金减免和人工审核。"
      >
        <template #actions>
          <a-space>
            <a-button :loading="loading" @click="loadProfile">刷新</a-button>
            <a-button type="primary" :loading="saving" @click="handleSubmit">保存修改</a-button>
          </a-space>
        </template>
      </PageHeader>
    </template>

    <div class="profile-grid">
      <section class="profile-summary surface-card">
        <div class="profile-summary__section">
          <h3>账号信息</h3>
          <p>账号：{{ auth.user?.username }}</p>
          <p>最近登录：{{ auth.user?.lastLoginAt ? formatDate(auth.user.lastLoginAt) : '—' }}</p>
          <p>资料更新时间：{{ profile?.updatedAt ? formatDate(profile.updatedAt) : '—' }}</p>
        </div>
        <div class="profile-summary__section credit-card">
          <div>
            <p>信用档案</p>
            <a-tag :color="creditColor(profile?.creditTier)">
              {{ creditLabel(profile?.creditTier) }} · {{ profile?.creditScore ?? '--' }} 分
            </a-tag>
          </div>
          <small>信用良好可享押金减免，异常行为会触发人工审核。</small>
        </div>
        <div
          class="profile-summary__section suspension-card"
          :class="isSuspended ? 'suspension-card--alert' : 'suspension-card--ok'"
        >
          <div class="suspension-header">
            <strong>{{ isSuspended ? '账号已冻结' : '账号状态正常' }}</strong>
            <a-tag :color="isSuspended ? 'red' : 'success'">{{ isSuspended ? '冻结' : '正常' }}</a-tag>
          </div>
          <p v-if="isSuspended">
            将于 {{ suspensionDeadlineText }} 自动解冻，剩余 {{ suspensionCountdown }}。
          </p>
          <p v-else>无冻结记录，保持良好信用可享押金减免。</p>
          <a-button v-if="isSuspended" type="link" size="small" @click="goNotificationCenter">
            查看通知提醒
          </a-button>
        </div>
      </section>

      <section class="profile-form surface-card">
        <a-form ref="formRef" layout="vertical" :model="formState" :rules="rules">
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="姓名" name="fullName" required>
                <a-input v-model:value="formState.fullName" placeholder="请输入真实姓名" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="性别" name="gender" required>
                <a-radio-group v-model:value="formState.gender">
                  <a-radio value="UNKNOWN">保密</a-radio>
                  <a-radio value="MALE">男</a-radio>
                  <a-radio value="FEMALE">女</a-radio>
                </a-radio-group>
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="手机号" name="phone" required>
                <a-input v-model:value="formState.phone" placeholder="用于联系与物流" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="邮箱" name="email" required>
                <a-input v-model:value="formState.email" placeholder="用于接收通知" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="联系地址" name="address">
            <a-textarea
              v-model:value="formState.address"
              :rows="3"
              placeholder="示例：广东省深圳市南山区 XX 路 XX 号"
            />
          </a-form-item>
          <div class="form-actions">
            <a-space>
              <a-button :loading="loading" @click="loadProfile">重置</a-button>
              <a-button type="primary" :loading="saving" @click="handleSubmit">保存修改</a-button>
            </a-space>
          </div>
        </a-form>
      </section>
    </div>
  </PageShell>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'ant-design-vue';
import { message } from 'ant-design-vue';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import { useAuthStore } from '../../stores/auth';
import {
  fetchUserProfile,
  updateUserProfile,
  type UserProfileUpdatePayload,
  type UserProfile
} from '../../services/userProfileService';
import { creditTierColor, creditTierLabel } from '../../types/credit';

const auth = useAuthStore();
const router = useRouter();
const profile = ref<UserProfile | null>(null);
const loading = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();

const formState = reactive<UserProfileUpdatePayload>({
  fullName: '',
  gender: 'UNKNOWN',
  phone: '',
  email: '',
  address: ''
});

const rules: FormRules = {
  fullName: [{ required: true, message: '请输入姓名' }, { max: 100, message: '姓名过长' }],
  gender: [{ required: true, message: '请选择性别' }],
  phone: [
    { required: true, message: '请输入手机号' },
    { max: 20, message: '手机号过长' }
  ],
  email: [
    { required: true, message: '请输入邮箱' },
    { type: 'email', message: '邮箱格式不正确' },
    { max: 100, message: '邮箱过长' }
  ],
  address: [{ max: 255, message: '地址过长' }]
};

const formatDate = (value: string | Date) => new Date(value).toLocaleString();
const creditColor = (tier?: UserProfile['creditTier']) => creditTierColor(tier);
const creditLabel = (tier?: UserProfile['creditTier']) => creditTierLabel(tier);

const suspensionDeadline = computed(() =>
  profile.value?.suspendedUntil ? new Date(profile.value.suspendedUntil) : null
);
const isSuspended = computed(() => Boolean(suspensionDeadline.value && suspensionDeadline.value.getTime() > Date.now()));
const suspensionDeadlineText = computed(() =>
  suspensionDeadline.value ? formatDate(suspensionDeadline.value) : '--'
);
const suspensionCountdown = computed(() => {
  if (!suspensionDeadline.value) {
    return '--';
  }
  const diff = suspensionDeadline.value.getTime() - Date.now();
  if (diff <= 0) {
    return '即刻恢复';
  }
  const dayMs = 24 * 60 * 60 * 1000;
  const hourMs = 60 * 60 * 1000;
  const minuteMs = 60 * 1000;
  const days = Math.floor(diff / dayMs);
  const hours = Math.floor((diff % dayMs) / hourMs);
  const minutes = Math.floor((diff % hourMs) / minuteMs);
  if (days > 0) {
    return `${days} 天 ${hours} 小时`;
  }
  if (hours > 0) {
    return `${hours} 小时 ${minutes} 分`;
  }
  return `${minutes} 分`;
});

const goNotificationCenter = () => {
  router.push({ name: 'notifications' });
};

const loadProfile = async () => {
  loading.value = true;
  try {
    const data = await fetchUserProfile();
    profile.value = data;
    Object.assign(formState, {
      fullName: data.fullName ?? '',
      gender: data.gender ?? 'UNKNOWN',
      phone: data.phone ?? '',
      email: data.email ?? '',
      address: data.address ?? ''
    });
  } catch (error) {
    console.error('加载个人资料失败', error);
    message.error('加载个人资料失败，请稍后再试');
  } finally {
    loading.value = false;
  }
};

const handleSubmit = async () => {
  if (!formRef.value) {
    return;
  }
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  saving.value = true;
  try {
    const updated = await updateUserProfile({ ...formState });
    profile.value = updated;
    message.success('个人资料已更新');
  } catch (error: any) {
    const msg = error?.message ?? '更新个人资料失败';
    message.error(msg);
  } finally {
    saving.value = false;
  }
};

onMounted(() => {
  loadProfile();
});
</script>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: var(--space-4);
}

.profile-summary {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.profile-summary__section h3 {
  margin-top: 0;
}

.credit-card {
  background: linear-gradient(120deg, rgba(37, 99, 235, 0.12), rgba(16, 185, 129, 0.12));
  border-radius: 16px;
  padding: var(--space-3);
}

.suspension-card {
  border-radius: 16px;
  padding: var(--space-3);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  border: 1px solid transparent;
}

.suspension-card--alert {
  background: rgba(244, 63, 94, 0.08);
  border-color: rgba(244, 63, 94, 0.35);
}

.suspension-card--ok {
  background: rgba(16, 185, 129, 0.08);
  border-color: rgba(16, 185, 129, 0.35);
}

.suspension-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.profile-form {
  padding: var(--space-4);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
