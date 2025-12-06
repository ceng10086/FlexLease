<template>
  <PageSection title="事件时间线" description="包含支付、物流、聊天、纠纷等所有节点。">
    <TimelineList :events="order?.events ?? []" />
  </PageSection>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import PageSection from '../../../components/layout/PageSection.vue';
import TimelineList from '../../../components/timeline/TimelineList.vue';
import { useOrderDetail } from '../../../composables/useOrderDetail';

const { order: getOrder } = useOrderDetail();
const order = computed(() => getOrder());
</script>
