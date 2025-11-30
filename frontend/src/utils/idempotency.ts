const randomHex = () => Math.floor(Math.random() * 0xffff)
  .toString(16)
  .padStart(4, '0');

const fallbackUuid = () =>
  `${randomHex()}${randomHex()}-${randomHex()}-${randomHex()}-${randomHex()}-${randomHex()}${randomHex()}${randomHex()}`;

/**
 * Generate a best-effort UUID string for Idempotency-Key headers.
 */
export const generateIdempotencyKey = (): string => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  return fallbackUuid();
};
