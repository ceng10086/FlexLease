<template>
  <a-form layout="vertical" @submit.prevent="handleSubmit">
    <a-form-item label="称呼">
      <a-input v-model:value="form.contactName" placeholder="如何称呼您" />
    </a-form-item>
    <a-form-item label="联系方式">
      <a-input v-model:value="form.contactMethod" placeholder="手机号 / 邮箱" />
    </a-form-item>
    <a-form-item label="咨询内容">
      <a-textarea
        v-model:value="form.message"
        :rows="4"
        placeholder="填写租赁需求、交付时间或更多问题，厂商将在 72 小时内回复"
      />
    </a-form-item>
    <a-space style="width: 100%" direction="vertical">
      <a-button type="primary" block :loading="submitting" :disabled="!form.message" @click="handleSubmit">
        发送咨询
      </a-button>
      <p class="inquiry-hint">咨询有效期 72 小时，回复将同步到通知中心及聊天面板。</p>
    </a-space>
  </a-form>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useRoute, useRouter } from 'vue-router';
import { submitProductInquiry, type ProductInquiryPayload } from '../../services/catalogService';
import { friendlyErrorMessage } from '../../utils/error';
import { useAuthStore } from '../../stores/auth';

const props = defineProps<{
  productId: string;
}>();

const router = useRouter();
const route = useRoute();
const auth = useAuthStore();

const form = reactive<ProductInquiryPayload>({
  contactName: '',
  contactMethod: '',
  message: ''
});

const submitting = ref(false);

const ensureAuthenticated = () => {
  if (auth.isAuthenticated) {
    return true;
  }
  message.info('登录后才能发送咨询');
  router.push({ name: 'login', query: { redirect: route.fullPath } });
  return false;
};

const handleSubmit = async () => {
  if (!form.message) {
    message.warning('请填写咨询内容');
    return;
  }
  if (!ensureAuthenticated()) {
    return;
  }
  submitting.value = true;
  try {
    await submitProductInquiry(props.productId, form);
    message.success('咨询已发送，稍后留意通知');
    form.message = '';
  } catch (error) {
    message.error(friendlyErrorMessage(error, '提交咨询失败，请稍后重试'));
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.inquiry-hint {
  margin: 0;
  font-size: var(--font-size-caption);
  color: var(--color-text-secondary);
  text-align: center;
}
</style>
