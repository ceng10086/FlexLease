<template>
  <div class="media-gallery">
    <div class="media-gallery__main" :style="mainStyle">
      <span v-if="!activeImage">暂无图片</span>
    </div>
    <div class="media-gallery__thumbs">
      <button
        v-for="(image, index) in displayImages"
        :key="index"
        type="button"
        class="media-gallery__thumb"
        :class="{ 'media-gallery__thumb--active': image === activeImage }"
        :style="{ backgroundImage: `url(${image})` }"
        @click="activeImage = image"
      />
    </div>
  </div>
</template>

<script lang="ts" setup>
// 媒体画廊：展示商品图片/视频缩略图与预览（以“可演示”为目标，功能从简）。
import { computed, ref, watch } from 'vue';

const props = defineProps<{
  cover?: string | null;
  media?: string[];
}>();

const activeImage = ref<string | null>(props.cover ?? props.media?.[0] ?? null);

const displayImages = computed(() => {
  const bucket = new Set<string>();
  if (props.cover) {
    bucket.add(props.cover);
  }
  (props.media ?? []).forEach((url) => bucket.add(url));
  return Array.from(bucket);
});

watch(
  () => [props.cover, props.media],
  () => {
    activeImage.value = props.cover ?? props.media?.[0] ?? null;
  },
  { immediate: true }
);

const mainStyle = computed(() => ({
  backgroundImage: activeImage.value
    ? `linear-gradient(180deg, rgba(0,0,0,0), rgba(0,0,0,0.25)), url(${activeImage.value})`
    : 'linear-gradient(135deg, rgba(219,234,254,0.8), rgba(191,219,254,0.8))'
}));
</script>

<style scoped>
.media-gallery {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.media-gallery__main {
  height: 320px;
  border-radius: var(--radius-card);
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-secondary);
  font-weight: 600;
}

.media-gallery__thumbs {
  display: flex;
  gap: var(--space-2);
  overflow-x: auto;
}

.media-gallery__thumb {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-card);
  border: 2px solid transparent;
  background-size: cover;
  background-position: center;
  padding: 0;
}

.media-gallery__thumb--active {
  border-color: var(--color-primary);
}
</style>
