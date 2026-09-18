package com.meowflow.common.mybatis;

import com.meowflow.common.security.AESEncryptor;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;

/**
 * 加密 Long 类型处理器
 * <p>
 * 用于 MyBatis-Plus 自动处理加密 Long 类型字段的读写。
 * 加密后以 String 形式存储在数据库中。
 * <p>
 * 使用方式：
 * <pre>
 * &#64;TableName("mf_sensitive_data")
 * public class SensitiveData {
 *     &#64;Encrypted
 *     &#64;TableField(typeHandler = EncryptedLongTypeHandler.class)
 *     private Long sensitiveId;
 * }
 * </pre>
 */
@org.apache.ibatis.type.MappedTypes(Long.class)
public class EncryptedLongTypeHandler extends BaseTypeHandler<Long> {

    private static AESEncryptor encryptor;

    /**
     * 设置加密器实例 (由框架注入)
     */
    public static void setEncryptor(AESEncryptor encryptor) {
        EncryptedLongTypeHandler.encryptor = encryptor;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Long parameter, JdbcType jdbcType)
            throws SQLException {
        // 将 Long 转为 String 后加密存储
        String encrypted = encryptor.encrypt(String.valueOf(parameter));
        ps.setString(i, encrypted);
    }

    @Override
    public Long getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String encrypted = rs.getString(columnName);
        return convertToLong(encrypted, columnName);
    }

    @Override
    public Long getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String encrypted = rs.getString(columnIndex);
        return convertToLong(encrypted, "column index " + columnIndex);
    }

    @Override
    public Long getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String encrypted = cs.getString(columnIndex);
        return convertToLong(encrypted, "column index " + columnIndex);
    }

    /**
     * 将加密值转为 Long
     */
    private Long convertToLong(String encrypted, String context) {
        if (encrypted == null) {
            return null;
        }
        try {
            String decrypted = encryptor.decrypt(encrypted);
            return Long.parseLong(decrypted);
        } catch (Exception e) {
            // 如果解密失败，尝试直接解析 (可能是未加密的旧数据)
            try {
                return Long.parseLong(encrypted);
            } catch (NumberFormatException ex) {
                throw new RuntimeException("Failed to parse Long from encrypted field: " + context, ex);
            }
        }
    }
}
