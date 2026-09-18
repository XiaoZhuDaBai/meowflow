package com.meowflow.workflow.executor.document;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DocumentExtractorExecutorTest {

    @Test
    void extractsTxtFromLocalPath() throws Exception {
        var file = Files.createTempFile("meowflow-doc", ".txt");
        Files.writeString(file, "hello workflow");

        DocumentExtractorExecutor executor = new DocumentExtractorExecutor(mock(RestTemplate.class));
        NodeDefinition node = NodeDefinition.builder()
                .id("n1")
                .name("Doc")
                .type(NodeType.DOCUMENT_EXTRACTOR)
                .data(Map.of("fileUrl", file.toString(), "fileType", "txt"))
                .build();

        NodeResult result = executor.execute(ExecutionContext.builder().build(), node);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getOutput()).containsEntry("text", "hello workflow");
        Files.deleteIfExists(file);
    }
}
