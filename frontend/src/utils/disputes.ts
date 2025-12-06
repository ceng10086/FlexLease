import type { DisputeResolutionOption, OrderDispute } from '../services/orderService';

export const disputeOptions: { label: string; value: DisputeResolutionOption }[] = [
  { label: '重新发货/补发', value: 'REDELIVER' },
  { label: '部分退款继续租赁', value: 'PARTIAL_REFUND' },
  { label: '退租并扣押金', value: 'RETURN_WITH_DEPOSIT_DEDUCTION' },
  { label: '优惠买断', value: 'DISCOUNTED_BUYOUT' },
  { label: '自定义方案', value: 'CUSTOM' }
];

const optionMap = disputeOptions.reduce<Record<DisputeResolutionOption, string>>((acc, option) => {
  acc[option.value] = option.label;
  return acc;
}, {} as Record<DisputeResolutionOption, string>);

export const disputeOptionLabel = (option?: DisputeResolutionOption | null) => {
  if (!option) {
    return '未填写';
  }
  return optionMap[option] ?? option;
};

export const disputeStatusLabel = (status: OrderDispute['status']) => {
  switch (status) {
    case 'OPEN':
      return '协商中';
    case 'PENDING_ADMIN':
      return '待平台处理';
    case 'RESOLVED':
      return '已协调';
    case 'CLOSED':
      return '已结案';
    default:
      return status;
  }
};

export const disputeStatusColor = (status: OrderDispute['status']) => {
  switch (status) {
    case 'OPEN':
      return 'orange';
    case 'PENDING_ADMIN':
      return 'blue';
    case 'RESOLVED':
      return 'green';
    case 'CLOSED':
      return 'red';
    default:
      return 'default';
  }
};

export const disputeActorLabel = (role?: string | null) => {
  switch (role) {
    case 'USER':
      return '消费者';
    case 'VENDOR':
      return '厂商';
    case 'ADMIN':
    case 'INTERNAL':
      return '平台';
    default:
      return '系统';
  }
};
