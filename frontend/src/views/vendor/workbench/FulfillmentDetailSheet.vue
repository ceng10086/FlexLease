<template>
  <a-drawer
    :open="open"
    :width="drawerWidth"
    :height="drawerHeight"
    :placement="drawerPlacement"
    destroy-on-close
    title="订单履约详情"
    @close="emit('close')"
  >
    <a-spin :spinning="loading">
      <template v-if="order">
        <div class="sheet-grid">
          <div class="sheet-main">
            <PageSection title="基础信息">
              <div class="order-meta">
                <div>
                  <p>订单号</p>
                  <strong>{{ order.orderNo }}</strong>
                </div>
                <div>
                  <p>状态</p>
                  <a-tag>{{ order.status }}</a-tag>
                </div>
                <div>
                  <p>租金</p>
                  <strong>¥{{ formatCurrency(order.rentAmount) }}</strong>
                </div>
                <div>
                  <p>押金</p>
                  <strong>¥{{ formatCurrency(order.depositAmount) }}</strong>
                </div>
              </div>
            </PageSection>

            <PageSection title="操作面板">
              <div class="action-grid">
                <div class="action-card">
                  <h4>发货</h4>
                  <p>上传足量发货凭证后填写物流，平台将推送给消费者。</p>
                  <a-form layout="vertical">
                    <a-form-item label="承运方">
                      <a-input v-model:value="shipForm.carrier" placeholder="如 SF" />
                    </a-form-item>
                    <a-form-item label="运单号">
                      <a-input v-model:value="shipForm.trackingNumber" placeholder="物流单号" />
                    </a-form-item>
                    <a-form-item label="留言给用户">
                      <a-textarea v-model:value="shipForm.message" :rows="2" />
                    </a-form-item>
                    <small class="hint">{{ shipmentRequirementHint }}</small>
                    <a-button
                      type="primary"
                      :loading="shipForm.loading"
                      :disabled="!canSubmitShipment"
                      @click="handleShip"
                    >
                      提交发货
                    </a-button>
                  </a-form>
                </div>
                <div class="action-card" v-if="canRequestInspection">
                  <h4>巡检</h4>
                  <p>发起巡检请求后，用户上传巡检凭证可获得信用加分。</p>
                  <a-form layout="vertical">
                    <a-form-item label="巡检说明（选填）">
                      <a-textarea v-model:value="inspectionForm.remark" :rows="2" placeholder="例如：请拍摄设备外观与开机状态" />
                    </a-form-item>
                    <a-button type="primary" :loading="inspectionForm.loading" @click="handleRequestInspection">
                      发起巡检请求
                    </a-button>
                  </a-form>
                </div>
                <div class="action-card">
                  <h4>退租处理</h4>
                  <template v-if="canApproveReturn">
                    <p>审核用户的退租申请，确认后进入退租中。</p>
                    <a-space>
                      <a-button :loading="returnDecision.loading" @click="handleReturnDecision(false)">
                        拒绝
                      </a-button>
                      <a-button type="primary" :loading="returnDecision.loading" @click="handleReturnDecision(true)">
                        同意退租
                      </a-button>
                    </a-space>
                  </template>
                  <template v-else-if="canCompleteReturn">
                    <p>完成验收入库并填写退款金额。</p>
                    <a-input-number
                      v-model:value="returnCompletion.refundAmount"
                      :min="0"
                      :max="maxRefundableDeposit"
                      :precision="2"
                      style="width: 100%"
                      placeholder="押金退款金额"
                    />
                    <a-textarea v-model:value="returnCompletion.remark" placeholder="备注" :rows="2" />
                    <a-button type="primary" :loading="returnCompletion.loading" @click="handleReturnCompletion">
                      确认退租完成
                    </a-button>
                  </template>
                  <template v-else>
                    <p>当前阶段无需退租操作。</p>
                  </template>
                </div>
                <div class="action-card" v-if="pendingExtension">
                  <h4>续租审批</h4>
                  <p>申请延长 {{ pendingExtension.additionalMonths }} 个月</p>
                  <a-space>
                    <a-button :loading="extensionDecision.loading" @click="handleExtensionDecision(false)">
                      驳回
                    </a-button>
                    <a-button type="primary" :loading="extensionDecision.loading" @click="handleExtensionDecision(true)">
                      同意续租
                    </a-button>
                  </a-space>
                </div>
                <div class="action-card" v-if="isBuyoutRequested">
                  <h4>买断审批</h4>
                  <p v-if="order.buyoutAmount != null">买断金额：¥{{ formatCurrency(order.buyoutAmount) }}</p>
                  <p v-else class="text-muted">用户未填写买断金额。</p>
                  <a-space>
                    <a-button :loading="buyoutDecision.loading" @click="handleBuyoutDecision(false)">
                      驳回
                    </a-button>
                    <a-button type="primary" :loading="buyoutDecision.loading" @click="handleBuyoutDecision(true)">
                      同意买断
                    </a-button>
                  </a-space>
                </div>
              </div>
            </PageSection>

            <PageSection title="纠纷与仲裁">
              <template #actions>
                <a-button
                  type="primary"
                  ghost
                  size="small"
                  :disabled="!canCreateDispute"
                  @click="openCreateDisputeModal"
                >
                  发起纠纷
                </a-button>
              </template>
              <div v-if="disputes.length" class="dispute-list">
                <div v-for="item in disputes" :key="item.id" class="dispute-card">
                  <div class="dispute-card__header">
                    <div>
                      <strong>{{ disputeStatusLabel(item.status) }}</strong>
                      <p>发起人：{{ disputeActorLabel(item.initiatorRole) }}</p>
                    </div>
                    <a-tag :color="disputeStatusColor(item.status)">{{ item.status }}</a-tag>
                  </div>
                  <p>诉求：{{ disputeOptionLabel(item.initiatorOption) }} · {{ item.initiatorReason }}</p>
                  <p v-if="item.initiatorPhoneMemo">发起人电话纪要：{{ item.initiatorPhoneMemo }}</p>
                  <div v-if="item.initiatorAttachmentProofIds?.length" class="dispute-proof-tags">
                    <span class="label">发起人凭证：</span>
                    <a-tag
                      v-for="proofId in item.initiatorAttachmentProofIds"
                      :key="proofId"
                      class="proof-tag"
                      @click="openProof(proofId)"
                    >
                      {{ proofLabel(proofId) }}
                    </a-tag>
                  </div>
                  <p v-if="item.respondentRemark">对方回应：{{ item.respondentRemark }}</p>
                  <p v-if="item.respondentPhoneMemo">对方电话纪要：{{ item.respondentPhoneMemo }}</p>
                  <div v-if="item.respondentAttachmentProofIds?.length" class="dispute-proof-tags">
                    <span class="label">对方凭证：</span>
                    <a-tag
                      v-for="proofId in item.respondentAttachmentProofIds"
                      :key="proofId"
                      class="proof-tag"
                      @click="openProof(proofId)"
                    >
                      {{ proofLabel(proofId) }}
                    </a-tag>
                  </div>
                  <div v-if="(item.status === 'OPEN' || item.status === 'PENDING_ADMIN') && item.deadlineAt" class="dispute-countdown">
                    <ClockCircleOutlined />
                    <span class="label">剩余处理时间：</span>
                    <a-statistic-countdown
                      :value="new Date(item.deadlineAt).getTime()"
                      format="D 天 H 时 m 分"
                      :value-style="{ fontSize: '14px', color: 'inherit', lineHeight: 1 }"
                    />
                  </div>
                  <div v-if="canRespondDispute(item) && disputeForms[item.id]" class="dispute-actions">
                    <a-radio-group
                      v-model:value="disputeForms[item.id].accept"
                      size="small"
                      button-style="solid"
                      class="dispute-actions__choices"
                    >
                      <a-radio-button :value="true">接受方案</a-radio-button>
                      <a-radio-button :value="false">提出新方案</a-radio-button>
                    </a-radio-group>
                    <a-select
                      v-if="!disputeForms[item.id].accept"
                      v-model:value="disputeForms[item.id].option"
                      :options="disputeOptions"
                      style="width: 200px"
                    />
                    <a-input
                      v-model:value="disputeForms[item.id].remark"
                      placeholder="补充说明"
                    />
                    <a-input
                      v-model:value="disputeForms[item.id].phoneMemo"
                      placeholder="电话纪要（可选）"
                    />
                    <a-select
                      v-model:value="disputeForms[item.id].attachmentProofIds"
                      mode="multiple"
                      :options="proofOptions"
                      placeholder="关联凭证（可选）"
                      style="width: 100%"
                    />
                    <a-button
                      type="primary"
                      :loading="disputeForms[item.id].loading"
                      @click="handleRespondDispute(item)"
                    >
                      回复
                    </a-button>
                    <a-button type="link" @click="handleEscalateDispute(item)">升级平台</a-button>
                  </div>
                  <div v-else-if="canEscalateDispute(item)" class="dispute-actions">
                    <a-button type="link" @click="handleEscalateDispute(item)">升级平台</a-button>
                    <span class="hint">当前轮到对方回应，必要时可直接请求平台介入。</span>
                  </div>
                  <div v-else-if="canAppealDispute(item)" class="dispute-actions">
                    <a-button type="default" danger ghost @click="openAppealModal(item)">
                      申诉复核
                    </a-button>
                    <span class="hint">每个纠纷仅可申诉一次，提交后将由复核组裁决。</span>
                  </div>
                </div>
              </div>
              <p v-else class="text-muted">暂无纠纷。</p>
            </PageSection>

            <PageSection title="操作时间线">
              <TimelineList :events="order.events" />
            </PageSection>
            <PageSection title="满意度调查">
              <OrderSurveyPanel
                :order="order"
                :current-user-id="auth.user?.id ?? null"
                :target-role="'VENDOR'"
                :target-ref="order.vendorId"
                @updated="loadOrder"
              />
            </PageSection>
          </div>
          <div class="sheet-side">
            <PageSection title="沟通记录">
              <OrderChatPanel
                :messages="chatEvents"
                :sending="chatSending"
                self-role="VENDOR"
                :quick-phrases="vendorQuickPhrases"
                @send="handleSendMessage"
              />
            </PageSection>
            <PageSection title="凭证">
              <ProofGallery :proofs="order.proofs" @preview="previewProof" />
              <ProofUploader
                :allowed-types="vendorProofTypes"
                :disabled="!order || !auth.user"
                :disabled-reason="!auth.user ? '请先登录后上传凭证。' : !order ? '请先选择订单后上传凭证。' : null"
                :upload-handler="handleUploadProof"
              />
              <p class="hint">{{ proofPolicyHint }}</p>
            </PageSection>
          </div>
        </div>
      </template>
      <DataStateBlock v-else-if="!loading" title="请选择订单" description="在左侧列表中选择订单以查看履约详情" />
    </a-spin>
  </a-drawer>
  <a-modal
    v-model:open="createDisputeModal.open"
    title="发起纠纷"
    ok-text="提交"
    :confirm-loading="createDisputeModal.loading"
    @ok="handleSubmitCreateDispute"
    @cancel="createDisputeModal.open = false"
  >
    <a-form layout="vertical">
      <a-form-item label="预设方案">
        <a-select v-model:value="createDisputeForm.option" :options="disputeOptions" />
      </a-form-item>
      <a-form-item label="纠纷原因" required>
        <a-textarea
          v-model:value="createDisputeForm.reason"
          :rows="3"
          placeholder="请描述问题与期望处理方式"
        />
      </a-form-item>
      <a-form-item label="补充说明（可选）">
        <a-input v-model:value="createDisputeForm.remark" placeholder="例如：希望补发配件/需要退款等" />
      </a-form-item>
      <a-form-item label="电话纪要（可选）">
        <a-input v-model:value="createDisputeForm.phoneMemo" placeholder="例如：已电话沟通约定 xx" />
      </a-form-item>
      <a-form-item label="关联凭证（可选）">
        <a-select
          v-model:value="createDisputeForm.attachmentProofIds"
          mode="multiple"
          :options="proofOptions"
          placeholder="选择要关联到纠纷的凭证"
        />
      </a-form-item>
    </a-form>
  </a-modal>
  <a-modal
    v-model:open="appealModal.open"
    title="提交申诉"
    ok-text="提交"
    :confirm-loading="appealModal.loading"
    @ok="handleSubmitAppeal"
    @cancel="appealModal.open = false"
  >
    <a-form layout="vertical">
      <a-form-item label="申诉说明">
        <a-textarea
          v-model:value="appealModal.reason"
          :rows="3"
          placeholder="请补充需要复核的理由，系统将同步时间线"
        />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import { ClockCircleOutlined } from '@ant-design/icons-vue';
import PageSection from '../../../components/layout/PageSection.vue';
import DataStateBlock from '../../../components/feedback/DataStateBlock.vue';
import OrderChatPanel from '../../../components/chat/OrderChatPanel.vue';
import ProofGallery from '../../../components/proof/ProofGallery.vue';
import ProofUploader from '../../../components/proof/ProofUploader.vue';
import TimelineList from '../../../components/timeline/TimelineList.vue';
import OrderSurveyPanel from '../../../components/orders/OrderSurveyPanel.vue';
import { useAuthStore } from '../../../stores/auth';
import { useViewport } from '../../../composables/useViewport';
import {
  fetchOrder,
  shipOrder,
  requestOrderInspection,
  decideOrderReturn,
  completeOrderReturn,
  decideOrderExtension,
  decideOrderBuyout,
  uploadOrderProof,
  postOrderMessage,
  fetchProofPolicy,
  createOrderDispute,
  respondOrderDispute,
  escalateOrderDispute,
  appealOrderDispute,
  type RentalOrderDetail,
  type OrderDispute,
  type DisputeResolutionOption,
  type ProofPolicySummary,
  type OrderProofType
} from '../../../services/orderService';
import { friendlyErrorMessage } from '../../../utils/error';
import { openProofInNewTab } from '../../../services/proofService';
import { formatCurrency } from '../../../utils/number';
import { disputeOptions, disputeOptionLabel, disputeStatusColor, disputeStatusLabel, disputeActorLabel } from '../../../utils/disputes';
import type { ChatSendPayload } from '../../../types/chat';

const props = defineProps<{
  open: boolean;
  vendorId: string | null;
  orderId: string | null;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'refresh-list'): void;
}>();

const auth = useAuthStore();
const { width: viewportWidth, isMobile } = useViewport();
const order = ref<RentalOrderDetail | null>(null);
const loading = ref(false);
const proofPolicy = ref<ProofPolicySummary | null>(null);
const vendorProofTypes: OrderProofType[] = ['SHIPMENT', 'RETURN', 'INSPECTION', 'OTHER'];

const shipForm = reactive({ carrier: '', trackingNumber: '', message: '', loading: false });
const inspectionForm = reactive({ remark: '', loading: false });
const returnDecision = reactive({ loading: false });
const returnCompletion = reactive({ refundAmount: 0, remark: '', loading: false });
const extensionDecision = reactive({ loading: false });
const buyoutDecision = reactive({ loading: false });
const chatSending = ref(false);
const disputeForms = reactive<Record<string, {
  option: DisputeResolutionOption;
  accept: boolean;
  remark: string;
  phoneMemo: string;
  attachmentProofIds: string[];
  loading: boolean;
}>>({});
const createDisputeModal = reactive<{ open: boolean; loading: boolean }>({ open: false, loading: false });
const createDisputeForm = reactive<{
  option: DisputeResolutionOption;
  reason: string;
  remark: string;
  phoneMemo: string;
  attachmentProofIds: string[];
}>({
  option: disputeOptions[0].value as DisputeResolutionOption,
  reason: '',
  remark: '',
  phoneMemo: '',
  attachmentProofIds: []
});
const appealModal = reactive<{ open: boolean; target: OrderDispute | null; reason: string; loading: boolean }>({
  open: false,
  target: null,
  reason: '',
  loading: false
});

const vendorQuickPhrases = [
  '已收到您的申请，我们会在 2 小时内回复。',
  '货物已安排发出，稍后同步物流单号。',
  '请按凭证指引补充照片或视频资料，谢谢配合。'
];

const loadOrder = async () => {
  if (!props.orderId) {
    order.value = null;
    return;
  }
  loading.value = true;
  try {
    order.value = await fetchOrder(props.orderId);
    shipForm.carrier = order.value?.shippingCarrier ?? '';
    shipForm.trackingNumber = order.value?.shippingTrackingNo ?? '';
    returnCompletion.refundAmount = order.value?.depositAmount ?? 0;
    order.value?.disputes?.forEach((item) => {
      if (!disputeForms[item.id]) {
        disputeForms[item.id] = {
          option: item.respondentOption ?? item.initiatorOption,
          accept: true,
          remark: '',
          phoneMemo: '',
          attachmentProofIds: [],
          loading: false
        };
      }
    });
  } catch (error) {
    order.value = null;
    message.error(friendlyErrorMessage(error, '加载订单失败'));
  } finally {
    loading.value = false;
  }
};

watch(
  () => props.open,
  (val) => {
    if (val) {
      loadOrder();
    } else {
      order.value = null;
    }
  }
);

watch(
  () => props.orderId,
  (id, prev) => {
    if (props.open && id && id !== prev) {
      loadOrder();
    }
  }
);

const ensureContext = () => {
  if (!props.vendorId || !order.value) {
    message.warning('缺少厂商身份或订单');
    return false;
  }
  return true;
};

const canShip = computed(() => order.value?.status === 'AWAITING_SHIPMENT');
const canRequestInspection = computed(() => order.value?.status === 'IN_LEASE');
const canApproveReturn = computed(() => order.value?.status === 'RETURN_REQUESTED');
const canCompleteReturn = computed(() => order.value?.status === 'RETURN_IN_PROGRESS');
const pendingExtension = computed(
  () => order.value?.extensions?.find((item) => item.status === 'PENDING') ?? null
);
const isBuyoutRequested = computed(() => order.value?.status === 'BUYOUT_REQUESTED');
const maxRefundableDeposit = computed(() => order.value?.depositAmount ?? 0);

const chatEvents = computed(() => {
  const events = order.value?.events ?? [];
  return events.filter((event) => event.eventType?.includes('MESSAGE') || event.eventType === 'COMMUNICATION_NOTE');
});

const disputes = computed(() => order.value?.disputes ?? []);
const activeDispute = computed(() => disputes.value.find((item) => item.status !== 'CLOSED') ?? null);
const canCreateDispute = computed(() => Boolean(order.value) && Boolean(auth.user?.id) && !activeDispute.value);

const proofList = computed(() => order.value?.proofs ?? []);
const proofOptions = computed(() =>
  proofList.value.map((proof) => ({
    label: `${proof.proofType} · ${new Date(proof.uploadedAt).toLocaleString()}`,
    value: proof.id
  }))
);

const proofMap = computed(() => {
  const map = new Map<string, (typeof proofList.value)[number]>();
  proofList.value.forEach((proof) => map.set(proof.id, proof));
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
    message.error(friendlyErrorMessage(error, '无法打开预览'));
  }
};

const resetCreateDisputeForm = () => {
  createDisputeForm.option = disputeOptions[0].value as DisputeResolutionOption;
  createDisputeForm.reason = '';
  createDisputeForm.remark = '';
  createDisputeForm.phoneMemo = '';
  createDisputeForm.attachmentProofIds = [];
};

const openCreateDisputeModal = () => {
  if (!canCreateDispute.value) {
    return;
  }
  resetCreateDisputeForm();
  createDisputeModal.open = true;
};

const drawerWidth = computed(() => {
  if (isMobile.value) {
    return '100%';
  }
  const margin = 80;
  const desired = Math.min(1100, viewportWidth.value - margin);
  return Math.max(720, desired);
});
const drawerPlacement = computed(() => (isMobile.value ? 'bottom' : 'right'));
const drawerHeight = computed(() => (isMobile.value ? '100%' : undefined));

const isImageProof = (url: string, contentType?: string | null) => {
  if (contentType && contentType.startsWith('image/')) return true;
  const lower = url.toLowerCase();
  return lower.endsWith('.jpg') || lower.endsWith('.jpeg') || lower.endsWith('.png');
};

const isVideoProof = (url: string, contentType?: string | null) => {
  if (contentType && contentType.startsWith('video/')) return true;
  const lower = url.toLowerCase();
  return lower.endsWith('.mp4') || lower.endsWith('.mov') || lower.endsWith('.m4v');
};

const shipmentRequirement = computed(() => ({
  photos: order.value?.shipmentPhotoRequired ?? 0,
  videos: order.value?.shipmentVideoRequired ?? 0
}));

const shipmentProofStats = computed(() => {
  const shipments = proofList.value.filter(
    (item) => item.proofType === 'SHIPMENT' && item.actorRole === 'VENDOR'
  );
  return {
    photos: shipments.filter((item) => isImageProof(item.fileUrl, item.contentType)).length,
    videos: shipments.filter((item) => isVideoProof(item.fileUrl, item.contentType)).length
  };
});

const meetsShipmentProofRequirement = computed(() =>
  shipmentProofStats.value.photos >= shipmentRequirement.value.photos &&
  shipmentProofStats.value.videos >= shipmentRequirement.value.videos
);

const canSubmitShipment = computed(() => canShip.value && meetsShipmentProofRequirement.value);

const shipmentRequirementHint = computed(() => {
  const requirement = shipmentRequirement.value;
  if (!requirement.photos && !requirement.videos) {
    return '无需额外凭证即可提交发货。';
  }
  const stats = shipmentProofStats.value;
  return `需至少 ${requirement.photos} 张照片、${requirement.videos} 段视频（当前 ${stats.photos}/${requirement.photos}、${stats.videos}/${requirement.videos}）`;
});

const proofPolicyHint = computed(() => {
  if (!proofPolicy.value) {
    return '按阶段上传凭证将自动同步到用户抽屉。';
  }
  const stage = proofPolicy.value;
  return `平台要求：发货至少 ${stage.shipment.photosRequired} 张照片 / ${stage.shipment.videosRequired} 段视频，收货 ${stage.receive.photosRequired} 张 / ${stage.receive.videosRequired} 段，退租 ${stage.returns.photosRequired} 张 / ${stage.returns.videosRequired} 段。`;
});

const handleShip = async () => {
  if (!ensureContext()) {
    return;
  }
  shipForm.loading = true;
  try {
    const updated = await shipOrder(order.value!.id, {
      vendorId: props.vendorId!,
      carrier: shipForm.carrier,
      trackingNumber: shipForm.trackingNumber,
      message: shipForm.message
    });
    order.value = updated;
    message.success('已提交发货');
    emit('refresh-list');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '提交失败'));
  } finally {
    shipForm.loading = false;
  }
};

const handleRequestInspection = async () => {
  if (!ensureContext() || !order.value) {
    return;
  }
  inspectionForm.loading = true;
  try {
    const updated = await requestOrderInspection(order.value.id, {
      vendorId: props.vendorId!,
      remark: inspectionForm.remark || undefined
    });
    order.value = updated;
    inspectionForm.remark = '';
    message.success('已发起巡检请求');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '操作失败'));
  } finally {
    inspectionForm.loading = false;
  }
};

const handleReturnDecision = async (approve: boolean) => {
  if (!ensureContext()) {
    return;
  }
  returnDecision.loading = true;
  try {
    const updated = await decideOrderReturn(order.value!.id, {
      vendorId: props.vendorId!,
      approve,
      remark: approve ? undefined : '需补充信息'
    });
    order.value = updated;
    message.success(approve ? '已同意退租' : '已拒绝退租');
    emit('refresh-list');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '操作失败'));
  } finally {
    returnDecision.loading = false;
  }
};

const handleReturnCompletion = async () => {
  if (!ensureContext()) {
    return;
  }
  returnCompletion.loading = true;
  try {
    const updated = await completeOrderReturn(order.value!.id, {
      vendorId: props.vendorId!,
      remark: returnCompletion.remark || undefined,
      refundAmount: returnCompletion.refundAmount
    });
    order.value = updated;
    message.success('退租完成');
    emit('refresh-list');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '操作失败'));
  } finally {
    returnCompletion.loading = false;
  }
};

const handleExtensionDecision = async (approve: boolean) => {
  if (!ensureContext() || !pendingExtension.value) {
    return;
  }
  extensionDecision.loading = true;
  try {
    const updated = await decideOrderExtension(order.value!.id, {
      vendorId: props.vendorId!,
      approve,
      remark: approve ? undefined : '暂不支持'
    });
    order.value = updated;
    emit('refresh-list');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '操作失败'));
  } finally {
    extensionDecision.loading = false;
  }
};

const handleBuyoutDecision = async (approve: boolean) => {
  if (!ensureContext()) {
    return;
  }
  buyoutDecision.loading = true;
  try {
    const updated = await decideOrderBuyout(order.value!.id, {
      vendorId: props.vendorId!,
      approve,
      remark: approve ? undefined : '暂不支持买断'
    });
    order.value = updated;
    emit('refresh-list');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '操作失败'));
  } finally {
    buyoutDecision.loading = false;
  }
};

const handleUploadProof = async (payload: { proofType: OrderProofType; description?: string; file: File }) => {
  if (!order.value) {
    throw new Error('请先选择订单');
  }
  if (!auth.user) {
    throw new Error('请先登录');
  }
  try {
    await uploadOrderProof(order.value.id, {
      actorId: auth.user.id,
      proofType: payload.proofType,
      description: payload.description,
      file: payload.file
    });
    message.success('凭证已上传');
    loadOrder();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '上传失败'));
  }
};

const handleSendMessage = async (payload: ChatSendPayload) => {
  if (!order.value || !auth.user) {
    return;
  }
  const trimmed = payload.content.trim();
  const hasAttachments = payload.attachments.length > 0;
  if (!trimmed && !hasAttachments) {
    return;
  }
  chatSending.value = true;
  try {
    let attachmentSummary = '';
    if (hasAttachments) {
      const uploads = await Promise.all(
        payload.attachments.map((file) =>
          uploadOrderProof(order.value!.id, {
            actorId: auth.user!.id,
            proofType: 'OTHER',
            description: trimmed || file.name,
            file
          })
        )
      );
      const lines = uploads.map((proof, index) => `• ${payload.attachments[index].name}: ${proof.fileUrl}`);
      attachmentSummary = [`已附加 ${uploads.length} 个文件`, ...lines].join('\n');
    }
    const finalMessage = [trimmed, attachmentSummary].filter((segment) => segment && segment.trim()).join('\n\n');
    if (!finalMessage) {
      chatSending.value = false;
      return;
    }
    const updated = await postOrderMessage(order.value.id, {
      actorId: auth.user.id,
      message: finalMessage
    });
    order.value = updated;
    message.success('已发送');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '发送失败'));
  } finally {
    chatSending.value = false;
  }
};

const canRespondDispute = (item: OrderDispute) => {
  if (item.status !== 'OPEN') {
    return false;
  }
  if (!item.respondentRole) {
    return item.initiatorRole !== 'VENDOR';
  }
  return item.respondentRole !== 'VENDOR';
};

const canEscalateDispute = (item: OrderDispute) => item.status === 'OPEN';
const canAppealDispute = (item: OrderDispute) =>
  item.status === 'CLOSED' &&
  item.appealCount === 0 &&
  Boolean(auth.user?.id) &&
  (item.initiatorId === auth.user?.id || item.respondentId === auth.user?.id);

const handleRespondDispute = async (item: OrderDispute) => {
  if (!order.value || !auth.user?.id) {
    return;
  }
  const form = disputeForms[item.id];
  if (!form) {
    return;
  }
  if (!form.accept && !form.remark.trim()) {
    message.warning('请填写新方案说明');
    return;
  }
  form.loading = true;
  try {
    await respondOrderDispute(order.value.id, item.id, {
      actorId: auth.user.id,
      option: form.option,
      accept: form.accept,
      remark: form.remark?.trim() || undefined,
      phoneMemo: form.phoneMemo?.trim() || undefined,
      attachmentProofIds: form.attachmentProofIds.length ? form.attachmentProofIds : undefined
    });
    message.success(form.accept ? '已接受用户方案' : '已提交新方案');
    form.remark = '';
    form.phoneMemo = '';
    form.attachmentProofIds = [];
    loadOrder();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '回复失败'));
  } finally {
    form.loading = false;
  }
};

const handleSubmitCreateDispute = async () => {
  if (!order.value || !auth.user?.id) {
    return;
  }
  if (!createDisputeForm.reason.trim()) {
    message.warning('请填写纠纷原因');
    return;
  }
  createDisputeModal.loading = true;
  try {
    await createOrderDispute(order.value.id, {
      actorId: auth.user.id,
      option: createDisputeForm.option,
      reason: createDisputeForm.reason.trim(),
      remark: createDisputeForm.remark?.trim() || undefined,
      phoneMemo: createDisputeForm.phoneMemo?.trim() || undefined,
      attachmentProofIds: createDisputeForm.attachmentProofIds.length
        ? [...createDisputeForm.attachmentProofIds]
        : undefined
    });
    message.success('纠纷已提交');
    createDisputeModal.open = false;
    loadOrder();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '提交纠纷失败'));
  } finally {
    createDisputeModal.loading = false;
  }
};

const handleEscalateDispute = async (item: OrderDispute) => {
  if (!order.value || !auth.user?.id) {
    return;
  }
  try {
    await escalateOrderDispute(order.value.id, item.id, { actorId: auth.user.id, reason: '需要平台介入' });
    message.success('已升级平台');
    loadOrder();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '升级失败'));
  }
};

const openAppealModal = (item: OrderDispute) => {
  if (!canAppealDispute(item)) {
    return;
  }
  appealModal.target = item;
  appealModal.reason = '';
  appealModal.open = true;
};

const handleSubmitAppeal = async () => {
  if (!order.value || !auth.user?.id || !appealModal.target) {
    return;
  }
  appealModal.loading = true;
  try {
    await appealOrderDispute(order.value.id, appealModal.target.id, {
      actorId: auth.user.id,
      reason: appealModal.reason?.trim() || undefined
    });
    message.success('申诉已提交');
    appealModal.open = false;
    loadOrder();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '申诉失败'));
  } finally {
    appealModal.loading = false;
  }
};

const previewProof = async (proof: { fileUrl: string }) => {
  try {
    await openProofInNewTab(proof.fileUrl);
  } catch (error) {
    message.error(friendlyErrorMessage(error, '无法打开预览'));
  }
};

fetchProofPolicy()
  .then((policy) => {
    proofPolicy.value = policy;
  })
  .catch(() => {
    proofPolicy.value = null;
  });
</script>

<style scoped>
.sheet-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: var(--space-4);
}

.order-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: var(--space-3);
}

.order-meta p {
  margin: 0;
  color: var(--color-text-secondary);
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: var(--space-3);
}

.action-card {
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: var(--radius-card);
  padding: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.hint {
  color: var(--color-text-secondary);
  font-size: var(--font-size-caption);
}

.dispute-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.dispute-card {
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: var(--radius-card);
  padding: var(--space-3);
}

.dispute-card__header {
  display: flex;
  justify-content: space-between;
}

.dispute-card__header p {
  margin: 0;
}

.dispute-actions {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
  margin-top: var(--space-2);
}

.dispute-actions__choices {
  display: flex;
  gap: var(--space-1);
  flex-wrap: wrap;
}

.sheet-side {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

@media (max-width: 1024px) {
  .sheet-grid {
    grid-template-columns: 1fr;
  }
}

.dispute-countdown {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  color: var(--color-warning);
  font-size: 13px;
  font-weight: 500;
}

.dispute-countdown .label {
  margin-right: -4px;
}

.dispute-proof-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.dispute-proof-tags .label {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.proof-tag {
  cursor: pointer;
}
</style>
