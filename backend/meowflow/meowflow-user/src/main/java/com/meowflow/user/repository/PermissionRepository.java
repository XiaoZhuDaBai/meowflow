package com.meowflow.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.user.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mapper
public interface PermissionRepository extends BaseMapper<Permission> {

    @Select("SELECT * FROM mf_sys_permission WHERE id = #{id}")
    Optional<Permission> findById(@Param("id") Long id);

    default boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    @Select("SELECT EXISTS(SELECT 1 FROM mf_sys_permission WHERE code = #{code})")
    boolean existsByCode(@Param("code") String code);

    @Select("SELECT * FROM mf_sys_permission WHERE id IN " +
            "(SELECT permission_id FROM mf_sys_role_permission WHERE role_id = #{roleId})")
    List<Permission> findByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT p.code FROM mf_sys_permission p " +
            "INNER JOIN mf_sys_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN mf_sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.status = 'active'")
    Set<String> findCodesByUserId(@Param("userId") Long userId);

    @Select("<script>" +
            "SELECT * FROM mf_sys_permission WHERE 1=1 " +
            "<when test='menuType != null and menuType.size > 0'>" +
            " AND type IN " +
            "<foreach collection='menuType' item='item' open='(' separator=',' close=')'>" +
            " #{item}" +
            "</foreach>" +
            "</when>" +
            "<when test='status != null and status != \"\"'>" +
            " AND status = #{status}" +
            "</when>" +
            "<when test='perms != null and perms.size > 0'>" +
            " AND code IN " +
            "<foreach collection='perms' item='item' open='(' separator=',' close=')'>" +
            " #{item}" +
            "</foreach>" +
            "</when>" +
            " ORDER BY sort ASC" +
            "</script>")
    List<Permission> findByMenuTypesAndStatusAndPermsIn(
            @Param("menuType") Set<String> menuType,
            @Param("status") String status,
            @Param("perms") Set<String> perms);

    @Select("SELECT * FROM mf_sys_permission WHERE type IN ('M', 'C') ORDER BY sort ASC")
    List<Permission> findAllMenus();

    @Select("<script>" +
            "SELECT * FROM mf_sys_permission WHERE 1=1 " +
            "<when test='pid != null'>" +
            " AND parent_id = #{pid}" +
            "</when>" +
            "<when test='pid == null'>" +
            " AND (parent_id = 0 OR parent_id IS NULL)" +
            "</when>" +
            " ORDER BY sort ASC" +
            "</script>")
    List<Permission> findByPid(@Param("pid") Long pid);

    @Select("SELECT * FROM mf_sys_permission WHERE type = 'F' ORDER BY sort ASC")
    List<Permission> findAllButtons();

    @Select("<script>" +
            "SELECT * FROM mf_sys_permission WHERE 1=1 " +
            "<when test='keyword != null and keyword != \"\"'>" +
            " AND (name LIKE CONCAT('%', #{keyword}, '%') " +
            " OR code LIKE CONCAT('%', #{keyword}, '%'))" +
            "</when>" +
            "<when test='status != null and status != \"\"'>" +
            " AND status = #{status}" +
            "</when>" +
            "<when test='menuType != null and menuType != \"\"'>" +
            " AND type = #{menuType}" +
            "</when>" +
            " ORDER BY sort ASC" +
            "</script>")
    IPage<Permission> pageQuery(Page<Permission> page,
                                 @Param("keyword") String keyword,
                                 @Param("status") String status,
                                 @Param("menuType") String menuType);
}
