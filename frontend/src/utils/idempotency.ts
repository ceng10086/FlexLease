const randomHex = () => Math.floor(Math.random() * 0xffff)
  .toString(16)
  .padStart(4, '0');

const fallbackUuid = () =>
  `${randomHex()}${randomHex()}-${randomHex()}-${randomHex()}-${randomHex()}-${randomHex()}${randomHex()}${randomHex()}`;

/**
 * 生成 Idempotency-Key 请求头用的 UUID（尽力而为）。
 * 可选传入 namespace 前缀，便于排查/追踪。
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
