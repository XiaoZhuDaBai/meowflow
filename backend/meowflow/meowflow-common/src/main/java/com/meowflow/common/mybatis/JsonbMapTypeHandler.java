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
import java.util.Map;

@MappedTypes(Map.class)
public class JsonbMapTypeHandler extends BaseTypeHandler<Map<String, Object>> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Map<String, Object> parameter, JdbcType jdbcType) throws SQLException {
        ps.setObject(i, JsonUtils.toJson(parameter), Types.OTHER);
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return JsonUtils.fromJsonToMap(rs.getString(columnName));
    }

    @Override
    public Map<String, Object> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return JsonUtils.fromJsonToMap(rs.getString(columnIndex));
    }

    @Override
    public Map<String, Object> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return JsonUtils.fromJsonToMap(cs.getString(columnIndex));
    }
}
