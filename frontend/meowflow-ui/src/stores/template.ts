/**
 * 模板市场状态（轻量级）
 *
 * 仅缓存列表/详情与搜索参数，避免组件重复请求。
 * 业务行为（搜索/分类/标签/使用）仍在视图层组织，便于理解与单测。
 */

import { defineStore } from 'pinia';
import { ref } from 'vue';
import {
  templateApi,
  type TemplateSearchQuery,
  type BackendTemplateCategory,
  type BackendTemplateTag,
} from '@/api/template';
import type { PaginatedResponse } from '@/types/workflow';
import type { Template } from '@/api/types';

export const useTemplateStore = defineStore('template', () => {
  // ---------- state ----------
  const items = ref<Template[]>([]);
  const total = ref(0);
  const page = ref(1);
  const pageSize = ref(20);
  const totalPages = ref(0);

  const current = ref<Template | null>(null);

  const categories = ref<BackendTemplateCategory[]>([]);
  const tags = ref<BackendTemplateTag[]>([]);

  const loading = ref(false);
  const error = ref<string | null>(null);

  /** 最近一次的查询参数（用于翻页/重排序） */
  const lastQuery = ref<TemplateSearchQuery>({});

  // ---------- getters ----------
  const availableTagNames = ref<string[]>([]);

  // ---------- actions ----------
  async function search(query: TemplateSearchQuery = {}): Promise<PaginatedResponse<Template>> {
    loading.value = true;
    error.value = null;
    lastQuery.value = { ...query };
    try {
      const res = await templateApi.search(query);
      items.value = res.items;
      total.value = res.total;
      page.value = res.page;
      pageSize.value = res.pageSize;
      totalPages.value = res.totalPages;
      return res;
    } catch (e: any) {
      error.value = e?.message || '加载模板失败';
      items.value = [];
      total.value = 0;
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function fetchById(id: string): Promise<Template | null> {
    loading.value = true;
    error.value = null;
    try {
      const t = await templateApi.getById(id);
      current.value = t;
      return t;
    } catch (e: any) {
      error.value = e?.message || '加载模板详情失败';
      current.value = null;
      throw e;
    } finally {
      loading.value = false;
    }
  }

  async function use(id: string, name?: string) {
    const res = await templateApi.use(id, name);
    // 刷新 current（如已加载）
    if (current.value?.id === id && res?.template) {
      current.value = res.template;
    }
    return res;
  }

  async function loadCategories() {
    try {
      categories.value = await templateApi.getCategories();
    } catch {
      categories.value = [];
    }
  }

  async function loadTags() {
    try {
      const list = await templateApi.getTags();
      tags.value = list;
      availableTagNames.value = (list || []).map((t) => t.name).filter(Boolean);
    } catch {
      tags.value = [];
      availableTagNames.value = [];
    }
  }

  function reset() {
    items.value = [];
    total.value = 0;
    page.value = 1;
    pageSize.value = 20;
    totalPages.value = 0;
    current.value = null;
    error.value = null;
  }

  return {
    items,
    total,
    page,
    pageSize,
    totalPages,
    current,
    categories,
    tags,
    availableTagNames,
    loading,
    error,
    lastQuery,
    search,
    fetchById,
    use,
    loadCategories,
    loadTags,
    reset,
  };
});