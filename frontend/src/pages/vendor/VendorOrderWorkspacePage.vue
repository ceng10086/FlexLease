<template>
  <div v-if="vendorReady" class="page-container">
    <div class="page-header">
      <div>
        <h2>订单履约</h2>
        <p class="page-header__meta">查看并处理租赁订单，完成发货、退租审批等关键动作。</p>
      </div>
      <a-space>
        <a-select v-model:value="filters.status" allow-clear placeholder="全部状态" style="width: 200px" @change="loadOrders">
          <a-select-option v-for="status in vendorStatuses" :key="status" :value="status">{{ status }}</a-select-option>
        </a-select>
        <a-button type="primary" @click="loadOrders" :loading="loading">刷新</a-button>
      </a-space>
    </div>

    <a-card>
      <a-table :data-source="orders" :loading="loading" row-key="id" :pagination="pagination" @change="handleTableChange">
        <a-table-column title="订单号" data-index="orderNo" key="orderNo" />
        <a-table-column title="状态" key="status">
          <template #default="{ record }"><a-tag>{{ record.status }}</a-tag></template>
        </a-table-column>
        <a-table-column title="金额" key="totalAmount">
          <template #default="{ record }">¥{{ formatCurrency(record.totalAmount) }}</template>
        </a-table-column>
        <a-table-column title="创建时间" key="createdAt">
          <template #default="{ record }">{{ formatDate(record.createdAt) }}</template>
        </a-table-column>
        <a-table-column title="操作" key="actions">
          <template #default="{ record }">
            <a-button size="small" @click="openDetail(record.id)">处理</a-button>
          </template>
        </a-table-column>
      </a-table>
    </a-card>

    <a-drawer v-model:open="detail.open" :width="840" title="订单详情" destroy-on-close>
      <a-spin :spinning="detail.loading">
        <template v-if="detail.order">
          <a-descriptions title="基础信息" :column="2" bordered size="small">
            <a-descriptions-item label="订单号">{{ detail.order.orderNo }}</a-descriptions-item>
            <a-descriptions-item label="状态">{{ detail.order.status }}</a-descriptions-item>
            <a-descriptions-item label="用户">{{ detail.order.userId }}</a-descriptions-item>
            <a-descriptions-item label="总金额">¥{{ formatCurrency(rentalOrderTotal(detail.order)) }}</a-descriptions-item>
            <a-descriptions-item label="押金">¥{{ formatCurrency(rentalOrderDeposit(detail.order)) }}</a-descriptions-item>
            <a-descriptions-item label="租金">¥{{ formatCurrency(rentalOrderRent(detail.order)) }}</a-descriptions-item>
            <a-descriptions-item label="创建时间">{{ formatDate(detail.order.createdAt) }}</a-descriptions-item>
            <a-descriptions-item label="客户备注" :span="2" v-if="detail.order.customerRemark">
              {{ detail.order.customerRemark }}
            </a-descriptions-item>
          </a-descriptions>
          <a-space style="margin: 12px 0;">
            <a-button type="link" @click="contractDrawerOpen = true">查看电子合同</a-button>
          </a-space>

          <a-divider />
          <h4>明细</h4>
          <a-table :data-source="detail.order.items" :pagination="false" size="small" row-key="id">
            <a-table-column title="商品" data-index="productName" key="product" />
            <a-table-column title="SKU" data-index="skuCode" key="sku" />
            <a-table-column title="数量" data-index="quantity" key="quantity" />
            <a-table-column title="月租金" key="rent"><template #default="{ record }">¥{{ formatCurrency(resolveItemRent(record)) }}</template></a-table-column>
          </a-table>

          <a-divider />
          <div class="detail__actions">
            <div class="action-box">
              <h5>发货</h5>
              <a-form layout="vertical">
                <a-form-item label="承运方" required>
                  <a-input
                    v-model:value="shipForm.carrier"
                    placeholder="如 SF"
                    :disabled="!canShip"
                  />
                </a-form-item>
                <a-form-item label="运单号" required>
                  <a-input
                    v-model:value="shipForm.trackingNumber"
                    placeholder="物流单号"
                    :disabled="!canShip"
                  />
                </a-form-item>
                <a-button
                  type="primary"
                  :loading="shipForm.loading"
                  :disabled="!canSubmitShipment"
                  @click="handleShip"
                >
                  提交发货
                </a-button>
                <p class="form-hint" v-if="!canShip">仅待发货订单可填写物流。</p>
                <p class="form-hint" v-else-if="!meetsShipmentProofRequirement">
                  {{ shipmentRequirementHint }}
                </p>
              </a-form>
            </div>
            <div class="action-box">
              <h5>退租处理</h5>
              <template v-if="canApproveReturn">
                <a-form layout="vertical">
                  <a-form-item label="审批意见">
                    <a-textarea v-model:value="returnForm.remark" :rows="3" placeholder="可选" />
                  </a-form-item>
                  <a-space>
                    <a-button
                      type="primary"
                      danger
                      :loading="returnForm.loading"
                      :disabled="!canApproveReturn"
                      @click="handleReturnDecision(false)"
                    >
                      拒绝退租
                    </a-button>
                    <a-button
                      type="primary"
                      :loading="returnForm.loading"
                      :disabled="!canApproveReturn"
                      @click="handleReturnDecision(true)"
                    >
                      同意退租
                    </a-button>
                  </a-space>
                  <p class="form-hint">审批通过后订单将进入“退租中”。</p>
                </a-form>
              </template>
              <template v-else-if="canCompleteReturn">
                <a-form layout="vertical">
                  <a-form-item label="完结备注">
                    <a-textarea v-model:value="returnCompletionForm.remark" :rows="3" placeholder="可选" />
                  </a-form-item>
                  <a-button
                    type="primary"
                    :loading="returnCompletionForm.loading"
                    @click="handleReturnCompletion"
                  >
                    确认退租完成
                  </a-button>
                  <p class="form-hint">确认后将触发入库与押金退款。</p>
                </a-form>
              </template>
              <p class="form-hint" v-else>当前状态无需退租操作。</p>
            </div>
            <div class="action-box" v-if="pendingExtension">
              <h5>续租审批</h5>
              <p class="action-box__meta">申请延长 {{ pendingExtension.additionalMonths }} 个月</p>
              <a-form layout="vertical">
                <a-form-item label="审批备注">
                  <a-textarea
                    v-model:value="extensionDecisionForm.remark"
                    :rows="3"
                    placeholder="可选"
                  />
                </a-form-item>
                <a-space>
                  <a-button
                    danger
                    :loading="extensionDecisionForm.loading"
                    @click="handleExtensionDecision(false)"
                  >
                    驳回续租
                  </a-button>
                  <a-button
                    type="primary"
                    :loading="extensionDecisionForm.loading"
                    @click="handleExtensionDecision(true)"
                  >
                    同意续租
                  </a-button>
                </a-space>
              </a-form>
            </div>
            <div class="action-box" v-if="isBuyoutRequested">
              <h5>买断审批</h5>
              <p class="action-box__meta">确认后订单将进入买断完成状态。</p>
              <a-form layout="vertical">
                <a-form-item label="审批备注">
                  <a-textarea
                    v-model:value="buyoutDecisionForm.remark"
                    :rows="3"
                    placeholder="可选"
                  />
                </a-form-item>
                <a-space>
                  <a-button
                    danger
                    :loading="buyoutDecisionForm.loading"
                    @click="handleBuyoutDecision(false)"
                  >
                    拒绝买断
                  </a-button>
                  <a-button
                    type="primary"
                    :loading="buyoutDecisionForm.loading"
                    @click="handleBuyoutDecision(true)"
                  >
                    确认买断
                  </a-button>
                </a-space>
              </a-form>
            </div>
          </div>

          <a-divider />
          <h5>操作记录</h5>
          <a-empty v-if="!detail.order.events?.length" description="暂无记录" />
          <a-timeline v-else>
            <a-timeline-item v-for="event in detail.order.events" :key="event.id">
              <div class="timeline-item">
                <strong>{{ event.eventType }}</strong>
                <span>{{ formatDate(event.createdAt) }}</span>
                <p v-if="event.description">{{ event.description }}</p>
              </div>
            </a-timeline-item>
          </a-timeline>

          <a-divider />
          <h5>取证资料</h5>
          <div v-if="!proofList.length">
            <a-empty description="暂无取证资料" />
          </div>
          <div v-else class="proof-grid">
            <div class="proof-item" v-for="item in proofList" :key="item.id">
              <div class="proof-item__meta">
                <strong>{{ proofTypeLabel(item.proofType) }}</strong>
                <span>{{ formatDate(item.uploadedAt) }}</span>
              </div>
              <p class="proof-item__desc">{{ item.description || '未填写说明' }}</p>
              <a href="#" @click.prevent="handleDownloadProof(item)">查看文件</a>
            </div>
          </div>
          <a-form layout="vertical" class="mt-12">
            <a-form-item label="取证类型">
              <a-select v-model:value="proofForm.type">
                <a-select-option v-for="option in proofTypeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </a-select-option>
              </a-select>
              <div class="proof-file-hint" v-if="proofForm.type === 'SHIPMENT'">
                {{ shipmentRequirementHint }}
              </div>
            </a-form-item>
            <a-form-item label="补充说明">
              <a-textarea v-model:value="proofForm.description" :rows="2" placeholder="例如：发货前包装、序列号等" />
            </a-form-item>
            <a-form-item label="上传文件">
              <input
                type="file"
                :key="proofForm.inputKey"
                @change="handleProofFileChange"
              />
              <div class="proof-file-hint" v-if="proofForm.file">{{ proofForm.file.name }}</div>
            </a-form-item>
            <a-button type="primary" :loading="proofForm.uploading" @click="handleProofUpload">
              上传凭证
            </a-button>
          </a-form>

          <a-divider />
          <h5>沟通记录</h5>
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
          <a-form layout="vertical">
            <a-form-item label="快速回复">
              <a-textarea v-model:value="conversationForm.message" :rows="3" placeholder="输入想回复用户的内容" />
            </a-form-item>
            <a-button type="primary" :loading="conversationForm.loading" @click="handleSendMessage">
              发送回复
            </a-button>
          </a-form>

          <a-divider />
          <h5>纠纷与仲裁</h5>
          <div class="dispute-header">
            <span>查看并回应用户纠纷，必要时可主动升级平台仲裁。</span>
            <a-button size="small" type="primary" @click="openVendorDisputeModal" :disabled="!detail.order">
              发起纠纷
            </a-button>
          </div>
          <a-empty v-if="!disputes.length" description="暂无纠纷记录" />
          <div v-else class="dispute-list">
            <div class="dispute-item" v-for="item in disputes" :key="item.id">
              <div class="dispute-item__header">
                <a-tag :color="disputeStatusColor(item.status)">{{ disputeStatusLabel(item.status) }}</a-tag>
                <span>发起人：{{ disputeActorLabel(item.initiatorRole) }}</span>
                <span>创建：{{ formatDate(item.createdAt) }}</span>
              </div>
              <p class="dispute-item__reason">
                <strong>诉求：</strong>{{ resolutionLabel(item.initiatorOption) }} | {{ item.initiatorReason }}
              </p>
              <p v-if="item.respondentOption" class="dispute-item__line">
                我方建议：{{ resolutionLabel(item.respondentOption) }}
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
                <a-button
                  size="small"
                  @click="openVendorRespondModal(item)"
                  v-if="canVendorRespondDispute(item)"
                >
                  回应方案
                </a-button>
                <a-button
                  size="small"
                  @click="confirmVendorEscalate(item)"
                  v-if="canVendorEscalateDispute(item)"
                >
                  申请仲裁
                </a-button>
                <a-button
                  size="small"
                  @click="confirmVendorAppeal(item)"
                  v-if="canVendorAppealDispute(item)"
                >
                  申诉复核
                </a-button>
              </div>
            </div>
          </div>

          <a-divider />
          <h5>满意度调查</h5>
          <a-empty v-if="!vendorSurveys.length" description="暂无调查记录" />
          <div v-else class="survey-list">
            <div class="survey-item" v-for="survey in vendorSurveys" :key="survey.id">
              <div class="survey-item__header">
                <a-tag :color="surveyStatusColor(survey.status)">{{ surveyStatusLabel(survey.status) }}</a-tag>
                <span>创建：{{ formatDate(survey.requestedAt) }}</span>
              </div>
              <p class="survey-item__meta">开放时间：{{ formatDate(survey.availableAt) }}</p>
              <p v-if="survey.comment" class="survey-item__comment">评价：{{ survey.comment }}</p>
              <div class="survey-item__actions">
                <a-button
                  v-if="survey.status === 'OPEN'"
                  size="small"
                  type="primary"
                  @click="openVendorSurveyModal(survey)"
                >
                  填写评价
                </a-button>
                <span v-else-if="survey.status === 'COMPLETED'">评分：{{ survey.rating ?? '-' }} 分</span>
              </div>
            </div>
          </div>
        </template>
        <template v-else>
          <a-empty description="未找到订单详情" />
        </template>
      </a-spin>
    </a-drawer>
    <OrderContractDrawer
      v-if="detail.order"
      v-model:open="contractDrawerOpen"
      :order-id="detail.order.id"
      :allow-sign="false"
    />
    <a-modal
      v-model:open="vendorDisputeModal.open"
      title="发起纠纷"
      ok-text="提交纠纷"
      cancel-text="取消"
      :confirm-loading="vendorDisputeModal.loading"
      @ok="handleVendorSubmitDispute"
      @cancel="handleVendorCloseDisputeModal"
    >
      <a-form layout="vertical">
        <a-form-item label="纠纷原因" required>
          <a-textarea v-model:value="vendorDisputeModal.reason" :rows="3" placeholder="描述纠纷原因" />
        </a-form-item>
        <a-form-item label="期望方案">
          <a-select v-model:value="vendorDisputeModal.option">
            <a-select-option v-for="option in disputeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="补充说明">
          <a-textarea v-model:value="vendorDisputeModal.remark" :rows="2" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="vendorRespondModal.open"
      title="回应纠纷方案"
      ok-text="提交回应"
      cancel-text="取消"
      :confirm-loading="vendorRespondModal.loading"
      @ok="handleVendorSubmitRespond"
      @cancel="handleVendorCloseRespondModal"
    >
      <a-form layout="vertical">
        <a-form-item label="处理方案">
          <a-select v-model:value="vendorRespondModal.option">
            <a-select-option v-for="option in disputeOptions" :key="option.value" :value="option.value">
              {{ option.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="同意对方方案">
          <a-switch v-model:checked="vendorRespondModal.accept" />
        </a-form-item>
        <a-form-item label="补充说明">
          <a-textarea v-model:value="vendorRespondModal.remark" :rows="2" placeholder="可选" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-modal
      v-model:open="surveyModal.open"
      title="满意度评价"
      ok-text="提交评价"
      cancel-text="取消"
      :confirm-loading="surveyModal.loading"
      @ok="handleVendorSubmitSurvey"
      @cancel="handleVendorCloseSurveyModal"
    >
      <a-form layout="vertical">
        <a-form-item label="满意度评分">
          <a-rate v-model:value="surveyModal.rating" :count="5" />
        </a-form-item>
        <a-form-item label="留言建议">
          <a-textarea
            v-model:value="surveyModal.comment"
            :rows="3"
            placeholder="可记录用户沟通体验或改进点"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>

  <div v-else class="page-container">
    <a-result status="warning" title="尚未获取厂商身份">
      <template #subTitle>
        请先退出当前账号并重新登录后，再访问厂商工作台。
      </template>
    </a-result>
  </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue';
import { message, Modal } from 'ant-design-vue';
import { useVendorContext } from '../../composables/useVendorContext';
import {
  listOrders,
  fetchOrder,
  shipOrder,
  decideOrderReturn,
  completeOrderReturn,
  decideOrderExtension,
  decideOrderBuyout,
  postOrderMessage,
  uploadOrderProof,
  createOrderDispute,
  respondOrderDispute,
  escalateOrderDispute,
  appealOrderDispute,
  submitOrderSurvey,
  type OrderStatus,
  type RentalOrderSummary,
  type RentalOrderDetail,
  type OrderEvent,
  type OrderProof,
  type OrderProofType,
  type OrderDispute,
  type DisputeResolutionOption,
  type OrderSurvey
} from '../../services/orderService';
import {
  resolveItemDeposit,
  resolveItemRent,
  rentalOrderDeposit,
  rentalOrderRent,
  rentalOrderTotal
} from '../../utils/orderAmounts';
import OrderContractDrawer from '../../components/orders/OrderContractDrawer.vue';
import { useAuthStore } from '../../stores/auth';
import http from '../../services/http';

const vendorStatuses: OrderStatus[] = [
  'PENDING_PAYMENT',
  'AWAITING_SHIPMENT',
  'IN_LEASE',
  'RETURN_REQUESTED',
  'RETURN_IN_PROGRESS',
  'COMPLETED',
  'BUYOUT_REQUESTED',
  'BUYOUT_COMPLETED',
  'CANCELLED',
  'EXCEPTION_CLOSED'
];

const {
  vendorId: currentVendorId,
  vendorReady,
  requireVendorId
} = useVendorContext();
const auth = useAuthStore();

const loading = ref(false);
const orders = ref<RentalOrderSummary[]>([]);
const filters = reactive<{ status?: OrderStatus }>({});
const pagination = reactive({ current: 1, pageSize: 10, total: 0 });

const detail = reactive<{ open: boolean; loading: boolean; order: RentalOrderDetail | null }>(
  {
    open: false,
    loading: false,
    order: null
  }
);

const shipForm = reactive({ carrier: '', trackingNumber: '', loading: false });
const returnForm = reactive({ remark: '', loading: false });
const returnCompletionForm = reactive({ remark: '', loading: false });
const extensionDecisionForm = reactive({ remark: '', loading: false });
const buyoutDecisionForm = reactive({ remark: '', loading: false });
const contractDrawerOpen = ref(false);
const conversationForm = reactive({ message: '', loading: false });
const proofForm = reactive({
  type: 'SHIPMENT' as OrderProofType,
  description: '',
  file: null as File | null,
  uploading: false,
  inputKey: Date.now()
});
const vendorDisputeModal = reactive({
  open: false,
  reason: '',
  option: 'REDELIVER' as DisputeResolutionOption,
  remark: '',
  loading: false
});
const vendorRespondModal = reactive({
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
  RECEIVE: '收货验收',
  RETURN: '退租寄回',
  INSPECTION: '巡检记录',
  OTHER: '其他'
};

const proofTypeOptions: { label: string; value: OrderProofType }[] = [
  { label: proofLabelMap.SHIPMENT, value: 'SHIPMENT' },
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

const disputes = computed(() => detail.order?.disputes ?? []);
const vendorSurveys = computed(() =>
  detail.order?.surveys?.filter((item) => item.targetRole === 'VENDOR') ?? []
);

const shipmentRequirement = computed(() => ({
  photos: detail.order?.shipmentPhotoRequired ?? 0,
  videos: detail.order?.shipmentVideoRequired ?? 0
}));

const disputeOptions: { label: string; value: DisputeResolutionOption }[] = [
  { label: '重新发货/补发', value: 'REDELIVER' },
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
const pendingExtension = computed(() =>
  detail.order?.extensions?.find((item) => item.status === 'PENDING') ?? null
);
const isBuyoutRequested = computed(() => detail.order?.status === 'BUYOUT_REQUESTED');
const canShip = computed(() => detail.order?.status === 'AWAITING_SHIPMENT');
const canApproveReturn = computed(() => detail.order?.status === 'RETURN_REQUESTED');
const canCompleteReturn = computed(() => detail.order?.status === 'RETURN_IN_PROGRESS');
const conversationEvents = computed(() =>
  detail.order?.events?.filter((item) => item.eventType === 'COMMUNICATION_NOTE') ?? []
);
const proofList = computed(() => detail.order?.proofs ?? []);
const proofTypeLabel = (type: OrderProofType) => proofTypeMap[type] ?? type;

const normalizeString = (value?: string | null) => value?.toLowerCase() ?? '';
const isImageProof = (proof: OrderProof) => {
  const contentType = normalizeString(proof.contentType);
  if (contentType.startsWith('image/')) {
    return true;
  }
  const fileUrl = normalizeString(proof.fileUrl);
  return fileUrl.endsWith('.jpg') || fileUrl.endsWith('.jpeg') || fileUrl.endsWith('.png');
};
const isVideoProof = (proof: OrderProof) => {
  const contentType = normalizeString(proof.contentType);
  if (contentType.startsWith('video/')) {
    return true;
  }
  const fileUrl = normalizeString(proof.fileUrl);
  return fileUrl.endsWith('.mp4') || fileUrl.endsWith('.mov') || fileUrl.endsWith('.m4v');
};

const shipmentProofStats = computed(() => {
  const shipments = proofList.value.filter(
    (item) => item.proofType === 'SHIPMENT' && item.actorRole === 'VENDOR'
  );
  return {
    photos: shipments.filter(isImageProof).length,
    videos: shipments.filter(isVideoProof).length
  };
});

const meetsShipmentProofRequirement = computed(() =>
  shipmentProofStats.value.photos >= shipmentRequirement.value.photos &&
  shipmentProofStats.value.videos >= shipmentRequirement.value.videos
);

const canSubmitShipment = computed(() => canShip.value && meetsShipmentProofRequirement.value);
const shipmentRequirementHint = computed(() => {
  if (shipmentRequirement.value.photos === 0 && shipmentRequirement.value.videos === 0) {
    return '无需额外凭证即可发货。';
  }
  const stats = shipmentProofStats.value;
  const requirement = shipmentRequirement.value;
  return `需至少上传${requirement.photos}张照片和${requirement.videos}段视频（当前 ${stats.photos}/${requirement.photos} 张、${stats.videos}/${requirement.videos} 段）`;
});
const resolveActorLabel = (event: OrderEvent) => {
  if (event.actorRole === 'USER') {
    return event.createdBy === auth.user?.id ? '我' : '用户';
  }
  if (event.actorRole === 'VENDOR') {
    return event.createdBy === auth.user?.id ? '我' : '厂商';
  }
  if (event.actorRole === 'ADMIN' || event.actorRole === 'INTERNAL') {
    return '平台';
  }
  return '系统';
};

const resolutionLabel = (option?: DisputeResolutionOption | null) =>
  option ? disputeOptionMap[option] ?? option : '未填写';

const disputeStatusLabel = (status: OrderDispute['status']) => {
  switch (status) {
    case 'OPEN':
      return '协商中';
    case 'PENDING_ADMIN':
      return '待平台处理';
    case 'RESOLVED':
      return '双方一致';
    case 'CLOSED':
      return '已结案';
    default:
      return status;
  }
};

const disputeStatusColor = (status: OrderDispute['status']) => {
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

const disputeActorLabel = (role?: string | null) => {
  if (role === 'USER') return '消费者';
  if (role === 'VENDOR') return '厂商';
  if (role === 'ADMIN' || role === 'INTERNAL') return '平台';
  return '系统';
};

const canVendorRespondDispute = (item: OrderDispute) => {
  if (item.status !== 'OPEN') {
    return false;
  }
  if (item.respondentRole === 'VENDOR') {
    return false;
  }
  if (item.initiatorRole === 'VENDOR') {
    return !!item.respondentRole && item.respondentRole !== 'VENDOR';
  }
  return true;
};

const canVendorEscalateDispute = (item: OrderDispute) => item.status === 'OPEN' || item.status === 'RESOLVED';

const canVendorAppealDispute = (item: OrderDispute) => item.status === 'CLOSED' && item.appealCount < 1;

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

const loadOrders = async () => {
  const vendorId = requireVendorId();
  if (!vendorId) {
    orders.value = [];
    pagination.total = 0;
    return;
  }
  loading.value = true;
  try {
    const result = await listOrders({
      vendorId,
      status: filters.status,
      page: pagination.current,
      size: pagination.pageSize
    });
    orders.value = result.content;
    pagination.total = result.totalElements;
  } catch (error) {
    console.error('加载订单失败', error);
    message.error('加载订单失败，请稍后重试');
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (pager: { current?: number; pageSize?: number }) => {
  pagination.current = pager.current ?? pagination.current;
  pagination.pageSize = pager.pageSize ?? pagination.pageSize;
  loadOrders();
};

const openDetail = async (orderId: string) => {
  detail.open = true;
  detail.loading = true;
  try {
    detail.order = await fetchOrder(orderId);
    shipForm.carrier = detail.order?.shippingCarrier ?? '';
    shipForm.trackingNumber = detail.order?.shippingTrackingNo ?? '';
    returnForm.remark = '';
    returnCompletionForm.remark = '';
    extensionDecisionForm.remark = '';
    buyoutDecisionForm.remark = '';
  } catch (error) {
    console.error('加载订单详情失败', error);
    message.error('加载详情失败');
    detail.order = null;
  } finally {
    detail.loading = false;
  }
};

const refreshDetail = async () => {
  if (detail.order?.id) {
    await openDetail(detail.order.id);
  }
};

const handleShip = async () => {
  if (!detail.order) {
    return;
  }
  if (!canShip.value) {
    message.warning('当前状态无需发货');
    return;
  }
  if (!meetsShipmentProofRequirement.value) {
    message.warning(shipmentRequirementHint.value);
    return;
  }
  const vendorId = requireVendorId(true);
  if (!vendorId) {
    return;
  }
  if (!shipForm.carrier || !shipForm.trackingNumber) {
    message.warning('请填写承运方与运单号');
    return;
  }
  shipForm.loading = true;
  try {
    await shipOrder(detail.order.id, {
      vendorId,
      carrier: shipForm.carrier,
      trackingNumber: shipForm.trackingNumber
    });
    message.success('发货信息已提交');
    await refreshDetail();
    await loadOrders();
  } catch (error) {
    console.error('发货失败', error);
    message.error('发货失败，请稍后重试');
  } finally {
    shipForm.loading = false;
  }
};

const handleReturnDecision = async (approve: boolean) => {
  if (!detail.order) {
    return;
  }
  if (!canApproveReturn.value) {
    message.warning('当前没有待处理的退租审批');
    return;
  }
  const vendorId = requireVendorId(true);
  if (!vendorId) {
    return;
  }
  returnForm.loading = true;
  try {
    await decideOrderReturn(detail.order.id, {
      vendorId,
      approve,
      remark: returnForm.remark || (approve ? '同意退租' : '拒绝退租')
    });
    message.success(approve ? '已同意退租申请' : '已驳回退租');
    returnForm.remark = '';
    await refreshDetail();
    await loadOrders();
  } catch (error) {
    console.error('退租审批失败', error);
    message.error('退租审批失败，请稍后重试');
  } finally {
    returnForm.loading = false;
  }
};

const handleReturnCompletion = async () => {
  if (!detail.order) {
    return;
  }
  if (!canCompleteReturn.value) {
    message.warning('当前状态无需完结退租');
    return;
  }
  const vendorId = requireVendorId(true);
  if (!vendorId) {
    return;
  }
  returnCompletionForm.loading = true;
  try {
    await completeOrderReturn(detail.order.id, {
      vendorId,
      remark: returnCompletionForm.remark || '已完成退租验收'
    });
    message.success('退租流程已完结');
    returnCompletionForm.remark = '';
    await refreshDetail();
    await loadOrders();
  } catch (error) {
    console.error('完结退租失败', error);
    message.error('完结退租失败，请稍后重试');
  } finally {
    returnCompletionForm.loading = false;
  }
};

const handleExtensionDecision = async (approve: boolean) => {
  if (!detail.order) {
    return;
  }
  if (!pendingExtension.value) {
    message.warning('当前没有待处理的续租申请');
    return;
  }
  const vendorId = requireVendorId(true);
  if (!vendorId) {
    return;
  }
  extensionDecisionForm.loading = true;
  try {
    await decideOrderExtension(detail.order.id, {
      vendorId,
      approve,
      remark: extensionDecisionForm.remark || (approve ? '同意续租' : '拒绝续租')
    });
    message.success(approve ? '已同意续租申请' : '已驳回续租申请');
    extensionDecisionForm.remark = '';
    await refreshDetail();
    await loadOrders();
  } catch (error) {
    console.error('续租审批失败', error);
    message.error('续租审批失败，请稍后重试');
  } finally {
    extensionDecisionForm.loading = false;
  }
};

const handleBuyoutDecision = async (approve: boolean) => {
  if (!detail.order) {
    return;
  }
  if (!isBuyoutRequested.value) {
    message.warning('当前没有待处理的买断申请');
    return;
  }
  const vendorId = requireVendorId(true);
  if (!vendorId) {
    return;
  }
  buyoutDecisionForm.loading = true;
  try {
    await decideOrderBuyout(detail.order.id, {
      vendorId,
      approve,
      remark: buyoutDecisionForm.remark || (approve ? '确认买断' : '拒绝买断')
    });
    message.success(approve ? '已确认买断' : '已拒绝买断申请');
    buyoutDecisionForm.remark = '';
    await refreshDetail();
    await loadOrders();
  } catch (error) {
    console.error('买断审批失败', error);
    message.error('买断审批失败，请稍后重试');
  } finally {
    buyoutDecisionForm.loading = false;
  }
};

const handleProofFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  proofForm.file = target.files && target.files.length > 0 ? target.files[0] : null;
};

const handleProofUpload = async () => {
  if (!detail.order) {
    return;
  }
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  if (!proofForm.file) {
    message.warning('请选择需要上传的文件');
    return;
  }
  proofForm.uploading = true;
  try {
    await uploadOrderProof(detail.order.id, {
      actorId: auth.user.id,
      proofType: proofForm.type,
      description: proofForm.description || undefined,
      file: proofForm.file
    });
    message.success('取证资料已上传');
    proofForm.description = '';
    proofForm.file = null;
    proofForm.inputKey = Date.now();
    await refreshDetail();
  } catch (error) {
    console.error('上传取证资料失败', error);
    message.error('上传取证资料失败，请稍后重试');
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
    message.error('下载取证资料失败，请稍后重试');
  }
};

const handleSendMessage = async () => {
  if (!detail.order) {
    return;
  }
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  const content = conversationForm.message.trim();
  if (!content) {
    message.warning('请输入需要发送的内容');
    return;
  }
  conversationForm.loading = true;
  try {
    await postOrderMessage(detail.order.id, {
      actorId: auth.user.id,
      message: content
    });
    conversationForm.message = '';
    await refreshDetail();
    message.success('留言已发送');
  } catch (error) {
    console.error('发送留言失败', error);
    message.error('发送留言失败，请稍后重试');
  } finally {
    conversationForm.loading = false;
  }
};

const openVendorDisputeModal = () => {
  if (!detail.order) {
    message.warning('请先选择订单');
    return;
  }
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  vendorDisputeModal.open = true;
};

const resetVendorDisputeModal = () => {
  vendorDisputeModal.reason = '';
  vendorDisputeModal.remark = '';
  vendorDisputeModal.option = 'REDELIVER';
};

const handleVendorSubmitDispute = async () => {
  if (!detail.order || !auth.user?.id) {
    return;
  }
  const reason = vendorDisputeModal.reason.trim();
  if (!reason) {
    message.warning('请填写纠纷原因');
    return;
  }
  vendorDisputeModal.loading = true;
  try {
    await createOrderDispute(detail.order.id, {
      actorId: auth.user.id,
      option: vendorDisputeModal.option,
      reason,
      remark: vendorDisputeModal.remark?.trim() || undefined
    });
    message.success('纠纷已创建');
    vendorDisputeModal.open = false;
    resetVendorDisputeModal();
    await refreshDetail();
  } catch (error) {
    console.error('创建纠纷失败', error);
    message.error('创建纠纷失败，请稍后重试');
  } finally {
    vendorDisputeModal.loading = false;
  }
};

const handleVendorCloseDisputeModal = () => {
  vendorDisputeModal.open = false;
  resetVendorDisputeModal();
};

const openVendorRespondModal = (dispute: OrderDispute) => {
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  vendorRespondModal.disputeId = dispute.id;
  vendorRespondModal.option = dispute.respondentOption ?? dispute.initiatorOption;
  vendorRespondModal.accept = false;
  vendorRespondModal.remark = '';
  vendorRespondModal.open = true;
};

const handleVendorSubmitRespond = async () => {
  if (!detail.order || !auth.user?.id || !vendorRespondModal.disputeId) {
    return;
  }
  vendorRespondModal.loading = true;
  try {
    await respondOrderDispute(detail.order.id, vendorRespondModal.disputeId, {
      actorId: auth.user.id,
      option: vendorRespondModal.option,
      accept: vendorRespondModal.accept,
      remark: vendorRespondModal.remark?.trim() || undefined
    });
    message.success('已提交回应');
    handleVendorCloseRespondModal();
    await refreshDetail();
  } catch (error) {
    console.error('回应纠纷失败', error);
    message.error('回应失败，请稍后重试');
  } finally {
    vendorRespondModal.loading = false;
  }
};

const openVendorSurveyModal = (survey: OrderSurvey) => {
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  surveyModal.surveyId = survey.id;
  surveyModal.rating = survey.rating ?? 5;
  surveyModal.comment = '';
  surveyModal.open = true;
};

const handleVendorSubmitSurvey = async () => {
  if (!detail.order || !surveyModal.surveyId || !auth.user?.id) {
    return;
  }
  surveyModal.loading = true;
  try {
    await submitOrderSurvey(detail.order.id, surveyModal.surveyId, {
      actorId: auth.user.id,
      rating: surveyModal.rating,
      comment: surveyModal.comment?.trim() || undefined
    });
    message.success('感谢反馈，我们已记录您的评价');
    surveyModal.open = false;
    surveyModal.comment = '';
    await refreshDetail();
  } catch (error) {
    console.error('提交满意度失败', error);
    message.error('提交满意度失败，请稍后重试');
  } finally {
    surveyModal.loading = false;
  }
};

const handleVendorCloseSurveyModal = () => {
  surveyModal.open = false;
  surveyModal.surveyId = null;
  surveyModal.comment = '';
};

const handleVendorCloseRespondModal = () => {
  vendorRespondModal.open = false;
  vendorRespondModal.disputeId = null;
  vendorRespondModal.remark = '';
};

const confirmVendorEscalate = (dispute: OrderDispute) => {
  Modal.confirm({
    title: '提交平台仲裁？',
    content: '平台会在 48 小时内回复处理结果。',
    okText: '提交',
    cancelText: '取消',
    onOk: () => performVendorEscalate(dispute)
  });
};

const performVendorEscalate = async (dispute: OrderDispute) => {
  if (!detail.order || !auth.user?.id) {
    return;
  }
  try {
    await escalateOrderDispute(detail.order.id, dispute.id, {
      actorId: auth.user.id,
      reason: '厂商申请平台仲裁'
    });
    message.success('已提交平台仲裁');
    await refreshDetail();
  } catch (error) {
    console.error('仲裁请求失败', error);
    message.error('仲裁请求失败，请稍后重试');
  }
};

const confirmVendorAppeal = (dispute: OrderDispute) => {
  Modal.confirm({
    title: '申诉复核',
    content: '确认向平台申诉？每个纠纷仅可申诉一次。',
    okText: '申诉',
    cancelText: '取消',
    onOk: () => performVendorAppeal(dispute)
  });
};

const performVendorAppeal = async (dispute: OrderDispute) => {
  if (!detail.order || !auth.user?.id) {
    return;
  }
  try {
    await appealOrderDispute(detail.order.id, dispute.id, {
      actorId: auth.user.id,
      reason: '厂商申请复核'
    });
    message.success('申诉已提交');
    await refreshDetail();
  } catch (error) {
    console.error('申诉失败', error);
    message.error('申诉失败，请稍后重试');
  }
};

watch(
  vendorReady,
  (ready) => {
    if (ready) {
      loadOrders();
    } else {
      orders.value = [];
      pagination.total = 0;
    }
  },
  { immediate: true }
);

watch(
  () => detail.open,
  (open) => {
    if (!open) {
      contractDrawerOpen.value = false;
    }
  }
);
</script>

<style scoped>
.detail__actions {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 16px;
}

.action-box {
  padding: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #f8fafc;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-box__meta {
  color: #475569;
  font-size: 13px;
}

.form-hint {
  margin-top: 6px;
  color: #94a3b8;
  font-size: 12px;
}

.timeline-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.proof-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
  margin: 12px 0;
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
  margin: 12px 0;
}

.conversation-item__meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #475569;
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
  font-size: 12px;
  color: #475569;
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
}

.dispute-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #475569;
  margin: 12px 0;
  gap: 12px;
}

.dispute-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
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
