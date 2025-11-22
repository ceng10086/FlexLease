export type CreditTier = 'EXCELLENT' | 'STANDARD' | 'WARNING' | 'RESTRICTED';

export const creditTierLabel = (tier?: CreditTier | null) => {
  switch (tier) {
    case 'EXCELLENT':
      return '信用优享';
    case 'WARNING':
      return '信用预警';
    case 'RESTRICTED':
      return '受限';
    case 'STANDARD':
      return '标准';
    default:
      return '未评估';
  }
};

export const creditTierColor = (tier?: CreditTier | null) => {
  switch (tier) {
    case 'EXCELLENT':
      return 'green';
    case 'STANDARD':
      return 'blue';
    case 'WARNING':
      return 'orange';
    case 'RESTRICTED':
      return 'red';
    default:
      return 'default';
  }
};
