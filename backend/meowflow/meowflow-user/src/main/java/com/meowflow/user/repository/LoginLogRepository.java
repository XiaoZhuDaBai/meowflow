package com.meowflow.user.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meowflow.user.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LoginLogRepository extends BaseMapper<LoginLog> {

    @Select("SELECT * FROM mf_sys_login_log WHERE username = #{username} ORDER BY login_time DESC")
    List<LoginLog> findByUsername(@Param("username") String username);

    @Select("SELECT * FROM mf_sys_login_log WHERE status = #{status} ORDER BY login_time DESC")
    List<LoginLog> findByStatus(@Param("status") String status);

    @Select("SELECT * FROM mf_sys_login_log WHERE login_time BETWEEN #{startTime} AND #{endTime} ORDER BY login_time DESC")
    List<LoginLog> findByLoginTimeBetween(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    @Select("SELECT COUNT(*) FROM mf_sys_login_log WHERE username = #{username} AND status = 'fail' AND login_time > #{since}")
    int countFailedLoginsSince(@Param("username") String username, @Param("since") LocalDateTime since);

    @Select("DELETE FROM mf_sys_login_log")
    void deleteAll();

    @Select("<script>" +
            "SELECT * FROM mf_sys_login_log WHERE 1=1 " +
            "<when test='username != null and username != \"\"'>" +
            " AND username = #{username}" +
            "</when>" +
            "<when test='status != null and status != \"\"'>" +
            " AND status = #{status}" +
            "</when>" +
            "<when test='startTime != null'>" +
            " AND login_time &gt;= #{startTime}" +
            "</when>" +
            "<when test='endTime != null'>" +
            " AND login_time &lt;= #{endTime}" +
            "</when>" +
            " ORDER BY login_time DESC" +
            "</script>")
    IPage<LoginLog> pageQuery(Page<LoginLog> page,
                               @Param("username") String username,
                               @Param("status") String status,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);
}
