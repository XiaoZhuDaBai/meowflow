package com.meowflow.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meowflow.user.entity.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

@Mapper
public interface UserRoleRepository extends BaseMapper<UserRole> {

    @Select("SELECT role_id FROM mf_sys_user_role WHERE user_id = #{userId}")
    Set<Long> findRoleIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT EXISTS(SELECT 1 FROM mf_sys_user_role WHERE user_id = #{userId} AND role_id = #{roleId})")
    boolean existsByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);

    @Delete("DELETE FROM mf_sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    @Delete("DELETE FROM mf_sys_user_role WHERE user_id = #{userId} AND role_id = #{roleId}")
    int deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
