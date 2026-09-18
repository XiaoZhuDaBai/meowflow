package com.meowflow.workflow.controller;

import com.meowflow.common.result.Result;
import com.meowflow.workflow.config.WorkflowProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileUploadControllerTest {

    @Test
    void uploadStoresFileAndReturnsReference() throws Exception {
        Path dir = Files.createTempDirectory("meowflow-files");
        WorkflowProperties properties = new WorkflowProperties();
        properties.getFile().setStorageDir(dir.toString());
        FileUploadController controller = new FileUploadController(properties);

        MockMultipartFile file = new MockMultipartFile(
                "file", "hello.txt", "text/plain", "hello".getBytes());
        Result<Map<String, Object>> result = controller.upload(file);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsEntry("name", "hello.txt");
        assertThat(Files.exists(Path.of(result.getData().get("path").toString()))).isTrue();
    }
}
