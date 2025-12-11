import http from './http';

export type VendorApplicationStatus = 'SUBMITTED' | 'APPROVED' | 'REJECTED';

export type VendorApplicationPayload = {
  companyName: string;
  unifiedSocialCode: string;
  contactName: string;
  contactPhone: string;
  contactEmail?: string;
  province?: string;
  city?: string;
  address?: string;
};

export type VendorApplicationReviewPayload = {
  remark?: string;
};

export type VendorApplication = {
  id: string;
  ownerUserId: string;
  companyName: string;
  unifiedSocialCode: string;
  contactName: string;
  contactPhone: string;
  contactEmail?: string | null;
  province?: string | null;
  city?: string | null;
  address?: string | null;
  status: VendorApplicationStatus;
  submittedAt?: string | null;
  reviewedBy?: string | null;
  reviewedAt?: string | null;
  reviewRemark?: string | null;
};

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export const submitVendorApplication = async (
  payload: VendorApplicationPayload
): Promise<VendorApplication> => {
  const response = await http.post<ApiResponse<VendorApplication>>('/vendors/applications', payload);
  return response.data.data;
};

export const listVendorApplications = async (
  status?: VendorApplicationStatus
): Promise<VendorApplication[]> => {
  const response = await http.get<ApiResponse<VendorApplication[]>>('/vendors/applications', {
    params: { status }
  });
  return response.data.data;
};

export const fetchVendorApplication = async (id: string): Promise<VendorApplication> => {
  const response = await http.get<ApiResponse<VendorApplication>>(`/vendors/applications/${id}`);
  return response.data.data;
};

export const approveVendorApplication = async (
  id: string,
  payload: VendorApplicationReviewPayload
): Promise<VendorApplication> => {
  const response = await http.post<ApiResponse<VendorApplication>>(`/vendors/applications/${id}/approve`, payload);
  return response.data.data;
};

export const rejectVendorApplication = async (
  id: string,
  payload: VendorApplicationReviewPayload
): Promise<VendorApplication> => {
  const response = await http.post<ApiResponse<VendorApplication>>(`/vendors/applications/${id}/reject`, payload);
  return response.data.data;
};

export type Vendor = {
  id: string;
  ownerUserId: string;
  companyName: string;
  contactName: string;
  contactPhone: string;
  contactEmail?: string | null;
  province?: string | null;
  city?: string | null;
  address?: string | null;
  status: string;
  commissionProfile?: VendorCommissionProfile | null;
};

export type PagedResponse<T> = {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type VendorCommissionProfile = {
  industryCategory: string;
  baseRate: number;
  creditTier: string;
  slaScore: number;
  commissionRate: number;
};

export const fetchVendor = async (vendorId: string): Promise<Vendor> => {
  const response = await http.get<ApiResponse<Vendor>>(`/vendors/${vendorId}`);
  return response.data.data;
};

export const listVendors = async (
  params: { status?: string; page?: number; size?: number } = {}
): Promise<PagedResponse<Vendor>> => {
  const response = await http.get<ApiResponse<PagedResponse<Vendor>>>('/vendors', {
    params
  });
  return response.data.data;
};

export type ProductInquiry = {
  id: string;
  productId: string;
  vendorId: string;
  requesterId?: string | null;
  contactName?: string | null;
  contactMethod?: string | null;
  message: string;
  status: 'OPEN' | 'RESPONDED' | 'EXPIRED';
  reply?: string | null;
  expiresAt: string;
  respondedAt?: string | null;
  createdAt: string;
};

export const listVendorInquiries = async (
  vendorId: string,
  params?: { status?: ProductInquiry['status'] }
): Promise<ProductInquiry[]> => {
  const response = await http.get<ApiResponse<ProductInquiry[]>>(`/vendors/${vendorId}/inquiries`, { params });
  return response.data.data;
};

export const replyVendorInquiry = async (
  vendorId: string,
  inquiryId: string,
  reply: string
): Promise<ProductInquiry> => {
  const response = await http.post<ApiResponse<ProductInquiry>>(
    `/vendors/${vendorId}/inquiries/${inquiryId}/reply`,
    { reply }
  );
  return response.data.data;
};
