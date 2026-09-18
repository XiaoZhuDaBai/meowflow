package com.meowflow.infra.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.infra.persistence.entity.ChunkEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分块 Mapper
 */
@Mapper
public interface ChunkMapper extends BaseMapper<ChunkEntity> {

    @Select("SELECT * FROM document_chunk WHERE document_id = #{docId} ORDER BY chunk_index")
    List<ChunkEntity> findByDocumentId(@Param("docId") Long documentId);

    @Select("SELECT * FROM document_chunk WHERE document_id = #{docId} ORDER BY chunk_index LIMIT #{limit}")
    List<ChunkEntity> findByDocumentIdWithLimit(@Param("docId") Long documentId, @Param("limit") int limit);
}
