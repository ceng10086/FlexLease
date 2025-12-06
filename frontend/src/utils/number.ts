export const formatCurrency = (
  value: number | string | null | undefined,
  fractionDigits = 2
): string => {
  if (value === null || value === undefined) {
    return (0).toFixed(fractionDigits);
  }
  const numeric = typeof value === 'string' ? Number(value) : value;
  if (Number.isNaN(numeric)) {
    return (0).toFixed(fractionDigits);
  }
  return Number(numeric).toFixed(fractionDigits);
};
