# 分布式缓存算法

## 概述

分布式缓存是分布式系统中用于提高数据访问性能的重要组件。通过将热点数据存储在内存中，并在多个节点间分布，可以显著减少数据库访问压力，提高系统响应速度。

### 核心概念

- **缓存分片**：将缓存数据分布到多个节点
- **缓存一致性**：保证多个缓存副本的数据一致性
- **缓存淘汰**：在缓存空间不足时移除数据的策略
- **缓存预热**：提前加载热点数据到缓存
- **缓存穿透**：查询不存在的数据导致缓存失效
- **缓存雪崩**：大量缓存同时失效导致系统压力激增
- **缓存击穿**：热点数据失效导致大量请求直接访问数据库

## 缓存分片算法

### 1. 一致性哈希算法

```java
public class ConsistentHashCache {
    
    // 虚拟节点数量
    private static final int VIRTUAL_NODES = 150;
    
    // 哈希环
    private final TreeMap<Long, String> hashRing = new TreeMap<>();
    
    // 物理节点集合
    private final Set<String> physicalNodes = new HashSet<>();
    
    // 缓存数据
    private final Map<String, Map<String, Object>> nodeData = new ConcurrentHashMap<>();
    
    /**
     * 添加缓存节点
     */
    public void addNode(String nodeId) {
        physicalNodes.add(nodeId);
        nodeData.put(nodeId, new ConcurrentHashMap<>());
        
        // 添加虚拟节点
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            String virtualNode = nodeId + "#" + i;
            long hash = hash(virtualNode);
            hashRing.put(hash, nodeId);
        }
        
        System.out.println("添加缓存节点: " + nodeId + ", 虚拟节点数: " + VIRTUAL_NODES);
    }
    
    /**
     * 移除缓存节点
     */
    public void removeNode(String nodeId) {
        if (!physicalNodes.contains(nodeId)) {
            return;
        }
        
        physicalNodes.remove(nodeId);
        
        // 移除虚拟节点
        for (int i = 0; i < VIRTUAL_NODES; i++) {
            String virtualNode = nodeId + "#" + i;
            long hash = hash(virtualNode);
            hashRing.remove(hash);
        }
        
        // 数据迁移到下一个节点
        Map<String, Object> data = nodeData.remove(nodeId);
        if (data != null && !data.isEmpty()) {
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                String targetNode = getNode(key);
                if (targetNode != null) {
                    nodeData.get(targetNode).put(key, value);
                }
            }
        }
        
        System.out.println("移除缓存节点: " + nodeId + ", 数据已迁移");
    }
    
    /**
     * 获取key对应的节点
     */
    public String getNode(String key) {
        if (hashRing.isEmpty()) {
            return null;
        }
        
        long hash = hash(key);
        
        // 查找第一个大于等于hash值的节点
        Map.Entry<Long, String> entry = hashRing.ceilingEntry(hash);
        if (entry == null) {
            // 如果没有找到，返回第一个节点（环形结构）
            entry = hashRing.firstEntry();
        }
        
        return entry.getValue();
    }
    
    /**
     * 存储数据
     */
    public void put(String key, Object value) {
        String nodeId = getNode(key);
        if (nodeId != null) {
            nodeData.get(nodeId).put(key, value);
            System.out.println("存储数据 [" + key + "] 到节点: " + nodeId);
        }
    }
    
    /**
     * 获取数据
     */
    public Object get(String key) {
        String nodeId = getNode(key);
        if (nodeId != null) {
            Object value = nodeData.get(nodeId).get(key);
            System.out.println("从节点 " + nodeId + " 获取数据 [" + key + "]: " + value);
            return value;
        }
        return null;
    }
    
    /**
     * 删除数据
     */
    public void remove(String key) {
        String nodeId = getNode(key);
        if (nodeId != null) {
            Object removed = nodeData.get(nodeId).remove(key);
            System.out.println("从节点 " + nodeId + " 删除数据 [" + key + "]: " + removed);
        }
    }
    
    /**
     * FNV-1a哈希算法
     */
    private long hash(String key) {
        final long FNV_64_INIT = 0xcbf29ce484222325L;
        final long FNV_64_PRIME = 0x100000001b3L;
        
        long hash = FNV_64_INIT;
        for (byte b : key.getBytes()) {
            hash ^= (b & 0xff);
            hash *= FNV_64_PRIME;
        }
        
        return hash;
    }
    
    /**
     * 获取节点分布统计
     */
    public void printDistribution() {
        System.out.println("\n=== 缓存节点分布统计 ===");
        for (String nodeId : physicalNodes) {
            Map<String, Object> data = nodeData.get(nodeId);
            System.out.println("节点 " + nodeId + ": " + data.size() + " 个key");
        }
    }
    
    /**
     * 获取所有节点
     */
    public Set<String> getNodes() {
        return new HashSet<>(physicalNodes);
    }
}
```

## 测试示例

### 1. 一致性哈希缓存测试

```java
public class ConsistentHashCacheTest {
    
    public static void main(String[] args) {
        System.out.println("=== 一致性哈希缓存测试 ===");
        
        ConsistentHashCache cache = new ConsistentHashCache();
        
        // 添加节点
        cache.addNode("node1");
        cache.addNode("node2");
        cache.addNode("node3");
        
        // 存储数据
        cache.put("user:1001", "Alice");
        cache.put("user:1002", "Bob");
        cache.put("user:1003", "Charlie");
        cache.put("user:1004", "David");
        cache.put("user:1005", "Eve");
        
        cache.printDistribution();
        
        // 读取数据
        System.out.println("\n=== 数据读取测试 ===");
        cache.get("user:1001");
        cache.get("user:1003");
        cache.get("user:1005");
        
        // 节点扩容测试
        System.out.println("\n=== 节点扩容测试 ===");
        cache.addNode("node4");
        cache.printDistribution();
        
        // 节点缩容测试
        System.out.println("\n=== 节点缩容测试 ===");
        cache.removeNode("node2");
        cache.printDistribution();
        
        // 验证数据完整性
        System.out.println("\n=== 数据完整性验证 ===");
        cache.get("user:1001");
        cache.get("user:1002");
        cache.get("user:1003");
        cache.get("user:1004");
        cache.get("user:1005");
    }
}
```

### 2. 范围分片缓存测试

```java
public class RangeShardingCacheTest {
    
    public static void main(String[] args) {
        System.out.println("=== 范围分片缓存测试 ===");
        
        RangeShardingCache cache = new RangeShardingCache();
        
        // 添加分片范围
        cache.addShardRange("shard1", "a", "h");
        cache.addShardRange("shard2", "h", "p");
        cache.addShardRange("shard3", "p", "z");
        
        // 存储数据
        cache.put("apple", "fruit1");
        cache.put("banana", "fruit2");
        cache.put("cherry", "fruit3");
        cache.put("mango", "fruit4");
        cache.put("orange", "fruit5");
        cache.put("peach", "fruit6");
        cache.put("strawberry", "fruit7");
        cache.put("watermelon", "fruit8");
        
        cache.printShardInfo();
        
        // 范围查询测试
        System.out.println("\n=== 范围查询测试 ===");
        cache.rangeQuery("b", "o");
        
        // 分片分裂测试
        System.out.println("\n=== 分片分裂测试 ===");
        cache.splitShard("shard2", "m", "shard4");
        cache.printShardInfo();
    }
}
```

### 3. 缓存一致性测试

```java
public class CacheConsistencyTest {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 写时复制缓存测试 ===");
        testCopyOnWriteCache();
        
        System.out.println("\n=== 最终一致性缓存测试 ===");
        testEventualConsistencyCache();
    }
    
    private static void testCopyOnWriteCache() throws InterruptedException {
        CopyOnWriteCache cache = new CopyOnWriteCache(2, 100); // 2个副本，100ms延迟
        
        // 写入数据
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        
        // 立即检查一致性（应该不一致）
        cache.checkConsistency();
        
        // 等待复制完成
        Thread.sleep(200);
        
        // 再次检查一致性（应该一致）
        cache.checkConsistency();
        
        // 读取测试
        cache.get("key1", 0); // 主节点
        cache.get("key1", 1); // 副本1
        cache.get("key1", 2); // 副本2
        
        // 强制同步测试
        cache.put("key3", "value3");
        cache.syncAllReplicas();
        cache.checkConsistency();
        
        cache.shutdown();
    }
    
    private static void testEventualConsistencyCache() throws InterruptedException {
        EventualConsistencyCache cache = new EventualConsistencyCache();
        
        // 添加节点
        cache.addNode("node1");
        cache.addNode("node2");
        cache.addNode("node3");
        
        // 建立连接
        cache.connectNodes("node1", "node2");
        cache.connectNodes("node2", "node3");
        cache.connectNodes("node1", "node3");
        
        // 写入数据
        cache.put("node1", "key1", "value1");
        cache.put("node2", "key2", "value2");
        
        // 立即检查一致性
        cache.checkConsistency();
        
        // 等待传播
        Thread.sleep(300);
        
        // 再次检查一致性
        cache.checkConsistency();
        
        // 启动反熵同步
        cache.startAntiEntropySync(1000);
        
        // 模拟网络分区后的数据写入
        cache.put("node1", "key3", "value3a");
        cache.put("node3", "key3", "value3b");
        
        Thread.sleep(2000);
        cache.checkConsistency();
        
        cache.shutdown();
    }
}
```

### 4. 缓存淘汰算法测试

```java
public class CacheEvictionTest {
    
    public static void main(String[] args) {
        System.out.println("=== LRU缓存测试 ===");
        testLRUCache();
        
        System.out.println("\n=== LFU缓存测试 ===");
        testLFUCache();
    }
    
    private static void testLRUCache() {
        LRUCache<String, String> cache = new LRUCache<>(3);
        
        // 添加数据
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.printCacheState();
        
        // 访问key1
        cache.get("key1");
        cache.printCacheState();
        
        // 添加新数据，应该淘汰key2
        cache.put("key4", "value4");
        cache.printCacheState();
        
        // 验证key2被淘汰
        cache.get("key2");
        
        System.out.println("\n最终统计:");
        LRUCache.CacheStats stats = cache.getStats();
        System.out.println("命中率: " + String.format("%.2f%%", stats.getHitRate() * 100));
    }
    
    private static void testLFUCache() {
        LFUCache<String, String> cache = new LFUCache<>(3);
        
        // 添加数据
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.put("key3", "value3");
        cache.printCacheState();
        
        // 多次访问key1
        cache.get("key1");
        cache.get("key1");
        cache.get("key2");
        cache.printCacheState();
        
        // 添加新数据，应该淘汰key3（频率最低）
        cache.put("key4", "value4");
        cache.printCacheState();
        
        // 验证key3被淘汰
        cache.get("key3");
        
        System.out.println("\n最终统计:");
        LFUCache.CacheStats stats = cache.getStats();
        System.out.println("命中率: " + String.format("%.2f%%", stats.getHitRate() * 100));
    }
}
```

## 性能对比

### 1. 分片算法性能对比

```java
public class ShardingPerformanceTest {
    
    public static void main(String[] args) {
        int dataSize = 10000;
        int nodeCount = 10;
        
        System.out.println("=== 分片算法性能对比 ===");
        System.out.println("数据量: " + dataSize + ", 节点数: " + nodeCount);
        
        testConsistentHashPerformance(dataSize, nodeCount);
        testRangeShardingPerformance(dataSize, nodeCount);
    }
    
    private static void testConsistentHashPerformance(int dataSize, int nodeCount) {
        ConsistentHashCache cache = new ConsistentHashCache();
        
        // 添加节点
        for (int i = 1; i <= nodeCount; i++) {
            cache.addNode("node" + i);
        }
        
        // 写入性能测试
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < dataSize; i++) {
            cache.put("key" + i, "value" + i);
        }
        long writeTime = System.currentTimeMillis() - startTime;
        
        // 读取性能测试
        startTime = System.currentTimeMillis();
        for (int i = 0; i < dataSize; i++) {
            cache.get("key" + i);
        }
        long readTime = System.currentTimeMillis() - startTime;
        
        System.out.println("\n一致性哈希性能:");
        System.out.println("写入时间: " + writeTime + "ms, 平均: " + (writeTime * 1000.0 / dataSize) + "μs/op");
        System.out.println("读取时间: " + readTime + "ms, 平均: " + (readTime * 1000.0 / dataSize) + "μs/op");
        
        // 负载均衡测试
        testLoadBalance(cache.getNodes(), dataSize);
    }
    
    private static void testRangeShardingPerformance(int dataSize, int nodeCount) {
        RangeShardingCache cache = new RangeShardingCache();
        
        // 添加分片范围（简单平均分配）
        char start = 'a';
        int rangeSize = 26 / nodeCount;
        for (int i = 0; i < nodeCount; i++) {
            char startChar = (char) (start + i * rangeSize);
            char endChar = (i == nodeCount - 1) ? 'z' : (char) (start + (i + 1) * rangeSize);
            cache.addShardRange("shard" + (i + 1), String.valueOf(startChar), String.valueOf(endChar));
        }
        
        // 写入性能测试（使用字母前缀）
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < dataSize; i++) {
            char prefix = (char) ('a' + (i % 26));
            cache.put(prefix + "key" + i, "value" + i);
        }
        long writeTime = System.currentTimeMillis() - startTime;
        
        // 读取性能测试
        startTime = System.currentTimeMillis();
        for (int i = 0; i < dataSize; i++) {
            char prefix = (char) ('a' + (i % 26));
            cache.get(prefix + "key" + i);
        }
        long readTime = System.currentTimeMillis() - startTime;
        
        System.out.println("\n范围分片性能:");
        System.out.println("写入时间: " + writeTime + "ms, 平均: " + (writeTime * 1000.0 / dataSize) + "μs/op");
        System.out.println("读取时间: " + readTime + "ms, 平均: " + (readTime * 1000.0 / dataSize) + "μs/op");
    }
    
    private static void testLoadBalance(Set<String> nodes, int dataSize) {
        Map<String, Integer> distribution = new HashMap<>();
        ConsistentHashCache cache = new ConsistentHashCache();
        
        for (String node : nodes) {
            cache.addNode(node);
            distribution.put(node, 0);
        }
        
        // 统计分布
        for (int i = 0; i < dataSize; i++) {
            String key = "key" + i;
            String node = cache.getNode(key);
            distribution.put(node, distribution.get(node) + 1);
        }
        
        System.out.println("\n负载均衡分析:");
        int avgLoad = dataSize / nodes.size();
        double variance = 0;
        
        for (Map.Entry<String, Integer> entry : distribution.entrySet()) {
            int load = entry.getValue();
            double deviation = ((double) load / avgLoad - 1) * 100;
            System.out.println(entry.getKey() + ": " + load + " (" + 
                             String.format("%.1f%%", deviation) + ")");
            variance += Math.pow(load - avgLoad, 2);
        }
        
        double stdDev = Math.sqrt(variance / nodes.size());
        System.out.println("标准差: " + String.format("%.2f", stdDev));
        System.out.println("负载均衡系数: " + String.format("%.3f", stdDev / avgLoad));
    }
}
```

### 2. 缓存淘汰算法性能对比

```java
public class EvictionPerformanceTest {
    
    public static void main(String[] args) {
        int cacheSize = 1000;
        int operationCount = 10000;
        
        System.out.println("=== 缓存淘汰算法性能对比 ===");
        System.out.println("缓存大小: " + cacheSize + ", 操作次数: " + operationCount);
        
        testLRUPerformance(cacheSize, operationCount);
        testLFUPerformance(cacheSize, operationCount);
    }
    
    private static void testLRUPerformance(int cacheSize, int operationCount) {
        LRUCache<Integer, String> cache = new LRUCache<>(cacheSize);
        Random random = new Random(42); // 固定种子确保可重复性
        
        long startTime = System.currentTimeMillis();
        
        // 混合读写操作
        for (int i = 0; i < operationCount; i++) {
            if (random.nextDouble() < 0.7) { // 70%读操作
                int key = random.nextInt(cacheSize * 2); // 可能访问不存在的key
                cache.get(key);
            } else { // 30%写操作
                int key = random.nextInt(cacheSize * 2);
                cache.put(key, "value" + key);
            }
        }
        
        long endTime = System.currentTimeMillis();
        LRUCache.CacheStats stats = cache.getStats();
        
        System.out.println("\nLRU性能结果:");
        System.out.println("总时间: " + (endTime - startTime) + "ms");
        System.out.println("平均操作时间: " + ((endTime - startTime) * 1000.0 / operationCount) + "μs/op");
        System.out.println("命中率: " + String.format("%.2f%%", stats.getHitRate() * 100));
        System.out.println("命中次数: " + stats.getHitCount());
        System.out.println("未命中次数: " + stats.getMissCount());
    }
    
    private static void testLFUPerformance(int cacheSize, int operationCount) {
        LFUCache<Integer, String> cache = new LFUCache<>(cacheSize);
        Random random = new Random(42); // 固定种子确保可重复性
        
        long startTime = System.currentTimeMillis();
        
        // 混合读写操作
        for (int i = 0; i < operationCount; i++) {
            if (random.nextDouble() < 0.7) { // 70%读操作
                int key = random.nextInt(cacheSize * 2);
                cache.get(key);
            } else { // 30%写操作
                int key = random.nextInt(cacheSize * 2);
                cache.put(key, "value" + key);
            }
        }
        
        long endTime = System.currentTimeMillis();
        LFUCache.CacheStats stats = cache.getStats();
        
        System.out.println("\nLFU性能结果:");
        System.out.println("总时间: " + (endTime - startTime) + "ms");
        System.out.println("平均操作时间: " + ((endTime - startTime) * 1000.0 / operationCount) + "μs/op");
        System.out.println("命中率: " + String.format("%.2f%%", stats.getHitRate() * 100));
        System.out.println("命中次数: " + stats.getHitCount());
        System.out.println("未命中次数: " + stats.getMissCount());
    }
}
```

## 算法选择指南

### 分片算法选择

| 算法 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| 一致性哈希 | 负载均衡好，扩缩容影响小 | 实现复杂，热点数据处理困难 | 大规模分布式系统，节点动态变化 |
| 范围分片 | 支持范围查询，实现简单 | 负载可能不均衡，热点问题 | 有序数据，需要范围查询 |
| 哈希分片 | 负载均衡，实现简单 | 扩缩容需要重新分片 | 小规模系统，节点相对稳定 |

### 一致性算法选择

| 算法 | 一致性级别 | 性能 | 复杂度 | 适用场景 |
|------|------------|------|--------|----------|
| 写时复制 | 最终一致性 | 高 | 中 | 读多写少，可容忍短暂不一致 |
| 最终一致性 | 最终一致性 | 高 | 中 | 分布式缓存，网络分区容忍 |
| 强一致性 | 强一致性 | 低 | 高 | 金融系统，数据准确性要求高 |

### 淘汰算法选择

| 算法 | 时间复杂度 | 空间复杂度 | 命中率 | 适用场景 |
|------|------------|------------|--------|----------|
| LRU | O(1) | O(n) | 中等 | 时间局部性强的访问模式 |
| LFU | O(log n) | O(n) | 高 | 频率局部性强的访问模式 |
| FIFO | O(1) | O(n) | 低 | 简单场景，性能要求不高 |
| Random | O(1) | O(n) | 低 | 访问模式随机，实现简单 |

## 最佳实践

### 1. 缓存预热策略

```java
public class CacheWarmupStrategy {
    
    private final LRUCache<String, Object> cache;
    private final DataSource dataSource;
    private final ScheduledExecutorService scheduler;
    
    public CacheWarmupStrategy(LRUCache<String, Object> cache, DataSource dataSource) {
        this.cache = cache;
        this.dataSource = dataSource;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    /**
     * 启动时预热热点数据
     */
    public void warmupHotData() {
        System.out.println("开始缓存预热...");
        
        // 获取热点数据列表
        List<String> hotKeys = dataSource.getHotKeys();
        
        // 并行加载热点数据
        hotKeys.parallelStream().forEach(key -> {
            try {
                Object value = dataSource.load(key);
                if (value != null) {
                    cache.put(key, value);
                    System.out.println("预热数据: " + key);
                }
            } catch (Exception e) {
                System.err.println("预热失败: " + key + ", 错误: " + e.getMessage());
            }
        });
        
        System.out.println("缓存预热完成，预热数据量: " + hotKeys.size());
    }
    
    /**
     * 定期刷新缓存
     */
    public void startPeriodicRefresh(long intervalMinutes) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                refreshExpiredData();
            } catch (Exception e) {
                System.err.println("定期刷新失败: " + e.getMessage());
            }
        }, intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
    }
    
    private void refreshExpiredData() {
        System.out.println("开始定期刷新缓存...");
        
        // 获取需要刷新的key（这里简化为随机选择）
        List<String> keysToRefresh = dataSource.getKeysToRefresh();
        
        for (String key : keysToRefresh) {
            try {
                Object newValue = dataSource.load(key);
                if (newValue != null) {
                    cache.put(key, newValue);
                    System.out.println("刷新缓存: " + key);
                }
            } catch (Exception e) {
                System.err.println("刷新失败: " + key + ", 错误: " + e.getMessage());
            }
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
    
    // 模拟数据源
    public static class DataSource {
        private final Random random = new Random();
        
        public List<String> getHotKeys() {
            return Arrays.asList("user:1001", "user:1002", "product:2001", "product:2002");
        }
        
        public List<String> getKeysToRefresh() {
            return Arrays.asList("user:1001", "product:2001");
        }
        
        public Object load(String key) {
            // 模拟数据库查询
            try {
                Thread.sleep(10 + random.nextInt(20)); // 10-30ms延迟
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "data_for_" + key;
        }
    }
}
```

### 2. 缓存监控和告警

```java
public class CacheMonitor {
    
    private final LRUCache<String, Object> cache;
    private final ScheduledExecutorService scheduler;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    
    // 告警阈值
    private final double hitRateThreshold = 0.8; // 命中率低于80%告警
    private final double errorRateThreshold = 0.05; // 错误率超过5%告警
    private final double memoryUsageThreshold = 0.9; // 内存使用率超过90%告警
    
    public CacheMonitor(LRUCache<String, Object> cache) {
        this.cache = cache;
        this.scheduler = Executors.newScheduledThreadPool(1);
    }
    
    /**
     * 启动监控
     */
    public void startMonitoring(long intervalSeconds) {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                collectMetrics();
                checkAlerts();
            } catch (Exception e) {
                System.err.println("监控异常: " + e.getMessage());
            }
        }, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
    }
    
    /**
     * 收集指标
     */
    private void collectMetrics() {
        LRUCache.CacheStats stats = cache.getStats();
        
        // 计算各种指标
        double hitRate = stats.getHitRate();
        double memoryUsage = (double) stats.getCurrentSize() / stats.getMaxSize();
        double errorRate = totalRequests.get() > 0 ? 
            (double) errorCount.get() / totalRequests.get() : 0;
        
        // 输出指标
        System.out.println("\n=== 缓存监控指标 ===");
        System.out.println("时间: " + new Date());
        System.out.println("命中率: " + String.format("%.2f%%", hitRate * 100));
        System.out.println("内存使用率: " + String.format("%.2f%%", memoryUsage * 100));
        System.out.println("错误率: " + String.format("%.2f%%", errorRate * 100));
        System.out.println("总请求数: " + totalRequests.get());
        System.out.println("错误数: " + errorCount.get());
        System.out.println("当前大小: " + stats.getCurrentSize() + "/" + stats.getMaxSize());
    }
    
    /**
     * 检查告警
     */
    private void checkAlerts() {
        LRUCache.CacheStats stats = cache.getStats();
        
        // 命中率告警
        if (stats.getHitRate() < hitRateThreshold) {
            sendAlert("缓存命中率过低", 
                     "当前命中率: " + String.format("%.2f%%", stats.getHitRate() * 100) + 
                     ", 阈值: " + String.format("%.2f%%", hitRateThreshold * 100));
        }
        
        // 内存使用率告警
        double memoryUsage = (double) stats.getCurrentSize() / stats.getMaxSize();
        if (memoryUsage > memoryUsageThreshold) {
            sendAlert("缓存内存使用率过高", 
                     "当前使用率: " + String.format("%.2f%%", memoryUsage * 100) + 
                     ", 阈值: " + String.format("%.2f%%", memoryUsageThreshold * 100));
        }
        
        // 错误率告警
        double errorRate = totalRequests.get() > 0 ? 
            (double) errorCount.get() / totalRequests.get() : 0;
        if (errorRate > errorRateThreshold) {
            sendAlert("缓存错误率过高", 
                     "当前错误率: " + String.format("%.2f%%", errorRate * 100) + 
                     ", 阈值: " + String.format("%.2f%%", errorRateThreshold * 100));
        }
    }
    
    /**
     * 发送告警
     */
    private void sendAlert(String title, String message) {
        System.err.println("\n🚨 告警: " + title);
        System.err.println("详情: " + message);
        System.err.println("时间: " + new Date());
        
        // 这里可以集成实际的告警系统，如邮件、短信、钉钉等
    }
    
    /**
     * 记录请求
     */
    public void recordRequest() {
        totalRequests.incrementAndGet();
    }
    
    /**
     * 记录错误
     */
    public void recordError() {
        errorCount.incrementAndGet();
    }
    
    /**
     * 重置计数器
     */
    public void resetCounters() {
        totalRequests.set(0);
        errorCount.set(0);
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
}
```

### 3. 缓存穿透和雪崩防护

```java
public class CacheProtection {
    
    private final LRUCache<String, Object> cache;
    private final BloomFilter bloomFilter;
    private final Map<String, CompletableFuture<Object>> loadingCache;
    private final Random random = new Random();
    
    public CacheProtection(LRUCache<String, Object> cache) {
        this.cache = cache;
        this.bloomFilter = new BloomFilter(10000, 0.01); // 10000个元素，1%误判率
        this.loadingCache = new ConcurrentHashMap<>();
    }
    
    /**
     * 防穿透的get方法
     */
    public Object getWithProtection(String key, Function<String, Object> loader) {
        // 1. 先从缓存获取
        Object value = cache.get(key);
        if (value != null) {
            return value;
        }
        
        // 2. 布隆过滤器检查
        if (!bloomFilter.mightContain(key)) {
            System.out.println("布隆过滤器拦截: " + key);
            return null;
        }
        
        // 3. 防击穿：同一个key只允许一个线程加载
        CompletableFuture<Object> future = loadingCache.computeIfAbsent(key, k -> 
            CompletableFuture.supplyAsync(() -> {
                try {
                    Object loadedValue = loader.apply(k);
                    if (loadedValue != null) {
                        // 添加随机过期时间防雪崩
                        int randomTtl = 300 + random.nextInt(60); // 5-6分钟
                        cache.put(k, loadedValue);
                        System.out.println("加载数据到缓存: " + k + ", TTL: " + randomTtl + "s");
                    } else {
                        // 缓存空值防穿透
                        cache.put(k, "NULL_VALUE");
                        System.out.println("缓存空值: " + k);
                    }
                    return loadedValue;
                } finally {
                    loadingCache.remove(k);
                }
            })
        );
        
        try {
            Object result = future.get(5, TimeUnit.SECONDS); // 5秒超时
            return "NULL_VALUE".equals(result) ? null : result;
        } catch (Exception e) {
            System.err.println("加载数据失败: " + key + ", 错误: " + e.getMessage());
            loadingCache.remove(key);
            return null;
        }
    }
    
    /**
     * 预热布隆过滤器
     */
    public void warmupBloomFilter(List<String> existingKeys) {
        System.out.println("预热布隆过滤器，key数量: " + existingKeys.size());
        for (String key : existingKeys) {
            bloomFilter.add(key);
        }
    }
    
    /**
     * 简单布隆过滤器实现
     */
    public static class BloomFilter {
        private final BitSet bitSet;
        private final int bitSetSize;
        private final int hashFunctionCount;
        
        public BloomFilter(int expectedElements, double falsePositiveRate) {
            this.bitSetSize = (int) (-expectedElements * Math.log(falsePositiveRate) / (Math.log(2) * Math.log(2)));
            this.hashFunctionCount = (int) (bitSetSize * Math.log(2) / expectedElements);
            this.bitSet = new BitSet(bitSetSize);
        }
        
        public void add(String element) {
            for (int i = 0; i < hashFunctionCount; i++) {
                int hash = hash(element, i);
                bitSet.set(Math.abs(hash % bitSetSize));
            }
        }
        
        public boolean mightContain(String element) {
            for (int i = 0; i < hashFunctionCount; i++) {
                int hash = hash(element, i);
                if (!bitSet.get(Math.abs(hash % bitSetSize))) {
                    return false;
                }
            }
            return true;
        }
        
        private int hash(String element, int seed) {
            return (element + seed).hashCode();
        }
    }
}
```

## 面试要点

### 高频问题

1. **分布式缓存的分片策略有哪些？各有什么优缺点？**
   - 一致性哈希：负载均衡好，扩缩容影响小，但实现复杂
   - 范围分片：支持范围查询，但可能负载不均
   - 哈希分片：简单高效，但扩缩容成本高

2. **如何解决缓存穿透、击穿、雪崩问题？**
   - 穿透：布隆过滤器 + 缓存空值
   - 击穿：互斥锁 + 热点数据永不过期
   - 雪崩：随机过期时间 + 多级缓存

3. **LRU和LFU算法的区别和适用场景？**
   - LRU：基于时间局部性，适合时间相关的访问模式
   - LFU：基于频率局部性，适合频率相关的访问模式

4. **分布式缓存的一致性如何保证？**
   - 最终一致性：异步复制 + 反熵同步
   - 强一致性：同步复制 + 共识算法
   - 弱一致性：写时复制 + 版本控制

### 深入问题

1. **一致性哈希算法中虚拟节点的作用？**
   - 提高负载均衡性
   - 减少数据迁移量
   - 虚拟节点数量的选择策略

2. **缓存更新策略有哪些？**
   - Cache Aside：应用负责缓存更新
   - Write Through：写入时同步更新缓存
   - Write Behind：异步批量更新

3. **如何设计一个高可用的分布式缓存系统？**
   - 多副本机制
   - 故障检测和自动切换
   - 数据分片和负载均衡
   - 监控和告警系统

### 实践经验

1. **生产环境中如何选择缓存方案？**
   - 根据数据访问模式选择淘汰算法
   - 根据一致性要求选择复制策略
   - 根据扩展性需求选择分片算法

2. **缓存性能优化的关键点？**
   - 合理设置缓存大小和过期时间
   - 使用批量操作减少网络开销
   - 实施缓存预热策略
   - 监控缓存命中率和响应时间

3. **如何处理缓存热点问题？**
   - 热点数据识别和预测
   - 多级缓存架构
   - 热点数据复制到多个节点
   - 使用本地缓存减少远程访问

## 总结

分布式缓存算法是构建高性能分布式系统的关键技术。本文详细介绍了：

1. **分片算法**：一致性哈希和范围分片，解决数据分布问题
2. **一致性算法**：写时复制和最终一致性，平衡性能和一致性
3. **淘汰算法**：LRU和LFU，优化内存使用效率
4. **最佳实践**：缓存预热、监控告警、防护机制

在实际应用中，需要根据具体的业务场景、性能要求和一致性需求来选择合适的算法组合。同时，要重视监控和运维，确保缓存系统的稳定性和高可用性。

通过深入理解这些算法的原理和实现，可以更好地设计和优化分布式缓存系统，为业务提供高性能的数据访问服务。
```

### 2. 范围分片算法

```java
public class RangeShardingCache {
    
    public static class ShardRange {
        private final String nodeId;
        private final String startKey;
        private final String endKey;
        
        public ShardRange(String nodeId, String startKey, String endKey) {
            this.nodeId = nodeId;
            this.startKey = startKey;
            this.endKey = endKey;
        }
        
        public boolean contains(String key) {
            return key.compareTo(startKey) >= 0 && key.compareTo(endKey) < 0;
        }
        
        // getters
        public String getNodeId() { return nodeId; }
        public String getStartKey() { return startKey; }
        public String getEndKey() { return endKey; }
    }
    
    private final List<ShardRange> shardRanges = new ArrayList<>();
    private final Map<String, Map<String, Object>> nodeData = new ConcurrentHashMap<>();
    
    /**
     * 添加分片范围
     */
    public void addShardRange(String nodeId, String startKey, String endKey) {
        shardRanges.add(new ShardRange(nodeId, startKey, endKey));
        nodeData.put(nodeId, new ConcurrentHashMap<>());
        
        // 按起始key排序
        shardRanges.sort((a, b) -> a.getStartKey().compareTo(b.getStartKey()));
        
        System.out.println("添加分片范围: " + nodeId + " [" + startKey + ", " + endKey + ")");
    }
    
    /**
     * 获取key对应的节点
     */
    public String getNode(String key) {
        for (ShardRange range : shardRanges) {
            if (range.contains(key)) {
                return range.getNodeId();
            }
        }
        return null; // 没有找到对应的分片
    }
    
    /**
     * 存储数据
     */
    public boolean put(String key, Object value) {
        String nodeId = getNode(key);
        if (nodeId != null) {
            nodeData.get(nodeId).put(key, value);
            System.out.println("存储数据 [" + key + "] 到节点: " + nodeId);
            return true;
        }
        System.out.println("警告: key [" + key + "] 没有对应的分片");
        return false;
    }
    
    /**
     * 获取数据
     */
    public Object get(String key) {
        String nodeId = getNode(key);
        if (nodeId != null) {
            Object value = nodeData.get(nodeId).get(key);
            System.out.println("从节点 " + nodeId + " 获取数据 [" + key + "]: " + value);
            return value;
        }
        return null;
    }
    
    /**
     * 范围查询
     */
    public Map<String, Object> rangeQuery(String startKey, String endKey) {
        Map<String, Object> result = new HashMap<>();
        
        for (ShardRange range : shardRanges) {
            // 检查分片范围是否与查询范围有交集
            if (range.getStartKey().compareTo(endKey) < 0 && range.getEndKey().compareTo(startKey) > 0) {
                Map<String, Object> shardData = nodeData.get(range.getNodeId());
                for (Map.Entry<String, Object> entry : shardData.entrySet()) {
                    String key = entry.getKey();
                    if (key.compareTo(startKey) >= 0 && key.compareTo(endKey) < 0) {
                        result.put(key, entry.getValue());
                    }
                }
            }
        }
        
        System.out.println("范围查询 [" + startKey + ", " + endKey + ") 返回 " + result.size() + " 条记录");
        return result;
    }
    
    /**
     * 分片分裂
     */
    public void splitShard(String nodeId, String splitKey, String newNodeId) {
        ShardRange targetRange = null;
        for (ShardRange range : shardRanges) {
            if (range.getNodeId().equals(nodeId)) {
                targetRange = range;
                break;
            }
        }
        
        if (targetRange == null || !targetRange.contains(splitKey)) {
            System.out.println("无法分裂分片: " + nodeId);
            return;
        }
        
        // 移除原分片
        shardRanges.remove(targetRange);
        
        // 创建两个新分片
        addShardRange(nodeId, targetRange.getStartKey(), splitKey);
        addShardRange(newNodeId, splitKey, targetRange.getEndKey());
        
        // 数据迁移
        Map<String, Object> originalData = nodeData.get(nodeId);
        Map<String, Object> newData = new HashMap<>();
        
        Iterator<Map.Entry<String, Object>> iterator = originalData.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> entry = iterator.next();
            String key = entry.getKey();
            if (key.compareTo(splitKey) >= 0) {
                newData.put(key, entry.getValue());
                iterator.remove();
            }
        }
        
        nodeData.get(newNodeId).putAll(newData);
        
        System.out.println("分片分裂完成: " + nodeId + " -> [" + nodeId + ", " + newNodeId + "], 分裂点: " + splitKey);
    }
    
    /**
     * 打印分片信息
     */
    public void printShardInfo() {
        System.out.println("\n=== 分片信息 ===");
        for (ShardRange range : shardRanges) {
            Map<String, Object> data = nodeData.get(range.getNodeId());
            System.out.println("节点 " + range.getNodeId() + ": [" + 
                             range.getStartKey() + ", " + range.getEndKey() + "), " + 
                             data.size() + " 个key");
        }
    }
}
```

## 缓存一致性算法

### 1. 写时复制（Copy-on-Write）

```java
public class CopyOnWriteCache {
    
    public static class CacheEntry {
        private final Object value;
        private final long version;
        private final long timestamp;
        
        public CacheEntry(Object value, long version) {
            this.value = value;
            this.version = version;
            this.timestamp = System.currentTimeMillis();
        }
        
        // getters
        public Object getValue() { return value; }
        public long getVersion() { return version; }
        public long getTimestamp() { return timestamp; }
    }
    
    // 主缓存（写节点）
    private final Map<String, CacheEntry> masterCache = new ConcurrentHashMap<>();
    
    // 只读副本缓存
    private final List<Map<String, CacheEntry>> replicaCaches = new ArrayList<>();
    
    // 版本计数器
    private final AtomicLong versionCounter = new AtomicLong(0);
    
    // 复制延迟（模拟网络延迟）
    private final long replicationDelayMs;
    
    // 异步复制执行器
    private final ScheduledExecutorService replicationExecutor;
    
    public CopyOnWriteCache(int replicaCount, long replicationDelayMs) {
        this.replicationDelayMs = replicationDelayMs;
        this.replicationExecutor = Executors.newScheduledThreadPool(2);
        
        // 初始化副本缓存
        for (int i = 0; i < replicaCount; i++) {
            replicaCaches.add(new ConcurrentHashMap<>());
        }
    }
    
    /**
     * 写操作（只在主节点执行）
     */
    public void put(String key, Object value) {
        long version = versionCounter.incrementAndGet();
        CacheEntry entry = new CacheEntry(value, version);
        
        // 写入主缓存
        masterCache.put(key, entry);
        System.out.println("主节点写入: [" + key + "] = " + value + ", 版本: " + version);
        
        // 异步复制到副本
        replicationExecutor.schedule(() -> {
            replicateToReplicas(key, entry);
        }, replicationDelayMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 读操作（可以从任意节点读取）
     */
    public Object get(String key, int nodeIndex) {
        if (nodeIndex == 0) {
            // 从主节点读取
            CacheEntry entry = masterCache.get(key);
            Object value = entry != null ? entry.getValue() : null;
            System.out.println("主节点读取: [" + key + "] = " + value);
            return value;
        } else if (nodeIndex <= replicaCaches.size()) {
            // 从副本节点读取
            Map<String, CacheEntry> replica = replicaCaches.get(nodeIndex - 1);
            CacheEntry entry = replica.get(key);
            Object value = entry != null ? entry.getValue() : null;
            System.out.println("副本" + nodeIndex + "读取: [" + key + "] = " + value);
            return value;
        }
        return null;
    }
    
    /**
     * 删除操作
     */
    public void remove(String key) {
        long version = versionCounter.incrementAndGet();
        
        // 从主缓存删除
        CacheEntry removed = masterCache.remove(key);
        System.out.println("主节点删除: [" + key + "], 版本: " + version);
        
        // 异步从副本删除
        replicationExecutor.schedule(() -> {
            for (int i = 0; i < replicaCaches.size(); i++) {
                replicaCaches.get(i).remove(key);
                System.out.println("副本" + (i + 1) + "删除: [" + key + "]");
            }
        }, replicationDelayMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 复制到副本节点
     */
    private void replicateToReplicas(String key, CacheEntry entry) {
        for (int i = 0; i < replicaCaches.size(); i++) {
            Map<String, CacheEntry> replica = replicaCaches.get(i);
            
            // 检查版本，只复制更新的数据
            CacheEntry existing = replica.get(key);
            if (existing == null || existing.getVersion() < entry.getVersion()) {
                replica.put(key, entry);
                System.out.println("复制到副本" + (i + 1) + ": [" + key + "] = " + 
                                 entry.getValue() + ", 版本: " + entry.getVersion());
            }
        }
    }
    
    /**
     * 强制同步所有副本
     */
    public void syncAllReplicas() {
        System.out.println("\n开始强制同步所有副本...");
        
        for (Map.Entry<String, CacheEntry> masterEntry : masterCache.entrySet()) {
            String key = masterEntry.getKey();
            CacheEntry entry = masterEntry.getValue();
            
            for (int i = 0; i < replicaCaches.size(); i++) {
                Map<String, CacheEntry> replica = replicaCaches.get(i);
                CacheEntry existing = replica.get(key);
                
                if (existing == null || existing.getVersion() < entry.getVersion()) {
                    replica.put(key, entry);
                    System.out.println("同步到副本" + (i + 1) + ": [" + key + "] = " + 
                                     entry.getValue() + ", 版本: " + entry.getVersion());
                }
            }
        }
        
        System.out.println("强制同步完成");
    }
    
    /**
     * 检查一致性
     */
    public void checkConsistency() {
        System.out.println("\n=== 一致性检查 ===");
        
        Set<String> allKeys = new HashSet<>(masterCache.keySet());
        for (Map<String, CacheEntry> replica : replicaCaches) {
            allKeys.addAll(replica.keySet());
        }
        
        for (String key : allKeys) {
            CacheEntry masterEntry = masterCache.get(key);
            System.out.print("Key [" + key + "]: 主节点=" + 
                           (masterEntry != null ? masterEntry.getValue() + "(v" + masterEntry.getVersion() + ")" : "null"));
            
            for (int i = 0; i < replicaCaches.size(); i++) {
                CacheEntry replicaEntry = replicaCaches.get(i).get(key);
                System.out.print(", 副本" + (i + 1) + "=" + 
                               (replicaEntry != null ? replicaEntry.getValue() + "(v" + replicaEntry.getVersion() + ")" : "null"));
            }
            System.out.println();
        }
    }
    
    /**
     * 关闭缓存
     */
    public void shutdown() {
        replicationExecutor.shutdown();
        try {
            if (!replicationExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                replicationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            replicationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### 2. 最终一致性缓存

```java
public class EventualConsistencyCache {
    
    public static class CacheNode {
        private final String nodeId;
        private final Map<String, Object> data = new ConcurrentHashMap<>();
        private final Map<String, Long> versions = new ConcurrentHashMap<>();
        private final Set<String> peers = new HashSet<>();
        
        public CacheNode(String nodeId) {
            this.nodeId = nodeId;
        }
        
        public void addPeer(String peerId) {
            peers.add(peerId);
        }
        
        public void put(String key, Object value, long version) {
            Long currentVersion = versions.get(key);
            if (currentVersion == null || version > currentVersion) {
                data.put(key, value);
                versions.put(key, version);
                System.out.println("节点 " + nodeId + " 更新: [" + key + "] = " + value + ", 版本: " + version);
            } else {
                System.out.println("节点 " + nodeId + " 忽略旧版本: [" + key + "], 当前版本: " + currentVersion + ", 接收版本: " + version);
            }
        }
        
        public Object get(String key) {
            return data.get(key);
        }
        
        public Long getVersion(String key) {
            return versions.get(key);
        }
        
        public void remove(String key, long version) {
            Long currentVersion = versions.get(key);
            if (currentVersion == null || version > currentVersion) {
                data.remove(key);
                versions.put(key, version); // 保留删除版本
                System.out.println("节点 " + nodeId + " 删除: [" + key + "], 版本: " + version);
            }
        }
        
        public Map<String, Object> getAllData() {
            return new HashMap<>(data);
        }
        
        public Map<String, Long> getAllVersions() {
            return new HashMap<>(versions);
        }
        
        public Set<String> getPeers() {
            return new HashSet<>(peers);
        }
        
        public String getNodeId() {
            return nodeId;
        }
    }
    
    private final Map<String, CacheNode> nodes = new ConcurrentHashMap<>();
    private final AtomicLong globalVersion = new AtomicLong(0);
    private final ScheduledExecutorService syncExecutor = Executors.newScheduledThreadPool(2);
    private final Random random = new Random();
    
    /**
     * 添加缓存节点
     */
    public void addNode(String nodeId) {
        nodes.put(nodeId, new CacheNode(nodeId));
        System.out.println("添加缓存节点: " + nodeId);
    }
    
    /**
     * 建立节点间连接
     */
    public void connectNodes(String nodeId1, String nodeId2) {
        CacheNode node1 = nodes.get(nodeId1);
        CacheNode node2 = nodes.get(nodeId2);
        
        if (node1 != null && node2 != null) {
            node1.addPeer(nodeId2);
            node2.addPeer(nodeId1);
            System.out.println("连接节点: " + nodeId1 + " <-> " + nodeId2);
        }
    }
    
    /**
     * 在指定节点写入数据
     */
    public void put(String nodeId, String key, Object value) {
        CacheNode node = nodes.get(nodeId);
        if (node == null) {
            return;
        }
        
        long version = globalVersion.incrementAndGet();
        node.put(key, value, version);
        
        // 异步传播到其他节点
        propagateUpdate(nodeId, key, value, version);
    }
    
    /**
     * 从指定节点读取数据
     */
    public Object get(String nodeId, String key) {
        CacheNode node = nodes.get(nodeId);
        if (node == null) {
            return null;
        }
        
        Object value = node.get(key);
        System.out.println("节点 " + nodeId + " 读取: [" + key + "] = " + value);
        return value;
    }
    
    /**
     * 从指定节点删除数据
     */
    public void remove(String nodeId, String key) {
        CacheNode node = nodes.get(nodeId);
        if (node == null) {
            return;
        }
        
        long version = globalVersion.incrementAndGet();
        node.remove(key, version);
        
        // 异步传播删除操作
        propagateRemove(nodeId, key, version);
    }
    
    /**
     * 传播更新操作
     */
    private void propagateUpdate(String sourceNodeId, String key, Object value, long version) {
        CacheNode sourceNode = nodes.get(sourceNodeId);
        if (sourceNode == null) {
            return;
        }
        
        for (String peerId : sourceNode.getPeers()) {
            // 模拟网络延迟
            int delay = 50 + random.nextInt(100); // 50-150ms
            
            syncExecutor.schedule(() -> {
                CacheNode peerNode = nodes.get(peerId);
                if (peerNode != null) {
                    peerNode.put(key, value, version);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * 传播删除操作
     */
    private void propagateRemove(String sourceNodeId, String key, long version) {
        CacheNode sourceNode = nodes.get(sourceNodeId);
        if (sourceNode == null) {
            return;
        }
        
        for (String peerId : sourceNode.getPeers()) {
            // 模拟网络延迟
            int delay = 50 + random.nextInt(100);
            
            syncExecutor.schedule(() -> {
                CacheNode peerNode = nodes.get(peerId);
                if (peerNode != null) {
                    peerNode.remove(key, version);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }
    
    /**
     * 反熵同步（定期同步以确保最终一致性）
     */
    public void startAntiEntropySync(long intervalMs) {
        syncExecutor.scheduleAtFixedRate(() -> {
            performAntiEntropySync();
        }, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }
    
    private void performAntiEntropySync() {
        System.out.println("\n执行反熵同步...");
        
        List<String> nodeIds = new ArrayList<>(nodes.keySet());
        
        // 随机选择两个节点进行同步
        if (nodeIds.size() >= 2) {
            Collections.shuffle(nodeIds);
            String node1Id = nodeIds.get(0);
            String node2Id = nodeIds.get(1);
            
            syncBetweenNodes(node1Id, node2Id);
        }
    }
    
    private void syncBetweenNodes(String nodeId1, String nodeId2) {
        CacheNode node1 = nodes.get(nodeId1);
        CacheNode node2 = nodes.get(nodeId2);
        
        if (node1 == null || node2 == null) {
            return;
        }
        
        // 获取所有key
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(node1.getAllVersions().keySet());
        allKeys.addAll(node2.getAllVersions().keySet());
        
        for (String key : allKeys) {
            Long version1 = node1.getVersion(key);
            Long version2 = node2.getVersion(key);
            
            if (version1 == null && version2 != null) {
                // node1没有，node2有
                Object value = node2.get(key);
                if (value != null) {
                    node1.put(key, value, version2);
                }
            } else if (version1 != null && version2 == null) {
                // node1有，node2没有
                Object value = node1.get(key);
                if (value != null) {
                    node2.put(key, value, version1);
                }
            } else if (version1 != null && version2 != null) {
                // 两个节点都有，比较版本
                if (version1 > version2) {
                    Object value = node1.get(key);
                    if (value != null) {
                        node2.put(key, value, version1);
                    }
                } else if (version2 > version1) {
                    Object value = node2.get(key);
                    if (value != null) {
                        node1.put(key, value, version2);
                    }
                }
            }
        }
        
        System.out.println("节点 " + nodeId1 + " 和 " + nodeId2 + " 同步完成");
    }
    
    /**
     * 检查所有节点的一致性状态
     */
    public void checkConsistency() {
        System.out.println("\n=== 一致性检查 ===");
        
        // 收集所有key
        Set<String> allKeys = new HashSet<>();
        for (CacheNode node : nodes.values()) {
            allKeys.addAll(node.getAllVersions().keySet());
        }
        
        for (String key : allKeys) {
            System.out.print("Key [" + key + "]: ");
            
            for (CacheNode node : nodes.values()) {
                Object value = node.get(key);
                Long version = node.getVersion(key);
                System.out.print(node.getNodeId() + "=" + value + "(v" + version + ") ");
            }
            System.out.println();
        }
    }
    
    /**
     * 关闭缓存系统
     */
    public void shutdown() {
        syncExecutor.shutdown();
        try {
            if (!syncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                syncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            syncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

## 缓存淘汰算法

### 1. LRU（最近最少使用）

```java
public class LRUCache<K, V> {
    
    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;
        long accessTime;
        
        Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.accessTime = System.currentTimeMillis();
        }
    }
    
    private final int capacity;
    private final Map<K, Node<K, V>> cache;
    private final Node<K, V> head;
    private final Node<K, V> tail;
    private final AtomicInteger size = new AtomicInteger(0);
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>();
        
        // 创建虚拟头尾节点
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }
    
    /**
     * 获取数据
     */
    public synchronized V get(K key) {
        Node<K, V> node = cache.get(key);
        
        if (node == null) {
            missCount.incrementAndGet();
            System.out.println("缓存未命中: " + key);
            return null;
        }
        
        hitCount.incrementAndGet();
        
        // 更新访问时间
        node.accessTime = System.currentTimeMillis();
        
        // 移动到头部
        moveToHead(node);
        
        System.out.println("缓存命中: " + key + " = " + node.value);
        return node.value;
    }
    
    /**
     * 存储数据
     */
    public synchronized void put(K key, V value) {
        Node<K, V> existing = cache.get(key);
        
        if (existing != null) {
            // 更新现有节点
            existing.value = value;
            existing.accessTime = System.currentTimeMillis();
            moveToHead(existing);
            System.out.println("更新缓存: " + key + " = " + value);
        } else {
            // 创建新节点
            Node<K, V> newNode = new Node<>(key, value);
            
            if (size.get() >= capacity) {
                // 缓存已满，移除最少使用的节点
                Node<K, V> tail = removeTail();
                cache.remove(tail.key);
                size.decrementAndGet();
                System.out.println("淘汰缓存: " + tail.key);
            }
            
            cache.put(key, newNode);
            addToHead(newNode);
            size.incrementAndGet();
            System.out.println("添加缓存: " + key + " = " + value);
        }
    }
    
    /**
     * 删除数据
     */
    public synchronized V remove(K key) {
        Node<K, V> node = cache.remove(key);
        
        if (node != null) {
            removeNode(node);
            size.decrementAndGet();
            System.out.println("删除缓存: " + key);
            return node.value;
        }
        
        return null;
    }
    
    /**
     * 添加节点到头部
     */
    private void addToHead(Node<K, V> node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }
    
    /**
     * 移除节点
     */
    private void removeNode(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }
    
    /**
     * 移动节点到头部
     */
    private void moveToHead(Node<K, V> node) {
        removeNode(node);
        addToHead(node);
    }
    
    /**
     * 移除尾部节点
     */
    private Node<K, V> removeTail() {
        Node<K, V> lastNode = tail.prev;
        removeNode(lastNode);
        return lastNode;
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        long hits = hitCount.get();
        long misses = missCount.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total : 0.0;
        
        return new CacheStats(hits, misses, hitRate, size.get(), capacity);
    }
    
    /**
     * 清空缓存
     */
    public synchronized void clear() {
        cache.clear();
        head.next = tail;
        tail.prev = head;
        size.set(0);
        hitCount.set(0);
        missCount.set(0);
        System.out.println("清空缓存");
    }
    
    /**
     * 获取当前大小
     */
    public int size() {
        return size.get();
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return size.get() == 0;
    }
    
    /**
     * 打印缓存状态
     */
    public synchronized void printCacheState() {
        System.out.println("\n=== LRU缓存状态 ===");
        System.out.println("容量: " + capacity + ", 当前大小: " + size.get());
        
        CacheStats stats = getStats();
        System.out.println("命中率: " + String.format("%.2f%%", stats.getHitRate() * 100));
        System.out.println("命中次数: " + stats.getHitCount());
        System.out.println("未命中次数: " + stats.getMissCount());
        
        System.out.println("\n缓存内容（按访问顺序）:");
        Node<K, V> current = head.next;
        int index = 1;
        while (current != tail) {
            System.out.println(index + ". " + current.key + " = " + current.value + 
                             " (访问时间: " + current.accessTime + ")");
            current = current.next;
            index++;
        }
    }
    
    public static class CacheStats {
        private final long hitCount;
        private final long missCount;
        private final double hitRate;
        private final int currentSize;
        private final int maxSize;
        
        public CacheStats(long hitCount, long missCount, double hitRate, int currentSize, int maxSize) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.currentSize = currentSize;
            this.maxSize = maxSize;
        }
        
        // getters
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public double getHitRate() { return hitRate; }
        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
    }
}
```

### 2. LFU（最少使用频率）

```java
public class LFUCache<K, V> {
    
    private static class Node<K, V> {
        K key;
        V value;
        int frequency;
        long lastAccessTime;
        
        Node(K key, V value) {
            this.key = key;
            this.value = value;
            this.frequency = 1;
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        void incrementFrequency() {
            frequency++;
            lastAccessTime = System.currentTimeMillis();
        }
    }
    
    private final int capacity;
    private final Map<K, Node<K, V>> cache;
    private final Map<Integer, LinkedHashSet<K>> frequencyGroups;
    private int minFrequency;
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    
    public LFUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>();
        this.frequencyGroups = new ConcurrentHashMap<>();
        this.minFrequency = 1;
    }
    
    /**
     * 获取数据
     */
    public synchronized V get(K key) {
        Node<K, V> node = cache.get(key);
        
        if (node == null) {
            missCount.incrementAndGet();
            System.out.println("缓存未命中: " + key);
            return null;
        }
        
        hitCount.incrementAndGet();
        
        // 更新频率
        updateFrequency(node);
        
        System.out.println("缓存命中: " + key + " = " + node.value + ", 频率: " + node.frequency);
        return node.value;
    }
    
    /**
     * 存储数据
     */
    public synchronized void put(K key, V value) {
        if (capacity <= 0) {
            return;
        }
        
        Node<K, V> existing = cache.get(key);
        
        if (existing != null) {
            // 更新现有节点
            existing.value = value;
            updateFrequency(existing);
            System.out.println("更新缓存: " + key + " = " + value + ", 频率: " + existing.frequency);
        } else {
            // 检查容量
            if (cache.size() >= capacity) {
                evictLFU();
            }
            
            // 创建新节点
            Node<K, V> newNode = new Node<>(key, value);
            cache.put(key, newNode);
            
            // 添加到频率组
            frequencyGroups.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(key);
            minFrequency = 1;
            
            System.out.println("添加缓存: " + key + " = " + value + ", 频率: 1");
        }
    }
    
    /**
     * 删除数据
     */
    public synchronized V remove(K key) {
        Node<K, V> node = cache.remove(key);
        
        if (node != null) {
            // 从频率组中移除
            LinkedHashSet<K> group = frequencyGroups.get(node.frequency);
            if (group != null) {
                group.remove(key);
                if (group.isEmpty() && node.frequency == minFrequency) {
                    minFrequency++;
                }
            }
            
            System.out.println("删除缓存: " + key);
            return node.value;
        }
        
        return null;
    }
    
    /**
     * 更新节点频率
     */
    private void updateFrequency(Node<K, V> node) {
        K key = node.key;
        int oldFreq = node.frequency;
        
        // 从旧频率组移除
        LinkedHashSet<K> oldGroup = frequencyGroups.get(oldFreq);
        if (oldGroup != null) {
            oldGroup.remove(key);
            if (oldGroup.isEmpty() && oldFreq == minFrequency) {
                minFrequency++;
            }
        }
        
        // 增加频率
        node.incrementFrequency();
        
        // 添加到新频率组
        frequencyGroups.computeIfAbsent(node.frequency, k -> new LinkedHashSet<>()).add(key);
    }
    
    /**
     * 淘汰最少使用频率的节点
     */
    private void evictLFU() {
        // 找到最小频率组
        LinkedHashSet<K> minFreqGroup = frequencyGroups.get(minFrequency);
        
        if (minFreqGroup == null || minFreqGroup.isEmpty()) {
            // 重新计算最小频率
            minFrequency = frequencyGroups.keySet().stream()
                .filter(freq -> !frequencyGroups.get(freq).isEmpty())
                .min(Integer::compareTo)
                .orElse(1);
            minFreqGroup = frequencyGroups.get(minFrequency);
        }
        
        if (minFreqGroup != null && !minFreqGroup.isEmpty()) {
            // 移除最早添加的节点（LRU策略作为tie-breaker）
            K evictKey = minFreqGroup.iterator().next();
            minFreqGroup.remove(evictKey);
            
            Node<K, V> evictNode = cache.remove(evictKey);
            System.out.println("淘汰缓存: " + evictKey + ", 频率: " + 
                             (evictNode != null ? evictNode.frequency : "unknown"));
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        long hits = hitCount.get();
        long misses = missCount.get();
        long total = hits + misses;
        double hitRate = total > 0 ? (double) hits / total : 0.0;
        
        return new CacheStats(hits, misses, hitRate, cache.size(), capacity);
    }
    
    /**
     * 清空缓存
     */
    public synchronized void clear() {
        cache.clear();
        frequencyGroups.clear();
        minFrequency = 1;
        hitCount.set(0);
        missCount.set(0);
        System.out.println("清空缓存");
    }
    
    /**
     * 获取当前大小
     */
    public int size() {
        return cache.size();
    }
    
    /**
     * 检查是否为空
     */
    public boolean isEmpty() {
        return cache.isEmpty();
    }
    
    /**
     * 打印缓存状态
     */
    public synchronized void printCacheState() {
        System.out.println("\n=== LFU缓存状态 ===");
        System.out.println("容量: " + capacity + ", 当前大小: " + cache.size());
        System.out.println("最小频率: " + minFrequency);
        
        CacheStats stats = getStats();
        System.out.println("命中率: " + String.format("%.2f%%", stats.getHitRate() * 100));
        System.out.println("命中次数: " + stats.getHitCount());
        System.out.println("未命中次数: " + stats.getMissCount());
        
        System.out.println("\n频率分布:");
        frequencyGroups.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                int freq = entry.getKey();
                Set<K> keys = entry.getValue();
                if (!keys.isEmpty()) {
                    System.out.println("频率 " + freq + ": " + keys.size() + " 个key - " + keys);
                }
            });
        
        System.out.println("\n缓存内容:");
        cache.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().frequency, e1.getValue().frequency))
            .forEach(entry -> {
                Node<K, V> node = entry.getValue();
                System.out.println(entry.getKey() + " = " + node.value + 
                                 ", 频率: " + node.frequency + 
                                 ", 最后访问: " + node.lastAccessTime);
            });
    }
    
    public static class CacheStats {
        private final long hitCount;
        private final long missCount;
        private final double hitRate;
        private final int currentSize;
        private final int maxSize;
        
        public CacheStats(long hitCount, long missCount, double hitRate, int currentSize, int maxSize) {
            this.hitCount = hitCount;
            this.missCount = missCount;
            this.hitRate = hitRate;
            this.currentSize = currentSize;
            this.maxSize = maxSize;
        }
        
        // getters
        public long getHitCount() { return hitCount; }
        public long getMissCount() { return missCount; }
        public double getHitRate() { return hitRate; }
        public int getCurrentSize() { return currentSize; }
        public int getMaxSize() { return maxSize; }
    }
}
```