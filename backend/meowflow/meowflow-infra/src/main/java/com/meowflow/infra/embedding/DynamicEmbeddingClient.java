package com.meowflow.infra.embedding;

import com.meowflow.common.exception.BizException;
import com.meowflow.common.result.ResultCode;
import com.meowflow.infra.entity.AIModelEntity;
import com.meowflow.infra.service.AIModelService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 运行时根据前端保存的默认 Embedding 模型创建客户端。
 */
@Component
@Primary
public class DynamicEmbeddingClient implements EmbeddingClient {

    private final AIModelService modelService;
    private final EmbeddingClientFactory clientFactory;

    public DynamicEmbeddingClient(AIModelService modelService, EmbeddingClientFactory clientFactory) {
        this.modelService = modelService;
        this.clientFactory = clientFactory;
    }

    @Override
    public EmbeddingResult embed(String text) {
        return current().embed(text);
    }

    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts) {
        return current().embedBatch(texts);
    }

    @Override
    public String getModelName() {
        return current().getModelName();
    }

    @Override
    public int getDimension() {
        return current().getDimension();
    }

    private EmbeddingClient current() {
        AIModelEntity model = modelService.resolveEmbedding();
        if (model == null) {
            throw new BizException(ResultCode.PARAM_ERROR,
                    "请先在系统设置中启用一个 capabilities 包含 embedding 的模型");
        }
        return clientFactory.create(model);
    }
}
