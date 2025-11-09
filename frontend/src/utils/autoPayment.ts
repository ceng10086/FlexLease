import { initPayment } from '../services/paymentService';

export type AutoPaymentParams = {
  orderId: string;
  vendorId: string;
  userId: string;
  amount?: number | null;
  description?: string;
};

export const autoCompleteInitialPayment = async ({
  orderId,
  vendorId,
  userId,
  amount,
  description
}: AutoPaymentParams): Promise<boolean> => {
  if (!amount || amount <= 0) {
    return false;
  }
  await initPayment(orderId, {
    userId,
    vendorId,
    scene: 'DEPOSIT',
    channel: 'MOCK',
    amount,
    description: description ?? '自动支付（下单即付）'
  });
  return true;
};
