<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>我的订单</h2>
        <p class="page-header__meta">查看租赁订单状态，进行支付、续租、退租等操作。</p>
      </div>
      <a-space>
        <a-select
          v-model:value="filters.status"
          allow-clear
          placeholder="全部状态"
          style="width: 200px"
          @change="loadOrders"
        >
          <a-select-option v-for="status in orderStatuses" :key="status" :value="status">
            {{ statusMeta(status).label }}
          </a-select-option>
        </a-select>
        <a-button type="primary" @click="loadOrders" :loading="loading">刷新</a-button>
      </a-space>
    </div>

    <div class="page-guidance">
      <div class="page-guidance__title">温馨提示</div>
      <div class="page-guidance__content">
        - 待支付订单超时将自动取消，可重新创建；<br>
        - 在租中的订单可直接在详情页发起续租、退租与买断；<br>
        - 若物流收货有误，可在确认收货前联系厂商客服修改。
      </div>
    </div>

    <a-card>
      <a-table
        :data-source="orders"
        :loading="loading"
        row-key="id"
        :pagination="pagination"
        :locale="{ emptyText: '暂无订单，请前往目录下单' }"
        @change="handleTableChange"
      >
        <a-table-column title="订单号" data-index="orderNo" key="orderNo" />
        <a-table-column title="状态" key="status">
          <template #default="{ record }">
            <a-tag :color="statusMeta(record.status).color">
              {{ statusMeta(record.status).label }}
            </a-tag>
          </template>
        </a-table-column>
        <a-table-column title="金额" key="totalAmount">
          <template #default="{ record }">¥{{ formatCurrency(record.totalAmount) }}</template>
        </a-table-column>
        <a-table-column title="创建时间" key="createdAt">
          <template #default="{ record }">{{ formatDate(record.createdAt) }}</template>
        </a-table-column>
        <a-table-column title="操作" key="actions">
          <template #default="{ record }">
            <a-button size="small" type="link" @click="goDetail(record.id)">查看详情</a-button>
          </template>
        </a-table-column>
      </a-table>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../../stores/auth';
import { listOrders, type OrderStatus, type RentalOrderSummary } from '../../services/orderService';
import { friendlyErrorMessage } from '../../utils/error';

const router = useRouter();
const auth = useAuthStore();

const orderStatuses: OrderStatus[] = [
  'PENDING_PAYMENT',
  'AWAITING_SHIPMENT',
  'IN_LEASE',
  'RETURN_REQUESTED',
  'RETURN_IN_PROGRESS',
  'COMPLETED',
  'BUYOUT_REQUESTED',
  'BUYOUT_COMPLETED',
  'CANCELLED'
];

const loading = ref(false);
const orders = ref<RentalOrderSummary[]>([]);
const filters = reactive<{ status?: OrderStatus }>({});
const pagination = reactive({ current: 1, pageSize: 10, total: 0 });

const statusDict: Record<OrderStatus, { label: string; color: string }> = {
  PENDING_PAYMENT: { label: '待支付', color: 'orange' },
  AWAITING_SHIPMENT: { label: '待发货', color: 'blue' },
  IN_LEASE: { label: '在租中', color: 'green' },
  RETURN_REQUESTED: { label: '退租处理中', color: 'gold' },
  RETURN_IN_PROGRESS: { label: '退租中', color: 'purple' },
  COMPLETED: { label: '已完成', color: 'default' },
  BUYOUT_REQUESTED: { label: '买断审批', color: 'cyan' },
  BUYOUT_COMPLETED: { label: '已买断', color: 'success' },
  CANCELLED: { label: '已取消', color: 'red' }
};

const statusMeta = (status: OrderStatus) => statusDict[status] ?? { label: status, color: 'blue' };

const formatCurrency = (value: number) => value.toFixed(2);
const formatDate = (value: string) => new Date(value).toLocaleString();

const loadOrders = async () => {
  if (!auth.user?.id) {
    message.error('请先登录');
    router.replace({ name: 'login', query: { redirect: '/app/orders' } });
    return;
  }
  loading.value = true;
  try {
    const result = await listOrders({
      userId: auth.user.id,
      status: filters.status,
      page: pagination.current,
      size: pagination.pageSize
    });
    orders.value = result.content;
    pagination.total = result.totalElements;
  } catch (error) {
    console.error('加载订单失败', error);
    message.error(friendlyErrorMessage(error, '加载订单失败，请稍后重试'));
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (pager: { current?: number; pageSize?: number }) => {
  pagination.current = pager.current ?? pagination.current;
  pagination.pageSize = pager.pageSize ?? pagination.pageSize;
  loadOrders();
};

const goDetail = (orderId: string) => {
  router.push({ name: 'order-detail', params: { orderId } });
};

loadOrders();
</script>
