package com.meowflow.common.security;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES 加密器，使用 GCM 模式提供authenticated encryption
 * <p>
 * GCM 模式特点：
 * - 每次加密自动生成随机 IV，无需外部随机数管理
 * - 同时提供加密和完整性校验，防止数据篡改
 * - 返回格式: base64(iv + ciphertext + tag)
 *
 * <p>本类不声明 {@code @Component}，避免被 component-scan 自动注册；
 * 由 {@link com.meowflow.common.config.CryptoConfig} 在密钥配置存在时显式创建。</p>
 */
@Slf4j
public class AESEncryptor {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits

    @Value("${meowflow.crypto.aes-key}")
    private String aesKey;

    /**
     * 加密敏感数据
     *
     * @param plaintext 明文
     * @return 密文 (base64编码: iv + ciphertext + tag)
     */
    public String encrypt(String plaintext) {
        if (StrUtil.isBlank(plaintext)) {
            return plaintext;
        }

        try {
            SecretKey key = getSecretKey();
            byte[] iv = generateIv();

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

            // 拼接 IV + 密文 (tag 已在密文中)
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("AES encryption failed", e);
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * 解密敏感数据
     *
     * @param ciphertext 密文 (base64编码)
     * @return 明文
     */
    public String decrypt(String ciphertext) {
        if (StrUtil.isBlank(ciphertext)) {
            return ciphertext;
        }

        try {
            SecretKey key = getSecretKey();
            byte[] decoded = Base64.getDecoder().decode(ciphertext);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encryptedData = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedData);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

            byte[] decrypted = cipher.doFinal(encryptedData);
            return new String(decrypted, "UTF-8");
        } catch (Exception e) {
            log.error("AES decryption failed", e);
            throw new RuntimeException("Decryption failed", e);
        }
    }

    /**
     * 生成随机 IV
     */
    private byte[] generateIv() {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }

    /**
     * 从配置获取 SecretKey
     */
    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.getDecoder().decode(aesKey);
        // 确保密钥长度为 32 字节 (AES-256)
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("AES key must be 32 bytes");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 设置密钥 (供静态方法调用)
     */
    public void setAesKey(String aesKey) {
        this.aesKey = aesKey;
    }
}
