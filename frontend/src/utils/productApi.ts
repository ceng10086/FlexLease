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
};

export type ProductApprovalPayload = {
  reviewerId: string;
  remark?: string;
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
