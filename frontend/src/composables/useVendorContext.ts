import { computed, ref } from 'vue';
import { message } from 'ant-design-vue';
import { useAuthStore } from '../stores/auth';

type RefreshOptions = {
  notify?: boolean;
};

export const useVendorContext = () => {
  const auth = useAuthStore();
  const syncingVendor = ref(false);

  const vendorId = computed(() => auth.vendorId ?? null);
  const vendorReady = computed(() => Boolean(vendorId.value));

  const requireVendorId = (notify = false): string | null => {
    if (vendorId.value) {
      return vendorId.value;
    }
    if (notify) {
      message.warning('未获取到厂商身份，请退出并重新登录后再试');
    }
    return null;
  };

  const refreshVendorContext = async (options: RefreshOptions = {}) => {
    if (syncingVendor.value) {
      return vendorId.value;
    }
    const { notify = true } = options;
    syncingVendor.value = true;
    try {
      await auth.bootstrap();
      if (notify) {
        if (vendorId.value) {
          message.success('账户信息已刷新（如依旧缺少厂商身份，请重新登录）');
        } else {
          message.warning('仍未获取到厂商身份，请退出并重新登录后再试');
        }
      }
      return vendorId.value;
    } catch (error) {
      if (notify) {
        message.error('同步厂商身份失败，请稍后重试');
      }
      throw error;
    } finally {
      syncingVendor.value = false;
    }
  };

  return {
    vendorId,
    vendorReady,
    requireVendorId,
    refreshVendorContext,
    syncingVendor
  };
};

export type UseVendorContextReturn = ReturnType<typeof useVendorContext>;
