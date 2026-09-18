package com.meowflow.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.infra.entity.IntegrationConfigEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 集成配置 Mapper
 */
@Mapper
public interface IntegrationConfigMapper extends BaseMapper<IntegrationConfigEntity> {
}
