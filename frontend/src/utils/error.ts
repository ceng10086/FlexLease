/**
 * 统一错误提示：
 * - 将 axios/network/后端错误 payload 转成更友好的中文提示
 */
import type { AxiosError } from 'axios';

type ErrorPayload = {
  message?: string;
  error?: string;
  errors?: Array<{ field?: string; message?: string } | string>;
};

const DEFAULT_MESSAGES: Record<string, string> = {
  network: '无法连接服务器，请检查网络后重试',
  unauthorized: '登录状态已过期，请重新登录',
  forbidden: '暂无访问权限，请联系管理员开通',
  notFound: '资源不存在或已被删除',
  server: '服务器开小差了，请稍后再试',
  timeout: '请求超时，请稍后再试',
  loginFailed: '账号或密码错误，请重新输入'
};

const isAxiosError = (error: unknown): error is AxiosError<ErrorPayload> =>
  typeof error === 'object' && error !== null && 'isAxiosError' in error;

const formatValidationErrors = (payload?: ErrorPayload) => {
  if (!payload?.errors || payload.errors.length === 0) {
    return null;
  }
  return payload.errors
    .map((item) => {
      if (!item) {
        return null;
      }
      if (typeof item === 'string') {
        return item;
      }
      if (item.message && item.field) {
        return `${item.field}: ${item.message}`;
      }
      return item.message ?? null;
    })
    .filter(Boolean)
    .join('；');
};

const mapHttpStatusToMessage = (status?: number, url?: string) => {
  if (!status) {
    return null;
  }
  if (status === 401 && url?.includes('/auth/token')) {
    return DEFAULT_MESSAGES.loginFailed;
  }
  if (status === 401) {
    return DEFAULT_MESSAGES.unauthorized;
  }
  if (status === 403) {
    return DEFAULT_MESSAGES.forbidden;
  }
  if (status === 404) {
    return DEFAULT_MESSAGES.notFound;
  }
  if (status >= 500) {
    return DEFAULT_MESSAGES.server;
  }
  return null;
};

export const extractApiErrorMessage = (error: unknown, fallback = DEFAULT_MESSAGES.server): string => {
  if (typeof error === 'string') {
    return error;
  }

  if (error instanceof Error && !isAxiosError(error)) {
    return error.message || fallback;
  }

  if (isAxiosError(error)) {
    if (error.code === 'ECONNABORTED') {
      return DEFAULT_MESSAGES.timeout;
    }
    const responseData = error.response?.data;
    const validationMessage = formatValidationErrors(responseData);
    if (validationMessage) {
      return validationMessage;
    }
    if (responseData?.message) {
      return responseData.message;
    }
    if (responseData?.error) {
      return responseData.error;
    }
    const statusMessage = mapHttpStatusToMessage(error.response?.status, error.config?.url);
    if (statusMessage) {
      return statusMessage;
    }
    if (error.message) {
      return error.message;
    }
  }

  return fallback;
};

export const isNetworkOfflineError = (error: unknown) => {
  if (isAxiosError(error)) {
    if (error.code === 'ERR_NETWORK') {
      return true;
    }
    if (!error.response && error.message.includes('Network Error')) {
      return true;
    }
  }
  if (error instanceof TypeError && error.message === 'Failed to fetch') {
    return true;
  }
  return false;
};

export const friendlyErrorMessage = (error: unknown, fallback?: string) => {
  if (isNetworkOfflineError(error)) {
    return DEFAULT_MESSAGES.network;
  }
  return extractApiErrorMessage(error, fallback);
};
