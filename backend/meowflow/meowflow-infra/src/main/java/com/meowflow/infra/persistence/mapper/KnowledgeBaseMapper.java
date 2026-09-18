package com.meowflow.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.infra.persistence.entity.KnowledgeBaseEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库 Mapper
 */
@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBaseEntity> {
}
