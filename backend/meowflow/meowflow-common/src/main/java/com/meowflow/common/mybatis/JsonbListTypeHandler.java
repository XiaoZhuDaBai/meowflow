package com.meowflow.common.mybatis;

import com.meowflow.common.util.JsonUtils;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * PostgreSQL jsonb &lt;-&gt; {@code List<String>} 映射。
 *
 * <p>背景：PostgreSQL 的 jsonb 列无法直接绑定 Java {@code List}，若不显式指定 TypeHandler，
 * MyBatis 会按默认规则绑定参数，写入时报
 * {@code column "tags" is of type jsonb but expression is of type character varying}，
 * 该异常被全局异常处理器包装成 500，表现为"创建工作流失败"。
 *
 * <p>用法（与 {@link JsonbStringTypeHandler} 一致，需要在字段上显式声明）：
 * <pre>
 * &#64;TableField(typeHandler = JsonbListTypeHandler.class)
 * private List&lt;String&gt; tags;
 * </pre>
 */
@MappedTypes(List.class)
public class JsonbListTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setObject(i, JsonUtils.toJson(parameter), Types.OTHER);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    /**
     * jsonb 列可能为 NULL 或空串；历史数据里也可能存的是 JSON 对象而非数组，
     * 这里统一兜底成空列表，避免读取路径抛异常导致列表接口整体 500。
     */
    private List<String> parse(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<String> parsed = JsonUtils.fromJsonToList(json, String.class);
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
