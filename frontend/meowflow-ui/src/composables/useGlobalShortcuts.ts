/**
 * 全局快捷键控制器 (GlobalShortcuts)
 *
 * - Ctrl/Cmd + K: 打开节点命令面板
 * - Ctrl/Cmd + D: 切换调试模式
 * - Ctrl/Cmd + Shift + P: 打开命令面板（同 VSCode）
 * - Ctrl/Cmd + Enter: 运行工作流
 * - Ctrl/Cmd + S: 保存工作流
 */
import { onMounted, onBeforeUnmount } from 'vue';

interface ShortcutHandlers {
  onOpenCommandPalette?: () => void;
  onToggleDebug?: () => void;
  onRun?: () => void;
  onSave?: () => void;
}

export function useGlobalShortcuts(handlers: ShortcutHandlers) {
  function isEditableTarget(target: EventTarget | null): boolean {
    if (!target || !(target instanceof HTMLElement)) return false;
    const tag = target.tagName.toLowerCase();
    return (
      tag === 'input'
      || tag === 'textarea'
      || tag === 'select'
      || target.isContentEditable
    );
  }

  function handleKeydown(e: KeyboardEvent) {
    if (isEditableTarget(e.target)) return;

    const isMac = navigator.platform.toUpperCase().includes('MAC');
    const modKey = isMac ? e.metaKey : e.ctrlKey;

    if (!modKey) return;

    // Ctrl+K or Ctrl+Shift+P: 命令面板
    if (e.key.toLowerCase() === 'k' || (e.shiftKey && e.key.toLowerCase() === 'p')) {
      e.preventDefault();
      handlers.onOpenCommandPalette?.();
      return;
    }

    // Ctrl+D: 切换调试模式
    if (e.key.toLowerCase() === 'd' && !e.shiftKey) {
      e.preventDefault();
      handlers.onToggleDebug?.();
      return;
    }

    // Ctrl+Enter: 运行
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handlers.onRun?.();
      return;
    }

    // Ctrl+S: 保存
    if (e.key.toLowerCase() === 's') {
      e.preventDefault();
      handlers.onSave?.();
      return;
    }
  }

  onMounted(() => {
    window.addEventListener('keydown', handleKeydown);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydown);
  });
}
