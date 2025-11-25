import http from './http';

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
  contextType?: string;
  contextReference?: string;
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
  contextType?: string | null;
  contextReference?: string | null;
  sentAt?: string | null;
  createdAt: string;
};

export type NotificationLogQuery = {
  status?: NotificationStatus;
  channel?: NotificationChannel;
  recipient?: string;
  contextType?: string;
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
