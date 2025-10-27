<template>
  <a-tag :color="color" class="role-badge">{{ label }}</a-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import type { UserRole } from '@/types';

const props = defineProps<{ role: UserRole | null }>();

const color = computed(() => {
  switch (props.role) {
    case 'CUSTOMER':
      return 'blue';
    case 'VENDOR':
      return 'purple';
    case 'ADMIN':
      return 'gold';
    default:
      return 'default';
  }
});

const label = computed(() => {
  switch (props.role) {
    case 'CUSTOMER':
      return 'C 端用户';
    case 'VENDOR':
      return 'B 端厂商';
    case 'ADMIN':
      return '平台管理员';
    default:
      return '未登录';
  }
});
</script>

<style scoped>
.role-badge {
  margin-left: 8px;
  font-weight: 500;
}
</style>
