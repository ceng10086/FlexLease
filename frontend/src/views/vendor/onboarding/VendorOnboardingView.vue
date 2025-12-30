<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="厂商入驻 · 一站式跟踪"
        eyebrow="Vendor Onboarding"
        description="提交企业资料 → 等待审核 → 激活厂商 ID，建议审核通过后重新登录。"
      >
        <template #actions>
          <a-button :loading="loading" @click="loadApplications">刷新</a-button>
        </template>
      </PageHeader>
    </template>

    <div class="onboarding-grid">
      <section class="surface-card onboarding-panel">
        <h3>最新申请</h3>
        <a-steps :current="currentStep" responsive size="small" class="steps">
          <a-step title="填写资料" description="完善公司主体信息" />
          <a-step title="等待审核" description="管理员核验资质" />
          <a-step title="完成入驻" description="账号激活并同步厂商 ID" />
        </a-steps>
        <div v-if="latestApplication" class="application-detail">
          <a-tag :color="statusMeta(latestApplication.status).color">
            {{ statusMeta(latestApplication.status).label }}
          </a-tag>
          <small>
            提交于
            {{ latestApplication.submittedAt ? formatDate(latestApplication.submittedAt) : '—' }}
          </small>
          <a-descriptions :column="1" size="small" bordered class="mt-3">
            <a-descriptions-item label="公司">{{ latestApplication.companyName }}</a-descriptions-item>
            <a-descriptions-item label="统一社会信用代码">
              {{ latestApplication.unifiedSocialCode }}
            </a-descriptions-item>
            <a-descriptions-item label="联系人">{{ latestApplication.contactName }}</a-descriptions-item>
            <a-descriptions-item label="联系电话">{{ latestApplication.contactPhone }}</a-descriptions-item>
            <a-descriptions-item label="联系邮箱">{{ latestApplication.contactEmail || '—' }}</a-descriptions-item>
            <a-descriptions-item label="所在地区">
              {{
                [latestApplication.province, latestApplication.city].filter(Boolean).join(' / ') || '—'
              }}
            </a-descriptions-item>
            <a-descriptions-item label="详细地址">
              {{ latestApplication.address || '—' }}
            </a-descriptions-item>
            <a-descriptions-item label="审核备注">{{ latestApplication.reviewRemark || '—' }}</a-descriptions-item>
          </a-descriptions>
          <a-alert
            class="mt-3"
            :type="statusMeta(latestApplication.status).alert"
            show-icon
            :message="statusMeta(latestApplication.status).message"
          />
        </div>
        <DataStateBlock
          v-else
          type="empty"
          title="暂无申请记录"
          description="提交资料后可在此查看审核进度。"
        />
      </section>

      <section class="surface-card onboarding-panel">
        <h3>提交新申请</h3>
        <a-alert
          v-if="submitGuard"
          :type="submitGuard.type"
          show-icon
          :message="submitGuard.message"
          class="mt-3"
        />
        <a-form layout="vertical" class="mt-3" @submit.prevent>
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="公司名称" required>
                <a-input v-model:value="form.companyName" placeholder="请输入公司主体名称" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="统一社会信用代码" required>
                <a-input v-model:value="form.unifiedSocialCode" placeholder="请输入 18 位信用代码" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="联系人" required>
                <a-input v-model:value="form.contactName" placeholder="负责人姓名" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="联系电话" required>
                <a-input v-model:value="form.contactPhone" placeholder="手机或座机" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="联系邮箱">
            <a-input v-model:value="form.contactEmail" placeholder="用于接收通知（可选）" />
          </a-form-item>
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="省份/直辖市">
                <a-input v-model:value="form.province" placeholder="如 广东省" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="城市">
                <a-input v-model:value="form.city" placeholder="如 深圳市" />
              </a-form-item>
            </a-col>
          </a-row>
          <a-form-item label="详细地址">
            <a-textarea v-model:value="form.address" :rows="3" placeholder="街道/园区地址" />
          </a-form-item>
          <div class="form-actions">
            <a-button
              type="primary"
              :loading="submitting"
              :disabled="!canSubmit"
              @click="handleSubmit"
            >
              提交申请
            </a-button>
          </div>
        </a-form>
      </section>
    </div>
  </PageShell>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import PageShell from '../../../components/layout/PageShell.vue';
import PageHeader from '../../../components/layout/PageHeader.vue';
import DataStateBlock from '../../../components/feedback/DataStateBlock.vue';
import { useAuthStore } from '../../../stores/auth';
import {
  listVendorApplications,
  submitVendorApplication,
  type VendorApplication
} from '../../../services/vendorService';
import { friendlyErrorMessage } from '../../../utils/error';

const auth = useAuthStore();
const loading = ref(false);
const submitting = ref(false);
const applications = ref<VendorApplication[]>([]);

const form = reactive({
  companyName: '',
  unifiedSocialCode: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  province: '',
  city: '',
  address: ''
});

const myApplications = computed(() => {
  if (!auth.user?.id) {
    return [];
  }
  return applications.value
    .filter((item) => item.ownerUserId === auth.user?.id)
    .sort((a, b) => {
      const aTime = a.submittedAt ? new Date(a.submittedAt).getTime() : 0;
      const bTime = b.submittedAt ? new Date(b.submittedAt).getTime() : 0;
      return bTime - aTime;
    });
});

const latestApplication = computed(() => myApplications.value.at(0) ?? null);
const canSubmit = computed(() => {
  const status = latestApplication.value?.status;
  return !status || status === 'REJECTED' || status === 'APPROVED';
});

const submitGuard = computed<null | { type: 'info' | 'success'; message: string }>(() => {
  const status = latestApplication.value?.status;
  if (!status || canSubmit.value) {
    return null;
  }
  if (status === 'SUBMITTED') {
    return { type: 'info', message: '当前申请仍在审核中，如需修改请等待结果后再提交。' };
  }
  return { type: 'info', message: '当前申请暂不可重复提交，请等待状态更新。' };
});

watch(
  latestApplication,
  (application) => {
    if (!application) {
      return;
    }
    const isFormEmpty = !form.companyName
      && !form.unifiedSocialCode
      && !form.contactName
      && !form.contactPhone
      && !form.contactEmail
      && !form.province
      && !form.city
      && !form.address;

    if (!isFormEmpty) {
      return;
    }
    Object.assign(form, {
      companyName: application.companyName ?? '',
      unifiedSocialCode: application.unifiedSocialCode ?? '',
      contactName: application.contactName ?? '',
      contactPhone: application.contactPhone ?? '',
      contactEmail: application.contactEmail ?? '',
      province: application.province ?? '',
      city: application.city ?? '',
      address: application.address ?? ''
    });
  },
  { immediate: true }
);
const currentStep = computed(() => {
  const status = latestApplication.value?.status ?? 'DRAFT';
  if (status === 'APPROVED') {
    return 2;
  }
  if (status === 'SUBMITTED') {
    return 1;
  }
  if (status === 'REJECTED') {
    return 1;
  }
  return 0;
});

const statusMeta = (status: VendorApplication['status']) => {
  switch (status) {
    case 'APPROVED':
      return { color: 'green', label: '已通过', alert: 'success', message: '审核已通过，账号已绑定厂商身份。' };
    case 'REJECTED':
      return { color: 'red', label: '已驳回', alert: 'error', message: '审核未通过，请根据备注修改资料后重试。' };
    default:
      return { color: 'blue', label: '审核中', alert: 'info', message: '审核中，请耐心等待管理员处理。' };
  }
};

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '-');

const loadApplications = async () => {
  loading.value = true;
  try {
    applications.value = await listVendorApplications();
  } catch (error) {
    console.error('加载厂商申请失败', error);
    message.error(friendlyErrorMessage(error, '加载申请列表失败，请稍后重试'));
  } finally {
    loading.value = false;
  }
};

const validateForm = () => {
  if (!form.companyName || !form.unifiedSocialCode || !form.contactName || !form.contactPhone) {
    message.warning('请完整填写必填字段');
    return false;
  }
  return true;
};

const resetForm = () => {
  Object.assign(form, {
    companyName: '',
    unifiedSocialCode: '',
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    province: '',
    city: '',
    address: ''
  });
};

const handleSubmit = async () => {
  if (!auth.user?.id) {
    message.error('未获取到当前用户信息');
    return;
  }
  if (!validateForm()) {
    return;
  }
  submitting.value = true;
  try {
    await submitVendorApplication({ ...form });
    message.success('申请已提交，请等待审核');
    resetForm();
    await loadApplications();
  } catch (error) {
    console.error('提交厂商申请失败', error);
    message.error(friendlyErrorMessage(error, '提交失败，请稍后重试'));
  } finally {
    submitting.value = false;
  }
};

loadApplications();
</script>

<style scoped>
.onboarding-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: var(--space-4);
}

.onboarding-panel {
  padding: var(--space-4);
}

.steps {
  margin-bottom: var(--space-4);
}

.application-detail {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mt-3 {
  margin-top: var(--space-3);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}
</style>
