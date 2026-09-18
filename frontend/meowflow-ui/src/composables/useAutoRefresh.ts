import { onBeforeUnmount, onMounted, watch, type Ref } from 'vue';

export interface AutoRefreshOptions {
  /** 轮询周期,默认 30000ms */
  intervalMs?: number;
  /** 周期回调,可以是 Promise;串行执行中会跳过本次 */
  onTick: () => Promise<void> | void;
  /** 可选启停开关 */
  enabled?: Ref<boolean>;
  /** onTick 抛错时回调(默认静默) */
  onError?: (err: unknown) => void;
}

/**
 * 看板类页面通用定时刷新:
 * - 默认 30s 一次
 * - 页面隐藏时不执行 tick(浏览器标签切走)
 * - 切回前台时立即补一次再恢复节奏
 * - onTick 已在飞时跳过,避免堆积
 * - 组件卸载时彻底清理
 */
export function useAutoRefresh(options: AutoRefreshOptions) {
  const intervalMs = options.intervalMs ?? 30_000;
  const enabled = options.enabled;

  let timer: ReturnType<typeof setInterval> | null = null;
  let running = false;

  async function tick() {
    if (enabled && !enabled.value) return;
    if (document.hidden) return;
    if (running) return;
    running = true;
    try {
      await options.onTick();
    } catch (err) {
      options.onError?.(err);
    } finally {
      running = false;
    }
  }

  async function onVisibility() {
    if (document.hidden) return;
    // 切回前台:补一次,然后保证 setInterval 仍按时走
    if (!timer) startTimer();
    await tick();
  }

  function startTimer() {
    if (timer) return;
    timer = setInterval(tick, intervalMs);
  }

  function stopTimer() {
    if (timer) {
      clearInterval(timer);
      timer = null;
    }
  }

  onMounted(() => {
    document.addEventListener('visibilitychange', onVisibility);
    startTimer();
  });

  onBeforeUnmount(() => {
    document.removeEventListener('visibilitychange', onVisibility);
    stopTimer();
  });

  if (enabled) {
    watch(enabled, (v) => {
      if (v) startTimer();
      else stopTimer();
    });
  }

  return {
    /** 手动触发一次,等价于一次 tick */
    refresh: tick,
    /** 停止定时器但保留当前 tick 任务 */
    stop: stopTimer,
    /** 重新启动定时器 */
    resume: startTimer,
  };
}
