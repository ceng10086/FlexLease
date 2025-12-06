<template>
  <div class="survey-panel">
    <div class="panel-header">
      <div>
        <p class="eyebrow">满意度 & 信用复盘</p>
        <h3>反馈履约体验，帮助完善信用档案</h3>
      </div>
    </div>

    <a-alert
      v-if="!currentUserId"
      type="warning"
      message="登录后可提交调查"
      show-icon
    />

    <div v-else>
      <div v-if="pendingSurveys.length" class="survey-list">
        <a-card v-for="survey in pendingSurveys" :key="survey.id" size="small" class="survey-card">
          <template #title>
            <div class="card-title">
              <div>
                <p class="card-subtitle">
                  调查开放时间 · {{ formatDate(survey.availableAt) }}
                </p>
                <strong>满意度调查</strong>
              </div>
              <a-tag color="orange">待提交</a-tag>
            </div>
          </template>
          <div class="card-body">
            <a-rate v-model:value="surveyForms[survey.id].rating" allow-clear style="margin-bottom: 8px" />
            <a-textarea
              v-model:value="surveyForms[survey.id].comment"
              :rows="3"
              placeholder="想对平台或厂商说的话"
            />
            <div class="card-actions">
              <a-button
                type="primary"
                size="small"
                :loading="submitting === survey.id"
                @click="submitSurvey(survey)">
                提交反馈
              </a-button>
            </div>
          </div>
        </a-card>
      </div>
      <a-empty v-else description="暂无待办调查" />

      <div v-if="completedSurveys.length" class="completed-list">
        <h4>已完成</h4>
        <ul>
          <li v-for="survey in completedSurveys" :key="survey.id">
            <span>{{ formatDate(survey.submittedAt) }}</span>
            <span>评分 {{ survey.rating }}/5</span>
            <span>{{ survey.comment || '——' }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue';
import { message } from 'ant-design-vue';
import type { OrderSurvey, RentalOrderDetail } from '../../services/orderService';
import { submitOrderSurvey } from '../../services/orderService';

const props = defineProps<{
  order: RentalOrderDetail;
  currentUserId?: string | null;
}>();

const emit = defineEmits<{ (e: 'updated'): void }>();

const surveyForms = reactive<Record<string, { rating: number; comment: string }>>({});
const submitting = ref<string | null>(null);

const currentUserId = computed(() => props.currentUserId ?? null);

const pendingSurveys = computed(() =>
  props.order.surveys.filter(
    (survey) =>
      survey.targetRef === currentUserId.value &&
      survey.status !== 'COMPLETED'
  )
);

const completedSurveys = computed(() =>
  props.order.surveys.filter(
    (survey) =>
      survey.targetRef === currentUserId.value &&
      survey.status === 'COMPLETED'
  )
);

watch(
  pendingSurveys,
  (surveys) => {
    surveys.forEach((survey) => {
      if (!surveyForms[survey.id]) {
        surveyForms[survey.id] = { rating: survey.rating ?? 5, comment: survey.comment ?? '' };
      }
    });
  },
  { immediate: true }
);

const formatDate = (value?: string | null) => (value ? new Date(value).toLocaleString() : '--');

const submitSurvey = async (survey: OrderSurvey) => {
  if (!currentUserId.value) {
    message.error('缺少用户信息');
    return;
  }
  const form = surveyForms[survey.id];
  if (!form || !form.rating) {
    message.warning('请先打分');
    return;
  }
  submitting.value = survey.id;
  try {
    await submitOrderSurvey(props.order.id, survey.id, {
      actorId: currentUserId.value,
      rating: form.rating,
      comment: form.comment?.trim() || undefined
    });
    message.success('感谢反馈，我们已记录');
    emit('updated');
  } catch (error) {
    console.error(error);
    message.error('提交失败，请稍后重试');
  } finally {
    submitting.value = null;
  }
};
</script>

<style scoped>
.survey-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: var(--color-text-secondary);
}

.survey-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.survey-card {
  border-radius: 12px;
}

.card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-subtitle {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
}

.completed-list {
  border-top: 1px solid var(--color-border);
  padding-top: 12px;
}

.completed-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.completed-list li {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}
</style>
