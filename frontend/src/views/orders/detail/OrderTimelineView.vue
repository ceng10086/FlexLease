<template>
  <div v-if="order" class="timeline-stack">
    <PageSection title="事件时间线" description="包含支付、物流、聊天、纠纷等所有节点。">
      <TimelineList :events="order.events" />
    </PageSection>

    <PageSection title="纠纷与仲裁" description="查看当前纠纷进度，支持发起、回应或升级。">
      <OrderDisputePanel
        :order="order"
        :current-user-id="currentUserId"
        @updated="refresh"
      />
    </PageSection>

    <PageSection title="满意度调查" description="完成调研可同步信用记录。">
      <OrderSurveyPanel
        :order="order"
        :current-user-id="currentUserId"
        @updated="refresh"
      />
    </PageSection>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import PageSection from '../../../components/layout/PageSection.vue';
import TimelineList from '../../../components/timeline/TimelineList.vue';
import OrderDisputePanel from '../../../components/orders/OrderDisputePanel.vue';
import OrderSurveyPanel from '../../../components/orders/OrderSurveyPanel.vue';
import { useOrderDetail } from '../../../composables/useOrderDetail';
import { useAuthStore } from '../../../stores/auth';

const { order: getOrder, refresh } = useOrderDetail();
const auth = useAuthStore();
const order = computed(() => getOrder());
const currentUserId = computed(() => auth.user?.id ?? null);
</script>

<style scoped>
.timeline-stack {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}
</style>
