/**
 * 通知中心 API（notification-service）：
 * - 站内信列表、状态/场景过滤等
 */
import http from './http';

export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED';

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export type NotificationSendPayload = {
  templateCode?: string;
  recipient: string;
  subject?: string;
  content?: string;
  variables?: Record<string, unknown>;
  contextType?: string;
  contextReference?: string;
};

export type NotificationLog = {
  id: string;
  templateCode?: string | null;
  recipient: string;
  subject: string;
  content: string;
  status: NotificationStatus;
  errorMessage?: string | null;
  contextType?: string | null;
  contextReference?: string | null;
  sentAt?: string | null;
  createdAt: string;
};

export type NotificationLogQuery = {
  status?: NotificationStatus;
  recipient?: string;
  contextType?: string;
};

export type NotificationTemplate = {
  id: string;
  code: string;
  subject: string;
  content: string;
  createdAt: string;
};

export const sendNotification = async (payload: NotificationSendPayload) => {
  const response = await http.post<ApiResponse<any>>('/notifications/send', payload);
  return response.data.data;
};

export const listNotificationLogs = async (filters: NotificationLogQuery = {}) => {
  const response = await http.get<ApiResponse<NotificationLog[]>>('/notifications/logs', {
    params: filters
  });
  return response.data.data;
};

export const listNotificationTemplates = async () => {
  const response = await http.get<ApiResponse<NotificationTemplate[]>>('/notifications/templates');
  return response.data.data;
};
