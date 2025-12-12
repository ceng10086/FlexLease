<template>
  <div class="review-panel">
    <div v-if="loading" class="review-panel__loading">
      <a-skeleton active :paragraph="{ rows: 4 }" />
    </div>
    <DataStateBlock
      v-else-if="error"
      type="error"
      title="评价加载失败"
      :description="error"
    >
      <template #action>
        <a-button type="link" @click="$emit('refresh')">重试</a-button>
      </template>
    </DataStateBlock>
    <DataStateBlock
      v-else-if="!summary || summary.totalReviews === 0"
      type="empty"
      title="还没有评价"
      description="完成租赁后欢迎在满意度调查中留下建议。"
    />
    <div v-else class="review-panel__body">
      <div class="review-panel__summary">
        <div class="review-panel__score">
          <p>综合评分</p>
          <strong>{{ summary.averageScore.toFixed(1) }}</strong>
          <a-rate :value="summary.averageScore" allow-half disabled />
          <span class="review-panel__count">{{ summary.totalReviews }} 条评价</span>
          <span v-if="summary.responseRate" class="review-panel__response">
            回复率 {{ Math.round(summary.responseRate * 100) }}%
          </span>
        </div>
        <div class="review-panel__distribution">
          <div v-for="star in starScale" :key="star" class="distribution-row">
            <span>{{ star }}★</span>
            <a-progress :percent="distributionPercent(star)" :show-info="false" />
            <span class="distribution-row__count">{{ summary.distribution[star] ?? 0 }}</span>
          </div>
        </div>
      </div>
      <div class="review-panel__list">
        <article v-for="review in reviews" :key="review.id" class="review-card">
          <header>
            <div>
              <strong>{{ review.author }}</strong>
              <span>{{ formatDate(review.createdAt) }}</span>
            </div>
            <div class="review-card__rating">
              <a-rate :value="review.rating" allow-half disabled />
              <span>{{ review.rating.toFixed(1) }}</span>
            </div>
          </header>
          <p class="review-card__content">{{ review.content }}</p>
          <ul v-if="review.tags?.length" class="review-card__tags">
            <li v-for="tag in review.tags" :key="tag">{{ tag }}</li>
          </ul>
        </article>
      </div>
      <div class="review-panel__actions">
        <a-button type="link" @click="$emit('refresh')">刷新评价</a-button>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';
import DataStateBlock from '../feedback/DataStateBlock.vue';
import type { ReviewItem, ReviewSummary } from '../../types/review';

const props = defineProps<{
  summary: ReviewSummary | null;
  reviews: ReviewItem[];
  loading: boolean;
  error: string | null;
}>();

defineEmits<{
  (e: 'refresh'): void;
}>();

const starScale = [5, 4, 3, 2, 1];

const distributionPercent = (star: number) => {
  if (!props.summary || props.summary.totalReviews === 0) {
    return 0;
  }
  const count = props.summary.distribution[star] ?? 0;
  return Number(((count / props.summary.totalReviews) * 100).toFixed(1));
};

const formatDate = (value: string) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'short',
    day: 'numeric'
  }).format(date);
};
</script>

<style scoped>
.review-panel {
  border: 1px solid var(--color-border);
  border-radius: 16px;
  background: var(--color-surface);
  padding: var(--space-4);
}

.review-panel__loading {
  padding: var(--space-2);
}

.review-panel__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.review-panel__summary {
  display: grid;
  grid-template-columns: minmax(160px, 1fr) minmax(220px, 1fr);
  gap: var(--space-4);
}

.review-panel__score p {
  margin: 0;
  color: var(--color-text-secondary);
}

.review-panel__score strong {
  display: block;
  font-size: 40px;
  line-height: 1;
  color: var(--color-primary);
  margin-bottom: 4px;
}

.review-panel__count {
  display: block;
  margin-top: 4px;
  color: var(--color-text-secondary);
}

.review-panel__response {
  margin-top: 4px;
  font-size: 12px;
  color: var(--color-success);
}

.review-panel__distribution {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.distribution-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.distribution-row__count {
  color: var(--color-text-secondary);
  min-width: 24px;
  text-align: right;
}

.review-panel__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.review-card {
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: var(--space-3);
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.08);
  background: #fff;
}

.review-card header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.review-card header strong {
  display: block;
}

.review-card header span {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.review-card__rating {
  display: flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.review-card__content {
  margin: 0 0 8px;
  color: var(--color-text);
  line-height: 1.6;
}

.review-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0;
  margin: 0;
  list-style: none;
}

.review-card__tags li {
  background: var(--color-surface-muted);
  border-radius: 999px;
  padding: 2px 10px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.review-panel__actions {
  text-align: right;
}

@media (max-width: 640px) {
  .review-panel__summary {
    grid-template-columns: 1fr;
  }
}
</style>
