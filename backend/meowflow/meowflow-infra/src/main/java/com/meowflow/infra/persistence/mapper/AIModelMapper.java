package com.meowflow.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.infra.entity.AIModelEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型配置 Mapper
 */
@Mapper
public interface AIModelMapper extends BaseMapper<AIModelEntity> {
}
