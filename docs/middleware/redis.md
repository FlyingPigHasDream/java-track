# Redis 缓存中间件

## Redis 概述

### 什么是Redis
Redis（Remote Dictionary Server）是一个开源的内存数据结构存储系统，可以用作数据库、缓存和消息代理。它支持多种数据结构，如字符串、哈希、列表、集合、有序集合等。

### Redis特点
- **高性能**: 基于内存存储，读写速度极快
- **丰富的数据类型**: 支持多种数据结构
- **持久化**: 支持RDB和AOF两种持久化方式
- **高可用**: 支持主从复制、哨兵模式、集群模式
- **原子性**: 所有操作都是原子性的
- **发布订阅**: 支持消息发布订阅模式

### 应用场景
- **缓存**: 减少数据库访问压力
- **会话存储**: 分布式会话管理
- **计数器**: 网站访问统计、点赞数等
- **排行榜**: 游戏排行榜、热门文章等
- **消息队列**: 简单的消息队列实现
- **分布式锁**: 实现分布式系统的锁机制

## Redis数据类型

### 1. String（字符串）

```java
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisStringExample {
    
    private static JedisPool jedisPool;
    
    static {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(100);
        config.setMaxIdle(10);
        config.setMinIdle(5);
        config.setTestOnBorrow(true);
        
        jedisPool = new JedisPool(config, "localhost", 6379);
    }
    
    public static void stringOperations() {
        try (Jedis jedis = jedisPool.getResource()) {
            // 设置字符串
            jedis.set("name", "张三");
            jedis.setex("session:123", 3600, "user_data"); // 设置过期时间
            
            // 获取字符串
            String name = jedis.get("name");
            System.out.println("姓名: " + name);
            
            // 数值操作
            jedis.set("counter", "0");
            jedis.incr("counter"); // 自增1
            jedis.incrBy("counter", 5); // 增加5
            jedis.decr("counter"); // 自减1
            
            String counter = jedis.get("counter");
            System.out.println("计数器: " + counter);
            
            // 批量操作
            jedis.mset("key1", "value1", "key2", "value2", "key3", "value3");
            List<String> values = jedis.mget("key1", "key2", "key3");
            System.out.println("批量获取: " + values);
        }
    }
}
```

### 2. Hash（哈希）

```java
import java.util.HashMap;
import java.util.Map;

public class RedisHashExample {
    
    public static void hashOperations() {
        try (Jedis jedis = jedisPool.getResource()) {
            String userKey = "user:1001";
            
            // 设置哈希字段
            jedis.hset(userKey, "name", "李四");
            jedis.hset(userKey, "age", "25");
            jedis.hset(userKey, "email", "lisi@example.com");
            
            // 批量设置
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("phone", "13800138000");
            userInfo.put("address", "北京市朝阳区");
            jedis.hmset(userKey, userInfo);
            
            // 获取哈希字段
            String name = jedis.hget(userKey, "name");
            String age = jedis.hget(userKey, "age");
            System.out.println("用户信息: " + name + ", " + age + "岁");
            
            // 获取所有字段
            Map<String, String> allFields = jedis.hgetAll(userKey);
            System.out.println("所有用户信息: " + allFields);
            
            // 检查字段是否存在
            boolean hasEmail = jedis.hexists(userKey, "email");
            System.out.println("是否有邮箱: " + hasEmail);
            
            // 获取所有字段名
            Set<String> fields = jedis.hkeys(userKey);
            System.out.println("所有字段: " + fields);
            
            // 数值操作
            jedis.hset("product:1", "price", "100");
            jedis.hincrBy("product:1", "price", 10); // 价格增加10
            String price = jedis.hget("product:1", "price");
            System.out.println("商品价格: " + price);
        }
    }
}
```

### 3. List（列表）

```java
import java.util.List;

public class RedisListExample {
    
    public static void listOperations() {
        try (Jedis jedis = jedisPool.getResource()) {
            String listKey = "message_queue";
            
            // 左侧推入元素
            jedis.lpush(listKey, "消息1", "消息2", "消息3");
            
            // 右侧推入元素
            jedis.rpush(listKey, "消息4", "消息5");
            
            // 获取列表长度
            long length = jedis.llen(listKey);
            System.out.println("列表长度: " + length);
            
            // 获取指定范围的元素
            List<String> messages = jedis.lrange(listKey, 0, -1); // 获取所有元素
            System.out.println("所有消息: " + messages);
            
            // 弹出元素
            String leftPop = jedis.lpop(listKey); // 左侧弹出
            String rightPop = jedis.rpop(listKey); // 右侧弹出
            System.out.println("左侧弹出: " + leftPop + ", 右侧弹出: " + rightPop);
            
            // 阻塞弹出（用于消息队列）
            // List<String> result = jedis.blpop(10, listKey); // 阻塞10秒
            
            // 修剪列表
            jedis.ltrim(listKey, 0, 2); // 只保留前3个元素
            
            // 获取指定索引的元素
            String element = jedis.lindex(listKey, 0);
            System.out.println("第一个元素: " + element);
            
            // 设置指定索引的元素
            jedis.lset(listKey, 0, "新消息");
        }
    }
}
```

### 4. Set（集合）

```java
import java.util.Set;

public class RedisSetExample {
    
    public static void setOperations() {
        try (Jedis jedis = jedisPool.getResource()) {
            String setKey = "tags";
            
            // 添加元素
            jedis.sadd(setKey, "Java", "Python", "JavaScript", "Go");
            jedis.sadd(setKey, "Java"); // 重复元素不会被添加
            
            // 获取所有元素
            Set<String> tags = jedis.smembers(setKey);
            System.out.println("所有标签: " + tags);
            
            // 检查元素是否存在
            boolean hasJava = jedis.sismember(setKey, "Java");
            System.out.println("是否包含Java: " + hasJava);
            
            // 获取集合大小
            long size = jedis.scard(setKey);
            System.out.println("集合大小: " + size);
            
            // 随机获取元素
            String randomTag = jedis.srandmember(setKey);
            System.out.println("随机标签: " + randomTag);
            
            // 弹出元素
            String poppedTag = jedis.spop(setKey);
            System.out.println("弹出的标签: " + poppedTag);
            
            // 集合运算
            String set1 = "skills:user1";
            String set2 = "skills:user2";
            
            jedis.sadd(set1, "Java", "Python", "MySQL");
            jedis.sadd(set2, "Java", "JavaScript", "Redis");
            
            // 交集
            Set<String> intersection = jedis.sinter(set1, set2);
            System.out.println("交集: " + intersection);
            
            // 并集
            Set<String> union = jedis.sunion(set1, set2);
            System.out.println("并集: " + union);
            
            // 差集
            Set<String> difference = jedis.sdiff(set1, set2);
            System.out.println("差集: " + difference);
        }
    }
}
```

### 5. Sorted Set（有序集合）

```java
import java.util.Set;
import redis.clients.jedis.Tuple;

public class RedisSortedSetExample {
    
    public static void sortedSetOperations() {
        try (Jedis jedis = jedisPool.getResource()) {
            String rankKey = "game_rank";
            
            // 添加元素（带分数）
            jedis.zadd(rankKey, 1000, "玩家A");
            jedis.zadd(rankKey, 1500, "玩家B");
            jedis.zadd(rankKey, 800, "玩家C");
            jedis.zadd(rankKey, 2000, "玩家D");
            jedis.zadd(rankKey, 1200, "玩家E");
            
            // 获取排名（按分数从小到大）
            Set<String> topPlayers = jedis.zrange(rankKey, 0, 2); // 前3名
            System.out.println("分数最低的3名: " + topPlayers);
            
            // 获取排名（按分数从大到小）
            Set<String> bottomPlayers = jedis.zrevrange(rankKey, 0, 2); // 前3名
            System.out.println("分数最高的3名: " + bottomPlayers);
            
            // 获取带分数的排名
            Set<Tuple> topWithScores = jedis.zrevrangeWithScores(rankKey, 0, 2);
            System.out.println("前3名及分数:");
            for (Tuple tuple : topWithScores) {
                System.out.println(tuple.getElement() + ": " + tuple.getScore());
            }
            
            // 获取指定分数范围的元素
            Set<String> midRangePlayers = jedis.zrangeByScore(rankKey, 1000, 1500);
            System.out.println("分数在1000-1500之间的玩家: " + midRangePlayers);
            
            // 获取元素的分数
            Double score = jedis.zscore(rankKey, "玩家B");
            System.out.println("玩家B的分数: " + score);
            
            // 获取元素的排名
            Long rank = jedis.zrevrank(rankKey, "玩家B"); // 从大到小的排名
            System.out.println("玩家B的排名: " + (rank + 1)); // 排名从0开始，所以+1
            
            // 增加分数
            jedis.zincrby(rankKey, 100, "玩家C"); // 玩家C分数增加100
            
            // 获取集合大小
            long count = jedis.zcard(rankKey);
            System.out.println("排行榜人数: " + count);
            
            // 获取指定分数范围的元素数量
            long countInRange = jedis.zcount(rankKey, 1000, 2000);
            System.out.println("分数在1000-2000之间的人数: " + countInRange);
        }
    }
}
```

## Spring Boot集成Redis

### 1. 添加依赖

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>
</dependencies>
```

### 2. 配置文件

```yaml
# application.yml
spring:
  redis:
    host: localhost
    port: 6379
    password: # 如果有密码
    database: 0
    timeout: 5000ms
    lettuce:
      pool:
        max-active: 100
        max-idle: 10
        min-idle: 5
        max-wait: 5000ms
```

### 3. Redis配置类

```java
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // JSON序列化配置
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = 
            new Jackson2JsonRedisSerializer<>(Object.class);
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);
        
        // String序列化配置
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        
        // key采用String的序列化方式
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);
        
        // value采用jackson的序列化方式
        template.setValueSerializer(jackson2JsonRedisSerializer);
        template.setHashValueSerializer(jackson2JsonRedisSerializer);
        
        template.afterPropertiesSet();
        return template;
    }
}
```

### 4. Redis工具类

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtil {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    // =============================common============================
    
    /**
     * 指定缓存失效时间
     * @param key 键
     * @param time 时间(秒)
     */
    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.SECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 根据key 获取过期时间
     * @param key 键 不能为null
     * @return 时间(秒) 返回0代表为永久有效
     */
    public long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }
    
    /**
     * 判断key是否存在
     * @param key 键
     * @return true 存在 false不存在
     */
    public boolean hasKey(String key) {
        try {
            return redisTemplate.hasKey(key);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 删除缓存
     * @param key 可以传一个值 或多个
     */
    @SuppressWarnings("unchecked")
    public void del(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete(CollectionUtils.arrayToList(key));
            }
        }
    }
    
    // ============================String=============================
    
    /**
     * 普通缓存获取
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }
    
    /**
     * 普通缓存放入
     * @param key 键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 普通缓存放入并设置时间
     * @param key 键
     * @param value 值
     * @param time 时间(秒) time要大于0 如果time小于等于0 将设置无限期
     * @return true成功 false 失败
     */
    public boolean set(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.SECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 递增
     * @param key 键
     * @param delta 要增加几(大于0)
     */
    public long incr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递增因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, delta);
    }
    
    /**
     * 递减
     * @param key 键
     * @param delta 要减少几(小于0)
     */
    public long decr(String key, long delta) {
        if (delta < 0) {
            throw new RuntimeException("递减因子必须大于0");
        }
        return redisTemplate.opsForValue().increment(key, -delta);
    }
    
    // ================================Hash=================================
    
    /**
     * HashGet
     * @param key 键 不能为null
     * @param item 项 不能为null
     */
    public Object hget(String key, String item) {
        return redisTemplate.opsForHash().get(key, item);
    }
    
    /**
     * 获取hashKey对应的所有键值
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmget(String key) {
        return redisTemplate.opsForHash().entries(key);
    }
    
    /**
     * HashSet
     * @param key 键
     * @param map 对应多个键值
     */
    public boolean hmset(String key, Map<String, Object> map) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * HashSet 并设置时间
     * @param key 键
     * @param map 对应多个键值
     * @param time 时间(秒)
     * @return true成功 false失败
     */
    public boolean hmset(String key, Map<String, Object> map, long time) {
        try {
            redisTemplate.opsForHash().putAll(key, map);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 向一张hash表中放入数据,如果不存在将创建
     * @param key 键
     * @param item 项
     * @param value 值
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 向一张hash表中放入数据,如果不存在将创建
     * @param key 键
     * @param item 项
     * @param value 值
     * @param time 时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
     * @return true 成功 false失败
     */
    public boolean hset(String key, String item, Object value, long time) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 删除hash表中的值
     * @param key 键 不能为null
     * @param item 项 可以使多个 不能为null
     */
    public void hdel(String key, Object... item) {
        redisTemplate.opsForHash().delete(key, item);
    }
    
    /**
     * 判断hash表中是否有该项的值
     * @param key 键 不能为null
     * @param item 项 不能为null
     * @return true 存在 false不存在
     */
    public boolean hHasKey(String key, String item) {
        return redisTemplate.opsForHash().hasKey(key, item);
    }
    
    // ============================set=============================
    
    /**
     * 根据key获取Set中的所有值
     * @param key 键
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 根据value从一个set中查询,是否存在
     * @param key 键
     * @param value 值
     * @return true 存在 false不存在
     */
    public boolean sHasKey(String key, Object value) {
        try {
            return redisTemplate.opsForSet().isMember(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 将数据放入set缓存
     * @param key 键
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            return redisTemplate.opsForSet().add(key, values);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 将set数据放入缓存
     * @param key 键
     * @param time 时间(秒)
     * @param values 值 可以是多个
     * @return 成功个数
     */
    public long sSetAndTime(String key, long time, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 获取set缓存的长度
     * @param key 键
     */
    public long sGetSetSize(String key) {
        try {
            return redisTemplate.opsForSet().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 移除值为value的
     * @param key 键
     * @param values 值 可以是多个
     * @return 移除的个数
     */
    public long setRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    // ===============================list=================================
    
    /**
     * 获取list缓存的内容
     * @param key 键
     * @param start 开始
     * @param end 结束 0 到 -1代表所有值
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 获取list缓存的长度
     * @param key 键
     */
    public long lGetListSize(String key) {
        try {
            return redisTemplate.opsForList().size(key);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * 通过索引 获取list中的值
     * @param key 键
     * @param index 索引 index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
     */
    public Object lGetIndex(String key, long index) {
        try {
            return redisTemplate.opsForList().index(key, index);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     * @param time 时间(秒)
     */
    public boolean lSet(String key, Object value, long time) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     */
    public boolean lSet(String key, List<Object> value) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 将list放入缓存
     * @param key 键
     * @param value 值
     * @param time 时间(秒)
     */
    public boolean lSet(String key, List<Object> value, long time) {
        try {
            redisTemplate.opsForList().rightPushAll(key, value);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 根据索引修改list中的某条数据
     * @param key 键
     * @param index 索引
     * @param value 值
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 移除N个值为value
     * @param key 键
     * @param count 移除多少个
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, count, value);
            return remove;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
```

### 5. 使用示例

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/redis")
public class RedisController {
    
    @Autowired
    private RedisUtil redisUtil;
    
    @PostMapping("/set")
    public String setValue(@RequestParam String key, @RequestParam String value) {
        boolean result = redisUtil.set(key, value);
        return result ? "设置成功" : "设置失败";
    }
    
    @GetMapping("/get")
    public Object getValue(@RequestParam String key) {
        return redisUtil.get(key);
    }
    
    @PostMapping("/setWithExpire")
    public String setValueWithExpire(@RequestParam String key, 
                                   @RequestParam String value, 
                                   @RequestParam long seconds) {
        boolean result = redisUtil.set(key, value, seconds);
        return result ? "设置成功" : "设置失败";
    }
    
    @PostMapping("/hash/set")
    public String setHashValue(@RequestParam String key, 
                             @RequestParam String field, 
                             @RequestParam String value) {
        boolean result = redisUtil.hset(key, field, value);
        return result ? "设置成功" : "设置失败";
    }
    
    @GetMapping("/hash/get")
    public Object getHashValue(@RequestParam String key, @RequestParam String field) {
        return redisUtil.hget(key, field);
    }
    
    @GetMapping("/hash/getAll")
    public Map<Object, Object> getAllHashValues(@RequestParam String key) {
        return redisUtil.hmget(key);
    }
    
    @PostMapping("/incr")
    public long increment(@RequestParam String key, @RequestParam(defaultValue = "1") long delta) {
        return redisUtil.incr(key, delta);
    }
    
    @DeleteMapping("/delete")
    public String deleteKey(@RequestParam String key) {
        redisUtil.del(key);
        return "删除成功";
    }
    
    @GetMapping("/exists")
    public boolean keyExists(@RequestParam String key) {
        return redisUtil.hasKey(key);
    }
    
    @GetMapping("/ttl")
    public long getTimeToLive(@RequestParam String key) {
        return redisUtil.getExpire(key);
    }
}
```

## Redis实际应用场景

### 1. 缓存应用

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    
    @Autowired
    private RedisUtil redisUtil;
    
    @Autowired
    private UserRepository userRepository; // 假设的数据库访问层
    
    private static final String USER_CACHE_PREFIX = "user:";
    private static final long CACHE_EXPIRE_TIME = 3600; // 1小时
    
    public User getUserById(Long userId) {
        String cacheKey = USER_CACHE_PREFIX + userId;
        
        // 先从缓存获取
        Object cachedUser = redisUtil.get(cacheKey);
        if (cachedUser != null) {
            return (User) cachedUser;
        }
        
        // 缓存未命中，从数据库获取
        User user = userRepository.findById(userId);
        if (user != null) {
            // 存入缓存
            redisUtil.set(cacheKey, user, CACHE_EXPIRE_TIME);
        }
        
        return user;
    }
    
    public void updateUser(User user) {
        // 更新数据库
        userRepository.save(user);
        
        // 删除缓存，下次访问时重新加载
        String cacheKey = USER_CACHE_PREFIX + user.getId();
        redisUtil.del(cacheKey);
    }
    
    public void deleteUser(Long userId) {
        // 删除数据库记录
        userRepository.deleteById(userId);
        
        // 删除缓存
        String cacheKey = USER_CACHE_PREFIX + userId;
        redisUtil.del(cacheKey);
    }
}
```

### 2. 分布式锁

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class RedisDistributedLock {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String LOCK_PREFIX = "lock:";
    private static final String UNLOCK_SCRIPT = 
        "if redis.call('get', KEYS[1]) == ARGV[1] then " +
        "return redis.call('del', KEYS[1]) " +
        "else " +
        "return 0 " +
        "end";
    
    /**
     * 尝试获取分布式锁
     * @param lockKey 锁的key
     * @param expireTime 锁的过期时间（秒）
     * @return 锁的值（用于释放锁时验证）
     */
    public String tryLock(String lockKey, long expireTime) {
        String lockValue = UUID.randomUUID().toString();
        String key = LOCK_PREFIX + lockKey;
        
        Boolean success = redisTemplate.opsForValue()
            .setIfAbsent(key, lockValue, expireTime, TimeUnit.SECONDS);
        
        return success != null && success ? lockValue : null;
    }
    
    /**
     * 释放分布式锁
     * @param lockKey 锁的key
     * @param lockValue 锁的值
     * @return 是否释放成功
     */
    public boolean releaseLock(String lockKey, String lockValue) {
        String key = LOCK_PREFIX + lockKey;
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(UNLOCK_SCRIPT);
        script.setResultType(Long.class);
        
        Long result = redisTemplate.execute(script, 
            Collections.singletonList(key), lockValue);
        
        return result != null && result == 1L;
    }
    
    /**
     * 带重试的获取锁
     * @param lockKey 锁的key
     * @param expireTime 锁的过期时间（秒）
     * @param retryTimes 重试次数
     * @param retryInterval 重试间隔（毫秒）
     * @return 锁的值
     */
    public String tryLockWithRetry(String lockKey, long expireTime, 
                                 int retryTimes, long retryInterval) {
        for (int i = 0; i < retryTimes; i++) {
            String lockValue = tryLock(lockKey, expireTime);
            if (lockValue != null) {
                return lockValue;
            }
            
            try {
                Thread.sleep(retryInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return null;
    }
}

// 使用示例
@Service
public class OrderService {
    
    @Autowired
    private RedisDistributedLock distributedLock;
    
    public void processOrder(String orderId) {
        String lockKey = "order:" + orderId;
        String lockValue = distributedLock.tryLockWithRetry(lockKey, 30, 3, 100);
        
        if (lockValue == null) {
            throw new RuntimeException("获取锁失败，订单正在处理中");
        }
        
        try {
            // 处理订单逻辑
            System.out.println("处理订单: " + orderId);
            Thread.sleep(1000); // 模拟处理时间
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 释放锁
            distributedLock.releaseLock(lockKey, lockValue);
        }
    }
}
```

### 3. 限流器

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Component
public class RedisRateLimiter {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    
    // 滑动窗口限流脚本
    private static final String SLIDING_WINDOW_SCRIPT = 
        "local key = KEYS[1] " +
        "local window = tonumber(ARGV[1]) " +
        "local limit = tonumber(ARGV[2]) " +
        "local current = tonumber(ARGV[3]) " +
        "redis.call('zremrangebyscore', key, '-inf', current - window) " +
        "local count = redis.call('zcard', key) " +
        "if count < limit then " +
        "redis.call('zadd', key, current, current) " +
        "redis.call('expire', key, window) " +
        "return 1 " +
        "else " +
        "return 0 " +
        "end";
    
    /**
     * 滑动窗口限流
     * @param key 限流key
     * @param windowSize 窗口大小（秒）
     * @param limit 限制次数
     * @return 是否允许通过
     */
    public boolean slidingWindowRateLimit(String key, int windowSize, int limit) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        long currentTime = System.currentTimeMillis();
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(SLIDING_WINDOW_SCRIPT);
        script.setResultType(Long.class);
        
        Long result = redisTemplate.execute(script, 
            Collections.singletonList(redisKey), 
            windowSize * 1000L, limit, currentTime);
        
        return result != null && result == 1L;
    }
    
    /**
     * 令牌桶限流
     * @param key 限流key
     * @param capacity 桶容量
     * @param refillRate 令牌补充速率（每秒）
     * @param tokens 请求令牌数
     * @return 是否允许通过
     */
    public boolean tokenBucketRateLimit(String key, int capacity, 
                                      double refillRate, int tokens) {
        String redisKey = RATE_LIMIT_PREFIX + "bucket:" + key;
        long currentTime = System.currentTimeMillis();
        
        // 获取当前桶状态
        Object bucketData = redisTemplate.opsForValue().get(redisKey);
        
        TokenBucket bucket;
        if (bucketData == null) {
            bucket = new TokenBucket(capacity, capacity, currentTime);
        } else {
            bucket = (TokenBucket) bucketData;
        }
        
        // 计算需要补充的令牌数
        long timePassed = currentTime - bucket.getLastRefillTime();
        double tokensToAdd = (timePassed / 1000.0) * refillRate;
        
        bucket.setTokens(Math.min(capacity, bucket.getTokens() + tokensToAdd));
        bucket.setLastRefillTime(currentTime);
        
        // 检查是否有足够的令牌
        if (bucket.getTokens() >= tokens) {
            bucket.setTokens(bucket.getTokens() - tokens);
            
            // 更新桶状态
            redisTemplate.opsForValue().set(redisKey, bucket, 3600, TimeUnit.SECONDS);
            return true;
        } else {
            // 更新桶状态
            redisTemplate.opsForValue().set(redisKey, bucket, 3600, TimeUnit.SECONDS);
            return false;
        }
    }
    
    // 令牌桶数据结构
    public static class TokenBucket {
        private int capacity;
        private double tokens;
        private long lastRefillTime;
        
        public TokenBucket(int capacity, double tokens, long lastRefillTime) {
            this.capacity = capacity;
            this.tokens = tokens;
            this.lastRefillTime = lastRefillTime;
        }
        
        // getters and setters
        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
        public double getTokens() { return tokens; }
        public void setTokens(double tokens) { this.tokens = tokens; }
        public long getLastRefillTime() { return lastRefillTime; }
        public void setLastRefillTime(long lastRefillTime) { this.lastRefillTime = lastRefillTime; }
    }
}

// 使用示例
@RestController
public class ApiController {
    
    @Autowired
    private RedisRateLimiter rateLimiter;
    
    @GetMapping("/api/data")
    public ResponseEntity<String> getData(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        
        // 每个IP每分钟最多10次请求
        if (!rateLimiter.slidingWindowRateLimit(clientIp, 60, 10)) {
            return ResponseEntity.status(429).body("请求过于频繁，请稍后再试");
        }
        
        return ResponseEntity.ok("数据内容");
    }
    
    @GetMapping("/api/upload")
    public ResponseEntity<String> uploadFile(HttpServletRequest request) {
        String clientIp = getClientIp(request);
        
        // 令牌桶限流：容量5，每秒补充1个令牌
        if (!rateLimiter.tokenBucketRateLimit(clientIp, 5, 1.0, 1)) {
            return ResponseEntity.status(429).body("上传频率过高，请稍后再试");
        }
        
        return ResponseEntity.ok("上传成功");
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
```

### 4. 会话管理

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class SessionService {
    
    @Autowired
    private RedisUtil redisUtil;
    
    private static final String SESSION_PREFIX = "session:";
    private static final long SESSION_TIMEOUT = 1800; // 30分钟
    
    /**
     * 创建会话
     * @param userId 用户ID
     * @param userInfo 用户信息
     * @return 会话ID
     */
    public String createSession(String userId, Map<String, Object> userInfo) {
        String sessionId = UUID.randomUUID().toString();
        String sessionKey = SESSION_PREFIX + sessionId;
        
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", userId);
        sessionData.put("createTime", System.currentTimeMillis());
        sessionData.put("lastAccessTime", System.currentTimeMillis());
        sessionData.putAll(userInfo);
        
        redisUtil.hmset(sessionKey, sessionData, SESSION_TIMEOUT);
        
        return sessionId;
    }
    
    /**
     * 获取会话信息
     * @param sessionId 会话ID
     * @return 会话数据
     */
    public Map<Object, Object> getSession(String sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        
        if (!redisUtil.hasKey(sessionKey)) {
            return null;
        }
        
        Map<Object, Object> sessionData = redisUtil.hmget(sessionKey);
        
        // 更新最后访问时间
        redisUtil.hset(sessionKey, "lastAccessTime", System.currentTimeMillis());
        redisUtil.expire(sessionKey, SESSION_TIMEOUT);
        
        return sessionData;
    }
    
    /**
     * 更新会话信息
     * @param sessionId 会话ID
     * @param key 键
     * @param value 值
     */
    public void updateSession(String sessionId, String key, Object value) {
        String sessionKey = SESSION_PREFIX + sessionId;
        
        if (redisUtil.hasKey(sessionKey)) {
            redisUtil.hset(sessionKey, key, value);
            redisUtil.hset(sessionKey, "lastAccessTime", System.currentTimeMillis());
            redisUtil.expire(sessionKey, SESSION_TIMEOUT);
        }
    }
    
    /**
     * 删除会话
     * @param sessionId 会话ID
     */
    public void removeSession(String sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        redisUtil.del(sessionKey);
    }
    
    /**
     * 验证会话是否有效
     * @param sessionId 会话ID
     * @return 是否有效
     */
    public boolean isValidSession(String sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        return redisUtil.hasKey(sessionKey);
    }
    
    /**
     * 延长会话时间
     * @param sessionId 会话ID
     */
    public void extendSession(String sessionId) {
        String sessionKey = SESSION_PREFIX + sessionId;
        
        if (redisUtil.hasKey(sessionKey)) {
            redisUtil.hset(sessionKey, "lastAccessTime", System.currentTimeMillis());
            redisUtil.expire(sessionKey, SESSION_TIMEOUT);
        }
    }
}
```

### 5. 排行榜系统

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class RankingService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String RANKING_PREFIX = "ranking:";
    
    /**
     * 更新用户分数
     * @param rankingType 排行榜类型
     * @param userId 用户ID
     * @param score 分数
     */
    public void updateScore(String rankingType, String userId, double score) {
        String key = RANKING_PREFIX + rankingType;
        redisTemplate.opsForZSet().add(key, userId, score);
    }
    
    /**
     * 增加用户分数
     * @param rankingType 排行榜类型
     * @param userId 用户ID
     * @param increment 增加的分数
     */
    public void incrementScore(String rankingType, String userId, double increment) {
        String key = RANKING_PREFIX + rankingType;
        redisTemplate.opsForZSet().incrementScore(key, userId, increment);
    }
    
    /**
     * 获取排行榜（从高到低）
     * @param rankingType 排行榜类型
     * @param start 开始位置
     * @param end 结束位置
     * @return 排行榜列表
     */
    public List<RankingItem> getRanking(String rankingType, long start, long end) {
        String key = RANKING_PREFIX + rankingType;
        
        Set<ZSetOperations.TypedTuple<Object>> tuples = 
            redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        
        List<RankingItem> ranking = new ArrayList<>();
        long rank = start + 1;
        
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            RankingItem item = new RankingItem();
            item.setRank(rank++);
            item.setUserId((String) tuple.getValue());
            item.setScore(tuple.getScore());
            ranking.add(item);
        }
        
        return ranking;
    }
    
    /**
     * 获取用户排名
     * @param rankingType 排行榜类型
     * @param userId 用户ID
     * @return 用户排名信息
     */
    public RankingItem getUserRanking(String rankingType, String userId) {
        String key = RANKING_PREFIX + rankingType;
        
        Double score = redisTemplate.opsForZSet().score(key, userId);
        if (score == null) {
            return null;
        }
        
        Long rank = redisTemplate.opsForZSet().reverseRank(key, userId);
        
        RankingItem item = new RankingItem();
        item.setRank(rank != null ? rank + 1 : -1); // 排名从1开始
        item.setUserId(userId);
        item.setScore(score);
        
        return item;
    }
    
    /**
     * 获取指定分数范围的用户
     * @param rankingType 排行榜类型
     * @param minScore 最小分数
     * @param maxScore 最大分数
     * @return 用户列表
     */
    public List<RankingItem> getUsersByScoreRange(String rankingType, 
                                                 double minScore, double maxScore) {
        String key = RANKING_PREFIX + rankingType;
        
        Set<ZSetOperations.TypedTuple<Object>> tuples = 
            redisTemplate.opsForZSet().reverseRangeByScoreWithScores(key, minScore, maxScore);
        
        List<RankingItem> users = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            RankingItem item = new RankingItem();
            item.setUserId((String) tuple.getValue());
            item.setScore(tuple.getScore());
            users.add(item);
        }
        
        return users;
    }
    
    /**
     * 获取排行榜总人数
     * @param rankingType 排行榜类型
     * @return 总人数
     */
    public long getRankingSize(String rankingType) {
        String key = RANKING_PREFIX + rankingType;
        return redisTemplate.opsForZSet().zCard(key);
    }
    
    /**
     * 移除用户
     * @param rankingType 排行榜类型
     * @param userId 用户ID
     */
    public void removeUser(String rankingType, String userId) {
        String key = RANKING_PREFIX + rankingType;
        redisTemplate.opsForZSet().remove(key, userId);
    }
    
    /**
     * 清空排行榜
     * @param rankingType 排行榜类型
     */
    public void clearRanking(String rankingType) {
        String key = RANKING_PREFIX + rankingType;
        redisTemplate.delete(key);
    }
    
    // 排行榜项数据结构
    public static class RankingItem {
        private long rank;
        private String userId;
        private double score;
        
        // getters and setters
        public long getRank() { return rank; }
        public void setRank(long rank) { this.rank = rank; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }
}

// 使用示例
@RestController
@RequestMapping("/ranking")
public class RankingController {
    
    @Autowired
    private RankingService rankingService;
    
    @PostMapping("/update")
    public String updateScore(@RequestParam String type, 
                            @RequestParam String userId, 
                            @RequestParam double score) {
        rankingService.updateScore(type, userId, score);
        return "更新成功";
    }
    
    @PostMapping("/increment")
    public String incrementScore(@RequestParam String type, 
                               @RequestParam String userId, 
                               @RequestParam double increment) {
        rankingService.incrementScore(type, userId, increment);
        return "增加成功";
    }
    
    @GetMapping("/top")
    public List<RankingService.RankingItem> getTopRanking(
            @RequestParam String type, 
            @RequestParam(defaultValue = "0") int start, 
            @RequestParam(defaultValue = "9") int end) {
        return rankingService.getRanking(type, start, end);
    }
    
    @GetMapping("/user")
    public RankingService.RankingItem getUserRanking(
            @RequestParam String type, 
            @RequestParam String userId) {
        return rankingService.getUserRanking(type, userId);
    }
}
```

## Redis性能优化

### 1. 内存优化

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisMemoryOptimization {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 使用Hash存储对象，节省内存
     */
    public void optimizeObjectStorage() {
        // 不推荐：为每个用户创建单独的key
        // redisTemplate.opsForValue().set("user:1001:name", "张三");
        // redisTemplate.opsForValue().set("user:1001:age", 25);
        // redisTemplate.opsForValue().set("user:1001:email", "zhangsan@example.com");
        
        // 推荐：使用Hash存储
        redisTemplate.opsForHash().put("user:1001", "name", "张三");
        redisTemplate.opsForHash().put("user:1001", "age", 25);
        redisTemplate.opsForHash().put("user:1001", "email", "zhangsan@example.com");
    }
    
    /**
     * 使用位图节省内存
     */
    public void useBitmap() {
        String key = "user_online:" + getDateString();
        
        // 设置用户在线状态（用户ID作为偏移量）
        redisTemplate.opsForValue().setBit(key, 1001, true);
        redisTemplate.opsForValue().setBit(key, 1002, false);
        redisTemplate.opsForValue().setBit(key, 1003, true);
        
        // 检查用户是否在线
        Boolean isOnline = redisTemplate.opsForValue().getBit(key, 1001);
        System.out.println("用户1001是否在线: " + isOnline);
    }
    
    /**
     * 使用HyperLogLog统计唯一访客
     */
    public void useHyperLogLog() {
        String key = "unique_visitors:" + getDateString();
        
        // 添加访客
        redisTemplate.opsForHyperLogLog().add(key, "user1", "user2", "user3");
        redisTemplate.opsForHyperLogLog().add(key, "user1"); // 重复用户不会增加计数
        
        // 获取唯一访客数量
        Long uniqueCount = redisTemplate.opsForHyperLogLog().size(key);
        System.out.println("今日唯一访客数: " + uniqueCount);
    }
    
    private String getDateString() {
        return java.time.LocalDate.now().toString();
    }
}
```

### 2. 连接池优化

```yaml
# application.yml
spring:
  redis:
    lettuce:
      pool:
        # 连接池最大连接数
        max-active: 200
        # 连接池最大空闲连接数
        max-idle: 20
        # 连接池最小空闲连接数
        min-idle: 10
        # 连接池最大阻塞等待时间（使用负值表示没有限制）
        max-wait: 5000ms
      # 关闭超时时间
      shutdown-timeout: 100ms
    # 连接超时时间
    timeout: 5000ms
    # 命令超时时间
    command-timeout: 5000ms
```

### 3. 批量操作优化

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class RedisBatchOptimization {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 批量设置键值对
     */
    public void batchSet(Map<String, Object> keyValues) {
        redisTemplate.opsForValue().multiSet(keyValues);
    }
    
    /**
     * 批量获取值
     */
    public List<Object> batchGet(List<String> keys) {
        return redisTemplate.opsForValue().multiGet(keys);
    }
    
    /**
     * 使用Pipeline批量操作
     */
    public void pipelineOperations() {
        redisTemplate.executePipelined((connection) -> {
            for (int i = 0; i < 1000; i++) {
                connection.set(("key" + i).getBytes(), ("value" + i).getBytes());
            }
            return null;
        });
    }
    
    /**
     * 使用Lua脚本原子性批量操作
     */
    public void luaScriptBatch() {
        String script = 
            "for i=1,#KEYS do " +
            "redis.call('set', KEYS[i], ARGV[i]) " +
            "end " +
            "return #KEYS";
        
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        
        List<String> keys = Arrays.asList("key1", "key2", "key3");
        List<String> values = Arrays.asList("value1", "value2", "value3");
        
        Long result = redisTemplate.execute(redisScript, keys, values.toArray());
        System.out.println("批量设置了 " + result + " 个键值对");
    }
}
```

## Redis最佳实践

### 1. 键命名规范

```java
public class RedisKeyNamingConvention {
    
    // 推荐的键命名规范
    public static final String USER_PREFIX = "user:";
    public static final String SESSION_PREFIX = "session:";
    public static final String CACHE_PREFIX = "cache:";
    public static final String LOCK_PREFIX = "lock:";
    public static final String COUNTER_PREFIX = "counter:";
    
    // 生成用户缓存键
    public static String getUserCacheKey(Long userId) {
        return USER_PREFIX + "cache:" + userId;
    }
    
    // 生成用户会话键
    public static String getUserSessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }
    
    // 生成分布式锁键
    public static String getLockKey(String resource) {
        return LOCK_PREFIX + resource;
    }
    
    // 生成计数器键
    public static String getCounterKey(String type, String date) {
        return COUNTER_PREFIX + type + ":" + date;
    }
    
    // 键命名最佳实践
    public static class KeyNamingBestPractices {
        // 1. 使用冒号分隔层级
        // 好: user:profile:1001
        // 坏: user_profile_1001
        
        // 2. 使用有意义的前缀
        // 好: cache:user:1001
        // 坏: c:u:1001
        
        // 3. 避免过长的键名
        // 好: product:1001
        // 坏: very_long_product_name_with_details:1001
        
        // 4. 使用小写字母
        // 好: user:session:abc123
        // 坏: User:Session:ABC123
        
        // 5. 包含版本信息（如果需要）
        // 好: api:v1:user:1001
        // 坏: api:user:1001
    }
}
```

### 2. 过期时间设置

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisExpirationBestPractices {
    
    @Autowired
    private RedisUtil redisUtil;
    
    // 不同类型数据的推荐过期时间
    private static final long CACHE_EXPIRE_SHORT = 300;      // 5分钟
    private static final long CACHE_EXPIRE_MEDIUM = 1800;    // 30分钟
    private static final long CACHE_EXPIRE_LONG = 3600;      // 1小时
    private static final long SESSION_EXPIRE = 7200;        // 2小时
    private static final long VERIFICATION_CODE_EXPIRE = 300; // 5分钟
    
    /**
     * 缓存用户信息（中等过期时间）
     */
    public void cacheUserInfo(String userId, Object userInfo) {
        String key = "user:info:" + userId;
        redisUtil.set(key, userInfo, CACHE_EXPIRE_MEDIUM);
    }
    
    /**
     * 缓存热点数据（长过期时间）
     */
    public void cacheHotData(String key, Object data) {
        redisUtil.set("hot:" + key, data, CACHE_EXPIRE_LONG);
    }
    
    /**
     * 存储验证码（短过期时间）
     */
    public void storeVerificationCode(String phone, String code) {
        String key = "verify:" + phone;
        redisUtil.set(key, code, VERIFICATION_CODE_EXPIRE);
    }
    
    /**
     * 随机过期时间，避免缓存雪崩
     */
    public void setWithRandomExpire(String key, Object value, long baseExpire) {
        // 在基础过期时间上增加随机时间（0-300秒）
        long randomExpire = baseExpire + (long) (Math.random() * 300);
        redisUtil.set(key, value, randomExpire);
    }
    
    /**
     * 设置过期时间的最佳实践
     */
    public void expirationBestPractices() {
        // 1. 根据数据特性设置合适的过期时间
        // 2. 为相同类型的数据设置随机过期时间，避免同时失效
        // 3. 对于重要数据，考虑使用软过期（在业务层面处理）
        // 4. 监控过期键的数量，避免大量键同时过期影响性能
    }
}
```

### 3. 异常处理

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.stereotype.Component;

@Component
public class RedisExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisExceptionHandler.class);
    
    @Autowired
    private RedisUtil redisUtil;
    
    /**
     * 安全的缓存获取
     */
    public Object safeGet(String key, Object defaultValue) {
        try {
            Object value = redisUtil.get(key);
            return value != null ? value : defaultValue;
        } catch (RedisConnectionFailureException e) {
            logger.error("Redis连接失败，使用默认值: {}", e.getMessage());
            return defaultValue;
        } catch (DataAccessException e) {
            logger.error("Redis数据访问异常，使用默认值: {}", e.getMessage());
            return defaultValue;
        } catch (Exception e) {
            logger.error("Redis操作异常，使用默认值: {}", e.getMessage());
            return defaultValue;
        }
    }
    
    /**
     * 安全的缓存设置
     */
    public boolean safeSet(String key, Object value, long expire) {
        try {
            return redisUtil.set(key, value, expire);
        } catch (RedisConnectionFailureException e) {
            logger.error("Redis连接失败，缓存设置失败: {}", e.getMessage());
            return false;
        } catch (DataAccessException e) {
            logger.error("Redis数据访问异常，缓存设置失败: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Redis操作异常，缓存设置失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 带降级的缓存获取
     */
    public <T> T getWithFallback(String key, Class<T> clazz, 
                                java.util.function.Supplier<T> fallbackSupplier) {
        try {
            Object cached = redisUtil.get(key);
            if (cached != null) {
                return clazz.cast(cached);
            }
        } catch (Exception e) {
            logger.warn("Redis获取失败，使用降级方案: {}", e.getMessage());
        }
        
        // 降级到其他数据源
        T result = fallbackSupplier.get();
        
        // 尝试重新缓存
        try {
            redisUtil.set(key, result, 300); // 短期缓存
        } catch (Exception e) {
            logger.warn("Redis缓存设置失败: {}", e.getMessage());
        }
        
        return result;
    }
}
```

## 面试要点

### 高频问题

1. **Redis的数据类型有哪些？各自的应用场景？**
   - String：缓存、计数器、分布式锁
   - Hash：对象存储、购物车
   - List：消息队列、最新列表
   - Set：标签、好友关系、去重
   - Sorted Set：排行榜、延时队列

2. **Redis持久化机制？**
   - RDB：快照持久化，适合备份和灾难恢复
   - AOF：追加文件，数据安全性更高
   - 混合持久化：RDB + AOF，兼顾性能和安全

3. **Redis缓存穿透、缓存击穿、缓存雪崩的解决方案？**
   - 缓存穿透：布隆过滤器、空值缓存
   - 缓存击穿：互斥锁、永不过期
   - 缓存雪崩：随机过期时间、多级缓存

### 深入问题

1. **Redis的内存淘汰策略？**
   - noeviction：不淘汰，内存满时报错
   - allkeys-lru：所有键LRU淘汰
   - volatile-lru：有过期时间的键LRU淘汰
   - allkeys-random：所有键随机淘汰
   - volatile-random：有过期时间的键随机淘汰
   - volatile-ttl：有过期时间的键按TTL淘汰

2. **Redis集群方案？**
   - 主从复制：读写分离，数据备份
   - 哨兵模式：自动故障转移
   - 集群模式：数据分片，水平扩展

3. **Redis事务的特点？**
   - 原子性：事务中的命令要么全部执行，要么全部不执行
   - 一致性：事务执行前后数据保持一致
   - 隔离性：事务之间相互隔离
   - 不支持回滚：命令错误不会回滚已执行的命令

### 实践经验

1. **如何设计Redis缓存架构？**
   - 缓存层次设计：L1本地缓存 + L2分布式缓存
   - 缓存更新策略：Cache Aside、Write Through、Write Behind
   - 缓存一致性：最终一致性、强一致性方案

2. **Redis性能优化经验？**
   - 合理设置过期时间
   - 使用连接池
   - 批量操作
   - 避免大key
   - 选择合适的数据结构

3. **Redis监控和运维？**
   - 关键指标：内存使用率、命中率、QPS、延迟
   - 监控工具：Redis-cli、RedisInsight、Prometheus
   - 故障处理：主从切换、数据恢复、性能调优

## 总结

Redis作为高性能的内存数据库，在现代应用架构中扮演着重要角色。本文详细介绍了：

### 核心特性
- **丰富的数据类型**：String、Hash、List、Set、Sorted Set
- **高性能**：基于内存存储，支持持久化
- **高可用**：主从复制、哨兵模式、集群模式
- **原子操作**：所有操作都是原子性的

### 实际应用
- **缓存系统**：提升应用性能，减少数据库压力
- **会话存储**：分布式会话管理
- **分布式锁**：解决并发问题
- **限流器**：API限流、防刷
- **排行榜**：实时排名系统

### 最佳实践
- **合理的键命名规范**
- **适当的过期时间设置**
- **完善的异常处理机制**
- **性能优化策略**
- **监控和运维**

### 学习建议
1. **理论基础**：深入理解Redis的数据结构和原理
2. **实践应用**：通过项目实践掌握各种使用场景
3. **性能调优**：学习Redis性能优化技巧
4. **运维监控**：掌握Redis的部署和监控方法

Redis的学习是一个循序渐进的过程，需要在实践中不断积累经验，才能真正掌握其精髓。