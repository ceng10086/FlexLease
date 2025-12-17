<template>
  <div class="dispute-panel">
    <div class="panel-header">
      <div>
        <p class="eyebrow">纠纷处理</p>
        <h3>选择预设方案或升级平台仲裁</h3>
      </div>
      <a-button
        type="primary"
        ghost
        size="small"
        :disabled="!canCreateDispute"
        @click="showCreateModal = true"
      >
        发起纠纷
      </a-button>
    </div>

    <a-alert
      v-if="!currentUserId"
      type="warning"
      show-icon
      message="登录后可提交或跟进纠纷"
    />

    <template v-else>
      <a-alert
        v-if="activeDispute && activeDispute.deadlineAt"
        type="info"
        show-icon
        class="mb-3"
      >
        <template #message>
          <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap">
            <span>存在进行中的纠纷，自动升级倒计时：</span>
            <a-statistic-countdown
              :value="new Date(activeDispute.deadlineAt).getTime()"
              format="D 天 H 时 m 分 s 秒"
              :value-style="{ fontSize: '14px', lineHeight: '1' }"
            />
          </div>
        </template>
      </a-alert>
      <a-empty v-if="!order.disputes.length" description="暂无纠纷记录" />
      <div v-else class="dispute-list">
        <a-card v-for="dispute in order.disputes" :key="dispute.id" size="small" class="dispute-card">
          <template #title>
            <div class="card-title">
              <span>#{{ dispute.id.slice(0, 8).toUpperCase() }}</span>
              <a-tag :color="disputeStatusColor(dispute.status)">
                {{ disputeStatusLabel(dispute.status) }}
              </a-tag>
            </div>
          </template>
          <div class="card-body">
            <div class="card-row">
              <span>发起人</span>
              <strong>{{ disputeActorLabel(dispute.initiatorRole) }}</strong>
            </div>
            <div class="card-row">
              <span>诉求</span>
              <strong>{{ disputeOptionLabel(dispute.initiatorOption) }}</strong>
            </div>
            <div class="card-row">
              <span>原因</span>
              <p>{{ dispute.initiatorReason }}</p>
            </div>
            <div class="card-row" v-if="dispute.initiatorPhoneMemo">
              <span>发起人电话纪要</span>
              <p>{{ dispute.initiatorPhoneMemo }}</p>
            </div>
            <div class="card-row" v-if="dispute.initiatorAttachmentProofIds?.length">
              <span>发起人关联凭证</span>
              <div class="proof-tags">
                <a-tag
                  v-for="proofId in dispute.initiatorAttachmentProofIds"
                  :key="proofId"
                  class="proof-tag"
                  @click="openProof(proofId)"
                >
                  {{ proofLabel(proofId) }}
                </a-tag>
              </div>
            </div>
            <div class="card-row" v-if="dispute.respondentRemark">
              <span>对方回应</span>
              <p>{{ dispute.respondentRemark }}</p>
            </div>
            <div class="card-row" v-if="dispute.respondentPhoneMemo">
              <span>对方电话纪要</span>
              <p>{{ dispute.respondentPhoneMemo }}</p>
            </div>
            <div class="card-row" v-if="dispute.respondentAttachmentProofIds?.length">
              <span>对方关联凭证</span>
              <div class="proof-tags">
                <a-tag
                  v-for="proofId in dispute.respondentAttachmentProofIds"
                  :key="proofId"
                  class="proof-tag"
                  @click="openProof(proofId)"
                >
                  {{ proofLabel(proofId) }}
                </a-tag>
              </div>
            </div>
            <div class="card-row" v-if="dispute.deadlineAt">
              <span>协商截止</span>
              <p>{{ formatDate(dispute.deadlineAt) }}</p>
            </div>
            <div class="card-row" v-if="dispute.adminDecisionRemark">
              <span>平台裁决</span>
              <p>{{ dispute.adminDecisionRemark }}</p>
            </div>
          </div>
          <div class="card-actions">
            <a-button
              v-if="canRespond(dispute)"
              size="small"
              @click="openRespond(dispute)"
            >
              回应方案
            </a-button>
            <a-button
              v-if="canEscalate(dispute)"
              type="primary"
              ghost
              size="small"
              @click="openActionModal('ESCALATE', dispute)"
            >
              升级平台仲裁
            </a-button>
            <a-button
              v-if="canAppeal(dispute)"
              size="small"
              danger
              ghost
              @click="openActionModal('APPEAL', dispute)"
            >
              申诉复核
            </a-button>
          </div>
        </a-card>
      </div>
    </template>
  </div>

  <a-modal
    v-model:open="showCreateModal"
    title="发起纠纷"
    ok-text="提交"
    :confirm-loading="createLoading"
    @ok="submitCreate"
  >
    <a-form layout="vertical">
      <a-form-item label="预设方案">
        <a-select v-model:value="createForm.option">
          <a-select-option v-for="option in disputeOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="原因">
        <a-textarea v-model:value="createForm.reason" :rows="3" placeholder="请描述问题" />
      </a-form-item>
      <a-form-item label="补充说明">
        <a-textarea v-model:value="createForm.remark" :rows="2" placeholder="可选" />
      </a-form-item>
      <a-form-item label="电话纪要">
        <a-input v-model:value="createForm.phoneMemo" placeholder="可选" />
      </a-form-item>
      <a-form-item label="关联凭证">
        <a-select
          v-model:value="createForm.attachmentProofIds"
          mode="multiple"
          :options="proofOptions"
          placeholder="可选择已上传的凭证"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="showRespondModal"
    title="回应纠纷方案"
    ok-text="提交"
    :confirm-loading="respondLoading"
    @ok="submitRespond"
  >
    <a-form layout="vertical">
      <a-form-item label="是否接受当前方案">
        <a-radio-group v-model:value="respondForm.accept">
          <a-radio :value="true">接受</a-radio>
          <a-radio :value="false">提出新方案</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item v-if="!respondForm.accept" label="新方案">
        <a-select v-model:value="respondForm.option">
          <a-select-option v-for="option in disputeOptions" :key="option.value" :value="option.value">
            {{ option.label }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="回复内容">
        <a-textarea v-model:value="respondForm.remark" :rows="3" placeholder="请输入补充说明" />
      </a-form-item>
      <a-form-item label="电话纪要">
        <a-input v-model:value="respondForm.phoneMemo" placeholder="可选" />
      </a-form-item>
      <a-form-item label="关联凭证">
        <a-select
          v-model:value="respondForm.attachmentProofIds"
          mode="multiple"
          :options="proofOptions"
          placeholder="可选择已上传的凭证"
        />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="actionModal.open"
    :title="actionModal.mode === 'ESCALATE' ? '升级平台仲裁' : '提交申诉'"
    ok-text="确认"
    :confirm-loading="actionLoading"
    @ok="submitActionModal"
  >
    <a-form layout="vertical">
      <a-form-item label="补充说明">
        <a-textarea v-model:value="actionModal.reason" :rows="3" placeholder="可选" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type {
  DisputeResolutionOption,
  OrderDispute,
  RentalOrderDetail
} from '../../services/orderService';
import {
  createOrderDispute,
  respondOrderDispute,
  escalateOrderDispute,
  appealOrderDispute
} from '../../services/orderService';
import {
  disputeOptions,
  disputeOptionLabel,
  disputeStatusColor,
  disputeStatusLabel,
  disputeActorLabel
} from '../../utils/disputes';
import { openProofInNewTab } from '../../services/proofService';

const props = defineProps<{
  order: RentalOrderDetail;
  currentUserId?: string | null;
}>();

const emit = defineEmits<{ (e: 'updated'): void }>();

const showCreateModal = ref(false);
const showRespondModal = ref(false);
const createLoading = ref(false);
const respondLoading = ref(false);
const respondingDispute = ref<OrderDispute | null>(null);
const actionLoading = ref(false);
const actionModal = reactive<{ mode: 'ESCALATE' | 'APPEAL' | null; open: boolean; dispute: OrderDispute | null; reason: string }>(
  {
    mode: null,
    open: false,
    dispute: null,
    reason: ''
  }
);

const proofOptions = computed(() =>
  (props.order.proofs ?? []).map((proof) => ({
    label: `${proof.proofType} · ${new Date(proof.uploadedAt).toLocaleString()}`,
    value: proof.id
  }))
);

const proofMap = computed(() => {
  const map = new Map<string, RentalOrderDetail['proofs'][number]>();
  (props.order.proofs ?? []).forEach((proof) => map.set(proof.id, proof));
  return map;
});

const proofLabel = (proofId: string) => {
  const proof = proofMap.value.get(proofId);
  if (!proof) {
    return proofId.slice(0, 8).toUpperCase();
  }
  return `${proof.proofType} · ${new Date(proof.uploadedAt).toLocaleString()}`;
};

const openProof = async (proofId: string) => {
  const proof = proofMap.value.get(proofId);
  if (!proof?.fileUrl) {
    message.warning('凭证链接不可用');
    return;
  }
  try {
    await openProofInNewTab(proof.fileUrl);
  } catch (error) {
    console.error(error);
    message.error('打开凭证失败');
  }
};

const createForm = reactive({
  option: disputeOptions[0].value as DisputeResolutionOption,
  reason: '',
  remark: '',
  phoneMemo: '',
  attachmentProofIds: [] as string[]
});

const respondForm = reactive({
  option: disputeOptions[0].value as DisputeResolutionOption,
  accept: true,
  remark: '',
  phoneMemo: '',
  attachmentProofIds: [] as string[]
});

const currentUserId = computed(() => props.currentUserId ?? null);
const isOrderOwner = computed(() => Boolean(currentUserId.value) && currentUserId.value === props.order.userId);
const activeDispute = computed(() =>
  props.order.disputes.find((dispute) =>
    dispute.status === 'OPEN' ||
    dispute.status === 'PENDING_ADMIN' ||
    dispute.status === 'RESOLVED' ||
    dispute.status === 'PENDING_REVIEW_PANEL'
  ) || null
);
const canCreateDispute = computed(
  () => Boolean(currentUserId.value) && isOrderOwner.value && !activeDispute.value
);

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '--');

const resetCreateForm = () => {
  createForm.option = disputeOptions[0].value as DisputeResolutionOption;
  createForm.reason = '';
  createForm.remark = '';
  createForm.phoneMemo = '';
  createForm.attachmentProofIds = [];
};

const requireUser = () => {
  if (!currentUserId.value) {
    message.error('缺少用户信息，无法提交');
    throw new Error('user-missing');
  }
  return currentUserId.value;
};

const submitCreate = async () => {
  if (!createForm.reason.trim()) {
    message.warning('请填写纠纷原因');
    return;
  }
  const actorId = requireUser();
  createLoading.value = true;
  try {
    await createOrderDispute(props.order.id, {
      actorId,
      option: createForm.option,
      reason: createForm.reason.trim(),
      remark: createForm.remark?.trim() || undefined,
      phoneMemo: createForm.phoneMemo?.trim() || undefined,
      attachmentProofIds: createForm.attachmentProofIds.length ? [...createForm.attachmentProofIds] : undefined
    });
    message.success('纠纷已提交');
    showCreateModal.value = false;
    resetCreateForm();
    emit('updated');
  } catch (error) {
    console.error(error);
    message.error('提交纠纷失败，请稍后重试');
  } finally {
    createLoading.value = false;
  }
};

const canRespond = (dispute: OrderDispute) => {
  if (!currentUserId.value || !isOrderOwner.value) {
    return false;
  }
  if (dispute.status !== 'OPEN') {
    return false;
  }
  if (!dispute.respondentRole) {
    return dispute.initiatorRole !== 'USER';
  }
  return dispute.respondentRole !== 'USER';
};

const canEscalate = (dispute: OrderDispute) =>
  Boolean(currentUserId.value) && isOrderOwner.value && dispute.status === 'OPEN';

const canAppeal = (dispute: OrderDispute) =>
  Boolean(currentUserId.value) &&
  dispute.status === 'CLOSED' &&
  dispute.appealCount === 0 &&
  (dispute.initiatorId === currentUserId.value || dispute.respondentId === currentUserId.value);

const openRespond = (dispute: OrderDispute) => {
  respondingDispute.value = dispute;
  respondForm.accept = true;
  respondForm.option = disputeOptions[0].value as DisputeResolutionOption;
  respondForm.remark = '';
  respondForm.phoneMemo = '';
  respondForm.attachmentProofIds = [];
  showRespondModal.value = true;
};

const submitRespond = async () => {
  if (!respondingDispute.value) {
    return;
  }
  const actorId = requireUser();
  if (!respondForm.accept && !respondForm.remark.trim()) {
    message.warning('请填写新方案说明');
    return;
  }
  respondLoading.value = true;
  try {
    await respondOrderDispute(props.order.id, respondingDispute.value.id, {
      actorId,
      option: respondForm.option,
      accept: respondForm.accept,
      remark: respondForm.remark?.trim() || undefined,
      phoneMemo: respondForm.phoneMemo?.trim() || undefined,
      attachmentProofIds: respondForm.attachmentProofIds.length
        ? [...respondForm.attachmentProofIds]
        : undefined
    });
    message.success('已提交回应');
    showRespondModal.value = false;
    emit('updated');
  } catch (error) {
    console.error(error);
    message.error('提交回应失败');
  } finally {
    respondLoading.value = false;
  }
};

const openActionModal = (mode: 'ESCALATE' | 'APPEAL', dispute: OrderDispute) => {
  actionModal.mode = mode;
  actionModal.dispute = dispute;
  actionModal.reason = '';
  actionModal.open = true;
};

const submitActionModal = async () => {
  if (!actionModal.dispute || !actionModal.mode) {
    return;
  }
  const actorId = requireUser();
  actionLoading.value = true;
  try {
    if (actionModal.mode === 'ESCALATE') {
      await escalateOrderDispute(props.order.id, actionModal.dispute.id, {
        actorId,
        reason: actionModal.reason?.trim() || undefined
      });
      message.success('已提交升级请求');
    } else {
      await appealOrderDispute(props.order.id, actionModal.dispute.id, {
        actorId,
        reason: actionModal.reason?.trim() || undefined
      });
      message.success('已提交申诉');
    }
    actionModal.open = false;
    emit('updated');
  } catch (error) {
    console.error(error);
    message.error('操作失败，请稍后重试');
  } finally {
    actionLoading.value = false;
  }
};
</script>

<style scoped>
.dispute-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--color-text-secondary);
}

.mb-3 {
  margin-bottom: 12px;
}

.dispute-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dispute-card {
  border-radius: 12px;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 14px;
}

.card-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-row span {
  font-size: 12px;
  color: var(--color-text-secondary);
  letter-spacing: 0.04em;
}

.card-actions {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.proof-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.proof-tag {
  cursor: pointer;
}
</style>
