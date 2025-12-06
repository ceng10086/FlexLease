import { computed, ref, unref, watch, type Ref, type WatchSource } from 'vue';

type UseQueryOptions<T> = {
  immediate?: boolean;
  enabled?: Ref<boolean> | boolean;
  cache?: boolean;
  dependencies?: WatchSource | WatchSource[];
  transform?: (value: T) => T;
};

const cacheStore = new Map<string, unknown>();

export const useQuery = <T>(
  key: string | (() => string),
  handler: () => Promise<T>,
  options: UseQueryOptions<T> = {}
) => {
  const loading = ref(false);
  const error = ref<Error | null>(null);
  const cacheKey = computed(() => (typeof key === 'function' ? key() : key));
  const data = ref<T | null>(null);

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
    const finalKey = cacheKey.value;
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
    cacheKey,
    () => {
      if (options.cache !== false && cacheStore.has(cacheKey.value)) {
        data.value = cacheStore.get(cacheKey.value) as T;
      } else {
        data.value = null;
      }
      if (options.immediate ?? true) {
        execute();
      }
    },
    { immediate: true }
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
