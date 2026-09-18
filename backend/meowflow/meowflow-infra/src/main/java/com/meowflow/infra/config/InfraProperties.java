package com.meowflow.infra.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Infra 模块配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "meowflow.knowledge")
public class InfraProperties {

    /**
     * 文件存储配置
     */
    private Storage storage = new Storage();

    @Data
    public static class Storage {
        /**
         * 存储类型: local | minio
         */
        private String type = "local";

        /**
         * 本地存储目录
         */
        private String localDir = "./knowledge-files";

        /**
         * MinIO 配置
         */
        private Minio minio = new Minio();

        @Data
        public static class Minio {
            /**
             * MinIO 服务端点
             */
            private String endpoint = "http://localhost:9000";

            /**
             * 访问密钥
             */
            private String accessKey = "minioadmin";

            /**
             * 密钥
             */
            private String secretKey = "minioadmin";

            /**
             * 存储桶名称
             */
            private String bucket = "meowflow-knowledge";

            /**
             * 是否自动创建存储桶
             */
            private boolean autoCreateBucket = true;
        }
    }
}
