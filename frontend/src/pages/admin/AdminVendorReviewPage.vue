<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>厂商入驻审核</h2>
        <p class="page-header__meta">审核厂商提交的入驻资料，审核通过后自动激活对应账号。</p>
      </div>
      <a-space>
        <a-select
          v-model:value="filters.status"
          style="width: 180px"
          allow-clear
          placeholder="全部状态"
          @change="loadApplications"
        >
          <a-select-option value="SUBMITTED">待审核</a-select-option>
          <a-select-option value="APPROVED">已通过</a-select-option>
          <a-select-option value="REJECTED">已驳回</a-select-option>
        </a-select>
        <a-button type="primary" @click="loadApplications" :loading="loading">刷新</a-button>
      </a-space>
    </div>

    <a-card>
      <a-table
        :data-source="applications"
        :loading="loading"
        :pagination="false"
        row-key="id"
      >
        <a-table-column title="公司" data-index="companyName" key="companyName" />
        <a-table-column title="联系人" key="contact">
          <template #default="{ record }">
            <div class="contact">
              <span>{{ record.contactName }}</span>
              <span class="contact__meta">{{ record.contactPhone }}</span>
            </div>
          </template>
        </a-table-column>
        <a-table-column title="状态" key="status">
          <template #default="{ record }">
            <a-tag :color="statusMeta(record.status).color">{{ statusMeta(record.status).label }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="提交时间" key="submittedAt">
          <template #default="{ record }">{{ formatDate(record.submittedAt) }}</template>
        </a-table-column>
        <a-table-column title="操作" key="actions">
          <template #default="{ record }">
            <a-space>
              <a-button size="small" @click="openDetail(record)">详情</a-button>
              <a-button
                size="small"
                type="primary"
                v-if="record.status === 'SUBMITTED'"
                @click="approve(record)"
              >通过</a-button>
              <a-button
                size="small"
                danger
                v-if="record.status === 'SUBMITTED'"
                @click="openReject(record)"
              >驳回</a-button>
            </a-space>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <a-drawer
      v-model:open="detailDrawer.open"
      title="申请详情"
      :width="520"
      destroy-on-close
    >
      <template v-if="detailDrawer.record">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="公司名称">{{ detailDrawer.record.companyName }}</a-descriptions-item>
          <a-descriptions-item label="统一社会信用代码">{{ detailDrawer.record.unifiedSocialCode }}</a-descriptions-item>
          <a-descriptions-item label="联系人">{{ detailDrawer.record.contactName }}</a-descriptions-item>
          <a-descriptions-item label="联系电话">{{ detailDrawer.record.contactPhone }}</a-descriptions-item>
          <a-descriptions-item label="联系邮箱" v-if="detailDrawer.record.contactEmail">
            {{ detailDrawer.record.contactEmail }}
          </a-descriptions-item>
          <a-descriptions-item label="所在地区" v-if="detailDrawer.record.city || detailDrawer.record.province">
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
      </template>
      <template #footer>
        <a-space v-if="detailDrawer.record?.status === 'SUBMITTED'">
          <a-button type="primary" @click="approve(detailDrawer.record)">审核通过</a-button>
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
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../../stores/auth';
import {
  listVendorApplications,
  approveVendorApplication,
  rejectVendorApplication,
  type VendorApplication,
  type VendorApplicationStatus
} from '../../services/vendorService';

const auth = useAuthStore();
const loading = ref(false);
const applications = ref<VendorApplication[]>([]);
const filters = reactive<{ status?: VendorApplicationStatus }>({ status: 'SUBMITTED' });

const detailDrawer = reactive<{ open: boolean; record: VendorApplication | null }>({
  open: false,
  record: null
});

const rejectModal = reactive<{ open: boolean; submitting: boolean; record: VendorApplication | null; remark: string }>(
  {
    open: false,
    submitting: false,
    record: null,
    remark: ''
  }
);

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

const formatDate = (value?: string | null) => {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString();
};

const reviewerId = () => {
  if (!auth.user?.id) {
    throw new Error('未找到当前管理员信息');
  }
  return auth.user.id;
};

const loadApplications = async () => {
  loading.value = true;
  try {
    applications.value = await listVendorApplications(filters.status);
  } catch (error) {
    console.error('Failed to load vendor applications', error);
    message.error('加载申请失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const openDetail = (record: VendorApplication) => {
  detailDrawer.record = record;
  detailDrawer.open = true;
};

const approve = async (record: VendorApplication | null) => {
  if (!record) {
    return;
  }
  try {
    await approveVendorApplication(record.id, { reviewerId: reviewerId(), remark: '审核通过' });
    message.success('已通过申请');
    detailDrawer.open = false;
    await loadApplications();
  } catch (error: any) {
    console.error('Approve vendor failed', error);
    message.error(error?.response?.data?.message ?? '操作失败');
  }
};

const openReject = (record: VendorApplication | null) => {
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
      reviewerId: reviewerId(),
      remark: rejectModal.remark
    });
    message.success('已驳回申请');
    rejectModal.open = false;
    detailDrawer.open = false;
    await loadApplications();
  } catch (error: any) {
    console.error('Reject vendor failed', error);
    message.error(error?.response?.data?.message ?? '操作失败');
  } finally {
    rejectModal.submitting = false;
  }
};

loadApplications();
</script>

<style scoped>
.contact {
  display: flex;
  flex-direction: column;
}

.contact__meta {
  font-size: 12px;
  color: #64748b;
}
</style>
