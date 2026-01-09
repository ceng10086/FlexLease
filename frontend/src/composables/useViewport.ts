/**
 * 视口尺寸监听：
 * - 提供响应式的 width/height 与断点判断（isMobile/isTablet）
 */
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';

const fallbackSize = { width: 1280, height: 720 };

const readViewport = () => {
  if (typeof window === 'undefined') {
    return fallbackSize;
  }
  return {
    width: window.innerWidth,
    height: window.innerHeight
  };
};

export const useViewport = () => {
  const initialSize = readViewport();
  const width = ref<number>(initialSize.width);
  const height = ref<number>(initialSize.height);

  const handleResize = () => {
    const next = readViewport();
    width.value = next.width;
    height.value = next.height;
  };

  onMounted(() => {
    handleResize();
    window.addEventListener('resize', handleResize);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('resize', handleResize);
  });

  const isMobile = computed(() => width.value <= 768);
  const isTablet = computed(() => width.value <= 1024);

  return {
    width,
    height,
    isMobile,
    isTablet
  };
};
