# Java网络安全编程

## 概述

网络安全是现代应用程序开发中的关键考虑因素。Java提供了丰富的安全API和框架，帮助开发者构建安全的网络应用。本文将详细介绍Java网络安全编程的各个方面，包括加密解密、数字签名、SSL/TLS、身份认证等核心技术。

### 网络安全威胁

1. **数据窃听**：未加密的数据在传输过程中可能被截获
2. **数据篡改**：传输中的数据可能被恶意修改
3. **身份伪造**：攻击者可能冒充合法用户或服务
4. **重放攻击**：攻击者重复发送之前截获的有效数据
5. **拒绝服务**：攻击者使服务不可用
6. **中间人攻击**：攻击者在通信双方之间插入自己

### 安全目标

1. **机密性（Confidentiality）**：确保数据只能被授权用户访问
2. **完整性（Integrity）**：确保数据在传输过程中不被篡改
3. **可用性（Availability）**：确保服务在需要时可用
4. **认证性（Authentication）**：验证通信双方的身份
5. **不可否认性（Non-repudiation）**：防止发送方否认已发送的消息

## Java加密基础

### 1. 对称加密

对称加密使用相同的密钥进行加密和解密，速度快但密钥分发困难。

#### AES加密示例

```java
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.util.Base64;

public class AESEncryption {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    
    // 生成AES密钥
    public static SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(256); // 256位密钥
        return keyGenerator.generateKey();
    }
    
    // 生成随机IV
    public static byte[] generateIV() {
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
    
    // 加密
    public static String encrypt(String plainText, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        
        byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(cipherText);
    }
    
    // 解密
    public static String decrypt(String cipherText, SecretKey key, byte[] iv) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        
        byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(plainText, "UTF-8");
    }
    
    // 从字节数组创建密钥
    public static SecretKey getKeyFromBytes(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    public static void main(String[] args) {
        try {
            // 生成密钥和IV
            SecretKey key = generateKey();
            byte[] iv = generateIV();
            
            String originalText = "Hello, World! This is a secret message.";
            System.out.println("原文: " + originalText);
            
            // 加密
            String encrypted = encrypt(originalText, key, iv);
            System.out.println("加密后: " + encrypted);
            
            // 解密
            String decrypted = decrypt(encrypted, key, iv);
            System.out.println("解密后: " + decrypted);
            
            // 密钥和IV的Base64编码（用于存储或传输）
            System.out.println("密钥: " + Base64.getEncoder().encodeToString(key.getEncoded()));
            System.out.println("IV: " + Base64.getEncoder().encodeToString(iv));
            
        } catch (Exception e) {
             e.printStackTrace();
         }
     }
 }
 ```

### 2. 安全审计和日志

```java
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.security.MessageDigest;

public class SecurityAuditLogger {
    
    private static final Logger SECURITY_LOGGER = Logger.getLogger("SECURITY");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    static {
        try {
            // 配置安全日志文件
            FileHandler fileHandler = new FileHandler("security.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            SECURITY_LOGGER.addHandler(fileHandler);
            SECURITY_LOGGER.setLevel(Level.ALL);
        } catch (Exception e) {
            System.err.println("无法配置安全日志: " + e.getMessage());
        }
    }
    
    // 记录认证事件
    public static void logAuthenticationEvent(String username, String clientIP, boolean success, String reason) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String status = success ? "SUCCESS" : "FAILURE";
        
        String message = String.format("[%s] AUTHENTICATION %s - User: %s, IP: %s, Reason: %s", 
                                     timestamp, status, maskSensitiveData(username), clientIP, reason);
        
        if (success) {
            SECURITY_LOGGER.info(message);
        } else {
            SECURITY_LOGGER.warning(message);
        }
    }
    
    // 记录授权事件
    public static void logAuthorizationEvent(String username, String resource, String action, boolean granted) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String status = granted ? "GRANTED" : "DENIED";
        
        String message = String.format("[%s] AUTHORIZATION %s - User: %s, Resource: %s, Action: %s", 
                                     timestamp, status, maskSensitiveData(username), resource, action);
        
        SECURITY_LOGGER.info(message);
    }
    
    // 记录安全异常
    public static void logSecurityException(String event, String details, Exception e) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        
        String message = String.format("[%s] SECURITY_EXCEPTION - Event: %s, Details: %s, Exception: %s", 
                                     timestamp, event, details, e.getMessage());
        
        SECURITY_LOGGER.severe(message);
    }
    
    // 记录数据访问
    public static void logDataAccess(String username, String dataType, String operation, String recordId) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        
        String message = String.format("[%s] DATA_ACCESS - User: %s, Type: %s, Operation: %s, Record: %s", 
                                     timestamp, maskSensitiveData(username), dataType, operation, recordId);
        
        SECURITY_LOGGER.info(message);
    }
    
    // 掩码敏感数据
    private static String maskSensitiveData(String data) {
        if (data == null || data.length() <= 2) {
            return "***";
        }
        
        return data.charAt(0) + "***" + data.charAt(data.length() - 1);
    }
    
    // 生成会话ID
    public static String generateSessionId(String username, String clientIP) {
        try {
            String input = username + clientIP + System.currentTimeMillis();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            
            return sb.toString().substring(0, 32); // 取前32个字符
            
        } catch (Exception e) {
            return "session_" + System.currentTimeMillis();
        }
    }
}
```

## 性能优化

### 1. 加密性能优化

```java
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class CryptoPerformanceOptimization {
    
    // 1. 密码器池化
    private static final ConcurrentHashMap<String, ThreadLocal<Cipher>> CIPHER_POOL = new ConcurrentHashMap<>();
    
    // 获取线程本地密码器
    public static Cipher getCipher(String transformation) throws Exception {
        ThreadLocal<Cipher> threadLocal = CIPHER_POOL.computeIfAbsent(transformation, 
            k -> ThreadLocal.withInitial(() -> {
                try {
                    return Cipher.getInstance(k);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        
        return threadLocal.get();
    }
    
    // 2. 批量加密
    public static class BatchEncryption {
        private final Cipher cipher;
        private final SecretKeySpec keySpec;
        
        public BatchEncryption(String algorithm, byte[] key) throws Exception {
            this.cipher = Cipher.getInstance(algorithm);
            this.keySpec = new SecretKeySpec(key, algorithm.split("/")[0]);
        }
        
        public byte[][] encryptBatch(byte[][] plaintexts) throws Exception {
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            
            byte[][] results = new byte[plaintexts.length][];
            for (int i = 0; i < plaintexts.length; i++) {
                results[i] = cipher.doFinal(plaintexts[i]);
            }
            
            return results;
        }
    }
    
    // 3. 随机数生成优化
    public static class OptimizedRandomGenerator {
        private static final ThreadLocal<SecureRandom> SECURE_RANDOM = 
            ThreadLocal.withInitial(SecureRandom::new);
        
        // 对于非安全关键场景，使用ThreadLocalRandom
        public static byte[] generateNonCriticalRandom(int size) {
            byte[] bytes = new byte[size];
            ThreadLocalRandom.current().nextBytes(bytes);
            return bytes;
        }
        
        // 对于安全关键场景，使用SecureRandom
        public static byte[] generateSecureRandom(int size) {
            byte[] bytes = new byte[size];
            SECURE_RANDOM.get().nextBytes(bytes);
            return bytes;
        }
    }
    
    // 4. 内存优化
    public static class MemoryOptimization {
        
        // 重用缓冲区
        private static final ThreadLocal<byte[]> BUFFER_POOL = 
            ThreadLocal.withInitial(() -> new byte[8192]);
        
        public static byte[] getBuffer(int minSize) {
            byte[] buffer = BUFFER_POOL.get();
            if (buffer.length < minSize) {
                buffer = new byte[minSize];
                BUFFER_POOL.set(buffer);
            }
            return buffer;
        }
        
        // 清理缓冲区
        public static void clearBuffer() {
            byte[] buffer = BUFFER_POOL.get();
            if (buffer != null) {
                java.util.Arrays.fill(buffer, (byte) 0);
            }
        }
    }
    
    // 性能测试
    public static void performanceTest() {
        try {
            int iterations = 10000;
            byte[] data = "Hello, World! This is a test message for encryption.".getBytes();
            byte[] key = OptimizedRandomGenerator.generateSecureRandom(16);
            
            // 测试普通加密
            long startTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                Cipher cipher = Cipher.getInstance("AES");
                SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                cipher.doFinal(data);
            }
            long normalTime = System.nanoTime() - startTime;
            
            // 测试优化后的加密
            startTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                Cipher cipher = getCipher("AES");
                SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                cipher.doFinal(data);
            }
            long optimizedTime = System.nanoTime() - startTime;
            
            System.out.printf("普通加密耗时: %.2f ms%n", normalTime / 1_000_000.0);
            System.out.printf("优化加密耗时: %.2f ms%n", optimizedTime / 1_000_000.0);
            System.out.printf("性能提升: %.2f%%%n", (normalTime - optimizedTime) * 100.0 / normalTime);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        performanceTest();
    }
}
```

### 2. SSL/TLS性能优化

```java
import javax.net.ssl.*;
import java.security.KeyStore;
import java.util.concurrent.ConcurrentHashMap;

public class SSLPerformanceOptimization {
    
    // SSL上下文缓存
    private static final ConcurrentHashMap<String, SSLContext> SSL_CONTEXT_CACHE = new ConcurrentHashMap<>();
    
    // 获取缓存的SSL上下文
    public static SSLContext getCachedSSLContext(String protocol, KeyStore keyStore, 
                                                char[] keyStorePassword, KeyStore trustStore) throws Exception {
        String cacheKey = protocol + "_" + System.identityHashCode(keyStore) + "_" + System.identityHashCode(trustStore);
        
        return SSL_CONTEXT_CACHE.computeIfAbsent(cacheKey, k -> {
            try {
                SSLContext context = SSLContext.getInstance(protocol);
                
                // 初始化KeyManager
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, keyStorePassword);
                
                // 初始化TrustManager
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(trustStore);
                
                context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                return context;
                
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    
    // SSL会话缓存配置
    public static void configureSSLSessionCache(SSLContext sslContext) {
        SSLSessionContext sessionContext = sslContext.getServerSessionContext();
        
        // 设置会话缓存大小
        sessionContext.setSessionCacheSize(1000);
        
        // 设置会话超时时间（秒）
        sessionContext.setSessionTimeout(3600); // 1小时
    }
    
    // 优化的SSL套接字工厂
    public static class OptimizedSSLSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;
        
        public OptimizedSSLSocketFactory(SSLContext sslContext) {
            this.delegate = sslContext.getSocketFactory();
        }
        
        @Override
        public SSLSocket createSocket() throws IOException {
            SSLSocket socket = (SSLSocket) delegate.createSocket();
            optimizeSocket(socket);
            return socket;
        }
        
        @Override
        public SSLSocket createSocket(String host, int port) throws IOException {
            SSLSocket socket = (SSLSocket) delegate.createSocket(host, port);
            optimizeSocket(socket);
            return socket;
        }
        
        // 其他createSocket方法的实现...
        
        private void optimizeSocket(SSLSocket socket) {
            // 启用会话重用
            socket.setUseClientMode(true);
            
            // 设置首选的密码套件
            String[] preferredCipherSuites = {
                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_RSA_WITH_AES_256_GCM_SHA384",
                "TLS_RSA_WITH_AES_128_GCM_SHA256"
            };
            
            socket.setEnabledCipherSuites(preferredCipherSuites);
            
            // 设置协议版本
            socket.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
        }
        
        @Override
        public String[] getDefaultCipherSuites() {
            return delegate.getDefaultCipherSuites();
        }
        
        @Override
        public String[] getSupportedCipherSuites() {
            return delegate.getSupportedCipherSuites();
        }
        
        // 其他必需方法的实现...
    }
}
```

## 面试要点

### 高频问题

1. **对称加密和非对称加密的区别？**
   - 对称加密：使用相同密钥加密和解密，速度快，适合大量数据
   - 非对称加密：使用公钥私钥对，安全性高，适合密钥交换和数字签名

2. **HTTPS的工作原理？**
   - SSL/TLS握手过程
   - 证书验证
   - 密钥交换
   - 数据加密传输

3. **如何防止常见的网络攻击？**
   - SQL注入：参数化查询、输入验证
   - XSS：输出编码、CSP策略
   - CSRF：Token验证、SameSite Cookie
   - 中间人攻击：证书验证、HSTS

### 深入问题

1. **JWT的安全性考虑？**
   - 签名验证
   - 过期时间设置
   - 敏感信息不放在载荷中
   - 密钥管理

2. **如何实现安全的会话管理？**
   - 会话ID的生成和管理
   - 会话固定攻击防护
   - 会话超时处理
   - 安全的会话存储

3. **数字证书的验证过程？**
   - 证书链验证
   - 证书有效期检查
   - 证书撤销列表（CRL）检查
   - 证书透明度日志

### 实践经验

1. **在项目中如何选择加密算法？**
   - 根据性能要求选择
   - 考虑安全强度
   - 兼容性考虑
   - 合规性要求

2. **如何处理密钥管理？**
   - 密钥生成和分发
   - 密钥轮换策略
   - 密钥存储安全
   - 密钥销毁

3. **网络安全监控和审计？**
   - 安全日志记录
   - 异常检测
   - 入侵检测系统
   - 安全事件响应

## 总结

Java网络安全编程是构建安全可靠网络应用的基础。本文详细介绍了：

### 核心技术
- **加密技术**：对称加密、非对称加密、消息摘要、HMAC
- **SSL/TLS**：安全传输层协议的实现和优化
- **身份认证**：Basic认证、JWT、OAuth 2.0等认证机制
- **数字证书**：PKI体系、证书操作、KeyStore管理

### 安全实践
- **安全编程原则**：输入验证、安全存储、数据清理
- **性能优化**：加密性能优化、SSL优化、内存管理
- **审计日志**：安全事件记录、监控和分析

### 学习建议
1. **理论基础**：深入理解密码学原理和网络安全概念
2. **实践应用**：通过项目实践掌握各种安全技术的应用
3. **持续学习**：关注最新的安全威胁和防护技术
4. **合规意识**：了解相关的安全标准和法规要求

网络安全是一个不断发展的领域，需要持续学习和实践。掌握这些核心技术和最佳实践，能够帮助开发者构建更加安全可靠的网络应用。

#### DES和3DES加密

```java
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class DESEncryption {
    
    // DES加密
    public static class DES {
        private static final String ALGORITHM = "DES";
        
        public static SecretKey generateKey() throws Exception {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            return keyGenerator.generateKey();
        }
        
        public static String encrypt(String plainText, SecretKey key) throws Exception {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        }
        
        public static String decrypt(String cipherText, SecretKey key) throws Exception {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted);
        }
    }
    
    // 3DES加密
    public static class TripleDES {
        private static final String ALGORITHM = "DESede";
        
        public static SecretKey generateKey() throws Exception {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            return keyGenerator.generateKey();
        }
        
        public static String encrypt(String plainText, SecretKey key) throws Exception {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(plainText.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        }
        
        public static String decrypt(String cipherText, SecretKey key) throws Exception {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted);
        }
    }
}
```

### 2. 非对称加密

非对称加密使用公钥和私钥对，公钥加密私钥解密，或私钥签名公钥验证。

#### RSA加密示例

```java
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import java.util.Base64;

public class RSAEncryption {
    
    private static final String ALGORITHM = "RSA";
    private static final String TRANSFORMATION = "RSA/ECB/PKCS1Padding";
    
    // 生成RSA密钥对
    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(2048); // 2048位密钥
        return keyPairGenerator.generateKeyPair();
    }
    
    // 公钥加密
    public static String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    // 私钥解密
    public static String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
        return new String(decrypted, "UTF-8");
    }
    
    // 私钥签名
    public static String sign(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes("UTF-8"));
        byte[] signed = signature.sign();
        return Base64.getEncoder().encodeToString(signed);
    }
    
    // 公钥验证签名
    public static boolean verify(String data, String signatureStr, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes("UTF-8"));
        byte[] signatureBytes = Base64.getDecoder().decode(signatureStr);
        return signature.verify(signatureBytes);
    }
    
    // 将公钥转换为字符串
    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    // 将私钥转换为字符串
    public static String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
    
    // 从字符串恢复公钥
    public static PublicKey stringToPublicKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(spec);
    }
    
    // 从字符串恢复私钥
    public static PrivateKey stringToPrivateKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }
    
    public static void main(String[] args) {
        try {
            // 生成密钥对
            KeyPair keyPair = generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            
            String originalText = "Hello, RSA Encryption!";
            System.out.println("原文: " + originalText);
            
            // 加密
            String encrypted = encrypt(originalText, publicKey);
            System.out.println("加密后: " + encrypted);
            
            // 解密
            String decrypted = decrypt(encrypted, privateKey);
            System.out.println("解密后: " + decrypted);
            
            // 签名
            String signature = sign(originalText, privateKey);
            System.out.println("签名: " + signature);
            
            // 验证签名
            boolean isValid = verify(originalText, signature, publicKey);
            System.out.println("签名验证: " + isValid);
            
            // 密钥的字符串表示
            System.out.println("公钥: " + publicKeyToString(publicKey));
            System.out.println("私钥: " + privateKeyToString(privateKey));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 3. 消息摘要

消息摘要用于验证数据完整性，常用的算法有MD5、SHA-1、SHA-256等。

```java
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class MessageDigestExample {
    
    // MD5摘要
    public static String md5(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    }
    
    // SHA-1摘要
    public static String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    }
    
    // SHA-256摘要
    public static String sha256(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    }
    
    // SHA-512摘要
    public static String sha512(String input) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(digest);
    }
    
    // 字节数组转十六进制字符串
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    // 文件摘要计算
    public static String fileDigest(String filePath, String algorithm) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        
        try (FileInputStream fis = new FileInputStream(filePath);
             DigestInputStream dis = new DigestInputStream(fis, md)) {
            
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                // 读取文件内容，DigestInputStream会自动更新摘要
            }
        }
        
        return bytesToHex(md.digest());
    }
    
    public static void main(String[] args) {
        try {
            String input = "Hello, World!";
            
            System.out.println("原文: " + input);
            System.out.println("MD5: " + md5(input));
            System.out.println("SHA-1: " + sha1(input));
            System.out.println("SHA-256: " + sha256(input));
            System.out.println("SHA-512: " + sha512(input));
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 4. HMAC（Hash-based Message Authentication Code）

HMAC结合了哈希函数和密钥，用于验证消息的完整性和真实性。

```java
import javax.crypto.Mac;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class HMACExample {
    
    // 生成HMAC密钥
    public static SecretKey generateHMACKey(String algorithm) throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(algorithm);
        return keyGenerator.generateKey();
    }
    
    // 计算HMAC
    public static String calculateHMAC(String data, SecretKey key, String algorithm) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(algorithm);
        mac.init(key);
        byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmac);
    }
    
    // 验证HMAC
    public static boolean verifyHMAC(String data, String expectedHMAC, SecretKey key, String algorithm) 
            throws NoSuchAlgorithmException, InvalidKeyException {
        String calculatedHMAC = calculateHMAC(data, key, algorithm);
        return calculatedHMAC.equals(expectedHMAC);
    }
    
    // 从字节数组创建密钥
    public static SecretKey createKeyFromBytes(byte[] keyBytes, String algorithm) {
        return new SecretKeySpec(keyBytes, algorithm);
    }
    
    public static void main(String[] args) {
        try {
            String data = "Hello, HMAC!";
            String algorithm = "HmacSHA256";
            
            // 生成密钥
            SecretKey key = generateHMACKey(algorithm);
            
            // 计算HMAC
            String hmac = calculateHMAC(data, key, algorithm);
            System.out.println("数据: " + data);
            System.out.println("HMAC: " + hmac);
            
            // 验证HMAC
            boolean isValid = verifyHMAC(data, hmac, key, algorithm);
            System.out.println("HMAC验证: " + isValid);
            
            // 密钥的Base64编码
            String keyStr = Base64.getEncoder().encodeToString(key.getEncoded());
            System.out.println("密钥: " + keyStr);
            
            // 不同算法的HMAC
            System.out.println("\n不同算法的HMAC:");
            String[] algorithms = {"HmacMD5", "HmacSHA1", "HmacSHA256", "HmacSHA512"};
            
            for (String alg : algorithms) {
                SecretKey algKey = generateHMACKey(alg);
                String algHmac = calculateHMAC(data, algKey, alg);
                System.out.println(alg + ": " + algHmac);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## SSL/TLS编程

### 1. SSL/TLS基础

SSL/TLS是保护网络通信安全的标准协议，提供加密、身份验证和数据完整性保护。

#### SSL Socket编程

```java
import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

// SSL服务器
public class SSLServer {
    
    private static final int PORT = 8443;
    
    public static void main(String[] args) {
        try {
            // 加载服务器证书和私钥
            System.setProperty("javax.net.ssl.keyStore", "server.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "password");
            
            // 创建SSL服务器套接字工厂
            SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
            SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(PORT);
            
            // 设置需要客户端认证
            serverSocket.setNeedClientAuth(true);
            
            System.out.println("SSL服务器启动，监听端口: " + PORT);
            
            while (true) {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void handleClient(SSLSocket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {
            
            // 开始SSL握手
            clientSocket.startHandshake();
            
            // 获取客户端证书
            SSLSession session = clientSocket.getSession();
            X509Certificate[] clientCerts = (X509Certificate[]) session.getPeerCertificates();
            
            System.out.println("客户端连接: " + clientSocket.getRemoteSocketAddress());
            System.out.println("SSL协议: " + session.getProtocol());
            System.out.println("加密套件: " + session.getCipherSuite());
            
            if (clientCerts.length > 0) {
                System.out.println("客户端证书: " + clientCerts[0].getSubjectDN());
            }
            
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("收到消息: " + inputLine);
                out.println("Echo: " + inputLine);
                
                if ("bye".equalsIgnoreCase(inputLine)) {
                    break;
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

// SSL客户端
public class SSLClient {
    
    private static final String HOST = "localhost";
    private static final int PORT = 8443;
    
    public static void main(String[] args) {
        try {
            // 加载客户端证书和私钥
            System.setProperty("javax.net.ssl.keyStore", "client.jks");
            System.setProperty("javax.net.ssl.keyStorePassword", "password");
            
            // 加载信任的CA证书
            System.setProperty("javax.net.ssl.trustStore", "truststore.jks");
            System.setProperty("javax.net.ssl.trustStorePassword", "password");
            
            // 创建SSL套接字工厂
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket socket = (SSLSocket) factory.createSocket(HOST, PORT);
            
            // 开始SSL握手
            socket.startHandshake();
            
            // 获取SSL会话信息
            SSLSession session = socket.getSession();
            System.out.println("连接到: " + HOST + ":" + PORT);
            System.out.println("SSL协议: " + session.getProtocol());
            System.out.println("加密套件: " + session.getCipherSuite());
            
            // 获取服务器证书
            X509Certificate[] serverCerts = (X509Certificate[]) session.getPeerCertificates();
            if (serverCerts.length > 0) {
                System.out.println("服务器证书: " + serverCerts[0].getSubjectDN());
            }
            
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {
                
                System.out.println("输入消息 (输入 'bye' 退出):");
                
                String inputLine;
                while ((inputLine = userInput.readLine()) != null) {
                    out.println(inputLine);
                    System.out.println("服务器响应: " + in.readLine());
                    
                    if ("bye".equalsIgnoreCase(inputLine)) {
                        break;
                    }
                }
            }
            
            socket.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 自定义TrustManager

```java
import javax.net.ssl.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

// 自定义信任管理器
public class CustomTrustManager implements X509TrustManager {
    
    private final X509TrustManager defaultTrustManager;
    
    public CustomTrustManager() throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        
        TrustManager[] trustManagers = factory.getTrustManagers();
        for (TrustManager tm : trustManagers) {
            if (tm instanceof X509TrustManager) {
                this.defaultTrustManager = (X509TrustManager) tm;
                return;
            }
        }
        
        throw new Exception("无法找到默认的X509TrustManager");
    }
    
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            defaultTrustManager.checkClientTrusted(chain, authType);
        } catch (CertificateException e) {
            // 自定义验证逻辑
            System.out.println("客户端证书验证失败，使用自定义验证: " + e.getMessage());
            
            // 这里可以添加自定义的证书验证逻辑
            if (isCustomTrusted(chain)) {
                System.out.println("自定义验证通过");
                return;
            }
            
            throw e;
        }
    }
    
    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        try {
            defaultTrustManager.checkServerTrusted(chain, authType);
        } catch (CertificateException e) {
            // 自定义验证逻辑
            System.out.println("服务器证书验证失败，使用自定义验证: " + e.getMessage());
            
            // 这里可以添加自定义的证书验证逻辑
            if (isCustomTrusted(chain)) {
                System.out.println("自定义验证通过");
                return;
            }
            
            throw e;
        }
    }
    
    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return defaultTrustManager.getAcceptedIssuers();
    }
    
    // 自定义信任验证逻辑
    private boolean isCustomTrusted(X509Certificate[] chain) {
        if (chain == null || chain.length == 0) {
            return false;
        }
        
        X509Certificate cert = chain[0];
        
        // 检查证书有效期
        try {
            cert.checkValidity();
        } catch (CertificateException e) {
            System.out.println("证书已过期或尚未生效");
            return false;
        }
        
        // 检查证书主题
        String subject = cert.getSubjectDN().getName();
        System.out.println("证书主题: " + subject);
        
        // 这里可以添加更多自定义验证逻辑
        // 例如：检查特定的CN、组织等
        
        return true; // 根据实际需求返回验证结果
    }
}

// 使用自定义TrustManager的SSL客户端
public class CustomSSLClient {
    
    public static void main(String[] args) {
        try {
            // 创建自定义TrustManager
            TrustManager[] trustManagers = {new CustomTrustManager()};
            
            // 创建SSL上下文
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, new java.security.SecureRandom());
            
            // 设置默认SSL套接字工厂
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            
            // 或者直接使用SSL套接字
            SSLSocketFactory factory = sslContext.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket("localhost", 8443);
            
            // 进行SSL通信...
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 2. HTTPS客户端

```java
import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.cert.X509Certificate;

public class HTTPSClient {
    
    // 忽略SSL证书验证的TrustManager（仅用于测试）
    private static class TrustAllTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
            // 不进行验证
        }
        
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
            // 不进行验证
        }
        
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
    
    // 忽略主机名验证的HostnameVerifier（仅用于测试）
    private static class TrustAllHostnameVerifier implements HostnameVerifier {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true; // 总是返回true
        }
    }
    
    // 配置忽略SSL验证（仅用于测试环境）
    public static void disableSSLVerification() {
        try {
            TrustManager[] trustAllCerts = {new TrustAllTrustManager()};
            
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new TrustAllHostnameVerifier());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 发送HTTPS GET请求
    public static String sendGET(String url) throws Exception {
        URL obj = new URL(url);
        HttpsURLConnection connection = (HttpsURLConnection) obj.openConnection();
        
        // 设置请求方法
        connection.setRequestMethod("GET");
        
        // 设置请求头
        connection.setRequestProperty("User-Agent", "Java HTTPS Client");
        connection.setRequestProperty("Accept", "application/json");
        
        // 获取响应码
        int responseCode = connection.getResponseCode();
        System.out.println("响应码: " + responseCode);
        
        // 读取响应
        BufferedReader in = new BufferedReader(new InputStreamReader(
            responseCode >= 200 && responseCode < 300 ? 
            connection.getInputStream() : connection.getErrorStream()));
        
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine).append("\n");
        }
        in.close();
        
        // 获取SSL信息
        SSLSession session = connection.getSSLSession();
        System.out.println("SSL协议: " + session.getProtocol());
        System.out.println("加密套件: " + session.getCipherSuite());
        
        return response.toString();
    }
    
    // 发送HTTPS POST请求
    public static String sendPOST(String url, String postData) throws Exception {
        URL obj = new URL(url);
        HttpsURLConnection connection = (HttpsURLConnection) obj.openConnection();
        
        // 设置请求方法
        connection.setRequestMethod("POST");
        
        // 设置请求头
        connection.setRequestProperty("User-Agent", "Java HTTPS Client");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        
        // 启用输出
        connection.setDoOutput(true);
        
        // 发送POST数据
        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            wr.writeBytes(postData);
            wr.flush();
        }
        
        // 获取响应码
        int responseCode = connection.getResponseCode();
        System.out.println("响应码: " + responseCode);
        
        // 读取响应
        BufferedReader in = new BufferedReader(new InputStreamReader(
            responseCode >= 200 && responseCode < 300 ? 
            connection.getInputStream() : connection.getErrorStream()));
        
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine).append("\n");
        }
        in.close();
        
        return response.toString();
    }
    
    public static void main(String[] args) {
        try {
            // 注意：在生产环境中不要禁用SSL验证
            // disableSSLVerification();
            
            // 发送HTTPS GET请求
            String getResponse = sendGET("https://httpbin.org/get");
            System.out.println("GET响应:");
            System.out.println(getResponse);
            
            // 发送HTTPS POST请求
            String postData = "{\"name\":\"John\",\"age\":30}";
            String postResponse = sendPOST("https://httpbin.org/post", postData);
            System.out.println("POST响应:");
            System.out.println(postResponse);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 身份认证和授权

### 1. 基本认证（Basic Authentication）

```java
import java.util.Base64;
import java.io.*;
import java.net.*;
import javax.net.ssl.HttpsURLConnection;

public class BasicAuthExample {
    
    // 生成Basic认证头
    public static String createBasicAuthHeader(String username, String password) {
        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        return "Basic " + encodedCredentials;
    }
    
    // 发送带Basic认证的HTTP请求
    public static String sendAuthenticatedRequest(String url, String username, String password) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        
        // 设置Basic认证头
        String authHeader = createBasicAuthHeader(username, password);
        connection.setRequestProperty("Authorization", authHeader);
        
        // 设置其他请求头
        connection.setRequestProperty("User-Agent", "Java HTTP Client");
        connection.setRequestMethod("GET");
        
        int responseCode = connection.getResponseCode();
        System.out.println("响应码: " + responseCode);
        
        // 读取响应
        BufferedReader in = new BufferedReader(new InputStreamReader(
            responseCode >= 200 && responseCode < 300 ? 
            connection.getInputStream() : connection.getErrorStream()));
        
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine).append("\n");
        }
        in.close();
        
        return response.toString();
    }
    
    // 简单的Basic认证服务器
    public static class BasicAuthServer {
        
        private static final String VALID_USERNAME = "admin";
        private static final String VALID_PASSWORD = "password";
        
        public static void main(String[] args) throws Exception {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            
            server.createContext("/protected", exchange -> {
                String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
                
                if (authHeader == null || !authHeader.startsWith("Basic ")) {
                    sendUnauthorized(exchange);
                    return;
                }
                
                String encodedCredentials = authHeader.substring(6);
                String credentials = new String(Base64.getDecoder().decode(encodedCredentials));
                String[] parts = credentials.split(":", 2);
                
                if (parts.length != 2 || !VALID_USERNAME.equals(parts[0]) || !VALID_PASSWORD.equals(parts[1])) {
                    sendUnauthorized(exchange);
                    return;
                }
                
                // 认证成功
                String response = "Hello, " + parts[0] + "! You are authenticated.";
                exchange.sendResponseHeaders(200, response.length());
                
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });
            
            server.setExecutor(null);
            server.start();
            
            System.out.println("Basic认证服务器启动，监听端口8080");
        }
        
        private static void sendUnauthorized(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"Protected Area\"");
            String response = "Unauthorized";
            exchange.sendResponseHeaders(401, response.length());
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            // 测试Basic认证
            String response = sendAuthenticatedRequest(
                "https://httpbin.org/basic-auth/user/pass", 
                "user", 
                "pass"
            );
            
            System.out.println("认证响应:");
            System.out.println(response);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 2. JWT（JSON Web Token）认证

```java
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JWTExample {
    
    private static final String SECRET_KEY = "mySecretKey123456789";
    private static final String ALGORITHM = "HmacSHA256";
    
    // JWT头部
    private static final String JWT_HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    
    // 生成JWT
    public static String generateJWT(String subject, String issuer, long expirationMinutes) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        
        // 创建载荷
        ObjectNode payload = mapper.createObjectNode();
        payload.put("sub", subject);
        payload.put("iss", issuer);
        payload.put("iat", Instant.now().getEpochSecond());
        payload.put("exp", Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES).getEpochSecond());
        
        // Base64编码头部和载荷
        String encodedHeader = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(JWT_HEADER.getBytes());
        String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mapper.writeValueAsString(payload).getBytes());
        
        // 创建签名
        String data = encodedHeader + "." + encodedPayload;
        String signature = createSignature(data, SECRET_KEY);
        
        return data + "." + signature;
    }
    
    // 验证JWT
    public static boolean validateJWT(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        
        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];
        
        // 验证签名
        String data = header + "." + payload;
        String expectedSignature = createSignature(data, SECRET_KEY);
        
        if (!signature.equals(expectedSignature)) {
            return false;
        }
        
        // 验证过期时间
        ObjectMapper mapper = new ObjectMapper();
        String payloadJson = new String(Base64.getUrlDecoder().decode(payload));
        ObjectNode payloadNode = (ObjectNode) mapper.readTree(payloadJson);
        
        long exp = payloadNode.get("exp").asLong();
        long now = Instant.now().getEpochSecond();
        
        return now < exp;
    }
    
    // 解析JWT载荷
    public static String parseJWTPayload(String jwt) throws Exception {
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT format");
        }
        
        String payload = parts[1];
        return new String(Base64.getUrlDecoder().decode(payload));
    }
    
    // 创建HMAC签名
    private static String createSignature(String data, String key) throws Exception {
        Mac mac = Mac.getInstance(ALGORITHM);
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(), ALGORITHM);
        mac.init(secretKeySpec);
        
        byte[] signature = mac.doFinal(data.getBytes());
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }
    
    // JWT认证过滤器示例
    public static class JWTAuthFilter {
        
        public boolean authenticate(String authHeader) {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return false;
            }
            
            String jwt = authHeader.substring(7);
            
            try {
                return validateJWT(jwt);
            } catch (Exception e) {
                System.err.println("JWT验证失败: " + e.getMessage());
                return false;
            }
        }
        
        public String extractSubject(String authHeader) throws Exception {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }
            
            String jwt = authHeader.substring(7);
            String payloadJson = parseJWTPayload(jwt);
            
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode payload = (ObjectNode) mapper.readTree(payloadJson);
            
            return payload.get("sub").asText();
        }
    }
    
    public static void main(String[] args) {
        try {
            // 生成JWT
            String jwt = generateJWT("user123", "myapp", 60); // 60分钟过期
            System.out.println("生成的JWT: " + jwt);
            
            // 验证JWT
            boolean isValid = validateJWT(jwt);
            System.out.println("JWT验证结果: " + isValid);
            
            // 解析JWT载荷
            String payload = parseJWTPayload(jwt);
            System.out.println("JWT载荷: " + payload);
            
            // 测试认证过滤器
            JWTAuthFilter filter = new JWTAuthFilter();
            String authHeader = "Bearer " + jwt;
            
            boolean authenticated = filter.authenticate(authHeader);
            System.out.println("认证结果: " + authenticated);
            
            if (authenticated) {
                String subject = filter.extractSubject(authHeader);
                System.out.println("用户: " + subject);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 3. OAuth 2.0客户端

```java
import java.io.*;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

public class OAuth2Client {
    
    private final String clientId;
    private final String clientSecret;
    private final String authorizationEndpoint;
    private final String tokenEndpoint;
    private final String redirectUri;
    
    public OAuth2Client(String clientId, String clientSecret, 
                       String authorizationEndpoint, String tokenEndpoint, 
                       String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizationEndpoint = authorizationEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.redirectUri = redirectUri;
    }
    
    // 生成授权URL
    public String getAuthorizationUrl(String scope, String state) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("response_type", "code");
        params.put("client_id", clientId);
        params.put("redirect_uri", redirectUri);
        params.put("scope", scope);
        params.put("state", state);
        
        return authorizationEndpoint + "?" + buildQueryString(params);
    }
    
    // 使用授权码获取访问令牌
    public TokenResponse getAccessToken(String authorizationCode) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "authorization_code");
        params.put("code", authorizationCode);
        params.put("redirect_uri", redirectUri);
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        
        String response = sendPostRequest(tokenEndpoint, params);
        return parseTokenResponse(response);
    }
    
    // 刷新访问令牌
    public TokenResponse refreshAccessToken(String refreshToken) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        
        String response = sendPostRequest(tokenEndpoint, params);
        return parseTokenResponse(response);
    }
    
    // 使用客户端凭据获取访问令牌
    public TokenResponse getClientCredentialsToken(String scope) throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("grant_type", "client_credentials");
        params.put("scope", scope);
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);
        
        String response = sendPostRequest(tokenEndpoint, params);
        return parseTokenResponse(response);
    }
    
    // 使用访问令牌调用API
    public String callAPI(String apiUrl, String accessToken) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", "Bearer " + accessToken);
        connection.setRequestProperty("Accept", "application/json");
        
        int responseCode = connection.getResponseCode();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(
            responseCode >= 200 && responseCode < 300 ? 
            connection.getInputStream() : connection.getErrorStream()));
        
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine).append("\n");
        }
        in.close();
        
        if (responseCode >= 400) {
            throw new Exception("API调用失败: " + responseCode + " - " + response.toString());
        }
        
        return response.toString();
    }
    
    // 发送POST请求
    private String sendPostRequest(String url, Map<String, String> params) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        
        String postData = buildQueryString(params);
        
        try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
            wr.writeBytes(postData);
            wr.flush();
        }
        
        int responseCode = connection.getResponseCode();
        
        BufferedReader in = new BufferedReader(new InputStreamReader(
            responseCode >= 200 && responseCode < 300 ? 
            connection.getInputStream() : connection.getErrorStream()));
        
        String inputLine;
        StringBuilder response = new StringBuilder();
        
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine).append("\n");
        }
        in.close();
        
        if (responseCode >= 400) {
            throw new Exception("请求失败: " + responseCode + " - " + response.toString());
        }
        
        return response.toString();
    }
    
    // 构建查询字符串
    private String buildQueryString(Map<String, String> params) throws Exception {
        StringBuilder result = new StringBuilder();
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (result.length() > 0) {
                result.append("&");
            }
            
            result.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                  .append("=")
                  .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        
        return result.toString();
    }
    
    // 解析令牌响应
    private TokenResponse parseTokenResponse(String response) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response);
        
        String accessToken = jsonNode.get("access_token").asText();
        String tokenType = jsonNode.has("token_type") ? jsonNode.get("token_type").asText() : "Bearer";
        int expiresIn = jsonNode.has("expires_in") ? jsonNode.get("expires_in").asInt() : 3600;
        String refreshToken = jsonNode.has("refresh_token") ? jsonNode.get("refresh_token").asText() : null;
        String scope = jsonNode.has("scope") ? jsonNode.get("scope").asText() : null;
        
        return new TokenResponse(accessToken, tokenType, expiresIn, refreshToken, scope);
    }
    
    // 令牌响应类
    public static class TokenResponse {
        private final String accessToken;
        private final String tokenType;
        private final int expiresIn;
        private final String refreshToken;
        private final String scope;
        
        public TokenResponse(String accessToken, String tokenType, int expiresIn, 
                           String refreshToken, String scope) {
            this.accessToken = accessToken;
            this.tokenType = tokenType;
            this.expiresIn = expiresIn;
            this.refreshToken = refreshToken;
            this.scope = scope;
        }
        
        // Getter方法
        public String getAccessToken() { return accessToken; }
        public String getTokenType() { return tokenType; }
        public int getExpiresIn() { return expiresIn; }
        public String getRefreshToken() { return refreshToken; }
        public String getScope() { return scope; }
        
        @Override
        public String toString() {
            return String.format("TokenResponse{accessToken='%s', tokenType='%s', expiresIn=%d, refreshToken='%s', scope='%s'}",
                    accessToken, tokenType, expiresIn, refreshToken, scope);
        }
    }
    
    public static void main(String[] args) {
        try {
            // GitHub OAuth示例
            OAuth2Client client = new OAuth2Client(
                "your_client_id",
                "your_client_secret",
                "https://github.com/login/oauth/authorize",
                "https://github.com/login/oauth/access_token",
                "http://localhost:8080/callback"
            );
            
            // 生成授权URL
            String authUrl = client.getAuthorizationUrl("user:email", "random_state_123");
            System.out.println("授权URL: " + authUrl);
            
            // 注意：在实际应用中，用户会被重定向到授权URL，
            // 然后授权服务器会将用户重定向回redirect_uri并带上授权码
            
            // 假设我们已经获得了授权码
            // String authCode = "received_authorization_code";
            // TokenResponse tokenResponse = client.getAccessToken(authCode);
            // System.out.println("令牌响应: " + tokenResponse);
            
            // 使用访问令牌调用API
            // String apiResponse = client.callAPI("https://api.github.com/user", tokenResponse.getAccessToken());
            // System.out.println("API响应: " + apiResponse);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 数字证书和PKI

### 1. 证书操作

```java
import java.security.*;
import java.security.cert.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.io.*;
import java.math.BigInteger;
import java.util.Date;
import java.util.Calendar;
import javax.security.auth.x500.X500Principal;

public class CertificateExample {
    
    // 生成自签名证书
    public static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String subjectDN, int validityDays) throws Exception {
        // 注意：这是一个简化的示例，实际生产环境中应使用专业的证书生成库
        // 如Bouncy Castle等
        
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        
        // 创建证书信息
        X500Principal subject = new X500Principal(subjectDN);
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        
        Calendar calendar = Calendar.getInstance();
        Date notBefore = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, validityDays);
        Date notAfter = calendar.getTime();
        
        // 注意：以下代码需要Bouncy Castle库支持
        // 这里仅作为示例说明证书生成的基本流程
        
        System.out.println("证书主题: " + subject);
        System.out.println("序列号: " + serialNumber);
        System.out.println("有效期: " + notBefore + " 到 " + notAfter);
        
        // 实际实现需要使用专业库
        return null; // 占位符
    }
    
    // 加载证书
    public static X509Certificate loadCertificate(String certPath) throws Exception {
        try (FileInputStream fis = new FileInputStream(certPath)) {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            return (X509Certificate) cf.generateCertificate(fis);
        }
    }
    
    // 验证证书
    public static boolean verifyCertificate(X509Certificate cert, PublicKey issuerPublicKey) {
        try {
            // 验证证书签名
            cert.verify(issuerPublicKey);
            
            // 检查证书有效期
            cert.checkValidity();
            
            System.out.println("证书验证成功");
            return true;
            
        } catch (Exception e) {
            System.err.println("证书验证失败: " + e.getMessage());
            return false;
        }
    }
    
    // 获取证书信息
    public static void printCertificateInfo(X509Certificate cert) {
        System.out.println("=== 证书信息 ===");
        System.out.println("主题: " + cert.getSubjectDN());
        System.out.println("颁发者: " + cert.getIssuerDN());
        System.out.println("序列号: " + cert.getSerialNumber());
        System.out.println("版本: " + cert.getVersion());
        System.out.println("签名算法: " + cert.getSigAlgName());
        System.out.println("有效期从: " + cert.getNotBefore());
        System.out.println("有效期到: " + cert.getNotAfter());
        System.out.println("公钥算法: " + cert.getPublicKey().getAlgorithm());
    }
    
    // 证书链验证
    public static boolean verifyCertificateChain(X509Certificate[] certChain, X509Certificate rootCert) {
        try {
            for (int i = 0; i < certChain.length - 1; i++) {
                X509Certificate cert = certChain[i];
                X509Certificate issuer = certChain[i + 1];
                
                // 验证当前证书是否由下一个证书签发
                cert.verify(issuer.getPublicKey());
                cert.checkValidity();
            }
            
            // 验证最后一个证书是否由根证书签发
            if (certChain.length > 0) {
                X509Certificate lastCert = certChain[certChain.length - 1];
                lastCert.verify(rootCert.getPublicKey());
                lastCert.checkValidity();
            }
            
            System.out.println("证书链验证成功");
            return true;
            
        } catch (Exception e) {
            System.err.println("证书链验证失败: " + e.getMessage());
            return false;
        }
    }
    
    public static void main(String[] args) {
        try {
            // 生成密钥对
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            
            // 生成自签名证书（示例）
            String subjectDN = "CN=Test Certificate, O=Test Org, C=US";
            generateSelfSignedCertificate(keyPair, subjectDN, 365);
            
            // 加载和验证证书（需要实际的证书文件）
            // X509Certificate cert = loadCertificate("path/to/certificate.crt");
            // printCertificateInfo(cert);
            // boolean isValid = verifyCertificate(cert, cert.getPublicKey());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 2. KeyStore操作

```java
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.io.*;
import java.util.Enumeration;

public class KeyStoreExample {
    
    // 创建KeyStore
    public static KeyStore createKeyStore(String type, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type);
        keyStore.load(null, password.toCharArray());
        return keyStore;
    }
    
    // 加载KeyStore
    public static KeyStore loadKeyStore(String keystorePath, String type, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance(type);
        
        try (FileInputStream fis = new FileInputStream(keystorePath)) {
            keyStore.load(fis, password.toCharArray());
        }
        
        return keyStore;
    }
    
    // 保存KeyStore
    public static void saveKeyStore(KeyStore keyStore, String keystorePath, String password) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
            keyStore.store(fos, password.toCharArray());
        }
    }
    
    // 添加私钥和证书
    public static void addPrivateKey(KeyStore keyStore, String alias, PrivateKey privateKey, 
                                   String keyPassword, Certificate[] certChain) throws Exception {
        keyStore.setKeyEntry(alias, privateKey, keyPassword.toCharArray(), certChain);
    }
    
    // 添加证书
    public static void addCertificate(KeyStore keyStore, String alias, Certificate cert) throws Exception {
        keyStore.setCertificateEntry(alias, cert);
    }
    
    // 获取私钥
    public static PrivateKey getPrivateKey(KeyStore keyStore, String alias, String keyPassword) throws Exception {
        return (PrivateKey) keyStore.getKey(alias, keyPassword.toCharArray());
    }
    
    // 获取证书
    public static Certificate getCertificate(KeyStore keyStore, String alias) throws Exception {
        return keyStore.getCertificate(alias);
    }
    
    // 列出KeyStore中的所有别名
    public static void listAliases(KeyStore keyStore) throws Exception {
        System.out.println("=== KeyStore别名列表 ===");
        
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            boolean isKeyEntry = keyStore.isKeyEntry(alias);
            boolean isCertEntry = keyStore.isCertificateEntry(alias);
            
            System.out.printf("别名: %s, 私钥: %s, 证书: %s%n", alias, isKeyEntry, isCertEntry);
            
            if (isCertEntry || isKeyEntry) {
                Certificate cert = keyStore.getCertificate(alias);
                if (cert instanceof X509Certificate) {
                    X509Certificate x509 = (X509Certificate) cert;
                    System.out.printf("  主题: %s%n", x509.getSubjectDN());
                    System.out.printf("  颁发者: %s%n", x509.getIssuerDN());
                }
            }
        }
    }
    
    // 删除条目
    public static void deleteEntry(KeyStore keyStore, String alias) throws Exception {
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias);
            System.out.println("已删除别名: " + alias);
        } else {
            System.out.println("别名不存在: " + alias);
        }
    }
    
    // 创建信任库
    public static KeyStore createTrustStore(Certificate[] trustedCerts, String password) throws Exception {
        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, password.toCharArray());
        
        for (int i = 0; i < trustedCerts.length; i++) {
            trustStore.setCertificateEntry("trusted_" + i, trustedCerts[i]);
        }
        
        return trustStore;
    }
    
    public static void main(String[] args) {
        try {
            String keystorePassword = "keystorepass";
            String keyPassword = "keypass";
            String keystorePath = "example.jks";
            
            // 创建新的KeyStore
            KeyStore keyStore = createKeyStore("JKS", keystorePassword);
            
            // 生成密钥对
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            
            // 创建自签名证书（这里需要实际的证书生成代码）
            // Certificate[] certChain = {selfSignedCert};
            
            // 添加私钥和证书到KeyStore
            // addPrivateKey(keyStore, "mykey", keyPair.getPrivate(), keyPassword, certChain);
            
            // 保存KeyStore
            // saveKeyStore(keyStore, keystorePath, keystorePassword);
            
            // 加载KeyStore
            // KeyStore loadedKeyStore = loadKeyStore(keystorePath, "JKS", keystorePassword);
            
            // 列出别名
            // listAliases(loadedKeyStore);
            
            System.out.println("KeyStore操作示例完成");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 网络安全最佳实践

### 1. 安全编程原则

```java
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class SecurityBestPractices {
    
    // 1. 使用安全的随机数生成器
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    // 2. 安全的密码存储
    public static class PasswordSecurity {
        
        // 生成盐值
        public static byte[] generateSalt() {
            byte[] salt = new byte[16];
            SECURE_RANDOM.nextBytes(salt);
            return salt;
        }
        
        // 哈希密码（使用盐值）
        public static String hashPassword(String password, byte[] salt) throws Exception {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes("UTF-8"));
            
            // 将盐值和哈希值组合
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);
            
            return Base64.getEncoder().encodeToString(combined);
        }
        
        // 验证密码
        public static boolean verifyPassword(String password, String storedHash) throws Exception {
            byte[] combined = Base64.getDecoder().decode(storedHash);
            
            // 提取盐值
            byte[] salt = new byte[16];
            System.arraycopy(combined, 0, salt, 0, 16);
            
            // 提取存储的哈希值
            byte[] storedPasswordHash = new byte[combined.length - 16];
            System.arraycopy(combined, 16, storedPasswordHash, 0, storedPasswordHash.length);
            
            // 计算输入密码的哈希值
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt);
            byte[] inputPasswordHash = md.digest(password.getBytes("UTF-8"));
            
            // 比较哈希值
            return Arrays.equals(storedPasswordHash, inputPasswordHash);
        }
    }
    
    // 3. 安全的数据清理
    public static class SecureDataCleanup {
        
        // 安全清理字节数组
        public static void clearByteArray(byte[] data) {
            if (data != null) {
                Arrays.fill(data, (byte) 0);
            }
        }
        
        // 安全清理字符数组
        public static void clearCharArray(char[] data) {
            if (data != null) {
                Arrays.fill(data, '\0');
            }
        }
        
        // 使用try-with-resources的安全字符数组
        public static class SecureCharArray implements AutoCloseable {
            private final char[] data;
            
            public SecureCharArray(int size) {
                this.data = new char[size];
            }
            
            public SecureCharArray(char[] data) {
                this.data = Arrays.copyOf(data, data.length);
            }
            
            public char[] getData() {
                return data;
            }
            
            @Override
            public void close() {
                clearCharArray(data);
            }
        }
    }
    
    // 4. 输入验证和清理
    public static class InputValidation {
        
        // SQL注入防护
        public static String sanitizeForSQL(String input) {
            if (input == null) {
                return null;
            }
            
            // 移除或转义危险字符
            return input.replaceAll("[';\"\\-]", "");
        }
        
        // XSS防护
        public static String sanitizeForHTML(String input) {
            if (input == null) {
                return null;
            }
            
            return input.replace("&", "&amp;")
                       .replace("<", "&lt;")
                       .replace(">", "&gt;")
                       .replace("\"", "&quot;")
                       .replace("'", "&#x27;");
        }
        
        // 路径遍历攻击防护
        public static boolean isValidPath(String path) {
            if (path == null) {
                return false;
            }
            
            // 检查路径遍历攻击
            return !path.contains("../") && !path.contains("..\\")
                   && !path.startsWith("/") && !path.contains(":");
        }
        
        // 文件名验证
        public static boolean isValidFileName(String fileName) {
            if (fileName == null || fileName.trim().isEmpty()) {
                return false;
            }
            
            // 检查非法字符
            String invalidChars = "<>:\"/\\|?*";
            for (char c : invalidChars.toCharArray()) {
                if (fileName.indexOf(c) >= 0) {
                    return false;
                }
            }
            
            return true;
        }
    }
    
    // 5. 安全的配置管理
    public static class SecureConfiguration {
        
        // 从环境变量读取敏感配置
        public static String getSecretFromEnv(String key) {
            String value = System.getenv(key);
            if (value == null) {
                throw new IllegalStateException("必需的环境变量未设置: " + key);
            }
            return value;
        }
        
        // 验证配置的安全性
        public static void validateSecurityConfig() {
            // 检查SSL/TLS配置
            String[] requiredProps = {
                "javax.net.ssl.keyStore",
                "javax.net.ssl.trustStore"
            };
            
            for (String prop : requiredProps) {
                if (System.getProperty(prop) == null) {
                    System.err.println("警告: 未设置SSL属性 " + prop);
                }
            }
            
            // 检查加密强度
            try {
                int maxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
                if (maxKeyLength < 256) {
                    System.err.println("警告: AES密钥长度受限，建议安装JCE无限强度策略文件");
                }
            } catch (Exception e) {
                System.err.println("无法检查加密强度: " + e.getMessage());
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            // 密码安全示例
            String password = "mySecretPassword";
            byte[] salt = PasswordSecurity.generateSalt();
            String hashedPassword = PasswordSecurity.hashPassword(password, salt);
            
            System.out.println("原密码: " + password);
            System.out.println("哈希后: " + hashedPassword);
            
            boolean isValid = PasswordSecurity.verifyPassword(password, hashedPassword);
            System.out.println("密码验证: " + isValid);
            
            // 安全数据清理示例
            try (SecureDataCleanup.SecureCharArray secureArray = 
                 new SecureDataCleanup.SecureCharArray(password.toCharArray())) {
                
                // 使用安全字符数组
                char[] data = secureArray.getData();
                System.out.println("使用安全字符数组: " + new String(data));
                
                // 自动清理
            }
            
            // 输入验证示例
            String userInput = "<script>alert('xss')</script>";
            String sanitized = InputValidation.sanitizeForHTML(userInput);
            System.out.println("原输入: " + userInput);
            System.out.println("清理后: " + sanitized);
            
            // 配置验证
            SecureConfiguration.validateSecurityConfig();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```