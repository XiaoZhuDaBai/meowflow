# 字段级加密组件

## 概述

喵流项目实现了基于 AES-256-GCM 的字段级加密组件，用于保护敏感数据（如 API Key、Webhook Secret 等）。

## 核心组件

### 1. AESEncryptor (`com.meowflow.common.security.AESEncryptor`)

使用 AES-GCM 模式提供 authenticated encryption：
- **自动 IV 生成**：每次加密自动生成随机 12 字节 IV
- **完整性校验**：GCM 模式自带 tag 校验，防止数据篡改
- **返回格式**：`base64(iv + ciphertext + tag)`

### 2. TypeHandler

| 处理器 | 类型 | 用途 |
|--------|------|------|
| `EncryptedStringTypeHandler` | String | 处理加密字符串字段 |
| `EncryptedLongTypeHandler` | Long | 处理加密 Long 字段（转为 String 存储） |

### 3. @Encrypted 注解

标记需要加密存储的字段，配合 TypeHandler 使用。

## 使用示例

```java
@TableName("mf_ai_model")
public class AIModelEntity {

    private String name;

    @Encrypted
    @TableField(typeHandler = EncryptedStringTypeHandler.class)
    private String apiKey;
}
```

## 配置

### application.yml

```yaml
meowflow:
  crypto:
    aes-key: ${CRYPTO_AES_KEY:dGhpcyBpcyBhIDMyLWJ5dGUga2V5ISEh}
    key-id: key-v1
```

### 生成 AES 密钥

```bash
# 生成 32 字节随机密钥并 base64 编码
openssl rand -base64 32
```

> **关键点**：`AESEncryptor` 同时带有 `@Component` 注解，会被任何 `@ComponentScan("com.meowflow.common")` 的业务模块自动注册为 Bean。如果你的业务模块（meowflow-template / meowflow-user / meowflow-workflow 等）的 `application.yml` / `application-{profile}.yml` 没有配置 `meowflow.crypto.aes-key`，启动时会报：
>
> ```
> Could not resolve placeholder 'meowflow.crypto.aes-key' in value "${meowflow.crypto.aes-key}"
> ```
>
> 推荐做法：
> - 在 `application-{profile}.yml` 中提供默认值
> - 生产环境强制使用 `${MEOWFLOW_CRYPTO_AES_KEY}` 环境变量或 Nacos 配置覆盖
> - 跨服务保持同一 key 以便测试加解密互通

## 密钥管理最佳实践

### 开发环境
- 使用默认密钥或本地 `.env` 文件
- 密钥可以硬编码在配置中（仅用于开发）

### 生产环境

#### 方案一：环境变量
```yaml
meowflow:
  crypto:
    aes-key: ${CRYPTO_AES_KEY}
```

#### 方案二：密钥管理服务

**AWS KMS**
```java
@Bean
public AESEncryptor aesEncryptor() {
    String key = awsKmsClient.decrypt(keyId);
    AESEncryptor encryptor = new AESEncryptor();
    encryptor.setAesKey(key);
    return encryptor;
}
```

**Azure Key Vault**
```java
@Bean
public AESEncryptor aesEncryptor() {
    SecretClient secretClient = new SecretClientBuilder()
        .vaultUrl(vaultUrl)
        .credential(credential)
        .buildClient();
    String key = secretClient.getSecret(keyId).getValue();
    AESEncryptor encryptor = new AESEncryptor();
    encryptor.setAesKey(key);
    return encryptor;
}
```

**HashiCorp Vault**
```java
@Bean
public AESEncryptor aesEncryptor() {
    VaultTemplate vaultTemplate = new VaultTemplate(vaultAddress, token);
    String key = vaultTemplate.opsForVersionedKey("secret", "meowflow-aes-key");
    AESEncryptor encryptor = new AESEncryptor();
    encryptor.setAesKey(key);
    return encryptor;
}
```

### 密钥轮换

当需要轮换密钥时：

1. 在配置中添加新版密钥：
```yaml
meowflow:
  crypto:
    aes-key: ${CRYPTO_AES_KEY_V2}
    key-id: key-v2
```

2. 实现多版本密钥支持：
```java
@Component
public class MultiVersionAESEncryptor {

    private final Map<String, AESEncryptor> encryptors = new HashMap<>();

    public String encrypt(String plaintext, String keyId) {
        return encryptors.get(keyId).encrypt(plaintext);
    }

    public String decrypt(String ciphertext) {
        // 解析 ciphertext 中的 keyId 或尝试所有密钥
    }
}
```

3. 渐进式迁移现有数据

## 注意事项

1. **性能影响**：加密/解密操作会带来一定性能开销，建议仅对敏感字段使用
2. **索引限制**：加密后的字段无法建索引，如需查询可考虑：
   - 使用加密字段的哈希值建索引
   - 引入搜索服务（如 Elasticsearch）
3. **备份安全**：数据库备份同样包含加密数据，需确保备份文件安全
4. **日志保护**：确保敏感字段不会出现在日志中
5. **双重注册风险**：`AESEncryptor` 既是 `@Component` 又被 `CryptoConfig.aesEncryptor()` 注册为 `@Bean`，Spring 会因 Bean 名冲突导致启动失败或行为不确定。后续重构建议去掉其中一个，二选一：
   - 仅保留 `@Component` + `@Value`（适合需要 @ConfigurationProperties 解密的场景不友好）
   - 仅保留 `@Bean` + `ConfigurationProperties`（推荐，配置更集中、便于接入 KMS / Vault）
