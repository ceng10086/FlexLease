import {
  initPayment,
  type PaymentScene,
  type PaymentSplitPayload,
  type PaymentStatus
} from '../services/paymentService';

export type AutoPaymentParams = {
  orderId: string;
  vendorId: string;
  userId: string;
  amount?: number | null;
  depositAmount?: number | null;
  rentAmount?: number | null;
  buyoutAmount?: number | null;
  description?: string;
};

export type AutoPaymentResult = {
  succeeded: boolean;
  status: PaymentStatus;
  transactionId?: string;
};

export const autoCompleteInitialPayment = async ({
  orderId,
  vendorId,
  userId,
  amount,
  depositAmount,
  rentAmount,
  buyoutAmount,
  description
}: AutoPaymentParams): Promise<AutoPaymentResult> => {
  const normalize = (value?: number | null) => {
    if (value === undefined || value === null || Number.isNaN(value)) {
      return 0;
    }
    return Math.max(0, Math.round(value * 100) / 100);
  };

  const depositPortion = normalize(depositAmount);
  const rentPortionRaw = normalize(rentAmount);
  const buyoutPortionRaw = normalize(buyoutAmount);
  const totalAmount = normalize(amount ?? (depositPortion + rentPortionRaw + buyoutPortionRaw));

  if (!totalAmount || totalAmount <= 0) {
    return {
      succeeded: false,
      status: 'PENDING'
    };
  }

  const splits: PaymentSplitPayload[] = [];
  let scene: PaymentScene = 'DEPOSIT';
  if (depositPortion > 0) {
    splits.push({
      splitType: 'DEPOSIT_RESERVE',
      amount: Math.min(depositPortion, totalAmount),
      beneficiary: 'PLATFORM_RESERVE'
    });
  }

  const maxVendorPortion = Math.max(totalAmount - (splits[0]?.amount ?? 0), 0);
  const vendorIncomePortion = Math.min(rentPortionRaw + buyoutPortionRaw, maxVendorPortion);
  if (vendorIncomePortion > 0) {
    splits.push({
      splitType: 'VENDOR_INCOME',
      amount: vendorIncomePortion,
      beneficiary: `VENDOR_${vendorId}`
    });
  }

  if (buyoutPortionRaw > 0) {
    scene = 'BUYOUT';
  } else if (rentPortionRaw > 0) {
    scene = 'RENT';
  } else if (depositPortion > 0) {
    scene = 'DEPOSIT';
  }

  const transaction = await initPayment(orderId, {
    userId,
    vendorId,
    scene,
    channel: 'MOCK',
    amount: totalAmount,
    description: description ?? '自动支付（下单即付）',
    splits: splits.length ? splits : undefined
  });
  const status = (transaction?.status as PaymentStatus) ?? ('PENDING' as PaymentStatus);
  return {
    succeeded: status === 'SUCCEEDED',
    status,
    transactionId: transaction?.id
  };
};
