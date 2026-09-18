package com.meowflow.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.user.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

@Mapper
public interface PostRepository extends BaseMapper<Post> {

    @Select("SELECT * FROM mf_sys_post WHERE id = #{id}")
    Optional<Post> findById(@Param("id") Long id);

    @Select("SELECT EXISTS(SELECT 1 FROM mf_sys_post WHERE post_code = #{postCode})")
    boolean existsByPostCode(@Param("postCode") String postCode);

    @Select("<script>" +
            "SELECT * FROM mf_sys_post WHERE 1=1 " +
            "<when test='keyword != null and keyword != \"\"'>" +
            " AND (post_name LIKE CONCAT('%', #{keyword}, '%') " +
            " OR post_code LIKE CONCAT('%', #{keyword}, '%'))" +
            "</when>" +
            "<when test='status != null and status != \"\"'>" +
            " AND status = #{status}" +
            "</when>" +
            " ORDER BY post_sort ASC" +
            "</script>")
    IPage<Post> pageQuery(Page<Post> page,
                           @Param("keyword") String keyword,
                           @Param("status") String status);
}
