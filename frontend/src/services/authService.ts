import http from './http';

export type AuthUser = {
  id: string;
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
  user?: AuthUser;
};

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export const login = async (payload: LoginPayload): Promise<AuthSession> => {
  const response = await http.post<ApiResponse<AuthSession>>('/auth/token', payload);
  const session = response.data?.data;
  if (!session?.accessToken) {
    throw new Error('登录失败，请检查账号或稍后再试');
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
  const response = await http.get<ApiResponse<AuthUser>>('/auth/me');
  const user = response.data?.data;
  if (!user) {
    throw new Error('未能获取当前用户信息');
  }
  return user;
};
