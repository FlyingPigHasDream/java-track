# 一致性哈希算法详解

## 目录
- [一致性哈希概述](#一致性哈希概述)
- [算法原理](#算法原理)
- [核心特性](#核心特性)
- [Java实现](#java实现)
- [虚拟节点优化](#虚拟节点优化)
- [实际应用](#实际应用)
- [性能分析](#性能分析)
- [最佳实践](#最佳实践)
- [面试要点](#面试要点)

## 一致性哈希概述

### 什么是一致性哈希
一致性哈希（Consistent Hashing）是一种特殊的哈希算法，主要用于分布式系统中的数据分片和负载均衡。它能够在节点动态增减时，最小化数据的重新分布。

### 传统哈希的问题
```java
// 传统哈希方法
public class TraditionalHashing {
    private List<String> servers;
    
    public String getServer(String key) {
        int hash = key.hashCode();
        int serverIndex = Math.abs(hash) % servers.size();
        return servers.get(serverIndex);
    }
}
```

**问题：**
- 节点增减时，大量数据需要重新分布
- 缓存失效率高
- 系统扩展性差

### 一致性哈希的优势
- **最小化重新分布**：只影响相邻节点
- **良好的扩展性**：支持动态增减节点
- **负载均衡**：通过虚拟节点实现
- **容错性**：单点故障影响最小

## 算法原理

### 哈希环（Hash Ring）

```java
public class HashRing {
    // 使用TreeMap维护有序的哈希环
    private final TreeMap<Long, String> ring = new TreeMap<>();
    private final MessageDigest md5;
    
    public HashRing() {
        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
    
    // 计算哈希值
    private long hash(String key) {
        md5.reset();
        md5.update(key.getBytes());
        byte[] digest = md5.digest();
        
        long hash = 0;
        for (int i = 0; i < 4; i++) {
            hash <<= 8;
            hash |= ((int) digest[i]) & 0xFF;
        }
        return hash;
    }
    
    // 添加节点
    public void addNode(String node) {
        long nodeHash = hash(node);
        ring.put(nodeHash, node);
        System.out.println("Added node: " + node + " at position: " + nodeHash);
    }
    
    // 移除节点
    public void removeNode(String node) {
        long nodeHash = hash(node);
        ring.remove(nodeHash);
        System.out.println("Removed node: " + node + " from position: " + nodeHash);
    }
    
    // 获取负责处理key的节点
    public String getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }
        
        long keyHash = hash(key);
        
        // 查找第一个大于等于keyHash的节点
        Map.Entry<Long, String> entry = ring.ceilingEntry(keyHash);
        
        // 如果没找到，说明key应该分配给环上的第一个节点
        if (entry == null) {
            entry = ring.firstEntry();
        }
        
        return entry.getValue();
    }
    
    // 获取环的状态
    public void printRing() {
        System.out.println("Hash Ring Status:");
        for (Map.Entry<Long, String> entry : ring.entrySet()) {
            System.out.println("Position: " + entry.getKey() + " -> Node: " + entry.getValue());
        }
    }
}
```

### 基本使用示例

```java
public class ConsistentHashingDemo {
    public static void main(String[] args) {
        HashRing hashRing = new HashRing();
        
        // 添加服务器节点
        hashRing.addNode("server1");
        hashRing.addNode("server2");
        hashRing.addNode("server3");
        
        // 测试数据分布
        String[] keys = {"user1", "user2", "user3", "user4", "user5"};
        
        System.out.println("\nData distribution:");
        for (String key : keys) {
            String node = hashRing.getNode(key);
            System.out.println("Key: " + key + " -> Node: " + node);
        }
        
        // 添加新节点
        System.out.println("\nAdding new server...");
        hashRing.addNode("server4");
        
        System.out.println("\nData distribution after adding server4:");
        for (String key : keys) {
            String node = hashRing.getNode(key);
            System.out.println("Key: " + key + " -> Node: " + node);
        }
    }
}
```

## 核心特性

### 1. 单调性（Monotonicity）
当节点增加时，只有部分数据需要重新分布，不会影响其他数据的映射关系。

### 2. 平衡性（Balance）
在理想情况下，每个节点应该承担相等的负载。

### 3. 分散性（Spread）
相同的数据在不同的客户端视图中应该映射到相同的节点。

## 虚拟节点优化

### 问题分析
基本的一致性哈希可能导致数据分布不均匀，特别是在节点较少时。

### 虚拟节点实现

```java
public class VirtualNodeHashRing {
    private final TreeMap<Long, String> ring = new TreeMap<>();
    private final int virtualNodeCount;
    private final MessageDigest md5;
    
    public VirtualNodeHashRing(int virtualNodeCount) {
        this.virtualNodeCount = virtualNodeCount;
        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not found", e);
        }
    }
    
    private long hash(String key) {
        md5.reset();
        md5.update(key.getBytes());
        byte[] digest = md5.digest();
        
        long hash = 0;
        for (int i = 0; i < 4; i++) {
            hash <<= 8;
            hash |= ((int) digest[i]) & 0xFF;
        }
        return hash;
    }
    
    // 添加物理节点（创建多个虚拟节点）
    public void addNode(String physicalNode) {
        for (int i = 0; i < virtualNodeCount; i++) {
            String virtualNode = physicalNode + "#" + i;
            long virtualNodeHash = hash(virtualNode);
            ring.put(virtualNodeHash, physicalNode);
        }
        System.out.println("Added physical node: " + physicalNode + 
                         " with " + virtualNodeCount + " virtual nodes");
    }
    
    // 移除物理节点（移除所有虚拟节点）
    public void removeNode(String physicalNode) {
        for (int i = 0; i < virtualNodeCount; i++) {
            String virtualNode = physicalNode + "#" + i;
            long virtualNodeHash = hash(virtualNode);
            ring.remove(virtualNodeHash);
        }
        System.out.println("Removed physical node: " + physicalNode + 
                         " and its " + virtualNodeCount + " virtual nodes");
    }
    
    public String getNode(String key) {
        if (ring.isEmpty()) {
            return null;
        }
        
        long keyHash = hash(key);
        Map.Entry<Long, String> entry = ring.ceilingEntry(keyHash);
        
        if (entry == null) {
            entry = ring.firstEntry();
        }
        
        return entry.getValue();
    }
    
    // 分析数据分布
    public void analyzeDistribution(String[] keys) {
        Map<String, Integer> distribution = new HashMap<>();
        
        for (String key : keys) {
            String node = getNode(key);
            distribution.put(node, distribution.getOrDefault(node, 0) + 1);
        }
        
        System.out.println("\nData Distribution Analysis:");
        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            double percentage = (double) entry.getValue() / keys.length * 100;
            System.out.printf("Node: %s, Keys: %d (%.2f%%)%n", 
                            entry.getKey(), entry.getValue(), percentage);
        }
    }
    
    // 获取环的详细信息
    public void printRingInfo() {
        System.out.println("\nVirtual Node Ring Info:");
        System.out.println("Total virtual nodes: " + ring.size());
        
        Map<String, Integer> nodeCount = new HashMap<>();
        for (String physicalNode : ring.values()) {
            nodeCount.put(physicalNode, nodeCount.getOrDefault(physicalNode, 0) + 1);
        }
        
        for (Map.Entry<String, Integer> entry : nodeCount.entrySet()) {
            System.out.println("Physical node: " + entry.getKey() + 
                             ", Virtual nodes: " + entry.getValue());
        }
    }
}
```

### 虚拟节点测试

```java
public class VirtualNodeDemo {
    public static void main(String[] args) {
        // 创建带虚拟节点的哈希环
        VirtualNodeHashRing hashRing = new VirtualNodeHashRing(150);
        
        // 添加物理节点
        hashRing.addNode("server1");
        hashRing.addNode("server2");
        hashRing.addNode("server3");
        
        // 生成测试数据
        String[] keys = new String[1000];
        for (int i = 0; i < keys.length; i++) {
            keys[i] = "key" + i;
        }
        
        // 分析初始分布
        System.out.println("Initial distribution:");
        hashRing.analyzeDistribution(keys);
        
        // 添加新节点
        System.out.println("\nAdding server4...");
        hashRing.addNode("server4");
        
        // 分析新分布
        System.out.println("\nDistribution after adding server4:");
        hashRing.analyzeDistribution(keys);
        
        // 移除节点
        System.out.println("\nRemoving server2...");
        hashRing.removeNode("server2");
        
        // 分析最终分布
        System.out.println("\nDistribution after removing server2:");
        hashRing.analyzeDistribution(keys);
        
        hashRing.printRingInfo();
    }
}
```

## 实际应用

### 1. 分布式缓存

```java
public class DistributedCache {
    private final VirtualNodeHashRing hashRing;
    private final Map<String, CacheClient> cacheClients;
    
    public DistributedCache(int virtualNodeCount) {
        this.hashRing = new VirtualNodeHashRing(virtualNodeCount);
        this.cacheClients = new ConcurrentHashMap<>();
    }
    
    // 添加缓存服务器
    public void addCacheServer(String serverName, String host, int port) {
        CacheClient client = new CacheClient(host, port);
        cacheClients.put(serverName, client);
        hashRing.addNode(serverName);
    }
    
    // 移除缓存服务器
    public void removeCacheServer(String serverName) {
        cacheClients.remove(serverName);
        hashRing.removeNode(serverName);
    }
    
    // 存储数据
    public void put(String key, Object value) {
        String serverName = hashRing.getNode(key);
        CacheClient client = cacheClients.get(serverName);
        if (client != null) {
            client.put(key, value);
        }
    }
    
    // 获取数据
    public Object get(String key) {
        String serverName = hashRing.getNode(key);
        CacheClient client = cacheClients.get(serverName);
        if (client != null) {
            return client.get(key);
        }
        return null;
    }
    
    // 删除数据
    public void delete(String key) {
        String serverName = hashRing.getNode(key);
        CacheClient client = cacheClients.get(serverName);
        if (client != null) {
            client.delete(key);
        }
    }
    
    // 缓存客户端模拟
    private static class CacheClient {
        private final String host;
        private final int port;
        private final Map<String, Object> localCache = new ConcurrentHashMap<>();
        
        public CacheClient(String host, int port) {
            this.host = host;
            this.port = port;
        }
        
        public void put(String key, Object value) {
            // 实际实现中这里会连接到真实的缓存服务器
            localCache.put(key, value);
            System.out.println("Stored " + key + " on " + host + ":" + port);
        }
        
        public Object get(String key) {
            Object value = localCache.get(key);
            System.out.println("Retrieved " + key + " from " + host + ":" + port);
            return value;
        }
        
        public void delete(String key) {
            localCache.remove(key);
            System.out.println("Deleted " + key + " from " + host + ":" + port);
        }
    }
}
```

### 2. 分布式数据库分片

```java
public class DatabaseSharding {
    private final VirtualNodeHashRing hashRing;
    private final Map<String, DatabaseShard> shards;
    
    public DatabaseSharding(int virtualNodeCount) {
        this.hashRing = new VirtualNodeHashRing(virtualNodeCount);
        this.shards = new ConcurrentHashMap<>();
    }
    
    // 添加数据库分片
    public void addShard(String shardName, String connectionUrl) {
        DatabaseShard shard = new DatabaseShard(shardName, connectionUrl);
        shards.put(shardName, shard);
        hashRing.addNode(shardName);
    }
    
    // 移除数据库分片
    public void removeShard(String shardName) {
        DatabaseShard shard = shards.remove(shardName);
        if (shard != null) {
            shard.close();
        }
        hashRing.removeNode(shardName);
    }
    
    // 插入数据
    public void insert(String userId, UserData userData) {
        String shardName = hashRing.getNode(userId);
        DatabaseShard shard = shards.get(shardName);
        if (shard != null) {
            shard.insert(userId, userData);
        }
    }
    
    // 查询数据
    public UserData query(String userId) {
        String shardName = hashRing.getNode(userId);
        DatabaseShard shard = shards.get(shardName);
        if (shard != null) {
            return shard.query(userId);
        }
        return null;
    }
    
    // 更新数据
    public void update(String userId, UserData userData) {
        String shardName = hashRing.getNode(userId);
        DatabaseShard shard = shards.get(shardName);
        if (shard != null) {
            shard.update(userId, userData);
        }
    }
    
    // 删除数据
    public void delete(String userId) {
        String shardName = hashRing.getNode(userId);
        DatabaseShard shard = shards.get(shardName);
        if (shard != null) {
            shard.delete(userId);
        }
    }
    
    // 数据库分片模拟
    private static class DatabaseShard {
        private final String shardName;
        private final String connectionUrl;
        private final Map<String, UserData> data = new ConcurrentHashMap<>();
        
        public DatabaseShard(String shardName, String connectionUrl) {
            this.shardName = shardName;
            this.connectionUrl = connectionUrl;
        }
        
        public void insert(String userId, UserData userData) {
            data.put(userId, userData);
            System.out.println("Inserted user " + userId + " into shard " + shardName);
        }
        
        public UserData query(String userId) {
            UserData userData = data.get(userId);
            System.out.println("Queried user " + userId + " from shard " + shardName);
            return userData;
        }
        
        public void update(String userId, UserData userData) {
            data.put(userId, userData);
            System.out.println("Updated user " + userId + " in shard " + shardName);
        }
        
        public void delete(String userId) {
            data.remove(userId);
            System.out.println("Deleted user " + userId + " from shard " + shardName);
        }
        
        public void close() {
            System.out.println("Closed shard " + shardName);
        }
    }
    
    // 用户数据模型
    public static class UserData {
        private String name;
        private String email;
        private int age;
        
        public UserData(String name, String email, int age) {
            this.name = name;
            this.email = email;
            this.age = age;
        }
        
        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        @Override
        public String toString() {
            return String.format("UserData{name='%s', email='%s', age=%d}", name, email, age);
        }
    }
}
```

### 3. 负载均衡器

```java
public class ConsistentHashLoadBalancer {
    private final VirtualNodeHashRing hashRing;
    private final Map<String, ServerInfo> servers;
    private final AtomicLong requestCounter = new AtomicLong(0);
    
    public ConsistentHashLoadBalancer(int virtualNodeCount) {
        this.hashRing = new VirtualNodeHashRing(virtualNodeCount);
        this.servers = new ConcurrentHashMap<>();
    }
    
    // 添加服务器
    public void addServer(String serverId, String host, int port, int weight) {
        ServerInfo serverInfo = new ServerInfo(serverId, host, port, weight);
        servers.put(serverId, serverInfo);
        
        // 根据权重添加虚拟节点
        for (int i = 0; i < weight; i++) {
            hashRing.addNode(serverId + "_weight_" + i);
        }
    }
    
    // 移除服务器
    public void removeServer(String serverId) {
        ServerInfo serverInfo = servers.remove(serverId);
        if (serverInfo != null) {
            // 移除所有权重相关的虚拟节点
            for (int i = 0; i < serverInfo.getWeight(); i++) {
                hashRing.removeNode(serverId + "_weight_" + i);
            }
        }
    }
    
    // 选择服务器处理请求
    public ServerInfo selectServer(String sessionId) {
        String selectedNode = hashRing.getNode(sessionId);
        if (selectedNode != null) {
            // 从虚拟节点名称中提取真实服务器ID
            String serverId = selectedNode.split("_weight_")[0];
            ServerInfo server = servers.get(serverId);
            if (server != null) {
                server.incrementRequestCount();
                return server;
            }
        }
        return null;
    }
    
    // 获取负载统计
    public void printLoadStatistics() {
        System.out.println("\nLoad Balancer Statistics:");
        long totalRequests = 0;
        
        for (ServerInfo server : servers.values()) {
            totalRequests += server.getRequestCount();
        }
        
        for (ServerInfo server : servers.values()) {
            double percentage = totalRequests > 0 ? 
                (double) server.getRequestCount() / totalRequests * 100 : 0;
            System.out.printf("Server: %s, Requests: %d (%.2f%%), Weight: %d%n",
                            server.getServerId(), server.getRequestCount(), 
                            percentage, server.getWeight());
        }
    }
    
    // 服务器信息
    public static class ServerInfo {
        private final String serverId;
        private final String host;
        private final int port;
        private final int weight;
        private final AtomicLong requestCount = new AtomicLong(0);
        
        public ServerInfo(String serverId, String host, int port, int weight) {
            this.serverId = serverId;
            this.host = host;
            this.port = port;
            this.weight = weight;
        }
        
        public void incrementRequestCount() {
            requestCount.incrementAndGet();
        }
        
        // getters
        public String getServerId() { return serverId; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public int getWeight() { return weight; }
        public long getRequestCount() { return requestCount.get(); }
        
        @Override
        public String toString() {
            return String.format("Server{id='%s', host='%s', port=%d, weight=%d}",
                               serverId, host, port, weight);
        }
    }
}
```

## 性能分析

### 时间复杂度
- **查找节点**：O(log N)，其中N是虚拟节点数量
- **添加节点**：O(V log N)，其中V是虚拟节点数量
- **删除节点**：O(V log N)

### 空间复杂度
- **存储开销**：O(N × V)，其中N是物理节点数，V是每个节点的虚拟节点数

### 性能测试

```java
public class PerformanceTest {
    public static void main(String[] args) {
        // 测试不同虚拟节点数量的性能
        int[] virtualNodeCounts = {50, 100, 150, 200, 300};
        int nodeCount = 10;
        int keyCount = 10000;
        
        for (int virtualNodes : virtualNodeCounts) {
            testPerformance(virtualNodes, nodeCount, keyCount);
        }
    }
    
    private static void testPerformance(int virtualNodes, int nodeCount, int keyCount) {
        VirtualNodeHashRing hashRing = new VirtualNodeHashRing(virtualNodes);
        
        // 添加节点
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < nodeCount; i++) {
            hashRing.addNode("server" + i);
        }
        long addNodeTime = System.currentTimeMillis() - startTime;
        
        // 生成测试键
        String[] keys = new String[keyCount];
        for (int i = 0; i < keyCount; i++) {
            keys[i] = "key" + i;
        }
        
        // 测试查找性能
        startTime = System.currentTimeMillis();
        for (String key : keys) {
            hashRing.getNode(key);
        }
        long lookupTime = System.currentTimeMillis() - startTime;
        
        // 测试数据分布均匀性
        Map<String, Integer> distribution = new HashMap<>();
        for (String key : keys) {
            String node = hashRing.getNode(key);
            distribution.put(node, distribution.getOrDefault(node, 0) + 1);
        }
        
        // 计算标准差
        double mean = (double) keyCount / nodeCount;
        double variance = 0;
        for (int count : distribution.values()) {
            variance += Math.pow(count - mean, 2);
        }
        double stdDev = Math.sqrt(variance / nodeCount);
        
        System.out.printf("Virtual Nodes: %d, Add Time: %dms, Lookup Time: %dms, Std Dev: %.2f%n",
                         virtualNodes, addNodeTime, lookupTime, stdDev);
    }
}
```

## 最佳实践

### 1. 虚拟节点数量选择

```java
public class VirtualNodeOptimizer {
    // 根据节点数量和负载均衡要求计算最优虚拟节点数
    public static int calculateOptimalVirtualNodes(int physicalNodes, 
                                                  double balanceThreshold) {
        // 经验公式：虚拟节点数 = 物理节点数 × 150
        int baseVirtualNodes = physicalNodes * 150;
        
        // 根据平衡阈值调整
        if (balanceThreshold < 0.1) {
            return baseVirtualNodes * 2; // 更高的均匀性要求
        } else if (balanceThreshold < 0.2) {
            return baseVirtualNodes;
        } else {
            return baseVirtualNodes / 2; // 较低的均匀性要求
        }
    }
    
    // 测试不同虚拟节点数量的负载均衡效果
    public static void testLoadBalance(int physicalNodes, int[] virtualNodeCounts) {
        int testKeys = 10000;
        String[] keys = new String[testKeys];
        for (int i = 0; i < testKeys; i++) {
            keys[i] = "key" + i;
        }
        
        System.out.println("Load Balance Test Results:");
        System.out.println("Physical Nodes: " + physicalNodes);
        System.out.println("Test Keys: " + testKeys);
        System.out.println();
        
        for (int virtualNodes : virtualNodeCounts) {
            VirtualNodeHashRing hashRing = new VirtualNodeHashRing(virtualNodes);
            
            // 添加物理节点
            for (int i = 0; i < physicalNodes; i++) {
                hashRing.addNode("server" + i);
            }
            
            // 统计分布
            Map<String, Integer> distribution = new HashMap<>();
            for (String key : keys) {
                String node = hashRing.getNode(key);
                distribution.put(node, distribution.getOrDefault(node, 0) + 1);
            }
            
            // 计算统计指标
            double mean = (double) testKeys / physicalNodes;
            double variance = 0;
            int min = Integer.MAX_VALUE;
            int max = Integer.MIN_VALUE;
            
            for (int count : distribution.values()) {
                variance += Math.pow(count - mean, 2);
                min = Math.min(min, count);
                max = Math.max(max, count);
            }
            
            double stdDev = Math.sqrt(variance / physicalNodes);
            double coefficient = stdDev / mean; // 变异系数
            
            System.out.printf("Virtual Nodes: %d%n", virtualNodes);
            System.out.printf("  Standard Deviation: %.2f%n", stdDev);
            System.out.printf("  Coefficient of Variation: %.4f%n", coefficient);
            System.out.printf("  Min Load: %d, Max Load: %d%n", min, max);
            System.out.printf("  Load Range: %.2f%%%n", (double)(max - min) / mean * 100);
            System.out.println();
        }
    }
}
```

### 2. 哈希函数选择

```java
public class HashFunctionComparison {
    // MD5哈希
    public static long md5Hash(String key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(key.getBytes());
            byte[] digest = md5.digest();
            
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash <<= 8;
                hash |= ((int) digest[i]) & 0xFF;
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    // SHA-1哈希
    public static long sha1Hash(String key) {
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            sha1.update(key.getBytes());
            byte[] digest = sha1.digest();
            
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash <<= 8;
                hash |= ((int) digest[i]) & 0xFF;
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
    
    // FNV哈希（更快的哈希函数）
    public static long fnvHash(String key) {
        final long FNV_64_INIT = 0xcbf29ce484222325L;
        final long FNV_64_PRIME = 0x100000001b3L;
        
        long hash = FNV_64_INIT;
        for (byte b : key.getBytes()) {
            hash ^= b;
            hash *= FNV_64_PRIME;
        }
        return hash;
    }
    
    // MurmurHash3（高性能哈希函数）
    public static long murmurHash3(String key) {
        byte[] data = key.getBytes();
        int length = data.length;
        int seed = 0;
        
        final long c1 = 0x87c37b91114253d5L;
        final long c2 = 0x4cf5ad432745937fL;
        
        long h1 = seed;
        int roundedEnd = (length & 0xFFFFFFF0); // round down to 16 byte block
        
        for (int i = 0; i < roundedEnd; i += 16) {
            long k1 = getLong(data, i);
            long k2 = getLong(data, i + 8);
            
            k1 *= c1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= c2;
            h1 ^= k1;
            
            h1 = Long.rotateLeft(h1, 27);
            h1 = h1 * 5 + 0x52dce729;
            
            k2 *= c2;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= c1;
            h1 ^= k2;
            
            h1 = Long.rotateLeft(h1, 31);
            h1 = h1 * 5 + 0x38495ab5;
        }
        
        // Handle remaining bytes
        long k1 = 0;
        switch (length & 15) {
            case 15: k1 ^= (long) (data[roundedEnd + 14] & 0xff) << 48;
            case 14: k1 ^= (long) (data[roundedEnd + 13] & 0xff) << 40;
            case 13: k1 ^= (long) (data[roundedEnd + 12] & 0xff) << 32;
            case 12: k1 ^= (long) (data[roundedEnd + 11] & 0xff) << 24;
            case 11: k1 ^= (long) (data[roundedEnd + 10] & 0xff) << 16;
            case 10: k1 ^= (long) (data[roundedEnd + 9] & 0xff) << 8;
            case 9:  k1 ^= (long) (data[roundedEnd + 8] & 0xff);
                k1 *= c1;
                k1 = Long.rotateLeft(k1, 31);
                k1 *= c2;
                h1 ^= k1;
            case 8:  k1 = getLong(data, roundedEnd);
                k1 *= c1;
                k1 = Long.rotateLeft(k1, 31);
                k1 *= c2;
                h1 ^= k1;
                break;
            case 7:  k1 ^= (long) (data[roundedEnd + 6] & 0xff) << 48;
            case 6:  k1 ^= (long) (data[roundedEnd + 5] & 0xff) << 40;
            case 5:  k1 ^= (long) (data[roundedEnd + 4] & 0xff) << 32;
            case 4:  k1 ^= (long) (data[roundedEnd + 3] & 0xff) << 24;
            case 3:  k1 ^= (long) (data[roundedEnd + 2] & 0xff) << 16;
            case 2:  k1 ^= (long) (data[roundedEnd + 1] & 0xff) << 8;
            case 1:  k1 ^= (long) (data[roundedEnd] & 0xff);
                k1 *= c1;
                k1 = Long.rotateLeft(k1, 31);
                k1 *= c2;
                h1 ^= k1;
        }
        
        h1 ^= length;
        h1 = fmix64(h1);
        
        return h1;
    }
    
    private static long getLong(byte[] data, int index) {
        return ((long) data[index] & 0xff) |
               (((long) data[index + 1] & 0xff) << 8) |
               (((long) data[index + 2] & 0xff) << 16) |
               (((long) data[index + 3] & 0xff) << 24) |
               (((long) data[index + 4] & 0xff) << 32) |
               (((long) data[index + 5] & 0xff) << 40) |
               (((long) data[index + 6] & 0xff) << 48) |
               (((long) data[index + 7] & 0xff) << 56);
    }
    
    private static long fmix64(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }
}
```

### 3. 监控和诊断

```java
public class ConsistentHashMonitor {
    private final VirtualNodeHashRing hashRing;
    private final Map<String, NodeMetrics> nodeMetrics;
    
    public ConsistentHashMonitor(VirtualNodeHashRing hashRing) {
        this.hashRing = hashRing;
        this.nodeMetrics = new ConcurrentHashMap<>();
    }
    
    // 记录请求
    public void recordRequest(String key, long responseTime) {
        String node = hashRing.getNode(key);
        if (node != null) {
            nodeMetrics.computeIfAbsent(node, k -> new NodeMetrics())
                      .recordRequest(responseTime);
        }
    }
    
    // 生成监控报告
    public void generateReport() {
        System.out.println("\n=== Consistent Hash Monitor Report ===");
        
        long totalRequests = 0;
        long totalResponseTime = 0;
        
        for (Map.Entry<String, NodeMetrics> entry : nodeMetrics.entrySet()) {
            NodeMetrics metrics = entry.getValue();
            totalRequests += metrics.getRequestCount();
            totalResponseTime += metrics.getTotalResponseTime();
        }
        
        System.out.println("Overall Statistics:");
        System.out.println("  Total Requests: " + totalRequests);
        System.out.println("  Average Response Time: " + 
                         (totalRequests > 0 ? totalResponseTime / totalRequests : 0) + "ms");
        System.out.println();
        
        System.out.println("Per-Node Statistics:");
        for (Map.Entry<String, NodeMetrics> entry : nodeMetrics.entrySet()) {
            String node = entry.getKey();
            NodeMetrics metrics = entry.getValue();
            
            double loadPercentage = totalRequests > 0 ? 
                (double) metrics.getRequestCount() / totalRequests * 100 : 0;
            
            System.out.printf("  Node: %s%n", node);
            System.out.printf("    Requests: %d (%.2f%%)%n", 
                            metrics.getRequestCount(), loadPercentage);
            System.out.printf("    Avg Response Time: %.2fms%n", 
                            metrics.getAverageResponseTime());
            System.out.printf("    Min/Max Response Time: %dms/%dms%n", 
                            metrics.getMinResponseTime(), metrics.getMaxResponseTime());
        }
    }
    
    // 检测热点
    public List<String> detectHotspots(double threshold) {
        List<String> hotspots = new ArrayList<>();
        
        long totalRequests = nodeMetrics.values().stream()
                                       .mapToLong(NodeMetrics::getRequestCount)
                                       .sum();
        
        double averageLoad = (double) totalRequests / nodeMetrics.size();
        double hotspotThreshold = averageLoad * (1 + threshold);
        
        for (Map.Entry<String, NodeMetrics> entry : nodeMetrics.entrySet()) {
            if (entry.getValue().getRequestCount() > hotspotThreshold) {
                hotspots.add(entry.getKey());
            }
        }
        
        return hotspots;
    }
    
    // 节点指标
    private static class NodeMetrics {
        private final AtomicLong requestCount = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private volatile long minResponseTime = Long.MAX_VALUE;
        private volatile long maxResponseTime = Long.MIN_VALUE;
        
        public void recordRequest(long responseTime) {
            requestCount.incrementAndGet();
            totalResponseTime.addAndGet(responseTime);
            
            // 更新最小值
            long currentMin = minResponseTime;
            while (responseTime < currentMin && 
                   !compareAndSetMin(currentMin, responseTime)) {
                currentMin = minResponseTime;
            }
            
            // 更新最大值
            long currentMax = maxResponseTime;
            while (responseTime > currentMax && 
                   !compareAndSetMax(currentMax, responseTime)) {
                currentMax = maxResponseTime;
            }
        }
        
        private boolean compareAndSetMin(long expect, long update) {
            // 简化的CAS操作
            if (minResponseTime == expect) {
                minResponseTime = update;
                return true;
            }
            return false;
        }
        
        private boolean compareAndSetMax(long expect, long update) {
            // 简化的CAS操作
            if (maxResponseTime == expect) {
                maxResponseTime = update;
                return true;
            }
            return false;
        }
        
        public long getRequestCount() {
            return requestCount.get();
        }
        
        public long getTotalResponseTime() {
            return totalResponseTime.get();
        }
        
        public double getAverageResponseTime() {
            long count = requestCount.get();
            return count > 0 ? (double) totalResponseTime.get() / count : 0;
        }
        
        public long getMinResponseTime() {
            return minResponseTime == Long.MAX_VALUE ? 0 : minResponseTime;
        }
        
        public long getMaxResponseTime() {
            return maxResponseTime == Long.MIN_VALUE ? 0 : maxResponseTime;
        }
    }
}
```

## 面试要点

### 高频问题

1. **一致性哈希解决了什么问题？**
   - 传统哈希在节点变化时需要重新分布大量数据
   - 一致性哈希只影响相邻节点，最小化数据迁移

2. **一致性哈希的基本原理？**
   - 将哈希值空间组织成环形
   - 节点和数据都映射到环上
   - 数据顺时针找到第一个节点

3. **虚拟节点的作用？**
   - 解决数据分布不均匀问题
   - 提高负载均衡效果
   - 减少节点变化的影响

4. **如何选择虚拟节点数量？**
   - 一般建议每个物理节点150-200个虚拟节点
   - 需要平衡内存开销和负载均衡效果

### 深入问题

1. **一致性哈希的数学证明？**
   - 证明在N个节点的系统中，添加一个节点只影响1/N的数据
   - 分析虚拟节点对负载均衡的改善效果

2. **哈希函数的选择标准？**
   - 均匀分布性
   - 计算效率
   - 雪崩效应

3. **一致性哈希的局限性？**
   - 不能完全保证负载均衡
   - 虚拟节点增加内存开销
   - 数据倾斜问题

### 实践经验

1. **在哪些系统中使用过一致性哈希？**
   - 分布式缓存（Redis Cluster、Memcached）
   - 分布式数据库分片
   - CDN负载均衡
   - 分布式存储系统

2. **实现中遇到的问题？**
   - 热点数据问题
   - 节点权重处理
   - 故障恢复策略

3. **性能优化经验？**
   - 选择高效的哈希函数
   - 合理设置虚拟节点数量
   - 实现增量rehash

## 总结

一致性哈希是分布式系统中的重要算法，它通过巧妙的环形结构设计，解决了传统哈希在动态环境中的问题。

**核心优势：**
- 最小化数据重新分布
- 良好的扩展性和容错性
- 支持加权负载均衡

**关键技术：**
- 哈希环的设计和实现
- 虚拟节点的优化
- 高效的哈希函数选择

**应用场景：**
- 分布式缓存系统
- 数据库分片
- 负载均衡
- 分布式存储

**实现要点：**
- 选择合适的数据结构（TreeMap）
- 合理设置虚拟节点数量
- 考虑节点权重和故障处理
- 实现监控和诊断功能