<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="聊天面板"
        eyebrow="Chat"
        description="集中查看需要沟通的订单，快速进入聊天子页。"
      />
    </template>
    <PageSection>
      <a-list :data-source="orders" :loading="loading" bordered>
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta
              :title="`订单号：${item.orderNo}`"
              :description="`状态：${orderStatusLabel(item.status)}`"
            />
            <a-button type="link" @click="openChat(item.id)">打开聊天</a-button>
          </a-list-item>
        </template>
      </a-list>
    </PageSection>
  </PageShell>
</template>

<script lang="ts" setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import PageShell from '../../components/layout/PageShell.vue';
import PageHeader from '../../components/layout/PageHeader.vue';
import PageSection from '../../components/layout/PageSection.vue';
import { useAuthStore } from '../../stores/auth';
import { listOrders, type RentalOrderSummary } from '../../services/orderService';
import { orderStatusLabel } from '../../utils/orderStatus';
import { message } from 'ant-design-vue';
import { friendlyErrorMessage } from '../../utils/error';

const auth = useAuthStore();
const router = useRouter();
const loading = ref(false);
const orders = ref<RentalOrderSummary[]>([]);

const fetchOrders = async () => {
  if (!auth.user) {
    return;
  }
  loading.value = true;
  try {
    const response = await listOrders({
      userId: auth.user.id,
      page: 1,
      size: 20
    });
    orders.value = response.content;
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载订单失败'));
  } finally {
    loading.value = false;
  }
};

const openChat = (orderId: string) => {
  router.push({ name: 'order-chat', params: { orderId } });
};

fetchOrders();
</script>
