<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>运营工具箱</h2>
        <p class="page-header__meta">用于本地联调和排障的综合工具，覆盖订单、支付与通知核心流程。</p>
      </div>
    </div>

    <a-tabs v-model:activeKey="activeTab">
      <a-tab-pane key="orders" tab="订单工具">
        <a-row :gutter="16">
          <a-col :xs="24" :lg="12">
            <a-card title="订单试算 / 创建">
              <a-form layout="vertical">
                <a-form-item label="用户 ID" required>
                  <a-input v-model:value="orderPreview.userId" placeholder="用户 UUID" />
                </a-form-item>
                <a-form-item label="厂商 ID" required>
                  <a-input v-model:value="orderPreview.vendorId" placeholder="厂商 UUID" />
                </a-form-item>
                <a-form-item label="订单明细 (JSON)" required>
                  <a-textarea v-model:value="orderPreview.itemsJson" :rows="6" :placeholder="orderItemsPlaceholder" />
                </a-form-item>
                <a-space>
                  <a-button type="primary" :loading="orderPreview.loading" @click="handleOrderPreview">
                    计算价格
                  </a-button>
                  <a-button :loading="orderCreate.loading" @click="handleOrderCreate">
                    创建订单
                  </a-button>
                </a-space>
              </a-form>
              <ResultBlock v-if="orderPreview.result" title="试算结果" :content="orderPreview.result" />
              <ResultBlock v-if="orderCreate.result" title="创建订单响应" :content="orderCreate.result" />
            </a-card>
          </a-col>
          <a-col :xs="24" :lg="12">
            <a-card title="订单操作">
              <a-form layout="vertical">
                <a-form-item label="订单 ID" required>
                  <a-input v-model:value="orderOps.orderId" placeholder="订单 UUID" />
                </a-form-item>
                <a-form-item label="用户 ID">
                  <a-input v-model:value="orderOps.userId" placeholder="支付/确认收货场景" />
                </a-form-item>
                <a-form-item label="厂商 ID">
                  <a-input v-model:value="orderOps.vendorId" placeholder="发货/退租场景" />
                </a-form-item>
                <a-form-item label="支付流水 ID">
                  <a-input v-model:value="orderOps.transactionId" placeholder="支付确认时使用" />
                </a-form-item>
                <a-form-item label="支付金额">
                  <a-input-number v-model:value="orderOps.paidAmount" :min="0" style="width: 100%" />
                </a-form-item>
                <a-form-item label="物流信息">
                  <a-space direction="vertical" style="width: 100%">
                    <a-input v-model:value="orderOps.carrier" placeholder="承运方" />
                    <a-input v-model:value="orderOps.trackingNumber" placeholder="运单号" />
                  </a-space>
                </a-form-item>
                <a-space wrap>
                  <a-button type="primary" @click="handleOrderPayment" :loading="orderOps.loading">确认支付</a-button>
                  <a-button @click="handleOrderShip" :loading="orderOps.loading">发货</a-button>
                  <a-button @click="handleOrderReceive" :loading="orderOps.loading">确认收货</a-button>
                  <a-button @click="handleOrderReturn" :loading="orderOps.loading">申请退租</a-button>
                  <a-button @click="handleOrderApproveReturn" :loading="orderOps.loading">完成退租</a-button>
                </a-space>
              </a-form>
              <ResultBlock v-if="orderOps.result" title="订单操作响应" :content="orderOps.result" />
            </a-card>
            <a-card title="订单查询" class="mt-16">
              <a-space align="start" wrap>
                <a-input v-model:value="orderQuery.orderId" placeholder="订单 UUID" style="width: 280px" />
                <a-button type="primary" :loading="orderQuery.loading" @click="handleOrderQuery">查询</a-button>
              </a-space>
              <ResultBlock v-if="orderQuery.result" title="订单详情" :content="orderQuery.result" />
            </a-card>
          </a-col>
        </a-row>
      </a-tab-pane>

      <a-tab-pane key="payments" tab="支付工具">
        <a-row :gutter="16">
          <a-col :xs="24" :lg="12">
            <a-card title="创建支付流水">
              <a-form layout="vertical">
                <a-form-item label="订单 ID" required><a-input v-model:value="paymentInit.orderId" /></a-form-item>
                <a-row :gutter="12">
                  <a-col :span="12">
                    <a-form-item label="用户 ID" required><a-input v-model:value="paymentInit.userId" /></a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="厂商 ID" required><a-input v-model:value="paymentInit.vendorId" /></a-form-item>
                  </a-col>
                </a-row>
                <a-row :gutter="12">
                  <a-col :span="12">
                    <a-form-item label="支付场景" required>
                      <a-select v-model:value="paymentInit.scene">
                        <a-select-option value="DEPOSIT">押金</a-select-option>
                        <a-select-option value="RENT">租金</a-select-option>
                        <a-select-option value="BUYOUT">买断</a-select-option>
                        <a-select-option value="PENALTY">违约金</a-select-option>
                      </a-select>
                    </a-form-item>
                  </a-col>
                  <a-col :span="12">
                    <a-form-item label="支付渠道" required>
                      <a-select v-model:value="paymentInit.channel">
                        <a-select-option value="MOCK">模拟</a-select-option>
                        <a-select-option value="ALIPAY">支付宝</a-select-option>
                        <a-select-option value="WECHAT">微信</a-select-option>
                        <a-select-option value="BANK_TRANSFER">银行转账</a-select-option>
                      </a-select>
                    </a-form-item>
                  </a-col>
                </a-row>
                <a-form-item label="金额" required>
                  <a-input-number v-model:value="paymentInit.amount" :min="0.01" :step="0.01" style="width: 100%" />
                </a-form-item>
                <a-form-item label="分账明细 (JSON)">
                  <a-textarea v-model:value="paymentInit.splitsJson" :rows="4" :placeholder="paymentSplitsPlaceholder" />
                </a-form-item>
                <a-button type="primary" :loading="paymentInit.loading" @click="handlePaymentInit">创建支付流水</a-button>
              </a-form>
              <ResultBlock v-if="paymentInit.result" title="支付流水" :content="paymentInit.result" />
            </a-card>
          </a-col>
          <a-col :xs="24" :lg="12">
            <a-card title="支付操作">
              <a-form layout="vertical">
                <a-form-item label="支付流水 ID" required><a-input v-model:value="paymentOps.transactionId" /></a-form-item>
                <a-form-item label="渠道交易号"><a-input v-model:value="paymentOps.channelTransactionNo" /></a-form-item>
                <a-form-item label="支付完成时间"><a-input v-model:value="paymentOps.paidAt" placeholder="2025-01-01T00:00:00Z" /></a-form-item>
                <a-form-item label="退款金额">
                  <a-input-number v-model:value="paymentOps.refundAmount" :min="0.01" :step="0.01" style="width: 100%" />
                </a-form-item>
                <a-space wrap>
                  <a-button type="primary" :loading="paymentOps.loading" @click="handlePaymentConfirm">标记成功</a-button>
                  <a-button :loading="paymentOps.loading" @click="handlePaymentLoad">查询详情</a-button>
                  <a-button danger :loading="paymentOps.loading" @click="handlePaymentRefund">立即退款</a-button>
                </a-space>
              </a-form>
              <ResultBlock v-if="paymentOps.result" title="支付结果" :content="paymentOps.result" />
            </a-card>
            <a-card title="结算汇总" class="mt-16">
              <a-form layout="inline" @submit.prevent>
                <a-form-item label="厂商 ID">
                  <a-input v-model:value="settlement.vendorId" style="width: 220px" />
                </a-form-item>
                <a-form-item label="时间窗口">
                  <a-input v-model:value="settlement.from" placeholder="from" style="width: 160px" />
                  <a-input v-model:value="settlement.to" placeholder="to" style="width: 160px" />
                </a-form-item>
                <a-button type="primary" :loading="settlement.loading" @click="handleSettlement">查询</a-button>
              </a-form>
              <ResultBlock v-if="settlement.result" title="结算结果" :content="settlement.result" />
            </a-card>
          </a-col>
        </a-row>
      </a-tab-pane>

      <a-tab-pane key="notifications" tab="通知工具">
        <a-row :gutter="16">
          <a-col :xs="24" :lg="10">
            <a-card title="发送通知">
              <a-form layout="vertical">
                <a-form-item label="模板编码">
                  <a-select v-model:value="notificationSend.templateCode" allow-clear placeholder="可选模板">
                    <a-select-option v-for="tpl in templates" :key="tpl.code" :value="tpl.code">
                      {{ tpl.code }} ({{ tpl.channel }})
                    </a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item label="渠道">
                  <a-select v-model:value="notificationSend.channel" allow-clear>
                    <a-select-option value="IN_APP">站内信</a-select-option>
                    <a-select-option value="EMAIL">邮件</a-select-option>
                    <a-select-option value="SMS">短信</a-select-option>
                  </a-select>
                </a-form-item>
                <a-form-item label="接收人" required><a-input v-model:value="notificationSend.recipient" /></a-form-item>
                <a-form-item label="标题"><a-input v-model:value="notificationSend.subject" /></a-form-item>
                <a-form-item label="内容"><a-textarea v-model:value="notificationSend.content" :rows="3" /></a-form-item>
                <a-form-item label="变量 (JSON)">
                  <a-textarea v-model:value="notificationSend.variablesJson" :rows="3" :placeholder="notificationVariablesPlaceholder" />
                </a-form-item>
                <a-button type="primary" :loading="notificationSend.loading" @click="handleSendNotification">发送通知</a-button>
              </a-form>
              <ResultBlock v-if="notificationSend.result" title="发送结果" :content="notificationSend.result" />
            </a-card>
          </a-col>
          <a-col :xs="24" :lg="14">
            <a-card title="通知日志">
              <a-space class="mb-16">
                <a-select v-model:value="notificationLogs.status" allow-clear style="width: 160px" placeholder="状态过滤">
                  <a-select-option value="PENDING">待发送</a-select-option>
                  <a-select-option value="SENT">已发送</a-select-option>
                  <a-select-option value="FAILED">发送失败</a-select-option>
                </a-select>
                <a-button type="primary" :loading="notificationLogs.loading" @click="loadNotificationLogs">刷新</a-button>
              </a-space>
              <a-table :data-source="notificationLogs.data" :loading="notificationLogs.loading" row-key="id" :pagination="false">
                <a-table-column title="时间" key="createdAt">
                  <template #default="{ record }">{{ formatDate(record.createdAt) }}</template>
                </a-table-column>
                <a-table-column title="渠道" data-index="channel" key="channel" />
                <a-table-column title="接收人" data-index="recipient" key="recipient" />
                <a-table-column title="标题" data-index="subject" key="subject" />
                <a-table-column title="状态" key="status">
                  <template #default="{ record }">
                    <a-tag :color="notificationStatusColor(record.status)">{{ record.status }}</a-tag>
                  </template>
                </a-table-column>
              </a-table>
            </a-card>
          </a-col>
        </a-row>
      </a-tab-pane>
    </a-tabs>
  </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import ResultBlock from './components/ResultBlock.vue';
import {
  previewOrder,
  createOrder,
  confirmOrderPayment,
  shipOrder,
  confirmOrderReceive,
  applyOrderReturn,
  decideOrderReturn,
  fetchOrder
} from '../../services/orderService';
import {
  initPayment,
  confirmPayment,
  fetchPayment,
  refundPayment,
  listSettlements,
  type PaymentScene,
  type PaymentChannel
} from '../../services/paymentService';
import {
  sendNotification,
  listNotificationTemplates,
  listNotificationLogs,
  type NotificationChannel,
  type NotificationStatus,
  type NotificationTemplate,
  type NotificationLog
} from '../../services/notificationService';

const activeTab = ref('orders');

const orderItemsPlaceholder = `[
  {
    "productId": "11111111-1111-1111-1111-111111111111",
    "skuId": "22222222-2222-2222-2222-222222222222",
    "planId": "33333333-3333-3333-3333-333333333333",
    "productName": "办公桌",
    "skuCode": "DESK-001",
    "planSnapshot": "{\\"termMonths\\":12}",
    "quantity": 1,
    "unitRentAmount": 299,
    "unitDepositAmount": 500,
    "buyoutPrice": 2599
  }
]`;

const paymentSplitsPlaceholder = `[
  { "splitType": "VENDOR_INCOME", "amount": 1000, "beneficiary": "vendor_account" }
]`;

const notificationVariablesPlaceholder = '{ "orderNo": "2025..." }';

const orderPreview = reactive({
  userId: '',
  vendorId: '',
  planType: '',
  itemsJson: '',
  loading: false,
  result: ''
});

const orderCreate = reactive({ loading: false, result: '' });
const orderOps = reactive({
  orderId: '',
  userId: '',
  vendorId: '',
  transactionId: '',
  paidAmount: undefined as number | undefined,
  carrier: '',
  trackingNumber: '',
  loading: false,
  result: ''
});

const orderQuery = reactive({ orderId: '', loading: false, result: '' });

const paymentInit = reactive({
  orderId: '',
  userId: '',
  vendorId: '',
  scene: 'DEPOSIT' as PaymentScene,
  channel: 'MOCK' as PaymentChannel,
  amount: 0,
  splitsJson: '',
  loading: false,
  result: ''
});

const paymentOps = reactive({
  transactionId: '',
  channelTransactionNo: '',
  paidAt: '',
  refundAmount: undefined as number | undefined,
  loading: false,
  result: ''
});

const settlement = reactive({ vendorId: '', from: '', to: '', loading: false, result: '' });

const templates = ref<NotificationTemplate[]>([]);
const notificationSend = reactive({
  templateCode: undefined as string | undefined,
  channel: undefined as NotificationChannel | undefined,
  recipient: '',
  subject: '',
  content: '',
  variablesJson: '',
  loading: false,
  result: ''
});

const notificationLogs = reactive({
  status: undefined as NotificationStatus | undefined,
  data: [] as NotificationLog[],
  loading: false
});

const jsonSafeStringify = (payload: unknown) => JSON.stringify(payload, null, 2);

const parseOrderItems = () => {
  try {
    const parsed = JSON.parse(orderPreview.itemsJson || '[]');
    if (!Array.isArray(parsed) || parsed.length === 0) {
      throw new Error('请提供至少一条订单明细');
    }
    return parsed;
  } catch (error: any) {
    throw new Error(`订单明细 JSON 解析失败: ${error.message ?? error}`);
  }
};

const handleOrderPreview = async () => {
  if (!orderPreview.userId || !orderPreview.vendorId) {
    message.warning('请填写用户与厂商 ID');
    return;
  }
  orderPreview.loading = true;
  orderPreview.result = '';
  try {
    const response = await previewOrder({
      userId: orderPreview.userId,
      vendorId: orderPreview.vendorId,
      planType: orderPreview.planType || undefined,
      items: parseOrderItems()
    });
    orderPreview.result = jsonSafeStringify(response);
    message.success('试算完成');
  } catch (error: any) {
    console.error('订单试算失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '试算失败');
  } finally {
    orderPreview.loading = false;
  }
};

const handleOrderCreate = async () => {
  if (orderCreate.loading) return;
  try {
    orderCreate.loading = true;
    orderCreate.result = '';
    const response = await createOrder({
      userId: orderPreview.userId,
      vendorId: orderPreview.vendorId,
      planType: orderPreview.planType || undefined,
      items: parseOrderItems()
    });
    orderCreate.result = jsonSafeStringify(response);
    message.success('订单创建成功');
  } catch (error: any) {
    console.error('创建订单失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '创建失败');
  } finally {
    orderCreate.loading = false;
  }
};

const ensureOrderContext = () => {
  if (!orderOps.orderId) {
    throw new Error('请先填写订单 ID');
  }
};

const handleOrderPayment = async () => {
  try {
    ensureOrderContext();
    if (!orderOps.userId || !orderOps.transactionId || orderOps.paidAmount == null) {
      throw new Error('确认支付需提供用户 ID、支付流水 ID 与金额');
    }
    orderOps.loading = true;
    const response = await confirmOrderPayment(orderOps.orderId, {
      userId: orderOps.userId,
      paymentReference: orderOps.transactionId,
      paidAmount: orderOps.paidAmount
    });
    orderOps.result = jsonSafeStringify(response);
    message.success('订单支付已确认');
  } catch (error: any) {
    console.error('确认支付失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '确认支付失败');
  } finally {
    orderOps.loading = false;
  }
};

const handleOrderShip = async () => {
  try {
    ensureOrderContext();
    if (!orderOps.vendorId || !orderOps.carrier || !orderOps.trackingNumber) {
      throw new Error('发货需提供厂商 ID、承运方与运单号');
    }
    orderOps.loading = true;
    const response = await shipOrder(orderOps.orderId, {
      vendorId: orderOps.vendorId,
      carrier: orderOps.carrier,
      trackingNumber: orderOps.trackingNumber
    });
    orderOps.result = jsonSafeStringify(response);
    message.success('已提交发货');
  } catch (error: any) {
    console.error('发货失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '发货失败');
  } finally {
    orderOps.loading = false;
  }
};

const handleOrderReceive = async () => {
  try {
    ensureOrderContext();
    if (!orderOps.userId) {
      throw new Error('确认收货需提供用户 ID');
    }
    orderOps.loading = true;
    const response = await confirmOrderReceive(orderOps.orderId, { actorId: orderOps.userId });
    orderOps.result = jsonSafeStringify(response);
    message.success('确认收货成功');
  } catch (error: any) {
    console.error('确认收货失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '确认收货失败');
  } finally {
    orderOps.loading = false;
  }
};

const handleOrderReturn = async () => {
  try {
    ensureOrderContext();
    if (!orderOps.userId) {
      throw new Error('退租申请需提供用户 ID');
    }
    orderOps.loading = true;
    const response = await applyOrderReturn(orderOps.orderId, {
      userId: orderOps.userId,
      reason: '工具箱退租申请',
      logisticsCompany: orderOps.carrier || undefined,
      trackingNumber: orderOps.trackingNumber || undefined
    });
    orderOps.result = jsonSafeStringify(response);
    message.success('退租申请已提交');
  } catch (error: any) {
    console.error('退租申请失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '退租申请失败');
  } finally {
    orderOps.loading = false;
  }
};

const handleOrderApproveReturn = async () => {
  try {
    ensureOrderContext();
    if (!orderOps.vendorId) {
      throw new Error('完成退租需提供厂商 ID');
    }
    orderOps.loading = true;
    const response = await decideOrderReturn(orderOps.orderId, {
      vendorId: orderOps.vendorId,
      approve: true,
      remark: '运营工具箱确认退租'
    });
    orderOps.result = jsonSafeStringify(response);
    message.success('退租流程已完成');
  } catch (error: any) {
    console.error('退租审批失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '退租审批失败');
  } finally {
    orderOps.loading = false;
  }
};

const handleOrderQuery = async () => {
  if (!orderQuery.orderId) {
    message.warning('请填写订单 ID');
    return;
  }
  orderQuery.loading = true;
  orderQuery.result = '';
  try {
    const response = await fetchOrder(orderQuery.orderId);
    orderQuery.result = jsonSafeStringify(response);
  } catch (error: any) {
    console.error('查询订单失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '查询订单失败');
  } finally {
    orderQuery.loading = false;
  }
};

const handlePaymentInit = async () => {
  if (!paymentInit.orderId || !paymentInit.userId || !paymentInit.vendorId) {
    message.warning('请完整填写订单/用户/厂商信息');
    return;
  }
  paymentInit.loading = true;
  paymentInit.result = '';
  try {
    let splits;
    if (paymentInit.splitsJson.trim()) {
      splits = JSON.parse(paymentInit.splitsJson);
    }
    const response = await initPayment(paymentInit.orderId, {
      userId: paymentInit.userId,
      vendorId: paymentInit.vendorId,
      scene: paymentInit.scene,
      channel: paymentInit.channel,
      amount: paymentInit.amount,
      splits
    });
    paymentInit.result = jsonSafeStringify(response);
    message.success('支付流水已创建');
  } catch (error: any) {
    console.error('创建支付流水失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '创建支付失败');
  } finally {
    paymentInit.loading = false;
  }
};

const handlePaymentConfirm = async () => {
  if (!paymentOps.transactionId) {
    message.warning('请填写支付流水 ID');
    return;
  }
  paymentOps.loading = true;
  paymentOps.result = '';
  try {
    const response = await confirmPayment(paymentOps.transactionId, {
      channelTransactionNo: paymentOps.channelTransactionNo || undefined,
      paidAt: paymentOps.paidAt || undefined
    });
    paymentOps.result = jsonSafeStringify(response);
    message.success('支付状态已更新');
  } catch (error: any) {
    console.error('确认支付失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '确认支付失败');
  } finally {
    paymentOps.loading = false;
  }
};

const handlePaymentLoad = async () => {
  if (!paymentOps.transactionId) {
    message.warning('请填写支付流水 ID');
    return;
  }
  paymentOps.loading = true;
  paymentOps.result = '';
  try {
    const response = await fetchPayment(paymentOps.transactionId);
    paymentOps.result = jsonSafeStringify(response);
  } catch (error: any) {
    console.error('查询支付失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '查询支付失败');
  } finally {
    paymentOps.loading = false;
  }
};

const handlePaymentRefund = async () => {
  if (!paymentOps.transactionId) {
    message.warning('请填写支付流水 ID');
    return;
  }
  if (!paymentOps.refundAmount || paymentOps.refundAmount <= 0) {
    message.warning('请填写大于 0 的退款金额');
    return;
  }
  paymentOps.loading = true;
  paymentOps.result = '';
  try {
    const response = await refundPayment(paymentOps.transactionId, {
      amount: paymentOps.refundAmount,
      reason: '运营工具箱退款'
    });
    paymentOps.result = jsonSafeStringify(response);
    message.success('退款请求已提交');
  } catch (error: any) {
    console.error('退款失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '退款失败');
  } finally {
    paymentOps.loading = false;
  }
};

const handleSettlement = async () => {
  settlement.loading = true;
  settlement.result = '';
  try {
    const response = await listSettlements({
      vendorId: settlement.vendorId || undefined,
      from: settlement.from || undefined,
      to: settlement.to || undefined
    });
    settlement.result = jsonSafeStringify(response);
  } catch (error: any) {
    console.error('查询结算失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '查询结算失败');
  } finally {
    settlement.loading = false;
  }
};

const loadNotificationTemplates = async () => {
  try {
    templates.value = await listNotificationTemplates();
  } catch (error) {
    console.warn('加载通知模板失败', error);
  }
};

const loadNotificationLogs = async () => {
  notificationLogs.loading = true;
  try {
    notificationLogs.data = await listNotificationLogs(notificationLogs.status);
  } catch (error) {
    console.error('加载通知日志失败', error);
    message.error('加载通知日志失败');
  } finally {
    notificationLogs.loading = false;
  }
};

const handleSendNotification = async () => {
  if (!notificationSend.recipient) {
    message.warning('请填写接收人');
    return;
  }
  notificationSend.loading = true;
  notificationSend.result = '';
  try {
    let variables: Record<string, unknown> | undefined;
    if (notificationSend.variablesJson.trim()) {
      variables = JSON.parse(notificationSend.variablesJson);
    }
    const response = await sendNotification({
      templateCode: notificationSend.templateCode || undefined,
      channel: notificationSend.channel,
      recipient: notificationSend.recipient,
      subject: notificationSend.subject || undefined,
      content: notificationSend.content || undefined,
      variables
    });
    notificationSend.result = jsonSafeStringify(response);
    message.success('通知已发送');
    loadNotificationLogs();
  } catch (error: any) {
    console.error('发送通知失败', error);
    message.error(error?.response?.data?.message ?? error?.message ?? '发送通知失败');
  } finally {
    notificationSend.loading = false;
  }
};

const notificationStatusColor = (status: NotificationStatus) => {
  switch (status) {
    case 'SENT':
      return 'green';
    case 'FAILED':
      return 'red';
    default:
      return 'blue';
  }
};

loadNotificationTemplates();
loadNotificationLogs();
</script>

<style scoped>
.mt-16 {
  margin-top: 16px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>
