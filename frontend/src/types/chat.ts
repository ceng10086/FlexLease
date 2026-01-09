/**
 * 聊天/沟通相关类型：
 * - 订单沟通中心与详情聊天页复用
 */
export type ChatSendPayload = {
  content: string;
  attachments: File[];
};
