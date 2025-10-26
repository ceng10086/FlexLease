<template>
  <div class="page">
    <header class="page__header">
      <div>
        <h2>厂商入驻申请</h2>
        <p class="page__subtitle">提交企业资料，等待管理员审核后即可管理共享租赁商品。</p>
      </div>
      <a-button type="default" @click="loadApplications" :loading="loading">刷新</a-button>
    </header>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="10">
        <a-card title="申请记录" bordered>
          <a-spin :spinning="loading">
            <template v-if="latestApplication">
              <div class="status">
                <a-tag :color="statusMeta(latestApplication.status).color">
                  {{ statusMeta(latestApplication.status).label }}
                </a-tag>
                <span class="status__time" v-if="latestApplication.submittedAt">
                  提交于 {{ formatDate(latestApplication.submittedAt) }}
                </span>
              </div>
              <a-descriptions :column="1" size="small" bordered>
                <a-descriptions-item label="公司名称">{{ latestApplication.companyName }}</a-descriptions-item>
                <a-descriptions-item label="统一社会信用代码">
                  {{ latestApplication.unifiedSocialCode }}
                </a-descriptions-item>
                <a-descriptions-item label="联系人">{{ latestApplication.contactName }}</a-descriptions-item>
                <a-descriptions-item label="联系电话">{{ latestApplication.contactPhone }}</a-descriptions-item>
                <a-descriptions-item label="联系邮箱" v-if="latestApplication.contactEmail">
                  {{ latestApplication.contactEmail }}
                </a-descriptions-item>
                <a-descriptions-item label="所在地区" v-if="latestApplication.city || latestApplication.province">
                  {{ [latestApplication.province, latestApplication.city].filter(Boolean).join(' / ') }}
                </a-descriptions-item>
                <a-descriptions-item label="详细地址" v-if="latestApplication.address">
                  {{ latestApplication.address }}
                </a-descriptions-item>
                <a-descriptions-item label="审核结果" v-if="latestApplication.reviewRemark">
                  {{ latestApplication.reviewRemark }}
                </a-descriptions-item>
                <a-descriptions-item label="审核时间" v-if="latestApplication.reviewedAt">
                  {{ formatDate(latestApplication.reviewedAt) }}
                </a-descriptions-item>
              </a-descriptions>
              <a-alert
                v-if="latestApplication.status === 'APPROVED'"
                type="success"
                show-icon
                class="mt-16"
                message="审核已通过，您的账号已激活，可继续创建商品并进行租赁业务。"
              />
              <a-alert
                v-else-if="latestApplication.status === 'REJECTED'"
                type="error"
                show-icon
                class="mt-16"
                message="审核未通过，请修改资料后再次提交。"
              />
              <a-alert
                v-else
                type="info"
                show-icon
                class="mt-16"
                message="审核中，请耐心等待管理员处理。"
              />
            </template>
            <a-empty v-else description="暂无申请记录" />
          </a-spin>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="14">
        <a-card title="提交新申请" bordered>
          <template v-if="canSubmit">
            <a-form layout="vertical" @submit.prevent>
              <a-form-item label="公司名称" required>
                <a-input v-model:value="form.companyName" placeholder="请输入公司主体名称" />
              </a-form-item>
              <a-form-item label="统一社会信用代码" required>
                <a-input v-model:value="form.unifiedSocialCode" placeholder="请输入 18 位统一社会信用代码" />
              </a-form-item>
              <a-row :gutter="12">
                <a-col :span="12">
                  <a-form-item label="联系人" required>
                    <a-input v-model:value="form.contactName" placeholder="负责人姓名" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="联系电话" required>
                    <a-input v-model:value="form.contactPhone" placeholder="手机或座机" />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-form-item label="联系邮箱">
                <a-input v-model:value="form.contactEmail" placeholder="用于接收通知，可选" />
              </a-form-item>
              <a-row :gutter="12">
                <a-col :span="12">
                  <a-form-item label="省份/直辖市">
                    <a-input v-model:value="form.province" placeholder="如 广东省" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="城市">
                    <a-input v-model:value="form.city" placeholder="如 深圳市" />
                  </a-form-item>
                </a-col>
              </a-row>
              <a-form-item label="详细地址">
                <a-textarea v-model:value="form.address" :rows="3" placeholder="街道/园区地址" />
              </a-form-item>
              <div class="form__actions">
                <a-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</a-button>
              </div>
            </a-form>
          </template>
          <a-alert
            v-else
            type="info"
            show-icon
            message="当前申请正在审核或已通过，如需修改资料请等待审核完成后再次提交。"
          />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../stores/auth';
import type { VendorApplication, VendorApplicationStatus } from '../utils/vendorApi';
import { listVendorApplications, submitVendorApplication } from '../utils/vendorApi';

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
    return [] as VendorApplication[];
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
  if (!latestApplication.value) {
    return true;
  }
  return latestApplication.value.status === 'REJECTED';
});

const statusMeta = (status: VendorApplicationStatus) => {
  switch (status) {
    case 'APPROVED':
      return { color: 'green', label: '已通过' };
    case 'REJECTED':
      return { color: 'red', label: '已驳回' };
    default:
      return { color: 'blue', label: '审核中' };
  }
};

const formatDate = (value?: string | null) => {
  if (!value) return '-';
  return new Date(value).toLocaleString();
};

const loadApplications = async () => {
  loading.value = true;
  try {
    applications.value = await listVendorApplications();
  } catch (error) {
    console.error('加载厂商申请失败', error);
    message.error('加载申请列表失败，请稍后重试');
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
    await submitVendorApplication(auth.user.id, { ...form });
    message.success('申请已提交，请等待审核');
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
    loadApplications();
  } catch (error: any) {
    console.error('提交厂商申请失败', error);
    message.error(error?.response?.data?.message ?? '提交失败，请稍后重试');
  } finally {
    submitting.value = false;
  }
};

onMounted(() => {
  loadApplications();
});
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
}

.page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page__subtitle {
  color: #6b7280;
  margin: 4px 0 0;
}

.status {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.status__time {
  color: #6b7280;
}

.form__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.mt-16 {
  margin-top: 16px;
}
</style>
