/**
 * 认证相关 API：
 * - 登录、刷新、注册、拉取当前用户（/auth/me）
 * - 对 auth-service 的 503 做轻量重试，避免服务启动初期抖动影响体验
 */
import http from './http';
import type { AxiosError } from 'axios';

export type AuthUser = {
  id: string;
  vendorId?: string | null;
  username: string;
  roles: string[];
  lastLoginAt?: string | null;
};

export type LoginPayload = {
  username: string;
  password: string;
};

export type RegisterPayload = {
  username: string;
  password: string;
};

export type AuthSession = {
  accessToken: string;
  expiresInSeconds?: number;
  refreshToken: string;
  refreshExpiresInSeconds?: number;
  user?: AuthUser;
};

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

const shouldRetryAuth = (error: unknown) => {
  const axiosError = error as AxiosError | undefined;
  const status = axiosError?.response?.status;
  const url = axiosError?.config?.url ?? '';
  return status === 503 && url.includes('/auth/');
};

const withAuthRetry = async <T>(fn: () => Promise<T>): Promise<T> => {
  try {
    return await fn();
  } catch (error) {
    if (!shouldRetryAuth(error)) {
      throw error;
    }
    await sleep(800);
    return await fn();
  }
};

export const login = async (payload: LoginPayload): Promise<AuthSession> => {
  const response = await withAuthRetry(() => http.post<ApiResponse<AuthSession>>('/auth/token', payload));
  const session = response.data?.data;
  if (!session?.accessToken) {
    throw new Error('登录失败，请检查账号或稍后再试');
  }
  if (!session.refreshToken) {
    throw new Error('登录返回缺少刷新令牌');
  }
  return session;
};

export const refreshAuthToken = async (token: string): Promise<AuthSession> => {
  const response = await withAuthRetry(() =>
    http.post<ApiResponse<AuthSession>>(
      '/auth/token/refresh',
      { refreshToken: token },
      { _skipAuthRefresh: true } as any
    )
  );
  const session = response.data?.data;
  if (!session?.accessToken || !session.refreshToken) {
    throw new Error('刷新登录状态失败');
  }
  return session;
};

export const registerCustomer = async (payload: RegisterPayload): Promise<AuthUser> => {
  const response = await http.post<ApiResponse<AuthUser>>('/auth/register/customer', payload);
  return response.data.data;
};

export const registerVendor = async (payload: RegisterPayload): Promise<AuthUser> => {
  const response = await http.post<ApiResponse<AuthUser>>('/auth/register/vendor', payload);
  return response.data.data;
};

export const fetchCurrentUser = async (): Promise<AuthUser> => {
  const response = await withAuthRetry(() => http.get<ApiResponse<AuthUser>>('/auth/me'));
  const user = response.data?.data;
  if (!user) {
    throw new Error('未能获取当前用户信息');
  }
  return user;
};
