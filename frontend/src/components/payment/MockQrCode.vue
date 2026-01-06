<template>
  <div class="qr" :style="{ '--size': size + 'px' }">
    <svg
      :width="size"
      :height="size"
      viewBox="0 0 210 210"
      role="img"
      aria-label="Mock QR Code"
    >
      <rect x="0" y="0" width="210" height="210" fill="#fff" />

      <!-- 定位图形（Finder Pattern，用于模拟二维码的三个角标） -->
      <g fill="#000">
        <rect x="0" y="0" width="70" height="70" />
        <rect x="140" y="0" width="70" height="70" />
        <rect x="0" y="140" width="70" height="70" />
      </g>
      <g fill="#fff">
        <rect x="10" y="10" width="50" height="50" />
        <rect x="150" y="10" width="50" height="50" />
        <rect x="10" y="150" width="50" height="50" />
      </g>
      <g fill="#000">
        <rect x="20" y="20" width="30" height="30" />
        <rect x="160" y="20" width="30" height="30" />
        <rect x="20" y="160" width="30" height="30" />
      </g>

      <!-- 数据模块（仅用于视觉模拟，并非真实二维码编码） -->
      <g fill="#000">
        <template v-for="cell in cells" :key="cell.key">
          <rect
            v-if="cell.on"
            :x="cell.x"
            :y="cell.y"
            width="10"
            height="10"
          />
        </template>
      </g>
    </svg>
  </div>
</template>

<script lang="ts" setup>
import { computed } from 'vue';

const props = withDefaults(
  defineProps<{
    value: string;
    size?: number;
  }>(),
  {
    size: 168
  }
);

// 单个模块（module）的像素大小；整体为 21×21（类似二维码 Version 1 的网格尺寸）。
const MODULE = 10;
const GRID = 21;

const isInFinder = (cx: number, cy: number) => {
  const inTopLeft = cx < 7 && cy < 7;
  const inTopRight = cx >= GRID - 7 && cy < 7;
  const inBottomLeft = cx < 7 && cy >= GRID - 7;
  return inTopLeft || inTopRight || inBottomLeft;
};

// 轻量哈希：用于把 value 映射为“稳定的伪随机”图案（同一 value 渲染结果一致）。
const hash = (input: string) => {
  let value = 2166136261;
  for (let i = 0; i < input.length; i += 1) {
    value ^= input.charCodeAt(i);
    value = Math.imul(value, 16777619);
  }
  return value >>> 0;
};

const cells = computed(() => {
  // 这里生成的是“看起来像二维码”的占位图，不承诺可被扫码识别。
  const seed = hash(props.value || 'FLEXLEASE');
  const result: Array<{ key: string; x: number; y: number; on: boolean }> = [];

  for (let y = 0; y < GRID; y += 1) {
    for (let x = 0; x < GRID; x += 1) {
      if (isInFinder(x, y)) {
        continue;
      }

      const mixed = seed ^ (x * 0x45d9f3b) ^ (y * 0x119de1f3);
      const v = hash(String(mixed));
      const on = (v & 1) === 0;

      result.push({
        key: `${x}-${y}`,
        x: x * MODULE,
        y: y * MODULE,
        on
      });
    }
  }

  return result;
});
</script>

<style scoped>
.qr {
  display: inline-flex;
  padding: var(--space-2);
  border-radius: var(--radius-card);
  border: 1px solid var(--color-border);
  background: var(--color-surface);
}
</style>
