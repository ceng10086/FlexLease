<template>
  <a-empty :description="description" class="empty-state">
    <template #image>
      <img class="empty-illustration" src="https://img.alicdn.com/imgextra/i3/O1CN01g6QWgT1uZV7aX6czS_!!6000000006037-2-tps-480-480.png" alt="empty" />
    </template>
    <template v-if="actionLabel" #extra>
      <a-button type="primary" @click="$emit('action')">{{ actionLabel }}</a-button>
    </template>
  </a-empty>
</template>

<script setup lang="ts">
defineProps<{
  description?: string;
  actionLabel?: string;
}>();

defineEmits<{ (e: 'action'): void }>();
</script>

<style scoped>
.empty-state {
  padding: 48px 0;
}

.empty-illustration {
  width: 120px;
  height: 120px;
  object-fit: contain;
}
</style>
