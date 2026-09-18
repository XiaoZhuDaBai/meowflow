/**
 * vitest setup file — runs before each test file
 * Mocks element-plus CSS imports to avoid "Unknown file extension .css" errors
 */

import { vi } from 'vitest';

// Mock element-plus CSS imports globally
vi.mock('element-plus/es/components/message/style/css.mjs', () => ({}));
vi.mock('element-plus/es/components/message-box/style/css.mjs', () => ({}));
vi.mock('element-plus/theme-chalk/el-message.css', () => ({}));
vi.mock('element-plus/theme-chalk/el-message-box.css', () => ({}));
