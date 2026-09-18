import { defineStore } from 'pinia';
import { ref, computed, watch } from 'vue';

/**
 * Canvas store - 管理画布视图状态（视口、网格、缩放等）
 * 持久化到 localStorage
 */
const CANVAS_PREFS_KEY = 'meowflow.canvas.prefs';

interface CanvasPrefs {
  minimapVisible: boolean;
  gridVisible: boolean;
  snapToGrid: boolean;
  fitViewOnLoad: boolean;
  zoomLevel: number;
}

const defaultPrefs: CanvasPrefs = {
  minimapVisible: true,
  gridVisible: true,
  snapToGrid: false,
  fitViewOnLoad: true,
  zoomLevel: 1,
};

function loadPrefs(): CanvasPrefs {
  try {
    const raw = localStorage.getItem(CANVAS_PREFS_KEY);
    if (!raw) return { ...defaultPrefs };
    return { ...defaultPrefs, ...JSON.parse(raw) };
  } catch {
    return { ...defaultPrefs };
  }
}

function savePrefs(prefs: CanvasPrefs) {
  try {
    localStorage.setItem(CANVAS_PREFS_KEY, JSON.stringify(prefs));
  } catch {
    /* ignore */
  }
}

export const useCanvasStore = defineStore('canvas', () => {
  const initial = loadPrefs();

  const viewport = ref({ x: 0, y: 0, zoom: initial.zoomLevel });
  const minimapVisible = ref(initial.minimapVisible);
  const gridVisible = ref(initial.gridVisible);
  const snapToGrid = ref(initial.snapToGrid);
  const fitViewOnLoad = ref(initial.fitViewOnLoad);
  const nodePaletteVisible = ref(true);
  const configPanelVisible = ref(true);
  const helpLinesVisible = ref(true);

  const isPanning = ref(false);

  function setViewport(v: { x: number; y: number; zoom: number }) {
    viewport.value = v;
  }

  function toggleMinimap() {
    minimapVisible.value = !minimapVisible.value;
  }

  function toggleGrid() {
    gridVisible.value = !gridVisible.value;
  }

  function toggleSnapToGrid() {
    snapToGrid.value = !snapToGrid.value;
  }

  function toggleNodePalette() {
    nodePaletteVisible.value = !nodePaletteVisible.value;
  }

  function toggleConfigPanel() {
    configPanelVisible.value = !configPanelVisible.value;
  }

  function toggleHelpLines() {
    helpLinesVisible.value = !helpLinesVisible.value;
  }

  function setPanning(panning: boolean) {
    isPanning.value = panning;
  }

  // 持久化 prefs
  watch(
    [minimapVisible, gridVisible, snapToGrid, fitViewOnLoad],
    ([minimap, grid, snap, fitView]) => {
      savePrefs({
        minimapVisible: minimap,
        gridVisible: grid,
        snapToGrid: snap,
        fitViewOnLoad: fitView,
        zoomLevel: viewport.value.zoom,
      });
    },
  );

  return {
    viewport,
    minimapVisible,
    gridVisible,
    snapToGrid,
    fitViewOnLoad,
    nodePaletteVisible,
    configPanelVisible,
    helpLinesVisible,
    isPanning,
    setViewport,
    toggleMinimap,
    toggleGrid,
    toggleSnapToGrid,
    toggleNodePalette,
    toggleConfigPanel,
    toggleHelpLines,
    setPanning,
  };
});
