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

    <PageSection title="凭证要求">
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
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { message, Modal } from 'ant-design-vue';
import PageSection from '../../../components/layout/PageSection.vue';
import OrderProgressPill from '../../../components/orders/OrderProgressPill.vue';
import OrderActionBar from '../../../components/orders/OrderActionBar.vue';
import OrderContractDrawer from '../../../components/orders/OrderContractDrawer.vue';
import { useOrderDetail } from '../../../composables/useOrderDetail';
import { useAuthStore } from '../../../stores/auth';
import {
  cancelOrder,
  confirmOrderReceive,
  applyOrderExtension,
  applyOrderReturn,
  applyOrderBuyout,
  type RentalOrderDetail
} from '../../../services/orderService';
import { autoCompleteInitialPayment } from '../../../utils/autoPayment';
import { friendlyErrorMessage } from '../../../utils/error';

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
  if (!returnForm.value.logisticsCompany.trim() || !returnForm.value.trackingNumber.trim()) {
    message.warning('请填写物流公司与运单号');
    return;
  }
  modalLoading.value = true;
  try {
    const updated = await applyOrderReturn(order.value.id, {
      userId: user.id,
      reason: returnForm.value.reason,
      logisticsCompany: returnForm.value.logisticsCompany,
      trackingNumber: returnForm.value.trackingNumber
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

.proof-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: var(--space-3);
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
