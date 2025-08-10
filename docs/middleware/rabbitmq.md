# RabbitMQ消息队列

## 目录
- [RabbitMQ概述](#rabbitmq概述)
- [RabbitMQ核心概念](#rabbitmq核心概念)
- [Spring Boot集成RabbitMQ](#spring-boot集成rabbitmq)
- [消息发送与接收](#消息发送与接收)
- [交换机类型详解](#交换机类型详解)
- [消息可靠性保证](#消息可靠性保证)
- [死信队列](#死信队列)
- [延迟队列](#延迟队列)
- [集群与高可用](#集群与高可用)
- [性能优化](#性能优化)
- [最佳实践](#最佳实践)
- [面试要点](#面试要点)
- [总结](#总结)

## RabbitMQ概述

### 什么是RabbitMQ

RabbitMQ是一个开源的消息代理和队列服务器，用于在分布式系统中存储和转发消息。它实现了高级消息队列协议（AMQP），提供了可靠、灵活的消息传递机制。

### 主要特点

1. **可靠性**：支持消息持久化、确认机制、事务
2. **灵活的路由**：支持多种交换机类型和路由策略
3. **集群支持**：支持集群部署，提供高可用性
4. **多协议支持**：支持AMQP、STOMP、MQTT等协议
5. **管理界面**：提供Web管理界面，便于监控和管理
6. **插件系统**：丰富的插件生态系统

### 应用场景

- **异步处理**：邮件发送、短信通知
- **应用解耦**：订单系统与库存系统解耦
- **流量削峰**：秒杀活动、大促销
- **数据同步**：多系统间数据同步
- **日志收集**：分布式日志收集
- **任务队列**：后台任务处理

## RabbitMQ核心概念

### 基本组件

```java
/**
 * RabbitMQ核心概念说明
 */
public class RabbitMQConcepts {
    
    /**
     * Producer（生产者）：发送消息的应用程序
     * Consumer（消费者）：接收消息的应用程序
     * Queue（队列）：存储消息的缓冲区
     * Exchange（交换机）：接收生产者发送的消息，并根据路由规则将消息路由到队列
     * Binding（绑定）：交换机和队列之间的路由规则
     * Routing Key（路由键）：生产者发送消息时指定的路由标识
     * Connection（连接）：应用程序与RabbitMQ服务器的TCP连接
     * Channel（信道）：在连接内部建立的逻辑连接
     * Virtual Host（虚拟主机）：用于逻辑隔离的虚拟分组
     */
    
    // 交换机类型
    public enum ExchangeType {
        DIRECT,    // 直连交换机：完全匹配路由键
        TOPIC,     // 主题交换机：模式匹配路由键
        FANOUT,    // 扇形交换机：广播到所有绑定的队列
        HEADERS    // 头交换机：根据消息头属性路由
    }
    
    // 消息属性
    public static class MessageProperties {
        private String messageId;      // 消息ID
        private String correlationId;  // 关联ID
        private String replyTo;        // 回复队列
        private Integer priority;      // 优先级
        private Long timestamp;        // 时间戳
        private String type;           // 消息类型
        private String userId;         // 用户ID
        private String appId;          // 应用ID
        private String clusterId;      // 集群ID
        private Integer deliveryMode;  // 投递模式（1=非持久化，2=持久化）
        private Long expiration;       // 过期时间
        private Map<String, Object> headers; // 自定义头信息
        
        // getters and setters...
    }
}
```

### 消息流转过程

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ消息流转示例配置
 */
@Configuration
public class RabbitMQFlowExample {
    
    // 1. 声明交换机
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("order.exchange", true, false);
    }
    
    // 2. 声明队列
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable("order.queue")
                .withArgument("x-message-ttl", 60000) // 消息TTL
                .withArgument("x-max-length", 1000)   // 队列最大长度
                .build();
    }
    
    // 3. 绑定交换机和队列
    @Bean
    public Binding orderBinding() {
        return BindingBuilder
                .bind(orderQueue())
                .to(orderExchange())
                .with("order.created");
    }
    
    /**
     * 消息流转过程：
     * 1. Producer发送消息到Exchange
     * 2. Exchange根据Routing Key和Binding规则路由消息到Queue
     * 3. Consumer从Queue中消费消息
     * 4. Consumer处理完成后发送ACK确认
     */
}
```

## Spring Boot集成RabbitMQ

### 1. 添加依赖

```xml
<!-- pom.xml -->
<dependencies>
    <!-- Spring Boot Starter AMQP -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-amqp</artifactId>
    </dependency>
    
    <!-- Spring Boot Starter Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- JSON处理 -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

### 2. 配置文件

```yaml
# application.yml
spring:
  rabbitmq:
    # 连接配置
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    
    # 连接池配置
    connection-timeout: 15000
    
    # 生产者配置
    publisher-confirm-type: correlated  # 确认模式
    publisher-returns: true             # 开启return机制
    
    # 消费者配置
    listener:
      simple:
        # 手动确认模式
        acknowledge-mode: manual
        # 并发消费者数量
        concurrency: 1
        max-concurrency: 10
        # 预取数量
        prefetch: 1
        # 重试配置
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
          max-interval: 10000
          multiplier: 1.0
    
    # 模板配置
    template:
      mandatory: true
      receive-timeout: 5000
      reply-timeout: 5000
```

### 3. RabbitMQ配置类

```java
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    /**
     * 消息转换器 - 使用JSON格式
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * RabbitTemplate配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        
        // 设置确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                System.out.println("消息发送成功: " + correlationData);
            } else {
                System.err.println("消息发送失败: " + cause);
            }
        });
        
        // 设置返回回调
        template.setReturnsCallback(returned -> {
            System.err.println("消息被退回: " + returned.getMessage());
            System.err.println("退回原因: " + returned.getReplyText());
            System.err.println("交换机: " + returned.getExchange());
            System.err.println("路由键: " + returned.getRoutingKey());
        });
        
        return template;
    }
    
    /**
     * 监听器容器工厂配置
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        
        // 设置并发消费者数量
        factory.setConcurrentConsumers(1);
        factory.setMaxConcurrentConsumers(10);
        
        // 设置预取数量
        factory.setPrefetchCount(1);
        
        return factory;
    }
}
```

## 消息发送与接收

### 1. 消息生产者

```java
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MessageProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * 发送简单消息
     */
    public void sendSimpleMessage(String exchange, String routingKey, Object message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
    
    /**
     * 发送带属性的消息
     */
    public void sendMessageWithProperties(String exchange, String routingKey, Object message) {
        MessageProperties properties = new MessageProperties();
        properties.setMessageId(UUID.randomUUID().toString());
        properties.setTimestamp(new java.util.Date());
        properties.setContentType("application/json");
        properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT); // 持久化
        properties.setPriority(1); // 优先级
        
        Message msg = new Message(message.toString().getBytes(), properties);
        rabbitTemplate.send(exchange, routingKey, msg);
    }
    
    /**
     * 发送延迟消息
     */
    public void sendDelayMessage(String exchange, String routingKey, Object message, long delayMs) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message, msg -> {
            msg.getMessageProperties().setDelay((int) delayMs);
            return msg;
        });
    }
    
    /**
     * 批量发送消息
     */
    public void sendBatchMessages(String exchange, String routingKey, java.util.List<Object> messages) {
        for (Object message : messages) {
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
        }
    }
}

// 订单消息生产者示例
@Service
public class OrderMessageProducer {
    
    @Autowired
    private MessageProducer messageProducer;
    
    private static final String ORDER_EXCHANGE = "order.exchange";
    
    /**
     * 发送订单创建消息
     */
    public void sendOrderCreated(OrderCreatedEvent event) {
        messageProducer.sendSimpleMessage(ORDER_EXCHANGE, "order.created", event);
    }
    
    /**
     * 发送订单支付消息
     */
    public void sendOrderPaid(OrderPaidEvent event) {
        messageProducer.sendSimpleMessage(ORDER_EXCHANGE, "order.paid", event);
    }
    
    /**
     * 发送订单取消消息
     */
    public void sendOrderCancelled(OrderCancelledEvent event) {
        messageProducer.sendSimpleMessage(ORDER_EXCHANGE, "order.cancelled", event);
    }
}

// 事件对象
public class OrderCreatedEvent {
    private String orderId;
    private String userId;
    private Double amount;
    private LocalDateTime createTime;
    
    // constructors, getters and setters...
    public OrderCreatedEvent() {}
    
    public OrderCreatedEvent(String orderId, String userId, Double amount) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.createTime = LocalDateTime.now();
    }
    
    // getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}

public class OrderPaidEvent {
    private String orderId;
    private String paymentId;
    private Double amount;
    private LocalDateTime paidTime;
    
    // constructors, getters and setters...
}

public class OrderCancelledEvent {
    private String orderId;
    private String reason;
    private LocalDateTime cancelTime;
    
    // constructors, getters and setters...
}
```

### 2. 消息消费者

```java
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OrderMessageConsumer {
    
    /**
     * 监听订单创建消息
     */
    @RabbitListener(queues = "order.created.queue")
    public void handleOrderCreated(@Payload OrderCreatedEvent event,
                                 Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            System.out.println("处理订单创建事件: " + event.getOrderId());
            
            // 业务处理逻辑
            processOrderCreated(event);
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            System.err.println("处理订单创建事件失败: " + e.getMessage());
            try {
                // 拒绝消息并重新入队
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                System.err.println("消息确认失败: " + ioException.getMessage());
            }
        }
    }
    
    /**
     * 监听订单支付消息
     */
    @RabbitListener(queues = "order.paid.queue")
    public void handleOrderPaid(@Payload OrderPaidEvent event,
                              Channel channel,
                              @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            System.out.println("处理订单支付事件: " + event.getOrderId());
            
            // 业务处理逻辑
            processOrderPaid(event);
            
            // 手动确认消息
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            System.err.println("处理订单支付事件失败: " + e.getMessage());
            try {
                // 拒绝消息，不重新入队（发送到死信队列）
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                System.err.println("消息确认失败: " + ioException.getMessage());
            }
        }
    }
    
    /**
     * 监听订单取消消息（自动确认模式）
     */
    @RabbitListener(queues = "order.cancelled.queue", 
                   containerFactory = "autoAckRabbitListenerContainerFactory")
    public void handleOrderCancelled(@Payload OrderCancelledEvent event) {
        System.out.println("处理订单取消事件: " + event.getOrderId());
        
        // 业务处理逻辑
        processOrderCancelled(event);
    }
    
    /**
     * 批量消息处理
     */
    @RabbitListener(queues = "order.batch.queue")
    public void handleBatchMessages(@Payload java.util.List<OrderCreatedEvent> events,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            System.out.println("批量处理订单事件，数量: " + events.size());
            
            for (OrderCreatedEvent event : events) {
                processOrderCreated(event);
            }
            
            // 批量确认
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            System.err.println("批量处理失败: " + e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                System.err.println("消息确认失败: " + ioException.getMessage());
            }
        }
    }
    
    /**
     * 处理原始消息
     */
    @RabbitListener(queues = "order.raw.queue")
    public void handleRawMessage(Message message, Channel channel) {
        try {
            String content = new String(message.getBody());
            System.out.println("接收到原始消息: " + content);
            
            // 获取消息属性
            String messageId = message.getMessageProperties().getMessageId();
            String correlationId = message.getMessageProperties().getCorrelationId();
            long timestamp = message.getMessageProperties().getTimestamp().getTime();
            
            System.out.println("消息ID: " + messageId);
            System.out.println("关联ID: " + correlationId);
            System.out.println("时间戳: " + timestamp);
            
            // 手动确认
            long deliveryTag = message.getMessageProperties().getDeliveryTag();
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            System.err.println("处理原始消息失败: " + e.getMessage());
        }
    }
    
    // 业务处理方法
    private void processOrderCreated(OrderCreatedEvent event) {
        // 库存扣减
        // 发送通知
        // 记录日志
        System.out.println("订单创建处理完成: " + event.getOrderId());
    }
    
    private void processOrderPaid(OrderPaidEvent event) {
        // 更新订单状态
        // 发送发货指令
        // 积分奖励
        System.out.println("订单支付处理完成: " + event.getOrderId());
    }
    
    private void processOrderCancelled(OrderCancelledEvent event) {
        // 恢复库存
        // 退款处理
        // 发送通知
        System.out.println("订单取消处理完成: " + event.getOrderId());
    }
}
```

### 3. 控制器示例

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderMessageProducer orderMessageProducer;
    
    /**
     * 创建订单
     */
    @PostMapping
    public String createOrder(@RequestBody CreateOrderRequest request) {
        // 创建订单逻辑
        String orderId = generateOrderId();
        
        // 发送订单创建事件
        OrderCreatedEvent event = new OrderCreatedEvent(
            orderId, 
            request.getUserId(), 
            request.getAmount()
        );
        orderMessageProducer.sendOrderCreated(event);
        
        return "订单创建成功: " + orderId;
    }
    
    /**
     * 支付订单
     */
    @PostMapping("/{orderId}/pay")
    public String payOrder(@PathVariable String orderId, 
                          @RequestBody PayOrderRequest request) {
        // 支付逻辑
        String paymentId = generatePaymentId();
        
        // 发送订单支付事件
        OrderPaidEvent event = new OrderPaidEvent();
        event.setOrderId(orderId);
        event.setPaymentId(paymentId);
        event.setAmount(request.getAmount());
        event.setPaidTime(java.time.LocalDateTime.now());
        
        orderMessageProducer.sendOrderPaid(event);
        
        return "订单支付成功: " + orderId;
    }
    
    /**
     * 取消订单
     */
    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(@PathVariable String orderId, 
                            @RequestBody CancelOrderRequest request) {
        // 取消订单逻辑
        
        // 发送订单取消事件
        OrderCancelledEvent event = new OrderCancelledEvent();
        event.setOrderId(orderId);
        event.setReason(request.getReason());
        event.setCancelTime(java.time.LocalDateTime.now());
        
        orderMessageProducer.sendOrderCancelled(event);
        
        return "订单取消成功: " + orderId;
    }
    
    private String generateOrderId() {
        return "ORDER_" + System.currentTimeMillis();
    }
    
    private String generatePaymentId() {
        return "PAY_" + System.currentTimeMillis();
    }
}

// 请求对象
class CreateOrderRequest {
    private String userId;
    private Double amount;
    private java.util.List<OrderItem> items;
    
    // getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public java.util.List<OrderItem> getItems() { return items; }
    public void setItems(java.util.List<OrderItem> items) { this.items = items; }
}

class PayOrderRequest {
    private Double amount;
    private String paymentMethod;
    
    // getters and setters
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}

class CancelOrderRequest {
    private String reason;
    
    // getters and setters
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

class OrderItem {
    private String productId;
    private Integer quantity;
    private Double price;
    
    // getters and setters
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}
```

## 交换机类型详解

### 1. Direct Exchange（直连交换机）

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DirectExchangeConfig {
    
    /**
     * 直连交换机：完全匹配路由键
     * 适用场景：点对点消息传递
     */
    @Bean
    public DirectExchange userDirectExchange() {
        return new DirectExchange("user.direct.exchange", true, false);
    }
    
    // 用户注册队列
    @Bean
    public Queue userRegisterQueue() {
        return QueueBuilder.durable("user.register.queue").build();
    }
    
    // 用户登录队列
    @Bean
    public Queue userLoginQueue() {
        return QueueBuilder.durable("user.login.queue").build();
    }
    
    // 绑定：用户注册
    @Bean
    public Binding userRegisterBinding() {
        return BindingBuilder
                .bind(userRegisterQueue())
                .to(userDirectExchange())
                .with("user.register");
    }
    
    // 绑定：用户登录
    @Bean
    public Binding userLoginBinding() {
        return BindingBuilder
                .bind(userLoginQueue())
                .to(userDirectExchange())
                .with("user.login");
    }
}

// 使用示例
@Service
public class UserDirectMessageService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendUserRegister(UserRegisterEvent event) {
        rabbitTemplate.convertAndSend("user.direct.exchange", "user.register", event);
    }
    
    public void sendUserLogin(UserLoginEvent event) {
        rabbitTemplate.convertAndSend("user.direct.exchange", "user.login", event);
    }
}
```

### 2. Topic Exchange（主题交换机）

```java
@Configuration
public class TopicExchangeConfig {
    
    /**
     * 主题交换机：模式匹配路由键
     * 通配符：
     * * 匹配一个单词
     * # 匹配零个或多个单词
     */
    @Bean
    public TopicExchange logTopicExchange() {
        return new TopicExchange("log.topic.exchange", true, false);
    }
    
    // 错误日志队列
    @Bean
    public Queue errorLogQueue() {
        return QueueBuilder.durable("log.error.queue").build();
    }
    
    // 警告日志队列
    @Bean
    public Queue warnLogQueue() {
        return QueueBuilder.durable("log.warn.queue").build();
    }
    
    // 所有日志队列
    @Bean
    public Queue allLogQueue() {
        return QueueBuilder.durable("log.all.queue").build();
    }
    
    // 绑定：错误日志（匹配 log.error.*）
    @Bean
    public Binding errorLogBinding() {
        return BindingBuilder
                .bind(errorLogQueue())
                .to(logTopicExchange())
                .with("log.error.*");
    }
    
    // 绑定：警告日志（匹配 log.warn.*）
    @Bean
    public Binding warnLogBinding() {
        return BindingBuilder
                .bind(warnLogQueue())
                .to(logTopicExchange())
                .with("log.warn.*");
    }
    
    // 绑定：所有日志（匹配 log.#）
    @Bean
    public Binding allLogBinding() {
        return BindingBuilder
                .bind(allLogQueue())
                .to(logTopicExchange())
                .with("log.#");
    }
}

// 使用示例
@Service
public class LogTopicMessageService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendErrorLog(String service, String message) {
        String routingKey = "log.error." + service;
        LogEvent event = new LogEvent("ERROR", service, message);
        rabbitTemplate.convertAndSend("log.topic.exchange", routingKey, event);
    }
    
    public void sendWarnLog(String service, String message) {
        String routingKey = "log.warn." + service;
        LogEvent event = new LogEvent("WARN", service, message);
        rabbitTemplate.convertAndSend("log.topic.exchange", routingKey, event);
    }
    
    public void sendInfoLog(String service, String message) {
        String routingKey = "log.info." + service;
        LogEvent event = new LogEvent("INFO", service, message);
        rabbitTemplate.convertAndSend("log.topic.exchange", routingKey, event);
    }
}

class LogEvent {
    private String level;
    private String service;
    private String message;
    private java.time.LocalDateTime timestamp;
    
    public LogEvent(String level, String service, String message) {
        this.level = level;
        this.service = service;
        this.message = message;
        this.timestamp = java.time.LocalDateTime.now();
    }
    
    // getters and setters
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public String getService() { return service; }
    public void setService(String service) { this.service = service; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public java.time.LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(java.time.LocalDateTime timestamp) { this.timestamp = timestamp; }
}
```

### 3. Fanout Exchange（扇形交换机）

```java
@Configuration
public class FanoutExchangeConfig {
    
    /**
     * 扇形交换机：广播消息到所有绑定的队列
     * 忽略路由键，适用于广播场景
     */
    @Bean
    public FanoutExchange notificationFanoutExchange() {
        return new FanoutExchange("notification.fanout.exchange", true, false);
    }
    
    // 邮件通知队列
    @Bean
    public Queue emailNotificationQueue() {
        return QueueBuilder.durable("notification.email.queue").build();
    }
    
    // 短信通知队列
    @Bean
    public Queue smsNotificationQueue() {
        return QueueBuilder.durable("notification.sms.queue").build();
    }
    
    // 推送通知队列
    @Bean
    public Queue pushNotificationQueue() {
        return QueueBuilder.durable("notification.push.queue").build();
    }
    
    // 绑定：邮件通知
    @Bean
    public Binding emailNotificationBinding() {
        return BindingBuilder
                .bind(emailNotificationQueue())
                .to(notificationFanoutExchange());
    }
    
    // 绑定：短信通知
    @Bean
    public Binding smsNotificationBinding() {
        return BindingBuilder
                .bind(smsNotificationQueue())
                .to(notificationFanoutExchange());
    }
    
    // 绑定：推送通知
    @Bean
    public Binding pushNotificationBinding() {
        return BindingBuilder
                .bind(pushNotificationQueue())
                .to(notificationFanoutExchange());
    }
}

// 使用示例
@Service
public class NotificationFanoutService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * 发送广播通知（所有渠道都会收到）
     */
    public void sendBroadcastNotification(NotificationEvent event) {
        // 扇形交换机忽略路由键，可以传空字符串
        rabbitTemplate.convertAndSend("notification.fanout.exchange", "", event);
    }
}

class NotificationEvent {
    private String userId;
    private String title;
    private String content;
    private String type;
    private java.time.LocalDateTime createTime;
    
    // constructors, getters and setters
    public NotificationEvent() {}
    
    public NotificationEvent(String userId, String title, String content, String type) {
        this.userId = userId;
        this.title = title;
        this.content = content;
        this.type = type;
        this.createTime = java.time.LocalDateTime.now();
    }
    
    // getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public java.time.LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(java.time.LocalDateTime createTime) { this.createTime = createTime; }
}
```

### 4. Headers Exchange（头交换机）

```java
@Configuration
public class HeadersExchangeConfig {
    
    /**
     * 头交换机：根据消息头属性路由
     * 支持 x-match 参数：
     * - all：所有头属性都匹配
     * - any：任意头属性匹配
     */
    @Bean
    public HeadersExchange orderHeadersExchange() {
        return new HeadersExchange("order.headers.exchange", true, false);
    }
    
    // VIP订单队列
    @Bean
    public Queue vipOrderQueue() {
        return QueueBuilder.durable("order.vip.queue").build();
    }
    
    // 普通订单队列
    @Bean
    public Queue normalOrderQueue() {
        return QueueBuilder.durable("order.normal.queue").build();
    }
    
    // 大额订单队列
    @Bean
    public Queue largeOrderQueue() {
        return QueueBuilder.durable("order.large.queue").build();
    }
    
    // 绑定：VIP订单（用户类型为VIP）
    @Bean
    public Binding vipOrderBinding() {
        return BindingBuilder
                .bind(vipOrderQueue())
                .to(orderHeadersExchange())
                .where("userType").matches("VIP");
    }
    
    // 绑定：普通订单（用户类型为NORMAL）
    @Bean
    public Binding normalOrderBinding() {
        return BindingBuilder
                .bind(normalOrderQueue())
                .to(orderHeadersExchange())
                .where("userType").matches("NORMAL");
    }
    
    // 绑定：大额订单（金额大于1000且需要审核）
    @Bean
    public Binding largeOrderBinding() {
        return BindingBuilder
                .bind(largeOrderQueue())
                .to(orderHeadersExchange())
                .whereAll(java.util.Map.of(
                    "amount", "large",
                    "needApproval", "true"
                )).match();
    }
}

// 使用示例
@Service
public class OrderHeadersService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendOrderWithHeaders(OrderEvent order) {
        rabbitTemplate.convertAndSend("order.headers.exchange", "", order, message -> {
            // 设置消息头
            message.getMessageProperties().getHeaders().put("userType", order.getUserType());
            message.getMessageProperties().getHeaders().put("amount", 
                order.getAmount() > 1000 ? "large" : "small");
            message.getMessageProperties().getHeaders().put("needApproval", 
                order.getAmount() > 1000 ? "true" : "false");
            return message;
        });
    }
}

class OrderEvent {
    private String orderId;
    private String userId;
    private String userType;
    private Double amount;
    
    // constructors, getters and setters
    public OrderEvent() {}
    
    public OrderEvent(String orderId, String userId, String userType, Double amount) {
        this.orderId = orderId;
        this.userId = userId;
        this.userType = userType;
        this.amount = amount;
    }
    
    // getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
}
```

## 消息可靠性保证

### 1. 生产者确认机制

```java
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ReliableMessageProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    // 消息确认缓存
    private final ConcurrentMap<String, MessageRecord> messageCache = new ConcurrentHashMap<>();
    
    /**
     * 发送可靠消息
     */
    public void sendReliableMessage(String exchange, String routingKey, Object message) {
        String messageId = UUID.randomUUID().toString();
        CorrelationData correlationData = new CorrelationData(messageId);
        
        // 记录消息
        MessageRecord record = new MessageRecord(messageId, exchange, routingKey, message);
        messageCache.put(messageId, record);
        
        // 发送消息
        rabbitTemplate.convertAndSend(exchange, routingKey, message, correlationData);
    }
    
    /**
     * 消息确认回调
     */
    @PostConstruct
    public void setupCallbacks() {
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String messageId = correlationData.getId();
            MessageRecord record = messageCache.get(messageId);
            
            if (ack) {
                System.out.println("消息发送成功: " + messageId);
                // 从缓存中移除
                messageCache.remove(messageId);
            } else {
                System.err.println("消息发送失败: " + messageId + ", 原因: " + cause);
                // 重试逻辑
                retryMessage(record);
            }
        });
        
        rabbitTemplate.setReturnsCallback(returned -> {
            System.err.println("消息被退回: " + returned.getMessage());
            System.err.println("退回原因: " + returned.getReplyText());
            System.err.println("交换机: " + returned.getExchange());
            System.err.println("路由键: " + returned.getRoutingKey());
            
            // 处理退回的消息
            handleReturnedMessage(returned);
        });
    }
    
    /**
     * 重试消息发送
     */
    private void retryMessage(MessageRecord record) {
        if (record.getRetryCount() < 3) {
            record.incrementRetryCount();
            
            // 延迟重试
            try {
                Thread.sleep(1000 * record.getRetryCount());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 重新发送
            sendReliableMessage(record.getExchange(), record.getRoutingKey(), record.getMessage());
        } else {
            System.err.println("消息重试次数超限，发送到死信队列: " + record.getMessageId());
            // 发送到死信队列或记录到数据库
            handleFailedMessage(record);
        }
    }
    
    /**
     * 处理退回的消息
     */
    private void handleReturnedMessage(org.springframework.amqp.core.ReturnedMessage returned) {
        // 可以选择重新路由或发送到备用队列
        String alternateRoutingKey = "alternate." + returned.getRoutingKey();
        rabbitTemplate.convertAndSend(returned.getExchange(), alternateRoutingKey, returned.getMessage());
    }
    
    /**
     * 处理失败的消息
     */
    private void handleFailedMessage(MessageRecord record) {
        // 发送到死信队列
        rabbitTemplate.convertAndSend("dlx.exchange", "failed.message", record);
        
        // 从缓存中移除
        messageCache.remove(record.getMessageId());
    }
    
    // 消息记录类
    private static class MessageRecord {
        private String messageId;
        private String exchange;
        private String routingKey;
        private Object message;
        private int retryCount = 0;
        private java.time.LocalDateTime createTime;
        
        public MessageRecord(String messageId, String exchange, String routingKey, Object message) {
            this.messageId = messageId;
            this.exchange = exchange;
            this.routingKey = routingKey;
            this.message = message;
            this.createTime = java.time.LocalDateTime.now();
        }
        
        // getters and setters
        public String getMessageId() { return messageId; }
        public String getExchange() { return exchange; }
        public String getRoutingKey() { return routingKey; }
        public Object getMessage() { return message; }
        public int getRetryCount() { return retryCount; }
        public void incrementRetryCount() { this.retryCount++; }
        public java.time.LocalDateTime getCreateTime() { return createTime; }
    }
}
```

### 2. 消费者确认机制

```java
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ReliableMessageConsumer {
    
    /**
     * 手动确认模式消费
     */
    @RabbitListener(queues = "reliable.queue")
    public void handleReliableMessage(@Payload OrderEvent order,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                    @Header(AmqpHeaders.REDELIVERED) boolean redelivered) {
        try {
            // 检查是否重复消费
            if (isMessageProcessed(order.getOrderId())) {
                System.out.println("消息已处理，跳过: " + order.getOrderId());
                channel.basicAck(deliveryTag, false);
                return;
            }
            
            // 业务处理
            processOrder(order);
            
            // 记录消息已处理
            markMessageProcessed(order.getOrderId());
            
            // 确认消息
            channel.basicAck(deliveryTag, false);
            
            System.out.println("消息处理成功: " + order.getOrderId());
            
        } catch (BusinessException e) {
            System.err.println("业务处理失败: " + e.getMessage());
            try {
                if (redelivered) {
                    // 已经重试过，拒绝消息并不重新入队（发送到死信队列）
                    channel.basicNack(deliveryTag, false, false);
                    System.err.println("消息发送到死信队列: " + order.getOrderId());
                } else {
                    // 第一次失败，拒绝消息并重新入队
                    channel.basicNack(deliveryTag, false, true);
                    System.err.println("消息重新入队: " + order.getOrderId());
                }
            } catch (IOException ioException) {
                System.err.println("消息确认失败: " + ioException.getMessage());
            }
        } catch (Exception e) {
            System.err.println("系统异常: " + e.getMessage());
            try {
                // 系统异常，拒绝消息并重新入队
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                System.err.println("消息确认失败: " + ioException.getMessage());
            }
        }
    }
    
    /**
     * 批量确认模式
     */
    @RabbitListener(queues = "batch.reliable.queue")
    public void handleBatchReliableMessages(@Payload java.util.List<OrderEvent> orders,
                                           Channel channel,
                                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            for (OrderEvent order : orders) {
                if (!isMessageProcessed(order.getOrderId())) {
                    processOrder(order);
                    markMessageProcessed(order.getOrderId());
                }
            }
            
            // 批量确认
            channel.basicAck(deliveryTag, false);
            System.out.println("批量消息处理成功，数量: " + orders.size());
            
        } catch (Exception e) {
            System.err.println("批量消息处理失败: " + e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                System.err.println("消息确认失败: " + ioException.getMessage());
            }
        }
    }
    
    /**
     * 事务模式消费
     */
    @RabbitListener(queues = "transaction.queue")
    @org.springframework.transaction.annotation.Transactional
    public void handleTransactionMessage(@Payload OrderEvent order,
                                       Channel channel,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // 在事务中处理业务逻辑
            processOrderInTransaction(order);
            
            // 确认消息
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            System.err.println("事务处理失败: " + e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                System.err.println("消息确认失败: " + ioException.getMessage());
            }
            // 事务会自动回滚
            throw new RuntimeException("事务处理失败", e);
        }
    }
    
    // 业务处理方法
    private void processOrder(OrderEvent order) throws BusinessException {
        // 模拟业务处理
        if (order.getAmount() < 0) {
            throw new BusinessException("订单金额不能为负数");
        }
        
        // 实际业务逻辑
        System.out.println("处理订单: " + order.getOrderId());
    }
    
    private void processOrderInTransaction(OrderEvent order) {
        // 在事务中处理订单
        System.out.println("在事务中处理订单: " + order.getOrderId());
    }
    
    // 幂等性检查
    private final java.util.Set<String> processedMessages = new java.util.concurrent.ConcurrentHashMap<String, Boolean>().keySet(java.util.concurrent.ConcurrentHashMap.newKeySet());
    
    private boolean isMessageProcessed(String messageId) {
        return processedMessages.contains(messageId);
    }
    
    private void markMessageProcessed(String messageId) {
        processedMessages.add(messageId);
    }
}

// 业务异常类
class BusinessException extends Exception {
    public BusinessException(String message) {
        super(message);
    }
}
```

### 3. 消息持久化

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagePersistenceConfig {
    
    /**
     * 持久化交换机
     */
    @Bean
    public DirectExchange persistentExchange() {
        return new DirectExchange("persistent.exchange", 
                                true,   // durable: 持久化
                                false); // autoDelete: 不自动删除
    }
    
    /**
     * 持久化队列
     */
    @Bean
    public Queue persistentQueue() {
        return QueueBuilder
                .durable("persistent.queue")  // 持久化队列
                .withArgument("x-message-ttl", 3600000)  // 消息TTL: 1小时
                .withArgument("x-max-length", 10000)     // 队列最大长度
                .withArgument("x-overflow", "reject-publish")  // 溢出策略
                .build();
    }
    
    /**
     * 绑定
     */
    @Bean
    public Binding persistentBinding() {
        return BindingBuilder
                .bind(persistentQueue())
                .to(persistentExchange())
                .with("persistent.message");
    }
}

@Service
public class PersistentMessageService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * 发送持久化消息
     */
    public void sendPersistentMessage(Object message) {
        rabbitTemplate.convertAndSend("persistent.exchange", "persistent.message", message, msg -> {
            // 设置消息持久化
            msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            // 设置消息ID
            msg.getMessageProperties().setMessageId(java.util.UUID.randomUUID().toString());
            // 设置时间戳
            msg.getMessageProperties().setTimestamp(new java.util.Date());
            return msg;
        });
    }
    
    /**
     * 发送带优先级的持久化消息
     */
    public void sendPriorityPersistentMessage(Object message, int priority) {
        rabbitTemplate.convertAndSend("persistent.exchange", "persistent.message", message, msg -> {
            msg.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            msg.getMessageProperties().setPriority(priority);
            msg.getMessageProperties().setMessageId(java.util.UUID.randomUUID().toString());
            return msg;
        });
    }
}
```

## 死信队列

### 1. 死信队列配置

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DeadLetterQueueConfig {
    
    // 正常业务交换机
    @Bean
    public DirectExchange businessExchange() {
        return new DirectExchange("business.exchange", true, false);
    }
    
    // 死信交换机
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dlx.exchange", true, false);
    }
    
    // 正常业务队列（配置死信交换机）
    @Bean
    public Queue businessQueue() {
        return QueueBuilder
                .durable("business.queue")
                // 配置死信交换机
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                // 配置死信路由键
                .withArgument("x-dead-letter-routing-key", "dead.letter")
                // 消息TTL（10秒后过期进入死信队列）
                .withArgument("x-message-ttl", 10000)
                // 队列最大长度
                .withArgument("x-max-length", 5)
                build();
    }
    
    // 死信队列
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("dead.letter.queue").build();
    }
    
    // 正常业务绑定
    @Bean
    public Binding businessBinding() {
        return BindingBuilder
                .bind(businessQueue())
                .to(businessExchange())
                .with("business.message");
    }
    
    // 死信绑定
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with("dead.letter");
    }
}
```

### 2. 死信消息处理

```java
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class DeadLetterMessageHandler {
    
    /**
     * 正常业务消息处理
     */
    @RabbitListener(queues = "business.queue")
    public void handleBusinessMessage(@Payload OrderEvent order,
                                    Channel channel,
                                    @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                    Message message) {
        try {
            System.out.println("处理业务消息: " + order.getOrderId());
            
            // 模拟业务处理失败
            if (order.getAmount() < 0) {
                throw new RuntimeException("订单金额不能为负数");
            }
            
            // 正常处理
            processBusinessOrder(order);
            
            // 确认消息
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            System.err.println("业务处理失败: " + e.getMessage());
            try {
                // 拒绝消息，不重新入队（进入死信队列）
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioException) {
                System.err.println("消息确认失败: " + ioException.getMessage());
            }
        }
    }
    
    /**
     * 死信消息处理
     */
    @RabbitListener(queues = "dead.letter.queue")
    public void handleDeadLetterMessage(@Payload OrderEvent order,
                                      Channel channel,
                                      @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
                                      Message message) {
        try {
            System.out.println("处理死信消息: " + order.getOrderId());
            
            // 获取死信原因
            Map<String, Object> headers = message.getMessageProperties().getHeaders();
            String reason = getDeadLetterReason(headers);
            System.out.println("死信原因: " + reason);
            
            // 根据死信原因进行不同处理
            switch (reason) {
                case "rejected":
                    handleRejectedMessage(order);
                    break;
                case "expired":
                    handleExpiredMessage(order);
                    break;
                case "maxlen":
                    handleMaxLengthMessage(order);
                    break;
                default:
                    handleUnknownDeadLetter(order);
            }
            
            // 确认死信消息
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            System.err.println("死信处理失败: " + e.getMessage());
            try {
                // 死信处理失败，可以选择重试或记录日志
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                System.err.println("死信确认失败: " + ioException.getMessage());
            }
        }
    }
    
    /**
     * 获取死信原因
     */
    private String getDeadLetterReason(Map<String, Object> headers) {
        if (headers.containsKey("x-first-death-reason")) {
            return headers.get("x-first-death-reason").toString();
        }
        return "unknown";
    }
    
    /**
     * 处理被拒绝的消息
     */
    private void handleRejectedMessage(OrderEvent order) {
        System.out.println("处理被拒绝的消息: " + order.getOrderId());
        // 可以进行人工审核、修复数据等操作
        // 或者发送告警通知
        sendAlert("订单处理被拒绝", order);
    }
    
    /**
     * 处理过期的消息
     */
    private void handleExpiredMessage(OrderEvent order) {
        System.out.println("处理过期的消息: " + order.getOrderId());
        // 可以进行订单取消、库存恢复等操作
        cancelExpiredOrder(order);
    }
    
    /**
     * 处理队列长度超限的消息
     */
    private void handleMaxLengthMessage(OrderEvent order) {
        System.out.println("处理队列长度超限的消息: " + order.getOrderId());
        // 可以扩容队列或者优化处理速度
        optimizeQueueProcessing(order);
    }
    
    /**
     * 处理未知原因的死信
     */
    private void handleUnknownDeadLetter(OrderEvent order) {
        System.out.println("处理未知原因的死信: " + order.getOrderId());
        // 记录详细日志，进行问题排查
        logUnknownDeadLetter(order);
    }
    
    // 业务处理方法
    private void processBusinessOrder(OrderEvent order) {
        System.out.println("正常处理订单: " + order.getOrderId());
    }
    
    private void sendAlert(String message, OrderEvent order) {
        System.out.println("发送告警: " + message + ", 订单: " + order.getOrderId());
    }
    
    private void cancelExpiredOrder(OrderEvent order) {
        System.out.println("取消过期订单: " + order.getOrderId());
    }
    
    private void optimizeQueueProcessing(OrderEvent order) {
        System.out.println("优化队列处理: " + order.getOrderId());
    }
    
    private void logUnknownDeadLetter(OrderEvent order) {
        System.out.println("记录未知死信日志: " + order.getOrderId());
    }
}
```

## 延迟队列

### 1. 基于TTL + 死信队列实现延迟队列

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DelayQueueConfig {
    
    // 延迟交换机
    @Bean
    public DirectExchange delayExchange() {
        return new DirectExchange("delay.exchange", true, false);
    }
    
    // 处理交换机
    @Bean
    public DirectExchange processExchange() {
        return new DirectExchange("process.exchange", true, false);
    }
    
    // 延迟队列（TTL队列，消息过期后进入处理队列）
    @Bean
    public Queue delayQueue() {
        return QueueBuilder
                .durable("delay.queue")
                // 消息TTL（30分钟）
                .withArgument("x-message-ttl", 1800000)
                // 死信交换机
                .withArgument("x-dead-letter-exchange", "process.exchange")
                // 死信路由键
                .withArgument("x-dead-letter-routing-key", "process.message")
                .build();
    }
    
    // 处理队列
    @Bean
    public Queue processQueue() {
        return QueueBuilder.durable("process.queue").build();
    }
    
    // 延迟绑定
    @Bean
    public Binding delayBinding() {
        return BindingBuilder
                .bind(delayQueue())
                .to(delayExchange())
                .with("delay.message");
    }
    
    // 处理绑定
    @Bean
    public Binding processBinding() {
        return BindingBuilder
                .bind(processQueue())
                .to(processExchange())
                .with("process.message");
    }
}
```

### 2. 基于RabbitMQ延迟插件实现

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DelayPluginConfig {
    
    /**
     * 延迟交换机（需要安装rabbitmq-delayed-message-exchange插件）
     */
    @Bean
    public CustomExchange delayPluginExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange("delay.plugin.exchange", "x-delayed-message", true, false, args);
    }
    
    // 延迟处理队列
    @Bean
    public Queue delayProcessQueue() {
        return QueueBuilder.durable("delay.process.queue").build();
    }
    
    // 绑定
    @Bean
    public Binding delayPluginBinding() {
        return BindingBuilder
                .bind(delayProcessQueue())
                .to(delayPluginExchange())
                .with("delay.process")
                .noargs();
    }
}
```

### 3. 延迟消息服务

```java
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class DelayMessageService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    /**
     * 发送延迟消息（基于TTL）
     */
    public void sendDelayMessage(Object message, long delaySeconds) {
        rabbitTemplate.convertAndSend("delay.exchange", "delay.message", message, msg -> {
            // 设置消息TTL
            msg.getMessageProperties().setExpiration(String.valueOf(delaySeconds * 1000));
            return msg;
        });
    }
    
    /**
     * 发送延迟消息（基于插件）
     */
    public void sendDelayMessageWithPlugin(Object message, long delayMs) {
        rabbitTemplate.convertAndSend("delay.plugin.exchange", "delay.process", message, msg -> {
            // 设置延迟时间
            msg.getMessageProperties().setDelay((int) delayMs);
            return msg;
        });
    }
    
    /**
     * 发送订单超时取消消息
     */
    public void sendOrderTimeoutMessage(String orderId, long timeoutMinutes) {
        OrderTimeoutEvent event = new OrderTimeoutEvent(orderId, LocalDateTime.now().plusMinutes(timeoutMinutes));
        sendDelayMessage(event, timeoutMinutes * 60);
    }
    
    /**
     * 发送定时提醒消息
     */
    public void sendReminderMessage(String userId, String content, long delayMinutes) {
        ReminderEvent event = new ReminderEvent(userId, content, LocalDateTime.now().plusMinutes(delayMinutes));
        sendDelayMessageWithPlugin(event, delayMinutes * 60 * 1000);
    }
}

// 订单超时事件
class OrderTimeoutEvent {
    private String orderId;
    private LocalDateTime timeoutTime;
    
    public OrderTimeoutEvent(String orderId, LocalDateTime timeoutTime) {
        this.orderId = orderId;
        this.timeoutTime = timeoutTime;
    }
    
    // getters and setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public LocalDateTime getTimeoutTime() { return timeoutTime; }
    public void setTimeoutTime(LocalDateTime timeoutTime) { this.timeoutTime = timeoutTime; }
}

// 提醒事件
class ReminderEvent {
    private String userId;
    private String content;
    private LocalDateTime reminderTime;
    
    public ReminderEvent(String userId, String content, LocalDateTime reminderTime) {
        this.userId = userId;
        this.content = content;
        this.reminderTime = reminderTime;
    }
    
    // getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getReminderTime() { return reminderTime; }
    public void setReminderTime(LocalDateTime reminderTime) { this.reminderTime = reminderTime; }
}
```

### 4. 延迟消息处理

```java
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class DelayMessageHandler {
    
    /**
     * 处理延迟消息（TTL方式）
     */
    @RabbitListener(queues = "process.queue")
    public void handleDelayMessage(@Payload Object message,
                                 Channel channel,
                                 @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            System.out.println("处理延迟消息: " + message);
            
            if (message instanceof OrderTimeoutEvent) {
                handleOrderTimeout((OrderTimeoutEvent) message);
            } else if (message instanceof ReminderEvent) {
                handleReminder((ReminderEvent) message);
            }
            
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            System.err.println("延迟消息处理失败: " + e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                System.err.println("消息确认失败: " + ex.getMessage());
            }
        }
    }
    
    /**
     * 处理延迟消息（插件方式）
     */
    @RabbitListener(queues = "delay.process.queue")
    public void handleDelayPluginMessage(@Payload Object message,
                                       Channel channel,
                                       @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            System.out.println("处理插件延迟消息: " + message);
            
            if (message instanceof ReminderEvent) {
                handleReminder((ReminderEvent) message);
            }
            
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            System.err.println("插件延迟消息处理失败: " + e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                System.err.println("消息确认失败: " + ex.getMessage());
            }
        }
    }
    
    /**
     * 处理订单超时
     */
    private void handleOrderTimeout(OrderTimeoutEvent event) {
        System.out.println("处理订单超时: " + event.getOrderId());
        
        // 检查订单状态
        if (isOrderUnpaid(event.getOrderId())) {
            // 取消订单
            cancelOrder(event.getOrderId());
            // 恢复库存
            restoreInventory(event.getOrderId());
            // 发送通知
            sendOrderCancelNotification(event.getOrderId());
        }
    }
    
    /**
     * 处理提醒
     */
    private void handleReminder(ReminderEvent event) {
        System.out.println("处理提醒: " + event.getUserId() + ", 内容: " + event.getContent());
        
        // 发送提醒通知
        sendReminderNotification(event.getUserId(), event.getContent());
    }
    
    // 业务方法
    private boolean isOrderUnpaid(String orderId) {
        // 检查订单支付状态
        return true; // 模拟未支付
    }
    
    private void cancelOrder(String orderId) {
        System.out.println("取消订单: " + orderId);
    }
    
    private void restoreInventory(String orderId) {
        System.out.println("恢复库存: " + orderId);
    }
    
    private void sendOrderCancelNotification(String orderId) {
        System.out.println("发送订单取消通知: " + orderId);
    }
    
    private void sendReminderNotification(String userId, String content) {
         System.out.println("发送提醒通知给用户: " + userId + ", 内容: " + content);
     }
 }
 ```

## 集群与高可用

### 1. RabbitMQ集群配置

```yaml
# application.yml
spring:
  rabbitmq:
    addresses: rabbitmq-node1:5672,rabbitmq-node2:5672,rabbitmq-node3:5672
    username: admin
    password: admin123
    virtual-host: /
    connection-timeout: 15000
    # 连接池配置
    cache:
      connection:
        mode: channel
        size: 25
      channel:
        size: 25
        checkout-timeout: 0
    # 高可用配置
    listener:
      simple:
        retry:
          enabled: true
          initial-interval: 1000
          max-attempts: 3
          max-interval: 10000
          multiplier: 1.0
        default-requeue-rejected: false
        acknowledge-mode: manual
```

### 2. 镜像队列配置

```java
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class HighAvailabilityConfig {
    
    /**
     * 高可用队列（镜像队列）
     */
    @Bean
    public Queue haQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置镜像队列策略
        args.put("x-ha-policy", "all");  // 在所有节点上镜像
        // args.put("x-ha-policy", "exactly");  // 在指定数量的节点上镜像
        // args.put("x-ha-policy-params", 2);   // 镜像节点数量
        
        // 设置队列同步模式
        args.put("x-ha-sync-mode", "automatic");
        
        return QueueBuilder
                .durable("ha.queue")
                .withArguments(args)
                .build();
    }
    
    /**
     * 仲裁队列（Quorum Queue）- RabbitMQ 3.8+
     */
    @Bean
    public Queue quorumQueue() {
        return QueueBuilder
                .durable("quorum.queue")
                .quorum()  // 设置为仲裁队列
                .build();
    }
    
    /**
     * 流队列（Stream Queue）- RabbitMQ 3.9+
     */
    @Bean
    public Queue streamQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-queue-type", "stream");
        args.put("x-max-length-bytes", 20_000_000_000L);  // 20GB
        args.put("x-stream-max-segment-size-bytes", 500_000_000);  // 500MB
        
        return QueueBuilder
                .durable("stream.queue")
                .withArguments(args)
                .build();
    }
}
```

### 3. 连接故障转移

```java
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class ConnectionFailoverConfig {
    
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory();
        
        // 设置集群地址
        factory.setAddresses("rabbitmq-node1:5672,rabbitmq-node2:5672,rabbitmq-node3:5672");
        factory.setUsername("admin");
        factory.setPassword("admin123");
        factory.setVirtualHost("/");
        
        // 连接超时设置
        factory.setConnectionTimeout(15000);
        factory.setRequestedHeartBeat(60);
        
        // 连接池设置
        factory.setCacheMode(CachingConnectionFactory.CacheMode.CHANNEL);
        factory.setChannelCacheSize(25);
        factory.setChannelCheckoutTimeout(0);
        
        // 发布者确认
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);
        
        return factory;
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        
        // 设置重试模板
        template.setRetryTemplate(retryTemplate());
        
        // 设置强制返回
        template.setMandatory(true);
        
        return template;
    }
    
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        
        // 重试策略
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(3);
        retryTemplate.setRetryPolicy(retryPolicy);
        
        // 退避策略
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);
        backOffPolicy.setMultiplier(2.0);
        backOffPolicy.setMaxInterval(10000);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        
        return retryTemplate;
    }
}
```

## 性能优化

### 1. 连接池优化

```java
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PerformanceOptimizationConfig {
    
    @Bean
    public CachingConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory("localhost");
        
        // 连接池优化
        factory.setCacheMode(CachingConnectionFactory.CacheMode.CONNECTION);
        factory.setConnectionCacheSize(10);  // 连接池大小
        factory.setChannelCacheSize(50);     // 每个连接的通道缓存大小
        
        // 连接超时优化
        factory.setConnectionTimeout(5000);  // 连接超时
        factory.setRequestedHeartBeat(30);   // 心跳间隔
        
        // 网络恢复间隔
        factory.setNetworkRecoveryInterval(5000);
        
        return factory;
    }
}
```

### 2. 批量操作优化

```java
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class BatchMessageService {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    private final ExecutorService executor = Executors.newFixedThreadPool(10);
    
    /**
     * 批量发送消息
     */
    public void sendBatchMessages(String exchange, String routingKey, List<Object> messages) {
        // 分批处理
        int batchSize = 100;
        for (int i = 0; i < messages.size(); i += batchSize) {
            int end = Math.min(i + batchSize, messages.size());
            List<Object> batch = messages.subList(i, end);
            
            // 异步发送批次
            CompletableFuture.runAsync(() -> {
                for (Object message : batch) {
                    rabbitTemplate.convertAndSend(exchange, routingKey, message);
                }
            }, executor);
        }
    }
    
    /**
     * 批量发送带确认的消息
     */
    public void sendBatchMessagesWithConfirm(String exchange, String routingKey, List<Object> messages) {
        rabbitTemplate.execute(channel -> {
            // 开启发布者确认
            channel.confirmSelect();
            
            try {
                for (Object message : messages) {
                    rabbitTemplate.convertAndSend(exchange, routingKey, message);
                }
                
                // 等待确认
                boolean confirmed = channel.waitForConfirms(5000);
                if (confirmed) {
                    System.out.println("批量消息发送成功");
                } else {
                    System.err.println("批量消息发送失败");
                }
                
            } catch (Exception e) {
                System.err.println("批量发送异常: " + e.getMessage());
            }
            
            return null;
        });
    }
    
    /**
     * 预分配消息
     */
    public void sendPreAllocatedMessages(String exchange, String routingKey, List<Object> messages) {
        rabbitTemplate.execute(channel -> {
            try {
                // 预分配消息ID
                long nextPublishSeqNo = channel.getNextPublishSeqNo();
                
                for (int i = 0; i < messages.size(); i++) {
                    Object message = messages.get(i);
                    
                    // 设置消息属性
                    org.springframework.amqp.core.MessageProperties properties = 
                        new org.springframework.amqp.core.MessageProperties();
                    properties.setMessageId(String.valueOf(nextPublishSeqNo + i));
                    properties.setDeliveryMode(org.springframework.amqp.core.MessageDeliveryMode.PERSISTENT);
                    
                    // 发送消息
                    org.springframework.amqp.core.Message amqpMessage = 
                        new org.springframework.amqp.core.Message(message.toString().getBytes(), properties);
                    
                    channel.basicPublish(exchange, routingKey, false, false, 
                        amqpMessage.getMessageProperties(), amqpMessage.getBody());
                }
                
            } catch (Exception e) {
                System.err.println("预分配发送异常: " + e.getMessage());
            }
            
            return null;
        });
    }
}
```

### 3. 消费者性能优化

```java
import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class ConsumerOptimizationConfig {
    
    /**
     * 高性能消费者容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        
        // 并发消费者数量
        factory.setConcurrentConsumers(5);
        factory.setMaxConcurrentConsumers(20);
        
        // 预取数量（QoS）
        factory.setPrefetchCount(50);
        
        // 批量大小
        factory.setBatchSize(10);
        factory.setConsumerBatchEnabled(true);
        
        // 确认模式
        factory.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);
        
        // 任务执行器
        factory.setTaskExecutor(taskExecutor());
        
        // 错误处理
        factory.setErrorHandler(t -> {
            System.err.println("消费者错误: " + t.getMessage());
        });
        
        return factory;
    }
    
    @Bean
    public Executor taskExecutor() {
        return Executors.newFixedThreadPool(20);
    }
}

@Component
public class OptimizedMessageConsumer {
    
    /**
     * 批量消费消息
     */
    @RabbitListener(queues = "batch.queue", containerFactory = "rabbitListenerContainerFactory")
    public void handleBatchMessages(@Payload List<OrderEvent> orders,
                                  Channel channel,
                                  @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // 批量处理业务逻辑
            processBatchOrders(orders);
            
            // 批量确认
            channel.basicAck(deliveryTag, false);
            
            System.out.println("批量处理完成，数量: " + orders.size());
            
        } catch (Exception e) {
            System.err.println("批量处理失败: " + e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                System.err.println("确认失败: " + ex.getMessage());
            }
        }
    }
    
    /**
     * 高并发消费
     */
    @RabbitListener(queues = "high.concurrency.queue", concurrency = "5-20")
    public void handleHighConcurrencyMessage(@Payload OrderEvent order,
                                           Channel channel,
                                           @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // 快速处理
            processOrderQuickly(order);
            
            // 确认消息
            channel.basicAck(deliveryTag, false);
            
        } catch (Exception e) {
            System.err.println("高并发处理失败: " + e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception ex) {
                System.err.println("确认失败: " + ex.getMessage());
            }
        }
    }
    
    private void processBatchOrders(List<OrderEvent> orders) {
        // 批量处理逻辑
        orders.parallelStream().forEach(this::processOrderQuickly);
    }
    
    private void processOrderQuickly(OrderEvent order) {
        // 快速处理逻辑
        System.out.println("快速处理订单: " + order.getOrderId());
    }
}
```

## 最佳实践

### 1. 消息设计原则

```java
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 标准消息格式
 */
public class StandardMessage {
    
    // 消息头信息
    @JsonProperty("message_id")
    private String messageId;
    
    @JsonProperty("message_type")
    private String messageType;
    
    @JsonProperty("version")
    private String version;
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("source")
    private String source;
    
    @JsonProperty("correlation_id")
    private String correlationId;
    
    // 消息体
    @JsonProperty("payload")
    private Object payload;
    
    // 扩展属性
    @JsonProperty("metadata")
    private Map<String, Object> metadata;
    
    // 构造函数
    public StandardMessage() {
        this.messageId = java.util.UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
        this.version = "1.0";
    }
    
    public StandardMessage(String messageType, Object payload) {
        this();
        this.messageType = messageType;
        this.payload = payload;
    }
    
    // getters and setters
    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }
    public String getMessageType() { return messageType; }
    public void setMessageType(String messageType) { this.messageType = messageType; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public Object getPayload() { return payload; }
    public void setPayload(Object payload) { this.payload = payload; }
    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
}
```

### 2. 异常处理策略

```java
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExceptionHandlingConsumer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    @RabbitListener(queues = "business.queue")
    public void handleMessage(StandardMessage message) {
        try {
            // 业务处理
            processBusinessMessage(message);
            
        } catch (BusinessException e) {
            // 业务异常：记录日志，发送到错误队列
            handleBusinessException(message, e);
            throw new AmqpRejectAndDontRequeueException("业务处理失败", e);
            
        } catch (ValidationException e) {
            // 验证异常：直接拒绝，不重试
            handleValidationException(message, e);
            throw new AmqpRejectAndDontRequeueException("消息验证失败", e);
            
        } catch (SystemException e) {
            // 系统异常：可以重试
            handleSystemException(message, e);
            throw e;  // 重新抛出，触发重试
            
        } catch (Exception e) {
            // 未知异常：记录详细日志
            handleUnknownException(message, e);
            throw new AmqpRejectAndDontRequeueException("未知异常", e);
        }
    }
    
    private void processBusinessMessage(StandardMessage message) throws BusinessException, ValidationException, SystemException {
        // 消息验证
        validateMessage(message);
        
        // 业务处理
        switch (message.getMessageType()) {
            case "ORDER_CREATED":
                processOrderCreated(message);
                break;
            case "PAYMENT_COMPLETED":
                processPaymentCompleted(message);
                break;
            default:
                throw new ValidationException("不支持的消息类型: " + message.getMessageType());
        }
    }
    
    private void validateMessage(StandardMessage message) throws ValidationException {
        if (message.getMessageId() == null || message.getMessageId().isEmpty()) {
            throw new ValidationException("消息ID不能为空");
        }
        if (message.getMessageType() == null || message.getMessageType().isEmpty()) {
            throw new ValidationException("消息类型不能为空");
        }
        if (message.getPayload() == null) {
            throw new ValidationException("消息体不能为空");
        }
    }
    
    private void processOrderCreated(StandardMessage message) throws BusinessException {
        // 处理订单创建消息
        System.out.println("处理订单创建: " + message.getMessageId());
    }
    
    private void processPaymentCompleted(StandardMessage message) throws SystemException {
        // 处理支付完成消息
        System.out.println("处理支付完成: " + message.getMessageId());
    }
    
    private void handleBusinessException(StandardMessage message, BusinessException e) {
        System.err.println("业务异常: " + e.getMessage());
        // 发送到业务错误队列
        rabbitTemplate.convertAndSend("business.error.exchange", "business.error", 
            createErrorMessage(message, e));
    }
    
    private void handleValidationException(StandardMessage message, ValidationException e) {
        System.err.println("验证异常: " + e.getMessage());
        // 发送到验证错误队列
        rabbitTemplate.convertAndSend("validation.error.exchange", "validation.error", 
            createErrorMessage(message, e));
    }
    
    private void handleSystemException(StandardMessage message, SystemException e) {
        System.err.println("系统异常: " + e.getMessage());
        // 记录系统异常日志
        logSystemException(message, e);
    }
    
    private void handleUnknownException(StandardMessage message, Exception e) {
        System.err.println("未知异常: " + e.getMessage());
        // 发送到未知错误队列
        rabbitTemplate.convertAndSend("unknown.error.exchange", "unknown.error", 
            createErrorMessage(message, e));
    }
    
    private ErrorMessage createErrorMessage(StandardMessage originalMessage, Exception e) {
        ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setOriginalMessage(originalMessage);
        errorMessage.setErrorType(e.getClass().getSimpleName());
        errorMessage.setErrorMessage(e.getMessage());
        errorMessage.setErrorTime(java.time.LocalDateTime.now());
        return errorMessage;
    }
    
    private void logSystemException(StandardMessage message, SystemException e) {
        // 记录系统异常日志
        System.err.println("系统异常详情: 消息ID=" + message.getMessageId() + 
                          ", 异常=" + e.getMessage());
    }
}

// 异常类定义
class BusinessException extends Exception {
    public BusinessException(String message) { super(message); }
}

class ValidationException extends Exception {
    public ValidationException(String message) { super(message); }
}

class SystemException extends Exception {
    public SystemException(String message) { super(message); }
}

// 错误消息类
class ErrorMessage {
    private StandardMessage originalMessage;
    private String errorType;
    private String errorMessage;
    private java.time.LocalDateTime errorTime;
    
    // getters and setters
    public StandardMessage getOriginalMessage() { return originalMessage; }
    public void setOriginalMessage(StandardMessage originalMessage) { this.originalMessage = originalMessage; }
    public String getErrorType() { return errorType; }
    public void setErrorType(String errorType) { this.errorType = errorType; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public java.time.LocalDateTime getErrorTime() { return errorTime; }
    public void setErrorTime(java.time.LocalDateTime errorTime) { this.errorTime = errorTime; }
}
```

### 3. 监控和日志

```java
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class MessageMonitor {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    // 消息计数器
    private final AtomicLong sentCount = new AtomicLong(0);
    private final AtomicLong receivedCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    
    /**
     * 发送消息监控
     */
    public void sendMessageWithMonitoring(String exchange, String routingKey, Object message) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 记录发送日志
            logMessageSent(exchange, routingKey, message);
            
            // 发送消息
            rabbitTemplate.convertAndSend(exchange, routingKey, message);
            
            // 更新计数器
            sentCount.incrementAndGet();
            
            // 记录性能指标
            long duration = System.currentTimeMillis() - startTime;
            recordSendMetrics(exchange, routingKey, duration);
            
        } catch (Exception e) {
            // 记录错误
            errorCount.incrementAndGet();
            logMessageSendError(exchange, routingKey, message, e);
            throw e;
        }
    }
    
    /**
     * 接收消息监控
     */
    public void recordMessageReceived(String queue, Object message) {
        receivedCount.incrementAndGet();
        logMessageReceived(queue, message);
    }
    
    /**
     * 记录消息处理错误
     */
    public void recordMessageError(String queue, Object message, Exception e) {
        errorCount.incrementAndGet();
        logMessageProcessError(queue, message, e);
    }
    
    /**
     * 获取监控指标
     */
    public MonitoringMetrics getMetrics() {
        MonitoringMetrics metrics = new MonitoringMetrics();
        metrics.setSentCount(sentCount.get());
        metrics.setReceivedCount(receivedCount.get());
        metrics.setErrorCount(errorCount.get());
        metrics.setTimestamp(LocalDateTime.now());
        return metrics;
    }
    
    private void logMessageSent(String exchange, String routingKey, Object message) {
        System.out.println(String.format("[%s] 发送消息 - 交换机: %s, 路由键: %s, 消息: %s",
            LocalDateTime.now(), exchange, routingKey, message.toString()));
    }
    
    private void logMessageReceived(String queue, Object message) {
        System.out.println(String.format("[%s] 接收消息 - 队列: %s, 消息: %s",
            LocalDateTime.now(), queue, message.toString()));
    }
    
    private void logMessageSendError(String exchange, String routingKey, Object message, Exception e) {
        System.err.println(String.format("[%s] 发送消息失败 - 交换机: %s, 路由键: %s, 消息: %s, 错误: %s",
            LocalDateTime.now(), exchange, routingKey, message.toString(), e.getMessage()));
    }
    
    private void logMessageProcessError(String queue, Object message, Exception e) {
        System.err.println(String.format("[%s] 处理消息失败 - 队列: %s, 消息: %s, 错误: %s",
            LocalDateTime.now(), queue, message.toString(), e.getMessage()));
    }
    
    private void recordSendMetrics(String exchange, String routingKey, long duration) {
        // 记录性能指标（可以集成到监控系统）
        System.out.println(String.format("发送性能 - 交换机: %s, 路由键: %s, 耗时: %dms",
            exchange, routingKey, duration));
    }
}

// 监控指标类
class MonitoringMetrics {
    private long sentCount;
    private long receivedCount;
    private long errorCount;
    private LocalDateTime timestamp;
    
    // getters and setters
    public long getSentCount() { return sentCount; }
    public void setSentCount(long sentCount) { this.sentCount = sentCount; }
    public long getReceivedCount() { return receivedCount; }
    public void setReceivedCount(long receivedCount) { this.receivedCount = receivedCount; }
    public long getErrorCount() { return errorCount; }
    public void setErrorCount(long errorCount) { this.errorCount = errorCount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    @Override
    public String toString() {
        return String.format("监控指标[发送: %d, 接收: %d, 错误: %d, 时间: %s]",
            sentCount, receivedCount, errorCount, timestamp);
    }
}
```

## 面试要点

### 高频问题

1. **RabbitMQ的核心概念**
   - 生产者、消费者、队列、交换机、绑定、路由键
   - 连接、信道、虚拟主机的作用
   - 四种交换机类型的区别和使用场景

2. **消息可靠性保证**
   - 生产者确认机制（Publisher Confirms）
   - 消费者确认机制（Consumer Acknowledgements）
   - 消息持久化（队列、交换机、消息）
   - 事务机制 vs 确认机制的区别

3. **死信队列和延迟队列**
   - 死信队列的触发条件和应用场景
   - 延迟队列的实现方式（TTL + DLX vs 插件）
   - 实际业务中的使用案例

### 深入问题

1. **RabbitMQ集群和高可用**
   - 普通集群 vs 镜像队列 vs 仲裁队列
   - 集群脑裂问题和解决方案
   - 负载均衡和故障转移机制

2. **性能优化**
   - 连接池和通道复用
   - 批量操作和预取数量（QoS）
   - 消息序列化和压缩
   - 内存和磁盘使用优化

3. **与其他消息队列的对比**
   - RabbitMQ vs Kafka vs RocketMQ
   - 适用场景和技术选型考虑
   - 性能特点和限制

### 实践经验

1. **生产环境部署经验**
   - 集群规划和容量评估
   - 监控指标和告警设置
   - 备份和恢复策略

2. **问题排查经验**
   - 常见问题和解决方案
   - 性能瓶颈分析
   - 消息积压处理

3. **业务场景应用**
   - 订单系统中的应用
   - 微服务间通信
   - 事件驱动架构设计

## 总结

RabbitMQ作为一个功能强大的消息队列中间件，在企业级应用中发挥着重要作用。通过本文的学习，我们掌握了：

1. **核心概念**：理解了RabbitMQ的基本架构和核心组件
2. **Spring Boot集成**：学会了如何在Spring Boot项目中集成和使用RabbitMQ
3. **交换机类型**：掌握了四种交换机的特点和使用场景
4. **可靠性保证**：了解了如何确保消息的可靠传递和处理
5. **高级特性**：学习了死信队列、延迟队列等高级功能
6. **集群部署**：了解了RabbitMQ的集群和高可用方案
7. **性能优化**：掌握了提升RabbitMQ性能的各种技巧
8. **最佳实践**：学习了消息设计、异常处理、监控等最佳实践

在实际项目中，需要根据具体的业务需求选择合适的消息模式和配置，同时要注意消息的可靠性、性能和可维护性。通过合理的设计和配置，RabbitMQ能够为分布式系统提供稳定可靠的消息传递服务。
```