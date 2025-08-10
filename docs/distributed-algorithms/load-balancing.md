# 负载均衡算法详解

## 目录
- [负载均衡概述](#负载均衡概述)
- [轮询算法](#轮询算法)
- [加权轮询算法](#加权轮询算法)
- [随机算法](#随机算法)
- [加权随机算法](#加权随机算法)
- [最少连接算法](#最少连接算法)
- [一致性哈希算法](#一致性哈希算法)
- [IP哈希算法](#ip哈希算法)
- [响应时间算法](#响应时间算法)
- [健康检查机制](#健康检查机制)
- [动态负载均衡](#动态负载均衡)
- [实际应用场景](#实际应用场景)
- [性能对比](#性能对比)
- [面试要点](#面试要点)

## 负载均衡概述

### 什么是负载均衡
负载均衡是一种在多个服务器之间分配网络流量的技术，目的是优化资源使用、最大化吞吐量、最小化响应时间，并避免任何单一资源的过载。

### 负载均衡的作用

```java
public class LoadBalancerDemo {
    public static void main(String[] args) {
        // 模拟没有负载均衡的情况
        System.out.println("=== 没有负载均衡 ===");
        simulateWithoutLoadBalancer();
        
        System.out.println("\n=== 使用负载均衡 ===");
        simulateWithLoadBalancer();
    }
    
    private static void simulateWithoutLoadBalancer() {
        // 所有请求都发送到同一台服务器
        Server server = new Server("Server-1", 100);
        
        for (int i = 0; i < 10; i++) {
            Request request = new Request("Request-" + i);
            server.handleRequest(request);
        }
        
        System.out.println("Server-1 负载: " + server.getCurrentLoad());
    }
    
    private static void simulateWithLoadBalancer() {
        // 使用负载均衡器分发请求
        List<Server> servers = Arrays.asList(
            new Server("Server-1", 100),
            new Server("Server-2", 100),
            new Server("Server-3", 100)
        );
        
        LoadBalancer loadBalancer = new RoundRobinLoadBalancer(servers);
        
        for (int i = 0; i < 10; i++) {
            Request request = new Request("Request-" + i);
            Server server = loadBalancer.selectServer(request);
            server.handleRequest(request);
        }
        
        // 显示各服务器负载
        for (Server server : servers) {
            System.out.println(server.getName() + " 负载: " + server.getCurrentLoad());
        }
    }
}

// 服务器类
class Server {
    private final String name;
    private final int capacity;
    private int currentLoad;
    private long totalResponseTime;
    private int requestCount;
    private boolean healthy = true;
    
    public Server(String name, int capacity) {
        this.name = name;
        this.capacity = capacity;
        this.currentLoad = 0;
    }
    
    public void handleRequest(Request request) {
        if (!healthy) {
            throw new RuntimeException("Server is not healthy");
        }
        
        currentLoad++;
        requestCount++;
        
        // 模拟请求处理时间
        long startTime = System.currentTimeMillis();
        try {
            Thread.sleep(10 + (int)(Math.random() * 20)); // 10-30ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        long responseTime = System.currentTimeMillis() - startTime;
        totalResponseTime += responseTime;
        
        System.out.println(name + " 处理 " + request.getId() + ", 响应时间: " + responseTime + "ms");
        
        // 模拟请求完成后负载减少
        currentLoad--;
    }
    
    // Getters
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public int getCurrentLoad() { return currentLoad; }
    public double getAverageResponseTime() {
        return requestCount > 0 ? (double) totalResponseTime / requestCount : 0;
    }
    public boolean isHealthy() { return healthy; }
    public void setHealthy(boolean healthy) { this.healthy = healthy; }
    public int getRequestCount() { return requestCount; }
}

// 请求类
class Request {
    private final String id;
    private final String clientIp;
    private final long timestamp;
    
    public Request(String id) {
        this.id = id;
        this.clientIp = generateRandomIp();
        this.timestamp = System.currentTimeMillis();
    }
    
    private String generateRandomIp() {
        return (int)(Math.random() * 256) + "." +
               (int)(Math.random() * 256) + "." +
               (int)(Math.random() * 256) + "." +
               (int)(Math.random() * 256);
    }
    
    public String getId() { return id; }
    public String getClientIp() { return clientIp; }
    public long getTimestamp() { return timestamp; }
}
```

### 负载均衡器接口

```java
public interface LoadBalancer {
    /**
     * 选择一个服务器处理请求
     */
    Server selectServer(Request request);
    
    /**
     * 添加服务器
     */
    void addServer(Server server);
    
    /**
     * 移除服务器
     */
    void removeServer(Server server);
    
    /**
     * 获取所有健康的服务器
     */
    List<Server> getHealthyServers();
    
    /**
     * 更新服务器状态
     */
    void updateServerStatus(Server server, boolean healthy);
}
```

## 轮询算法

### 基本轮询实现

```java
public class RoundRobinLoadBalancer implements LoadBalancer {
    private final List<Server> servers;
    private final AtomicInteger currentIndex;
    
    public RoundRobinLoadBalancer(List<Server> servers) {
        this.servers = new CopyOnWriteArrayList<>(servers);
        this.currentIndex = new AtomicInteger(0);
    }
    
    @Override
    public Server selectServer(Request request) {
        List<Server> healthyServers = getHealthyServers();
        if (healthyServers.isEmpty()) {
            throw new RuntimeException("No healthy servers available");
        }
        
        int index = currentIndex.getAndIncrement() % healthyServers.size();
        return healthyServers.get(index);
    }
    
    @Override
    public void addServer(Server server) {
        servers.add(server);
    }
    
    @Override
    public void removeServer(Server server) {
        servers.remove(server);
    }
    
    @Override
    public List<Server> getHealthyServers() {
        return servers.stream()
                .filter(Server::isHealthy)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateServerStatus(Server server, boolean healthy) {
        server.setHealthy(healthy);
    }
}
```

### 轮询算法测试

```java
public class RoundRobinTest {
    public static void main(String[] args) {
        List<Server> servers = Arrays.asList(
            new Server("Server-A", 100),
            new Server("Server-B", 100),
            new Server("Server-C", 100)
        );
        
        LoadBalancer loadBalancer = new RoundRobinLoadBalancer(servers);
        
        System.out.println("=== 轮询算法测试 ===");
        for (int i = 0; i < 9; i++) {
            Request request = new Request("Request-" + i);
            Server server = loadBalancer.selectServer(request);
            System.out.println("请求 " + request.getId() + " -> " + server.getName());
        }
        
        // 测试服务器下线情况
        System.out.println("\n=== 服务器B下线后 ===");
        loadBalancer.updateServerStatus(servers.get(1), false);
        
        for (int i = 9; i < 15; i++) {
            Request request = new Request("Request-" + i);
            Server server = loadBalancer.selectServer(request);
            System.out.println("请求 " + request.getId() + " -> " + server.getName());
        }
    }
}
```

## 加权轮询算法

### 加权轮询实现

```java
public class WeightedRoundRobinLoadBalancer implements LoadBalancer {
    private final List<WeightedServer> servers;
    private final AtomicInteger currentIndex;
    
    public WeightedRoundRobinLoadBalancer(List<WeightedServer> servers) {
        this.servers = new CopyOnWriteArrayList<>(servers);
        this.currentIndex = new AtomicInteger(0);
    }
    
    @Override
    public Server selectServer(Request request) {
        List<WeightedServer> healthyServers = getHealthyWeightedServers();
        if (healthyServers.isEmpty()) {
            throw new RuntimeException("No healthy servers available");
        }
        
        // 计算总权重
        int totalWeight = healthyServers.stream()
                .mapToInt(WeightedServer::getWeight)
                .sum();
        
        // 使用权重进行选择
        int index = currentIndex.getAndIncrement() % totalWeight;
        int currentWeight = 0;
        
        for (WeightedServer server : healthyServers) {
            currentWeight += server.getWeight();
            if (index < currentWeight) {
                return server.getServer();
            }
        }
        
        // 默认返回第一个服务器
        return healthyServers.get(0).getServer();
    }
    
    private List<WeightedServer> getHealthyWeightedServers() {
        return servers.stream()
                .filter(ws -> ws.getServer().isHealthy())
                .collect(Collectors.toList());
    }
    
    @Override
    public void addServer(Server server) {
        servers.add(new WeightedServer(server, 1)); // 默认权重为1
    }
    
    public void addServer(Server server, int weight) {
        servers.add(new WeightedServer(server, weight));
    }
    
    @Override
    public void removeServer(Server server) {
        servers.removeIf(ws -> ws.getServer().equals(server));
    }
    
    @Override
    public List<Server> getHealthyServers() {
        return getHealthyWeightedServers().stream()
                .map(WeightedServer::getServer)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateServerStatus(Server server, boolean healthy) {
        server.setHealthy(healthy);
    }
    
    // 加权服务器类
    public static class WeightedServer {
        private final Server server;
        private final int weight;
        
        public WeightedServer(Server server, int weight) {
            this.server = server;
            this.weight = weight;
        }
        
        public Server getServer() { return server; }
        public int getWeight() { return weight; }
    }
}
```

## 一致性哈希算法

```java
public class ConsistentHashLoadBalancer implements LoadBalancer {
    private final TreeMap<Long, Server> ring = new TreeMap<>();
    private final List<Server> servers = new CopyOnWriteArrayList<>();
    private final int virtualNodes = 150; // 虚拟节点数量
    
    public ConsistentHashLoadBalancer(List<Server> servers) {
        for (Server server : servers) {
            addServer(server);
        }
    }
    
    @Override
    public Server selectServer(Request request) {
        if (ring.isEmpty()) {
            throw new RuntimeException("No servers available");
        }
        
        // 使用客户端IP作为哈希键
        String key = request.getClientIp();
        long hash = hash(key);
        
        // 在哈希环上找到第一个大于等于该哈希值的服务器
        Map.Entry<Long, Server> entry = ring.ceilingEntry(hash);
        if (entry == null) {
            // 如果没有找到，则使用环上的第一个服务器
            entry = ring.firstEntry();
        }
        
        Server server = entry.getValue();
        
        // 如果选中的服务器不健康，则顺时针查找下一个健康的服务器
        if (!server.isHealthy()) {
            return findNextHealthyServer(hash);
        }
        
        return server;
    }
    
    private Server findNextHealthyServer(long hash) {
        Map.Entry<Long, Server> entry = ring.ceilingEntry(hash);
        
        // 遍历整个环查找健康的服务器
        for (int i = 0; i < ring.size(); i++) {
            if (entry == null) {
                entry = ring.firstEntry();
            }
            
            if (entry.getValue().isHealthy()) {
                return entry.getValue();
            }
            
            entry = ring.higherEntry(entry.getKey());
        }
        
        throw new RuntimeException("No healthy servers available");
    }
    
    @Override
    public void addServer(Server server) {
        servers.add(server);
        
        // 为每个服务器添加虚拟节点
        for (int i = 0; i < virtualNodes; i++) {
            String virtualNodeKey = server.getName() + "#" + i;
            long hash = hash(virtualNodeKey);
            ring.put(hash, server);
        }
    }
    
    @Override
    public void removeServer(Server server) {
        servers.remove(server);
        
        // 移除服务器的所有虚拟节点
        for (int i = 0; i < virtualNodes; i++) {
            String virtualNodeKey = server.getName() + "#" + i;
            long hash = hash(virtualNodeKey);
            ring.remove(hash);
        }
    }
    
    @Override
    public List<Server> getHealthyServers() {
        return servers.stream()
                .filter(Server::isHealthy)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateServerStatus(Server server, boolean healthy) {
        server.setHealthy(healthy);
    }
    
    // 哈希函数
    private long hash(String key) {
        // 使用FNV-1a哈希算法
        long hash = 2166136261L;
        for (byte b : key.getBytes()) {
            hash ^= b;
            hash *= 16777619L;
        }
        return Math.abs(hash);
    }
    
    // 获取哈希环状态（用于调试）
    public void printRingStatus() {
        System.out.println("\n=== 哈希环状态 ===");
        for (Map.Entry<Long, Server> entry : ring.entrySet()) {
            System.out.println("Hash: " + entry.getKey() + " -> " + entry.getValue().getName());
        }
    }
}
```

## IP哈希算法

```java
public class IpHashLoadBalancer implements LoadBalancer {
    private final List<Server> servers;
    
    public IpHashLoadBalancer(List<Server> servers) {
        this.servers = new CopyOnWriteArrayList<>(servers);
    }
    
    @Override
    public Server selectServer(Request request) {
        List<Server> healthyServers = getHealthyServers();
        if (healthyServers.isEmpty()) {
            throw new RuntimeException("No healthy servers available");
        }
        
        // 使用客户端IP的哈希值选择服务器
        String clientIp = request.getClientIp();
        int hash = Math.abs(clientIp.hashCode());
        int index = hash % healthyServers.size();
        
        return healthyServers.get(index);
    }
    
    @Override
    public void addServer(Server server) {
        servers.add(server);
    }
    
    @Override
    public void removeServer(Server server) {
        servers.remove(server);
    }
    
    @Override
    public List<Server> getHealthyServers() {
        return servers.stream()
                .filter(Server::isHealthy)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateServerStatus(Server server, boolean healthy) {
        server.setHealthy(healthy);
    }
}
```

## 响应时间算法

```java
public class ResponseTimeLoadBalancer implements LoadBalancer {
    private final List<Server> servers;
    
    public ResponseTimeLoadBalancer(List<Server> servers) {
        this.servers = new CopyOnWriteArrayList<>(servers);
    }
    
    @Override
    public Server selectServer(Request request) {
        List<Server> healthyServers = getHealthyServers();
        if (healthyServers.isEmpty()) {
            throw new RuntimeException("No healthy servers available");
        }
        
        // 选择平均响应时间最短的服务器
        return healthyServers.stream()
                .min(Comparator.comparingDouble(Server::getAverageResponseTime))
                .orElse(healthyServers.get(0));
    }
    
    @Override
    public void addServer(Server server) {
        servers.add(server);
    }
    
    @Override
    public void removeServer(Server server) {
        servers.remove(server);
    }
    
    @Override
    public List<Server> getHealthyServers() {
        return servers.stream()
                .filter(Server::isHealthy)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateServerStatus(Server server, boolean healthy) {
        server.setHealthy(healthy);
    }
}
```

### 加权响应时间算法

```java
public class WeightedResponseTimeLoadBalancer implements LoadBalancer {
    private final List<WeightedServer> servers;
    private final Map<Server, Double> responseTimeWeights = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    public WeightedResponseTimeLoadBalancer(List<WeightedServer> servers) {
        this.servers = new CopyOnWriteArrayList<>(servers);
        
        // 定期更新响应时间权重
        scheduler.scheduleAtFixedRate(this::updateResponseTimeWeights, 0, 30, TimeUnit.SECONDS);
    }
    
    @Override
    public Server selectServer(Request request) {
        List<WeightedServer> healthyServers = getHealthyWeightedServers();
        if (healthyServers.isEmpty()) {
            throw new RuntimeException("No healthy servers available");
        }
        
        // 计算总权重（考虑响应时间）
        double totalWeight = 0;
        for (WeightedServer ws : healthyServers) {
            double weight = calculateEffectiveWeight(ws);
            totalWeight += weight;
        }
        
        // 随机选择
        double random = Math.random() * totalWeight;
        double currentWeight = 0;
        
        for (WeightedServer ws : healthyServers) {
            currentWeight += calculateEffectiveWeight(ws);
            if (random <= currentWeight) {
                return ws.getServer();
            }
        }
        
        return healthyServers.get(0).getServer();
    }
    
    private double calculateEffectiveWeight(WeightedServer weightedServer) {
        Server server = weightedServer.getServer();
        double baseWeight = weightedServer.getWeight();
        
        // 获取响应时间权重（响应时间越短，权重越高）
        double responseTimeWeight = responseTimeWeights.getOrDefault(server, 1.0);
        
        return baseWeight * responseTimeWeight;
    }
    
    private void updateResponseTimeWeights() {
        List<Server> healthyServers = getHealthyServers();
        if (healthyServers.isEmpty()) {
            return;
        }
        
        // 计算平均响应时间
        double avgResponseTime = healthyServers.stream()
                .mapToDouble(Server::getAverageResponseTime)
                .average()
                .orElse(1.0);
        
        // 更新每个服务器的响应时间权重
        for (Server server : healthyServers) {
            double serverResponseTime = server.getAverageResponseTime();
            if (serverResponseTime > 0) {
                // 响应时间越短，权重越高
                double weight = avgResponseTime / serverResponseTime;
                responseTimeWeights.put(server, Math.max(0.1, Math.min(10.0, weight)));
            } else {
                responseTimeWeights.put(server, 1.0);
            }
        }
    }
    
    private List<WeightedServer> getHealthyWeightedServers() {
        return servers.stream()
                .filter(ws -> ws.getServer().isHealthy())
                .collect(Collectors.toList());
    }
    
    @Override
    public void addServer(Server server) {
        servers.add(new WeightedServer(server, 1));
    }
    
    public void addServer(Server server, int weight) {
        servers.add(new WeightedServer(server, weight));
    }
    
    @Override
    public void removeServer(Server server) {
        servers.removeIf(ws -> ws.getServer().equals(server));
        responseTimeWeights.remove(server);
    }
    
    @Override
    public List<Server> getHealthyServers() {
        return getHealthyWeightedServers().stream()
                .map(WeightedServer::getServer)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateServerStatus(Server server, boolean healthy) {
        server.setHealthy(healthy);
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
    
    // 使用之前定义的WeightedServer类
    public static class WeightedServer {
        private final Server server;
        private final int weight;
        
        public WeightedServer(Server server, int weight) {
            this.server = server;
            this.weight = weight;
        }
        
        public Server getServer() { return server; }
        public int getWeight() { return weight; }
    }
}
```

## 健康检查机制

```java
public class HealthChecker {
    private final List<Server> servers;
    private final ScheduledExecutorService scheduler;
    private final LoadBalancer loadBalancer;
    private final int checkInterval; // 检查间隔（秒）
    
    public HealthChecker(List<Server> servers, LoadBalancer loadBalancer, int checkInterval) {
        this.servers = servers;
        this.loadBalancer = loadBalancer;
        this.checkInterval = checkInterval;
        this.scheduler = Executors.newScheduledThreadPool(2);
    }
    
    public void startHealthCheck() {
        scheduler.scheduleAtFixedRate(this::performHealthCheck, 0, checkInterval, TimeUnit.SECONDS);
    }
    
    private void performHealthCheck() {
        for (Server server : servers) {
            CompletableFuture.supplyAsync(() -> checkServerHealth(server), scheduler)
                    .thenAccept(healthy -> {
                        boolean wasHealthy = server.isHealthy();
                        if (wasHealthy != healthy) {
                            loadBalancer.updateServerStatus(server, healthy);
                            System.out.println("服务器 " + server.getName() + " 状态变更: " + 
                                             (healthy ? "健康" : "不健康"));
                        }
                    })
                    .exceptionally(throwable -> {
                        System.err.println("健康检查异常: " + throwable.getMessage());
                        loadBalancer.updateServerStatus(server, false);
                        return null;
                    });
        }
    }
    
    private boolean checkServerHealth(Server server) {
        try {
            // 模拟健康检查（实际应用中可能是HTTP请求、TCP连接等）
            // 这里简单模拟：90%概率健康
            Thread.sleep(100); // 模拟检查耗时
            return Math.random() > 0.1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

### 高级健康检查

```java
public class AdvancedHealthChecker {
    private final Map<Server, HealthStatus> serverHealthStatus = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler;
    private final LoadBalancer loadBalancer;
    private final HealthCheckConfig config;
    
    public AdvancedHealthChecker(LoadBalancer loadBalancer, HealthCheckConfig config) {
        this.loadBalancer = loadBalancer;
        this.config = config;
        this.scheduler = Executors.newScheduledThreadPool(config.getThreadPoolSize());
    }
    
    public void addServer(Server server) {
        serverHealthStatus.put(server, new HealthStatus());
    }
    
    public void removeServer(Server server) {
        serverHealthStatus.remove(server);
    }
    
    public void startHealthCheck() {
        scheduler.scheduleAtFixedRate(this::performHealthCheck, 0, 
                                    config.getCheckInterval(), TimeUnit.SECONDS);
    }
    
    private void performHealthCheck() {
        for (Map.Entry<Server, HealthStatus> entry : serverHealthStatus.entrySet()) {
            Server server = entry.getKey();
            HealthStatus status = entry.getValue();
            
            CompletableFuture.supplyAsync(() -> performDetailedHealthCheck(server), scheduler)
                    .thenAccept(result -> updateServerHealth(server, status, result))
                    .exceptionally(throwable -> {
                        updateServerHealth(server, status, new HealthCheckResult(false, "检查异常: " + throwable.getMessage()));
                        return null;
                    });
        }
    }
    
    private HealthCheckResult performDetailedHealthCheck(Server server) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 模拟多种健康检查
            boolean tcpCheck = performTcpCheck(server);
            boolean httpCheck = performHttpCheck(server);
            boolean resourceCheck = performResourceCheck(server);
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            boolean healthy = tcpCheck && httpCheck && resourceCheck && 
                            responseTime < config.getMaxResponseTime();
            
            String message = String.format("TCP:%s, HTTP:%s, Resource:%s, ResponseTime:%dms", 
                                         tcpCheck, httpCheck, resourceCheck, responseTime);
            
            return new HealthCheckResult(healthy, message);
            
        } catch (Exception e) {
            return new HealthCheckResult(false, "检查异常: " + e.getMessage());
        }
    }
    
    private boolean performTcpCheck(Server server) {
        // 模拟TCP连接检查
        return Math.random() > 0.05; // 95%成功率
    }
    
    private boolean performHttpCheck(Server server) {
        // 模拟HTTP健康检查
        return Math.random() > 0.03; // 97%成功率
    }
    
    private boolean performResourceCheck(Server server) {
        // 模拟资源使用率检查（CPU、内存等）
        return Math.random() > 0.02; // 98%成功率
    }
    
    private void updateServerHealth(Server server, HealthStatus status, HealthCheckResult result) {
        boolean wasHealthy = server.isHealthy();
        
        if (result.isHealthy()) {
            status.recordSuccess();
        } else {
            status.recordFailure();
        }
        
        // 根据连续成功/失败次数决定服务器状态
        boolean shouldBeHealthy = status.shouldBeHealthy(config);
        
        if (wasHealthy != shouldBeHealthy) {
            loadBalancer.updateServerStatus(server, shouldBeHealthy);
            System.out.println(String.format("服务器 %s 状态变更: %s -> %s, 原因: %s",
                                            server.getName(),
                                            wasHealthy ? "健康" : "不健康",
                                            shouldBeHealthy ? "健康" : "不健康",
                                            result.getMessage()));
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
    
    // 健康状态类
    private static class HealthStatus {
        private int consecutiveSuccesses = 0;
        private int consecutiveFailures = 0;
        
        public void recordSuccess() {
            consecutiveSuccesses++;
            consecutiveFailures = 0;
        }
        
        public void recordFailure() {
            consecutiveFailures++;
            consecutiveSuccesses = 0;
        }
        
        public boolean shouldBeHealthy(HealthCheckConfig config) {
            if (consecutiveFailures >= config.getFailureThreshold()) {
                return false;
            }
            if (consecutiveSuccesses >= config.getSuccessThreshold()) {
                return true;
            }
            // 保持当前状态
            return consecutiveFailures < config.getFailureThreshold();
        }
    }
    
    // 健康检查结果类
    private static class HealthCheckResult {
        private final boolean healthy;
        private final String message;
        
        public HealthCheckResult(boolean healthy, String message) {
            this.healthy = healthy;
            this.message = message;
        }
        
        public boolean isHealthy() { return healthy; }
        public String getMessage() { return message; }
    }
    
    // 健康检查配置类
    public static class HealthCheckConfig {
        private int checkInterval = 10; // 检查间隔（秒）
        private int maxResponseTime = 5000; // 最大响应时间（毫秒）
        private int failureThreshold = 3; // 连续失败阈值
        private int successThreshold = 2; // 连续成功阈值
        private int threadPoolSize = 5; // 线程池大小
        
        // Getters and Setters
        public int getCheckInterval() { return checkInterval; }
        public void setCheckInterval(int checkInterval) { this.checkInterval = checkInterval; }
        
        public int getMaxResponseTime() { return maxResponseTime; }
        public void setMaxResponseTime(int maxResponseTime) { this.maxResponseTime = maxResponseTime; }
        
        public int getFailureThreshold() { return failureThreshold; }
        public void setFailureThreshold(int failureThreshold) { this.failureThreshold = failureThreshold; }
        
        public int getSuccessThreshold() { return successThreshold; }
        public void setSuccessThreshold(int successThreshold) { this.successThreshold = successThreshold; }
        
        public int getThreadPoolSize() { return threadPoolSize; }
        public void setThreadPoolSize(int threadPoolSize) { this.threadPoolSize = threadPoolSize; }
    }
}
```

## 测试示例

```java
public class LoadBalancerTest {
    public static void main(String[] args) {
        // 创建服务器列表
        List<Server> servers = Arrays.asList(
            new Server("Server1", "192.168.1.1", 8080),
            new Server("Server2", "192.168.1.2", 8080),
            new Server("Server3", "192.168.1.3", 8080)
        );
        
        // 测试不同的负载均衡算法
        testLoadBalancer("轮询算法", new RoundRobinLoadBalancer(servers));
        testLoadBalancer("随机算法", new RandomLoadBalancer(servers));
        testLoadBalancer("最少连接算法", new LeastConnectionsLoadBalancer(servers));
        testLoadBalancer("一致性哈希算法", new ConsistentHashLoadBalancer(servers));
        
        // 测试加权算法
        List<WeightedRoundRobinLoadBalancer.WeightedServer> weightedServers = Arrays.asList(
            new WeightedRoundRobinLoadBalancer.WeightedServer(servers.get(0), 3),
            new WeightedRoundRobinLoadBalancer.WeightedServer(servers.get(1), 2),
            new WeightedRoundRobinLoadBalancer.WeightedServer(servers.get(2), 1)
        );
        testWeightedLoadBalancer("加权轮询算法", new WeightedRoundRobinLoadBalancer(weightedServers));
        
        // 测试健康检查
        testHealthCheck(servers);
    }
    
    private static void testLoadBalancer(String name, LoadBalancer loadBalancer) {
        System.out.println("\n=== " + name + " 测试 ===");
        
        // 模拟请求
        for (int i = 0; i < 10; i++) {
            Request request = new Request("request-" + i, "192.168.100." + (i % 5 + 1));
            try {
                Server server = loadBalancer.selectServer(request);
                System.out.println("请求 " + request.getId() + " 路由到: " + server.getName());
            } catch (Exception e) {
                System.out.println("请求 " + request.getId() + " 路由失败: " + e.getMessage());
            }
        }
    }
    
    private static void testWeightedLoadBalancer(String name, WeightedRoundRobinLoadBalancer loadBalancer) {
        System.out.println("\n=== " + name + " 测试 ===");
        
        Map<String, Integer> serverCounts = new HashMap<>();
        
        // 发送100个请求统计分布
        for (int i = 0; i < 100; i++) {
            Request request = new Request("request-" + i, "192.168.100.1");
            Server server = loadBalancer.selectServer(request);
            serverCounts.merge(server.getName(), 1, Integer::sum);
        }
        
        System.out.println("请求分布统计:");
        serverCounts.forEach((serverName, count) -> 
            System.out.println(serverName + ": " + count + " 次"));
    }
    
    private static void testHealthCheck(List<Server> servers) {
        System.out.println("\n=== 健康检查测试 ===");
        
        LoadBalancer loadBalancer = new RoundRobinLoadBalancer(servers);
        HealthChecker healthChecker = new HealthChecker(servers, loadBalancer, 2);
        
        // 启动健康检查
        healthChecker.startHealthCheck();
        
        // 模拟服务器故障
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                System.out.println("\n模拟 Server1 故障...");
                servers.get(0).setHealthy(false);
                
                Thread.sleep(10000);
                System.out.println("\nServer1 恢复正常...");
                servers.get(0).setHealthy(true);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        // 持续发送请求
        for (int i = 0; i < 20; i++) {
            try {
                Thread.sleep(1000);
                Request request = new Request("request-" + i, "192.168.100.1");
                Server server = loadBalancer.selectServer(request);
                System.out.println("请求 " + request.getId() + " 路由到: " + server.getName());
            } catch (Exception e) {
                System.out.println("请求路由失败: " + e.getMessage());
            }
        }
        
        healthChecker.shutdown();
    }
}
```

## 性能对比

```java
public class LoadBalancerBenchmark {
    private static final int THREAD_COUNT = 10;
    private static final int REQUESTS_PER_THREAD = 10000;
    
    public static void main(String[] args) throws InterruptedException {
        List<Server> servers = createServers(5);
        
        // 测试不同算法的性能
        benchmarkLoadBalancer("轮询算法", new RoundRobinLoadBalancer(servers));
        benchmarkLoadBalancer("随机算法", new RandomLoadBalancer(servers));
        benchmarkLoadBalancer("最少连接算法", new LeastConnectionsLoadBalancer(servers));
        benchmarkLoadBalancer("一致性哈希算法", new ConsistentHashLoadBalancer(servers));
    }
    
    private static List<Server> createServers(int count) {
        List<Server> servers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            servers.add(new Server("Server" + (i + 1), "192.168.1." + (i + 1), 8080));
        }
        return servers;
    }
    
    private static void benchmarkLoadBalancer(String name, LoadBalancer loadBalancer) 
            throws InterruptedException {
        System.out.println("\n=== " + name + " 性能测试 ===");
        
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        AtomicLong totalTime = new AtomicLong(0);
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);
        
        long startTime = System.currentTimeMillis();
        
        // 创建多个线程并发测试
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            new Thread(() -> {
                try {
                    for (int j = 0; j < REQUESTS_PER_THREAD; j++) {
                        long requestStart = System.nanoTime();
                        try {
                            Request request = new Request("request-" + threadId + "-" + j, 
                                                        "192.168.100." + (j % 10 + 1));
                            Server server = loadBalancer.selectServer(request);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                        }
                        long requestEnd = System.nanoTime();
                        totalTime.addAndGet(requestEnd - requestStart);
                    }
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        long endTime = System.currentTimeMillis();
        
        long totalRequests = THREAD_COUNT * REQUESTS_PER_THREAD;
        double avgLatency = totalTime.get() / (double) totalRequests / 1_000_000; // 转换为毫秒
        double throughput = totalRequests / ((endTime - startTime) / 1000.0);
        
        System.out.println("总请求数: " + totalRequests);
        System.out.println("成功请求: " + successCount.get());
        System.out.println("失败请求: " + errorCount.get());
        System.out.println("总耗时: " + (endTime - startTime) + " ms");
        System.out.println("平均延迟: " + String.format("%.3f", avgLatency) + " ms");
        System.out.println("吞吐量: " + String.format("%.2f", throughput) + " 请求/秒");
    }
}
```

## 算法选择指南

| 算法 | 适用场景 | 优点 | 缺点 | 复杂度 |
|------|----------|------|------|--------|
| 轮询 | 服务器性能相近 | 简单、公平 | 不考虑服务器负载 | O(1) |
| 加权轮询 | 服务器性能不同 | 考虑服务器能力 | 配置复杂 | O(1) |
| 随机 | 简单场景 | 实现简单 | 分布可能不均匀 | O(1) |
| 加权随机 | 服务器性能不同 | 长期分布均匀 | 短期可能不均匀 | O(1) |
| 最少连接 | 长连接场景 | 考虑实际负载 | 需要维护连接数 | O(n) |
| 一致性哈希 | 缓存、有状态服务 | 扩展性好 | 实现复杂 | O(log n) |
| IP哈希 | 会话保持 | 同一客户端固定服务器 | 分布可能不均匀 | O(1) |
| 响应时间 | 性能敏感场景 | 考虑实际性能 | 需要监控响应时间 | O(n) |

## 最佳实践

### 1. 算法选择原则

```java
public class LoadBalancerSelector {
    
    public static LoadBalancer selectLoadBalancer(LoadBalancerConfig config) {
        List<Server> servers = config.getServers();
        
        // 根据场景选择算法
        switch (config.getScenario()) {
            case WEB_APPLICATION:
                // Web应用：使用加权轮询
                return createWeightedRoundRobin(servers, config.getWeights());
                
            case CACHE_CLUSTER:
                // 缓存集群：使用一致性哈希
                return new ConsistentHashLoadBalancer(servers);
                
            case DATABASE_CLUSTER:
                // 数据库集群：使用最少连接
                return new LeastConnectionsLoadBalancer(servers);
                
            case SESSION_STICKY:
                // 会话保持：使用IP哈希
                return new IpHashLoadBalancer(servers);
                
            case HIGH_PERFORMANCE:
                // 高性能场景：使用响应时间算法
                return new ResponseTimeLoadBalancer(servers);
                
            default:
                // 默认使用轮询
                return new RoundRobinLoadBalancer(servers);
        }
    }
    
    private static LoadBalancer createWeightedRoundRobin(List<Server> servers, Map<String, Integer> weights) {
        List<WeightedRoundRobinLoadBalancer.WeightedServer> weightedServers = servers.stream()
                .map(server -> new WeightedRoundRobinLoadBalancer.WeightedServer(
                    server, weights.getOrDefault(server.getName(), 1)))
                .collect(Collectors.toList());
        return new WeightedRoundRobinLoadBalancer(weightedServers);
    }
    
    public enum Scenario {
        WEB_APPLICATION,
        CACHE_CLUSTER,
        DATABASE_CLUSTER,
        SESSION_STICKY,
        HIGH_PERFORMANCE
    }
    
    public static class LoadBalancerConfig {
        private List<Server> servers;
        private Scenario scenario;
        private Map<String, Integer> weights = new HashMap<>();
        
        // Getters and Setters
        public List<Server> getServers() { return servers; }
        public void setServers(List<Server> servers) { this.servers = servers; }
        
        public Scenario getScenario() { return scenario; }
        public void setScenario(Scenario scenario) { this.scenario = scenario; }
        
        public Map<String, Integer> getWeights() { return weights; }
        public void setWeights(Map<String, Integer> weights) { this.weights = weights; }
    }
}
```

### 2. 监控和指标

```java
public class LoadBalancerMetrics {
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final Map<String, AtomicLong> serverRequestCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> serverResponseTimes = new ConcurrentHashMap<>();
    
    public void recordRequest(String serverName, long responseTime, boolean success) {
        totalRequests.incrementAndGet();
        
        if (success) {
            successRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        
        serverRequestCounts.computeIfAbsent(serverName, k -> new AtomicLong(0)).incrementAndGet();
        serverResponseTimes.computeIfAbsent(serverName, k -> new AtomicLong(0)).addAndGet(responseTime);
    }
    
    public void printMetrics() {
        System.out.println("\n=== 负载均衡器指标 ===");
        System.out.println("总请求数: " + totalRequests.get());
        System.out.println("成功请求: " + successRequests.get());
        System.out.println("失败请求: " + failedRequests.get());
        System.out.println("成功率: " + String.format("%.2f%%", 
            (double) successRequests.get() / totalRequests.get() * 100));
        
        System.out.println("\n各服务器请求分布:");
        serverRequestCounts.forEach((server, count) -> {
            long avgResponseTime = serverResponseTimes.get(server).get() / count.get();
            System.out.println(server + ": " + count.get() + " 请求, 平均响应时间: " + avgResponseTime + "ms");
        });
    }
    
    public double getSuccessRate() {
        long total = totalRequests.get();
        return total > 0 ? (double) successRequests.get() / total : 0.0;
    }
    
    public Map<String, Double> getServerDistribution() {
        long total = totalRequests.get();
        Map<String, Double> distribution = new HashMap<>();
        
        serverRequestCounts.forEach((server, count) -> {
            double percentage = total > 0 ? (double) count.get() / total * 100 : 0.0;
            distribution.put(server, percentage);
        });
        
        return distribution;
    }
}
```

### 3. 配置管理

```java
public class LoadBalancerConfigManager {
    private final String configFile;
    private LoadBalancerConfig config;
    private final List<ConfigChangeListener> listeners = new ArrayList<>();
    
    public LoadBalancerConfigManager(String configFile) {
        this.configFile = configFile;
        loadConfig();
        startConfigWatcher();
    }
    
    private void loadConfig() {
        try {
            // 从配置文件加载配置（这里简化为硬编码）
            config = new LoadBalancerConfig();
            config.setAlgorithm("WEIGHTED_ROUND_ROBIN");
            config.setHealthCheckInterval(10);
            config.setMaxRetries(3);
            
            Map<String, ServerConfig> servers = new HashMap<>();
            servers.put("server1", new ServerConfig("192.168.1.1", 8080, 3));
            servers.put("server2", new ServerConfig("192.168.1.2", 8080, 2));
            servers.put("server3", new ServerConfig("192.168.1.3", 8080, 1));
            config.setServers(servers);
            
        } catch (Exception e) {
            throw new RuntimeException("加载配置失败", e);
        }
    }
    
    private void startConfigWatcher() {
        // 监控配置文件变化（实际实现中可以使用文件监控或配置中心）
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                LoadBalancerConfig newConfig = loadConfigFromFile();
                if (!config.equals(newConfig)) {
                    LoadBalancerConfig oldConfig = config;
                    config = newConfig;
                    notifyConfigChange(oldConfig, newConfig);
                }
            } catch (Exception e) {
                System.err.println("配置重载失败: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    private LoadBalancerConfig loadConfigFromFile() {
        // 实际实现中从文件或配置中心加载
        return config; // 简化实现
    }
    
    private void notifyConfigChange(LoadBalancerConfig oldConfig, LoadBalancerConfig newConfig) {
        for (ConfigChangeListener listener : listeners) {
            try {
                listener.onConfigChange(oldConfig, newConfig);
            } catch (Exception e) {
                System.err.println("配置变更通知失败: " + e.getMessage());
            }
        }
    }
    
    public void addConfigChangeListener(ConfigChangeListener listener) {
        listeners.add(listener);
    }
    
    public LoadBalancerConfig getConfig() {
        return config;
    }
    
    public interface ConfigChangeListener {
        void onConfigChange(LoadBalancerConfig oldConfig, LoadBalancerConfig newConfig);
    }
    
    public static class LoadBalancerConfig {
        private String algorithm;
        private int healthCheckInterval;
        private int maxRetries;
        private Map<String, ServerConfig> servers;
        
        // Getters and Setters
        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
        
        public int getHealthCheckInterval() { return healthCheckInterval; }
        public void setHealthCheckInterval(int healthCheckInterval) { this.healthCheckInterval = healthCheckInterval; }
        
        public int getMaxRetries() { return maxRetries; }
        public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
        
        public Map<String, ServerConfig> getServers() { return servers; }
        public void setServers(Map<String, ServerConfig> servers) { this.servers = servers; }
    }
    
    public static class ServerConfig {
        private String host;
        private int port;
        private int weight;
        
        public ServerConfig(String host, int port, int weight) {
            this.host = host;
            this.port = port;
            this.weight = weight;
        }
        
        // Getters and Setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        
        public int getWeight() { return weight; }
        public void setWeight(int weight) { this.weight = weight; }
    }
}
```

## 面试要点

### 高频问题

1. **常见负载均衡算法有哪些？各自的优缺点是什么？**
   - 轮询：简单公平，但不考虑服务器负载
   - 加权轮询：考虑服务器能力差异
   - 随机：实现简单，长期分布均匀
   - 最少连接：适合长连接场景
   - 一致性哈希：适合缓存场景，扩展性好
   - IP哈希：实现会话保持
   - 响应时间：考虑服务器实际性能

2. **一致性哈希算法的原理和优势？**
   - 原理：将服务器和请求映射到哈希环上
   - 优势：添加/删除服务器时只影响相邻节点
   - 虚拟节点：解决数据分布不均匀问题

3. **如何处理服务器故障？**
   - 健康检查机制
   - 故障转移策略
   - 自动恢复机制

### 深入问题

1. **如何设计一个高可用的负载均衡器？**
   - 多层负载均衡
   - 健康检查和故障转移
   - 配置热更新
   - 监控和告警

2. **负载均衡器的性能瓶颈在哪里？如何优化？**
   - 算法复杂度优化
   - 并发处理能力
   - 内存使用优化
   - 网络延迟优化

3. **如何实现会话保持（Session Sticky）？**
   - IP哈希
   - Cookie-based路由
   - Session复制
   - 外部Session存储

### 实践经验

1. **生产环境中如何选择负载均衡算法？**
   - 根据应用特性选择
   - 考虑服务器性能差异
   - 监控和调优

2. **如何处理热点数据问题？**
   - 一致性哈希的局限性
   - 虚拟节点优化
   - 动态权重调整

3. **负载均衡器的监控指标有哪些？**
   - 请求分布
   - 响应时间
   - 错误率
   - 服务器健康状态

## 总结

负载均衡算法是分布式系统中的核心组件，不同的算法适用于不同的场景：

1. **简单场景**：使用轮询或随机算法
2. **性能差异**：使用加权算法
3. **长连接**：使用最少连接算法
4. **缓存场景**：使用一致性哈希算法
5. **会话保持**：使用IP哈希算法
6. **性能敏感**：使用响应时间算法

在实际应用中，需要结合具体业务场景、服务器性能、网络环境等因素来选择合适的负载均衡算法，并配合健康检查、监控告警等机制，构建高可用、高性能的分布式系统。

### 平滑加权轮询算法

```java
public class SmoothWeightedRoundRobinLoadBalancer implements LoadBalancer {
    private final List<WeightedServerNode> servers;
    
    public SmoothWeightedRoundRobinLoadBalancer(List<WeightedServerNode> servers) {
        this.servers = new CopyOnWriteArrayList<>(servers);
    }
    
    @Override
    public synchronized Server selectServer(Request request) {
        List<WeightedServerNode> healthyServers = getHealthyWeightedNodes();
        if (healthyServers.isEmpty()) {
            throw new RuntimeException("No healthy servers available");
        }
        
        // 计算总权重
        int totalWeight = healthyServers.stream()
                .mapToInt(WeightedServerNode::getWeight)
                .sum();
        
        // 更新当前权重
        for (WeightedServerNode node : healthyServers) {
            node.setCurrentWeight(node.getCurrentWeight() + node.getWeight());
        }
        
        // 选择当前权重最大的服务器
        WeightedServerNode selected = healthyServers.stream()
                .max(Comparator.comparingInt(WeightedServerNode::getCurrentWeight))
                .orElse(healthyServers.get(0));
        
        // 减少选中服务器的当前权重
        selected.setCurrentWeight(selected.getCurrentWeight() - totalWeight);
        
        return selected.getServer();
    }
    
    private List<WeightedServerNode> getHealthyWeightedNodes() {
        return servers.stream()
                .filter(node -> node.getServer().isHealthy())
                .collect(Collectors.toList());
    }
    
    @Override
    public void addServer(Server server) {
        servers.add(new WeightedServerNode(server, 1));
    }
    
    public void addServer(Server server, int weight) {
        servers.add(new WeightedServerNode(server, weight));
    }
    
    @Override
    public void removeServer(Server server) {
        servers.removeIf(node -> node.getServer().equals(server));
    }
    
    @Override
    public List<Server> getHealthyServers() {
        return getHealthyWeightedNodes().stream()
                .map(WeightedServerNode::getServer)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateServerStatus(Server server, boolean healthy) {
        server.setHealthy(healthy);
    }
    
    // 加权服务器节点类
    public static class WeightedServerNode {
        private final Server server;
        private final int weight;
        private int currentWeight;
        
        public WeightedServerNode(Server server, int weight) {
            this.server = server;
            this.weight = weight;
            this.currentWeight = 0;
        }
        
        public Server getServer() { return server; }
        public int getWeight() { return weight; }
        public int getCurrentWeight() { return currentWeight; }
        public void setCurrentWeight(int currentWeight) { this.currentWeight = currentWeight; }
    }
}
```

## 随机算法

### 基本随机算法

```java
public class RandomLoadBalancer implements LoadBalancer {
    private final List<Server> servers;
    private final Random random;
    
    public RandomLoadBalancer(List<Server> servers) {
        this.servers = new CopyOnWriteArrayList<>(servers);
        this.random = new Random();
    }
    
    @Override
    public Server selectServer(Request request) {
        List<Server> healthyServers = getHealthyServers();
        if (healthyServers.isEmpty()) {
            throw new RuntimeException("No healthy servers available");
        }
        
        int index = random.nextInt(healthyServers.size());
        return healthyServers.get(index);
    }
    
    @Override
    public void addServer(Server server) {
        servers.add(server);
    }
    
    @Override
    public void removeServer(Server server) {
        servers.remove(server);
    }
    
    @Override
    public List<Server> getHealthyServers() {
        return servers.stream()
                .filter(Server::isHealthy)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateServerStatus(Server server, boolean healthy) {
        server.setHealthy(healthy);
    }
}
```

## 加权随机算法

```java
public class WeightedRandomLoadBalancer implements LoadBalancer {
    private final List<WeightedServer> servers;
    private final Random random;
    
    public WeightedRandomLoadBalancer(List<WeightedServer> servers) {
        this.servers = new CopyOnWriteArrayList<>(servers);
        this.random = new Random();
    }
    
    @Override
    public Server selectServer(Request request) {
        List<WeightedServer> healthyServers = getHealthyWeightedServers();
        if (healthyServers.isEmpty()) {
            throw new RuntimeException("No healthy servers available");
        }
        
        // 计算总权重
        int totalWeight = healthyServers.stream()
                .mapToInt(WeightedServer::getWeight)
                .sum();
        
        // 生成随机数
        int randomWeight = random.nextInt(totalWeight);
        int currentWeight = 0;
        
        // 根据权重选择服务器
        for (WeightedServer server : healthyServers) {
            currentWeight += server.getWeight();
            if (randomWeight < currentWeight) {
                return server.getServer();
            }
        }
        
        // 默认返回最后一个服务器
        return healthyServers.get(healthyServers.size() - 1).getServer();
    }
    
    private List<WeightedServer> getHealthyWeightedServers() {
        return servers.stream()
                .filter(ws -> ws.getServer().isHealthy())
                .collect(Collectors.toList());
    }
    
    @Override
    public void addServer(Server server) {
        servers.add(new WeightedServer(server, 1));
    }
    
    public void addServer(Server server, int weight) {
        servers.add(new WeightedServer(server, weight));
    }
    
    @Override
    public void removeServer(Server server) {
        servers.removeIf(ws -> ws.getServer().equals(server));
    }
    
    @Override
    public List<Server> getHealthyServers() {
        return getHealthyWeightedServers().stream()
                .map(WeightedServer::getServer)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateServerStatus(Server server, boolean healthy) {
        server.setHealthy(healthy);
    }
    
    // 使用之前定义的WeightedServer类
    public static class WeightedServer {
        private final Server server;
        private final int weight;
        
        public WeightedServer(Server server, int weight) {
            this.server = server;
            this.weight = weight;
        }
        
        public Server getServer() { return server; }
        public int getWeight() { return weight; }
    }
}
```

## 最少连接算法

```java
public class LeastConnectionsLoadBalancer implements LoadBalancer {
    private final List<Server> servers;
    
    public LeastConnectionsLoadBalancer(List<Server> servers) {
        this.servers = new CopyOnWriteArrayList<>(servers);
    }
    
    @Override
    public Server selectServer(Request request) {
        List<Server> healthyServers = getHealthyServers();
        if (healthyServers.isEmpty()) {
            throw new RuntimeException("No healthy servers available");
        }
        
        // 选择当前连接数最少的服务器
        return healthyServers.stream()
                .min(Comparator.comparingInt(Server::getCurrentLoad))
                .orElse(healthyServers.get(0));
    }
    
    @Override
    public void addServer(Server server) {
        servers.add(server);
    }
    
    @Override
    public void removeServer(Server server) {
        servers.remove(server);
    }
    
    @Override
    public List<Server> getHealthyServers() {
        return servers.stream()
                .filter(Server::isHealthy)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateServerStatus(Server server, boolean healthy) {
        server.setHealthy(healthy);
    }
}
```

### 加权最少连接算法

```java
public class WeightedLeastConnectionsLoadBalancer implements LoadBalancer {
    private final List<WeightedServer> servers;
    
    public WeightedLeastConnectionsLoadBalancer(List<WeightedServer> servers) {
        this.servers = new CopyOnWriteArrayList<>(servers);
    }
    
    @Override
    public Server selectServer(Request request) {
        List<WeightedServer> healthyServers = getHealthyWeightedServers();
        if (healthyServers.isEmpty()) {
            throw new RuntimeException("No healthy servers available");
        }
        
        // 选择连接数/权重比值最小的服务器
        return healthyServers.stream()
                .min(Comparator.comparingDouble(this::getLoadRatio))
                .map(WeightedServer::getServer)
                .orElse(healthyServers.get(0).getServer());
    }
    
    private double getLoadRatio(WeightedServer weightedServer) {
        Server server = weightedServer.getServer();
        int weight = weightedServer.getWeight();
        return weight > 0 ? (double) server.getCurrentLoad() / weight : Double.MAX_VALUE;
    }
    
    private List<WeightedServer> getHealthyWeightedServers() {
        return servers.stream()
                .filter(ws -> ws.getServer().isHealthy())
                .collect(Collectors.toList());
    }
    
    @Override
    public void addServer(Server server) {
        servers.add(new WeightedServer(server, 1));
    }
    
    public void addServer(Server server, int weight) {
        servers.add(new WeightedServer(server, weight));
    }
    
    @Override
    public void removeServer(Server server) {
        servers.removeIf(ws -> ws.getServer().equals(server));
    }
    
    @Override
    public List<Server> getHealthyServers() {
        return getHealthyWeightedServers().stream()
                .map(WeightedServer::getServer)
                .collect(Collectors.toList());
    }
    
    @Override
    public void updateServerStatus(Server server, boolean healthy) {
        server.setHealthy(healthy);
    }
    
    // 使用之前定义的WeightedServer类
    public static class WeightedServer {
        private final Server server;
        private final int weight;
        
        public WeightedServer(Server server, int weight) {
            this.server = server;
            this.weight = weight;
        }
        
        public Server getServer() { return server; }
        public int getWeight() { return weight; }
    }
}
```