<template>
  <PageShell>
    <template #header>
      <PageHeader
        title="聊天面板"
        eyebrow="Chat"
        description="集中查看需要沟通的订单，快速进入聊天子页。"
      >
        <template #actions>
          <a-button size="small" :loading="loading" @click="refresh">
            刷新
          </a-button>
        </template>
      </PageHeader>
    </template>
    <PageSection>
      <div class="conversation-summary">
        <span>共 {{ pagination.total }} 单</span>
        <span>最近刷新：{{ lastUpdatedLabel }}</span>
      </div>
      <a-list :data-source="orders" :loading="loading" bordered :locale="{ emptyText: '暂无订单' }" :pagination="false">
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
      <div class="conversation-pagination" v-if="pagination.total > pagination.size">
        <a-pagination
          :current="pagination.page"
          :total="pagination.total"
          :page-size="pagination.size"
          :show-size-changer="true"
          :page-size-options="['10', '20', '30', '50']"
          @change="handlePageChange"
          @showSizeChange="handlePageSizeChange"
        />
      </div>
    </PageSection>
  </PageShell>
</template>

<script lang="ts" setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue';
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
const pagination = ref({ page: 1, size: 20, total: 0 });
const lastUpdated = ref<number | null>(null);
const refreshInterval = 30_000;
let intervalId: ReturnType<typeof setInterval> | null = null;

const lastUpdatedLabel = computed(() => {
  if (!lastUpdated.value) {
    return '尚未刷新';
  }
  return new Date(lastUpdated.value).toLocaleTimeString();
});

const loadOrders = async (page = pagination.value.page, size = pagination.value.size) => {
  if (!auth.user) {
    orders.value = [];
    pagination.value = { page: 1, size, total: 0 };
    return;
  }
  loading.value = true;
  try {
    const response = await listOrders({
      userId: auth.user.id,
      page,
      size
    });
    orders.value = response.content;
    pagination.value = {
      page: response.page ?? page,
      size: response.size ?? size,
      total: response.totalElements ?? response.content.length
    };
    lastUpdated.value = Date.now();
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载订单失败'));
  } finally {
    loading.value = false;
  }
};

const refresh = () => {
  pagination.value.page = 1;
  loadOrders(1, pagination.value.size);
};

const handlePageChange = (page: number) => {
  pagination.value.page = page;
  loadOrders(page, pagination.value.size);
};

const handlePageSizeChange = (_current: number, size: number) => {
  pagination.value = { ...pagination.value, size, page: 1 };
  loadOrders(1, size);
};

const startPolling = () => {
  stopPolling();
  intervalId = setInterval(() => {
    loadOrders(pagination.value.page, pagination.value.size);
  }, refreshInterval);
};

const stopPolling = () => {
  if (intervalId) {
    clearInterval(intervalId);
    intervalId = null;
  }
};

const openChat = (orderId: string) => {
  router.push({ name: 'order-chat', params: { orderId } });
};

watch(
  () => auth.user?.id,
  (userId) => {
    if (userId) {
      refresh();
      startPolling();
    } else {
      stopPolling();
      orders.value = [];
      pagination.value = { page: 1, size: 20, total: 0 };
      lastUpdated.value = null;
    }
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  stopPolling();
});
</script>

<style scoped>
.conversation-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);
  color: var(--color-text-secondary);
}

.conversation-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-3);
}

@media (max-width: 640px) {
  .conversation-summary {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .conversation-pagination {
    justify-content: center;
  }
}
</style>
