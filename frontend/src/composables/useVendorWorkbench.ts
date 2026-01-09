/**
 * 厂商工作台上下文（provide/inject）：
 * - VendorWorkbenchLayout 提供 vendor 档案与 loading/refresh
 * - 具体看板页通过 useVendorWorkbench 读取并复用数据
 */
import { inject, provide, type Ref } from 'vue';
import type { Vendor } from '../services/vendorService';

type VendorWorkbenchContext = {
  vendor: Ref<Vendor | null>;
  vendorLoading: Ref<boolean>;
  refreshVendorProfile: () => Promise<void>;
};

const VENDOR_WORKBENCH_KEY = Symbol('vendor-workbench');

export const provideVendorWorkbench = (context: VendorWorkbenchContext) => {
  provide(VENDOR_WORKBENCH_KEY, context);
};

export const useVendorWorkbench = (): VendorWorkbenchContext => {
  const ctx = inject<VendorWorkbenchContext | null>(VENDOR_WORKBENCH_KEY, null);
  if (!ctx) {
    throw new Error('useVendorWorkbench must be used within VendorWorkbenchLayout');
  }
  return ctx;
};
