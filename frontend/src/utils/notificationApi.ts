import api from './api';

export type NotificationStatus = 'PENDING' | 'SENT' | 'FAILED';
export type NotificationChannel = 'IN_APP' | 'EMAIL' | 'SMS';

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export type NotificationSendPayload = {
  templateCode?: string;
  channel?: NotificationChannel;
  recipient: string;
  subject?: string;
  content?: string;
  variables?: Record<string, unknown>;
};

export type NotificationLog = {
  id: string;
  templateCode?: string | null;
  channel: NotificationChannel;
  recipient: string;
  subject: string;
  content: string;
  status: NotificationStatus;
  errorMessage?: string | null;
  sentAt?: string | null;
  createdAt: string;
};

export type NotificationTemplate = {
  id: string;
  code: string;
  channel: NotificationChannel;
  subject: string;
  content: string;
  createdAt: string;
};

export const sendNotification = async (payload: NotificationSendPayload) => {
  const response = await api.post<ApiResponse<any>>('/notifications/send', payload);
  return response.data.data;
};

export const listNotificationLogs = async (status?: NotificationStatus) => {
  const response = await api.get<ApiResponse<NotificationLog[]>>('/notifications/logs', {
    params: { status }
  });
  return response.data.data;
};

export const listNotificationTemplates = async () => {
  const response = await api.get<ApiResponse<NotificationTemplate[]>>('/notifications/templates');
  return response.data.data;
};
