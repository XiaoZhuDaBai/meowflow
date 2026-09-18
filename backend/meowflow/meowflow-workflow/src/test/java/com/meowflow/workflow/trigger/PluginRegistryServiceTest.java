package com.meowflow.workflow.trigger;

import com.meowflow.common.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PluginRegistryServiceTest {

    private static final String PLUGIN_KEY = "plugin:registry:p1";
    private static final String INDEX_KEY = "plugin:registry:index";

    private RedisService redisService;
    private PluginRegistryService registryService;

    @BeforeEach
    void setUp() {
        redisService = mock(RedisService.class);
        registryService = new PluginRegistryService(redisService);
    }

    @Test
    void registerAddsManifestAndIndex() {
        doReturn(null).when(redisService).get(PLUGIN_KEY);
        doReturn(null).when(redisService).get(INDEX_KEY);

        Map<String, Object> saved = registryService.register(Map.of(
                "pluginId", "p1",
                "secret", "s1",
                "workflowId", 10));

        assertEquals("active", saved.get("status"));
        assertTrue(saved.containsKey("createdAt"));
        assertTrue(saved.containsKey("updatedAt"));
        verify(redisService).set(eq(PLUGIN_KEY), anyMap());
        verify(redisService).set(eq(INDEX_KEY), eq(List.of("p1")));
    }

    @Test
    void updateStatusPersistsOnlyKnownStatus() {
        Map<String, Object> current = new java.util.HashMap<>(Map.of(
                "pluginId", "p1",
                "status", "active"));
        doReturn(current).when(redisService).get(PLUGIN_KEY);

        Map<String, Object> updated = registryService.updateStatus("p1", "disabled");

        assertEquals("disabled", updated.get("status"));
        verify(redisService).set(eq(PLUGIN_KEY), anyMap());
    }

    @Test
    void deleteRemovesManifestAndIndexEntry() {
        doReturn(true).when(redisService).delete(PLUGIN_KEY);
        doReturn(new java.util.ArrayList<>(List.of("p1", "p2"))).when(redisService).get(INDEX_KEY);

        assertTrue(registryService.delete("p1"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(redisService).set(eq(INDEX_KEY), captor.capture());
        assertEquals(List.of("p2"), captor.getValue());
    }

    @Test
    void listReturnsIndexedManifests() {
        doReturn(new java.util.ArrayList<>(List.of("p1", "missing"))).when(redisService).get(INDEX_KEY);
        doReturn(Map.of("pluginId", "p1")).when(redisService).get(PLUGIN_KEY);
        doReturn(null).when(redisService).get("plugin:registry:missing");

        List<Map<String, Object>> result = registryService.list();

        assertEquals(1, result.size());
        assertEquals("p1", result.get(0).get("pluginId"));
    }

    @Test
    void isValidStatusRejectsUnknownValues() {
        assertTrue(PluginRegistryService.isValidStatus("active"));
        assertTrue(PluginRegistryService.isValidStatus("INACTIVE"));
        assertFalse(PluginRegistryService.isValidStatus("unknown"));
    }
}
