package com.meowflow.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.user.entity.RolePermission;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

@Mapper
public interface RolePermissionRepository extends BaseMapper<RolePermission> {

    @Select("SELECT permission_id FROM mf_sys_role_permission WHERE role_id = #{roleId}")
    Set<Long> findPermissionIdsByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM mf_sys_role_permission WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM mf_sys_role_permission WHERE role_id = #{roleId} AND permission_id = #{permissionId}")
    int deleteByRoleIdAndPermissionId(@Param("roleId") Long roleId, @Param("permissionId") Long permissionId);
}
