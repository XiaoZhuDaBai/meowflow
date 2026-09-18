package com.meowflow.workflow.executor.db;

import com.meowflow.common.exception.NodeException;
import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import com.meowflow.workflow.executor.AbstractNodeExecutor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库操作节点：通过 JDBC 执行查询或写入。
 */
@Component
public class DatabaseExecutor extends AbstractNodeExecutor {

    @Override
    public NodeType getNodeType() {
        return NodeType.DB;
    }

    @Override
    protected NodeResult doExecute(ExecutionContext context, NodeDefinition node, Map<String, Object> input) {
        Object dsnObj = input.get("dsn");
        if (dsnObj == null) dsnObj = input.get("jdbcUrl");
        if (dsnObj == null) dsnObj = input.get("url");
        String url = dsnObj != null ? dsnObj.toString() : null;
        String username = input.get("username") != null ? input.get("username").toString() : null;
        String password = input.get("password") != null ? input.get("password").toString() : null;
        String sql = input.get("sql") != null ? input.get("sql").toString() : null;
        if (url == null || url.isBlank() || sql == null || sql.isBlank()) {
            throw new NodeException(node.getId(), node.getType().getCode(),
                    "数据库 URL 与 SQL 均必填");
        }

        List<Object> params = resolveParams(input.get("params"));
        boolean query = isQuery(sql);
        Map<String, Object> output = new HashMap<>();

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) {
                statement.setObject(i + 1, params.get(i));
            }

            if (query) {
                try (ResultSet rs = statement.executeQuery()) {
                    List<Map<String, Object>> rows = new ArrayList<>();
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(meta.getColumnLabel(i), rs.getObject(i));
                        }
                        rows.add(row);
                    }
                    output.put("rows", rows);
                    output.put("rowCount", rows.size());
                }
            } else {
                int affected = statement.executeUpdate();
                output.put("affectedRows", affected);
            }
            output.put("sql", sql);
            return NodeResult.success(node.getId(), node.getType(), node.getName(), output);
        } catch (SQLException e) {
            throw new NodeException(node.getId(), node.getType().getCode(),
                    "数据库执行失败: " + e.getMessage(), e);
        }
    }

    private List<Object> resolveParams(Object value) {
        List<Object> params = new ArrayList<>();
        if (value instanceof List) {
            params.addAll((List<?>) value);
        } else if (value instanceof Map) {
            params.addAll(((Map<?, ?>) value).values());
        }
        return params;
    }

    private boolean isQuery(String sql) {
        String trimmed = sql.trim().toLowerCase();
        return trimmed.startsWith("select") || trimmed.startsWith("with") || trimmed.startsWith("explain");
    }
}
