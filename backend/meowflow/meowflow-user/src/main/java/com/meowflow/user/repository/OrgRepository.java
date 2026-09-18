package com.meowflow.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.user.entity.Org;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface OrgRepository extends BaseMapper<Org> {

    @Select("SELECT * FROM mf_sys_org WHERE id = #{id}")
    Optional<Org> findById(@Param("id") Long id);

    @Select("SELECT * FROM mf_sys_org WHERE parent_id = #{parentId} ORDER BY sort ASC")
    List<Org> findByParentId(@Param("parentId") Long parentId);

    @Select("WITH RECURSIVE descendants AS (" +
            "SELECT * FROM mf_sys_org WHERE parent_id = #{ancestors} " +
            "UNION ALL " +
            "SELECT o.* FROM mf_sys_org o INNER JOIN descendants d ON o.parent_id = d.id" +
            ") SELECT * FROM descendants")
    List<Org> findByAncestorsContaining(@Param("ancestors") String ancestors);

    @Select("SELECT EXISTS(SELECT 1 FROM mf_sys_org WHERE code = #{code})")
    boolean existsByCode(@Param("code") String code);

    @Select("SELECT * FROM mf_sys_org WHERE code = #{code}")
    Optional<Org> findByCode(@Param("code") String code);

    @Select("<script>" +
            "SELECT * FROM mf_sys_org WHERE 1=1 " +
            "<when test='keyword != null and keyword != \"\"'>" +
            " AND (name LIKE CONCAT('%', #{keyword}, '%') " +
            " OR code LIKE CONCAT('%', #{keyword}, '%'))" +
            "</when>" +
            "<when test='status != null and status != \"\"'>" +
            " AND status = #{status}" +
            "</when>" +
            " ORDER BY sort ASC" +
            "</script>")
    IPage<Org> pageQuery(Page<Org> page,
                         @Param("keyword") String keyword,
                         @Param("status") String status);

    @Select("SELECT COUNT(*) FROM mf_sys_org WHERE parent_id = #{parentId}")
    int countByParentId(@Param("parentId") Long parentId);
}
