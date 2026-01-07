export type ProductCategoryCode = 'ELECTRONICS' | 'HOME_APPLIANCE' | 'ENTERTAINMENT' | 'OUTDOOR';

export const PRODUCT_CATEGORY_OPTIONS: Array<{ label: string; value: ProductCategoryCode }> = [
  { label: '3C 数码', value: 'ELECTRONICS' },
  { label: '智能家电', value: 'HOME_APPLIANCE' },
  { label: '影音娱乐', value: 'ENTERTAINMENT' },
  { label: '户外出行', value: 'OUTDOOR' }
];

export const ensureCategoryOption = (
  options: Array<{ label: string; value: string }>,
  current?: string | null
) => {
  const code = (current ?? '').trim();
  if (!code) {
    return options;
  }
  if (options.some((item) => item.value === code)) {
    return options;
  }
  return [{ label: `未预置（${code}）`, value: code }, ...options];
};
