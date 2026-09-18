package com.meowflow.workflow.executor.db;

import com.meowflow.workflow.definition.NodeDefinition;
import com.meowflow.workflow.definition.NodeType;
import com.meowflow.workflow.engine.ExecutionContext;
import com.meowflow.workflow.engine.NodeResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseExecutorTest {

    @Test
    void executesSelectAgainstH2() throws Exception {
        Class.forName("org.h2.Driver");
        String url = "jdbc:h2:mem:db_" + System.nanoTime() + ";DB_CLOSE_DELAY=-1";
        try (var connection = java.sql.DriverManager.getConnection(url)) {
            connection.createStatement().execute("CREATE TABLE t (id INT, name VARCHAR(32))");
            connection.createStatement().execute("INSERT INTO t VALUES (1, 'a'), (2, 'b')");
        }

        DatabaseExecutor executor = new DatabaseExecutor();
        NodeDefinition node = NodeDefinition.builder()
                .id("n1")
                .name("DB")
                .type(NodeType.DB)
                .data(Map.of("dsn", url, "sql", "SELECT * FROM t ORDER BY id"))
                .build();

        NodeResult result = executor.execute(ExecutionContext.builder().build(), node);

        assertThat(result.isSuccess()).isTrue();
        List<?> rows = (List<?>) result.getOutput().get("rows");
        assertThat(rows).hasSize(2);
    }
}
