import http from '@/api/http';
import type { ApiResponseEnvelope, UserProfile } from '@/types';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  expiresIn: number;
}

export async function login(payload: LoginRequest): Promise<TokenResponse> {
  const { data } = await http.post<ApiResponseEnvelope<TokenResponse>>('/api/v1/auth/token', payload);
  if (!data.success) {
    throw new Error(data.message ?? '登录失败');
  }
  return data.data;
}

export async function fetchProfile(): Promise<UserProfile> {
  const { data } = await http.get<ApiResponseEnvelope<UserProfile>>('/api/v1/auth/me');
  if (!data.success) {
    throw new Error(data.message ?? '获取用户信息失败');
  }
  return data.data;
}
