# 分布式存储算法

## 概述

分布式存储系统将数据分散存储在多个节点上，通过复制、分片、一致性哈希等技术实现高可用性、可扩展性和容错性。本文档介绍主要的分布式存储算法和实现。

### 核心概念

- **数据分片(Sharding)**：将数据水平分割到多个节点
- **数据复制(Replication)**：在多个节点上保存数据副本
- **一致性级别**：强一致性、最终一致性、弱一致性
- **CAP定理**：一致性、可用性、分区容错性不能同时满足

## 数据分片算法

### 范围分片(Range Sharding)

根据数据的键值范围进行分片，适用于有序数据。

```java
public class RangeSharding {
    private final List<ShardRange> shardRanges;
    private final Map<String, StorageNode> nodes;
    
    public RangeSharding() {
        this.shardRanges = new ArrayList<>();
        this.nodes = new HashMap<>();
        initializeShards();
    }
    
    private void initializeShards() {
        // 初始化分片范围
        shardRanges.add(new ShardRange("shard1", "0000", "3333"));
        shardRanges.add(new ShardRange("shard2", "3334", "6666"));
        shardRanges.add(new ShardRange("shard3", "6667", "9999"));
        
        // 初始化存储节点
        nodes.put("shard1", new StorageNode("node1", "192.168.1.1"));
        nodes.put("shard2", new StorageNode("node2", "192.168.1.2"));
        nodes.put("shard3", new StorageNode("node3", "192.168.1.3"));
    }
    
    public StorageNode getShardForKey(String key) {
        for (ShardRange range : shardRanges) {
            if (isKeyInRange(key, range)) {
                return nodes.get(range.getShardId());
            }
        }
        throw new IllegalArgumentException("无法找到键对应的分片: " + key);
    }
    
    private boolean isKeyInRange(String key, ShardRange range) {
        return key.compareTo(range.getStartKey()) >= 0 && 
               key.compareTo(range.getEndKey()) <= 0;
    }
    
    public void put(String key, String value) {
        StorageNode node = getShardForKey(key);
        node.store(key, value);
        System.out.println("数据存储到节点: " + node.getNodeId() + ", Key: " + key);
    }
    
    public String get(String key) {
        StorageNode node = getShardForKey(key);
        String value = node.retrieve(key);
        System.out.println("从节点 " + node.getNodeId() + " 获取数据: " + key + " = " + value);
        return value;
    }
    
    // 分片分裂
    public void splitShard(String shardId, String splitKey) {
        ShardRange originalRange = null;
        for (ShardRange range : shardRanges) {
            if (range.getShardId().equals(shardId)) {
                originalRange = range;
                break;
            }
        }
        
        if (originalRange == null) {
            throw new IllegalArgumentException("分片不存在: " + shardId);
        }
        
        // 创建新的分片范围
        String newShardId = shardId + "_split";
        ShardRange newRange1 = new ShardRange(shardId, originalRange.getStartKey(), splitKey);
        ShardRange newRange2 = new ShardRange(newShardId, splitKey + "1", originalRange.getEndKey());
        
        // 更新分片列表
        shardRanges.remove(originalRange);
        shardRanges.add(newRange1);
        shardRanges.add(newRange2);
        
        // 创建新节点
        StorageNode newNode = new StorageNode("node_" + newShardId, "192.168.1." + (nodes.size() + 1));
        nodes.put(newShardId, newNode);
        
        System.out.println("分片 " + shardId + " 已分裂为 " + shardId + " 和 " + newShardId);
    }
    
    public void printShardInfo() {
        System.out.println("\n=== 分片信息 ===");
        for (ShardRange range : shardRanges) {
            StorageNode node = nodes.get(range.getShardId());
            System.out.println("分片: " + range.getShardId() + 
                             ", 范围: [" + range.getStartKey() + ", " + range.getEndKey() + "]" +
                             ", 节点: " + node.getNodeId());
        }
    }
}

class ShardRange {
    private final String shardId;
    private final String startKey;
    private final String endKey;
    
    public ShardRange(String shardId, String startKey, String endKey) {
        this.shardId = shardId;
        this.startKey = startKey;
        this.endKey = endKey;
    }
    
    public String getShardId() { return shardId; }
    public String getStartKey() { return startKey; }
    public String getEndKey() { return endKey; }
}

class StorageNode {
    private final String nodeId;
    private final String address;
    private final Map<String, String> data;
    
    public StorageNode(String nodeId, String address) {
        this.nodeId = nodeId;
        this.address = address;
        this.data = new ConcurrentHashMap<>();
    }
    
    public void store(String key, String value) {
        data.put(key, value);
    }
    
    public String retrieve(String key) {
        return data.get(key);
    }
    
    public String getNodeId() { return nodeId; }
    public String getAddress() { return address; }
    public Map<String, String> getData() { return new HashMap<>(data); }
}
```

### 哈希分片(Hash Sharding)

使用哈希函数将数据均匀分布到各个分片。

```java
public class HashSharding {
    private final List<StorageNode> nodes;
    private final int shardCount;
    
    public HashSharding(int shardCount) {
        this.shardCount = shardCount;
        this.nodes = new ArrayList<>();
        initializeNodes();
    }
    
    private void initializeNodes() {
        for (int i = 0; i < shardCount; i++) {
            nodes.add(new StorageNode("node" + i, "192.168.1." + (i + 1)));
        }
    }
    
    private int getShardIndex(String key) {
        return Math.abs(key.hashCode()) % shardCount;
    }
    
    public StorageNode getShardForKey(String key) {
        int shardIndex = getShardIndex(key);
        return nodes.get(shardIndex);
    }
    
    public void put(String key, String value) {
        StorageNode node = getShardForKey(key);
        node.store(key, value);
        System.out.println("哈希分片存储: " + node.getNodeId() + ", Key: " + key);
    }
    
    public String get(String key) {
        StorageNode node = getShardForKey(key);
        String value = node.retrieve(key);
        System.out.println("哈希分片获取: " + node.getNodeId() + ", Key: " + key + " = " + value);
        return value;
    }
    
    // 数据迁移（扩容时）
    public void addNode() {
        int newShardCount = shardCount + 1;
        StorageNode newNode = new StorageNode("node" + shardCount, "192.168.1." + (shardCount + 1));
        
        // 收集所有数据
        Map<String, String> allData = new HashMap<>();
        for (StorageNode node : nodes) {
            allData.putAll(node.getData());
            node.getData().clear(); // 清空原数据
        }
        
        nodes.add(newNode);
        
        // 重新分布数据
        for (Map.Entry<String, String> entry : allData.entrySet()) {
            int newShardIndex = Math.abs(entry.getKey().hashCode()) % newShardCount;
            nodes.get(newShardIndex).store(entry.getKey(), entry.getValue());
        }
        
        System.out.println("新增节点完成，当前节点数: " + nodes.size());
    }
    
    public void printDistribution() {
        System.out.println("\n=== 哈希分片数据分布 ===");
        for (int i = 0; i < nodes.size(); i++) {
            StorageNode node = nodes.get(i);
            System.out.println("节点 " + node.getNodeId() + ": " + node.getData().size() + " 条数据");
        }
    }
}
```

## 数据复制算法

### 主从复制(Master-Slave Replication)

```java
public class MasterSlaveReplication {
    private final StorageNode master;
    private final List<StorageNode> slaves;
    private final ExecutorService executorService;
    
    public MasterSlaveReplication(String masterId) {
        this.master = new StorageNode(masterId, "192.168.1.1");
        this.slaves = new ArrayList<>();
        this.executorService = Executors.newFixedThreadPool(10);
    }
    
    public void addSlave(String slaveId, String address) {
        StorageNode slave = new StorageNode(slaveId, address);
        slaves.add(slave);
        System.out.println("添加从节点: " + slaveId);
    }
    
    public void put(String key, String value) {
        // 写入主节点
        master.store(key, value);
        System.out.println("主节点写入: " + key + " = " + value);
        
        // 异步复制到从节点
        replicateToSlaves(key, value);
    }
    
    private void replicateToSlaves(String key, String value) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (StorageNode slave : slaves) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 模拟网络延迟
                    Thread.sleep(10 + (int)(Math.random() * 50));
                    slave.store(key, value);
                    System.out.println("复制到从节点 " + slave.getNodeId() + ": " + key);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("复制到从节点失败: " + slave.getNodeId());
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // 等待所有复制完成（可选）
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> System.out.println("所有从节点复制完成: " + key));
    }
    
    public String get(String key) {
        // 从主节点读取（强一致性）
        return getFromMaster(key);
    }
    
    public String getFromMaster(String key) {
        String value = master.retrieve(key);
        System.out.println("从主节点读取: " + key + " = " + value);
        return value;
    }
    
    public String getFromSlave(String key) {
        if (slaves.isEmpty()) {
            return getFromMaster(key);
        }
        
        // 随机选择一个从节点
        StorageNode slave = slaves.get((int)(Math.random() * slaves.size()));
        String value = slave.retrieve(key);
        System.out.println("从从节点 " + slave.getNodeId() + " 读取: " + key + " = " + value);
        return value;
    }
    
    // 主节点故障转移
    public boolean failover() {
        if (slaves.isEmpty()) {
            System.err.println("没有可用的从节点进行故障转移");
            return false;
        }
        
        // 选择第一个从节点作为新主节点
        StorageNode newMaster = slaves.remove(0);
        System.out.println("故障转移：" + newMaster.getNodeId() + " 成为新的主节点");
        
        // 在实际实现中，这里需要更新配置和通知其他节点
        return true;
    }
    
    public void printReplicationStatus() {
        System.out.println("\n=== 主从复制状态 ===");
        System.out.println("主节点: " + master.getNodeId() + ", 数据量: " + master.getData().size());
        for (StorageNode slave : slaves) {
            System.out.println("从节点: " + slave.getNodeId() + ", 数据量: " + slave.getData().size());
        }
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}

## 测试示例

### 分片算法测试

```java
public class DistributedStorageTest {
    
    public static void testRangeSharding() {
        System.out.println("\n=== 范围分片测试 ===");
        RangeSharding rangeSharding = new RangeSharding();
        
        // 测试数据存储
        rangeSharding.put("1000", "用户1000数据");
        rangeSharding.put("5000", "用户5000数据");
        rangeSharding.put("8000", "用户8000数据");
        
        // 测试数据读取
        rangeSharding.get("1000");
        rangeSharding.get("5000");
        rangeSharding.get("8000");
        
        rangeSharding.printShardInfo();
        
        // 测试分片分裂
        rangeSharding.splitShard("shard2", "5000");
        rangeSharding.printShardInfo();
    }
    
    public static void testHashSharding() {
        System.out.println("\n=== 哈希分片测试 ===");
        HashSharding hashSharding = new HashSharding(3);
        
        // 测试数据分布
        String[] keys = {"user1", "user2", "user3", "user4", "user5", "user6"};
        for (String key : keys) {
            hashSharding.put(key, "数据_" + key);
        }
        
        hashSharding.printDistribution();
        
        // 测试扩容
        hashSharding.addNode();
        hashSharding.printDistribution();
    }
    
    public static void testMasterSlaveReplication() {
        System.out.println("\n=== 主从复制测试 ===");
        MasterSlaveReplication replication = new MasterSlaveReplication("master1");
        
        // 添加从节点
        replication.addSlave("slave1", "192.168.1.2");
        replication.addSlave("slave2", "192.168.1.3");
        
        // 测试写入和读取
        replication.put("key1", "value1");
        replication.put("key2", "value2");
        
        try {
            Thread.sleep(200); // 等待复制完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        replication.getFromMaster("key1");
        replication.getFromSlave("key1");
        
        replication.printReplicationStatus();
        replication.shutdown();
    }
    
    public static void testMultiMasterReplication() {
        System.out.println("\n=== 多主复制测试 ===");
        MultiMasterReplication multiMaster = new MultiMasterReplication();
        
        // 添加主节点
        multiMaster.addMaster("master1", "192.168.1.1");
        multiMaster.addMaster("master2", "192.168.1.2");
        multiMaster.addMaster("master3", "192.168.1.3");
        
        // 测试并发写入
        multiMaster.put("key1", "value1_from_master1", "master1");
        multiMaster.put("key1", "value1_from_master2", "master2"); // 可能产生冲突
        
        try {
            Thread.sleep(300); // 等待复制和冲突解决
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        multiMaster.get("key1", "master1");
        multiMaster.get("key1", "master2");
        
        multiMaster.printReplicationStatus();
        multiMaster.shutdown();
    }
    
    public static void testQuorumConsistency() {
        System.out.println("\n=== Quorum一致性测试 ===");
        QuorumConsistency quorum = new QuorumConsistency(5, 3);
        
        quorum.printQuorumInfo();
        
        // 测试写入
        boolean writeSuccess = quorum.put("key1", "value1");
        System.out.println("写入结果: " + writeSuccess);
        
        // 测试读取
        String value = quorum.get("key1");
        System.out.println("读取结果: " + value);
        
        quorum.shutdown();
    }
    
    public static void main(String[] args) {
        testRangeSharding();
        testHashSharding();
        testMasterSlaveReplication();
        testMultiMasterReplication();
        testQuorumConsistency();
    }
}
```

## 性能对比

### 分片算法性能对比

```java
public class ShardingPerformanceTest {
    
    public static void compareShardingPerformance() {
        System.out.println("\n=== 分片算法性能对比 ===");
        
        int dataCount = 10000;
        String[] keys = new String[dataCount];
        for (int i = 0; i < dataCount; i++) {
            keys[i] = "key" + i;
        }
        
        // 测试范围分片
        long startTime = System.currentTimeMillis();
        RangeSharding rangeSharding = new RangeSharding();
        for (String key : keys) {
            rangeSharding.put(key, "value_" + key);
        }
        long rangeShardingTime = System.currentTimeMillis() - startTime;
        
        // 测试哈希分片
        startTime = System.currentTimeMillis();
        HashSharding hashSharding = new HashSharding(3);
        for (String key : keys) {
            hashSharding.put(key, "value_" + key);
        }
        long hashShardingTime = System.currentTimeMillis() - startTime;
        
        System.out.println("数据量: " + dataCount);
        System.out.println("范围分片耗时: " + rangeShardingTime + "ms");
        System.out.println("哈希分片耗时: " + hashShardingTime + "ms");
        
        // 测试查询性能
        String[] queryKeys = new String[1000];
        for (int i = 0; i < 1000; i++) {
            queryKeys[i] = "key" + (int)(Math.random() * dataCount);
        }
        
        startTime = System.currentTimeMillis();
        for (String key : queryKeys) {
            rangeSharding.get(key);
        }
        long rangeQueryTime = System.currentTimeMillis() - startTime;
        
        startTime = System.currentTimeMillis();
        for (String key : queryKeys) {
            hashSharding.get(key);
        }
        long hashQueryTime = System.currentTimeMillis() - startTime;
        
        System.out.println("查询量: " + queryKeys.length);
        System.out.println("范围分片查询耗时: " + rangeQueryTime + "ms");
        System.out.println("哈希分片查询耗时: " + hashQueryTime + "ms");
    }
}
```

### 复制算法性能对比

```java
public class ReplicationPerformanceTest {
    
    public static void compareReplicationPerformance() {
        System.out.println("\n=== 复制算法性能对比 ===");
        
        int operationCount = 1000;
        
        // 测试主从复制
        MasterSlaveReplication masterSlave = new MasterSlaveReplication("master");
        masterSlave.addSlave("slave1", "192.168.1.2");
        masterSlave.addSlave("slave2", "192.168.1.3");
        
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < operationCount; i++) {
            masterSlave.put("key" + i, "value" + i);
        }
        long masterSlaveTime = System.currentTimeMillis() - startTime;
        
        // 测试多主复制
        MultiMasterReplication multiMaster = new MultiMasterReplication();
        multiMaster.addMaster("master1", "192.168.1.1");
        multiMaster.addMaster("master2", "192.168.1.2");
        
        startTime = System.currentTimeMillis();
        for (int i = 0; i < operationCount; i++) {
            String masterId = (i % 2 == 0) ? "master1" : "master2";
            multiMaster.put("key" + i, "value" + i, masterId);
        }
        long multiMasterTime = System.currentTimeMillis() - startTime;
        
        // 测试Quorum一致性
        QuorumConsistency quorum = new QuorumConsistency(5, 3);
        
        startTime = System.currentTimeMillis();
        int successCount = 0;
        for (int i = 0; i < operationCount; i++) {
            if (quorum.put("key" + i, "value" + i)) {
                successCount++;
            }
        }
        long quorumTime = System.currentTimeMillis() - startTime;
        
        System.out.println("操作数量: " + operationCount);
        System.out.println("主从复制耗时: " + masterSlaveTime + "ms");
        System.out.println("多主复制耗时: " + multiMasterTime + "ms");
        System.out.println("Quorum一致性耗时: " + quorumTime + "ms (成功: " + successCount + ")");
        
        masterSlave.shutdown();
        multiMaster.shutdown();
        quorum.shutdown();
    }
}
```

## 算法选择指南

### 分片算法选择

| 算法类型 | 优势 | 劣势 | 适用场景 |
|---------|------|------|----------|
| 范围分片 | 支持范围查询，数据局部性好 | 可能数据倾斜，热点问题 | 有序数据，范围查询频繁 |
| 哈希分片 | 数据分布均匀，简单高效 | 不支持范围查询，扩容复杂 | 随机访问，负载均衡要求高 |
| 一致性哈希 | 扩容时数据迁移少 | 实现复杂，可能负载不均 | 动态扩容，节点变化频繁 |

### 复制算法选择

| 算法类型 | 一致性 | 可用性 | 性能 | 复杂度 | 适用场景 |
|---------|--------|--------|------|--------|----------|
| 主从复制 | 强一致性 | 中等 | 高 | 低 | 读多写少，一致性要求高 |
| 多主复制 | 最终一致性 | 高 | 高 | 高 | 写入频繁，地理分布 |
| Quorum | 可调一致性 | 高 | 中等 | 中等 | 平衡一致性和可用性 |

## 最佳实践

### 1. 分片策略

```java
public class ShardingBestPractices {
    
    // 动态分片调整
    public static class AdaptiveSharding {
        private final Map<String, ShardMetrics> shardMetrics;
        private final double loadThreshold = 0.8;
        
        public AdaptiveSharding() {
            this.shardMetrics = new ConcurrentHashMap<>();
        }
        
        public void monitorAndRebalance() {
            for (Map.Entry<String, ShardMetrics> entry : shardMetrics.entrySet()) {
                ShardMetrics metrics = entry.getValue();
                if (metrics.getLoadFactor() > loadThreshold) {
                    System.out.println("分片 " + entry.getKey() + " 负载过高，触发重平衡");
                    rebalanceShard(entry.getKey());
                }
            }
        }
        
        private void rebalanceShard(String shardId) {
            // 实现分片重平衡逻辑
            System.out.println("重平衡分片: " + shardId);
        }
    }
    
    // 分片监控指标
    public static class ShardMetrics {
        private final AtomicLong requestCount = new AtomicLong();
        private final AtomicLong dataSize = new AtomicLong();
        private final AtomicLong responseTime = new AtomicLong();
        
        public double getLoadFactor() {
            // 综合考虑请求量、数据大小、响应时间
            return (requestCount.get() * 0.4 + dataSize.get() * 0.3 + responseTime.get() * 0.3) / 1000.0;
        }
        
        public void recordRequest(long responseTimeMs) {
            requestCount.incrementAndGet();
            responseTime.addAndGet(responseTimeMs);
        }
        
        public void recordDataSize(long size) {
            dataSize.set(size);
        }
    }
}
```

### 2. 复制一致性保证

```java
public class ConsistencyBestPractices {
    
    // 读写分离优化
    public static class ReadWriteSeparation {
        private final StorageNode master;
        private final List<StorageNode> readReplicas;
        private final AtomicInteger readIndex = new AtomicInteger(0);
        
        public ReadWriteSeparation(StorageNode master, List<StorageNode> readReplicas) {
            this.master = master;
            this.readReplicas = readReplicas;
        }
        
        public void write(String key, String value) {
            master.store(key, value);
            // 异步复制到读副本
            CompletableFuture.runAsync(() -> {
                for (StorageNode replica : readReplicas) {
                    replica.store(key, value);
                }
            });
        }
        
        public String read(String key) {
            // 负载均衡读取
            StorageNode replica = readReplicas.get(readIndex.getAndIncrement() % readReplicas.size());
            return replica.retrieve(key);
        }
        
        public String strongRead(String key) {
            // 强一致性读取，从主节点读
            return master.retrieve(key);
        }
    }
    
    // 数据版本控制
    public static class VersionControl {
        private final Map<String, Long> versions = new ConcurrentHashMap<>();
        
        public boolean putWithVersion(String key, String value, long expectedVersion) {
            Long currentVersion = versions.get(key);
            if (currentVersion == null) {
                currentVersion = 0L;
            }
            
            if (currentVersion.equals(expectedVersion)) {
                versions.put(key, currentVersion + 1);
                // 存储数据
                return true;
            }
            return false; // 版本冲突
        }
        
        public long getVersion(String key) {
            return versions.getOrDefault(key, 0L);
        }
    }
}
```

### 3. 监控和运维

```java
public class MonitoringBestPractices {
    
    // 存储系统监控
    public static class StorageMonitor {
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
        private final Map<String, NodeMetrics> nodeMetrics = new ConcurrentHashMap<>();
        
        public void startMonitoring() {
            // 定期收集指标
            scheduler.scheduleAtFixedRate(this::collectMetrics, 0, 30, TimeUnit.SECONDS);
            
            // 定期检查健康状态
            scheduler.scheduleAtFixedRate(this::healthCheck, 0, 10, TimeUnit.SECONDS);
        }
        
        private void collectMetrics() {
            for (Map.Entry<String, NodeMetrics> entry : nodeMetrics.entrySet()) {
                NodeMetrics metrics = entry.getValue();
                System.out.println("节点 " + entry.getKey() + " 指标: " + metrics.getSummary());
            }
        }
        
        private void healthCheck() {
            for (Map.Entry<String, NodeMetrics> entry : nodeMetrics.entrySet()) {
                NodeMetrics metrics = entry.getValue();
                if (metrics.getErrorRate() > 0.05) { // 错误率超过5%
                    System.err.println("警告: 节点 " + entry.getKey() + " 错误率过高");
                }
            }
        }
        
        public void shutdown() {
            scheduler.shutdown();
        }
    }
    
    public static class NodeMetrics {
        private final AtomicLong totalRequests = new AtomicLong();
        private final AtomicLong errorRequests = new AtomicLong();
        private final AtomicLong totalLatency = new AtomicLong();
        
        public void recordRequest(boolean success, long latencyMs) {
            totalRequests.incrementAndGet();
            if (!success) {
                errorRequests.incrementAndGet();
            }
            totalLatency.addAndGet(latencyMs);
        }
        
        public double getErrorRate() {
            long total = totalRequests.get();
            return total > 0 ? (double) errorRequests.get() / total : 0.0;
        }
        
        public double getAverageLatency() {
            long total = totalRequests.get();
            return total > 0 ? (double) totalLatency.get() / total : 0.0;
        }
        
        public String getSummary() {
            return String.format("请求: %d, 错误率: %.2f%%, 平均延迟: %.2fms", 
                    totalRequests.get(), getErrorRate() * 100, getAverageLatency());
        }
    }
}
```

## 面试要点

### 高频问题

1. **分片策略对比**
   - 范围分片 vs 哈希分片的优缺点
   - 一致性哈希的原理和优势
   - 如何处理数据倾斜问题

2. **数据复制机制**
   - 主从复制的实现原理
   - 多主复制的冲突解决
   - Quorum机制的工作原理

3. **一致性保证**
   - CAP定理的理解和应用
   - 强一致性 vs 最终一致性
   - 如何在可用性和一致性间平衡

### 深入问题

1. **如何设计一个支持PB级数据的分布式存储系统？**
   - 分层存储架构
   - 数据分片和路由策略
   - 元数据管理
   - 故障恢复机制

2. **如何处理分布式存储中的热点数据？**
   - 热点检测机制
   - 动态负载均衡
   - 缓存策略
   - 分片分裂

3. **分布式存储的数据一致性如何保证？**
   - 向量时钟的使用
   - 冲突检测和解决
   - 读写Quorum的配置
   - 最终一致性的实现

### 实践经验

1. **性能优化经验**
   - 批量操作优化
   - 连接池管理
   - 异步复制策略
   - 压缩和编码优化

2. **运维监控经验**
   - 关键指标监控
   - 故障检测和恢复
   - 容量规划
   - 数据备份策略

3. **故障处理经验**
   - 节点故障处理
   - 网络分区处理
   - 数据不一致修复
   - 性能瓶颈定位

## 总结

分布式存储算法是构建大规模分布式系统的核心技术，主要包括：

1. **数据分片算法**：范围分片、哈希分片、一致性哈希
2. **数据复制算法**：主从复制、多主复制、Quorum一致性
3. **一致性协议**：强一致性、最终一致性、因果一致性
4. **性能优化**：负载均衡、缓存策略、批量操作
5. **监控运维**：指标收集、故障检测、容量规划

在实际应用中，需要根据业务需求在一致性、可用性、性能之间做出权衡，选择合适的算法组合。同时，要重视监控和运维，确保系统的稳定性和可靠性。
```

### 多主复制(Multi-Master Replication)

```java
public class MultiMasterReplication {
    private final List<StorageNode> masters;
    private final Map<String, VectorClock> vectorClocks;
    private final ExecutorService executorService;
    
    public MultiMasterReplication() {
        this.masters = new ArrayList<>();
        this.vectorClocks = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(20);
    }
    
    public void addMaster(String masterId, String address) {
        StorageNode master = new StorageNode(masterId, address);
        masters.add(master);
        System.out.println("添加主节点: " + masterId);
    }
    
    public void put(String key, String value, String masterId) {
        StorageNode master = findMaster(masterId);
        if (master == null) {
            throw new IllegalArgumentException("主节点不存在: " + masterId);
        }
        
        // 更新向量时钟
        VectorClock clock = vectorClocks.computeIfAbsent(key, k -> new VectorClock(masters.size()));
        clock.increment(getMasterIndex(masterId));
        
        // 存储数据和时钟
        VersionedValue versionedValue = new VersionedValue(value, clock.copy());
        master.storeVersioned(key, versionedValue);
        
        System.out.println("主节点 " + masterId + " 写入: " + key + " = " + value);
        
        // 复制到其他主节点
        replicateToOtherMasters(key, versionedValue, masterId);
    }
    
    private void replicateToOtherMasters(String key, VersionedValue value, String excludeMasterId) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (StorageNode master : masters) {
            if (!master.getNodeId().equals(excludeMasterId)) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(20 + (int)(Math.random() * 80));
                        
                        // 检查冲突并合并
                        VersionedValue existing = master.retrieveVersioned(key);
                        if (existing == null || value.getClock().isAfter(existing.getClock())) {
                            master.storeVersioned(key, value);
                            System.out.println("复制到主节点 " + master.getNodeId() + ": " + key);
                        } else if (value.getClock().isConcurrent(existing.getClock())) {
                            // 处理冲突
                            handleConflict(master, key, existing, value);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }, executorService);
                
                futures.add(future);
            }
        }
    }
    
    private void handleConflict(StorageNode master, String key, VersionedValue existing, VersionedValue incoming) {
        // 简单的冲突解决策略：保留两个版本
        System.out.println("检测到冲突: " + key + " 在节点 " + master.getNodeId());
        
        // 创建合并后的向量时钟
        VectorClock mergedClock = existing.getClock().merge(incoming.getClock());
        
        // 简单合并策略：连接两个值
        String mergedValue = existing.getValue() + "|" + incoming.getValue();
        VersionedValue mergedVersionedValue = new VersionedValue(mergedValue, mergedClock);
        
        master.storeVersioned(key, mergedVersionedValue);
        System.out.println("冲突已解决，合并值: " + mergedValue);
    }
    
    public String get(String key, String masterId) {
        StorageNode master = findMaster(masterId);
        if (master == null) {
            throw new IllegalArgumentException("主节点不存在: " + masterId);
        }
        
        VersionedValue versionedValue = master.retrieveVersioned(key);
        String value = versionedValue != null ? versionedValue.getValue() : null;
        System.out.println("从主节点 " + masterId + " 读取: " + key + " = " + value);
        return value;
    }
    
    private StorageNode findMaster(String masterId) {
        return masters.stream()
                .filter(master -> master.getNodeId().equals(masterId))
                .findFirst()
                .orElse(null);
    }
    
    private int getMasterIndex(String masterId) {
        for (int i = 0; i < masters.size(); i++) {
            if (masters.get(i).getNodeId().equals(masterId)) {
                return i;
            }
        }
        return -1;
    }
    
    public void printReplicationStatus() {
        System.out.println("\n=== 多主复制状态 ===");
        for (StorageNode master : masters) {
            System.out.println("主节点: " + master.getNodeId() + ", 数据量: " + master.getData().size());
        }
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}

// 向量时钟实现
class VectorClock {
    private final int[] clock;
    
    public VectorClock(int size) {
        this.clock = new int[size];
    }
    
    public VectorClock(int[] clock) {
        this.clock = clock.clone();
    }
    
    public void increment(int nodeIndex) {
        if (nodeIndex >= 0 && nodeIndex < clock.length) {
            clock[nodeIndex]++;
        }
    }
    
    public boolean isAfter(VectorClock other) {
        boolean hasGreater = false;
        for (int i = 0; i < clock.length; i++) {
            if (clock[i] < other.clock[i]) {
                return false;
            }
            if (clock[i] > other.clock[i]) {
                hasGreater = true;
            }
        }
        return hasGreater;
    }
    
    public boolean isConcurrent(VectorClock other) {
        return !isAfter(other) && !other.isAfter(this) && !equals(other);
    }
    
    public VectorClock merge(VectorClock other) {
        int[] merged = new int[clock.length];
        for (int i = 0; i < clock.length; i++) {
            merged[i] = Math.max(clock[i], other.clock[i]);
        }
        return new VectorClock(merged);
    }
    
    public VectorClock copy() {
        return new VectorClock(clock);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VectorClock that = (VectorClock) obj;
        return Arrays.equals(clock, that.clock);
    }
    
    @Override
    public String toString() {
        return Arrays.toString(clock);
    }
}

// 版本化值
class VersionedValue {
    private final String value;
    private final VectorClock clock;
    
    public VersionedValue(String value, VectorClock clock) {
        this.value = value;
        this.clock = clock;
    }
    
    public String getValue() { return value; }
    public VectorClock getClock() { return clock; }
}
```

### 扩展StorageNode以支持版本化存储

```java
// 扩展StorageNode类
class StorageNode {
    private final String nodeId;
    private final String address;
    private final Map<String, String> data;
    private final Map<String, VersionedValue> versionedData;
    
    public StorageNode(String nodeId, String address) {
        this.nodeId = nodeId;
        this.address = address;
        this.data = new ConcurrentHashMap<>();
        this.versionedData = new ConcurrentHashMap<>();
    }
    
    public void store(String key, String value) {
        data.put(key, value);
    }
    
    public String retrieve(String key) {
        return data.get(key);
    }
    
    public void storeVersioned(String key, VersionedValue value) {
        versionedData.put(key, value);
    }
    
    public VersionedValue retrieveVersioned(String key) {
        return versionedData.get(key);
    }
    
    public String getNodeId() { return nodeId; }
    public String getAddress() { return address; }
    public Map<String, String> getData() { return new HashMap<>(data); }
    public Map<String, VersionedValue> getVersionedData() { return new HashMap<>(versionedData); }
}
```

## 一致性协议

### Quorum一致性

```java
public class QuorumConsistency {
    private final List<StorageNode> nodes;
    private final int replicationFactor;
    private final int readQuorum;
    private final int writeQuorum;
    private final ExecutorService executorService;
    
    public QuorumConsistency(int nodeCount, int replicationFactor) {
        this.replicationFactor = replicationFactor;
        this.readQuorum = (replicationFactor / 2) + 1;
        this.writeQuorum = (replicationFactor / 2) + 1;
        this.nodes = new ArrayList<>();
        this.executorService = Executors.newFixedThreadPool(nodeCount);
        
        // 初始化节点
        for (int i = 0; i < nodeCount; i++) {
            nodes.add(new StorageNode("node" + i, "192.168.1." + (i + 1)));
        }
    }
    
    public boolean put(String key, String value) {
        // 选择复制节点
        List<StorageNode> replicaNodes = selectReplicaNodes(key);
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (StorageNode node : replicaNodes) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 模拟网络延迟和可能的失败
                    Thread.sleep(50 + (int)(Math.random() * 100));
                    
                    if (Math.random() < 0.1) { // 10%失败率
                        System.out.println("节点 " + node.getNodeId() + " 写入失败");
                        return false;
                    }
                    
                    node.store(key, value);
                    System.out.println("节点 " + node.getNodeId() + " 写入成功: " + key);
                    return true;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // 等待写入Quorum
        int successCount = 0;
        try {
            for (CompletableFuture<Boolean> future : futures) {
                if (future.get(2, TimeUnit.SECONDS)) {
                    successCount++;
                    if (successCount >= writeQuorum) {
                        System.out.println("写入Quorum达成: " + key + " (" + successCount + "/" + replicationFactor + ")");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("写入超时或异常: " + e.getMessage());
        }
        
        System.err.println("写入Quorum未达成: " + key + " (" + successCount + "/" + writeQuorum + ")");
        return false;
    }
    
    public String get(String key) {
        // 选择读取节点
        List<StorageNode> replicaNodes = selectReplicaNodes(key);
        
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        for (StorageNode node : replicaNodes) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(30 + (int)(Math.random() * 70));
                    
                    if (Math.random() < 0.1) { // 10%失败率
                        System.out.println("节点 " + node.getNodeId() + " 读取失败");
                        return null;
                    }
                    
                    String value = node.retrieve(key);
                    System.out.println("节点 " + node.getNodeId() + " 读取: " + key + " = " + value);
                    return value;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // 等待读取Quorum
        Map<String, Integer> valueCount = new HashMap<>();
        int responseCount = 0;
        
        try {
            for (CompletableFuture<String> future : futures) {
                String value = future.get(2, TimeUnit.SECONDS);
                if (value != null) {
                    valueCount.put(value, valueCount.getOrDefault(value, 0) + 1);
                    responseCount++;
                    
                    if (responseCount >= readQuorum) {
                        // 返回最多的值
                        String mostCommonValue = valueCount.entrySet().stream()
                                .max(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey)
                                .orElse(null);
                        
                        System.out.println("读取Quorum达成: " + key + " = " + mostCommonValue + 
                                         " (" + responseCount + "/" + readQuorum + ")");
                        return mostCommonValue;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("读取超时或异常: " + e.getMessage());
        }
        
        System.err.println("读取Quorum未达成: " + key + " (" + responseCount + "/" + readQuorum + ")");
        return null;
    }
    
    private List<StorageNode> selectReplicaNodes(String key) {
        // 使用一致性哈希选择节点
        int startIndex = Math.abs(key.hashCode()) % nodes.size();
        List<StorageNode> selected = new ArrayList<>();
        
        for (int i = 0; i < replicationFactor && i < nodes.size(); i++) {
            int index = (startIndex + i) % nodes.size();
            selected.add(nodes.get(index));
        }
        
        return selected;
    }
    
    public void printQuorumInfo() {
        System.out.println("\n=== Quorum配置 ===");
        System.out.println("节点总数: " + nodes.size());
        System.out.println("复制因子: " + replicationFactor);
        System.out.println("读Quorum: " + readQuorum);
        System.out.println("写Quorum: " + writeQuorum);
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}
```