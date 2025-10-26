<template>
  <div class="page">
    <header class="page__header">
      <div>
        <h2>厂商入驻审核</h2>
        <p class="page__subtitle">查看并审核厂商提交的入驻申请，审核通过后将自动激活相关账号。</p>
      </div>
      <a-space>
        <a-select v-model:value="filters.status" style="width: 160px" allow-clear placeholder="全部状态" @change="loadApplications">
          <a-select-option value="SUBMITTED">待审核</a-select-option>
          <a-select-option value="APPROVED">已通过</a-select-option>
          <a-select-option value="REJECTED">已驳回</a-select-option>
        </a-select>
        <a-button @click="loadApplications" :loading="loading">刷新</a-button>
      </a-space>
    </header>

    <a-card bordered>
      <a-table
        :data-source="applications"
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <a-table-column key="companyName" title="公司" data-index="companyName" />
        <a-table-column key="contactName" title="联系人">
          <template #default="{ record }">
            <div class="contact">
              <div>{{ record.contactName }}</div>
              <div class="contact__info">{{ record.contactPhone }}</div>
            </div>
          </template>
        </a-table-column>
        <a-table-column key="status" title="状态">
          <template #default="{ record }">
            <a-tag :color="statusMeta(record.status).color">{{ statusMeta(record.status).label }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column key="submittedAt" title="提交时间">
          <template #default="{ record }">{{ formatDate(record.submittedAt) }}</template>
        </a-table-column>
        <a-table-column key="actions" title="操作">
          <template #default="{ record }">
            <a-space>
              <a-button size="small" @click="openDetail(record)">详情</a-button>
              <a-button
                v-if="record.status === 'SUBMITTED'"
                size="small"
                type="primary"
                @click="handleQuickApprove(record)"
              >通过</a-button>
              <a-button
                v-if="record.status === 'SUBMITTED'"
                size="small"
                danger
                @click="openReject(record)"
              >驳回</a-button>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detailDrawer.open" :width="520" title="申请详情" destroy-on-close>
      <a-descriptions :column="1" size="small" bordered v-if="detailDrawer.record">
        <a-descriptions-item label="公司名称">{{ detailDrawer.record.companyName }}</a-descriptions-item>
        <a-descriptions-item label="统一社会信用代码">
          {{ detailDrawer.record.unifiedSocialCode }}
        </a-descriptions-item>
        <a-descriptions-item label="联系人">{{ detailDrawer.record.contactName }}</a-descriptions-item>
        <a-descriptions-item label="联系电话">{{ detailDrawer.record.contactPhone }}</a-descriptions-item>
        <a-descriptions-item label="联系邮箱" v-if="detailDrawer.record.contactEmail">
          {{ detailDrawer.record.contactEmail }}
        </a-descriptions-item>
        <a-descriptions-item label="省份/城市" v-if="detailDrawer.record.city || detailDrawer.record.province">
          {{ [detailDrawer.record.province, detailDrawer.record.city].filter(Boolean).join(' / ') }}
        </a-descriptions-item>
        <a-descriptions-item label="详细地址" v-if="detailDrawer.record.address">
          {{ detailDrawer.record.address }}
        </a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-tag :color="statusMeta(detailDrawer.record.status).color">
            {{ statusMeta(detailDrawer.record.status).label }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="提交时间">{{ formatDate(detailDrawer.record.submittedAt) }}</a-descriptions-item>
        <a-descriptions-item label="审核时间" v-if="detailDrawer.record.reviewedAt">
          {{ formatDate(detailDrawer.record.reviewedAt) }}
        </a-descriptions-item>
        <a-descriptions-item label="审核意见" v-if="detailDrawer.record.reviewRemark">
          {{ detailDrawer.record.reviewRemark }}
        </a-descriptions-item>
      </a-descriptions>
      <template #footer v-if="detailDrawer.record?.status === 'SUBMITTED'">
        <a-space>
          <a-button type="primary" @click="handleQuickApprove(detailDrawer.record)">审核通过</a-button>
          <a-button danger @click="openReject(detailDrawer.record)">驳回</a-button>
        </a-space>
      </template>
    </a-drawer>

    <a-modal
      v-model:open="rejectModal.open"
      title="驳回申请"
      :confirm-loading="rejectModal.submitting"
      @ok="handleReject"
    >
      <a-form layout="vertical">
        <a-form-item label="驳回原因">
          <a-textarea v-model:value="rejectModal.remark" :rows="4" placeholder="请输入驳回原因" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../stores/auth';
import {
  approveVendorApplication,
  listVendorApplications,
  rejectVendorApplication
} from '../utils/vendorApi';
import type { VendorApplication, VendorApplicationStatus } from '../utils/vendorApi';

const auth = useAuthStore();

const loading = ref(false);
const applications = ref<VendorApplication[]>([]);
const filters = reactive<{ status?: VendorApplicationStatus | undefined }>({ status: 'SUBMITTED' });

const detailDrawer = reactive<{ open: boolean; record: VendorApplication | null }>({
  open: false,
  record: null
});

const rejectModal = reactive<{
  open: boolean;
  submitting: boolean;
  record: VendorApplication | null;
  remark: string;
}>({
  open: false,
  submitting: false,
  record: null,
  remark: ''
});

const formatDate = (value?: string | null) => {
  if (!value) return '-';
  return new Date(value).toLocaleString();
};

const statusMeta = (status: VendorApplicationStatus) => {
  switch (status) {
    case 'APPROVED':
      return { color: 'green', label: '已通过' };
    case 'REJECTED':
      return { color: 'red', label: '已驳回' };
    default:
      return { color: 'blue', label: '待审核' };
  }
};

const loadApplications = async () => {
  loading.value = true;
  try {
    applications.value = await listVendorApplications(filters.status);
  } catch (error) {
    console.error('加载厂商申请失败', error);
    message.error('加载申请失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const ensureReviewerId = () => {
  if (!auth.user?.id) {
    throw new Error('未获取当前用户，无法完成审核操作');
  }
  return auth.user.id;
};

const handleQuickApprove = async (record: VendorApplication) => {
  try {
    await approveVendorApplication(record.id, {
      reviewerId: ensureReviewerId(),
      remark: '审核通过'
    });
    message.success('已通过申请');
    detailDrawer.open = false;
    loadApplications();
  } catch (error: any) {
    console.error('审核通过失败', error);
    message.error(error?.response?.data?.message ?? '操作失败，请稍后重试');
  }
};

const openDetail = (record: VendorApplication) => {
  detailDrawer.record = record;
  detailDrawer.open = true;
};

const openReject = (record: VendorApplication) => {
  rejectModal.record = record;
  rejectModal.remark = '';
  rejectModal.open = true;
};

const handleReject = async () => {
  if (!rejectModal.record) {
    return;
  }
  rejectModal.submitting = true;
  try {
    await rejectVendorApplication(rejectModal.record.id, {
      reviewerId: ensureReviewerId(),
      remark: rejectModal.remark
    });
    message.success('已驳回申请');
    rejectModal.open = false;
    detailDrawer.open = false;
    loadApplications();
  } catch (error: any) {
    console.error('驳回申请失败', error);
    message.error(error?.response?.data?.message ?? '操作失败，请稍后重试');
  } finally {
    rejectModal.submitting = false;
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

.contact {
  display: flex;
  flex-direction: column;
}

.contact__info {
  color: #6b7280;
  font-size: 12px;
}
</style>
