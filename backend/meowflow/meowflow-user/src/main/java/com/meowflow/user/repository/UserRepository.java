package com.meowflow.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Mapper
public interface UserRepository extends BaseMapper<User> {

    @Select("SELECT * FROM mf_sys_user WHERE username = #{username}")
    Optional<User> findByUsername(@Param("username") String username);

    @Select("SELECT * FROM mf_sys_user WHERE id = #{id}")
    Optional<User> findById(@Param("id") Long id);

    @Select("SELECT EXISTS(SELECT 1 FROM mf_sys_user WHERE username = #{username})")
    boolean existsByUsername(@Param("username") String username);

    @Select("SELECT * FROM mf_sys_user WHERE email = #{email}")
    Optional<User> findByEmail(@Param("email") String email);

    @Select("SELECT EXISTS(SELECT 1 FROM mf_sys_user WHERE email = #{email})")
    boolean existsByEmail(@Param("email") String email);

    @Select("SELECT EXISTS(SELECT 1 FROM mf_sys_user WHERE phone = #{phone})")
    boolean existsByPhone(@Param("phone") String phone);

    default boolean existsById(Long id) {
        return findById(id).isPresent();
    }

    @Select("<script>" +
            "SELECT * FROM mf_sys_user WHERE 1=1 " +
            "<when test='keyword != null and keyword != \"\"'>" +
            " AND (username LIKE CONCAT('%', #{keyword}, '%') " +
            " OR nickname LIKE CONCAT('%', #{keyword}, '%') " +
            " OR phone LIKE CONCAT('%', #{keyword}, '%'))" +
            "</when>" +
            "<when test='status != null and status != \"\"'>" +
            " AND status = #{status}" +
            "</when>" +
            "<when test='orgId != null'>" +
            " AND org_id = #{orgId}" +
            "</when>" +
            " ORDER BY create_time DESC" +
            "</script>")
    IPage<User> pageQuery(Page<User> page,
                          @Param("keyword") String keyword,
                          @Param("status") String status,
                          @Param("orgId") Long orgId);

    @Select("SELECT u.* FROM mf_sys_user u " +
            "INNER JOIN mf_sys_user_role ur ON u.id = ur.user_id " +
            "WHERE ur.role_id = #{roleId}")
    List<User> findByRoleId(@Param("roleId") Long roleId);

    @Select("SELECT p.code FROM mf_sys_permission p " +
            "INNER JOIN mf_sys_role_permission rp ON p.id = rp.permission_id " +
            "INNER JOIN mf_sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND p.status = 'active'")
    Set<String> findPermissionsByUserId(@Param("userId") Long userId);

    @Select("SELECT r.code FROM mf_sys_role r " +
            "INNER JOIN mf_sys_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 'active'")
    Set<String> findRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 用户所属组织 id 集合（个人组织 + 加入的团队）。
     * 用于前端获取当前用户可切换的组织列表。
     */
    @Select("SELECT DISTINCT org_id FROM mf_sys_user_role WHERE user_id = #{userId}")
    Set<Long> findOrgIdsByUserId(@Param("userId") Long userId);
}
