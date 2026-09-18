<template>
  <router-view />
</template>

<script setup lang="ts">
import { useAppStore } from '@/stores/app';
import { storeToRefs } from 'pinia';
const appStore = useAppStore();
const { useMock } = storeToRefs(appStore);
void useMock;
</script>