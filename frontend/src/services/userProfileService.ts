import http from './http';
import type { CreditTier } from '../types/credit';

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export type UserProfileGender = 'UNKNOWN' | 'MALE' | 'FEMALE';

export type UserProfile = {
  id: string;
  userId: string;
  fullName?: string | null;
  gender: UserProfileGender;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  creditScore: number;
  creditTier: CreditTier;
  createdAt?: string;
  updatedAt?: string;
};

export type UserProfileUpdatePayload = {
  fullName: string;
  gender: UserProfileGender;
  phone: string;
  email: string;
  address?: string;
};

export const fetchUserProfile = async (): Promise<UserProfile> => {
  const response = await http.get<ApiResponse<UserProfile>>('/customers/profile');
  return response.data.data;
};

export const updateUserProfile = async (payload: UserProfileUpdatePayload): Promise<UserProfile> => {
  const response = await http.put<ApiResponse<UserProfile>>('/customers/profile', payload);
  return response.data.data;
};
