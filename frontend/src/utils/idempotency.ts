const randomHex = () => Math.floor(Math.random() * 0xffff)
  .toString(16)
  .padStart(4, '0');

const fallbackUuid = () =>
  `${randomHex()}${randomHex()}-${randomHex()}-${randomHex()}-${randomHex()}-${randomHex()}${randomHex()}${randomHex()}`;

/**
 * Generate a best-effort UUID string for Idempotency-Key headers.
 * Optionally accept a namespace prefix to aid debugging/tracing.
 */
export const generateIdempotencyKey = (namespace?: string): string => {
  const key =
    typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function'
      ? crypto.randomUUID()
      : fallbackUuid();
  if (namespace && namespace.trim()) {
    return `${namespace.trim()}_${key}`;
  }
  return key;
};
