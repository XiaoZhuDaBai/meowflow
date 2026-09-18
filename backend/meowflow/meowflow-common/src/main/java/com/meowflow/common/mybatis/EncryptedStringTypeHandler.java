package com.meowflow.common.mybatis;

import com.meowflow.common.security.AESEncryptor;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;

/**
 * 加密字符串类型处理器
 * <p>
 * 用于 MyBatis-Plus 自动处理加密字段的读写。
 * <p>
 * 使用方式：
 * 1. 在实体字段上标注 @Encrypted 注解
 * 2. 在 TypeHandler 配置中指定使用此类
 * <p>
 * 示例：
 * <pre>
 * &#64;TableName("mf_int_config")
 * public class IntegrationConfig {
 *     &#64;Encrypted
 *     &#64;TableField(typeHandler = EncryptedStringTypeHandler.class)
 *     private String webhookSecret;
 * }
 * </pre>
 */
@org.apache.ibatis.type.MappedTypes(String.class)
public class EncryptedStringTypeHandler extends BaseTypeHandler<String> {

    private static AESEncryptor encryptor;

    /**
     * 设置加密器实例 (由框架注入)
     */
    public static void setEncryptor(AESEncryptor encryptor) {
        EncryptedStringTypeHandler.encryptor = encryptor;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        String encrypted = encryptor.encrypt(parameter);
        ps.setString(i, encrypted);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String encrypted = rs.getString(columnName);
        return decryptIfNeeded(encrypted);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String encrypted = rs.getString(columnIndex);
        return decryptIfNeeded(encrypted);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String encrypted = cs.getString(columnIndex);
        return decryptIfNeeded(encrypted);
    }

    /**
     * 解密数据
     */
    private String decryptIfNeeded(String encrypted) {
        if (encrypted == null) {
            return null;
        }
        try {
            return encryptor.decrypt(encrypted);
        } catch (Exception e) {
            // 如果解密失败，可能数据未被加密，直接返回原值
            return encrypted;
        }
    }
}
