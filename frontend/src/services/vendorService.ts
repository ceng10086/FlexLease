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
  reviewerId: string;
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
  ownerUserId: string,
  payload: VendorApplicationPayload
): Promise<VendorApplication> => {
  const response = await http.post<ApiResponse<VendorApplication>>('/vendors/applications', payload, {
    headers: {
      'X-User-Id': ownerUserId
    }
  });
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
  const response = await http.post<ApiResponse<VendorApplication>>(
    `/vendors/applications/${id}/approve`,
    payload
  );
  return response.data.data;
};

export const rejectVendorApplication = async (
  id: string,
  payload: VendorApplicationReviewPayload
): Promise<VendorApplication> => {
  const response = await http.post<ApiResponse<VendorApplication>>(
    `/vendors/applications/${id}/reject`,
    payload
  );
  return response.data.data;
};
