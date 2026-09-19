package com.meowflow.infra.service;

import com.meowflow.infra.config.InfraProperties;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件存储服务 - 支持本地文件系统和 MinIO 对象存储
 */
@Slf4j
@Service
public class FileStorageService {
    
    private final InfraProperties infraProperties;
    private MinioClient minioClient;
    
    public FileStorageService(InfraProperties infraProperties) {
        this.infraProperties = infraProperties;
    }
    
    @PostConstruct
    public void init() {
        String storageType = infraProperties.getStorage().getType();
        
        if ("minio".equalsIgnoreCase(storageType)) {
            initMinioClient();
        } else if ("local".equalsIgnoreCase(storageType)) {
            initLocalStorage();
        } else {
            log.warn("Unknown storage type: {}, falling back to local", storageType);
            initLocalStorage();
        }
    }
    
    /**
     * 初始化 MinIO 客户端
     */
    private void initMinioClient() {
        try {
            InfraProperties.MinioConfig minioConfig = infraProperties.getStorage().getMinio();
            
            minioClient = MinioClient.builder()
                    .endpoint(minioConfig.getEndpoint())
                    .credentials(minioConfig.getAccessKey(), minioConfig.getSecretKey())
                    .build();
            
            // 检查 Bucket 是否存在，不存在则创建
            if (minioConfig.isAutoCreateBucket()) {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(minioConfig.getBucket())
                                .build()
                );
                
                if (!exists) {
                    minioClient.makeBucket(
                            MakeBucketArgs.builder()
                                    .bucket(minioConfig.getBucket())
                                    .build()
                    );
                    log.info("Created MinIO bucket: {}", minioConfig.getBucket());
                }
            }
            
            log.info("MinIO client initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize MinIO client", e);
            throw new RuntimeException("MinIO initialization failed", e);
        }
    }
    
    /**
     * 初始化本地存储目录
     */
    private void initLocalStorage() {
        try {
            String localDir = infraProperties.getStorage().getLocalDir();
            Path storagePath = Paths.get(localDir);
            
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                log.info("Created local storage directory: {}", localDir);
            }
            
            log.info("Local storage initialized at: {}", storagePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to initialize local storage", e);
            throw new RuntimeException("Local storage initialization failed", e);
        }
    }
    
    /**
     * 保存文件
     * 
     * @param file 上传的文件
     * @param originalFilename 原始文件名
     * @return 文件存储路径（相对路径或对象名）
     */
    public String saveFile(MultipartFile file, String originalFilename) throws IOException {
        String storageType = infraProperties.getStorage().getType();
        
        if ("minio".equalsIgnoreCase(storageType)) {
            return saveToMinio(file, originalFilename);
        } else {
            return saveToLocal(file, originalFilename);
        }
    }

    /**
     * 归档上传文件并返回其元数据。
     *
     * <p>KnowledgeController 使用该入口：先归档原文件，再用 Tika 解析内容。
     *
     * @param file 上传的文件
     * @return 归档路径与文件大小
     */
    public FileMetadata store(MultipartFile file) throws IOException {
        String path = saveFile(file, file.getOriginalFilename());
        return new FileMetadata(path, file.getSize());
    }

    /**
     * 删除已归档的文件。与 {@link #deleteFile(String)} 等价，
     * 保留该方法名是因为 KnowledgeService 删除文档时按此签名调用。
     *
     * @param filePath {@link #store(MultipartFile)} 返回的归档路径
     */
    public void delete(String filePath) {
        deleteFile(filePath);
    }

    /**
     * 归档文件的元数据。
     *
     * <p>用 getter 而非 record：调用方（KnowledgeController）按 JavaBean 方式取值。
     */
    public static class FileMetadata {
        private String filePath;
        private long fileSize;

        public FileMetadata() {
        }

        public FileMetadata(String filePath, long fileSize) {
            this.filePath = filePath;
            this.fileSize = fileSize;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }

        public long getFileSize() {
            return fileSize;
        }

        public void setFileSize(long fileSize) {
            this.fileSize = fileSize;
        }
    }
    
    /**
     * 保存文件到 MinIO
     */
    private String saveToMinio(MultipartFile file, String originalFilename) throws IOException {
        try {
            String objectName = generateObjectName(originalFilename);
            String bucket = infraProperties.getStorage().getMinio().getBucket();
            
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            
            log.info("File saved to MinIO: /{}", bucket, objectName);
            return objectName;
        } catch (Exception e) {
            log.error("Failed to save file to MinIO", e);
            throw new IOException("Failed to save file to MinIO", e);
        }
    }
    
    /**
     * 保存文件到本地
     */
    private String saveToLocal(MultipartFile file, String originalFilename) throws IOException {
        String fileName = generateObjectName(originalFilename);
        String localDir = infraProperties.getStorage().getLocalDir();
        Path filePath = Paths.get(localDir, fileName);
        
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to local: {}", filePath);
            return fileName;
        }
    }
    
    /**
     * 生成唯一的文件名（保留原扩展名）
     */
    private String generateObjectName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }
    
    /**
     * 获取文件下载 URL
     * 
     * @param filePath 文件路径
     * @return 下载 URL（MinIO 返回预签名 URL，本地返回相对路径）
     */
    public String getDownloadUrl(String filePath) {
        String storageType = infraProperties.getStorage().getType();
        
        if ("minio".equalsIgnoreCase(storageType)) {
            return getMinioDownloadUrl(filePath);
        } else {
            return filePath; // 本地模式返回文件名，由 Controller 处理实际下载
        }
    }
    
    /**
     * 获取 MinIO 预签名下载 URL（有效期 7 天）
     */
    private String getMinioDownloadUrl(String objectName) {
        try {
            String bucket = infraProperties.getStorage().getMinio().getBucket();
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate MinIO download URL", e);
            throw new RuntimeException("Failed to generate download URL", e);
        }
    }
    
    /**
     * 获取本地文件的完整路径
     */
    public Path getLocalFilePath(String fileName) {
        String localDir = infraProperties.getStorage().getLocalDir();
        return Paths.get(localDir, fileName);
    }

    /**
     * 打开归档文件的读取流（仅本地存储模式）。
     *
     * <p>本地模式没有可重定向的 URL，需要由 Controller 直接回传文件内容。
     *
     * @param fileName {@link #store(MultipartFile)} 返回的归档文件名
     */
    public InputStream openLocalStream(String fileName) throws IOException {
        Path path = getLocalFilePath(fileName);
        if (!Files.exists(path)) {
            throw new IOException("归档文件不存在: " + path);
        }
        return Files.newInputStream(path);
    }
    
    /**
     * 删除文件
     * 
     * @param filePath 文件路径
     */
    public void deleteFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            return;
        }
        
        String storageType = infraProperties.getStorage().getType();
        
        try {
            if ("minio".equalsIgnoreCase(storageType)) {
                deleteFromMinio(filePath);
            } else {
                deleteFromLocal(filePath);
            }
        } catch (Exception e) {
            log.error("Failed to delete file: {}", filePath, e);
        }
    }
    
    /**
     * 从 MinIO 删除文件
     */
    private void deleteFromMinio(String objectName) throws Exception {
        String bucket = infraProperties.getStorage().getMinio().getBucket();
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        );
        log.info("File deleted from MinIO: {}/{}", bucket, objectName);
    }
    
    /**
     * 从本地删除文件
     */
    private void deleteFromLocal(String fileName) throws IOException {
        Path filePath = getLocalFilePath(fileName);
        if (Files.exists(filePath)) {
            Files.delete(filePath);
            log.info("File deleted from local: {}", filePath);
        }
    }
    
    /**
     * 判断是否为 MinIO 存储模式
     */
    public boolean isMinioMode() {
        return "minio".equalsIgnoreCase(infraProperties.getStorage().getType());
    }
}
