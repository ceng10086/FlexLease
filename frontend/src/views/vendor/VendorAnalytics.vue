<template>
  <div class="page-wrapper">
    <a-row :gutter="16">
      <a-col :xs="24" :lg="12">
        <a-card title="关键指标" :bordered="false">
          <a-list :data-source="metrics">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :title="item.label" :description="item.value" />
                <template #actions>
                  <a-tag :color="item.trend >= 0 ? 'green' : 'red'">
                    {{ item.trend >= 0 ? '+' : '' }}{{ item.trend }}%
                  </a-tag>
                </template>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
      <a-col :xs="24" :lg="12">
        <a-card title="履约完成率" :bordered="false">
          <a-progress type="dashboard" :percent="performance" :stroke-color="{ '0%': '#1677ff', '100%': '#52c41a' }" />
          <p class="analytics-desc">近30天履约完成率，根据订单履约状态实时计算。</p>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useAuthStore } from '@/stores/auth';
import { fetchVendorMetrics } from '@/api/vendors';
import type { VendorMetric } from '@/types';
import { sampleVendorMetrics } from '@/utils/sampleData';

const auth = useAuthStore();

const metrics = ref<VendorMetric[]>([]);

onMounted(async () => {
  try {
    metrics.value = await fetchVendorMetrics(auth.profile?.id ?? '');
  } catch (error) {
    console.error(error);
    metrics.value = sampleVendorMetrics;
  }
});

const performance = computed(() => {
  const metric = metrics.value.find((item) => item.label.includes('履约')); 
  if (!metric) return 0;
  const parsed = Number.parseFloat(metric.value.replace(/[^0-9.]/g, ''));
  return Number.isNaN(parsed) ? 0 : parsed;
});
</script>

<style scoped>
.page-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.analytics-desc {
  color: var(--app-muted);
  font-size: 13px;
  margin-top: 16px;
}
</style>
