<template>
  <a-drawer
    :open="open"
    :width="drawerWidth"
    :height="drawerHeight"
    :placement="drawerPlacement"
    title="订单详情"
    destroy-on-close
    @close="emit('close')"
  >
    <a-spin :spinning="loading">
      <template v-if="order">
        <div class="drawer-grid">
          <div class="main-column">
            <PageSection title="订单概览">
              <div class="order-meta">
                <div>
                  <p>订单号</p>
                  <strong>{{ order.orderNo }}</strong>
                </div>
                <div>
                  <p>状态</p>
                  <a-tag>{{ order.status }}</a-tag>
                </div>
                <div>
                  <p>押金</p>
                  <strong>¥{{ formatCurrency(order.depositAmount) }}</strong>
                </div>
                <div>
                  <p>租金</p>
                  <strong>¥{{ formatCurrency(order.rentAmount) }}</strong>
                </div>
              </div>
            </PageSection>

            <PageSection v-if="canForceClose" title="平台管理员操作">
              <a-textarea v-model:value="forceCloseForm.reason" :rows="3" placeholder="强制关闭原因" />
              <div class="section-actions">
                <a-button type="primary" danger :loading="forceCloseForm.loading" @click="handleForceClose">
                  强制关闭订单
                </a-button>
              </div>
            </PageSection>

            <PageSection title="纠纷仲裁">
                <div v-if="order.disputes?.length" class="dispute-list">
                  <div v-for="item in order.disputes" :key="item.id" class="dispute-card">
                    <div class="dispute-card__header">
                      <div>
                        <strong>{{ disputeStatusLabel(item.status) }}</strong>
                        <p>发起人：{{ disputeActorLabel(item.initiatorRole) }}</p>
                      </div>
                      <a-tag :color="disputeStatusColor(item.status)">{{ item.status }}</a-tag>
                    </div>
                    <p>诉求：{{ disputeOptionLabel(item.initiatorOption) }} · {{ item.initiatorReason }}</p>
                    <div class="dispute-actions">
                      <a-button
                        :data-testid="`dispute-ai-suggest-${item.id}`"
                        :loading="aiLoading[item.id]"
                        :disabled="!canGenerateAiSuggestion"
                        @click="handleGenerateAiSuggestion(item)"
                      >
                        生成仲裁建议
                      </a-button>
                      <a-select
                        v-model:value="disputeForms[item.id].decision"
                        :options="disputeOptions"
                        style="width: 200px"
                        :disabled="!canResolveDispute(item)"
                    />
                    <a-checkbox v-model:checked="disputeForms[item.id].malicious" :disabled="!canResolveDispute(item)">
                      判定恶意行为（冻结 30 天）
                    </a-checkbox>
                    <a-input-number
                      v-model:value="disputeForms[item.id].penalize"
                      :min="-30"
                      :max="30"
                      placeholder="扣分(正)/加分(负)"
                      :disabled="!canResolveDispute(item) || disputeForms[item.id].malicious"
                    />
                    <a-input v-model:value="disputeForms[item.id].remark" placeholder="备注" :disabled="!canResolveDispute(item)" />
                    <a-button
                      type="primary"
                      :loading="disputeForms[item.id].loading"
                      :disabled="!canResolveDispute(item)"
                        @click="handleResolveDispute(item)"
                      >
                        裁决
                      </a-button>
                    </div>
                    <a-alert
                      v-if="!canGenerateAiSuggestion && !canResolveDispute(item)"
                      type="warning"
                      show-icon
                      class="dispute-ai-card"
                      message="当前账号无仲裁权限"
                      description="请使用“仲裁管理人员”账号处理仲裁建议/裁决；申诉复核案件也可由“复核组”处理。"
                    />
                    <a-alert
                      v-if="aiSuggestions[item.id]"
                      class="dispute-ai-card"
                      type="info"
                      :message="`AI 仲裁建议（${aiSuggestions[item.id]?.model ?? 'LLM'}）`"
                      show-icon
                      :data-testid="`dispute-ai-suggest-result-${item.id}`"
                    >
                      <template #description>
                        <div class="dispute-ai-card__body">
                          <div class="dispute-ai-card__section">
                            <strong>事实摘要</strong>
                            <div class="text-muted">{{ aiSuggestions[item.id]?.summary }}</div>
                          </div>
                          <div v-if="aiSuggestions[item.id]?.missingEvidence?.length" class="dispute-ai-card__section">
                            <strong>缺失证据</strong>
                            <ul class="dispute-ai-card__list">
                              <li v-for="(row, index) in aiSuggestions[item.id]?.missingEvidence" :key="index">
                                <span class="tag-pill">{{ row.who }}</span>
                                {{ row.need }}（{{ row.why }}）
                              </li>
                            </ul>
                          </div>
                          <div v-if="aiSuggestions[item.id]?.recommendedDecision" class="dispute-ai-card__section">
                            <strong>建议裁决</strong>
                            <div class="text-muted">
                              {{ disputeOptionLabel(aiSuggestions[item.id]!.recommendedDecision!.option) }}
                              <span v-if="aiSuggestions[item.id]!.recommendedDecision!.creditDelta !== null && aiSuggestions[item.id]!.recommendedDecision!.creditDelta !== undefined">
                                · 信用 {{ aiSuggestions[item.id]!.recommendedDecision!.creditDelta! > 0 ? '扣分' : '加分' }}
                                {{ Math.abs(aiSuggestions[item.id]!.recommendedDecision!.creditDelta!) }}
                              </span>
                              <span v-if="aiSuggestions[item.id]!.recommendedDecision!.maliciousBehavior"> · 恶意行为</span>
                            </div>
                            <div v-if="aiSuggestions[item.id]!.recommendedDecision!.rationale" class="text-muted">
                              {{ aiSuggestions[item.id]!.recommendedDecision!.rationale }}
                            </div>
                          </div>
                        </div>
                      </template>
                    </a-alert>
                  </div>
                </div>
                <p v-else class="text-muted">暂无纠纷。</p>
              </PageSection>

            <PageSection title="时间线">
              <TimelineList :events="order.events" />
            </PageSection>
          </div>
          <div class="side-column">
            <PageSection title="聊天">
              <OrderChatPanel
                :messages="chatEvents"
                :sending="chatSending"
                :self-role="chatSelfRole"
                :quick-phrases="adminQuickPhrases"
                @send="handleSendMessage"
              />
            </PageSection>
            <PageSection title="凭证">
              <ProofGallery :proofs="order.proofs" @preview="previewProof" />
            </PageSection>
          </div>
        </div>
      </template>
      <DataStateBlock v-else-if="!loading" title="请选择订单" description="在列表中点击订单查看详情" />
    </a-spin>
  </a-drawer>
</template>

<script lang="ts" setup>
// 管理端订单详情抽屉：在列表页内快速查看订单明细、时间线与干预操作。
import { computed, reactive, ref, watch } from 'vue';
import PageSection from '../../../components/layout/PageSection.vue';
import DataStateBlock from '../../../components/feedback/DataStateBlock.vue';
import OrderChatPanel from '../../../components/chat/OrderChatPanel.vue';
import ProofGallery from '../../../components/proof/ProofGallery.vue';
import TimelineList from '../../../components/timeline/TimelineList.vue';
import {
  fetchOrder,
  postOrderMessage,
  forceCloseOrder,
  resolveOrderDispute,
  generateDisputeAiSuggestion,
  uploadOrderProof,
  type RentalOrderDetail,
  type OrderDispute,
  type DisputeAiSuggestion,
  type DisputeResolutionOption
} from '../../../services/orderService';
import { friendlyErrorMessage } from '../../../utils/error';
import { openProofInNewTab } from '../../../services/proofService';
import { message } from 'ant-design-vue';
import { formatCurrency } from '../../../utils/number';
import { disputeOptions, disputeOptionLabel, disputeStatusColor, disputeStatusLabel, disputeActorLabel } from '../../../utils/disputes';
import { useAuthStore } from '../../../stores/auth';
import type { ChatSendPayload } from '../../../types/chat';
import { useViewport } from '../../../composables/useViewport';

const props = defineProps<{
  open: boolean;
  orderId: string | null;
}>();

const emit = defineEmits<{
  (e: 'close'): void;
  (e: 'refresh'): void;
}>();

const auth = useAuthStore();
const { width: viewportWidth, isMobile } = useViewport();
const order = ref<RentalOrderDetail | null>(null);
const loading = ref(false);
const forceCloseForm = reactive({ reason: '', loading: false });
const disputeForms = reactive<Record<string, { decision: DisputeResolutionOption; remark: string; penalize: number | null; malicious: boolean; loading: boolean }>>({});
const aiSuggestions = reactive<Record<string, DisputeAiSuggestion | null>>({});
const aiLoading = reactive<Record<string, boolean>>({});
const chatSending = ref(false);
const adminQuickPhrases = [
  '平台已介入处理，请保持与对方沟通。',
  '已收到凭证，我们正在核查，请耐心等待。',
  '如需补充资料，请在凭证区上传以便复核。'
];

const canForceClose = computed(() => auth.hasRole('ADMIN'));
const canGenerateAiSuggestion = computed(() => auth.hasRole('ARBITRATOR'));
const chatSelfRole = computed(() => {
  if (auth.hasRole('ARBITRATOR')) {
    return 'ARBITRATOR';
  }
  if (auth.hasRole('REVIEW_PANEL')) {
    return 'REVIEW_PANEL';
  }
  return 'ADMIN';
});

const canResolveDispute = (dispute: OrderDispute) => {
  if (auth.hasRole('ARBITRATOR')) {
    return dispute.status === 'PENDING_ADMIN' || dispute.status === 'PENDING_REVIEW_PANEL';
  }
  if (auth.hasRole('REVIEW_PANEL')) {
    return dispute.status === 'PENDING_REVIEW_PANEL';
  }
  return false;
};

const drawerWidth = computed(() => {
  if (isMobile.value) {
    return '100%';
  }
  const margin = 64;
  const desired = Math.min(1100, viewportWidth.value - margin);
  return Math.max(720, desired);
});
const drawerPlacement = computed(() => (isMobile.value ? 'bottom' : 'right'));
const drawerHeight = computed(() => (isMobile.value ? '100%' : undefined));

const loadOrder = async () => {
  if (!props.orderId) {
    order.value = null;
    return;
  }
  loading.value = true;
  try {
    order.value = await fetchOrder(props.orderId);
    order.value?.disputes?.forEach((item) => {
      if (!disputeForms[item.id]) {
        disputeForms[item.id] = {
          decision: item.adminDecisionOption ?? item.initiatorOption ?? 'REDELIVER',
          remark: '',
          penalize: 0,
          malicious: false,
          loading: false
        };
      }
      if (aiSuggestions[item.id] === undefined) {
        aiSuggestions[item.id] = null;
      }
      if (aiLoading[item.id] === undefined) {
        aiLoading[item.id] = false;
      }
    });
  } catch (error) {
    message.error(friendlyErrorMessage(error, '加载订单失败'));
    order.value = null;
  } finally {
    loading.value = false;
  }
};

watch(
  () => props.open,
  (val) => {
    if (val) {
      loadOrder();
    } else {
      order.value = null;
    }
  }
);

watch(
  () => props.orderId,
  (id, prev) => {
    if (props.open && id && id !== prev) {
      loadOrder();
    }
  }
);

const chatEvents = computed(() => {
  const events = order.value?.events ?? [];
  return events.filter((event) => event.eventType?.includes('MESSAGE') || event.eventType === 'COMMUNICATION_NOTE');
});

const handleSendMessage = async (payload: ChatSendPayload) => {
  if (!order.value || !auth.user) {
    return;
  }
  const trimmed = payload.content.trim();
  const hasAttachments = payload.attachments.length > 0;
  if (!trimmed && !hasAttachments) {
    return;
  }
  chatSending.value = true;
  try {
    let attachmentSummary = '';
    if (hasAttachments) {
      const uploads = await Promise.all(
        payload.attachments.map((file) =>
          uploadOrderProof(order.value!.id, {
            actorId: auth.user!.id,
            proofType: 'OTHER',
            description: trimmed || file.name,
            file
          })
        )
      );
      const lines = uploads.map((proof, index) => `• ${payload.attachments[index].name}: ${proof.fileUrl}`);
      attachmentSummary = [`已附加 ${uploads.length} 个文件`, ...lines].join('\n');
    }
    const finalMessage = [trimmed, attachmentSummary].filter((segment) => segment && segment.trim()).join('\n\n');
    if (!finalMessage) {
      chatSending.value = false;
      return;
    }
    order.value = await postOrderMessage(order.value.id, {
      actorId: auth.user.id,
      message: finalMessage
    });
    message.success('已发送');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '发送失败'));
  } finally {
    chatSending.value = false;
  }
};

const handleForceClose = async () => {
  if (!order.value) {
    return;
  }
  if (!canForceClose.value) {
    message.warning('当前账号无强制关闭权限');
    return;
  }
  forceCloseForm.loading = true;
  try {
    order.value = await forceCloseOrder(order.value.id, { reason: forceCloseForm.reason || undefined });
    message.success('已强制关闭订单');
    emit('refresh');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '操作失败'));
  } finally {
    forceCloseForm.loading = false;
  }
};

const handleResolveDispute = async (dispute: OrderDispute) => {
  if (!order.value) {
    return;
  }
  if (!canResolveDispute(dispute)) {
    message.warning('当前账号无裁决权限');
    return;
  }
  const form = disputeForms[dispute.id];
  form.loading = true;
  try {
    await resolveOrderDispute(order.value.id, dispute.id, {
      decision: form.decision,
      penalizeUserDelta: form.malicious ? undefined : form.penalize ?? undefined,
      remark: form.remark || undefined,
      maliciousBehavior: form.malicious ? true : undefined
    });
    message.success('已提交裁决');
    loadOrder();
    emit('refresh');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '裁决失败'));
  } finally {
    form.loading = false;
  }
};

const handleGenerateAiSuggestion = async (dispute: OrderDispute) => {
  if (!order.value) {
    return;
  }
  if (!canGenerateAiSuggestion.value) {
    message.warning('当前账号无生成仲裁建议权限');
    return;
  }
  aiLoading[dispute.id] = true;
  try {
    const suggestion = await generateDisputeAiSuggestion(order.value.id, dispute.id, { tone: 'NEUTRAL' });
    aiSuggestions[dispute.id] = suggestion;
    const decision = suggestion.recommendedDecision;
    const form = disputeForms[dispute.id];
    if (decision?.option) {
      form.decision = decision.option;
    }
    if (decision?.maliciousBehavior) {
      form.malicious = true;
      form.penalize = 0;
    } else if (decision?.creditDelta !== null && decision?.creditDelta !== undefined) {
      form.penalize = decision.creditDelta;
    }
    if (decision?.rationale) {
      form.remark = decision.rationale;
    }
    message.success('已生成仲裁建议');
  } catch (error) {
    message.error(friendlyErrorMessage(error, '生成失败'));
  } finally {
    aiLoading[dispute.id] = false;
  }
};

const previewProof = async (proof: { fileUrl: string }) => {
  try {
    await openProofInNewTab(proof.fileUrl);
  } catch (error) {
    message.error(friendlyErrorMessage(error, '无法打开预览'));
  }
};
</script>

<style scoped>
.drawer-grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: var(--space-4);
}

.order-meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: var(--space-3);
}

.order-meta p {
  margin: 0;
  color: var(--color-text-secondary);
}

.section-actions {
  margin-top: var(--space-3);
  text-align: right;
}

.dispute-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.dispute-card {
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: var(--radius-card);
  padding: var(--space-3);
}

.dispute-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dispute-card__header p {
  margin: 0;
}

.dispute-actions {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
  margin-top: var(--space-2);
}

.dispute-ai-card {
  margin-top: var(--space-3);
}

.dispute-ai-card__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.dispute-ai-card__section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.dispute-ai-card__list {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text-secondary);
}

.tag-pill {
  display: inline-block;
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(59, 130, 246, 0.12);
  color: rgba(59, 130, 246, 0.9);
  margin-right: 6px;
  font-size: 12px;
}

.side-column {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

@media (max-width: 1024px) {
  .drawer-grid {
    grid-template-columns: 1fr;
  }
}
</style>
