import api from './api';

export type ProductStatus = 'DRAFT' | 'PENDING_REVIEW' | 'ACTIVE' | 'INACTIVE' | 'REJECTED';

export type ProductSummary = {
  id: string;
  vendorId: string;
  name: string;
  categoryCode: string;
  status: ProductStatus;
  submittedAt?: string | null;
  reviewedAt?: string | null;
  createdAt: string;
};

export type RentalPlanType = 'STANDARD' | 'RENT_TO_OWN' | 'LEASE_TO_SALE';

export type RentalPlanStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE';

export type ProductSkuStatus = 'ACTIVE' | 'INACTIVE';

export type RentalSku = {
  id: string;
  skuCode: string;
  attributes: Record<string, unknown>;
  stockTotal: number;
  stockAvailable: number;
  status: ProductSkuStatus;
  createdAt: string;
  updatedAt: string;
};

export type RentalPlan = {
  id: string;
  planType: RentalPlanType;
  termMonths: number;
  depositAmount: number;
  rentAmountMonthly: number;
  buyoutPrice?: number | null;
  allowExtend: boolean;
  extensionUnit?: string | null;
  extensionPrice?: number | null;
  status: RentalPlanStatus;
  createdAt: string;
  updatedAt: string;
  skus: RentalSku[];
};

export type PagedResult<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type ProductPayload = {
  name: string;
  categoryCode: string;
  description?: string;
  coverImageUrl?: string;
};

export type ProductDetail = ProductSummary & {
  description?: string | null;
  coverImageUrl?: string | null;
  reviewRemark?: string | null;
  reviewedBy?: string | null;
  rentalPlans?: RentalPlan[];
};

export type ProductApprovalPayload = {
  reviewerId: string;
  remark?: string;
};

export type RentalPlanPayload = {
  planType: RentalPlanType;
  termMonths: number;
  depositAmount: number;
  rentAmountMonthly: number;
  buyoutPrice?: number | null;
  allowExtend: boolean;
  extensionUnit?: string | null;
  extensionPrice?: number | null;
};

export type SkuPayload = {
  skuCode: string;
  attributes?: Record<string, unknown>;
  stockTotal: number;
  stockAvailable?: number | null;
  status?: ProductSkuStatus;
};

export type InventoryAdjustPayload = {
  changeType: 'INBOUND' | 'OUTBOUND' | 'RESERVE' | 'RELEASE';
  quantity: number;
  referenceId?: string;
};

export const listVendorProducts = async (
  vendorId: string,
  params: { page?: number; size?: number; status?: ProductStatus; keyword?: string } = {}
): Promise<PagedResult<ProductSummary>> => {
  const response = await api.get(`/vendors/${vendorId}/products`, { params });
  return response.data.data as PagedResult<ProductSummary>;
};

export const createVendorProduct = async (
  vendorId: string,
  payload: ProductPayload
): Promise<ProductDetail> => {
  const response = await api.post(`/vendors/${vendorId}/products`, payload);
  return response.data.data as ProductDetail;
};

export const submitVendorProduct = async (
  vendorId: string,
  productId: string
): Promise<ProductDetail> => {
  const response = await api.post(`/vendors/${vendorId}/products/${productId}/submit`);
  return response.data.data as ProductDetail;
};

export const getVendorProduct = async (
  vendorId: string,
  productId: string
): Promise<ProductDetail> => {
  const response = await api.get(`/vendors/${vendorId}/products/${productId}`);
  return response.data.data as ProductDetail;
};

export const createRentalPlan = async (
  vendorId: string,
  productId: string,
  payload: RentalPlanPayload
): Promise<RentalPlan> => {
  const response = await api.post(`/vendors/${vendorId}/products/${productId}/rental-plans`, payload);
  return response.data.data as RentalPlan;
};

export const activateRentalPlan = async (
  vendorId: string,
  productId: string,
  planId: string
): Promise<RentalPlan> => {
  const response = await api.post(
    `/vendors/${vendorId}/products/${productId}/rental-plans/${planId}/activate`
  );
  return response.data.data as RentalPlan;
};

export const deactivateRentalPlan = async (
  vendorId: string,
  productId: string,
  planId: string
): Promise<RentalPlan> => {
  const response = await api.post(
    `/vendors/${vendorId}/products/${productId}/rental-plans/${planId}/deactivate`
  );
  return response.data.data as RentalPlan;
};

export const createSku = async (
  vendorId: string,
  productId: string,
  planId: string,
  payload: SkuPayload
): Promise<RentalSku> => {
  const response = await api.post(
    `/vendors/${vendorId}/products/${productId}/rental-plans/${planId}/skus`,
    payload
  );
  return response.data.data as RentalSku;
};

export const updateSku = async (
  vendorId: string,
  productId: string,
  planId: string,
  skuId: string,
  payload: SkuPayload
): Promise<RentalSku> => {
  const response = await api.put(
    `/vendors/${vendorId}/products/${productId}/rental-plans/${planId}/skus/${skuId}`,
    payload
  );
  return response.data.data as RentalSku;
};

export const adjustSkuInventory = async (
  vendorId: string,
  productId: string,
  planId: string,
  skuId: string,
  payload: InventoryAdjustPayload
): Promise<RentalSku> => {
  const response = await api.post(
    `/vendors/${vendorId}/products/${productId}/rental-plans/${planId}/skus/${skuId}/inventory/adjust`,
    payload
  );
  return response.data.data as RentalSku;
};

export const listAdminProducts = async (
  params: { status?: ProductStatus; keyword?: string; page?: number; size?: number } = {}
): Promise<PagedResult<ProductSummary>> => {
  const response = await api.get('/admin/products', { params });
  return response.data.data as PagedResult<ProductSummary>;
};

export const approveProduct = async (
  productId: string,
  payload: ProductApprovalPayload
): Promise<ProductDetail> => {
  const response = await api.post(`/admin/products/${productId}/approve`, payload);
  return response.data.data as ProductDetail;
};

export const rejectProduct = async (
  productId: string,
  payload: ProductApprovalPayload
): Promise<ProductDetail> => {
  const response = await api.post(`/admin/products/${productId}/reject`, payload);
  return response.data.data as ProductDetail;
};
