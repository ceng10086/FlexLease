<template>
  <div class="page-container" v-if="order">
    <div class="page-header">
      <div>
        <h2>订单详情</h2>
        <p class="page-header__meta">订单号：{{ order.orderNo }}</p>
      </div>
      <a-button type="default" @click="goBack">返回</a-button>
    </div>

    <div class="page-guidance order-guidance">
      <div class="page-guidance__title">操作提醒</div>
      <div class="page-guidance__content">
        - 确认收货后订单进入在租状态，若收货信息异常请勿立即确认；<br>
        - 续租、退租、买断申请提交后平台会通知厂商处理，可在操作记录中查看结果；<br>
        - 所有合同与支付记录均可在此页面查看，建议在关键步骤截图留存。
      </div>
    </div>

    <a-row :gutter="24">
      <a-col :xs="24" :lg="16">
        <a-card title="基础信息">
          <a-descriptions :column="2" bordered size="small">
            <a-descriptions-item label="状态">{{ order.status }}</a-descriptions-item>
            <a-descriptions-item label="厂商">{{ order.vendorId }}</a-descriptions-item>
            <a-descriptions-item label="押金">¥{{ formatCurrency(rentalOrderDeposit(order)) }}</a-descriptions-item>
            <a-descriptions-item label="租金">¥{{ formatCurrency(rentalOrderRent(order)) }}</a-descriptions-item>
            <a-descriptions-item label="总金额">¥{{ formatCurrency(rentalOrderTotal(order)) }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatDate(order.createdAt) }}</a-descriptions-item>
            <a-descriptions-item label="开始日期" v-if="order.leaseStartAt">{{ formatDate(order.leaseStartAt) }}</a-descriptions-item>
            <a-descriptions-item label="结束日期" v-if="order.leaseEndAt">{{ formatDate(order.leaseEndAt) }}</a-descriptions-item>
            <a-descriptions-item label="承运方" v-if="order.shippingCarrier">{{ order.shippingCarrier }}</a-descriptions-item>
            <a-descriptions-item label="运单号" v-if="order.shippingTrackingNo">{{ order.shippingTrackingNo }}</a-descriptions-item>
          </a-descriptions>
        </a-card>

        <a-card title="租赁明细" class="mt-16">
          <a-table :data-source="order.items" :pagination="false" row-key="id">
            <a-table-column title="商品" data-index="productName" key="product" />
            <a-table-column title="SKU" data-index="skuCode" key="sku" />
            <a-table-column title="数量" data-index="quantity" key="quantity" />
            <a-table-column title="月租金" key="rent">
              <template #default="{ record }">¥{{ formatCurrency(resolveItemRent(record)) }}</template>
            </a-table-column>
            <a-table-column title="押金" key="deposit">
              <template #default="{ record }">¥{{ formatCurrency(resolveItemDeposit(record)) }}</template>
            </a-table-column>
          </a-table>
        </a-card>

        <a-card title="操作记录" class="mt-16">
          <a-empty v-if="!order.events?.length" description="暂无记录" />
          <a-timeline v-else>
            <a-timeline-item v-for="item in order.events" :key="item.id">
              <div class="timeline-item">
                <strong>{{ item.eventType }}</strong>
                <span>{{ formatDate(item.createdAt) }}</span>
                <p v-if="item.description">{{ item.description }}</p>
              </div>
            </a-timeline-item>
          </a-timeline>
        </a-card>

        <a-card title="取证资料" class="mt-16">
          <template v-if="!proofList.length">
            <a-empty description="暂未上传取证资料" />
          </template>
          <template v-else>
            <div class="proof-grid">
              <div class="proof-item" v-for="item in proofList" :key="item.id">
                <div class="proof-item__meta">
                  <strong>{{ proofTypeLabel(item.proofType) }}</strong>
                  <span>{{ formatDate(item.uploadedAt) }}</span>
                </div>
                <p class="proof-item__desc">{{ item.description || '未填写说明' }}</p>
                <a href="#" @click.prevent="handleDownloadProof(item)">查看文件</a>
              </div>
            </div>
          </template>
          <a-divider />
          <a-form layout="vertical">
            <a-form-item label="取证类型">
              <a-select v-model:value="proofForm.type">
                <a-select-option v-for="option in proofTypeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="补充说明">
              <a-textarea v-model:value="proofForm.description" :rows="2" placeholder="例如：包装外观、快递单号" />
            </a-form-item>
            <a-form-item label="上传文件">
              <input
                type="file"
                :key="proofForm.inputKey"
                @change="handleProofFileChange"
              />
              <div class="proof-file-hint" v-if="proofForm.file">{{ proofForm.file.name }}</div>
            </a-form-item>
            <a-button type="primary" block :loading="proofForm.uploading" @click="handleProofUpload">
              上传取证资料
            </a-button>
          </a-form>
        </a-card>

        <a-card title="沟通记录" class="mt-16">
          <a-empty v-if="!conversationEvents.length" description="暂无留言" />
          <div v-else class="conversation-list">
            <div class="conversation-item" v-for="item in conversationEvents" :key="item.id">
              <div class="conversation-item__meta">
                <strong>{{ resolveActorLabel(item) }}</strong>
                <span>{{ formatDate(item.createdAt) }}</span>
              </div>
              <p>{{ item.description }}</p>
            </div>
          </div>
          <a-divider />
          <a-form layout="vertical">
            <a-form-item label="我要留言">
              <a-textarea v-model:value="conversationForm.message" :rows="3" placeholder="例如：想了解物流进度" />
            </a-form-item>
            <a-button type="primary" block :loading="conversationForm.loading" @click="handleSendMessage">
              发送留言
            </a-button>
          </a-form>
        </a-card>

        <a-card title="纠纷与仲裁" class="mt-16">
          <div class="dispute-header">
            <span>记录纠纷详情并邀请平台介入，保障押金处理透明。</span>
            <a-button size="small" type="primary" @click="showDisputeModal" :disabled="!canCreateDispute">
              发起纠纷
            </a-button>
          </div>
          <a-empty v-if="!disputes.length" description="暂无纠纷记录" />
          <div v-else class="dispute-list">
            <div class="dispute-item" v-for="item in disputes" :key="item.id">
              <div class="dispute-item__header">
                <a-tag :color="statusColor(item.status)">{{ formatDisputeStatus(item.status) }}</a-tag>
                <span>发起人：{{ disputeActorLabel(item.initiatorRole) }}</span>
                <span>创建时间：{{ formatDate(item.createdAt) }}</span>
              </div>
              <p class="dispute-item__reason">
                <strong>诉求：</strong>{{ resolutionLabel(item.initiatorOption) }} | {{ item.initiatorReason }}
              </p>
              <p v-if="item.respondentOption" class="dispute-item__line">
                对方建议：{{ resolutionLabel(item.respondentOption) }}
                <span v-if="item.respondentRemark">（{{ item.respondentRemark }}）</span>
              </p>
              <p v-if="item.adminDecisionOption" class="dispute-item__line">
                平台裁决：{{ resolutionLabel(item.adminDecisionOption) }}
                <span v-if="item.adminDecisionRemark">（{{ item.adminDecisionRemark }}）</span>
              </p>
              <div class="dispute-meta">
                <span v-if="item.deadlineAt && item.status === 'OPEN'">
                  升级截止：{{ formatDate(item.deadlineAt) }}
                </span>
                <span v-if="item.userCreditDelta">
                  信用变动：{{ item.userCreditDelta }} 分
                </span>
              </div>
              <div class="dispute-actions">
                <a-button size="small" @click="openRespondModal(item)" v-if="canRespondDispute(item)">
                  回应方案
                </a-button>
                <a-button size="small" @click="confirmEscalate(item)" v-if="canEscalateDispute(item)">
                  申请仲裁
                </a-button>
                <a-button size="small" @click="confirmAppeal(item)" v-if="canAppealDispute(item)">
                  申诉复核
                </a-button>
              </div>
            </div>
          </div>
        </a-card>

        <a-card title="满意度调查" class="mt-16">
          <a-empty v-if="!userSurveys.length" description="暂无调查" />
          <div v-else class="survey-list">
            <div class="survey-item" v-for="survey in userSurveys" :key="survey.id">
              <div class="survey-item__header">
                <a-tag :color="surveyStatusColor(survey.status)">{{ surveyStatusLabel(survey.status) }}</a-tag>
                <span>创建时间：{{ formatDate(survey.requestedAt) }}</span>
              </div>
              <p class="survey-item__meta">
                可填写时间：{{ formatDate(survey.availableAt) }}
              </p>
              <p v-if="survey.comment" class="survey-item__comment">我的评价：{{ survey.comment }}</p>
              <div class="survey-item__actions">
                <a-button
                  v-if="survey.status === 'OPEN'"
                  type="primary"
                  size="small"
                  @click="openSurveyModal(survey)"
                >
                  填写评价
                </a-button>
                <span v-else-if="survey.status === 'COMPLETED'">
                  评分：{{ survey.rating ?? '-' }} 分
                </span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="8">
        <a-card title="电子合同" class="mt-16">
          <a-space direction="vertical" style="width: 100%">
            <a-button
              type="primary"
              ghost
              block
              :disabled="!order"
              @click="contractDrawerOpen = true"
            >
              查看合同
            </a-button>
            <span class="contract-hint">签署完成后将自动回写订单并记录操作日志。</span>
          </a-space>
        </a-card>
        <a-card title="付款">
          <a-form layout="vertical">
            <a-form-item label="支付场景">
              <a-select v-model:value="paymentForm.scene">
                <a-select-option value="DEPOSIT">押金</a-select-option>
                <a-select-option value="RENT">租金</a-select-option>
                <a-select-option value="BUYOUT">买断</a-select-option>
                <a-select-option value="PENALTY">违约金</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="支付金额">
              <a-input-number v-model:value="paymentForm.amount" :min="0.01" :step="0.01" style="width: 100%" />
            </a-form-item>
            <a-button type="primary" block :loading="paymentForm.loading" @click="handleCreatePayment">生成支付单</a-button>
          </a-form>
          <a-alert
            v-if="paymentForm.lastResult"
            type="success"
            class="mt-12"
            :message="`流水号：${paymentForm.lastResult}`"
            show-icon
          />
          <a-alert
            type="info"
            show-icon
            class="mt-12"
            message="说明"
            description="支付单生成后系统会自动模拟支付通知，如需重新补款可重复生成，平台会防止重复扣款。"
          />
        </a-card>

        <a-card title="确认收货" class="mt-16">
          <a-button
            type="primary"
            block
            :disabled="!canConfirmReceive"
            :loading="receiveLoading"
            @click="confirmReceive"
          >
            确认收到设备
          </a-button>
          <p class="form-hint" v-if="!hasReceiveProof">
            请先在“取证资料”卡片上传收货凭证（开箱视频/签收照片），系统会自动解锁确认按钮。
          </p>
        </a-card>

        <a-card title="续租" class="mt-16">
          <a-form layout="vertical">
            <a-form-item label="续租月数">
              <a-input-number v-model:value="extensionForm.months" :min="1" :max="24" style="width: 100%" />
            </a-form-item>
            <a-button block :loading="extensionForm.loading" @click="handleExtension">提交续租申请</a-button>
          </a-form>
        </a-card>

        <a-card title="退租 / 买断" class="mt-16">
          <a-alert
            type="warning"
            show-icon
            class="mb-12"
            message="请先与厂商确认回收时间或买断金额，再提交申请，避免重复操作。"
          />
          <a-form layout="vertical">
            <a-form-item label="退租原因">
              <a-textarea v-model:value="returnForm.reason" :rows="2" placeholder="填写退租原因" />
            </a-form-item>
            <a-space direction="vertical" style="width: 100%">
              <a-button block :loading="returnForm.loading" @click="confirmReturnRequest">
                申请退租
              </a-button>
              <a-button type="primary" block :loading="buyoutForm.loading" @click="confirmBuyout">
                申请买断
              </a-button>
            </a-space>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
    <OrderContractDrawer
      v-model:open="contractDrawerOpen"
      :order-id="orderId"
      :user-id="auth.user?.id ?? null"
      :default-signature="auth.user?.username ?? ''"
      @signed="handleContractSigned"
    />
    <a-modal
      v-model:open="disputeModal.open"
      title="发起纠纷"
      ok-text="提交纠纷"
      cancel-text="取消"
      :confirm-loading="disputeModal.loading"
      @ok="handleSubmitDispute"
      @cancel="handleCloseDisputeModal"
    >
      <a-form layout="vertical">
        <a-form-item label="纠纷原因" required>
          <a-textarea v-model:value="disputeModal.reason" :rows="3" placeholder="描述问题与诉求" />
        </a-form-item>
        <a-form-item label="期望方案">
          <a-select v-model:value="disputeModal.option">
            <a-select-option v-for="option in disputeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="补充说明">
          <a-textarea v-model:value="disputeModal.remark" :rows="2" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="respondModal.open"
      title="回应纠纷方案"
      ok-text="提交回应"
      cancel-text="取消"
      :confirm-loading="respondModal.loading"
      @ok="handleSubmitRespond"
      @cancel="handleCloseRespondModal"
    >
      <a-form layout="vertical">
        <a-form-item label="处理方案">
          <a-select v-model:value="respondModal.option">
            <a-select-option v-for="option in disputeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="我同意对方方案">
          <a-switch v-model:checked="respondModal.accept" />
        </a-form-item>
        <a-form-item label="补充说明">
          <a-textarea v-model:value="respondModal.remark" :rows="2" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="surveyModal.open"
      title="满意度评价"
      ok-text="提交评价"
      cancel-text="取消"
      :confirm-loading="surveyModal.loading"
      @ok="handleSubmitSurvey"
      @cancel="handleCloseSurveyModal"
    >
      <a-form layout="vertical">
        <a-form-item label="满意度评分">
          <a-rate v-model:value="surveyModal.rating" :count="5" />
        </a-form-item>
        <a-form-item label="留言建议">
          <a-textarea
            v-model:value="surveyModal.comment"
            :rows="3"
            placeholder="欢迎写下沟通体验与建议"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
  <div v-else class="page-container">
    <a-card :loading="loading">正在加载...</a-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import { useAuthStore } from '../../stores/auth';
import {
  fetchOrder,
  confirmOrderReceive,
  applyOrderExtension,
  applyOrderReturn,
  applyOrderBuyout,
  postOrderMessage,
  uploadOrderProof,
  createOrderDispute,
  respondOrderDispute,
  escalateOrderDispute,
  appealOrderDispute,
  submitOrderSurvey,
  type RentalOrderDetail,
  type OrderEvent,
  type OrderProofType,
  type OrderProof,
  type OrderDispute,
  type DisputeResolutionOption,
  type OrderSurvey
} from '../../services/orderService';
import { initPayment, type PaymentSplitPayload } from '../../services/paymentService';
import {
  resolveItemDeposit,
  resolveItemRent,
  rentalOrderDeposit,
  rentalOrderRent,
  rentalOrderTotal
} from '../../utils/orderAmounts';
import OrderContractDrawer from '../../components/orders/OrderContractDrawer.vue';
import { friendlyErrorMessage } from '../../utils/error';
import http from '../../services/http';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();

const orderId = route.params.orderId as string;
const loading = ref(false);
const order = ref<RentalOrderDetail | null>(null);
const receiveLoading = ref(false);

const paymentForm = reactive({
  scene: 'DEPOSIT',
  amount: 0,
  loading: false,
  lastResult: ''
});

const extensionForm = reactive({ months: 3, loading: false });
const returnForm = reactive({ reason: '', loading: false });
const buyoutForm = reactive({ loading: false });
const contractDrawerOpen = ref(false);
const conversationForm = reactive({ message: '', loading: false });
const proofForm = reactive({
  type: 'RECEIVE' as OrderProofType,
  description: '',
  file: null as File | null,
  uploading: false,
  inputKey: Date.now()
});

const disputeModal = reactive({
  open: false,
  reason: '',
  option: 'REDELIVER' as DisputeResolutionOption,
  remark: '',
  loading: false
});

const respondModal = reactive({
  open: false,
  disputeId: null as string | null,
  option: 'REDELIVER' as DisputeResolutionOption,
  accept: true,
  remark: '',
  loading: false
});

const surveyModal = reactive({
  open: false,
  surveyId: null as string | null,
  rating: 5,
  comment: '',
  loading: false
});

const proofLabelMap: Record<OrderProofType, string> = {
  SHIPMENT: '发货凭证',
  RECEIVE: '收货/验收',
  RETURN: '退租寄回',
  INSPECTION: '巡检记录',
  OTHER: '其他'
};

const proofTypeOptions: { label: string; value: OrderProofType }[] = [
  { label: proofLabelMap.RECEIVE, value: 'RECEIVE' },
  { label: proofLabelMap.RETURN, value: 'RETURN' },
  { label: proofLabelMap.INSPECTION, value: 'INSPECTION' },
  { label: proofLabelMap.OTHER, value: 'OTHER' }
];

const proofTypeMap: Record<OrderProofType, string> = proofLabelMap;

const API_PREFIX = '/api/v1';
const normalizeApiPath = (url: string) =>
  url.startsWith(API_PREFIX) ? url.substring(API_PREFIX.length) : url;

const inferProofExtension = (url?: string | null) => {
  if (!url) {
    return '';
  }
  const match = url.match(/(\.[a-zA-Z0-9]+)$/);
  return match ? match[1] : '';
};

const resolveProofDownloadName = (proof: OrderProof) => {
  const extension = inferProofExtension(proof.fileUrl);
  return `${proof.proofType ?? 'PROOF'}-${proof.id}${extension}`;
};

const disputes = computed(() => order.value?.disputes ?? []);
const userSurveys = computed(() =>
  order.value?.surveys?.filter((item) => item.targetRole === 'USER') ?? []
);
const canCreateDispute = computed(() => !!order.value && order.value.userId === auth.user?.id);
const proofList = computed(() => order.value?.proofs ?? []);
const hasReceiveProof = computed(() =>
  proofList.value.some((item) => item.proofType === 'RECEIVE' && item.actorRole === 'USER')
);
const canConfirmReceive = computed(() => {
  if (!order.value) {
    return false;
  }
  return order.value.status === 'IN_LEASE' && hasReceiveProof.value;
});

const disputeOptions: { label: string; value: DisputeResolutionOption }[] = [
  { label: '重新发货/补发配件', value: 'REDELIVER' },
  { label: '部分退款继续租赁', value: 'PARTIAL_REFUND' },
  { label: '退租并扣押金', value: 'RETURN_WITH_DEPOSIT_DEDUCTION' },
  { label: '优惠买断', value: 'DISCOUNTED_BUYOUT' },
  { label: '自定义方案', value: 'CUSTOM' }
];

const disputeOptionMap = disputeOptions.reduce<Record<DisputeResolutionOption, string>>((acc, option) => {
  acc[option.value] = option.label;
  return acc;
}, {} as Record<DisputeResolutionOption, string>);

const formatCurrency = (value: number) => value.toFixed(2);
const formatDate = (value: string) => new Date(value).toLocaleString();
const proofTypeLabel = (type: OrderProofType) => proofTypeMap[type] ?? type;
const conversationEvents = computed(() =>
  order.value?.events?.filter((item) => item.eventType === 'COMMUNICATION_NOTE') ?? []
);
const resolveActorLabel = (event: OrderEvent) => {
  if (event.actorRole === 'USER') {
    return event.createdBy === auth.user?.id ? '我' : '用户';
  }
  if (event.actorRole === 'VENDOR') {
    return '厂商';
  }
  if (event.actorRole === 'ADMIN' || event.actorRole === 'INTERNAL') {
    return '平台';
  }
  return '系统';
};

const resolutionLabel = (option?: DisputeResolutionOption | null) =>
  option ? disputeOptionMap[option] ?? option : '未填写';

const formatDisputeStatus = (status: OrderDispute['status']) => {
  switch (status) {
    case 'OPEN':
      return '协商中';
    case 'PENDING_ADMIN':
      return '待平台处理';
    case 'RESOLVED':
      return '双方达成一致';
    case 'CLOSED':
      return '平台已裁决';
    default:
      return status;
  }
};

const statusColor = (status: OrderDispute['status']) => {
  switch (status) {
    case 'OPEN':
      return 'orange';
    case 'PENDING_ADMIN':
      return 'blue';
    case 'RESOLVED':
      return 'green';
    case 'CLOSED':
      return 'red';
    default:
      return 'default';
  }
};

const surveyStatusLabel = (status: OrderSurvey['status']) => {
  switch (status) {
    case 'PENDING':
      return '待开放';
    case 'OPEN':
      return '待填写';
    case 'COMPLETED':
      return '已完成';
    default:
      return status;
  }
};

const surveyStatusColor = (status: OrderSurvey['status']) => {
  switch (status) {
    case 'PENDING':
      return 'blue';
    case 'OPEN':
      return 'orange';
    case 'COMPLETED':
      return 'green';
    default:
      return 'default';
  }
};

const disputeActorLabel = (role?: string | null) => {
  if (role === 'USER') return '消费者';
  if (role === 'VENDOR') return '厂商';
  if (role === 'ADMIN' || role === 'INTERNAL') return '平台';
  return '系统';
};

const canRespondDispute = (item: OrderDispute) => {
  if (item.status !== 'OPEN') {
    return false;
  }
  if (item.respondentRole === 'USER') {
    return false;
  }
  if (item.initiatorRole === 'USER') {
    return !!item.respondentRole && item.respondentRole !== 'USER';
  }
  return true;
};

const canEscalateDispute = (item: OrderDispute) => item.status === 'OPEN' || item.status === 'RESOLVED';

const canAppealDispute = (item: OrderDispute) => item.status === 'CLOSED' && item.appealCount < 1;

const loadOrder = async () => {
  loading.value = true;
  try {
    order.value = await fetchOrder(orderId);
    paymentForm.amount = order.value ? rentalOrderTotal(order.value) : 0;
  } catch (error) {
    console.error('加载订单失败', error);
    message.error(friendlyErrorMessage(error, '加载订单失败，请稍后重试'));
    router.replace({ name: 'orders' });
  } finally {
    loading.value = false;
  }
};

const goBack = () => {
  router.back();
};

const buildManualPaymentSplits = (scene: string, amount: number): PaymentSplitPayload[] | undefined => {
  if (!order.value) {
    return undefined;
  }
  const normalized = Math.round(amount * 100) / 100;
  if (!normalized || normalized <= 0) {
    return undefined;
  }
  if (scene === 'DEPOSIT') {
    return [
      {
        splitType: 'DEPOSIT_RESERVE',
        amount: normalized,
        beneficiary: 'PLATFORM_RESERVE'
      }
    ];
  }
  return [
    {
      splitType: 'VENDOR_INCOME',
      amount: normalized,
      beneficiary: order.value.vendorId ? `VENDOR_${order.value.vendorId}` : 'VENDOR_UNKNOWN'
    }
  ];
};

const handleCreatePayment = async () => {
  if (!order.value) {
    return;
  }
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  const normalizedAmount = Math.round((Number(paymentForm.amount) || 0) * 100) / 100;
  if (!normalizedAmount || normalizedAmount <= 0) {
    message.warning('请输入有效的支付金额');
    return;
  }
  const splits = buildManualPaymentSplits(paymentForm.scene, normalizedAmount);
  paymentForm.loading = true;
  try {
    const result = await initPayment(order.value.id, {
      userId: auth.user.id,
      vendorId: order.value.vendorId,
      scene: paymentForm.scene as any,
      channel: 'MOCK',
      amount: normalizedAmount,
      splits
    });
    paymentForm.lastResult = result.id ?? '';
    message.success('支付单已创建并自动完成支付');
  } catch (error) {
    console.error('创建支付单失败', error);
    message.error(friendlyErrorMessage(error, '创建支付单失败，请稍后重试'));
  } finally {
    paymentForm.loading = false;
  }
};

const confirmReceive = () => {
  if (!hasReceiveProof.value) {
    message.warning('请先上传收货凭证后再确认收货');
    return;
  }
  Modal.confirm({
    title: '确认已经收到设备？',
    content: '确认后订单将进入在租阶段，如物流信息仍未更新请耐心等待。',
    okText: '确认收货',
    cancelText: '暂不',
    onOk: () => handleConfirmReceive()
  });
};

const handleConfirmReceive = async () => {
  if (!order.value || !auth.user?.id) {
    return;
  }
  receiveLoading.value = true;
  try {
    await confirmOrderReceive(order.value.id, { actorId: auth.user.id });
    message.success('已确认收货');
    await loadOrder();
  } catch (error) {
    console.error('确认收货失败', error);
    message.error(friendlyErrorMessage(error, '确认收货失败，请稍后重试'));
  } finally {
    receiveLoading.value = false;
  }
};

const handleExtension = async () => {
  if (!order.value || !auth.user?.id) {
    return;
  }
  extensionForm.loading = true;
  try {
    await applyOrderExtension(order.value.id, {
      userId: auth.user.id,
      additionalMonths: extensionForm.months,
      remark: '用户发起续租'
    });
    message.success('续租申请已提交，请等待厂商确认');
    extensionForm.loading = false;
  } catch (error) {
    console.error('续租申请失败', error);
    message.error(friendlyErrorMessage(error, '续租申请失败，请稍后重试'));
    extensionForm.loading = false;
  }
};

const confirmReturnRequest = () => {
  Modal.confirm({
    title: '提交退租申请',
    content: '提交后厂商将安排回收，若仍在使用请暂缓操作。',
    okText: '提交退租',
    cancelText: '再想想',
    onOk: () => handleReturnRequest()
  });
};

const handleReturnRequest = async () => {
  if (!order.value || !auth.user?.id) {
    return;
  }
  returnForm.loading = true;
  try {
    await applyOrderReturn(order.value.id, {
      userId: auth.user.id,
      reason: returnForm.reason || '用户发起退租'
    });
    message.success('退租申请已提交，请关注通知更新');
    returnForm.loading = false;
  } catch (error) {
    console.error('退租申请失败', error);
    message.error(friendlyErrorMessage(error, '退租申请失败，请稍后重试'));
    returnForm.loading = false;
  }
};

const confirmBuyout = () => {
  Modal.confirm({
    title: '确认提交买断申请？',
    content: '买断申请提交后厂商会回访确认，若需分期支付请在备注中说明。',
    okText: '提交买断',
    cancelText: '取消',
    onOk: () => handleBuyout()
  });
};

const handleBuyout = async () => {
  if (!order.value || !auth.user?.id) {
    return;
  }
  buyoutForm.loading = true;
  try {
    await applyOrderBuyout(order.value.id, {
      userId: auth.user.id,
      remark: '用户申请买断'
    });
    message.success('买断申请已提交');
  } catch (error) {
    console.error('买断申请失败', error);
    message.error(friendlyErrorMessage(error, '买断申请失败，请稍后重试'));
  } finally {
    buyoutForm.loading = false;
  }
};

const handleProofFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  proofForm.file = target.files && target.files.length > 0 ? target.files[0] : null;
};

const handleProofUpload = async () => {
  if (!order.value) {
    return;
  }
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  if (!proofForm.file) {
    message.warning('请先选择需要上传的文件');
    return;
  }
  proofForm.uploading = true;
  try {
    await uploadOrderProof(order.value.id, {
      actorId: auth.user.id,
      proofType: proofForm.type,
      description: proofForm.description || undefined,
      file: proofForm.file
    });
    message.success('取证资料已上传');
    proofForm.description = '';
    proofForm.file = null;
    proofForm.inputKey = Date.now();
    await loadOrder();
  } catch (error) {
    console.error('上传取证资料失败', error);
    message.error(friendlyErrorMessage(error, '上传取证资料失败，请稍后重试'));
  } finally {
    proofForm.uploading = false;
  }
};

const handleDownloadProof = async (proof: OrderProof) => {
  if (!proof.fileUrl) {
    message.warning('文件地址无效');
    return;
  }
  const path = normalizeApiPath(proof.fileUrl);
  try {
    const response = await http.get(path, { responseType: 'blob' });
    const blob = new Blob([response.data], { type: proof.contentType ?? 'application/octet-stream' });
    const objectUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = objectUrl;
    link.download = resolveProofDownloadName(proof);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(objectUrl);
  } catch (error) {
    console.error('下载取证资料失败', error);
    message.error(friendlyErrorMessage(error, '下载取证资料失败，请稍后重试'));
  }
};

const handleSendMessage = async () => {
  if (!order.value) {
    return;
  }
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  const content = conversationForm.message.trim();
  if (!content) {
    message.warning('请输入留言内容');
    return;
  }
  conversationForm.loading = true;
  try {
    await postOrderMessage(order.value.id, {
      actorId: auth.user.id,
      message: content
    });
    conversationForm.message = '';
    await loadOrder();
    message.success('留言已发送');
  } catch (error) {
    console.error('发送留言失败', error);
    message.error(friendlyErrorMessage(error, '发送留言失败，请稍后重试'));
  } finally {
    conversationForm.loading = false;
  }
};

const showDisputeModal = () => {
  if (!canCreateDispute.value) {
    message.warning('仅订单本人可发起纠纷');
    return;
  }
  disputeModal.open = true;
};

const resetDisputeModal = () => {
  disputeModal.reason = '';
  disputeModal.remark = '';
  disputeModal.option = 'REDELIVER';
};

const handleSubmitDispute = async () => {
  if (!order.value || !auth.user?.id) {
    return;
  }
  const reason = disputeModal.reason.trim();
  if (!reason) {
    message.warning('请填写纠纷原因');
    return;
  }
  disputeModal.loading = true;
  try {
    await createOrderDispute(order.value.id, {
      actorId: auth.user.id,
      option: disputeModal.option,
      reason,
      remark: disputeModal.remark?.trim() || undefined
    });
    message.success('纠纷已记录');
    disputeModal.open = false;
    resetDisputeModal();
    await loadOrder();
  } catch (error) {
    console.error('创建纠纷失败', error);
    message.error(friendlyErrorMessage(error, '提交纠纷失败，请稍后重试'));
  } finally {
    disputeModal.loading = false;
  }
};

const handleCloseDisputeModal = () => {
  disputeModal.open = false;
  resetDisputeModal();
};

const handleCloseRespondModal = () => {
  respondModal.open = false;
  respondModal.disputeId = null;
  respondModal.remark = '';
};

const openRespondModal = (dispute: OrderDispute) => {
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  respondModal.disputeId = dispute.id;
  respondModal.option = dispute.respondentOption ?? dispute.initiatorOption;
  respondModal.accept = true;
  respondModal.remark = '';
  respondModal.open = true;
};

const handleSubmitRespond = async () => {
  if (!order.value || !auth.user?.id || !respondModal.disputeId) {
    return;
  }
  respondModal.loading = true;
  try {
    await respondOrderDispute(order.value.id, respondModal.disputeId, {
      actorId: auth.user.id,
      option: respondModal.option,
      accept: respondModal.accept,
      remark: respondModal.remark?.trim() || undefined
    });
    message.success('已提交回应');
    handleCloseRespondModal();
    await loadOrder();
  } catch (error) {
    console.error('回应纠纷失败', error);
    message.error(friendlyErrorMessage(error, '回应失败，请稍后重试'));
  } finally {
    respondModal.loading = false;
  }
};

const confirmEscalate = (dispute: OrderDispute) => {
  Modal.confirm({
    title: '提交平台仲裁？',
    content: '平台将在 48 小时内介入处理该纠纷。',
    okText: '提交仲裁',
    cancelText: '取消',
    onOk: () => performEscalate(dispute)
  });
};

const performEscalate = async (dispute: OrderDispute) => {
  if (!order.value || !auth.user?.id) {
    return;
  }
  try {
    await escalateOrderDispute(order.value.id, dispute.id, {
      actorId: auth.user.id,
      reason: '用户申请平台仲裁'
    });
    message.success('已提交平台仲裁');
    await loadOrder();
  } catch (error) {
    console.error('仲裁请求失败', error);
    message.error(friendlyErrorMessage(error, '仲裁请求失败，请稍后重试'));
  }
};

const confirmAppeal = (dispute: OrderDispute) => {
  Modal.confirm({
    title: '确认发起申诉？',
    content: '每个纠纷仅可申诉一次，请确保已补充全部证据。',
    okText: '提交申诉',
    cancelText: '取消',
    onOk: () => performAppeal(dispute)
  });
};

const performAppeal = async (dispute: OrderDispute) => {
  if (!order.value || !auth.user?.id) {
    return;
  }
  try {
    await appealOrderDispute(order.value.id, dispute.id, {
      actorId: auth.user.id,
      reason: '用户申请复核'
    });
    message.success('申诉已提交');
    await loadOrder();
  } catch (error) {
    console.error('申诉失败', error);
    message.error(friendlyErrorMessage(error, '申诉失败，请稍后重试'));
  }
};

const openSurveyModal = (survey: OrderSurvey) => {
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  surveyModal.surveyId = survey.id;
  surveyModal.rating = survey.rating ?? 5;
  surveyModal.comment = '';
  surveyModal.open = true;
};

const handleSubmitSurvey = async () => {
  if (!order.value || !surveyModal.surveyId || !auth.user?.id) {
    return;
  }
  surveyModal.loading = true;
  try {
    await submitOrderSurvey(order.value.id, surveyModal.surveyId, {
      actorId: auth.user.id,
      rating: surveyModal.rating,
      comment: surveyModal.comment?.trim() || undefined
    });
    message.success('感谢反馈，我们已记录您的评价');
    surveyModal.open = false;
    surveyModal.comment = '';
    await loadOrder();
  } catch (error) {
    console.error('提交满意度失败', error);
    message.error(friendlyErrorMessage(error, '提交失败，请稍后重试'));
  } finally {
    surveyModal.loading = false;
  }
};

const handleCloseSurveyModal = () => {
  surveyModal.open = false;
  surveyModal.surveyId = null;
  surveyModal.comment = '';
};

loadOrder();

const handleContractSigned = async () => {
  await loadOrder();
};
</script>

<style scoped>
.mt-12 {
  margin-top: 12px;
}

.mt-16 {
  margin-top: 16px;
}

.mb-12 {
  margin-bottom: 12px;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.contract-hint {
  color: #64748b;
  font-size: 12px;
}

.order-guidance {
  margin: 0 0 12px;
}

.proof-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin-bottom: 12px;
}

.proof-item {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.proof-item__meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #475569;
}

.proof-item__desc {
  margin: 0;
  color: #0f172a;
  min-height: 36px;
}

.proof-file-hint {
  margin-top: 4px;
  font-size: 12px;
  color: #475569;
}

.conversation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 12px;
}

.conversation-item__meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #475569;
}

.dispute-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #475569;
  margin-bottom: 12px;
  gap: 12px;
}

.dispute-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.survey-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.survey-item {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  background: #f8fafc;
}

.survey-item__header {
  display: flex;
  justify-content: space-between;
  color: #475569;
  font-size: 12px;
  margin-bottom: 4px;
}

.survey-item__meta {
  margin: 0 0 6px;
  font-size: 12px;
  color: #475569;
}

.survey-item__comment {
  margin: 0 0 8px;
  color: #0f172a;
}

.survey-item__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.dispute-item {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  background: #f8fafc;
}

.dispute-item__header {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #475569;
  align-items: center;
}

.dispute-item__reason {
  margin: 8px 0;
  color: #0f172a;
}

.dispute-item__line {
  margin: 4px 0;
  color: #334155;
}

.dispute-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 12px;
  color: #64748b;
}

.dispute-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}
</style>
