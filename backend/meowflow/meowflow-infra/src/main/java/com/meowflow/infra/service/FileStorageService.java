package com.meowflow.infra.service;

import com.meowflow.infra.config.InfraProperties;
import io.minio.*;
import io.minio.http.Method;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件存储服务
 * 支持本地文件系统和 MinIO 对象存储
 */
@Service
@Slf4j
public class FileStorageService {

    private final InfraProperties infraProperties;
    private MinioClient minioClient;

    public FileStorageService(InfraProperties infraProperties) {
        this.infraProperties = infraProperties;
    }

    @PostConstruct
    public void init() {
        if ("minio".equalsIgnoreCase(infraProperties.getStorage().getType())) {
            initMinioClient();
        } else {
            initLocalStorage();
        }
    }

    /**
     * 初始化 MinIO 客户端
     */
    private void initMinioClient() {
        try {
            InfraProperties.Storage.Minio minioConfig = infraProperties.getStorage().getMinio();
            minioClient = MinioClient.builder()
                    .endpoint(minioConfig.getEndpoint())
                    .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                    .build();

            // 自动创建 bucket
            if (minioConfig.isAutoCreateBucket()) {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(minioConfig.getBucket())
                                .build());
                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder()
                                    .bucket(minioConfig.getBucket())
                                    .build());
                    log.info("MinIO bucket created: {}", minioConfig.getBucket());
                }
            }

            log.info("MinIO client initialized: endpoint={}, bucket={}",
                    minioConfig.getEndpoint(), minioConfig.getBucket());
        } catch (Exception e) {
            log.error("Failed to initialize MinIO client", e);
            throw new RuntimeException("MinIO 客户端初始化失败: " + e.getMessage(), e);
        }
    }

    /**
     * 初始化本地存储目录
     */
    private void initLocalStorage() {
        try {
            Path dir = Paths.get(infraProperties.getStorage().getLocalDir());
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                log.info("Local storage directory created: {}", dir.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to create local storage directory", e);
            throw new RuntimeException("本地存储目录创建失败: " + e.getMessage(), e);
        }
    }

    /**
     * 存储文件
     * 
     * @param file MultipartFile
     * @return 文件元数据
     * @throws Exception 存储失败
     */
    public FileMetadata store(MultipartFile file) throws Exception {
        String originalName = file.getOriginalFilename();
        String objectName = generateObjectName(originalName);
        long fileSize = file.getSize();

        if ("minio".equalsIgnoreCase(infraProperties.getStorage().getType())) {
            return storeToMinio(objectName, file, fileSize);
        } else {
            return storeToLocal(objectName, file, fileSize);
        }
    }

    /**
     * 存储到 MinIO
     */
    private FileMetadata storeToMinio(String objectName, MultipartFile file, long fileSize) throws Exception {
        String bucket = infraProperties.getStorage().getMinio().getBucket();
        
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(inputStream, fileSize, -1)
                            .contentType(file.getContentType())
                            .build());

            log.info("File stored to MinIO: bucket={}, object={}, size={}", bucket, objectName, fileSize);

            FileMetadata metadata = new FileMetadata();
            metadata.setFilePath(objectName);
            metadata.setFileSize(fileSize);
            metadata.setStorageType("minio");
            return metadata;
        }
    }

    /**
     * 存储到本地文件系统
     */
    private FileMetadata storeToLocal(String objectName, MultipartFile file, long fileSize) throws Exception {
        Path targetPath = Paths.get(infraProperties.getStorage().getLocalDir()).resolve(objectName);
        
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File stored locally: path={}, size={}", targetPath.toAbsolutePath(), fileSize);

            FileMetadata metadata = new FileMetadata();
            metadata.setFilePath(objectName);
            metadata.setFileSize(fileSize);
            metadata.setStorageType("local");
            return metadata;
        }
    }

    /**
     * 生成预签名下载链接
     * 
     * @param filePath 文件路径
     * @return 下载 URL
     * @throws Exception 生成失败
     */
    public String getDownloadUrl(String filePath) throws Exception {
        if ("minio".equalsIgnoreCase(infraProperties.getStorage().getType())) {
            String bucket = infraProperties.getStorage().getMinio().getBucket();
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(filePath)
                            .expiry(1, TimeUnit.HOURS)
                            .build());
        } else {
            // 本地文件返回相对路径，由 Controller 处理流式下载
            return "/api/infra/knowledge/documents/download/" + filePath;
        }
    }

    /**
     * 获取文件流（用于本地存储）
     * 
     * @param filePath 文件路径
     * @return 输入流
     * @throws Exception 读取失败
     */
    public InputStream getFileStream(String filePath) throws Exception {
        if ("minio".equalsIgnoreCase(infraProperties.getStorage().getType())) {
            String bucket = infraProperties.getStorage().getMinio().getBucket();
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(filePath)
                            .build());
        } else {
            Path path = Paths.get(infraProperties.getStorage().getLocalDir()).resolve(filePath);
            return Files.newInputStream(path);
        }
    }

    /**
     * 删除文件
     * 
     * @param filePath 文件路径
     * @throws Exception 删除失败
     */
    public void delete(String filePath) throws Exception {
        if (filePath == null || filePath.isBlank()) {
            return;
        }

        if ("minio".equalsIgnoreCase(infraProperties.getStorage().getType())) {
            String bucket = infraProperties.getStorage().getMinio().getBucket();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(filePath)
                            .build());
            log.info("File deleted from MinIO: bucket={}, object={}", bucket, filePath);
        } else {
            Path path = Paths.get(infraProperties.getStorage().getLocalDir()).resolve(filePath);
            Files.deleteIfExists(path);
            log.info("File deleted locally: path={}", path.toAbsolutePath());
        }
    }

    /**
     * 生成对象名称（UUID + 原扩展名）
     */
    private String generateObjectName(String originalName) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String extension = extractExtension(originalName);
        return uuid + extension;
    }

    /**
     * 提取文件扩展名
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    /**
     * 文件元数据
     */
    @Data
    public static class FileMetadata {
        /**
         * 文件路径（对象名称）
         */
        private String filePath;

        /**
         * 文件大小（字节）
         */
        private long fileSize;

        /**
         * 存储类型: local | minio
         */
        private String storageType;
    }
}
