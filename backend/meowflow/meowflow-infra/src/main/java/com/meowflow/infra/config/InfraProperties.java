package com.meowflow.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "meowflow.knowledge")
public class InfraProperties {
    
    private Storage storage = new Storage();
    
    @Data
    public static class Storage {
        /**
         * 存储类型：local（本地文件系统）或 minio（MinIO 对象存储）
         */
        private String type = "local";
        
        /**
         * 本地存储目录路径
         */
        private String localDir = "./knowledge-files";
        
        /**
         * MinIO 配置
         */
        private MinioConfig minio = new MinioConfig();
    }
    
    @Data
    public static class MinioConfig {
        /**
         * MinIO 服务端点
         */
        private String endpoint;
        
        /**
         * MinIO 访问密钥
         */
        private String accessKey;
        
        /**
         * MinIO 秘密密钥
         */
        private String secretKey;
        
        /**
         * 知识库文件存储的 Bucket 名称
         */
        private String bucket;
        
        /**
         * 是否自动创建 Bucket（如果不存在）
         */
        private boolean autoCreateBucket = true;
    }
}
