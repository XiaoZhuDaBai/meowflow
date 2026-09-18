<template>
  <div class="cron-config">
    <el-form label-width="100px" size="small">
      <el-form-item label="Cron 表达式">
        <CronEditor v-model="cronExpr" :timezone="timezone" />
      </el-form-item>

      <el-form-item label="时区">
        <el-select v-model="timezone">
          <el-option value="Asia/Shanghai" label="中国标准时间 (UTC+8)" />
          <el-option value="Asia/Tokyo" label="东京时间 (UTC+9)" />
          <el-option value="Asia/Singapore" label="新加坡时间 (UTC+8)" />
          <el-option value="UTC" label="UTC" />
          <el-option value="America/New_York" label="纽约时间 (UTC-5)" />
          <el-option value="America/Los_Angeles" label="洛杉矶时间 (UTC-8)" />
        </el-select>
      </el-form-item>

      <el-form-item label="启用">
        <el-switch v-model="enabled" />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import CronEditor from '@/components/common/CronEditor.vue';

const props = defineProps<{
  modelValue: Record<string, any>;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>): void;
}>();

const cronExpr = computed({
  get: () => props.modelValue?.cron ?? '0 0 * * *',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, cron: v }),
});

const timezone = computed({
  get: () => props.modelValue?.timezone ?? 'Asia/Shanghai',
  set: (v: string) => emit('update:modelValue', { ...props.modelValue, timezone: v }),
});

const enabled = computed({
  get: () => props.modelValue?.enabled ?? true,
  set: (v: boolean) => emit('update:modelValue', { ...props.modelValue, enabled: v }),
});
</script>
