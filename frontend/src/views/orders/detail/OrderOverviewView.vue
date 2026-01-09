<template>
  <div class="overview-grid" v-if="order">
    <PageSection title="租赁摘要">
      <div class="summary-list">
        <div class="summary-row">
          <span>状态</span>
          <OrderProgressPill :status="order.status" />
        </div>
        <div class="summary-row">
          <span>方案</span>
          <strong>{{ order.planType ?? 'N/A' }}</strong>
        </div>
        <div class="summary-row">
          <span>押金 / 租金</span>
          <strong>¥{{ order.depositAmount.toFixed(2) }} / ¥{{ order.rentAmount.toFixed(2) }}</strong>
        </div>
        <div class="summary-row">
          <span>租期</span>
          <strong>{{ order.leaseStartAt ?? '--' }} ~ {{ order.leaseEndAt ?? '--' }}</strong>
        </div>
      </div>
      <div class="credit-card">
        <div class="credit-card__header">
          <span>信用快照</span>
          <a-tag :color="creditColor(order.creditTier)">
            {{ creditLabel(order.creditTier) }} · {{ order.creditScore }} 分
          </a-tag>
        </div>
        <p>押金原价 ¥{{ order.originalDepositAmount.toFixed(2) }}，{{ depositAdjustmentText }}</p>
        <a-alert
          v-if="order.requiresManualReview"
          type="warning"
          show-icon
          message="信用预警：平台将关注该订单（不影响正常支付与履约）"
        />
      </div>
      <OrderActionBar
        :order="order"
        @pay="handlePay"
        @cancel="handleCancel"
        @confirm="handleConfirmReceive"
        @extend="showExtendModal = true"
        @return="showReturnModal = true"
        @buyout="showBuyoutModal = true"
        @chat="goChat"
      />
      <div class="contract-entry">
        <div>
          <p>电子合同 · 已同步订单条款</p>
          <small>签署完成后会写入时间线并触发通知</small>
        </div>
        <a-button type="link" size="small" @click="showContractDrawer = true">
          查看 / 签署
        </a-button>
      </div>
    </PageSection>

    <PageSection title="物流 / 履约">
      <p>承运方：{{ order.shippingCarrier ?? '待填写' }}</p>
      <p>运单号：{{ order.shippingTrackingNo ?? '-' }}</p>
      <p>自定义备注：{{ order.customerRemark ?? '无' }}</p>
    </PageSection>

    <PageSection title="凭证要求" description="下单快照 + 最新拍摄指引，确保凭证达标后再提交操作。">
      <div class="proof-grid">
        <div>
          <h4>发货</h4>
          <p>照片 ≥ {{ order.shipmentPhotoRequired }} · 视频 ≥ {{ order.shipmentVideoRequired }}</p>
        </div>
        <div>
          <h4>收货</h4>
          <p>照片 ≥ {{ order.receivePhotoRequired }} · 视频 ≥ {{ order.receiveVideoRequired }}</p>
        </div>
        <div>
          <h4>退租</h4>
          <p>照片 ≥ {{ order.returnPhotoRequired }} · 视频 ≥ {{ order.returnVideoRequired }}</p>
        </div>
      </div>
      <a-skeleton :loading="proofPolicyLoading" active>
        <template #default>
          <div v-if="proofPolicyStages.length" class="policy-grid">
            <div v-for="stage in proofPolicyStages" :key="stage.key" class="policy-card">
              <div class="policy-card__header">
                <div>
                  <strong>{{ stage.label }}</strong>
                  <p>{{ stage.summary }}</p>
                </div>
              </div>
              <ul>
                <li v-for="(tip, index) in stage.guidance" :key="index">{{ tip }}</li>
              </ul>
              <a-button
                v-if="stage.watermark"
                type="link"
                size="small"
                @click="openWatermarkExample(stage.watermark)"
              >
                查看水印示例
              </a-button>
            </div>
          </div>
          <DataStateBlock
            v-else
            type="empty"
            title="暂无指引"
            description="稍后刷新即可自动同步最新凭证策略。"
          />
        </template>
      </a-skeleton>
    </PageSection>
  </div>

  <a-modal
    v-model:open="showExtendModal"
    title="续租申请"
    ok-text="提交申请"
    :confirm-loading="modalLoading"
    @ok="handleExtension"
  >
    <a-form layout="vertical">
      <a-form-item label="追加月份">
        <a-input-number v-model:value="extensionForm.additionalMonths" :min="1" />
      </a-form-item>
      <a-form-item label="备注">
        <a-input v-model:value="extensionForm.remark" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="showReturnModal"
    title="退租申请"
    ok-text="提交申请"
    :confirm-loading="modalLoading"
    @ok="handleReturn"
  >
    <a-form layout="vertical">
      <a-form-item label="退租原因">
        <a-textarea v-model:value="returnForm.reason" :rows="3" />
      </a-form-item>
      <a-form-item label="物流公司">
        <a-input v-model:value="returnForm.logisticsCompany" placeholder="如 SF、JD" />
      </a-form-item>
      <a-form-item label="运单号">
        <a-input v-model:value="returnForm.trackingNumber" placeholder="请填写退租寄回单号" />
      </a-form-item>
    </a-form>
  </a-modal>

  <a-modal
    v-model:open="showBuyoutModal"
    title="买断申请"
    ok-text="提交申请"
    :confirm-loading="modalLoading"
    @ok="handleBuyout"
  >
    <a-form layout="vertical">
      <a-form-item label="买断金额">
        <a-input-number v-model:value="buyoutForm.buyoutAmount" :min="0" />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="buyoutForm.remark" :rows="3" />
      </a-form-item>
    </a-form>
  </a-modal>

  <OrderContractDrawer
    v-if="order"
    v-model:open="showContractDrawer"
    :order-id="order.id"
    :user-id="auth.user?.id ?? null"
    :allow-sign="order.userId === auth.user?.id"
    :default-signature="defaultSignature"
    @signed="handleContractSigned"
  />
</template>

<script lang="ts" setup>
// 订单概览页：展示金额/物流/信用快照等摘要，并串联合同、支付与履约操作。
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import PageSection from '../../../components/layout/PageSection.vue';
import DataStateBlock from '../../../components/feedback/DataStateBlock.vue';
import OrderProgressPill from '../../../components/orders/OrderProgressPill.vue';
import OrderActionBar from '../../../components/orders/OrderActionBar.vue';
import OrderContractDrawer from '../../../components/orders/OrderContractDrawer.vue';
import { useOrderDetail } from '../../../composables/useOrderDetail';
import { useQuery } from '../../../composables/useQuery';
import { useAuthStore } from '../../../stores/auth';
import {
  cancelOrder,
  confirmOrderReceive,
  applyOrderExtension,
  applyOrderReturn,
  applyOrderBuyout,
  fetchProofPolicy,
  type RentalOrderDetail,
  type ProofPolicySummary
} from '../../../services/orderService';
import { autoCompleteInitialPayment } from '../../../utils/autoPayment';
import { friendlyErrorMessage } from '../../../utils/error';
import { creditTierColor, creditTierLabel } from '../../../types/credit';

const { order: getOrder, updateOrder, refresh } = useOrderDetail();
const auth = useAuthStore();
const router = useRouter();
const order = computed(() => getOrder());
const showContractDrawer = ref(false);
const defaultSignature = computed(() => auth.user?.username ?? '');

const showExtendModal = ref(false);
const showReturnModal = ref(false);
const showBuyoutModal = ref(false);
const modalLoading = ref(false);

const extensionForm = ref({ additionalMonths: 1, remark: '' });
const returnForm = ref({ reason: '', logisticsCompany: '', trackingNumber: '' });
const buyoutForm = ref<{ buyoutAmount?: number; remark?: string }>({});
const creditColor = creditTierColor;
const creditLabel = creditTierLabel;
const depositAdjustmentText = computed(() => {
  if (!order.value) {
    return '押金按标准计算';
  }
  const rate = order.value.depositAdjustmentRate ?? 1;
  if (Math.abs(rate - 1) < 0.001) {
    return '押金维持原价';
  }
  const percent = Math.abs(rate - 1) * 100;
  return rate < 1 ? `减免 ${percent.toFixed(0)}%` : `上浮 ${percent.toFixed(0)}%`;
});

const { data: proofPolicyData, loading: proofPolicyLoading } = useQuery<ProofPolicySummary>(
  'order-proof-policy',
  () => fetchProofPolicy()
);
const proofPolicy = computed(() => proofPolicyData.value);
const proofPolicyStages = computed(() => {
  if (!proofPolicy.value) {
    return [];
  }
  return [
    { key: 'shipment', label: '发货', stage: proofPolicy.value.shipment },
    { key: 'receive', label: '收货', stage: proofPolicy.value.receive },
    { key: 'returns', label: '退租', stage: proofPolicy.value.returns }
  ].map((entry) => ({
    key: entry.key,
    label: entry.label,
    summary: `照片 ${entry.stage.photosRequired} 张 · 视频 ${entry.stage.videosRequired} 段`,
    guidance: entry.stage.guidance ?? [],
    watermark: entry.stage.watermarkExample
  }));
});

const openWatermarkExample = (value?: string | null) => {
  if (!value) {
    return;
  }
  const normalized = value.trim();
  if (!normalized) {
    return;
  }
  const looksLikeUrl = /^https?:\/\//i.test(normalized) || normalized.startsWith('/');
  if (looksLikeUrl) {
    window.open(normalized, '_blank');
    return;
  }
  Modal.info({
    title: '水印示例',
    content: normalized,
    okText: '知道了'
  });
};

const requireAuthUser = () => {
  if (!auth.user) {
    throw new Error('需要登录');
  }
  return auth.user;
};

const handlePay = async () => {
  if (!order.value) {
    return;
  }
  try {
    const result = await autoCompleteInitialPayment({
      orderId: order.value.id,
      vendorId: order.value.vendorId,
      userId: order.value.userId,
      amount: order.value.totalAmount,
      depositAmount: order.value.depositAmount,
      rentAmount: order.value.rentAmount,
      buyoutAmount: order.value.buyoutAmount ?? undefined,
      description: '订单补款'
    });
    if (result.succeeded) {
      message.success('支付成功');
      await refresh();
    } else {
      message.info('已生成支付流水，请稍候刷新');
    }
  } catch (error) {
    message.error(friendlyErrorMessage(error, '支付失败'));
  }
};

const handleCancel = async () => {
  if (!order.value) {
    return;
  }
  const user = requireAuthUser();
  Modal.confirm({
    title: '确认取消该订单？',
    onOk: async () => {
      const updated = await cancelOrder(order.value!.id, { userId: user.id });
      updateOrder(updated);
      message.success('订单已取消');
    }
  });
};

const handleConfirmReceive = async () => {
  if (!order.value) {
    return;
  }
  const user = requireAuthUser();
  Modal.confirm({
    title: '确认已收到货物？',
    onOk: async () => {
      const updated = await confirmOrderReceive(order.value!.id, { actorId: user.id });
      updateOrder(updated);
      message.success('已确认收货');
    }
  });
};

const handleExtension = async () => {
  if (!order.value) {
    return;
  }
  const user = requireAuthUser();
  modalLoading.value = true;
  try {
    const updated = await applyOrderExtension(order.value.id, {
      userId: user.id,
      additionalMonths: extensionForm.value.additionalMonths,
      remark: extensionForm.value.remark
    });
    updateOrder(updated);
    showExtendModal.value = false;
    message.success('已提交续租申请');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '续租失败'));
  } finally {
    modalLoading.value = false;
  }
};

const handleReturn = async () => {
  if (!order.value) {
    return;
  }
  const user = requireAuthUser();
  const logisticsCompany = returnForm.value.logisticsCompany.trim();
  const trackingNumber = returnForm.value.trackingNumber.trim();
  if ((logisticsCompany && !trackingNumber) || (!logisticsCompany && trackingNumber)) {
    message.warning('请同时填写物流公司与运单号，或暂时留空稍后补充');
    return;
  }
  const reason = returnForm.value.reason?.trim();
  modalLoading.value = true;
  try {
    const updated = await applyOrderReturn(order.value.id, {
      userId: user.id,
      reason: reason || undefined,
      logisticsCompany: logisticsCompany || undefined,
      trackingNumber: trackingNumber || undefined
    });
    updateOrder(updated);
    showReturnModal.value = false;
    returnForm.value = { reason: '', logisticsCompany: '', trackingNumber: '' };
    message.success('退租申请已提交');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '退租申请失败'));
  } finally {
    modalLoading.value = false;
  }
};

const handleBuyout = async () => {
  if (!order.value) {
    return;
  }
  const user = requireAuthUser();
  modalLoading.value = true;
  try {
    const updated = await applyOrderBuyout(order.value.id, {
      userId: user.id,
      buyoutAmount: buyoutForm.value.buyoutAmount,
      remark: buyoutForm.value.remark
    });
    updateOrder(updated);
    showBuyoutModal.value = false;
    message.success('买断申请已提交');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '买断申请失败'));
  } finally {
    modalLoading.value = false;
  }
};

const goChat = () => {
  router.push({ name: 'order-chat', params: { orderId: order.value?.id } });
};

const handleContractSigned = async () => {
  await refresh();
};
</script>

<style scoped>
.overview-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.summary-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-3);
}

.credit-card {
  margin-top: var(--space-3);
  padding: var(--space-3);
  border-radius: var(--radius-card);
  border: 1px solid var(--color-border);
  background: var(--color-surface-muted);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.credit-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.proof-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: var(--space-3);
}

.policy-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.policy-card {
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: var(--radius-card);
  padding: var(--space-3);
  background: rgba(37, 99, 235, 0.04);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.policy-card__header p {
  margin: 0;
  color: var(--color-text-secondary);
}

.policy-card ul {
  padding-left: 1.2rem;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.contract-entry {
  margin-top: var(--space-3);
  padding: var(--space-3);
  background: var(--color-surface-muted);
  border-radius: var(--radius-card);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.contract-entry small {
  color: var(--color-text-secondary);
}
</style>
