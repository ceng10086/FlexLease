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
                <a :href="item.fileUrl" target="_blank" rel="noopener">查看文件</a>
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
            :disabled="order.status !== 'AWAITING_SHIPMENT' && order.status !== 'IN_LEASE'"
            :loading="receiveLoading"
            @click="confirmReceive"
          >
            确认收到设备
          </a-button>
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
  type RentalOrderDetail,
  type OrderEvent,
  type OrderProofType,
  type OrderProof
} from '../../services/orderService';
import { initPayment } from '../../services/paymentService';
import {
  resolveItemDeposit,
  resolveItemRent,
  rentalOrderDeposit,
  rentalOrderRent,
  rentalOrderTotal
} from '../../utils/orderAmounts';
import OrderContractDrawer from '../../components/orders/OrderContractDrawer.vue';
import { friendlyErrorMessage } from '../../utils/error';

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

const proofTypeOptions: { label: string; value: OrderProofType }[] = [
  { label: '收货/验收', value: 'RECEIVE' },
  { label: '发货凭证', value: 'SHIPMENT' },
  { label: '退租寄回', value: 'RETURN' },
  { label: '巡检记录', value: 'INSPECTION' },
  { label: '其他', value: 'OTHER' }
];
const proofTypeMap = proofTypeOptions.reduce<Record<OrderProofType, string>>((acc, option) => {
  acc[option.value] = option.label;
  return acc;
}, {} as Record<OrderProofType, string>);

const formatCurrency = (value: number) => value.toFixed(2);
const formatDate = (value: string) => new Date(value).toLocaleString();
const proofTypeLabel = (type: OrderProofType) => proofTypeMap[type] ?? type;
const conversationEvents = computed(() =>
  order.value?.events?.filter((item) => item.eventType === 'COMMUNICATION_NOTE') ?? []
);
const proofList = computed(() => order.value?.proofs ?? []);
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

const handleCreatePayment = async () => {
  if (!order.value) {
    return;
  }
  if (!auth.user?.id) {
    message.error('请先登录');
    return;
  }
  paymentForm.loading = true;
  try {
    const result = await initPayment(order.value.id, {
      userId: auth.user.id,
      vendorId: order.value.vendorId,
      scene: paymentForm.scene as any,
      channel: 'MOCK',
      amount: paymentForm.amount
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
</style>
