package com.meowflow.workflow.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DifyWorkflowImporterTest {

    @Test
    void importsBasicDifyDsl() {
        String json = """
                {
                  "app": {"name": "Dify Demo", "description": "test"},
                  "workflow": {
                    "environment_variables": [],
                    "graph": {
                      "nodes": [
                        {"id": "n1", "type": "start", "title": "Start", "position": {"x": 0, "y": 0}},
                        {"id": "n2", "type": "llm", "title": "LLM", "position": {"x": 280, "y": 0}, "data": {"model": "gpt-4o"}},
                        {"id": "n3", "type": "end", "title": "End", "position": {"x": 560, "y": 0}}
                      ],
                      "edges": [
                        {"id": "e1", "source": "n1", "target": "n2"},
                        {"id": "e2", "source": "n2", "target": "n3"}
                      ]
                    }
                  }
                }
                """;

        DifyWorkflowImporter.ImportedDify result = DifyWorkflowImporter.tryImport(json);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Dify Demo");
        assertThat(result.definition().getNodes()).hasSize(3);
        assertThat(result.definition().getNodes().get(0).getType()).isEqualTo("trigger.manual");
        assertThat(result.definition().getNodes().get(1).getType()).isEqualTo("ai.llm");
        assertThat(result.definition().getEdges()).hasSize(2);
    }
}
