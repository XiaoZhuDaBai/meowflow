package com.meowflow.common.util;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 安全工具类
 * <p>
 * 提供密码加密、签名验证、数据脱敏等通用安全功能。
 * 依赖 Hutool 工具库实现。
 */
@Slf4j
public class SecurityUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{15}|\\d{18}$");

    private SecurityUtils() {
    }

    // ==================== 密码处理 ====================

    /**
     * 使用 SHA-256 对密码进行加盐哈希
     * <p>
     * 生成格式: salt:hash
     *
     * @param password 明文密码
     * @param salt     盐值
     * @return 哈希后的密码
     */
    public static String hashPassword(String password, String salt) {
        if (StrUtil.isBlank(password) || StrUtil.isBlank(salt)) {
            throw new IllegalArgumentException("Password and salt cannot be blank");
        }
        return salt + ":" + SecureUtil.sha256(password + salt);
    }

    /**
     * 验证密码是否匹配
     *
     * @param inputPassword  输入的明文密码
     * @param storedPassword 存储的哈希密码 (格式: salt:hash)
     * @return 是否匹配
     */
    public static boolean verifyPassword(String inputPassword, String storedPassword) {
        if (StrUtil.isBlank(inputPassword) || StrUtil.isBlank(storedPassword)) {
            return false;
        }
        if (!storedPassword.contains(":")) {
            return false;
        }
        String[] parts = storedPassword.split(":");
        String salt = parts[0];
        String storedHash = parts[1];
        String inputHash = SecureUtil.sha256(inputPassword + salt);
        return storedHash.equals(inputHash);
    }

    /**
     * 生成随机盐值
     *
     * @return 32位随机盐值
     */
    public static String generateSalt() {
        // 使用 UUID 生成随机盐值
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * MD5 哈希（用于非敏感数据快速校验）
     *
     * @param input 输入字符串
     * @return MD5 哈希值
     */
    public static String md5(String input) {
        return SecureUtil.md5(input);
    }

    /**
     * SHA-256 哈希
     *
     * @param input 输入字符串
     * @return SHA-256 哈希值
     */
    public static String sha256(String input) {
        return SecureUtil.sha256(input);
    }

    // ==================== 对称加密 ====================

    /**
     * AES-128 加密（ECB 模式，简单场景使用）
     *
     * @param plaintext 明文
     * @param key      密钥（16字节）
     * @return Base64 编码的密文
     */
    public static String aesEncrypt(String plaintext, String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, keyBytes);
        return aes.encryptBase64(plaintext, CharsetUtil.CHARSET_UTF_8);
    }

    /**
     * AES-128 解密
     *
     * @param ciphertext Base64 编码的密文
     * @param key       密钥（16字节）
     * @return 解密后的明文
     */
    public static String aesDecrypt(String ciphertext, String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        SymmetricCrypto aes = new SymmetricCrypto(SymmetricAlgorithm.AES, keyBytes);
        return aes.decryptStr(ciphertext, CharsetUtil.CHARSET_UTF_8);
    }

    // ==================== 数据脱敏 ====================

    /**
     * 脱敏手机号
     * 显示前3后4，中间用 * 代替
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (StrUtil.isBlank(phone)) {
            return phone;
        }
        // 显示前3后4，中间用 * 代替
        if (phone.length() <= 7) {
            return phone;
        }
        return mask(phone, 3, 4);
    }

    /**
     * 脱敏邮箱
     * 显示前2后@前，中间用 * 代替
     *
     * @param email 邮箱
     * @return 脱敏后的邮箱
     */
    public static String maskEmail(String email) {
        if (StrUtil.isBlank(email)) {
            return email;
        }
        // 显示前2后@前，中间用 * 代替
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return email;
        }
        String prefix = email.substring(0, 2);
        String domain = email.substring(atIndex);
        String masked = StrUtil.repeat("*", atIndex - 2);
        return prefix + masked + domain;
    }

    /**
     * 脱敏身份证号
     * 显示前6后4，中间用 * 代替
     *
     * @param idCard 身份证号
     * @return 脱敏后的身份证号
     */
    public static String maskIdCard(String idCard) {
        if (StrUtil.isBlank(idCard)) {
            return idCard;
        }
        // 显示前6后4，中间用 * 代替
        return mask(idCard, 6, 4);
    }

    /**
     * 脱敏银行卡号
     * 显示前6后4，中间用 * 代替
     *
     * @param bankCard 银行卡号
     * @return 脱敏后的银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (StrUtil.isBlank(bankCard)) {
            return bankCard;
        }
        // 显示前6后4，中间用 * 代替
        return mask(bankCard, 6, 4);
    }

    /**
     * 通用脱敏
     *
     * @param str  原始字符串
     * @param prefixLen 前缀保留长度
     * @param suffixLen 后缀保留长度
     * @return 脱敏后的字符串
     */
    public static String mask(String str, int prefixLen, int suffixLen) {
        if (StrUtil.isBlank(str) || str.length() <= prefixLen + suffixLen) {
            return str;
        }
        int maskedLen = str.length() - prefixLen - suffixLen;
        String masked = StrUtil.repeat("*", maskedLen);
        return str.substring(0, prefixLen) + masked + str.substring(str.length() - suffixLen);
    }

    /**
     * 密码强度校验
     *
     * @param password 密码
     * @return 强度等级：0=弱, 1=中等, 2=强
     */
    public static int passwordStrength(String password) {
        if (StrUtil.isBlank(password)) {
            return 0;
        }
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.length() >= 12) score++;
        if (Pattern.compile("[a-z]").matcher(password).find()) score++;
        if (Pattern.compile("[A-Z]").matcher(password).find()) score++;
        if (Pattern.compile("[0-9]").matcher(password).find()) score++;
        if (Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]").matcher(password).find()) score++;

        if (score <= 2) return 0;
        if (score <= 4) return 1;
        return 2;
    }

    /**
     * 检测字符串是否为敏感信息格式
     *
     * @param str 待检测字符串
     * @return 敏感类型：null=非敏感, email=邮箱, phone=手机号, idcard=身份证
     */
    public static String detectSensitiveType(String str) {
        if (StrUtil.isBlank(str)) {
            return null;
        }
        if (EMAIL_PATTERN.matcher(str).matches()) {
            return "email";
        }
        if (PHONE_PATTERN.matcher(str).matches()) {
            return "phone";
        }
        if (ID_CARD_PATTERN.matcher(str).matches()) {
            return "idcard";
        }
        return null;
    }

    /**
     * 自动检测并脱敏敏感信息
     *
     * @param str 原始字符串
     * @return 脱敏后的字符串
     */
    public static String autoMask(String str) {
        String type = detectSensitiveType(str);
        if (type == null) {
            return str;
        }
        return switch (type) {
            case "email" -> maskEmail(str);
            case "phone" -> maskPhone(str);
            case "idcard" -> maskIdCard(str);
            default -> str;
        };
    }

    /**
     * 生成 HMAC-SHA256 签名
     *
     * @param data 数据
     * @param key  密钥
     * @return Base64 编码的签名
     */
    public static String hmacSha256(String data, String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("HmacSHA256");
            md.update(key.getBytes(StandardCharsets.UTF_8));
            byte[] hmac = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmac);
        } catch (Exception e) {
            log.error("HMAC-SHA256签名失败", e);
            throw new RuntimeException("签名失败", e);
        }
    }

    /**
     * 验证 HMAC-SHA256 签名
     *
     * @param data              数据
     * @param key               密钥
     * @param expectedSignature 期望的签名
     * @return 是否匹配
     */
    public static boolean verifyHmacSha256(String data, String key, String expectedSignature) {
        String actualSignature = hmacSha256(data, key);
        return MessageDigest.isEqual(
                actualSignature.getBytes(StandardCharsets.UTF_8),
                expectedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }
}
