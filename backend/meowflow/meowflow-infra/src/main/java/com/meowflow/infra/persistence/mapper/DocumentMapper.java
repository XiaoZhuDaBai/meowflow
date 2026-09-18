package com.meowflow.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.infra.persistence.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 文档 Mapper
 */
@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity> {

    @Select("SELECT * FROM document WHERE knowledge_base_id = #{kbId} AND status = #{status}")
    List<DocumentEntity> findByKbIdAndStatus(@Param("kbId") Long kbId, @Param("status") String status);
}
