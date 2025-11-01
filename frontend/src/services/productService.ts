import http from './http';

export type ProductStatus = 'DRAFT' | 'PENDING_REVIEW' | 'ACTIVE' | 'INACTIVE' | 'REJECTED';
export type RentalPlanType = 'STANDARD' | 'RENT_TO_OWN' | 'LEASE_TO_SALE';
export type RentalPlanStatus = 'DRAFT' | 'ACTIVE' | 'INACTIVE';
export type ProductSkuStatus = 'ACTIVE' | 'INACTIVE';

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

export type RentalSku = {
  id: string;
  skuCode: string;
  attributes?: Record<string, unknown> | null;
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

export type ProductDetail = ProductSummary & {
  description?: string | null;
  coverImageUrl?: string | null;
  reviewRemark?: string | null;
  reviewedBy?: string | null;
  rentalPlans: RentalPlan[];
};

export type ProductPayload = {
  name: string;
  categoryCode: string;
  description?: string;
  coverImageUrl?: string;
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

export type ProductApprovalPayload = {
  remark?: string;
};

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export type PagedResult<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export const listVendorProducts = async (
  vendorId: string,
  params: { page?: number; size?: number; status?: ProductStatus; keyword?: string } = {}
): Promise<PagedResult<ProductSummary>> => {
  const response = await http.get<ApiResponse<PagedResult<ProductSummary>>>(
    `/vendors/${vendorId}/products`,
    { params }
  );
  return response.data.data;
};

export const createVendorProduct = async (
  vendorId: string,
  payload: ProductPayload
): Promise<ProductDetail> => {
  const response = await http.post<ApiResponse<ProductDetail>>(`/vendors/${vendorId}/products`, payload);
  return response.data.data;
};

export const updateVendorProduct = async (
  vendorId: string,
  productId: string,
  payload: ProductPayload
): Promise<ProductDetail> => {
  const response = await http.put<ApiResponse<ProductDetail>>(
    `/vendors/${vendorId}/products/${productId}`,
    payload
  );
  return response.data.data;
};

export const submitVendorProduct = async (
  vendorId: string,
  productId: string
): Promise<ProductDetail> => {
  const response = await http.post<ApiResponse<ProductDetail>>(
    `/vendors/${vendorId}/products/${productId}/submit`
  );
  return response.data.data;
};

export const toggleProductShelf = async (
  vendorId: string,
  productId: string,
  publish: boolean
): Promise<ProductDetail> => {
  const response = await http.post<ApiResponse<ProductDetail>>(
    `/vendors/${vendorId}/products/${productId}/shelve`,
    { publish }
  );
  return response.data.data;
};

export const fetchVendorProduct = async (
  vendorId: string,
  productId: string
): Promise<ProductDetail> => {
  const response = await http.get<ApiResponse<ProductDetail>>(
    `/vendors/${vendorId}/products/${productId}`
  );
  return response.data.data;
};

export const createRentalPlan = async (
  vendorId: string,
  productId: string,
  payload: RentalPlanPayload
): Promise<RentalPlan> => {
  const response = await http.post<ApiResponse<RentalPlan>>(
    `/vendors/${vendorId}/products/${productId}/rental-plans`,
    payload
  );
  return response.data.data;
};

export const activateRentalPlan = async (
  vendorId: string,
  productId: string,
  planId: string
): Promise<RentalPlan> => {
  const response = await http.post<ApiResponse<RentalPlan>>(
    `/vendors/${vendorId}/products/${productId}/rental-plans/${planId}/activate`
  );
  return response.data.data;
};

export const deactivateRentalPlan = async (
  vendorId: string,
  productId: string,
  planId: string
): Promise<RentalPlan> => {
  const response = await http.post<ApiResponse<RentalPlan>>(
    `/vendors/${vendorId}/products/${productId}/rental-plans/${planId}/deactivate`
  );
  return response.data.data;
};

export const createSku = async (
  vendorId: string,
  productId: string,
  planId: string,
  payload: SkuPayload
): Promise<RentalSku> => {
  const response = await http.post<ApiResponse<RentalSku>>(
    `/vendors/${vendorId}/products/${productId}/rental-plans/${planId}/skus`,
    payload
  );
  return response.data.data;
};

export const updateSku = async (
  vendorId: string,
  productId: string,
  planId: string,
  skuId: string,
  payload: SkuPayload
): Promise<RentalSku> => {
  const response = await http.put<ApiResponse<RentalSku>>(
    `/vendors/${vendorId}/products/${productId}/rental-plans/${planId}/skus/${skuId}`,
    payload
  );
  return response.data.data;
};

export const adjustInventory = async (
  vendorId: string,
  productId: string,
  planId: string,
  skuId: string,
  payload: InventoryAdjustPayload
): Promise<RentalSku> => {
  const response = await http.post<ApiResponse<RentalSku>>(
    `/vendors/${vendorId}/products/${productId}/rental-plans/${planId}/skus/${skuId}/inventory/adjust`,
    payload
  );
  return response.data.data;
};

export const listAdminProducts = async (
  params: { status?: ProductStatus; keyword?: string; page?: number; size?: number } = {}
): Promise<PagedResult<ProductSummary>> => {
  const response = await http.get<ApiResponse<PagedResult<ProductSummary>>>('/admin/products', {
    params
  });
  return response.data.data;
};

export const approveProduct = async (
  productId: string,
  payload: ProductApprovalPayload
): Promise<ProductDetail> => {
  const response = await http.post<ApiResponse<ProductDetail>>(
    `/admin/products/${productId}/approve`,
    payload
  );
  return response.data.data;
};

export const rejectProduct = async (
  productId: string,
  payload: ProductApprovalPayload
): Promise<ProductDetail> => {
  const response = await http.post<ApiResponse<ProductDetail>>(
    `/admin/products/${productId}/reject`,
    payload
  );
  return response.data.data;
};
