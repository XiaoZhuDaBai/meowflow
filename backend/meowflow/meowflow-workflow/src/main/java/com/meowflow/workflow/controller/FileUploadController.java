package com.meowflow.workflow.controller;

import com.meowflow.common.result.Result;
import com.meowflow.workflow.config.WorkflowProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileUploadController {

    private final WorkflowProperties workflowProperties;
    private MinioClient minioClient;

    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error(400, "文件不能为空");
        }
        if (file.getSize() > workflowProperties.getFile().getMaxSize()) {
            return Result.error(413, "文件超过大小限制");
        }

        try {
            String extension = extensionOf(file.getOriginalFilename());
            String id = UUID.randomUUID().toString().replace("-", "");
            String storedName = id + (extension.isBlank() ? "" : "." + extension);

            Map<String, Object> reference = new LinkedHashMap<>();
            reference.put("id", id);
            reference.put("name", file.getOriginalFilename());
            reference.put("contentType", file.getContentType());
            reference.put("size", file.getSize());
            if ("minio".equalsIgnoreCase(workflowProperties.getFile().getStorageType())) {
                storeToMinio(storedName, file, reference);
            } else {
                Path dir = Path.of(workflowProperties.getFile().getStorageDir()).toAbsolutePath().normalize();
                Files.createDirectories(dir);
                Path target = dir.resolve(storedName);
                file.transferTo(target.toFile());
                reference.put("path", target.toString());
                reference.put("storage", "local");
            }
            return Result.success(reference);
        } catch (Exception e) {
            return Result.error(500, "文件保存失败: " + e.getMessage());
        }
    }

    private void storeToMinio(String objectName, MultipartFile file, Map<String, Object> reference) throws Exception {
        WorkflowProperties.File cfg = workflowProperties.getFile();
        if (minioClient == null) {
            minioClient = MinioClient.builder()
                    .endpoint(cfg.getMinioEndpoint())
                    .credentials(cfg.getMinioAccessKey(), cfg.getMinioSecretKey())
                    .build();
        }
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(cfg.getMinioBucket()).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(cfg.getMinioBucket()).build());
        }
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(cfg.getMinioBucket())
                .object(objectName)
                .stream(file.getInputStream(), file.getSize(), -1)
                .contentType(file.getContentType())
                .build());
        reference.put("path", cfg.getMinioBucket() + "/" + objectName);
        reference.put("url", cfg.getMinioEndpoint() + "/" + cfg.getMinioBucket() + "/" + objectName);
        reference.put("storage", "minio");
    }

    private String extensionOf(String name) {
        if (name == null || !name.contains(".")) return "";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }
}
