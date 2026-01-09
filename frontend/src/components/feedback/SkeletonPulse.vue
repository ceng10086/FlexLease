<template>
  <div class="skeleton" :style="{ height }"></div>
</template>

<script lang="ts" setup>
// 轻量骨架屏：用于列表/卡片加载时的占位动效（避免页面“跳一下”）。
withDefaults(
  defineProps<{
    height?: string;
  }>(),
  {
    height: '18px'
  }
);
</script>

<style scoped>
.skeleton {
  width: 100%;
  border-radius: 999px;
  background: linear-gradient(
    120deg,
    rgba(148, 163, 184, 0.3),
    rgba(148, 163, 184, 0.15),
    rgba(148, 163, 184, 0.3)
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s linear infinite;
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
