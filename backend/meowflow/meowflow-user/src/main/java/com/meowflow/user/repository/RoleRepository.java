package com.meowflow.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.user.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;
import java.util.Set;

@Mapper
public interface RoleRepository extends BaseMapper<Role> {

    @Select("SELECT * FROM mf_sys_role WHERE id = #{id}")
    Optional<Role> findById(@Param("id") Long id);

    @Select("SELECT * FROM mf_sys_role WHERE code = #{code}")
    Optional<Role> findByCode(@Param("code") String code);

    @Select("SELECT EXISTS(SELECT 1 FROM mf_sys_role WHERE code = #{code})")
    boolean existsByCode(@Param("code") String code);

    default boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    @Select("<script>" +
            "SELECT * FROM mf_sys_role WHERE 1=1 " +
            "<when test='keyword != null and keyword != \"\"'>" +
            " AND (name LIKE CONCAT('%', #{keyword}, '%') " +
            " OR code LIKE CONCAT('%', #{keyword}, '%'))" +
            "</when>" +
            "<when test='status != null and status != \"\"'>" +
            " AND status = #{status}" +
            "</when>" +
            " ORDER BY sort ASC" +
            "</script>")
    IPage<Role> pageQuery(Page<Role> page,
                          @Param("keyword") String keyword,
                          @Param("status") String status);

    @Select("SELECT p.id FROM mf_sys_permission p " +
            "INNER JOIN mf_sys_role_permission rp ON p.id = rp.permission_id " +
            "WHERE rp.role_id = #{roleId}")
    Set<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT r.* FROM mf_sys_role r " +
            "INNER JOIN mf_sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    Set<Role> findByUserId(@Param("userId") Long userId);
}
