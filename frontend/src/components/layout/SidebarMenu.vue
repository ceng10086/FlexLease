<template>
  <a-menu
    :selectedKeys="selectedKeys"
    :items="menuItems"
    theme="dark"
    mode="inline"
    @click="onClick"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { MenuProps } from 'ant-design-vue';

export interface SidebarItem {
  key: string;
  label: string;
  path: string;
  icon?: () => JSX.Element;
}

const props = defineProps<{ items: SidebarItem[] }>();

const route = useRoute();
const router = useRouter();

const selectedKeys = computed(() => {
  const active = props.items.find((item) => route.path.startsWith(item.path));
  return active ? [active.key] : [];
});

const menuItems = computed<MenuProps['items']>(() =>
  props.items.map((item) => ({
    key: item.key,
    label: item.label,
    icon: item.icon
  }))
);

function onClick({ key }: { key: string }) {
  const target = props.items.find((item) => item.key === key);
  if (target) {
    router.push(target.path);
  }
}
</script>
