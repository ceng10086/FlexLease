/**
 * 极简 useQuery（不引入第三方状态库）：
 * - 支持 enabled/immediate/dependencies 触发
 * - 默认按“用户 scope + key”做内存缓存，避免跨账号串数据
 */
import { computed, ref, unref, watch, type Ref, type WatchSource } from 'vue';

type UseQueryOptions<T> = {
  immediate?: boolean;
  enabled?: Ref<boolean> | boolean;
  cache?: boolean;
  dependencies?: WatchSource | WatchSource[];
  transform?: (value: T) => T;
};

const DEFAULT_SCOPE = 'anonymous';
const cacheStore = new Map<string, unknown>();
const cacheScope = ref(DEFAULT_SCOPE);

export const setQueryCacheScope = (scope?: string | null) => {
  const normalized = scope?.trim() || DEFAULT_SCOPE;
  if (cacheScope.value !== normalized) {
    cacheScope.value = normalized;
  }
};

export const clearQueryCache = (scope?: string | null) => {
  if (!scope) {
    cacheStore.clear();
    return;
  }
  const prefix = `${scope}::`;
  Array.from(cacheStore.keys()).forEach((key) => {
    if (key.startsWith(prefix)) {
      cacheStore.delete(key);
    }
  });
};

export const useQuery = <T>(
  key: string | (() => string),
  handler: () => Promise<T>,
  options: UseQueryOptions<T> = {}
) => {
  const loading = ref(false);
  const error = ref<Error | null>(null);
  const resolvedKey = computed(() => (typeof key === 'function' ? key() : key));
  const scopedKey = computed(() => `${cacheScope.value}::${resolvedKey.value}`);
  const data = ref<T | null>(null);
  const shouldAutoRun = options.immediate ?? true;

  const enabled = computed(() => {
    if (options.enabled === undefined) {
      return true;
    }
    return Boolean(unref(options.enabled));
  });

  const execute = async () => {
    if (!enabled.value) {
      return;
    }
    const finalKey = scopedKey.value;
    loading.value = true;
    error.value = null;
    try {
      const result = await handler();
      const finalResult = options.transform ? options.transform(result) : result;
      data.value = finalResult;
      if (options.cache !== false) {
        cacheStore.set(finalKey, finalResult);
      }
    } catch (err) {
      const friendlyError = err instanceof Error ? err : new Error(String(err));
      error.value = friendlyError;
      throw friendlyError;
    } finally {
      loading.value = false;
    }
  };

  const refresh = () => execute();

  watch(
    scopedKey,
    () => {
      if (options.cache !== false && cacheStore.has(scopedKey.value)) {
        data.value = cacheStore.get(scopedKey.value) as T;
      } else {
        data.value = null;
      }
      if (shouldAutoRun) {
        execute();
      }
    },
    { immediate: true }
  );

  watch(
    enabled,
    (value, previous) => {
      if (value && !previous && shouldAutoRun) {
        execute();
      }
    },
    { immediate: false }
  );

  const deps = options.dependencies;
  if (deps) {
    watch(
      Array.isArray(deps) ? deps : [deps],
      () => {
        execute();
      }
    );
  }

  return {
    data,
    loading,
    error,
    refresh
  };
};
