<template>
  <div class="empty-state">
    <i :class="icon" />
    <div class="title">{{ title }}</div>
    <div class="desc" v-if="description">{{ description }}</div>
    <slot />
  </div>
</template>

<script setup lang="ts">
defineProps<{ icon?: string; title: string; description?: string }>();
</script>

<style scoped>
.title { font-size: 14px; color: var(--text-secondary); margin-bottom: 4px; }
.desc { font-size: 12px; color: var(--text-tertiary); max-width: 360px; }
</style>