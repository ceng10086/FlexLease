<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>个人资料</h2>
        <p class="page-header__meta">完善联系信息，用于订单通知、配送及合同签署。</p>
      </div>
      <a-space>
        <a-button :loading="loading" @click="loadProfile">刷新</a-button>
        <a-button type="primary" :loading="saving" @click="handleSubmit">保存修改</a-button>
      </a-space>
    </div>

    <a-card>
      <a-spin :spinning="loading">
        <div class="card-section">
          <a-alert
            type="info"
            show-icon
            message="为保证通知顺利送达，请保持手机号和邮箱最新。首次进入会自动为您创建档案。"
          />
          <a-descriptions :column="2" size="small" bordered>
            <a-descriptions-item label="账号">{{ auth.user?.username }}</a-descriptions-item>
            <a-descriptions-item label="最近登录时间">
              {{ auth.user?.lastLoginAt ? formatDate(auth.user.lastLoginAt) : '—' }}
            </a-descriptions-item>
            <a-descriptions-item label="资料更新时间" :span="2">
              {{ profile?.updatedAt ? formatDate(profile.updatedAt) : '—' }}
            </a-descriptions-item>
          </a-descriptions>

          <a-form ref="formRef" layout="vertical" :model="formState" :rules="rules">
            <a-row :gutter="12">
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

            <a-row :gutter="12">
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
          </a-form>

          <div class="form-actions">
            <a-space>
              <a-button @click="loadProfile" :loading="loading">重置</a-button>
              <a-button type="primary" :loading="saving" @click="handleSubmit">保存修改</a-button>
            </a-space>
          </div>
        </div>
      </a-spin>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'ant-design-vue';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../../stores/auth';
import {
  fetchUserProfile,
  updateUserProfile,
  type UserProfile,
  type UserProfileUpdatePayload
} from '../../services/userProfileService';

const auth = useAuthStore();
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

const formatDate = (value: string) => new Date(value).toLocaleString();

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
    message.error('加载个人资料失败，请稍后重试');
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
.form-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
