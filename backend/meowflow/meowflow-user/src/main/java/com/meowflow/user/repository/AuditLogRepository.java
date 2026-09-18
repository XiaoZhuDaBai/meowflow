package com.meowflow.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.user.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Mapper
public interface AuditLogRepository extends BaseMapper<AuditLog> {

    @Select("SELECT * FROM mf_sys_audit_log WHERE id = #{id}")
    Optional<AuditLog> findById(@Param("id") Long id);

    @Select("SELECT * FROM mf_sys_audit_log WHERE username = #{username} ORDER BY operate_time DESC")
    List<AuditLog> findByUsername(@Param("username") String username);

    @Select("SELECT * FROM mf_sys_audit_log WHERE module = #{module} ORDER BY operate_time DESC")
    List<AuditLog> findByModule(@Param("module") String module);

    @Select("SELECT * FROM mf_sys_audit_log WHERE operate_time BETWEEN #{startTime} AND #{endTime} ORDER BY operate_time DESC")
    IPage<AuditLog> findByOperateTimeBetween(Page<AuditLog> page,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    @Select("<script>" +
            "SELECT * FROM mf_sys_audit_log WHERE 1=1 " +
            "<when test='username != null and username != \"\"'>" +
            " AND username = #{username}" +
            "</when>" +
            "<when test='module != null and module != \"\"'>" +
            " AND module LIKE CONCAT('%', #{module}, '%')" +
            "</when>" +
            "<when test='status != null and status != \"\"'>" +
            " AND status = #{status}" +
            "</when>" +
            "<when test='startTime != null'>" +
            " AND operate_time >= #{startTime}" +
            "</when>" +
            "<when test='endTime != null'>" +
            " AND operate_time &lt;= #{endTime}" +
            "</when>" +
            " ORDER BY operate_time DESC" +
            "</script>")
    IPage<AuditLog> pageQuery(Page<AuditLog> page,
                              @Param("username") String username,
                              @Param("module") String module,
                              @Param("status") String status,
                              @Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);
}
