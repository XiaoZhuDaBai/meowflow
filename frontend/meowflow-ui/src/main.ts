import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import { useUserStore } from '@/stores/user';
import { setupPermissionDirective } from '@/directives/permission';
import '@/assets/styles/global.css';

const app = createApp(App);
app.use(createPinia());
app.use(router);
setupPermissionDirective(app);

router.isReady().then(() => {
  const userStore = useUserStore();
  userStore.bootstrap();
  app.mount('#app');
});