# Kafka 消息队列

## 概述

Apache Kafka是一个分布式流处理平台，最初由LinkedIn开发，现在是Apache软件基金会的顶级项目。Kafka被设计为高吞吐量、低延迟的分布式消息系统，广泛应用于大数据处理、实时流处理和微服务架构中。

### 1. Kafka特点

- **高吞吐量**：单机可处理数十万条消息/秒
- **低延迟**：毫秒级的消息传递延迟
- **可扩展性**：支持水平扩展，可动态增加节点
- **持久性**：消息持久化存储，支持数据回放
- **容错性**：支持数据复制和故障转移
- **顺序保证**：分区内消息有序

### 2. 应用场景

- **日志收集**：收集和聚合大量日志数据
- **消息队列**：异步消息传递和解耦
- **活动跟踪**：用户行为数据收集
- **流处理**：实时数据流处理
- **事件溯源**：事件驱动架构的事件存储
- **数据管道**：不同系统间的数据传输

## 核心概念

### 1. 基本概念

- **Producer（生产者）**：发送消息到Kafka的客户端
- **Consumer（消费者）**：从Kafka读取消息的客户端
- **Broker**：Kafka服务器节点
- **Topic（主题）**：消息的分类，类似于消息队列
- **Partition（分区）**：Topic的物理分割，提高并行度
- **Offset（偏移量）**：消息在分区中的唯一标识
- **Consumer Group（消费者组）**：多个消费者的逻辑分组

### 2. 架构组件

```
┌─────────────────────────────────────────────────────────────┐
│                    Kafka Cluster                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   Broker 1  │  │   Broker 2  │  │   Broker 3  │        │
│  │             │  │             │  │             │        │
│  │  Topic A    │  │  Topic A    │  │  Topic B    │        │
│  │  Partition 0│  │  Partition 1│  │  Partition 0│        │
│  │  Partition 1│  │  Partition 2│  │  Partition 1│        │
│  └─────────────┘  └─────────────┘  └─────────────┘        │
└─────────────────────────────────────────────────────────────┘
         ▲                                    │
         │                                    ▼
┌─────────────────┐                  ┌─────────────────┐
│    Producers    │                  │    Consumers    │
│                 │                  │                 │
│  ┌───────────┐  │                  │  ┌───────────┐  │
│  │Producer 1 │  │                  │  │Consumer 1 │  │
│  └───────────┘  │                  │  └───────────┘  │
│  ┌───────────┐  │                  │  ┌───────────┐  │
│  │Producer 2 │  │                  │  │Consumer 2 │  │
│  └───────────┘  │                  │  └───────────┘  │
└─────────────────┘                  └─────────────────┘
```

### 3. 消息流转过程

1. **生产者发送消息**：Producer将消息发送到指定Topic的分区
2. **消息存储**：Broker将消息持久化到磁盘
3. **消费者拉取消息**：Consumer从分区中拉取消息
4. **偏移量管理**：Consumer提交消费偏移量

## Spring Boot集成Kafka

### 1. 添加依赖

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>
    
    <!-- Spring Kafka -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    
    <!-- JSON处理 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    
    <!-- 测试依赖 -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2. 配置文件

```yaml
# application.yml
spring:
  kafka:
    # Kafka服务器地址
    bootstrap-servers: localhost:9092
    
    # 生产者配置
    producer:
      # 序列化器
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      # 确认机制
      acks: all
      # 重试次数
      retries: 3
      # 批量大小
      batch-size: 16384
      # 缓冲区大小
      buffer-memory: 33554432
      # 压缩类型
      compression-type: gzip
      # 幂等性
      enable-idempotence: true
      
    # 消费者配置
    consumer:
      # 反序列化器
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      # 消费者组
      group-id: my-consumer-group
      # 自动提交偏移量
      enable-auto-commit: false
      # 偏移量重置策略
      auto-offset-reset: earliest
      # 最大拉取记录数
      max-poll-records: 500
      # 会话超时时间
      session-timeout-ms: 30000
      # 心跳间隔
      heartbeat-interval-ms: 10000
      # JSON反序列化配置
      properties:
        spring.json.trusted.packages: "com.example.model"
        
    # 监听器配置
    listener:
      # 确认模式
      ack-mode: manual_immediate
      # 并发数
      concurrency: 3
      # 轮询超时
      poll-timeout: 3000
      # 错误处理
      missing-topics-fatal: false
```

### 3. Kafka配置类

```java
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    /**
     * 生产者配置
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // 性能配置
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    /**
     * Kafka模板
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
    
    /**
     * 消费者配置
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "my-consumer-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // 消费者配置
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 500);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);
        
        // JSON反序列化配置
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.example.model");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.example.model.Message");
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }
    
    /**
     * 监听器容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // 并发配置
        factory.setConcurrency(3);
        
        // 确认模式
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // 错误处理
        factory.setErrorHandler((exception, data) -> {
            System.err.println("Kafka消费异常: " + exception.getMessage());
            System.err.println("消息数据: " + data);
        });
        
        return factory;
    }
}
```

## 消息发送与接收

### 1. 消息模型

```java
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Kafka消息模型
 */
public class KafkaMessage {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("data")
    private Object data;
    
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // 构造函数
    public KafkaMessage() {
        this.id = java.util.UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }
    
    public KafkaMessage(String type, Object data) {
        this();
        this.type = type;
        this.data = data;
    }
    
    // getters and setters
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public java.util.Map<String, Object> getContext() { return context; }
    public void setContext(java.util.Map<String, Object> context) { this.context = context; }
    
    @Override
    public String toString() {
        return "LogEvent{" +
                "service='" + service + '\'' +
                ", level='" + level + '\'' +
                ", message='" + message + '\'' +
                ", traceId='" + traceId + '\'' +
                ", timestamp=" + timestamp +
                ", context=" + context +
                '}';
    }
}

/**
 * 访问日志事件模型
 */
class AccessLogEvent {
    private String clientIp;
    private String method;
    private String path;
    private Integer statusCode;
    private Long responseTime;
    private String userAgent;
    private LocalDateTime timestamp;
    
    // 构造函数
    public AccessLogEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    // getters and setters
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Integer getStatusCode() { return statusCode; }
    public void setStatusCode(Integer statusCode) { this.statusCode = statusCode; }
    public Long getResponseTime() { return responseTime; }
    public void setResponseTime(Long responseTime) { this.responseTime = responseTime; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return "AccessLogEvent{" +
                "clientIp='" + clientIp + '\'' +
                ", method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", statusCode=" + statusCode +
                ", responseTime=" + responseTime +
                ", userAgent='" + userAgent + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

/**
 * 告警事件模型
 */
class AlertEvent {
    private String level;
    private String service;
    private String message;
    private LocalDateTime timestamp;
    
    // 构造函数
    public AlertEvent() {
        this.timestamp = LocalDateTime.now();
    }
    
    // getters and setters
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return "AlertEvent{" +
                "level='" + level + '\'' +
                ", service='" + service + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
```

## 性能优化

### 1. 生产者性能优化

```java
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaPerformanceConfig {
    
    /**
     * 高性能生产者配置
     */
    @Bean
    public ProducerFactory<String, Object> highPerformanceProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // 基础配置
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                       org.apache.kafka.common.serialization.StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                       org.springframework.kafka.support.serializer.JsonSerializer.class);
        
        // 性能优化配置
        configProps.put(ProducerConfig.ACKS_CONFIG, "1");  // 只需要leader确认
        configProps.put(ProducerConfig.RETRIES_CONFIG, 0);  // 不重试，提高性能
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536);  // 增大批次大小
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);  // 增加等待时间
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 67108864);  // 增大缓冲区
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");  // 使用LZ4压缩
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);  // 增加并发请求
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, Object> highPerformanceKafkaTemplate() {
        return new KafkaTemplate<>(highPerformanceProducerFactory());
    }
}
```

### 2. 消费者性能优化

```java
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerPerformanceConfig {
    
    /**
     * 高性能消费者配置
     */
    @Bean
    public ConsumerFactory<String, Object> highPerformanceConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // 基础配置
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "high-performance-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
                       org.apache.kafka.common.serialization.StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
                       org.springframework.kafka.support.serializer.JsonDeserializer.class);
        
        // 性能优化配置
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 50000);  // 增大最小拉取字节数
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);  // 增大最大等待时间
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1000);  // 增大单次拉取记录数
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 600000);  // 增大轮询间隔
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 60000);  // 增大会话超时
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 20000);  // 增大心跳间隔
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);  // 启用自动提交
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 5000);  // 自动提交间隔
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }
    
    /**
     * 高性能监听器容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> 
           highPerformanceKafkaListenerContainerFactory() {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(highPerformanceConsumerFactory());
        
        // 并发配置
        factory.setConcurrency(10);  // 增加并发数
        
        // 批量监听
        factory.setBatchListener(true);
        
        // 确认模式
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        
        // 轮询超时
        factory.getContainerProperties().setPollTimeout(3000);
        
        return factory;
    }
}
```

### 3. 批量处理优化

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class BatchProcessingService {
    
    private final Executor executor = Executors.newFixedThreadPool(10);
    
    /**
     * 批量处理消息
     */
    @KafkaListener(topics = "batch-topic", 
                   containerFactory = "highPerformanceKafkaListenerContainerFactory")
    public void handleBatchMessages(List<KafkaMessage> messages, 
                                   Acknowledgment acknowledgment) {
        try {
            System.out.println("批量处理消息，数量: " + messages.size());
            
            // 并行处理消息
            List<CompletableFuture<Void>> futures = messages.stream()
                .map(message -> CompletableFuture.runAsync(() -> {
                    processMessage(message);
                }, executor))
                .toList();
            
            // 等待所有消息处理完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    // 批量确认
                    acknowledgment.acknowledge();
                    System.out.println("批量处理完成");
                })
                .exceptionally(throwable -> {
                    System.err.println("批量处理失败: " + throwable.getMessage());
                    return null;
                });
            
        } catch (Exception e) {
            System.err.println("批量处理异常: " + e.getMessage());
        }
    }
    
    private void processMessage(KafkaMessage message) {
        // 模拟消息处理
        try {
            Thread.sleep(10);  // 模拟处理时间
            System.out.println("处理消息: " + message.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

## 集群与高可用

### 1. Kafka集群配置

```properties
# server.properties (Broker 1)
broker.id=1
listeners=PLAINTEXT://kafka1:9092
log.dirs=/var/kafka-logs
zookeeper.connect=zk1:2181,zk2:2181,zk3:2181

# 复制配置
default.replication.factor=3
min.insync.replicas=2
unclean.leader.election.enable=false

# 性能配置
num.network.threads=8
num.io.threads=16
socket.send.buffer.bytes=102400
socket.receive.buffer.bytes=102400
socket.request.max.bytes=104857600

# 日志配置
log.retention.hours=168
log.segment.bytes=1073741824
log.retention.check.interval.ms=300000
```

### 2. 生产者高可用配置

```java
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaHighAvailabilityConfig {
    
    /**
     * 高可用生产者配置
     */
    @Bean
    public ProducerFactory<String, Object> haProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // 集群配置
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                       "kafka1:9092,kafka2:9092,kafka3:9092");
        
        // 序列化配置
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, 
                       org.apache.kafka.common.serialization.StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, 
                       org.springframework.kafka.support.serializer.JsonSerializer.class);
        
        // 高可用配置
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");  // 等待所有副本确认
        configProps.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);  // 无限重试
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);  // 保证顺序
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);  // 幂等性
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000);  // 请求超时
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);  // 交付超时
        
        // 重试配置
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);  // 重试间隔
        configProps.put(ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);  // 重连间隔
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }
    
    @Bean
    public KafkaTemplate<String, Object> haKafkaTemplate() {
        return new KafkaTemplate<>(haProducerFactory());
    }
}
```

### 3. 消费者高可用配置

```java
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerHAConfig {
    
    /**
     * 高可用消费者配置
     */
    @Bean
    public ConsumerFactory<String, Object> haConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // 集群配置
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                       "kafka1:9092,kafka2:9092,kafka3:9092");
        
        // 消费者组配置
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "ha-consumer-group");
        
        // 序列化配置
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
                       org.apache.kafka.common.serialization.StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
                       org.springframework.kafka.support.serializer.JsonDeserializer.class);
        
        // 高可用配置
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);  // 手动提交
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");  // 从最早开始
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);  // 会话超时
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000);  // 心跳间隔
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000);  // 轮询间隔
        configProps.put(ConsumerConfig.CONNECTIONS_MAX_IDLE_MS_CONFIG, 540000);  // 连接空闲时间
        
        // 重试配置
        configProps.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }
    
    /**
     * 高可用监听器容器工厂
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> 
           haKafkaListenerContainerFactory() {
        
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(haConsumerFactory());
        
        // 并发配置
        factory.setConcurrency(3);
        
        // 确认模式
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        
        // 错误处理
        factory.setCommonErrorHandler(new DefaultErrorHandler(
            new FixedBackOff(1000L, 3L)  // 重试3次，间隔1秒
        ));
        
        return factory;
    }
}
```

## 最佳实践

### 1. 主题设计原则

```java
/**
 * 主题设计最佳实践
 */
public class TopicDesignBestPractices {
    
    /**
     * 主题命名规范
     * 格式: {环境}.{业务域}.{数据类型}.{版本}
     * 例如: prod.order.events.v1
     */
    public static final String ORDER_EVENTS_TOPIC = "prod.order.events.v1";
    public static final String USER_BEHAVIOR_TOPIC = "prod.user.behavior.v1";
    public static final String PAYMENT_EVENTS_TOPIC = "prod.payment.events.v1";
    
    /**
     * 分区数量设计原则
     * 1. 考虑并发消费者数量
     * 2. 考虑数据分布均匀性
     * 3. 考虑未来扩展需求
     */
    public static int calculatePartitions(int expectedConsumers, 
                                        long expectedThroughput) {
        // 基于消费者数量和吞吐量计算分区数
        int partitionsByConsumers = expectedConsumers;
        int partitionsByThroughput = (int) Math.ceil(expectedThroughput / 10000.0);
        
        // 取较大值，并考虑扩展性
        return Math.max(partitionsByConsumers, partitionsByThroughput) * 2;
    }
    
    /**
     * 副本数量设计原则
     * 1. 至少3个副本保证高可用
     * 2. 奇数个副本避免脑裂
     * 3. 不超过集群节点数
     */
    public static int calculateReplicas(int clusterSize) {
        if (clusterSize >= 5) {
            return 3;  // 大集群使用3个副本
        } else if (clusterSize >= 3) {
            return 3;  // 中等集群使用3个副本
        } else {
            return clusterSize;  // 小集群使用所有节点
        }
    }
}
```

### 2. 消息设计原则

```java
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息设计最佳实践
 */
public class MessageDesignBestPractices {
    
    /**
     * 标准消息格式
     */
    public static class StandardMessage {
        
        @JsonProperty("schema_version")
        private String schemaVersion = "1.0";  // 模式版本
        
        @JsonProperty("message_id")
        private String messageId;  // 消息唯一标识
        
        @JsonProperty("correlation_id")
        private String correlationId;  // 关联标识
        
        @JsonProperty("timestamp")
        private LocalDateTime timestamp;  // 时间戳
        
        @JsonProperty("source")
        private String source;  // 消息来源
        
        @JsonProperty("event_type")
        private String eventType;  // 事件类型
        
        @JsonProperty("payload")
        private Object payload;  // 消息载荷
        
        @JsonProperty("metadata")
        private Map<String, Object> metadata;  // 元数据
        
        // 构造函数
        public StandardMessage() {
            this.messageId = java.util.UUID.randomUUID().toString();
            this.timestamp = LocalDateTime.now();
        }
        
        // getters and setters...
    }
    
    /**
     * 消息键设计原则
     */
    public static String designMessageKey(String entityType, String entityId) {
        // 格式: {实体类型}:{实体ID}
        return entityType + ":" + entityId;
    }
    
    /**
     * 分区键设计
     */
    public static String designPartitionKey(String userId, String orderId) {
        // 使用用户ID作为分区键，保证同一用户的消息有序
        return userId;
    }
}
```

### 3. 异常处理策略

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class ExceptionHandlingBestPractices {
    
    /**
     * 带重试的消息处理
     */
    @KafkaListener(topics = "order-events")
    @Retryable(value = {Exception.class}, maxAttempts = 3, 
               backoff = @Backoff(delay = 1000, multiplier = 2))
    public void handleOrderEventWithRetry(KafkaMessage message,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                        @Header(KafkaHeaders.OFFSET) long offset,
                                        Acknowledgment acknowledgment) {
        try {
            // 处理消息
            processOrderEvent(message);
            
            // 确认消息
            acknowledgment.acknowledge();
            
        } catch (BusinessException e) {
            // 业务异常，发送到死信队列
            sendToDeadLetterQueue(message, topic, partition, offset, e);
            acknowledgment.acknowledge();  // 确认消息，避免重复处理
            
        } catch (TransientException e) {
            // 临时异常，抛出以触发重试
            System.err.println("临时异常，将重试: " + e.getMessage());
            throw e;
            
        } catch (Exception e) {
            // 未知异常，记录日志并发送告警
            System.err.println("未知异常: " + e.getMessage());
            sendAlert("UNKNOWN_ERROR", message, e);
            acknowledgment.acknowledge();  // 确认消息，避免阻塞
        }
    }
    
    /**
     * 幂等性处理
     */
    @KafkaListener(topics = "payment-events")
    public void handlePaymentEventIdempotent(KafkaMessage message,
                                            Acknowledgment acknowledgment) {
        try {
            String messageId = message.getId();
            
            // 检查消息是否已处理
            if (isMessageProcessed(messageId)) {
                System.out.println("消息已处理，跳过: " + messageId);
                acknowledgment.acknowledge();
                return;
            }
            
            // 处理消息
            processPaymentEvent(message);
            
            // 记录消息已处理
            markMessageAsProcessed(messageId);
            
            // 确认消息
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            System.err.println("处理支付事件失败: " + e.getMessage());
        }
    }
    
    private void processOrderEvent(KafkaMessage message) {
        // 模拟订单事件处理
        System.out.println("处理订单事件: " + message.getId());
    }
    
    private void processPaymentEvent(KafkaMessage message) {
        // 模拟支付事件处理
        System.out.println("处理支付事件: " + message.getId());
    }
    
    private void sendToDeadLetterQueue(KafkaMessage message, String topic, 
                                     int partition, long offset, Exception e) {
        // 发送到死信队列
        System.out.println("发送到死信队列: " + message.getId() + ", 错误: " + e.getMessage());
    }
    
    private void sendAlert(String level, KafkaMessage message, Exception e) {
        // 发送告警
        System.out.println("发送告警: " + level + ", 消息: " + message.getId() + 
                          ", 错误: " + e.getMessage());
    }
    
    private boolean isMessageProcessed(String messageId) {
        // 检查消息是否已处理（可以使用Redis、数据库等）
        return false;  // 简化实现
    }
    
    private void markMessageAsProcessed(String messageId) {
        // 标记消息已处理
        System.out.println("标记消息已处理: " + messageId);
    }
    
    // 自定义异常类
    public static class BusinessException extends Exception {
        public BusinessException(String message) {
            super(message);
        }
    }
    
    public static class TransientException extends Exception {
        public TransientException(String message) {
            super(message);
        }
    }
}
```

### 4. 监控和日志

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class MonitoringAndLoggingService {
    
    // 监控指标
    private final AtomicLong processedMessages = new AtomicLong(0);
    private final AtomicLong failedMessages = new AtomicLong(0);
    private final AtomicLong processingTime = new AtomicLong(0);
    
    /**
     * 带监控的消息处理
     */
    @KafkaListener(topics = "monitored-topic")
    public void handleMonitoredMessage(KafkaMessage message,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset,
                                     Acknowledgment acknowledgment) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 记录开始处理日志
            logMessageProcessingStart(message, topic, partition, offset);
            
            // 处理消息
            processMessage(message);
            
            // 更新成功指标
            processedMessages.incrementAndGet();
            
            // 记录处理时间
            long duration = System.currentTimeMillis() - startTime;
            processingTime.addAndGet(duration);
            
            // 记录成功日志
            logMessageProcessingSuccess(message, duration);
            
            // 确认消息
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            // 更新失败指标
            failedMessages.incrementAndGet();
            
            // 记录失败日志
            logMessageProcessingFailure(message, e);
            
            // 发送告警
            sendProcessingAlert(message, e);
            
        } finally {
            // 记录处理完成日志
            logMessageProcessingEnd(message);
        }
    }
    
    /**
     * 获取监控指标
     */
    public MonitoringMetrics getMetrics() {
        MonitoringMetrics metrics = new MonitoringMetrics();
        metrics.setProcessedMessages(processedMessages.get());
        metrics.setFailedMessages(failedMessages.get());
        metrics.setAverageProcessingTime(
            processedMessages.get() > 0 ? 
            processingTime.get() / processedMessages.get() : 0
        );
        metrics.setTimestamp(LocalDateTime.now());
        return metrics;
    }
    
    private void processMessage(KafkaMessage message) {
        // 模拟消息处理
        System.out.println("处理消息: " + message.getId());
    }
    
    private void logMessageProcessingStart(KafkaMessage message, String topic, 
                                         int partition, long offset) {
        System.out.println(String.format(
            "[%s] 开始处理消息 - 主题: %s, 分区: %d, 偏移量: %d, 消息ID: %s",
            LocalDateTime.now(), topic, partition, offset, message.getId()
        ));
    }
    
    private void logMessageProcessingSuccess(KafkaMessage message, long duration) {
        System.out.println(String.format(
            "[%s] 消息处理成功 - 消息ID: %s, 耗时: %d ms",
            LocalDateTime.now(), message.getId(), duration
        ));
    }
    
    private void logMessageProcessingFailure(KafkaMessage message, Exception e) {
        System.err.println(String.format(
            "[%s] 消息处理失败 - 消息ID: %s, 错误: %s",
            LocalDateTime.now(), message.getId(), e.getMessage()
        ));
    }
    
    private void logMessageProcessingEnd(KafkaMessage message) {
        System.out.println(String.format(
            "[%s] 消息处理结束 - 消息ID: %s",
            LocalDateTime.now(), message.getId()
        ));
    }
    
    private void sendProcessingAlert(KafkaMessage message, Exception e) {
        // 发送处理告警
        System.out.println("发送处理告警: 消息" + message.getId() + ", 错误: " + e.getMessage());
    }
    
    /**
     * 监控指标模型
     */
    public static class MonitoringMetrics {
        private long processedMessages;
        private long failedMessages;
        private long averageProcessingTime;
        private LocalDateTime timestamp;
        
        // getters and setters
        public long getProcessedMessages() { return processedMessages; }
        public void setProcessedMessages(long processedMessages) { this.processedMessages = processedMessages; }
        public long getFailedMessages() { return failedMessages; }
        public void setFailedMessages(long failedMessages) { this.failedMessages = failedMessages; }
        public long getAverageProcessingTime() { return averageProcessingTime; }
        public void setAverageProcessingTime(long averageProcessingTime) { this.averageProcessingTime = averageProcessingTime; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
        
        @Override
        public String toString() {
            return "MonitoringMetrics{" +
                    "processedMessages=" + processedMessages +
                    ", failedMessages=" + failedMessages +
                    ", averageProcessingTime=" + averageProcessingTime +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
 }
 ```

## 面试要点

### 高频问题

1. **Kafka与其他消息队列的区别？**
   - 高吞吐量：支持百万级TPS
   - 持久化存储：消息持久化到磁盘
   - 分布式架构：天然支持集群部署
   - 多消费者：支持消费者组模式
   - 顺序保证：分区内消息有序

2. **Kafka的核心概念？**
   - Producer：消息生产者
   - Consumer：消息消费者
   - Broker：Kafka服务器节点
   - Topic：消息主题
   - Partition：主题分区
   - Offset：消息偏移量
   - Consumer Group：消费者组

3. **Kafka如何保证消息不丢失？**
   - 生产者：设置acks=all，retries>0
   - Broker：设置副本数>=3，min.insync.replicas>=2
   - 消费者：手动提交offset，处理完成后再提交

4. **Kafka如何保证消息不重复？**
   - 生产者：启用幂等性（enable.idempotence=true）
   - 消费者：业务层面实现幂等性处理
   - 使用唯一消息ID进行去重

5. **Kafka分区策略？**
   - 轮询分区：默认策略，均匀分布
   - 随机分区：随机选择分区
   - 按键分区：相同key的消息发送到同一分区
   - 自定义分区：实现Partitioner接口

### 深入问题

1. **Kafka的存储机制？**
   - 日志分段：每个分区分为多个segment
   - 索引文件：.index文件用于快速定位
   - 时间索引：.timeindex文件支持按时间查找
   - 压缩策略：支持gzip、snappy、lz4等

2. **Kafka的副本机制？**
   - Leader副本：处理读写请求
   - Follower副本：同步Leader数据
   - ISR列表：同步副本集合
   - 选举机制：Leader故障时从ISR中选举

3. **Kafka的消费者组协调？**
   - 协调器：GroupCoordinator管理消费者组
   - 心跳机制：定期发送心跳保持活跃
   - 重平衡：消费者加入/离开时触发
   - 分区分配：Range、RoundRobin、Sticky策略

4. **Kafka的性能优化？**
   - 批量发送：增大batch.size和linger.ms
   - 压缩：启用消息压缩减少网络传输
   - 并发：增加生产者和消费者线程数
   - 磁盘优化：使用SSD，调整刷盘策略

### 实践经验

1. **生产环境部署经验**
   - 集群规模：至少3个Broker节点
   - 硬件配置：SSD磁盘，充足内存
   - 网络配置：千兆网络，低延迟
   - 监控告警：JMX指标监控

2. **性能调优经验**
   - 分区数量：根据吞吐量和消费者数量设计
   - 副本配置：3个副本，min.insync.replicas=2
   - 批量配置：适当增大批次大小
   - 内存配置：合理设置JVM堆内存

3. **故障处理经验**
   - 消息积压：增加消费者实例，优化处理逻辑
   - 网络分区：配置合理的超时时间
   - 磁盘满：定期清理过期日志
   - 内存溢出：调整JVM参数

4. **业务场景应用**
   - 日志收集：ELK + Kafka架构
   - 事件驱动：微服务间异步通信
   - 数据管道：实时数据处理
   - 消息通知：用户行为触发

## 总结

Kafka作为高性能的分布式消息队列，在现代微服务架构中扮演着重要角色。通过本文的学习，我们了解了：

1. **核心概念**：掌握了Producer、Consumer、Broker、Topic、Partition等核心概念
2. **Spring Boot集成**：学会了如何在Spring Boot项目中集成和使用Kafka
3. **Kafka Streams**：了解了流处理的基本概念和使用方法
4. **实际应用**：通过订单处理、用户行为分析、日志收集等场景加深理解
5. **性能优化**：掌握了生产者、消费者和批量处理的优化技巧
6. **高可用部署**：学习了集群配置和高可用方案
7. **最佳实践**：了解了主题设计、消息设计、异常处理等最佳实践

Kafka的学习是一个循序渐进的过程，建议从基础概念开始，逐步深入到高级特性和性能优化。在实际项目中，要根据业务需求选择合适的配置和架构方案，并做好监控和运维工作。

掌握Kafka不仅能够提升系统的性能和可靠性，还能为构建大规模分布式系统奠定坚实基础。
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    @Override
    public String toString() {
        return "KafkaMessage{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", timestamp=" + timestamp +
                ", source='" + source + '\'' +
                ", data=" + data +
                ", metadata=" + metadata +
                '}';
    }
}

/**
 * 订单事件模型
 */
public class OrderEvent {
    private String orderId;
    private String userId;
    private String action;  // CREATED, PAID, SHIPPED, DELIVERED, CANCELLED
    private Double amount;
    private LocalDateTime eventTime;
    
    // 构造函数
    public OrderEvent() {
        this.eventTime = LocalDateTime.now();
    }
    
    public OrderEvent(String orderId, String userId, String action, Double amount) {
        this();
        this.orderId = orderId;
        this.userId = userId;
        this.action = action;
        this.amount = amount;
    }
    
    // getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public LocalDateTime getEventTime() { return eventTime; }
    public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }
    
    @Override
    public String toString() {
        return "OrderEvent{" +
                "orderId='" + orderId + '\'' +
                ", userId='" + userId + '\'' +
                ", action='" + action + '\'' +
                ", amount=" + amount +
                ", eventTime=" + eventTime +
                '}';
    }
}
```

### 2. 消息生产者

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.concurrent.CompletableFuture;

@Service
public class KafkaProducerService {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    /**
     * 发送简单消息
     */
    public void sendMessage(String topic, Object message) {
        kafkaTemplate.send(topic, message);
        System.out.println("消息已发送到主题: " + topic + ", 内容: " + message);
    }
    
    /**
     * 发送带键的消息
     */
    public void sendMessage(String topic, String key, Object message) {
        kafkaTemplate.send(topic, key, message);
        System.out.println("消息已发送到主题: " + topic + ", 键: " + key + ", 内容: " + message);
    }
    
    /**
     * 发送消息到指定分区
     */
    public void sendMessage(String topic, Integer partition, String key, Object message) {
        kafkaTemplate.send(topic, partition, key, message);
        System.out.println("消息已发送到主题: " + topic + ", 分区: " + partition + 
                          ", 键: " + key + ", 内容: " + message);
    }
    
    /**
     * 异步发送消息（带回调）
     */
    public void sendMessageAsync(String topic, String key, Object message) {
        ListenableFuture<SendResult<String, Object>> future = 
            kafkaTemplate.send(topic, key, message);
        
        future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
            @Override
            public void onSuccess(SendResult<String, Object> result) {
                System.out.println("消息发送成功: " + message + 
                                  ", 偏移量: " + result.getRecordMetadata().offset());
            }
            
            @Override
            public void onFailure(Throwable ex) {
                System.err.println("消息发送失败: " + message + ", 错误: " + ex.getMessage());
            }
        });
    }
    
    /**
     * 同步发送消息
     */
    public SendResult<String, Object> sendMessageSync(String topic, String key, Object message) {
        try {
            SendResult<String, Object> result = kafkaTemplate.send(topic, key, message).get();
            System.out.println("同步发送成功: " + message + 
                              ", 偏移量: " + result.getRecordMetadata().offset());
            return result;
        } catch (Exception e) {
            System.err.println("同步发送失败: " + message + ", 错误: " + e.getMessage());
            throw new RuntimeException("消息发送失败", e);
        }
    }
    
    /**
     * 发送订单事件
     */
    public void sendOrderEvent(OrderEvent orderEvent) {
        KafkaMessage message = new KafkaMessage("ORDER_EVENT", orderEvent);
        message.setSource("order-service");
        
        // 使用订单ID作为键，确保同一订单的事件有序
        sendMessageAsync("order-events", orderEvent.getOrderId(), message);
    }
    
    /**
     * 批量发送消息
     */
    public void sendBatchMessages(String topic, java.util.List<Object> messages) {
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            messages.stream()
                .map(message -> {
                    ListenableFuture<SendResult<String, Object>> future = 
                        kafkaTemplate.send(topic, message);
                    return convertToCompletableFuture(future);
                })
                .toArray(CompletableFuture[]::new)
        );
        
        allFutures.thenRun(() -> {
            System.out.println("批量消息发送完成，数量: " + messages.size());
        }).exceptionally(throwable -> {
            System.err.println("批量消息发送失败: " + throwable.getMessage());
            return null;
        });
    }
    
    private CompletableFuture<SendResult<String, Object>> convertToCompletableFuture(
            ListenableFuture<SendResult<String, Object>> listenableFuture) {
        CompletableFuture<SendResult<String, Object>> completableFuture = new CompletableFuture<>();
        
        listenableFuture.addCallback(
            completableFuture::complete,
            completableFuture::completeExceptionally
        );
        
        return completableFuture;
    }
}
```

### 3. 消息消费者

```java
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KafkaConsumerService {
    
    /**
     * 简单消息消费
     */
    @KafkaListener(topics = "simple-topic")
    public void handleSimpleMessage(String message) {
        System.out.println("接收到简单消息: " + message);
        // 处理业务逻辑
        processSimpleMessage(message);
    }
    
    /**
     * 消费带确认的消息
     */
    @KafkaListener(topics = "order-events")
    public void handleOrderEvent(@Payload KafkaMessage message,
                               @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                               @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                               @Header(KafkaHeaders.OFFSET) long offset,
                               Acknowledgment acknowledgment) {
        try {
            System.out.println("接收到订单事件: " + message);
            System.out.println("主题: " + topic + ", 分区: " + partition + ", 偏移量: " + offset);
            
            // 处理订单事件
            processOrderEvent(message);
            
            // 手动确认
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            System.err.println("处理订单事件失败: " + e.getMessage());
            // 不确认消息，会重新消费
        }
    }
    
    /**
     * 消费指定分区的消息
     */
    @KafkaListener(topicPartitions = {
        @TopicPartition(topic = "user-events", partitions = {"0", "1"})
    })
    public void handleUserEventFromSpecificPartitions(ConsumerRecord<String, KafkaMessage> record,
                                                     Acknowledgment acknowledgment) {
        try {
            System.out.println("从指定分区接收用户事件: " + record.value());
            System.out.println("分区: " + record.partition() + ", 偏移量: " + record.offset());
            
            // 处理用户事件
            processUserEvent(record.value());
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            System.err.println("处理用户事件失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量消费消息
     */
    @KafkaListener(topics = "batch-topic", containerFactory = "batchKafkaListenerContainerFactory")
    public void handleBatchMessages(List<ConsumerRecord<String, KafkaMessage>> records,
                                  Acknowledgment acknowledgment) {
        try {
            System.out.println("批量接收消息，数量: " + records.size());
            
            for (ConsumerRecord<String, KafkaMessage> record : records) {
                System.out.println("处理消息: " + record.value());
                // 处理单条消息
                processBatchMessage(record.value());
            }
            
            // 批量确认
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            System.err.println("批量处理消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 多主题消费
     */
    @KafkaListener(topics = {"topic1", "topic2", "topic3"})
    public void handleMultipleTopics(@Payload KafkaMessage message,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   Acknowledgment acknowledgment) {
        try {
            System.out.println("从主题 " + topic + " 接收消息: " + message);
            
            // 根据主题处理不同逻辑
            switch (topic) {
                case "topic1":
                    processTopic1Message(message);
                    break;
                case "topic2":
                    processTopic2Message(message);
                    break;
                case "topic3":
                    processTopic3Message(message);
                    break;
                default:
                    System.out.println("未知主题: " + topic);
            }
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            System.err.println("处理多主题消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 消费者组消费
     */
    @KafkaListener(topics = "group-topic", groupId = "group1")
    public void handleMessageGroup1(KafkaMessage message, Acknowledgment acknowledgment) {
        try {
            System.out.println("消费者组1处理消息: " + message);
            processGroup1Message(message);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            System.err.println("消费者组1处理失败: " + e.getMessage());
        }
    }
    
    @KafkaListener(topics = "group-topic", groupId = "group2")
    public void handleMessageGroup2(KafkaMessage message, Acknowledgment acknowledgment) {
        try {
            System.out.println("消费者组2处理消息: " + message);
            processGroup2Message(message);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            System.err.println("消费者组2处理失败: " + e.getMessage());
        }
    }
    
    // 业务处理方法
    private void processSimpleMessage(String message) {
        // 简单消息处理逻辑
        System.out.println("处理简单消息: " + message);
    }
    
    private void processOrderEvent(KafkaMessage message) {
        // 订单事件处理逻辑
        OrderEvent orderEvent = (OrderEvent) message.getData();
        System.out.println("处理订单事件: " + orderEvent.getAction() + 
                          ", 订单ID: " + orderEvent.getOrderId());
    }
    
    private void processUserEvent(KafkaMessage message) {
        // 用户事件处理逻辑
        System.out.println("处理用户事件: " + message.getType());
    }
    
    private void processBatchMessage(KafkaMessage message) {
        // 批量消息处理逻辑
        System.out.println("批量处理消息: " + message.getId());
    }
    
    private void processTopic1Message(KafkaMessage message) {
        System.out.println("处理主题1消息: " + message.getType());
    }
    
    private void processTopic2Message(KafkaMessage message) {
        System.out.println("处理主题2消息: " + message.getType());
    }
    
    private void processTopic3Message(KafkaMessage message) {
        System.out.println("处理主题3消息: " + message.getType());
    }
    
    private void processGroup1Message(KafkaMessage message) {
        System.out.println("组1业务处理: " + message.getType());
    }
    
    private void processGroup2Message(KafkaMessage message) {
        System.out.println("组2业务处理: " + message.getType());
    }
}
```

### 4. 控制器示例

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/kafka")
public class KafkaController {
    
    @Autowired
    private KafkaProducerService producerService;
    
    /**
     * 发送简单消息
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam String topic,
            @RequestBody String message) {
        
        producerService.sendMessage(topic, message);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "消息发送成功");
        response.put("topic", topic);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 发送订单事件
     */
    @PostMapping("/order-event")
    public ResponseEntity<Map<String, Object>> sendOrderEvent(
            @RequestBody OrderEvent orderEvent) {
        
        producerService.sendOrderEvent(orderEvent);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "订单事件发送成功");
        response.put("orderId", orderEvent.getOrderId());
        response.put("action", orderEvent.getAction());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 发送带键的消息
     */
    @PostMapping("/send-with-key")
    public ResponseEntity<Map<String, Object>> sendMessageWithKey(
            @RequestParam String topic,
            @RequestParam String key,
            @RequestBody Object message) {
        
        producerService.sendMessage(topic, key, message);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "带键消息发送成功");
        response.put("topic", topic);
        response.put("key", key);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 同步发送消息
     */
    @PostMapping("/send-sync")
    public ResponseEntity<Map<String, Object>> sendMessageSync(
            @RequestParam String topic,
            @RequestParam String key,
            @RequestBody Object message) {
        
        try {
            var result = producerService.sendMessageSync(topic, key, message);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "同步消息发送成功");
            response.put("topic", topic);
            response.put("partition", result.getRecordMetadata().partition());
            response.put("offset", result.getRecordMetadata().offset());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "同步消息发送失败: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 创建订单（示例业务）
     */
    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(
            @RequestParam String userId,
            @RequestParam Double amount) {
        
        // 生成订单ID
        String orderId = "ORDER_" + System.currentTimeMillis();
        
        // 创建订单事件
        OrderEvent orderEvent = new OrderEvent(orderId, userId, "CREATED", amount);
        
        // 发送订单创建事件
        producerService.sendOrderEvent(orderEvent);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "订单创建成功");
        response.put("orderId", orderId);
        response.put("userId", userId);
        response.put("amount", amount);
        
        return ResponseEntity.ok(response);
    }
}
```

## Kafka Streams

Kafka Streams是一个用于构建实时流处理应用的客户端库。

### 1. 添加依赖

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-streams</artifactId>
</dependency>
```

### 2. Streams配置

```java
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KafkaStreamsConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Bean
    public KafkaStreams kafkaStreams() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-processing-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        
        StreamsBuilder builder = new StreamsBuilder();
        
        // 构建流处理拓扑
        buildTopology(builder);
        
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
        
        return streams;
    }
    
    private void buildTopology(StreamsBuilder builder) {
        // 订单流
        KStream<String, String> orderStream = builder.stream("order-events");
        
        // 过滤已支付订单
        KStream<String, String> paidOrders = orderStream
            .filter((key, value) -> value.contains("PAID"));
        
        // 输出到新主题
        paidOrders.to("paid-orders", Produced.with(Serdes.String(), Serdes.String()));
        
        // 订单统计
        KTable<String, Long> orderCounts = orderStream
            .groupByKey()
            .count(Materialized.as("order-counts"));
        
        // 输出统计结果
        orderCounts.toStream().to("order-statistics");
    }
}
```

### 3. 流处理示例

```java
import org.apache.kafka.streams.kstream.*;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class OrderStreamProcessor {
    
    /**
     * 处理订单流
     */
    public void processOrderStream(StreamsBuilder builder) {
        // 订单事件流
        KStream<String, OrderEvent> orderStream = builder.stream("order-events");
        
        // 1. 过滤和转换
        KStream<String, OrderEvent> validOrders = orderStream
            .filter((key, order) -> order.getAmount() > 0)
            .mapValues(order -> {
                // 添加处理时间戳
                order.setEventTime(java.time.LocalDateTime.now());
                return order;
            });
        
        // 2. 分支处理
        KStream<String, OrderEvent>[] branches = validOrders.branch(
            (key, order) -> "CREATED".equals(order.getAction()),
            (key, order) -> "PAID".equals(order.getAction()),
            (key, order) -> "SHIPPED".equals(order.getAction()),
            (key, order) -> true  // 其他所有情况
        );
        
        // 处理新创建的订单
        branches[0].to("new-orders");
        
        // 处理已支付的订单
        branches[1].to("paid-orders");
        
        // 处理已发货的订单
        branches[2].to("shipped-orders");
        
        // 3. 聚合统计
        KTable<String, Long> orderCountByUser = orderStream
            .groupBy((key, order) -> order.getUserId())
            .count();
        
        // 4. 窗口聚合
        KTable<Windowed<String>, Double> hourlyRevenue = orderStream
            .filter((key, order) -> "PAID".equals(order.getAction()))
            .groupBy((key, order) -> order.getUserId())
            .windowedBy(TimeWindows.of(Duration.ofHours(1)))
            .aggregate(
                () -> 0.0,
                (key, order, aggregate) -> aggregate + order.getAmount(),
                Materialized.with(Serdes.String(), Serdes.Double())
            );
        
        // 5. 流连接
        KStream<String, String> userStream = builder.stream("user-events");
        KStream<String, String> enrichedOrders = orderStream
            .join(userStream,
                (order, user) -> "订单: " + order.getOrderId() + ", 用户: " + user,
                JoinWindows.of(Duration.ofMinutes(5))
            );
        
        // 输出结果
        orderCountByUser.toStream().to("user-order-counts");
        hourlyRevenue.toStream().to("hourly-revenue");
        enrichedOrders.to("enriched-orders");
    }
}
```

## 实际应用场景

### 1. 订单处理系统

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderProcessingService {
    
    @Autowired
    private KafkaProducerService kafkaProducer;
    
    /**
     * 创建订单
     */
    public void createOrder(String userId, Double amount) {
        // 1. 生成订单
        String orderId = generateOrderId();
        OrderEvent orderEvent = new OrderEvent(orderId, userId, "CREATED", amount);
        
        // 2. 发送订单创建事件
        kafkaProducer.sendOrderEvent(orderEvent);
        
        System.out.println("订单创建完成: " + orderId);
    }
    
    /**
     * 支付订单
     */
    public void payOrder(String orderId, String userId, Double amount) {
        // 1. 处理支付逻辑
        boolean paymentSuccess = processPayment(orderId, amount);
        
        if (paymentSuccess) {
            // 2. 发送支付成功事件
            OrderEvent orderEvent = new OrderEvent(orderId, userId, "PAID", amount);
            kafkaProducer.sendOrderEvent(orderEvent);
            
            System.out.println("订单支付完成: " + orderId);
        } else {
            // 3. 发送支付失败事件
            OrderEvent orderEvent = new OrderEvent(orderId, userId, "PAYMENT_FAILED", amount);
            kafkaProducer.sendOrderEvent(orderEvent);
            
            System.out.println("订单支付失败: " + orderId);
        }
    }
    
    /**
     * 发货订单
     */
    public void shipOrder(String orderId, String userId) {
        // 1. 处理发货逻辑
        String trackingNumber = processShipping(orderId);
        
        // 2. 发送发货事件
        OrderEvent orderEvent = new OrderEvent(orderId, userId, "SHIPPED", null);
        kafkaProducer.sendOrderEvent(orderEvent);
        
        System.out.println("订单发货完成: " + orderId + ", 快递单号: " + trackingNumber);
    }
    
    /**
     * 取消订单
     */
    public void cancelOrder(String orderId, String userId, String reason) {
        // 1. 处理取消逻辑
        processCancellation(orderId, reason);
        
        // 2. 发送取消事件
        OrderEvent orderEvent = new OrderEvent(orderId, userId, "CANCELLED", null);
        kafkaProducer.sendOrderEvent(orderEvent);
        
        System.out.println("订单取消完成: " + orderId + ", 原因: " + reason);
    }
    
    private String generateOrderId() {
        return "ORDER_" + System.currentTimeMillis();
    }
    
    private boolean processPayment(String orderId, Double amount) {
        // 模拟支付处理
        System.out.println("处理支付: 订单" + orderId + ", 金额" + amount);
        return true;  // 假设支付成功
    }
    
    private String processShipping(String orderId) {
        // 模拟发货处理
        System.out.println("处理发货: 订单" + orderId);
        return "TRACK_" + System.currentTimeMillis();
    }
    
    private void processCancellation(String orderId, String reason) {
        // 模拟取消处理
        System.out.println("处理取消: 订单" + orderId + ", 原因" + reason);
    }
}
```

### 2. 用户行为分析

```java
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserBehaviorAnalysisService {
    
    // 用户行为统计
    private final ConcurrentHashMap<String, AtomicLong> userActionCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> pageViewCounts = new ConcurrentHashMap<>();
    
    /**
     * 处理用户行为事件
     */
    @KafkaListener(topics = "user-behavior")
    public void handleUserBehavior(UserBehaviorEvent event) {
        try {
            System.out.println("处理用户行为: " + event);
            
            // 统计用户行为
            String userId = event.getUserId();
            String action = event.getAction();
            
            // 更新用户行为计数
            userActionCounts.computeIfAbsent(userId + ":" + action, k -> new AtomicLong(0))
                           .incrementAndGet();
            
            // 处理不同类型的行为
            switch (action) {
                case "PAGE_VIEW":
                    handlePageView(event);
                    break;
                case "CLICK":
                    handleClick(event);
                    break;
                case "PURCHASE":
                    handlePurchase(event);
                    break;
                case "SEARCH":
                    handleSearch(event);
                    break;
                default:
                    System.out.println("未知行为类型: " + action);
            }
            
        } catch (Exception e) {
            System.err.println("处理用户行为失败: " + e.getMessage());
        }
    }
    
    private void handlePageView(UserBehaviorEvent event) {
        String page = (String) event.getProperties().get("page");
        if (page != null) {
            pageViewCounts.computeIfAbsent(page, k -> new AtomicLong(0))
                         .incrementAndGet();
            System.out.println("页面访问: " + page + ", 用户: " + event.getUserId());
        }
    }
    
    private void handleClick(UserBehaviorEvent event) {
        String element = (String) event.getProperties().get("element");
        System.out.println("点击事件: " + element + ", 用户: " + event.getUserId());
    }
    
    private void handlePurchase(UserBehaviorEvent event) {
        Double amount = (Double) event.getProperties().get("amount");
        String productId = (String) event.getProperties().get("productId");
        System.out.println("购买事件: 产品" + productId + ", 金额" + amount + ", 用户: " + event.getUserId());
    }
    
    private void handleSearch(UserBehaviorEvent event) {
        String keyword = (String) event.getProperties().get("keyword");
        System.out.println("搜索事件: 关键词" + keyword + ", 用户: " + event.getUserId());
    }
    
    /**
     * 获取用户行为统计
     */
    public void printStatistics() {
        System.out.println("=== 用户行为统计 ===");
        userActionCounts.forEach((key, count) -> {
            System.out.println(key + ": " + count.get());
        });
        
        System.out.println("=== 页面访问统计 ===");
        pageViewCounts.forEach((page, count) -> {
            System.out.println(page + ": " + count.get());
        });
    }
}

/**
 * 用户行为事件模型
 */
class UserBehaviorEvent {
    private String userId;
    private String sessionId;
    private String action;
    private java.time.LocalDateTime timestamp;
    private java.util.Map<String, Object> properties;
    
    // 构造函数
    public UserBehaviorEvent() {
        this.timestamp = java.time.LocalDateTime.now();
    }
    
    public UserBehaviorEvent(String userId, String action) {
        this();
        this.userId = userId;
        this.action = action;
    }
    
    // getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public java.time.LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(java.time.LocalDateTime timestamp) { this.timestamp = timestamp; }
    public java.util.Map<String, Object> getProperties() { return properties; }
    public void setProperties(java.util.Map<String, Object> properties) { this.properties = properties; }
    
    @Override
    public String toString() {
        return "UserBehaviorEvent{" +
                "userId='" + userId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", action='" + action + '\'' +
                ", timestamp=" + timestamp +
                ", properties=" + properties +
                '}';
    }
}
```

### 3. 日志收集系统

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LogCollectionService {
    
    @Autowired
    private KafkaProducerService kafkaProducer;
    
    // 日志统计
    private final ConcurrentHashMap<String, AtomicLong> logLevelCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> serviceCounts = new ConcurrentHashMap<>();
    
    /**
     * 发送应用日志
     */
    public void sendApplicationLog(String service, String level, String message, String traceId) {
        LogEvent logEvent = new LogEvent();
        logEvent.setService(service);
        logEvent.setLevel(level);
        logEvent.setMessage(message);
        logEvent.setTraceId(traceId);
        logEvent.setTimestamp(LocalDateTime.now());
        
        // 发送到日志主题
        kafkaProducer.sendMessage("application-logs", service, logEvent);
    }
    
    /**
     * 处理应用日志
     */
    @KafkaListener(topics = "application-logs")
    public void handleApplicationLog(LogEvent logEvent) {
        try {
            System.out.println("处理应用日志: " + logEvent);
            
            // 统计日志级别
            logLevelCounts.computeIfAbsent(logEvent.getLevel(), k -> new AtomicLong(0))
                         .incrementAndGet();
            
            // 统计服务日志
            serviceCounts.computeIfAbsent(logEvent.getService(), k -> new AtomicLong(0))
                        .incrementAndGet();
            
            // 根据日志级别处理
            switch (logEvent.getLevel()) {
                case "ERROR":
                    handleErrorLog(logEvent);
                    break;
                case "WARN":
                    handleWarnLog(logEvent);
                    break;
                case "INFO":
                    handleInfoLog(logEvent);
                    break;
                case "DEBUG":
                    handleDebugLog(logEvent);
                    break;
                default:
                    System.out.println("未知日志级别: " + logEvent.getLevel());
            }
            
        } catch (Exception e) {
            System.err.println("处理应用日志失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理访问日志
     */
    @KafkaListener(topics = "access-logs")
    public void handleAccessLog(AccessLogEvent accessLog) {
        try {
            System.out.println("处理访问日志: " + accessLog);
            
            // 分析访问模式
            analyzeAccessPattern(accessLog);
            
            // 检测异常访问
            detectAnomalousAccess(accessLog);
            
        } catch (Exception e) {
            System.err.println("处理访问日志失败: " + e.getMessage());
        }
    }
    
    private void handleErrorLog(LogEvent logEvent) {
        System.err.println("错误日志: " + logEvent.getMessage());
        
        // 发送告警
        sendAlert("ERROR", logEvent);
    }
    
    private void handleWarnLog(LogEvent logEvent) {
        System.out.println("警告日志: " + logEvent.getMessage());
        
        // 记录警告统计
        recordWarningStats(logEvent);
    }
    
    private void handleInfoLog(LogEvent logEvent) {
        System.out.println("信息日志: " + logEvent.getMessage());
    }
    
    private void handleDebugLog(LogEvent logEvent) {
        System.out.println("调试日志: " + logEvent.getMessage());
    }
    
    private void analyzeAccessPattern(AccessLogEvent accessLog) {
        // 分析访问模式
        System.out.println("分析访问模式: " + accessLog.getPath() + 
                          ", IP: " + accessLog.getClientIp());
    }
    
    private void detectAnomalousAccess(AccessLogEvent accessLog) {
        // 检测异常访问
        if (accessLog.getStatusCode() >= 400) {
            System.out.println("检测到异常访问: " + accessLog.getStatusCode() + 
                              ", IP: " + accessLog.getClientIp());
        }
    }
    
    private void sendAlert(String level, LogEvent logEvent) {
        // 发送告警消息
        AlertEvent alert = new AlertEvent();
        alert.setLevel(level);
        alert.setService(logEvent.getService());
        alert.setMessage("服务异常: " + logEvent.getMessage());
        alert.setTimestamp(LocalDateTime.now());
        
        kafkaProducer.sendMessage("alerts", alert);
    }
    
    private void recordWarningStats(LogEvent logEvent) {
        // 记录警告统计
        System.out.println("记录警告统计: " + logEvent.getService());
    }
    
    /**
     * 获取日志统计
     */
    public void printLogStatistics() {
        System.out.println("=== 日志级别统计 ===");
        logLevelCounts.forEach((level, count) -> {
            System.out.println(level + ": " + count.get());
        });
        
        System.out.println("=== 服务日志统计 ===");
        serviceCounts.forEach((service, count) -> {
            System.out.println(service + ": " + count.get());
        });
    }
}

/**
 * 日志事件模型
 */
class LogEvent {
    private String service;
    private String level;
    private String message;
    private String traceId;
    private LocalDateTime timestamp;
    private java.util.Map<String, Object> context;
    
    // getters an