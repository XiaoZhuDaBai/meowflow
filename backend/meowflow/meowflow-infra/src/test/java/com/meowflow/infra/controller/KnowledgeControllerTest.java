package com.meowflow.infra.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meowflow.common.exception.GlobalExceptionHandler;
import com.meowflow.common.test.SaTokenMockHelper;
import com.meowflow.infra.knowledge.DocumentParser;
import com.meowflow.infra.persistence.entity.DocumentEntity;
import com.meowflow.infra.persistence.entity.KnowledgeBaseEntity;
import com.meowflow.infra.service.FileStorageService;
import com.meowflow.infra.service.KnowledgeService;
import com.meowflow.infra.service.SearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = KnowledgeController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("KnowledgeController HTTP 层")
class KnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private KnowledgeService knowledgeService;
    @MockBean
    private DocumentParser documentParser;
    @MockBean
    private SearchService searchService;

    @MockBean
    private FileStorageService fileStorageService;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void loginAdmin() { SaTokenMockHelper.loginAsAdmin(); }
    @AfterEach
    void logout() { SaTokenMockHelper.clear(); }

    @Test
    @DisplayName("POST /knowledge/bases")
    void createKnowledgeBase_invokesService() throws Exception {
        when(knowledgeService.createKnowledgeBase(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(new KnowledgeBaseEntity());

        KnowledgeController.CreateKnowledgeBaseRequest req = new KnowledgeController.CreateKnowledgeBaseRequest();
        req.setName("kb-1");
        req.setDimension(768);
        mockMvc.perform(post("/api/infra/knowledge/bases")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /knowledge/bases/{id}")
    void getKnowledgeBase_invokesService() throws Exception {
        when(knowledgeService.getKnowledgeBase(anyLong())).thenReturn(new KnowledgeBaseEntity());

        mockMvc.perform(get("/api/infra/knowledge/bases/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /knowledge/bases")
    void listKnowledgeBases_invokesService() throws Exception {
        when(knowledgeService.listKnowledgeBases()).thenReturn(List.of());

        mockMvc.perform(get("/api/infra/knowledge/bases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("DELETE /knowledge/bases/{id}")
    void deleteKnowledgeBase_invokesService() throws Exception {
        mockMvc.perform(delete("/api/infra/knowledge/bases/1"))
                .andExpect(status().isOk());
        verify(knowledgeService).deleteKnowledgeBase(1L);
    }

    @Test
    @DisplayName("POST /knowledge/documents")
    void uploadDocument_invokesService() throws Exception {
        when(knowledgeService.uploadDocument(anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(new DocumentEntity());

        KnowledgeController.UploadDocumentRequest req = new KnowledgeController.UploadDocumentRequest();
        req.setKnowledgeBaseId(1L);
        req.setTitle("t");
        req.setContent("c");
        req.setContentType("text/plain");
        mockMvc.perform(post("/api/infra/knowledge/documents")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /knowledge/documents")
    void listDocuments_invokesService() throws Exception {
        when(knowledgeService.listDocuments(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/infra/knowledge/documents").param("knowledgeBaseId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /knowledge/documents/{id}")
    void getDocument_invokesService() throws Exception {
        when(knowledgeService.getDocument(anyLong())).thenReturn(new DocumentEntity());

        mockMvc.perform(get("/api/infra/knowledge/documents/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /knowledge/search")
    void search_invokesService() throws Exception {
        when(searchService.search(anyString(), anyLong(), anyInt()))
                .thenReturn(List.of());

        KnowledgeController.SearchRequest req = new KnowledgeController.SearchRequest();
        req.setKnowledgeBaseId(1L);
        req.setQuery("foo");
        req.setTopK(5);

        mockMvc.perform(post("/api/infra/knowledge/search")
                        .contentType("application/json")
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}



