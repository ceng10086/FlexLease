export type PlanSnapshot = {
  planId?: string | null;
  planType?: string | null;
  termMonths?: number | null;
  depositAmount?: number | null;
  rentAmountMonthly?: number | null;
  buyoutPrice?: number | null;
};

export type PlanSnapshotInput = PlanSnapshot;

export const serializePlanSnapshot = (input: PlanSnapshotInput): string =>
  JSON.stringify({
    planId: input.planId ?? null,
    planType: input.planType ?? null,
    termMonths: normalizeNumber(input.termMonths),
    depositAmount: normalizeNumber(input.depositAmount),
    rentAmountMonthly: normalizeNumber(input.rentAmountMonthly),
    buyoutPrice: normalizeNumber(input.buyoutPrice)
  });

export const parsePlanSnapshot = (raw?: string | null): PlanSnapshot | null => {
  if (!raw) {
    return null;
  }

  try {
    const parsed = JSON.parse(raw) as PlanSnapshot | null;
    if (!parsed || typeof parsed !== 'object') {
      return null;
    }
    return {
      planId: parsed.planId ?? null,
      planType: parsed.planType ?? null,
      termMonths: normalizeNumber(parsed.termMonths),
      depositAmount: normalizeNumber(parsed.depositAmount),
      rentAmountMonthly: normalizeNumber(parsed.rentAmountMonthly),
      buyoutPrice: normalizeNumber(parsed.buyoutPrice)
    };
  } catch (error) {
    console.warn('Failed to parse planSnapshot payload', error);
    return null;
  }
};

export const resolveDeposit = (
  explicitValue: number | null | undefined,
  snapshot: PlanSnapshot | null
): number | null => explicitValue ?? snapshot?.depositAmount ?? null;

export const resolveRent = (
  explicitValue: number | null | undefined,
  snapshot: PlanSnapshot | null
): number | null => explicitValue ?? snapshot?.rentAmountMonthly ?? null;

export const resolveBuyout = (
  explicitValue: number | null | undefined,
  snapshot: PlanSnapshot | null
): number | null => explicitValue ?? snapshot?.buyoutPrice ?? null;

const normalizeNumber = (value?: unknown): number | null => {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value;
  }
  if (typeof value === 'string') {
    const parsed = Number.parseFloat(value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
};
