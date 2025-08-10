# 分布式锁详解

## 目录
- [分布式锁概述](#分布式锁概述)
- [实现方案对比](#实现方案对比)
- [基于数据库的分布式锁](#基于数据库的分布式锁)
- [基于Redis的分布式锁](#基于redis的分布式锁)
- [基于ZooKeeper的分布式锁](#基于zookeeper的分布式锁)
- [基于Etcd的分布式锁](#基于etcd的分布式锁)
- [Redlock算法](#redlock算法)
- [性能对比与选择](#性能对比与选择)
- [最佳实践](#最佳实践)
- [面试要点](#面试要点)

## 分布式锁概述

### 什么是分布式锁
分布式锁是在分布式系统中实现互斥访问共享资源的一种机制。它确保在分布式环境下，同一时刻只有一个进程能够访问特定的资源。

### 为什么需要分布式锁

```java
// 没有分布式锁的问题示例
public class InventoryService {
    private int inventory = 100;
    
    // 在分布式环境下，这个方法不是线程安全的
    public boolean purchaseProduct(int quantity) {
        if (inventory >= quantity) {
            // 模拟业务处理时间
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            inventory -= quantity;
            System.out.println("购买成功，剩余库存：" + inventory);
            return true;
        } else {
            System.out.println("库存不足，购买失败");
            return false;
        }
    }
}
```

**问题：**
- 多个服务实例同时读取库存
- 可能导致超卖问题
- 数据不一致

### 分布式锁的特性

1. **互斥性**：同一时刻只有一个客户端能持有锁
2. **安全性**：锁只能被持有锁的客户端释放
3. **活性**：不会发生死锁，最终能够获取锁
4. **容错性**：部分节点故障不影响锁服务
5. **高性能**：获取和释放锁的开销要小

## 实现方案对比

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| 数据库 | 实现简单，事务支持 | 性能较低，单点故障 | 对性能要求不高的场景 |
| Redis | 性能高，实现简单 | 可能丢失锁，需要考虑主从切换 | 高性能要求，可以容忍偶尔失效 |
| ZooKeeper | 强一致性，可靠性高 | 性能相对较低，复杂度高 | 对一致性要求极高的场景 |
| Etcd | 强一致性，现代化 | 相对较新，生态不如ZK | 云原生环境，K8s相关 |

## 基于数据库的分布式锁

### 基于唯一索引实现

```sql
-- 创建锁表
CREATE TABLE distributed_lock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    lock_name VARCHAR(255) NOT NULL UNIQUE,
    holder VARCHAR(255) NOT NULL,
    expire_time TIMESTAMP NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_expire_time (expire_time)
);
```

```java
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class DatabaseDistributedLock {
    private final String dataSourceUrl;
    private final String lockName;
    private final String holder;
    private final int expireSeconds;
    
    public DatabaseDistributedLock(String dataSourceUrl, String lockName, int expireSeconds) {
        this.dataSourceUrl = dataSourceUrl;
        this.lockName = lockName;
        this.holder = UUID.randomUUID().toString();
        this.expireSeconds = expireSeconds;
    }
    
    public boolean tryLock() {
        try (Connection conn = DriverManager.getConnection(dataSourceUrl)) {
            // 先清理过期锁
            cleanExpiredLocks(conn);
            
            // 尝试获取锁
            String sql = "INSERT INTO distributed_lock (lock_name, holder, expire_time) VALUES (?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, lockName);
                stmt.setString(2, holder);
                
                LocalDateTime expireTime = LocalDateTime.now().plusSeconds(expireSeconds);
                stmt.setTimestamp(3, Timestamp.valueOf(expireTime));
                
                int result = stmt.executeUpdate();
                return result > 0;
            }
        } catch (SQLException e) {
            // 唯一索引冲突，说明锁已被占用
            if (e.getErrorCode() == 1062) { // MySQL duplicate entry error
                return false;
            }
            throw new RuntimeException("Failed to acquire lock", e);
        }
    }
    
    public boolean tryLock(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (tryLock()) {
                return true;
            }
            
            try {
                Thread.sleep(100); // 等待100ms后重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }
    
    public void unlock() {
        try (Connection conn = DriverManager.getConnection(dataSourceUrl)) {
            String sql = "DELETE FROM distributed_lock WHERE lock_name = ? AND holder = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, lockName);
                stmt.setString(2, holder);
                
                int result = stmt.executeUpdate();
                if (result == 0) {
                    System.out.println("Warning: Lock not found or not owned by current holder");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to release lock", e);
        }
    }
    
    private void cleanExpiredLocks(Connection conn) throws SQLException {
        String sql = "DELETE FROM distributed_lock WHERE expire_time < NOW()";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }
    
    // 锁续期
    public boolean renewLock() {
        try (Connection conn = DriverManager.getConnection(dataSourceUrl)) {
            String sql = "UPDATE distributed_lock SET expire_time = ? WHERE lock_name = ? AND holder = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                LocalDateTime newExpireTime = LocalDateTime.now().plusSeconds(expireSeconds);
                stmt.setTimestamp(1, Timestamp.valueOf(newExpireTime));
                stmt.setString(2, lockName);
                stmt.setString(3, holder);
                
                int result = stmt.executeUpdate();
                return result > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to renew lock", e);
        }
    }
}
```

### 基于悲观锁实现

```java
public class PessimisticDatabaseLock {
    private final String dataSourceUrl;
    private final String lockName;
    private Connection connection;
    
    public PessimisticDatabaseLock(String dataSourceUrl, String lockName) {
        this.dataSourceUrl = dataSourceUrl;
        this.lockName = lockName;
    }
    
    public boolean tryLock() {
        try {
            connection = DriverManager.getConnection(dataSourceUrl);
            connection.setAutoCommit(false);
            
            // 使用SELECT ... FOR UPDATE获取行锁
            String sql = "SELECT lock_name FROM distributed_lock WHERE lock_name = ? FOR UPDATE";
            try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, lockName);
                stmt.setQueryTimeout(1); // 1秒超时
                
                ResultSet rs = stmt.executeQuery();
                if (!rs.next()) {
                    // 锁不存在，创建锁
                    String insertSql = "INSERT INTO distributed_lock (lock_name, holder, expire_time) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStmt = connection.prepareStatement(insertSql)) {
                        insertStmt.setString(1, lockName);
                        insertStmt.setString(2, Thread.currentThread().getName());
                        insertStmt.setTimestamp(3, new Timestamp(System.currentTimeMillis() + 30000));
                        insertStmt.executeUpdate();
                    }
                }
                
                return true;
            }
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    connection.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        }
    }
    
    public void unlock() {
        if (connection != null) {
            try {
                connection.commit();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
```

## 基于Redis的分布式锁

### 基础实现

```java
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

public class RedisDistributedLock {
    private final Jedis jedis;
    private final String lockKey;
    private final String lockValue;
    private final int expireTime;
    
    public RedisDistributedLock(Jedis jedis, String lockKey, int expireTime) {
        this.jedis = jedis;
        this.lockKey = lockKey;
        this.lockValue = UUID.randomUUID().toString();
        this.expireTime = expireTime;
    }
    
    public boolean tryLock() {
        SetParams params = SetParams.setParams().nx().ex(expireTime);
        String result = jedis.set(lockKey, lockValue, params);
        return "OK".equals(result);
    }
    
    public boolean tryLock(long timeoutMs) {
        long startTime = System.currentTimeMillis();
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (tryLock()) {
                return true;
            }
            
            try {
                Thread.sleep(50); // 等待50ms后重试
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        return false;
    }
    
    public void unlock() {
        // 使用Lua脚本确保原子性
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";
        
        jedis.eval(script, 1, lockKey, lockValue);
    }
    
    // 锁续期
    public boolean renewLock() {
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('expire', KEYS[1], ARGV[2]) " +
            "else " +
            "    return 0 " +
            "end";
        
        Object result = jedis.eval(script, 1, lockKey, lockValue, String.valueOf(expireTime));
        return "1".equals(result.toString());
    }
}
```

### 自动续期的Redis锁

```java
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoRenewRedisLock {
    private final Jedis jedis;
    private final String lockKey;
    private final String lockValue;
    private final int expireTime;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean locked = new AtomicBoolean(false);
    
    public AutoRenewRedisLock(Jedis jedis, String lockKey, int expireTime) {
        this.jedis = jedis;
        this.lockKey = lockKey;
        this.lockValue = UUID.randomUUID().toString();
        this.expireTime = expireTime;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RedisLock-Renew-" + lockKey);
            t.setDaemon(true);
            return t;
        });
    }
    
    public boolean tryLock() {
        SetParams params = SetParams.setParams().nx().ex(expireTime);
        String result = jedis.set(lockKey, lockValue, params);
        
        if ("OK".equals(result)) {
            locked.set(true);
            startRenewal();
            return true;
        }
        
        return false;
    }
    
    private void startRenewal() {
        // 每隔expireTime/3的时间续期一次
        long renewalInterval = expireTime * 1000L / 3;
        
        scheduler.scheduleAtFixedRate(() -> {
            if (locked.get()) {
                boolean renewed = renewLock();
                if (!renewed) {
                    System.out.println("Failed to renew lock: " + lockKey);
                    locked.set(false);
                }
            }
        }, renewalInterval, renewalInterval, TimeUnit.MILLISECONDS);
    }
    
    private boolean renewLock() {
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('expire', KEYS[1], ARGV[2]) " +
            "else " +
            "    return 0 " +
            "end";
        
        Object result = jedis.eval(script, 1, lockKey, lockValue, String.valueOf(expireTime));
        return "1".equals(result.toString());
    }
    
    public void unlock() {
        locked.set(false);
        
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";
        
        jedis.eval(script, 1, lockKey, lockValue);
        scheduler.shutdown();
    }
    
    public boolean isLocked() {
        return locked.get();
    }
}
```

### 可重入Redis锁

```java
public class ReentrantRedisLock {
    private final Jedis jedis;
    private final String lockKey;
    private final String threadId;
    private final int expireTime;
    
    public ReentrantRedisLock(Jedis jedis, String lockKey, int expireTime) {
        this.jedis = jedis;
        this.lockKey = lockKey;
        this.threadId = Thread.currentThread().getId() + ":" + UUID.randomUUID().toString();
        this.expireTime = expireTime;
    }
    
    public boolean tryLock() {
        // 使用Lua脚本实现可重入逻辑
        String script = 
            "local key = KEYS[1] " +
            "local threadId = ARGV[1] " +
            "local expireTime = ARGV[2] " +
            "local lockValue = redis.call('get', key) " +
            "if lockValue == false then " +
            "    redis.call('hset', key, threadId, 1) " +
            "    redis.call('expire', key, expireTime) " +
            "    return 1 " +
            "elseif redis.call('hexists', key, threadId) == 1 then " +
            "    redis.call('hincrby', key, threadId, 1) " +
            "    redis.call('expire', key, expireTime) " +
            "    return 1 " +
            "else " +
            "    return 0 " +
            "end";
        
        Object result = jedis.eval(script, 1, lockKey, threadId, String.valueOf(expireTime));
        return "1".equals(result.toString());
    }
    
    public void unlock() {
        String script = 
            "local key = KEYS[1] " +
            "local threadId = ARGV[1] " +
            "if redis.call('hexists', key, threadId) == 0 then " +
            "    return 0 " +
            "end " +
            "local count = redis.call('hincrby', key, threadId, -1) " +
            "if count > 0 then " +
            "    return 1 " +
            "else " +
            "    redis.call('del', key) " +
            "    return 1 " +
            "end";
        
        jedis.eval(script, 1, lockKey, threadId);
    }
    
    public int getLockCount() {
        String count = jedis.hget(lockKey, threadId);
        return count != null ? Integer.parseInt(count) : 0;
    }
}
```

## 基于ZooKeeper的分布式锁

### 基础实现

```java
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ZooKeeperDistributedLock implements Watcher {
    private final ZooKeeper zooKeeper;
    private final String lockPath;
    private final String lockPrefix;
    private String currentLockPath;
    private String waitPath;
    private CountDownLatch latch;
    
    public ZooKeeperDistributedLock(ZooKeeper zooKeeper, String lockPath) {
        this.zooKeeper = zooKeeper;
        this.lockPath = lockPath;
        this.lockPrefix = lockPath + "/lock-";
        
        // 确保锁路径存在
        try {
            Stat stat = zooKeeper.exists(lockPath, false);
            if (stat == null) {
                zooKeeper.create(lockPath, new byte[0], 
                               ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create lock path", e);
        }
    }
    
    public boolean tryLock() {
        try {
            // 创建临时顺序节点
            currentLockPath = zooKeeper.create(lockPrefix, new byte[0],
                                             ZooDefs.Ids.OPEN_ACL_UNSAFE, 
                                             CreateMode.EPHEMERAL_SEQUENTIAL);
            
            // 获取所有子节点并排序
            List<String> children = zooKeeper.getChildren(lockPath, false);
            Collections.sort(children);
            
            // 获取当前节点的序号
            String currentNode = currentLockPath.substring(lockPath.length() + 1);
            int currentIndex = children.indexOf(currentNode);
            
            if (currentIndex == 0) {
                // 当前节点是最小的，获得锁
                return true;
            } else {
                // 监听前一个节点
                String prevNode = children.get(currentIndex - 1);
                waitPath = lockPath + "/" + prevNode;
                
                latch = new CountDownLatch(1);
                Stat stat = zooKeeper.exists(waitPath, this);
                
                if (stat == null) {
                    // 前一个节点已经不存在，重新尝试获取锁
                    return tryLock();
                }
                
                return false;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to acquire lock", e);
        }
    }
    
    public boolean tryLock(long timeout, TimeUnit unit) {
        if (tryLock()) {
            return true;
        }
        
        try {
            return latch.await(timeout, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public void unlock() {
        try {
            if (currentLockPath != null) {
                zooKeeper.delete(currentLockPath, -1);
                currentLockPath = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to release lock", e);
        }
    }
    
    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeDeleted && 
            event.getPath().equals(waitPath)) {
            // 前一个节点被删除，尝试获取锁
            if (tryLock()) {
                latch.countDown();
            }
        }
    }
}
```

### 改进版ZooKeeper锁

```java
public class ImprovedZooKeeperLock implements Watcher {
    private final ZooKeeper zooKeeper;
    private final String lockPath;
    private final String lockPrefix;
    private final long sessionTimeout;
    private String currentLockPath;
    private String waitPath;
    private CountDownLatch latch;
    private volatile boolean locked = false;
    
    public ImprovedZooKeeperLock(ZooKeeper zooKeeper, String lockPath, long sessionTimeout) {
        this.zooKeeper = zooKeeper;
        this.lockPath = lockPath;
        this.lockPrefix = lockPath + "/lock-";
        this.sessionTimeout = sessionTimeout;
        
        ensureLockPathExists();
    }
    
    private void ensureLockPathExists() {
        try {
            Stat stat = zooKeeper.exists(lockPath, false);
            if (stat == null) {
                // 创建持久节点
                String[] paths = lockPath.split("/");
                StringBuilder currentPath = new StringBuilder();
                
                for (String path : paths) {
                    if (!path.isEmpty()) {
                        currentPath.append("/").append(path);
                        try {
                            zooKeeper.create(currentPath.toString(), new byte[0],
                                           ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
                        } catch (KeeperException.NodeExistsException e) {
                            // 节点已存在，忽略
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to ensure lock path exists", e);
        }
    }
    
    public boolean tryLock() {
        try {
            return attemptLock();
        } catch (Exception e) {
            throw new RuntimeException("Failed to acquire lock", e);
        }
    }
    
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        long timeoutMs = unit.toMillis(timeout);
        
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (tryLock()) {
                return true;
            }
            
            if (latch != null) {
                long remainingTime = timeoutMs - (System.currentTimeMillis() - startTime);
                if (remainingTime > 0) {
                    latch.await(remainingTime, TimeUnit.MILLISECONDS);
                }
            }
        }
        
        return false;
    }
    
    private boolean attemptLock() throws KeeperException, InterruptedException {
        // 创建临时顺序节点
        if (currentLockPath == null) {
            currentLockPath = zooKeeper.create(lockPrefix, new byte[0],
                                             ZooDefs.Ids.OPEN_ACL_UNSAFE,
                                             CreateMode.EPHEMERAL_SEQUENTIAL);
        }
        
        // 获取所有子节点
        List<String> children = zooKeeper.getChildren(lockPath, false);
        Collections.sort(children);
        
        String currentNode = currentLockPath.substring(lockPath.length() + 1);
        int currentIndex = children.indexOf(currentNode);
        
        if (currentIndex == 0) {
            // 获得锁
            locked = true;
            return true;
        } else {
            // 监听前一个节点
            String prevNode = children.get(currentIndex - 1);
            waitPath = lockPath + "/" + prevNode;
            
            latch = new CountDownLatch(1);
            Stat stat = zooKeeper.exists(waitPath, this);
            
            if (stat == null) {
                // 前一个节点已删除，重新尝试
                return attemptLock();
            }
            
            return false;
        }
    }
    
    public void unlock() {
        try {
            if (currentLockPath != null && locked) {
                zooKeeper.delete(currentLockPath, -1);
                currentLockPath = null;
                locked = false;
            }
        } catch (Exception e) {
            System.err.println("Failed to release lock: " + e.getMessage());
        }
    }
    
    @Override
    public void process(WatchedEvent event) {
        if (event.getType() == Event.EventType.NodeDeleted &&
            event.getPath().equals(waitPath)) {
            try {
                if (attemptLock()) {
                    latch.countDown();
                }
            } catch (Exception e) {
                System.err.println("Error in watcher: " + e.getMessage());
            }
        }
    }
    
    public boolean isLocked() {
        return locked;
    }
    
    // 获取锁的持有者信息
    public String getLockInfo() {
        try {
            List<String> children = zooKeeper.getChildren(lockPath, false);
            if (!children.isEmpty()) {
                Collections.sort(children);
                String lockHolder = children.get(0);
                return lockPath + "/" + lockHolder;
            }
        } catch (Exception e) {
            System.err.println("Failed to get lock info: " + e.getMessage());
        }
        return null;
    }
}
```

## 基于Etcd的分布式锁

```java
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Lock;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lock.LockResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;

public class EtcdDistributedLock {
    private final Client etcdClient;
    private final Lock lockClient;
    private final Lease leaseClient;
    private final String lockName;
    private final long leaseTime;
    private long leaseId;
    private ByteSequence lockKey;
    
    public EtcdDistributedLock(Client etcdClient, String lockName, long leaseTime) {
        this.etcdClient = etcdClient;
        this.lockClient = etcdClient.getLockClient();
        this.leaseClient = etcdClient.getLeaseClient();
        this.lockName = lockName;
        this.leaseTime = leaseTime;
    }
    
    public boolean tryLock() {
        try {
            // 创建租约
            LeaseGrantResponse leaseResponse = leaseClient.grant(leaseTime).get();
            leaseId = leaseResponse.getID();
            
            // 尝试获取锁
            ByteSequence lockNameBytes = ByteSequence.from(lockName, StandardCharsets.UTF_8);
            CompletableFuture<LockResponse> lockFuture = lockClient.lock(lockNameBytes, leaseId);
            
            LockResponse lockResponse = lockFuture.get(1, TimeUnit.SECONDS);
            lockKey = lockResponse.getKey();
            
            return true;
        } catch (Exception e) {
            // 清理租约
            if (leaseId != 0) {
                try {
                    leaseClient.revoke(leaseId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        }
    }
    
    public boolean tryLock(long timeout, TimeUnit unit) {
        try {
            // 创建租约
            LeaseGrantResponse leaseResponse = leaseClient.grant(leaseTime).get();
            leaseId = leaseResponse.getID();
            
            // 尝试获取锁
            ByteSequence lockNameBytes = ByteSequence.from(lockName, StandardCharsets.UTF_8);
            CompletableFuture<LockResponse> lockFuture = lockClient.lock(lockNameBytes, leaseId);
            
            LockResponse lockResponse = lockFuture.get(timeout, unit);
            lockKey = lockResponse.getKey();
            
            return true;
        } catch (Exception e) {
            // 清理租约
            if (leaseId != 0) {
                try {
                    leaseClient.revoke(leaseId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        }
    }
    
    public void unlock() {
        try {
            if (lockKey != null) {
                lockClient.unlock(lockKey).get();
            }
        } catch (Exception e) {
            System.err.println("Failed to unlock: " + e.getMessage());
        } finally {
            // 撤销租约
            if (leaseId != 0) {
                try {
                    leaseClient.revoke(leaseId).get();
                } catch (Exception e) {
                    System.err.println("Failed to revoke lease: " + e.getMessage());
                }
            }
        }
    }
    
    // 续期锁
    public boolean renewLock() {
        try {
            if (leaseId != 0) {
                leaseClient.keepAliveOnce(leaseId).get();
                return true;
            }
        } catch (Exception e) {
            System.err.println("Failed to renew lock: " + e.getMessage());
        }
        return false;
    }
}
```

## Redlock算法

### Redlock实现

```java
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class Redlock {
    private final List<Jedis> redisInstances;
    private final int quorum;
    private final int retryDelay;
    private final int retryCount;
    private final double clockDriftFactor = 0.01;
    
    public Redlock(List<Jedis> redisInstances) {
        this.redisInstances = redisInstances;
        this.quorum = redisInstances.size() / 2 + 1;
        this.retryDelay = 200; // ms
        this.retryCount = 3;
    }
    
    public RedlockResult tryLock(String resource, int ttl) {
        String value = UUID.randomUUID().toString();
        
        for (int i = 0; i < retryCount; i++) {
            RedlockResult result = attemptLock(resource, value, ttl);
            if (result.isSuccess()) {
                return result;
            }
            
            // 随机延迟后重试
            try {
                Thread.sleep(ThreadLocalRandom.current().nextInt(retryDelay));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        return new RedlockResult(false, null, 0, 0);
    }
    
    private RedlockResult attemptLock(String resource, String value, int ttl) {
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        List<String> lockedInstances = new ArrayList<>();
        
        // 尝试在所有Redis实例上获取锁
        for (int i = 0; i < redisInstances.size(); i++) {
            Jedis jedis = redisInstances.get(i);
            if (lockInstance(jedis, resource, value, ttl)) {
                successCount++;
                lockedInstances.add(String.valueOf(i));
            }
        }
        
        long elapsedTime = System.currentTimeMillis() - startTime;
        long validityTime = ttl - elapsedTime - (long) (ttl * clockDriftFactor);
        
        if (successCount >= quorum && validityTime > 0) {
            return new RedlockResult(true, value, validityTime, elapsedTime);
        } else {
            // 释放已获取的锁
            unlockInstances(resource, value, lockedInstances);
            return new RedlockResult(false, null, 0, elapsedTime);
        }
    }
    
    private boolean lockInstance(Jedis jedis, String resource, String value, int ttl) {
        try {
            SetParams params = SetParams.setParams().nx().px(ttl);
            String result = jedis.set(resource, value, params);
            return "OK".equals(result);
        } catch (Exception e) {
            return false;
        }
    }
    
    public void unlock(String resource, String value) {
        unlockInstances(resource, value, null);
    }
    
    private void unlockInstances(String resource, String value, List<String> specificInstances) {
        String script = 
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
            "    return redis.call('del', KEYS[1]) " +
            "else " +
            "    return 0 " +
            "end";
        
        for (int i = 0; i < redisInstances.size(); i++) {
            if (specificInstances == null || specificInstances.contains(String.valueOf(i))) {
                try {
                    Jedis jedis = redisInstances.get(i);
                    jedis.eval(script, 1, resource, value);
                } catch (Exception e) {
                    // 忽略解锁失败
                }
            }
        }
    }
    
    // Redlock结果
    public static class RedlockResult {
        private final boolean success;
        private final String value;
        private final long validityTime;
        private final long elapsedTime;
        
        public RedlockResult(boolean success, String value, long validityTime, long elapsedTime) {
            this.success = success;
            this.value = value;
            this.validityTime = validityTime;
            this.elapsedTime = elapsedTime;
        }
        
        public boolean isSuccess() { return success; }
        public String getValue() { return value; }
        public long getValidityTime() { return validityTime; }
        public long getElapsedTime() { return elapsedTime; }
    }
}
```

### Redlock使用示例

```java
public class RedlockExample {
    public static void main(String[] args) {
        // 创建多个Redis实例连接
        List<Jedis> redisInstances = new ArrayList<>();
        redisInstances.add(new Jedis("redis1.example.com", 6379));
        redisInstances.add(new Jedis("redis2.example.com", 6379));
        redisInstances.add(new Jedis("redis3.example.com", 6379));
        redisInstances.add(new Jedis("redis4.example.com", 6379));
        redisInstances.add(new Jedis("redis5.example.com", 6379));
        
        Redlock redlock = new Redlock(redisInstances);
        
        String resource = "my-resource";
        int ttl = 10000; // 10秒
        
        Redlock.RedlockResult result = redlock.tryLock(resource, ttl);
        
        if (result.isSuccess()) {
            try {
                System.out.println("获取锁成功，有效时间：" + result.getValidityTime() + "ms");
                
                // 执行业务逻辑
                doBusinessLogic();
                
            } finally {
                redlock.unlock(resource, result.getValue());
                System.out.println("释放锁");
            }
        } else {
            System.out.println("获取锁失败");
        }
        
        // 关闭连接
        for (Jedis jedis : redisInstances) {
            jedis.close();
        }
    }
    
    private static void doBusinessLogic() {
        // 模拟业务处理
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

## 性能对比与选择

### 性能测试

```java
public class DistributedLockBenchmark {
    private static final int THREAD_COUNT = 100;
    private static final int OPERATIONS_PER_THREAD = 100;
    
    public static void main(String[] args) {
        // 测试Redis锁
        benchmarkRedisLock();
        
        // 测试ZooKeeper锁
        benchmarkZooKeeperLock();
        
        // 测试数据库锁
        benchmarkDatabaseLock();
    }
    
    private static void benchmarkRedisLock() {
        System.out.println("\n=== Redis Lock Benchmark ===");
        
        Jedis jedis = new Jedis("localhost", 6379);
        
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        RedisDistributedLock lock = new RedisDistributedLock(
                            jedis, "test-lock-" + (j % 10), 5);
                        
                        if (lock.tryLock(1000)) {
                            try {
                                // 模拟业务处理
                                Thread.sleep(1);
                                successCount.incrementAndGet();
                            } finally {
                                lock.unlock();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("Total time: " + (endTime - startTime) + "ms");
        System.out.println("Success operations: " + successCount.get());
        System.out.println("TPS: " + (successCount.get() * 1000.0 / (endTime - startTime)));
        
        executor.shutdown();
        jedis.close();
    }
    
    private static void benchmarkZooKeeperLock() {
        System.out.println("\n=== ZooKeeper Lock Benchmark ===");
        
        try {
            ZooKeeper zk = new ZooKeeper("localhost:2181", 5000, null);
            
            long startTime = System.currentTimeMillis();
            
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
            AtomicInteger successCount = new AtomicInteger(0);
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                            ZooKeeperDistributedLock lock = new ZooKeeperDistributedLock(
                                zk, "/locks/test-lock-" + (j % 10));
                            
                            if (lock.tryLock(1, TimeUnit.SECONDS)) {
                                try {
                                    // 模拟业务处理
                                    Thread.sleep(1);
                                    successCount.incrementAndGet();
                                } finally {
                                    lock.unlock();
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            latch.await();
            
            long endTime = System.currentTimeMillis();
            
            System.out.println("Total time: " + (endTime - startTime) + "ms");
            System.out.println("Success operations: " + successCount.get());
            System.out.println("TPS: " + (successCount.get() * 1000.0 / (endTime - startTime)));
            
            executor.shutdown();
            zk.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void benchmarkDatabaseLock() {
        System.out.println("\n=== Database Lock Benchmark ===");
        
        String jdbcUrl = "jdbc:mysql://localhost:3306/test";
        
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        DatabaseDistributedLock lock = new DatabaseDistributedLock(
                            jdbcUrl, "test-lock-" + (j % 10), 5);
                        
                        if (lock.tryLock(1000)) {
                            try {
                                // 模拟业务处理
                                Thread.sleep(1);
                                successCount.incrementAndGet();
                            } finally {
                                lock.unlock();
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("Total time: " + (endTime - startTime) + "ms");
        System.out.println("Success operations: " + successCount.get());
        System.out.println("TPS: " + (successCount.get() * 1000.0 / (endTime - startTime)));
        
        executor.shutdown();
    }
}
```

### 选择指南

```java
public class DistributedLockSelector {
    
    public enum LockType {
        DATABASE, REDIS, ZOOKEEPER, ETCD, REDLOCK
    }
    
    public static class LockRequirement {
        private boolean highPerformance;
        private boolean strongConsistency;
        private boolean faultTolerance;
        private boolean simplicity;
        private int expectedTps;
        private int maxLatencyMs;
        
        // constructors, getters, setters
        public LockRequirement(boolean highPerformance, boolean strongConsistency,
                             boolean faultTolerance, boolean simplicity,
                             int expectedTps, int maxLatencyMs) {
            this.highPerformance = highPerformance;
            this.strongConsistency = strongConsistency;
            this.faultTolerance = faultTolerance;
            this.simplicity = simplicity;
            this.expectedTps = expectedTps;
            this.maxLatencyMs = maxLatencyMs;
        }
        
        // getters
        public boolean isHighPerformance() { return highPerformance; }
        public boolean isStrongConsistency() { return strongConsistency; }
        public boolean isFaultTolerance() { return faultTolerance; }
        public boolean isSimplicity() { return simplicity; }
        public int getExpectedTps() { return expectedTps; }
        public int getMaxLatencyMs() { return maxLatencyMs; }
    }
    
    public static LockType selectLockType(LockRequirement requirement) {
        // 强一致性要求
        if (requirement.isStrongConsistency()) {
            if (requirement.isHighPerformance() && requirement.getExpectedTps() > 1000) {
                return LockType.ETCD; // 现代化，性能较好
            } else {
                return LockType.ZOOKEEPER; // 成熟稳定
            }
        }
        
        // 高性能要求
        if (requirement.isHighPerformance()) {
            if (requirement.isFaultTolerance()) {
                return LockType.REDLOCK; // 高可用Redis集群
            } else {
                return LockType.REDIS; // 单Redis实例
            }
        }
        
        // 简单性要求
        if (requirement.isSimplicity()) {
            if (requirement.getExpectedTps() < 100) {
                return LockType.DATABASE; // 最简单
            } else {
                return LockType.REDIS; // 简单且性能好
            }
        }
        
        // 默认推荐
        return LockType.REDIS;
    }
    
    public static void printRecommendation(LockRequirement requirement) {
        LockType recommended = selectLockType(requirement);
        
        System.out.println("\n=== 分布式锁选择建议 ===");
        System.out.println("需求分析:");
        System.out.println("  高性能: " + requirement.isHighPerformance());
        System.out.println("  强一致性: " + requirement.isStrongConsistency());
        System.out.println("  容错性: " + requirement.isFaultTolerance());
        System.out.println("  简单性: " + requirement.isSimplicity());
        System.out.println("  期望TPS: " + requirement.getExpectedTps());
        System.out.println("  最大延迟: " + requirement.getMaxLatencyMs() + "ms");
        
        System.out.println("\n推荐方案: " + recommended);
        
        switch (recommended) {
            case DATABASE:
                System.out.println("优点: 实现简单，事务支持，强一致性");
                System.out.println("缺点: 性能较低，可能成为瓶颈");
                System.out.println("适用: 小规模系统，对一致性要求高");
                break;
            case REDIS:
                System.out.println("优点: 性能高，实现简单，延迟低");
                System.out.println("缺点: 可能丢失锁，主从切换问题");
                System.out.println("适用: 高性能要求，可容忍偶尔失效");
                break;
            case ZOOKEEPER:
                System.out.println("优点: 强一致性，可靠性高，成熟稳定");
                System.out.println("缺点: 性能相对较低，复杂度高");
                System.out.println("适用: 对一致性要求极高的场景");
                break;
            case ETCD:
                System.out.println("优点: 强一致性，现代化设计，云原生");
                System.out.println("缺点: 相对较新，生态不如ZooKeeper");
                System.out.println("适用: 云原生环境，Kubernetes相关");
                break;
            case REDLOCK:
                System.out.println("优点: 高可用，性能好，容错性强");
                System.out.println("缺点: 实现复杂，需要多个Redis实例");
                System.out.println("适用: 高可用高性能要求");
                break;
        }
    }
}
```

## 最佳实践

### 1. 锁的设计原则

```java
public class DistributedLockBestPractices {
    
    // 1. 锁的粒度要合适
    public void lockGranularityExample() {
        // 错误：锁粒度太粗
        // distributedLock.lock("global-lock");
        
        // 正确：根据业务逻辑设计锁粒度
        String userId = "user123";
        String lockKey = "user-operation:" + userId;
        // distributedLock.lock(lockKey);
    }
    
    // 2. 设置合理的超时时间
    public void timeoutExample() {
        RedisDistributedLock lock = new RedisDistributedLock(null, "test", 30); // 30秒超时
        
        try {
            if (lock.tryLock(5000)) { // 5秒获取超时
                // 业务逻辑
                processBusinessLogic();
            } else {
                throw new RuntimeException("获取锁超时");
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 3. 实现锁的自动续期
    public void autoRenewalExample() {
        AutoRenewRedisLock lock = new AutoRenewRedisLock(null, "test", 30);
        
        try {
            if (lock.tryLock()) {
                // 长时间业务处理，锁会自动续期
                longRunningBusinessLogic();
            }
        } finally {
            lock.unlock();
        }
    }
    
    // 4. 处理锁的异常情况
    public void exceptionHandlingExample() {
        RedisDistributedLock lock = new RedisDistributedLock(null, "test", 30);
        boolean locked = false;
        
        try {
            locked = lock.tryLock(5000);
            if (!locked) {
                throw new RuntimeException("获取锁失败");
            }
            
            // 业务逻辑
            processBusinessLogic();
            
        } catch (Exception e) {
            // 记录日志，处理异常
            System.err.println("业务处理异常: " + e.getMessage());
            throw e;
        } finally {
            if (locked) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    System.err.println("释放锁异常: " + e.getMessage());
                }
            }
        }
    }
    
    // 5. 实现可重入锁
    public void reentrantLockExample() {
        ReentrantRedisLock lock = new ReentrantRedisLock(null, "test", 30);
        
        if (lock.tryLock()) {
            try {
                method1(lock);
            } finally {
                lock.unlock();
            }
        }
    }
    
    private void method1(ReentrantRedisLock lock) {
        if (lock.tryLock()) { // 可重入
            try {
                method2(lock);
            } finally {
                lock.unlock();
            }
        }
    }
    
    private void method2(ReentrantRedisLock lock) {
        if (lock.tryLock()) { // 可重入
            try {
                // 业务逻辑
                System.out.println("执行业务逻辑，当前锁计数: " + lock.getLockCount());
            } finally {
                lock.unlock();
            }
        }
    }
    
    private void processBusinessLogic() {
        // 模拟业务处理
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void longRunningBusinessLogic() {
        // 模拟长时间业务处理
        try {
            Thread.sleep(60000); // 1分钟
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 2. 监控和告警

```java
public class DistributedLockMonitor {
    private final Map<String, LockMetrics> lockMetrics = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public DistributedLockMonitor() {
        // 定期输出监控报告
        scheduler.scheduleAtFixedRate(this::printMetrics, 60, 60, TimeUnit.SECONDS);
    }
    
    public void recordLockAcquisition(String lockName, boolean success, long elapsedTime) {
        LockMetrics metrics = lockMetrics.computeIfAbsent(lockName, k -> new LockMetrics());
        metrics.recordAcquisition(success, elapsedTime);
    }
    
    public void recordLockHold(String lockName, long holdTime) {
        LockMetrics metrics = lockMetrics.computeIfAbsent(lockName, k -> new LockMetrics());
        metrics.recordHold(holdTime);
    }
    
    public void recordLockRelease(String lockName) {
        LockMetrics metrics = lockMetrics.get(lockName);
        if (metrics != null) {
            metrics.recordRelease();
        }
    }
    
    private void printMetrics() {
        System.out.println("\n=== 分布式锁监控报告 ===");
        for (Map.Entry<String, LockMetrics> entry : lockMetrics.entrySet()) {
            String lockName = entry.getKey();
            LockMetrics metrics = entry.getValue();
            
            System.out.println("锁名称: " + lockName);
            System.out.println("  获取成功率: " + String.format("%.2f%%", metrics.getSuccessRate()));
            System.out.println("  平均获取时间: " + metrics.getAverageAcquisitionTime() + "ms");
            System.out.println("  平均持有时间: " + metrics.getAverageHoldTime() + "ms");
            System.out.println("  当前持有数: " + metrics.getCurrentHolders());
            System.out.println("  总获取次数: " + metrics.getTotalAttempts());
            System.out.println();
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
    
    private static class LockMetrics {
        private final AtomicLong totalAttempts = new AtomicLong(0);
        private final AtomicLong successfulAttempts = new AtomicLong(0);
        private final AtomicLong totalAcquisitionTime = new AtomicLong(0);
        private final AtomicLong totalHoldTime = new AtomicLong(0);
        private final AtomicLong currentHolders = new AtomicLong(0);
        private final AtomicLong totalHolds = new AtomicLong(0);
        
        public void recordAcquisition(boolean success, long elapsedTime) {
            totalAttempts.incrementAndGet();
            totalAcquisitionTime.addAndGet(elapsedTime);
            if (success) {
                successfulAttempts.incrementAndGet();
                currentHolders.incrementAndGet();
            }
        }
        
        public void recordHold(long holdTime) {
            totalHoldTime.addAndGet(holdTime);
            totalHolds.incrementAndGet();
        }
        
        public void recordRelease() {
            currentHolders.decrementAndGet();
        }
        
        public double getSuccessRate() {
            long total = totalAttempts.get();
            return total > 0 ? (successfulAttempts.get() * 100.0 / total) : 0.0;
        }
        
        public long getAverageAcquisitionTime() {
            long total = totalAttempts.get();
            return total > 0 ? totalAcquisitionTime.get() / total : 0;
        }
        
        public long getAverageHoldTime() {
            long total = totalHolds.get();
            return total > 0 ? totalHoldTime.get() / total : 0;
        }
        
        public long getCurrentHolders() {
            return currentHolders.get();
        }
        
        public long getTotalAttempts() {
            return totalAttempts.get();
        }
    }
}
```

### 3. 性能优化

```java
public class DistributedLockOptimization {
    
    // 1. 连接池优化
    public static class OptimizedRedisLock {
        private final JedisPool jedisPool;
        private final String lockKey;
        private final String lockValue;
        private final int expireTime;
        
        public OptimizedRedisLock(JedisPool jedisPool, String lockKey, int expireTime) {
            this.jedisPool = jedisPool;
            this.lockKey = lockKey;
            this.lockValue = UUID.randomUUID().toString();
            this.expireTime = expireTime;
        }
        
        public boolean tryLock() {
            try (Jedis jedis = jedisPool.getResource()) {
                SetParams params = SetParams.setParams().nx().ex(expireTime);
                String result = jedis.set(lockKey, lockValue, params);
                return "OK".equals(result);
            }
        }
        
        public void unlock() {
            try (Jedis jedis = jedisPool.getResource()) {
                String script = 
                    "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "    return redis.call('del', KEYS[1]) " +
                    "else " +
                    "    return 0 " +
                    "end";
                jedis.eval(script, 1, lockKey, lockValue);
            }
        }
    }
    
    // 2. 批量操作优化
    public static class BatchLockManager {
        private final JedisPool jedisPool;
        
        public BatchLockManager(JedisPool jedisPool) {
            this.jedisPool = jedisPool;
        }
        
        public Map<String, Boolean> tryLockBatch(List<String> lockKeys, int expireTime) {
            Map<String, Boolean> results = new HashMap<>();
            
            try (Jedis jedis = jedisPool.getResource()) {
                Pipeline pipeline = jedis.pipelined();
                Map<String, Response<String>> responses = new HashMap<>();
                
                for (String lockKey : lockKeys) {
                    String lockValue = UUID.randomUUID().toString();
                    SetParams params = SetParams.setParams().nx().ex(expireTime);
                    Response<String> response = pipeline.set(lockKey, lockValue, params);
                    responses.put(lockKey, response);
                }
                
                pipeline.sync();
                
                for (Map.Entry<String, Response<String>> entry : responses.entrySet()) {
                    String lockKey = entry.getKey();
                    Response<String> response = entry.getValue();
                    results.put(lockKey, "OK".equals(response.get()));
                }
            }
            
            return results;
        }
    }
    
    // 3. 本地缓存优化
    public static class CachedDistributedLock {
        private final RedisDistributedLock redisLock;
        private final Map<String, Long> localCache = new ConcurrentHashMap<>();
        private final long cacheTimeout = 1000; // 1秒本地缓存
        
        public CachedDistributedLock(RedisDistributedLock redisLock) {
            this.redisLock = redisLock;
        }
        
        public boolean tryLock() {
            // 检查本地缓存
            String lockKey = redisLock.lockKey;
            Long cachedTime = localCache.get(lockKey);
            if (cachedTime != null && System.currentTimeMillis() - cachedTime < cacheTimeout) {
                return false; // 本地缓存显示锁被占用
            }
            
            boolean acquired = redisLock.tryLock();
            if (!acquired) {
                // 缓存失败信息
                localCache.put(lockKey, System.currentTimeMillis());
            }
            
            return acquired;
        }
        
        public void unlock() {
            redisLock.unlock();
            // 清除本地缓存
            localCache.remove(redisLock.lockKey);
        }
    }
}
```

## 面试要点

### 高频问题

**Q1: 分布式锁有哪些实现方案？各有什么优缺点？**

A: 主要有四种实现方案：

1. **数据库锁**
   - 优点：实现简单，事务支持，强一致性
   - 缺点：性能较低，可能成为瓶颈，单点故障

2. **Redis锁**
   - 优点：性能高，实现简单，延迟低
   - 缺点：可能丢失锁，主从切换时有问题

3. **ZooKeeper锁**
   - 优点：强一致性，可靠性高，支持阻塞等待
   - 缺点：性能相对较低，复杂度高

4. **Etcd锁**
   - 优点：强一致性，现代化设计，云原生友好
   - 缺点：相对较新，生态不如ZooKeeper成熟

**Q2: Redis分布式锁如何防止死锁？**

A: 主要通过以下方式：
1. **设置过期时间**：使用`SET key value EX seconds NX`命令
2. **锁续期机制**：定期延长锁的过期时间
3. **唯一标识**：使用UUID确保只有锁的持有者才能释放
4. **Lua脚本**：保证释放锁操作的原子性

**Q3: 什么是Redlock算法？**

A: Redlock是Redis作者提出的分布式锁算法，用于在多个Redis实例上实现分布式锁：
- 在多个独立的Redis实例上尝试获取锁
- 只有在大多数实例上成功获取锁才算成功
- 考虑时钟漂移，计算锁的有效时间
- 提供更高的可用性和安全性

### 深入问题

**Q4: ZooKeeper分布式锁的实现原理？**

A: ZooKeeper分布式锁基于临时顺序节点：
1. 客户端在指定路径下创建临时顺序节点
2. 获取所有子节点并排序
3. 如果自己是最小节点，则获得锁
4. 否则监听前一个节点的删除事件
5. 当前一个节点删除时，重新尝试获取锁

**Q5: 如何解决Redis主从切换时的锁丢失问题？**

A: 几种解决方案：
1. **使用Redlock算法**：在多个独立Redis实例上获取锁
2. **延迟启动**：主从切换后延迟一段时间再提供服务
3. **使用Redis Cluster**：提供更好的高可用性
4. **结合ZooKeeper**：使用ZooKeeper作为协调服务

**Q6: 分布式锁的性能如何优化？**

A: 优化策略：
1. **连接池**：使用连接池减少连接开销
2. **批量操作**：使用Pipeline批量处理
3. **本地缓存**：缓存锁状态减少远程调用
4. **锁粒度**：合理设计锁的粒度
5. **异步处理**：使用异步方式处理锁操作

### 实践经验

**Q7: 在生产环境中如何选择分布式锁方案？**

A: 选择依据：
- **性能要求高**：选择Redis
- **一致性要求高**：选择ZooKeeper或Etcd
- **简单场景**：选择数据库锁
- **高可用要求**：选择Redlock或ZooKeeper集群

**Q8: 分布式锁的监控指标有哪些？**

A: 关键监控指标：
- 锁获取成功率
- 锁获取平均耗时
- 锁持有时间分布
- 锁竞争激烈程度
- 锁超时次数
- 系统可用性

## 总结

分布式锁是分布式系统中重要的同步机制，不同的实现方案各有优缺点：

1. **数据库锁**适合简单场景，实现容易但性能有限
2. **Redis锁**性能优秀，适合高并发场景，但需要考虑一致性问题
3. **ZooKeeper锁**提供强一致性，适合对数据一致性要求极高的场景
4. **Etcd锁**现代化设计，适合云原生环境
5. **Redlock算法**在多Redis实例间提供高可用的分布式锁

在实际应用中，需要根据具体的业务需求、性能要求、一致性要求来选择合适的方案，并注意锁的设计原则、异常处理、监控告警等最佳实践。