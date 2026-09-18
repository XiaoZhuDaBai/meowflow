package com.meowflow.infra.config;

import com.meowflow.infra.embedding.EmbeddingClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Embedding 客户端配置
 */
@Configuration
public class EmbeddingConfig {

    @Bean
    @ConditionalOnMissingBean(EmbeddingClient.class)
    public EmbeddingClient defaultEmbeddingClient(
            @Qualifier("aliEmbeddingClient") List<EmbeddingClient> clients) {
        if (clients == null || clients.isEmpty()) {
            throw new IllegalStateException("No EmbeddingClient implementation found");
        }
        return clients.get(0);
    }
}
