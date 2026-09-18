import { defineStore } from 'pinia';
import { ref } from 'vue';
import { captchaApi, type CaptchaVO } from '@/api/captcha';

export const useCaptchaStore = defineStore('captcha', () => {
  const current = ref<CaptchaVO | null>(null);
  const loading = ref(false);

  async function refresh() {
    loading.value = true;
    try {
      current.value = await captchaApi.generate();
    } finally {
      loading.value = false;
    }
  }

  function getUuid() {
    return current.value?.uuid ?? '';
  }

  function getImg() {
    return current.value?.img ?? '';
  }

  return { current, loading, refresh, getUuid, getImg };
});
