# 分布式共识算法

## 概述

分布式共识算法是分布式系统中最核心的算法之一，用于在多个节点之间就某个值或决策达成一致。在存在网络分区、节点故障等异常情况下，共识算法确保系统的一致性和可用性。

### 核心概念

1. **共识问题**：多个节点需要对某个提议达成一致
2. **拜占庭将军问题**：在存在恶意节点的情况下达成共识
3. **FLP不可能性**：在异步网络中无法同时保证安全性、活性和容错性
4. **安全性**：不会产生错误的结果
5. **活性**：系统最终会产生结果

### 共识算法分类

1. **CFT算法**（Crash Fault Tolerant）：只考虑节点崩溃故障
   - Paxos、Raft、PBFT等

2. **BFT算法**（Byzantine Fault Tolerant）：考虑拜占庭故障
   - PBFT、Tendermint、HotStuff等

## Raft共识算法

### 算法原理

Raft是一个易于理解的共识算法，将共识问题分解为：
1. **领导者选举**（Leader Election）
2. **日志复制**（Log Replication）
3. **安全性保证**（Safety）

### Java实现

#### 基础数据结构

```java
public class RaftConsensus {
    
    // 节点状态
    public enum NodeState {
        FOLLOWER, CANDIDATE, LEADER
    }
    
    // 日志条目
    public static class LogEntry {
        private final int term;
        private final String command;
        private final long timestamp;
        
        public LogEntry(int term, String command) {
            this.term = term;
            this.command = command;
            this.timestamp = System.currentTimeMillis();
        }
        
        // getters
        public int getTerm() { return term; }
        public String getCommand() { return command; }
        public long getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return "LogEntry{term=" + term + ", command='" + command + "', timestamp=" + timestamp + "}";
        }
    }
    
    // 投票请求
    public static class VoteRequest {
        private final int term;
        private final String candidateId;
        private final int lastLogIndex;
        private final int lastLogTerm;
        
        public VoteRequest(int term, String candidateId, int lastLogIndex, int lastLogTerm) {
            this.term = term;
            this.candidateId = candidateId;
            this.lastLogIndex = lastLogIndex;
            this.lastLogTerm = lastLogTerm;
        }
        
        // getters
        public int getTerm() { return term; }
        public String getCandidateId() { return candidateId; }
        public int getLastLogIndex() { return lastLogIndex; }
        public int getLastLogTerm() { return lastLogTerm; }
    }
    
    // 投票响应
    public static class VoteResponse {
        private final int term;
        private final boolean voteGranted;
        private final String voterId;
        
        public VoteResponse(int term, boolean voteGranted, String voterId) {
            this.term = term;
            this.voteGranted = voteGranted;
            this.voterId = voterId;
        }
        
        // getters
        public int getTerm() { return term; }
        public boolean isVoteGranted() { return voteGranted; }
        public String getVoterId() { return voterId; }
    }
    
    // 日志复制请求
    public static class AppendEntriesRequest {
        private final int term;
        private final String leaderId;
        private final int prevLogIndex;
        private final int prevLogTerm;
        private final List<LogEntry> entries;
        private final int leaderCommit;
        
        public AppendEntriesRequest(int term, String leaderId, int prevLogIndex, 
                                  int prevLogTerm, List<LogEntry> entries, int leaderCommit) {
            this.term = term;
            this.leaderId = leaderId;
            this.prevLogIndex = prevLogIndex;
            this.prevLogTerm = prevLogTerm;
            this.entries = new ArrayList<>(entries);
            this.leaderCommit = leaderCommit;
        }
        
        // getters
        public int getTerm() { return term; }
        public String getLeaderId() { return leaderId; }
        public int getPrevLogIndex() { return prevLogIndex; }
        public int getPrevLogTerm() { return prevLogTerm; }
        public List<LogEntry> getEntries() { return new ArrayList<>(entries); }
        public int getLeaderCommit() { return leaderCommit; }
    }
    
    // 日志复制响应
    public static class AppendEntriesResponse {
        private final int term;
        private final boolean success;
        private final String followerId;
        private final int matchIndex;
        
        public AppendEntriesResponse(int term, boolean success, String followerId, int matchIndex) {
            this.term = term;
            this.success = success;
            this.followerId = followerId;
            this.matchIndex = matchIndex;
        }
        
        // getters
        public int getTerm() { return term; }
        public boolean isSuccess() { return success; }
        public String getFollowerId() { return followerId; }
        public int getMatchIndex() { return matchIndex; }
    }
}
```

#### Raft节点实现

```java
public class RaftNode {
    private final String nodeId;
    private final List<String> clusterNodes;
    private final Map<String, RaftNode> peers;
    
    // 持久化状态
    private volatile int currentTerm = 0;
    private volatile String votedFor = null;
    private final List<LogEntry> log = new ArrayList<>();
    
    // 易失状态
    private volatile NodeState state = NodeState.FOLLOWER;
    private volatile int commitIndex = -1;
    private volatile int lastApplied = -1;
    
    // 领导者状态
    private final Map<String, Integer> nextIndex = new HashMap<>();
    private final Map<String, Integer> matchIndex = new HashMap<>();
    
    // 选举相关
    private volatile long lastHeartbeat = System.currentTimeMillis();
    private volatile String currentLeader = null;
    private final Set<String> votesReceived = new HashSet<>();
    
    // 配置参数
    private final int electionTimeoutMin = 150;
    private final int electionTimeoutMax = 300;
    private final int heartbeatInterval = 50;
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private final Random random = new Random();
    
    public RaftNode(String nodeId, List<String> clusterNodes) {
        this.nodeId = nodeId;
        this.clusterNodes = new ArrayList<>(clusterNodes);
        this.peers = new HashMap<>();
        
        // 初始化索引
        for (String peer : clusterNodes) {
            if (!peer.equals(nodeId)) {
                nextIndex.put(peer, 0);
                matchIndex.put(peer, -1);
            }
        }
        
        startElectionTimer();
    }
    
    public void addPeer(String peerId, RaftNode peer) {
        peers.put(peerId, peer);
    }
    
    // 启动选举定时器
    private void startElectionTimer() {
        int timeout = electionTimeoutMin + random.nextInt(electionTimeoutMax - electionTimeoutMin);
        
        scheduler.schedule(() -> {
            if (state != NodeState.LEADER && 
                System.currentTimeMillis() - lastHeartbeat > timeout) {
                startElection();
            }
            startElectionTimer(); // 重新启动定时器
        }, timeout, TimeUnit.MILLISECONDS);
    }
    
    // 开始选举
    private synchronized void startElection() {
        state = NodeState.CANDIDATE;
        currentTerm++;
        votedFor = nodeId;
        votesReceived.clear();
        votesReceived.add(nodeId); // 给自己投票
        lastHeartbeat = System.currentTimeMillis();
        
        System.out.println("节点 " + nodeId + " 开始选举，任期: " + currentTerm);
        
        // 向所有其他节点发送投票请求
        for (String peerId : clusterNodes) {
            if (!peerId.equals(nodeId)) {
                RaftNode peer = peers.get(peerId);
                if (peer != null) {
                    requestVote(peer);
                }
            }
        }
        
        // 检查是否获得多数票
        checkElectionResult();
    }
    
    // 发送投票请求
    private void requestVote(RaftNode peer) {
        int lastLogIndex = log.size() - 1;
        int lastLogTerm = lastLogIndex >= 0 ? log.get(lastLogIndex).getTerm() : 0;
        
        VoteRequest request = new VoteRequest(currentTerm, nodeId, lastLogIndex, lastLogTerm);
        
        // 异步发送请求
        scheduler.submit(() -> {
            try {
                VoteResponse response = peer.handleVoteRequest(request);
                handleVoteResponse(response);
            } catch (Exception e) {
                System.err.println("发送投票请求失败: " + e.getMessage());
            }
        });
    }
    
    // 处理投票请求
    public synchronized VoteResponse handleVoteRequest(VoteRequest request) {
        boolean voteGranted = false;
        
        // 如果请求的任期更大，更新当前任期
        if (request.getTerm() > currentTerm) {
            currentTerm = request.getTerm();
            votedFor = null;
            state = NodeState.FOLLOWER;
            currentLeader = null;
        }
        
        // 投票条件检查
        if (request.getTerm() == currentTerm && 
            (votedFor == null || votedFor.equals(request.getCandidateId()))) {
            
            // 检查候选者的日志是否至少和自己一样新
            int lastLogIndex = log.size() - 1;
            int lastLogTerm = lastLogIndex >= 0 ? log.get(lastLogIndex).getTerm() : 0;
            
            boolean logUpToDate = request.getLastLogTerm() > lastLogTerm || 
                                (request.getLastLogTerm() == lastLogTerm && 
                                 request.getLastLogIndex() >= lastLogIndex);
            
            if (logUpToDate) {
                voteGranted = true;
                votedFor = request.getCandidateId();
                lastHeartbeat = System.currentTimeMillis();
            }
        }
        
        System.out.println("节点 " + nodeId + " 处理投票请求，候选者: " + 
                          request.getCandidateId() + ", 投票: " + voteGranted);
        
        return new VoteResponse(currentTerm, voteGranted, nodeId);
    }
    
    // 处理投票响应
    private synchronized void handleVoteResponse(VoteResponse response) {
        if (state != NodeState.CANDIDATE || response.getTerm() != currentTerm) {
            return;
        }
        
        if (response.getTerm() > currentTerm) {
            currentTerm = response.getTerm();
            state = NodeState.FOLLOWER;
            votedFor = null;
            currentLeader = null;
            return;
        }
        
        if (response.isVoteGranted()) {
            votesReceived.add(response.getVoterId());
            checkElectionResult();
        }
    }
    
    // 检查选举结果
    private void checkElectionResult() {
        int majority = (clusterNodes.size() / 2) + 1;
        
        if (votesReceived.size() >= majority) {
            becomeLeader();
        }
    }
    
    // 成为领导者
    private synchronized void becomeLeader() {
        if (state != NodeState.CANDIDATE) {
            return;
        }
        
        state = NodeState.LEADER;
        currentLeader = nodeId;
        
        // 初始化领导者状态
        for (String peer : clusterNodes) {
            if (!peer.equals(nodeId)) {
                nextIndex.put(peer, log.size());
                matchIndex.put(peer, -1);
            }
        }
        
        System.out.println("节点 " + nodeId + " 成为领导者，任期: " + currentTerm);
        
        // 立即发送心跳
        sendHeartbeats();
        
        // 启动心跳定时器
        startHeartbeatTimer();
    }
    
    // 启动心跳定时器
    private void startHeartbeatTimer() {
        scheduler.scheduleAtFixedRate(() -> {
            if (state == NodeState.LEADER) {
                sendHeartbeats();
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }
    
    // 发送心跳
    private void sendHeartbeats() {
        for (String peerId : clusterNodes) {
            if (!peerId.equals(nodeId)) {
                RaftNode peer = peers.get(peerId);
                if (peer != null) {
                    sendAppendEntries(peer, new ArrayList<>());
                }
            }
        }
    }
    
    // 客户端提交命令
    public synchronized boolean submitCommand(String command) {
        if (state != NodeState.LEADER) {
            System.out.println("节点 " + nodeId + " 不是领导者，无法处理命令");
            return false;
        }
        
        LogEntry entry = new LogEntry(currentTerm, command);
        log.add(entry);
        
        System.out.println("领导者 " + nodeId + " 接收命令: " + command);
        
        // 向所有跟随者复制日志
        replicateLog();
        
        return true;
    }
    
    // 复制日志
    private void replicateLog() {
        for (String peerId : clusterNodes) {
            if (!peerId.equals(nodeId)) {
                RaftNode peer = peers.get(peerId);
                if (peer != null) {
                    replicateLogToPeer(peer, peerId);
                }
            }
        }
    }
    
    // 向特定节点复制日志
    private void replicateLogToPeer(RaftNode peer, String peerId) {
        int nextIdx = nextIndex.get(peerId);
        List<LogEntry> entries = new ArrayList<>();
        
        // 获取需要发送的日志条目
        for (int i = nextIdx; i < log.size(); i++) {
            entries.add(log.get(i));
        }
        
        sendAppendEntries(peer, entries);
    }
    
    // 发送日志追加请求
    private void sendAppendEntries(RaftNode peer, List<LogEntry> entries) {
        String peerId = peer.getNodeId();
        int nextIdx = nextIndex.get(peerId);
        int prevLogIndex = nextIdx - 1;
        int prevLogTerm = prevLogIndex >= 0 ? log.get(prevLogIndex).getTerm() : 0;
        
        AppendEntriesRequest request = new AppendEntriesRequest(
            currentTerm, nodeId, prevLogIndex, prevLogTerm, entries, commitIndex
        );
        
        scheduler.submit(() -> {
            try {
                AppendEntriesResponse response = peer.handleAppendEntries(request);
                handleAppendEntriesResponse(response, peerId, entries.size());
            } catch (Exception e) {
                System.err.println("发送日志追加请求失败: " + e.getMessage());
            }
        });
    }
    
    // 处理日志追加请求
    public synchronized AppendEntriesResponse handleAppendEntries(AppendEntriesRequest request) {
        boolean success = false;
        int matchIndex = -1;
        
        // 更新任期
        if (request.getTerm() > currentTerm) {
            currentTerm = request.getTerm();
            votedFor = null;
            state = NodeState.FOLLOWER;
        }
        
        if (request.getTerm() == currentTerm) {
            state = NodeState.FOLLOWER;
            currentLeader = request.getLeaderId();
            lastHeartbeat = System.currentTimeMillis();
            
            // 检查日志一致性
            if (request.getPrevLogIndex() == -1 || 
                (request.getPrevLogIndex() < log.size() && 
                 log.get(request.getPrevLogIndex()).getTerm() == request.getPrevLogTerm())) {
                
                success = true;
                
                // 删除冲突的日志条目
                if (request.getPrevLogIndex() + 1 < log.size()) {
                    log.subList(request.getPrevLogIndex() + 1, log.size()).clear();
                }
                
                // 追加新的日志条目
                log.addAll(request.getEntries());
                matchIndex = request.getPrevLogIndex() + request.getEntries().size();
                
                // 更新提交索引
                if (request.getLeaderCommit() > commitIndex) {
                    commitIndex = Math.min(request.getLeaderCommit(), log.size() - 1);
                    applyCommittedEntries();
                }
            }
        }
        
        return new AppendEntriesResponse(currentTerm, success, nodeId, matchIndex);
    }
    
    // 处理日志追加响应
    private synchronized void handleAppendEntriesResponse(AppendEntriesResponse response, 
                                                        String peerId, int entriesCount) {
        if (state != NodeState.LEADER || response.getTerm() != currentTerm) {
            return;
        }
        
        if (response.getTerm() > currentTerm) {
            currentTerm = response.getTerm();
            state = NodeState.FOLLOWER;
            votedFor = null;
            currentLeader = null;
            return;
        }
        
        if (response.isSuccess()) {
            // 更新索引
            nextIndex.put(peerId, response.getMatchIndex() + 1);
            matchIndex.put(peerId, response.getMatchIndex());
            
            // 检查是否可以提交新的日志条目
            updateCommitIndex();
        } else {
            // 日志不一致，减少nextIndex重试
            int currentNext = nextIndex.get(peerId);
            nextIndex.put(peerId, Math.max(0, currentNext - 1));
            
            // 重新发送
            RaftNode peer = peers.get(peerId);
            if (peer != null) {
                replicateLogToPeer(peer, peerId);
            }
        }
    }
    
    // 更新提交索引
    private void updateCommitIndex() {
        for (int i = log.size() - 1; i > commitIndex; i--) {
            if (log.get(i).getTerm() == currentTerm) {
                int replicationCount = 1; // 包括领导者自己
                
                for (String peerId : clusterNodes) {
                    if (!peerId.equals(nodeId) && matchIndex.get(peerId) >= i) {
                        replicationCount++;
                    }
                }
                
                if (replicationCount > clusterNodes.size() / 2) {
                    commitIndex = i;
                    applyCommittedEntries();
                    break;
                }
            }
        }
    }
    
    // 应用已提交的日志条目
    private void applyCommittedEntries() {
        while (lastApplied < commitIndex) {
            lastApplied++;
            LogEntry entry = log.get(lastApplied);
            System.out.println("节点 " + nodeId + " 应用命令: " + entry.getCommand());
        }
    }
    
    // 获取节点状态
    public NodeState getState() {
        return state;
    }
    
    public String getNodeId() {
        return nodeId;
    }
    
    public String getCurrentLeader() {
        return currentLeader;
    }
    
    public int getCurrentTerm() {
        return currentTerm;
    }
    
    public int getLogSize() {
        return log.size();
    }
    
    public int getCommitIndex() {
        return commitIndex;
    }
    
    // 打印节点状态
    public void printStatus() {
        System.out.println("节点 " + nodeId + " 状态: " + state + 
                          ", 任期: " + currentTerm + 
                          ", 领导者: " + currentLeader + 
                          ", 日志大小: " + log.size() + 
                          ", 提交索引: " + commitIndex);
    }
    
    // 关闭节点
    public void shutdown() {
        scheduler.shutdown();
    }
}

## 测试示例

### Raft算法测试

```java
public class ConsensusAlgorithmTest {
    
    public static void testRaftConsensus() {
        System.out.println("\n=== Raft共识算法测试 ===");
        
        // 创建5个节点的Raft集群
        List<String> nodeIds = Arrays.asList("node1", "node2", "node3", "node4", "node5");
        Map<String, RaftNode> nodes = new HashMap<>();
        
        // 初始化节点
        for (String nodeId : nodeIds) {
            nodes.put(nodeId, new RaftNode(nodeId, nodeIds));
        }
        
        // 建立节点间连接
        for (String nodeId : nodeIds) {
            RaftNode node = nodes.get(nodeId);
            for (String peerId : nodeIds) {
                if (!peerId.equals(nodeId)) {
                    node.addPeer(peerId, nodes.get(peerId));
                }
            }
        }
        
        try {
            // 等待领导者选举
            Thread.sleep(2000);
            
            // 查找领导者
            RaftNode leader = null;
            for (RaftNode node : nodes.values()) {
                if (node.getState() == NodeState.LEADER) {
                    leader = node;
                    break;
                }
            }
            
            if (leader != null) {
                System.out.println("\n领导者选举完成，领导者: " + leader.getNodeId());
                
                // 提交一些命令
                String[] commands = {"SET x=1", "SET y=2", "GET x", "SET z=3", "GET y"};
                
                for (String command : commands) {
                    leader.submitCommand(command);
                    Thread.sleep(500); // 等待复制
                }
                
                // 等待日志复制完成
                Thread.sleep(2000);
                
                // 打印所有节点状态
                System.out.println("\n所有节点状态:");
                for (RaftNode node : nodes.values()) {
                    node.printStatus();
                }
                
                // 模拟领导者故障
                System.out.println("\n模拟领导者故障...");
                leader.shutdown();
                
                // 等待新领导者选举
                Thread.sleep(3000);
                
                // 查找新领导者
                RaftNode newLeader = null;
                for (RaftNode node : nodes.values()) {
                    if (node.getState() == NodeState.LEADER && !node.getNodeId().equals(leader.getNodeId())) {
                        newLeader = node;
                        break;
                    }
                }
                
                if (newLeader != null) {
                    System.out.println("新领导者选举完成，新领导者: " + newLeader.getNodeId());
                    
                    // 继续提交命令
                    newLeader.submitCommand("SET a=100");
                    Thread.sleep(1000);
                    
                    System.out.println("\n故障恢复后节点状态:");
                    for (RaftNode node : nodes.values()) {
                        if (!node.getNodeId().equals(leader.getNodeId())) {
                            node.printStatus();
                        }
                    }
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 关闭所有节点
            for (RaftNode node : nodes.values()) {
                node.shutdown();
            }
        }
    }
    
    public static void testPBFTConsensus() {
        System.out.println("\n=== PBFT共识算法测试 ===");
        
        // 创建4个节点的PBFT集群（容忍1个拜占庭节点）
        List<String> nodeIds = Arrays.asList("pbft1", "pbft2", "pbft3", "pbft4");
        Map<String, PBFTNode> nodes = new HashMap<>();
        
        // 初始化节点
        for (String nodeId : nodeIds) {
            nodes.put(nodeId, new PBFTNode(nodeId, nodeIds));
        }
        
        // 建立节点间连接
        for (String nodeId : nodeIds) {
            PBFTNode node = nodes.get(nodeId);
            for (String peerId : nodeIds) {
                if (!peerId.equals(nodeId)) {
                    node.addPeer(peerId, nodes.get(peerId));
                }
            }
        }
        
        try {
            // 等待初始化完成
            Thread.sleep(1000);
            
            // 获取主节点
            PBFTNode primary = nodes.get(nodeIds.get(0)); // 第一个节点作为主节点
            
            System.out.println("\n开始PBFT共识测试，主节点: " + primary.getNodeId());
            
            // 模拟客户端请求
            String[] operations = {"TRANSFER A->B 100", "TRANSFER B->C 50", "BALANCE A", "TRANSFER C->A 25"};
            
            for (int i = 0; i < operations.length; i++) {
                ClientRequest request = new ClientRequest("client" + i, operations[i]);
                System.out.println("\n提交客户端请求: " + operations[i]);
                
                primary.handleClientRequest(request);
                
                // 等待共识完成
                Thread.sleep(2000);
                
                // 打印节点状态
                System.out.println("请求 " + (i + 1) + " 处理完成后的节点状态:");
                for (PBFTNode node : nodes.values()) {
                    node.printStatus();
                }
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // 关闭所有节点
            for (PBFTNode node : nodes.values()) {
                node.shutdown();
            }
        }
    }
    
    public static void testConsensusPerformance() {
        System.out.println("\n=== 共识算法性能测试 ===");
        
        int requestCount = 100;
        
        // 测试Raft性能
        long startTime = System.currentTimeMillis();
        testRaftPerformance(requestCount);
        long raftTime = System.currentTimeMillis() - startTime;
        
        // 测试PBFT性能
        startTime = System.currentTimeMillis();
        testPBFTPerformance(requestCount);
        long pbftTime = System.currentTimeMillis() - startTime;
        
        System.out.println("\n性能测试结果（" + requestCount + "个请求）:");
        System.out.println("Raft算法: " + raftTime + "ms, 平均: " + (raftTime / (double) requestCount) + "ms/请求");
        System.out.println("PBFT算法: " + pbftTime + "ms, 平均: " + (pbftTime / (double) requestCount) + "ms/请求");
    }
    
    private static void testRaftPerformance(int requestCount) {
        List<String> nodeIds = Arrays.asList("r1", "r2", "r3");
        Map<String, RaftNode> nodes = new HashMap<>();
        
        for (String nodeId : nodeIds) {
            nodes.put(nodeId, new RaftNode(nodeId, nodeIds));
        }
        
        for (String nodeId : nodeIds) {
            RaftNode node = nodes.get(nodeId);
            for (String peerId : nodeIds) {
                if (!peerId.equals(nodeId)) {
                    node.addPeer(peerId, nodes.get(peerId));
                }
            }
        }
        
        try {
            Thread.sleep(1000); // 等待选举
            
            RaftNode leader = null;
            for (RaftNode node : nodes.values()) {
                if (node.getState() == NodeState.LEADER) {
                    leader = node;
                    break;
                }
            }
            
            if (leader != null) {
                for (int i = 0; i < requestCount; i++) {
                    leader.submitCommand("CMD" + i);
                    if (i % 10 == 0) {
                        Thread.sleep(10); // 避免过载
                    }
                }
                
                Thread.sleep(1000); // 等待完成
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            for (RaftNode node : nodes.values()) {
                node.shutdown();
            }
        }
    }
    
    private static void testPBFTPerformance(int requestCount) {
        List<String> nodeIds = Arrays.asList("p1", "p2", "p3", "p4");
        Map<String, PBFTNode> nodes = new HashMap<>();
        
        for (String nodeId : nodeIds) {
            nodes.put(nodeId, new PBFTNode(nodeId, nodeIds));
        }
        
        for (String nodeId : nodeIds) {
            PBFTNode node = nodes.get(nodeId);
            for (String peerId : nodeIds) {
                if (!peerId.equals(nodeId)) {
                    node.addPeer(peerId, nodes.get(peerId));
                }
            }
        }
        
        try {
            Thread.sleep(500); // 等待初始化
            
            PBFTNode primary = nodes.get(nodeIds.get(0));
            
            for (int i = 0; i < requestCount; i++) {
                ClientRequest request = new ClientRequest("client", "OP" + i);
                primary.handleClientRequest(request);
                
                if (i % 10 == 0) {
                    Thread.sleep(50); // 避免过载
                }
            }
            
            Thread.sleep(2000); // 等待完成
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            for (PBFTNode node : nodes.values()) {
                node.shutdown();
            }
        }
    }
    
    public static void main(String[] args) {
        testRaftConsensus();
        testPBFTConsensus();
        testConsensusPerformance();
    }
}
```

## 性能对比

### 共识算法特性对比

| 算法 | 容错类型 | 消息复杂度 | 延迟 | 吞吐量 | 网络分区容忍 | 实现复杂度 |
|------|----------|------------|------|--------|-------------|------------|
| Raft | CFT | O(n) | 低 | 高 | 部分 | 低 |
| PBFT | BFT | O(n²) | 高 | 中 | 否 | 高 |
| Paxos | CFT | O(n) | 中 | 中 | 是 | 高 |
| HotStuff | BFT | O(n) | 中 | 高 | 部分 | 中 |

### 性能测试代码

```java
public class ConsensusPerformanceBenchmark {
    
    public static class PerformanceMetrics {
        private long totalLatency = 0;
        private int successCount = 0;
        private int failureCount = 0;
        private long startTime;
        private long endTime;
        
        public void start() {
            startTime = System.currentTimeMillis();
        }
        
        public void end() {
            endTime = System.currentTimeMillis();
        }
        
        public void recordSuccess(long latency) {
            successCount++;
            totalLatency += latency;
        }
        
        public void recordFailure() {
            failureCount++;
        }
        
        public double getAverageLatency() {
            return successCount > 0 ? (double) totalLatency / successCount : 0;
        }
        
        public double getThroughput() {
            long duration = endTime - startTime;
            return duration > 0 ? (double) successCount * 1000 / duration : 0;
        }
        
        public double getSuccessRate() {
            int total = successCount + failureCount;
            return total > 0 ? (double) successCount / total : 0;
        }
        
        public void printResults(String algorithmName) {
            System.out.println("\n" + algorithmName + " 性能指标:");
            System.out.println("成功请求数: " + successCount);
            System.out.println("失败请求数: " + failureCount);
            System.out.println("成功率: " + String.format("%.2f%%", getSuccessRate() * 100));
            System.out.println("平均延迟: " + String.format("%.2f ms", getAverageLatency()));
            System.out.println("吞吐量: " + String.format("%.2f 请求/秒", getThroughput()));
            System.out.println("总耗时: " + (endTime - startTime) + " ms");
        }
    }
    
    public static void benchmarkRaft(int nodeCount, int requestCount) {
        System.out.println("\n=== Raft算法性能基准测试 ===");
        System.out.println("节点数: " + nodeCount + ", 请求数: " + requestCount);
        
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        // 创建节点
        List<String> nodeIds = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            nodeIds.add("raft-node-" + i);
        }
        
        Map<String, RaftNode> nodes = new HashMap<>();
        for (String nodeId : nodeIds) {
            nodes.put(nodeId, new RaftNode(nodeId, nodeIds));
        }
        
        // 建立连接
        for (String nodeId : nodeIds) {
            RaftNode node = nodes.get(nodeId);
            for (String peerId : nodeIds) {
                if (!peerId.equals(nodeId)) {
                    node.addPeer(peerId, nodes.get(peerId));
                }
            }
        }
        
        try {
            // 等待选举完成
            Thread.sleep(2000);
            
            // 找到领导者
            RaftNode leader = null;
            for (RaftNode node : nodes.values()) {
                if (node.getState() == NodeState.LEADER) {
                    leader = node;
                    break;
                }
            }
            
            if (leader == null) {
                System.err.println("未能选出领导者");
                return;
            }
            
            metrics.start();
            
            // 发送请求
            for (int i = 0; i < requestCount; i++) {
                long requestStart = System.currentTimeMillis();
                
                boolean success = leader.submitCommand("BENCHMARK-" + i);
                
                long requestEnd = System.currentTimeMillis();
                
                if (success) {
                    metrics.recordSuccess(requestEnd - requestStart);
                } else {
                    metrics.recordFailure();
                }
                
                // 控制请求速率
                if (i % 50 == 0) {
                    Thread.sleep(10);
                }
            }
            
            // 等待所有请求处理完成
            Thread.sleep(1000);
            
            metrics.end();
            metrics.printResults("Raft (" + nodeCount + "节点)");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            for (RaftNode node : nodes.values()) {
                node.shutdown();
            }
        }
    }
    
    public static void benchmarkPBFT(int nodeCount, int requestCount) {
        System.out.println("\n=== PBFT算法性能基准测试 ===");
        System.out.println("节点数: " + nodeCount + ", 请求数: " + requestCount);
        
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        // 创建节点
        List<String> nodeIds = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            nodeIds.add("pbft-node-" + i);
        }
        
        Map<String, PBFTNode> nodes = new HashMap<>();
        for (String nodeId : nodeIds) {
            nodes.put(nodeId, new PBFTNode(nodeId, nodeIds));
        }
        
        // 建立连接
        for (String nodeId : nodeIds) {
            PBFTNode node = nodes.get(nodeId);
            for (String peerId : nodeIds) {
                if (!peerId.equals(nodeId)) {
                    node.addPeer(peerId, nodes.get(peerId));
                }
            }
        }
        
        try {
            // 等待初始化
            Thread.sleep(1000);
            
            PBFTNode primary = nodes.get(nodeIds.get(0));
            
            metrics.start();
            
            // 发送请求
            for (int i = 0; i < requestCount; i++) {
                long requestStart = System.currentTimeMillis();
                
                ClientRequest request = new ClientRequest("benchmark-client", "BENCHMARK-" + i);
                primary.handleClientRequest(request);
                
                // PBFT需要更多时间处理
                Thread.sleep(20);
                
                long requestEnd = System.currentTimeMillis();
                metrics.recordSuccess(requestEnd - requestStart);
                
                // 控制请求速率
                if (i % 20 == 0) {
                    Thread.sleep(100);
                }
            }
            
            // 等待所有请求处理完成
            Thread.sleep(3000);
            
            metrics.end();
            metrics.printResults("PBFT (" + nodeCount + "节点)");
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            for (PBFTNode node : nodes.values()) {
                node.shutdown();
            }
        }
    }
    
    public static void compareAlgorithms() {
        System.out.println("\n=== 共识算法对比测试 ===");
        
        int[] nodeCounts = {3, 5, 7};
        int requestCount = 50;
        
        for (int nodeCount : nodeCounts) {
            System.out.println("\n--- " + nodeCount + "节点集群对比 ---");
            
            // 测试Raft
            benchmarkRaft(nodeCount, requestCount);
            
            // 等待资源清理
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 测试PBFT（需要至少4个节点）
            if (nodeCount >= 4) {
                benchmarkPBFT(nodeCount, requestCount);
            }
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public static void main(String[] args) {
        compareAlgorithms();
    }
}
```

## 算法选择指南

### 选择决策树

1. **是否需要拜占庭容错？**
   - 是 → 选择BFT算法（PBFT、HotStuff、Tendermint）
   - 否 → 选择CFT算法（Raft、Paxos）

2. **对性能要求如何？**
   - 高吞吐量 → Raft、HotStuff
   - 低延迟 → Raft
   - 平衡 → Paxos

3. **实现复杂度要求？**
   - 简单 → Raft
   - 可接受 → Paxos、HotStuff
   - 复杂可接受 → PBFT

4. **网络环境？**
   - 稳定网络 → Raft、PBFT
   - 不稳定网络 → Paxos
   - 异步网络 → Paxos

### 应用场景推荐

```java
public class ConsensusApplicationGuide {
    
    public enum ApplicationScenario {
        DISTRIBUTED_DATABASE,
        BLOCKCHAIN,
        CONFIGURATION_MANAGEMENT,
        DISTRIBUTED_LOCK,
        MESSAGE_QUEUE,
        FINANCIAL_SYSTEM
    }
    
    public static class AlgorithmRecommendation {
        private final String algorithm;
        private final String reason;
        private final int priority; // 1-5, 5最高
        
        public AlgorithmRecommendation(String algorithm, String reason, int priority) {
            this.algorithm = algorithm;
            this.reason = reason;
            this.priority = priority;
        }
        
        // getters
        public String getAlgorithm() { return algorithm; }
        public String getReason() { return reason; }
        public int getPriority() { return priority; }
    }
    
    public static List<AlgorithmRecommendation> getRecommendations(ApplicationScenario scenario) {
        List<AlgorithmRecommendation> recommendations = new ArrayList<>();
        
        switch (scenario) {
            case DISTRIBUTED_DATABASE:
                recommendations.add(new AlgorithmRecommendation(
                    "Raft", "简单实现，高性能，适合内部系统", 5));
                recommendations.add(new AlgorithmRecommendation(
                    "Paxos", "成熟稳定，网络分区容忍性好", 4));
                recommendations.add(new AlgorithmRecommendation(
                    "PBFT", "需要拜占庭容错时使用", 2));
                break;
                
            case BLOCKCHAIN:
                recommendations.add(new AlgorithmRecommendation(
                    "PBFT", "联盟链首选，拜占庭容错", 5));
                recommendations.add(new AlgorithmRecommendation(
                    "HotStuff", "现代BFT算法，性能更好", 4));
                recommendations.add(new AlgorithmRecommendation(
                    "Tendermint", "专为区块链设计", 4));
                break;
                
            case CONFIGURATION_MANAGEMENT:
                recommendations.add(new AlgorithmRecommendation(
                    "Raft", "简单可靠，配置变更频率低", 5));
                recommendations.add(new AlgorithmRecommendation(
                    "Paxos", "经典选择，久经考验", 3));
                break;
                
            case DISTRIBUTED_LOCK:
                recommendations.add(new AlgorithmRecommendation(
                    "Raft", "强一致性，适合锁服务", 5));
                recommendations.add(new AlgorithmRecommendation(
                    "Paxos", "网络分区时表现更好", 4));
                break;
                
            case MESSAGE_QUEUE:
                recommendations.add(new AlgorithmRecommendation(
                    "Raft", "高吞吐量，顺序保证", 4));
                recommendations.add(new AlgorithmRecommendation(
                    "Paxos", "消息持久化场景", 3));
                break;
                
            case FINANCIAL_SYSTEM:
                recommendations.add(new AlgorithmRecommendation(
                    "PBFT", "金融级安全要求", 5));
                recommendations.add(new AlgorithmRecommendation(
                    "Raft", "内部系统，性能要求高", 3));
                break;
        }
        
        // 按优先级排序
        recommendations.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        return recommendations;
    }
    
    public static void printRecommendations(ApplicationScenario scenario) {
        System.out.println("\n=== " + scenario + " 算法推荐 ===");
        
        List<AlgorithmRecommendation> recommendations = getRecommendations(scenario);
        
        for (int i = 0; i < recommendations.size(); i++) {
            AlgorithmRecommendation rec = recommendations.get(i);
            System.out.println((i + 1) + ". " + rec.getAlgorithm() + 
                              " (优先级: " + rec.getPriority() + "/5)");
            System.out.println("   理由: " + rec.getReason());
        }
    }
    
    public static void main(String[] args) {
        for (ApplicationScenario scenario : ApplicationScenario.values()) {
            printRecommendations(scenario);
        }
    }
}
```

## 最佳实践

### 1. 共识算法优化

```java
public class ConsensusBestPractices {
    
    // 批量处理优化
    public static class BatchProcessor {
        private final List<String> batch = new ArrayList<>();
        private final int batchSize;
        private final long batchTimeoutMs;
        private final ScheduledExecutorService scheduler;
        private ScheduledFuture<?> timeoutTask;
        
        public BatchProcessor(int batchSize, long batchTimeoutMs) {
            this.batchSize = batchSize;
            this.batchTimeoutMs = batchTimeoutMs;
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
        }
        
        public synchronized void addRequest(String request) {
            batch.add(request);
            
            if (batch.size() == 1) {
                // 第一个请求，启动超时定时器
                timeoutTask = scheduler.schedule(this::processBatch, 
                                               batchTimeoutMs, TimeUnit.MILLISECONDS);
            }
            
            if (batch.size() >= batchSize) {
                // 达到批量大小，立即处理
                processBatch();
            }
        }
        
        private synchronized void processBatch() {
            if (batch.isEmpty()) {
                return;
            }
            
            if (timeoutTask != null) {
                timeoutTask.cancel(false);
                timeoutTask = null;
            }
            
            List<String> currentBatch = new ArrayList<>(batch);
            batch.clear();
            
            // 处理批量请求
            System.out.println("处理批量请求，大小: " + currentBatch.size());
            // 这里调用实际的共识算法
        }
        
        public void shutdown() {
            scheduler.shutdown();
        }
    }
    
    // 网络优化
    public static class NetworkOptimizer {
        private final Map<String, Long> nodeLatencies = new ConcurrentHashMap<>();
        private final Map<String, Double> nodeReliabilities = new ConcurrentHashMap<>();
        
        public void recordLatency(String nodeId, long latency) {
            nodeLatencies.put(nodeId, latency);
        }
        
        public void recordReliability(String nodeId, boolean success) {
            nodeReliabilities.compute(nodeId, (k, v) -> {
                if (v == null) {
                    return success ? 1.0 : 0.0;
                }
                // 指数移动平均
                double alpha = 0.1;
                return alpha * (success ? 1.0 : 0.0) + (1 - alpha) * v;
            });
        }
        
        public List<String> selectOptimalNodes(List<String> candidates, int count) {
            return candidates.stream()
                .sorted((a, b) -> {
                    // 综合考虑延迟和可靠性
                    double scoreA = getNodeScore(a);
                    double scoreB = getNodeScore(b);
                    return Double.compare(scoreB, scoreA);
                })
                .limit(count)
                .collect(Collectors.toList());
        }
        
        private double getNodeScore(String nodeId) {
            long latency = nodeLatencies.getOrDefault(nodeId, 100L);
            double reliability = nodeReliabilities.getOrDefault(nodeId, 0.5);
            
            // 分数 = 可靠性 / (延迟权重)
            return reliability / (1 + latency / 100.0);
        }
    }
    
    // 故障检测和恢复
    public static class FailureDetector {
        private final Map<String, Long> lastHeartbeat = new ConcurrentHashMap<>();
        private final Map<String, Integer> failureCount = new ConcurrentHashMap<>();
        private final long heartbeatTimeout = 5000; // 5秒
        private final int maxFailures = 3;
        
        public void recordHeartbeat(String nodeId) {
            lastHeartbeat.put(nodeId, System.currentTimeMillis());
            failureCount.put(nodeId, 0); // 重置失败计数
        }
        
        public boolean isNodeAlive(String nodeId) {
            Long lastTime = lastHeartbeat.get(nodeId);
            if (lastTime == null) {
                return false;
            }
            
            return System.currentTimeMillis() - lastTime < heartbeatTimeout;
        }
        
        public void recordFailure(String nodeId) {
            int count = failureCount.getOrDefault(nodeId, 0) + 1;
            failureCount.put(nodeId, count);
            
            if (count >= maxFailures) {
                System.out.println("节点 " + nodeId + " 被标记为失效");
                // 触发节点移除或恢复流程
            }
        }
        
        public Set<String> getFailedNodes() {
            return failureCount.entrySet().stream()
                .filter(entry -> entry.getValue() >= maxFailures)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        }
        
        public Set<String> getAliveNodes(Set<String> allNodes) {
            return allNodes.stream()
                .filter(this::isNodeAlive)
                .collect(Collectors.toSet());
        }
    }
    
    // 动态配置管理
    public static class DynamicConfiguration {
        private volatile int heartbeatInterval = 50;
        private volatile int electionTimeout = 150;
        private volatile int batchSize = 10;
        private volatile long batchTimeout = 100;
        
        private final Map<String, Object> config = new ConcurrentHashMap<>();
        
        public void updateConfig(String key, Object value) {
            config.put(key, value);
            applyConfig(key, value);
        }
        
        private void applyConfig(String key, Object value) {
            switch (key) {
                case "heartbeat.interval":
                    heartbeatInterval = (Integer) value;
                    System.out.println("更新心跳间隔: " + heartbeatInterval + "ms");
                    break;
                case "election.timeout":
                    electionTimeout = (Integer) value;
                    System.out.println("更新选举超时: " + electionTimeout + "ms");
                    break;
                case "batch.size":
                    batchSize = (Integer) value;
                    System.out.println("更新批量大小: " + batchSize);
                    break;
                case "batch.timeout":
                    batchTimeout = (Long) value;
                    System.out.println("更新批量超时: " + batchTimeout + "ms");
                    break;
            }
        }
        
        // 自适应调整
        public void adaptToNetworkConditions(double averageLatency, double packetLoss) {
            if (averageLatency > 100) {
                // 网络延迟高，增加超时时间
                updateConfig("election.timeout", (int) (electionTimeout * 1.5));
                updateConfig("heartbeat.interval", (int) (heartbeatInterval * 1.2));
            }
            
            if (packetLoss > 0.05) {
                // 丢包率高，减少批量大小
                updateConfig("batch.size", Math.max(1, batchSize / 2));
            }
        }
        
        // getters
        public int getHeartbeatInterval() { return heartbeatInterval; }
        public int getElectionTimeout() { return electionTimeout; }
        public int getBatchSize() { return batchSize; }
        public long getBatchTimeout() { return batchTimeout; }
    }
}
```

### 2. 监控和诊断

```java
public class ConsensusMonitoring {
    
    public static class ConsensusMetrics {
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong successfulRequests = new AtomicLong(0);
        private final AtomicLong failedRequests = new AtomicLong(0);
        private final AtomicLong totalLatency = new AtomicLong(0);
        
        private final Map<String, AtomicLong> nodeMetrics = new ConcurrentHashMap<>();
        private final Queue<Long> recentLatencies = new ConcurrentLinkedQueue<>();
        private final int maxRecentSamples = 1000;
        
        public void recordRequest(long latency, boolean success) {
            totalRequests.incrementAndGet();
            
            if (success) {
                successfulRequests.incrementAndGet();
                totalLatency.addAndGet(latency);
                
                // 记录最近的延迟
                recentLatencies.offer(latency);
                if (recentLatencies.size() > maxRecentSamples) {
                    recentLatencies.poll();
                }
            } else {
                failedRequests.incrementAndGet();
            }
        }
        
        public void recordNodeMetric(String nodeId, String metric) {
            nodeMetrics.computeIfAbsent(nodeId + "." + metric, k -> new AtomicLong(0))
                      .incrementAndGet();
        }
        
        public double getSuccessRate() {
            long total = totalRequests.get();
            return total > 0 ? (double) successfulRequests.get() / total : 0;
        }
        
        public double getAverageLatency() {
            long successful = successfulRequests.get();
            return successful > 0 ? (double) totalLatency.get() / successful : 0;
        }
        
        public double getP95Latency() {
            List<Long> latencies = new ArrayList<>(recentLatencies);
            if (latencies.isEmpty()) {
                return 0;
            }
            
            Collections.sort(latencies);
            int index = (int) (latencies.size() * 0.95);
            return latencies.get(Math.min(index, latencies.size() - 1));
        }
        
        public void printMetrics() {
            System.out.println("\n=== 共识算法性能指标 ===");
            System.out.println("总请求数: " + totalRequests.get());
            System.out.println("成功请求数: " + successfulRequests.get());
            System.out.println("失败请求数: " + failedRequests.get());
            System.out.println("成功率: " + String.format("%.2f%%", getSuccessRate() * 100));
            System.out.println("平均延迟: " + String.format("%.2f ms", getAverageLatency()));
            System.out.println("P95延迟: " + String.format("%.2f ms", getP95Latency()));
            
            System.out.println("\n节点指标:");
            nodeMetrics.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue().get()));
        }
    }
    
    public static class HealthChecker {
        private final Map<String, NodeHealth> nodeHealthMap = new ConcurrentHashMap<>();
        private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        
        public static class NodeHealth {
            private volatile boolean isHealthy = true;
            private volatile long lastCheckTime = System.currentTimeMillis();
            private volatile String lastError = null;
            private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
            
            public boolean isHealthy() { return isHealthy; }
            public long getLastCheckTime() { return lastCheckTime; }
            public String getLastError() { return lastError; }
            public int getConsecutiveFailures() { return consecutiveFailures.get(); }
            
            public void recordSuccess() {
                isHealthy = true;
                lastCheckTime = System.currentTimeMillis();
                lastError = null;
                consecutiveFailures.set(0);
            }
            
            public void recordFailure(String error) {
                lastCheckTime = System.currentTimeMillis();
                lastError = error;
                int failures = consecutiveFailures.incrementAndGet();
                
                if (failures >= 3) {
                    isHealthy = false;
                }
            }
        }
        
        public void startHealthCheck(Set<String> nodeIds, long intervalMs) {
            for (String nodeId : nodeIds) {
                nodeHealthMap.put(nodeId, new NodeHealth());
            }
            
            scheduler.scheduleAtFixedRate(() -> {
                for (String nodeId : nodeIds) {
                    checkNodeHealth(nodeId);
                }
            }, 0, intervalMs, TimeUnit.MILLISECONDS);
        }
        
        private void checkNodeHealth(String nodeId) {
            NodeHealth health = nodeHealthMap.get(nodeId);
            if (health == null) {
                return;
            }
            
            try {
                // 模拟健康检查
                boolean isReachable = pingNode(nodeId);
                
                if (isReachable) {
                    health.recordSuccess();
                } else {
                    health.recordFailure("节点不可达");
                }
                
            } catch (Exception e) {
                health.recordFailure(e.getMessage());
            }
        }
        
        private boolean pingNode(String nodeId) {
            // 模拟网络检查
            return Math.random() > 0.1; // 90%成功率
        }
        
        public Set<String> getHealthyNodes() {
            return nodeHealthMap.entrySet().stream()
                .filter(entry -> entry.getValue().isHealthy())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        }
        
        public Set<String> getUnhealthyNodes() {
            return nodeHealthMap.entrySet().stream()
                .filter(entry -> !entry.getValue().isHealthy())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        }
        
        public void printHealthStatus() {
            System.out.println("\n=== 节点健康状态 ===");
            for (Map.Entry<String, NodeHealth> entry : nodeHealthMap.entrySet()) {
                NodeHealth health = entry.getValue();
                System.out.println(entry.getKey() + ": " + 
                    (health.isHealthy() ? "健康" : "不健康") +
                    ", 连续失败: " + health.getConsecutiveFailures() +
                    (health.getLastError() != null ? ", 错误: " + health.getLastError() : ""));
            }
        }
        
        public void shutdown() {
            scheduler.shutdown();
        }
    }
}
```

## 面试要点

### 高频问题

1. **Raft vs Paxos对比**
   - 算法复杂度差异
   - 性能特点
   - 适用场景
   - 实现难度

2. **拜占庭容错的必要性**
   - 什么是拜占庭故障
   - CFT vs BFT的区别
   - PBFT算法原理
   - 实际应用场景

3. **共识算法的性能优化**
   - 批量处理
   - 网络优化
   - 并行处理
   - 故障快速恢复

### 深入问题

1. **如何设计一个高性能的共识系统？**
   - 架构设计考虑
   - 性能瓶颈分析
   - 优化策略
   - 监控和诊断

2. **在CAP定理约束下如何选择共识算法？**
   - 一致性要求分析
   - 可用性权衡
   - 分区容错处理
   - 实际案例分析

3. **共识算法在分布式数据库中的应用？**
   - 日志复制
   - 元数据管理
   - 分片协调
   - 故障恢复

### 实践经验

1. **生产环境部署经验**
   - 集群规模选择
   - 网络配置优化
   - 监控告警设置
   - 故障处理流程

2. **性能调优经验**
   - 参数调优策略
   - 批量处理优化
   - 网络延迟优化
   - 资源使用优化

3. **常见问题和解决方案**
   - 脑裂问题
   - 网络分区处理
   - 节点故障恢复
   - 数据一致性保证

## 总结

分布式共识算法是分布式系统的核心，主要包括：

1. **CFT算法**：Raft、Paxos，适用于可信环境
2. **BFT算法**：PBFT、HotStuff，适用于不可信环境
3. **性能优化**：批量处理、网络优化、并行处理
4. **监控诊断**：性能指标、健康检查、故障检测
5. **最佳实践**：算法选择、参数调优、运维管理

在实际应用中，需要根据业务需求、安全要求、性能指标和运维能力选择合适的共识算法，并进行针对性的优化和监控。
```

## PBFT算法

### 算法原理

PBFT（Practical Byzantine Fault Tolerance）是一个实用的拜占庭容错算法，能够在存在恶意节点的情况下达成共识。

### 三阶段协议

1. **Pre-prepare阶段**：主节点广播提议
2. **Prepare阶段**：节点验证并广播准备消息
3. **Commit阶段**：节点广播提交消息

### Java实现

```java
public class PBFTConsensus {
    
    // 消息类型
    public enum MessageType {
        PRE_PREPARE, PREPARE, COMMIT, VIEW_CHANGE, NEW_VIEW
    }
    
    // PBFT消息
    public static class PBFTMessage {
        private final MessageType type;
        private final int view;
        private final int sequenceNumber;
        private final String digest;
        private final String senderId;
        private final long timestamp;
        private final String payload;
        
        public PBFTMessage(MessageType type, int view, int sequenceNumber, 
                          String digest, String senderId, String payload) {
            this.type = type;
            this.view = view;
            this.sequenceNumber = sequenceNumber;
            this.digest = digest;
            this.senderId = senderId;
            this.payload = payload;
            this.timestamp = System.currentTimeMillis();
        }
        
        // getters
        public MessageType getType() { return type; }
        public int getView() { return view; }
        public int getSequenceNumber() { return sequenceNumber; }
        public String getDigest() { return digest; }
        public String getSenderId() { return senderId; }
        public long getTimestamp() { return timestamp; }
        public String getPayload() { return payload; }
        
        @Override
        public String toString() {
            return "PBFTMessage{type=" + type + ", view=" + view + 
                   ", seq=" + sequenceNumber + ", sender=" + senderId + "}";
        }
    }
    
    // 请求
    public static class ClientRequest {
        private final String clientId;
        private final String operation;
        private final long timestamp;
        private final String requestId;
        
        public ClientRequest(String clientId, String operation) {
            this.clientId = clientId;
            this.operation = operation;
            this.timestamp = System.currentTimeMillis();
            this.requestId = clientId + "-" + timestamp;
        }
        
        // getters
        public String getClientId() { return clientId; }
        public String getOperation() { return operation; }
        public long getTimestamp() { return timestamp; }
        public String getRequestId() { return requestId; }
        
        public String getDigest() {
            return Integer.toString((clientId + operation + timestamp).hashCode());
        }
    }
}
```

#### PBFT节点实现

```java
public class PBFTNode {
    private final String nodeId;
    private final List<String> clusterNodes;
    private final Map<String, PBFTNode> peers;
    private final int faultTolerance; // f值，最多容忍f个拜占庭节点
    
    // 状态
    private volatile int currentView = 0;
    private volatile int sequenceNumber = 0;
    private volatile String primaryId;
    
    // 消息日志
    private final Map<String, ClientRequest> clientRequests = new ConcurrentHashMap<>();
    private final Map<String, PBFTMessage> prePrepareMessages = new ConcurrentHashMap<>();
    private final Map<String, Set<PBFTMessage>> prepareMessages = new ConcurrentHashMap<>();
    private final Map<String, Set<PBFTMessage>> commitMessages = new ConcurrentHashMap<>();
    
    // 已执行的请求
    private final Set<String> executedRequests = new HashSet<>();
    
    // 定时器
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();
    
    public PBFTNode(String nodeId, List<String> clusterNodes) {
        this.nodeId = nodeId;
        this.clusterNodes = new ArrayList<>(clusterNodes);
        this.peers = new HashMap<>();
        this.faultTolerance = (clusterNodes.size() - 1) / 3;
        
        // 确定主节点（简单的轮换策略）
        this.primaryId = clusterNodes.get(currentView % clusterNodes.size());
        
        System.out.println("PBFT节点 " + nodeId + " 启动，主节点: " + primaryId + 
                          ", 容错数: " + faultTolerance);
    }
    
    public void addPeer(String peerId, PBFTNode peer) {
        peers.put(peerId, peer);
    }
    
    // 客户端请求处理
    public synchronized void handleClientRequest(ClientRequest request) {
        if (!isPrimary()) {
            System.out.println("节点 " + nodeId + " 不是主节点，转发请求到主节点 " + primaryId);
            PBFTNode primary = peers.get(primaryId);
            if (primary != null) {
                primary.handleClientRequest(request);
            }
            return;
        }
        
        // 检查请求是否已处理
        if (executedRequests.contains(request.getRequestId())) {
            System.out.println("请求 " + request.getRequestId() + " 已处理，忽略");
            return;
        }
        
        clientRequests.put(request.getRequestId(), request);
        
        // 开始三阶段协议
        startThreePhaseProtocol(request);
    }
    
    // 开始三阶段协议
    private void startThreePhaseProtocol(ClientRequest request) {
        int currentSeq = ++sequenceNumber;
        
        // Pre-prepare阶段
        PBFTMessage prePrepareMsg = new PBFTMessage(
            MessageType.PRE_PREPARE, currentView, currentSeq, 
            request.getDigest(), nodeId, request.getOperation()
        );
        
        String msgKey = getMessageKey(currentView, currentSeq);
        prePrepareMessages.put(msgKey, prePrepareMsg);
        
        System.out.println("主节点 " + nodeId + " 发送Pre-prepare消息，序号: " + currentSeq);
        
        // 广播Pre-prepare消息
        broadcastMessage(prePrepareMsg);
        
        // 启动超时定时器
        startTimer(msgKey, 5000); // 5秒超时
    }
    
    // 处理PBFT消息
    public synchronized void handlePBFTMessage(PBFTMessage message) {
        switch (message.getType()) {
            case PRE_PREPARE:
                handlePrePrepare(message);
                break;
            case PREPARE:
                handlePrepare(message);
                break;
            case COMMIT:
                handleCommit(message);
                break;
            case VIEW_CHANGE:
                handleViewChange(message);
                break;
            case NEW_VIEW:
                handleNewView(message);
                break;
        }
    }
    
    // 处理Pre-prepare消息
    private void handlePrePrepare(PBFTMessage message) {
        if (isPrimary()) {
            return; // 主节点不处理自己的Pre-prepare消息
        }
        
        // 验证消息
        if (!validatePrePrepare(message)) {
            System.out.println("节点 " + nodeId + " 拒绝无效的Pre-prepare消息");
            return;
        }
        
        String msgKey = getMessageKey(message.getView(), message.getSequenceNumber());
        prePrepareMessages.put(msgKey, message);
        
        // 发送Prepare消息
        PBFTMessage prepareMsg = new PBFTMessage(
            MessageType.PREPARE, message.getView(), message.getSequenceNumber(),
            message.getDigest(), nodeId, message.getPayload()
        );
        
        System.out.println("节点 " + nodeId + " 发送Prepare消息，序号: " + 
                          message.getSequenceNumber());
        
        broadcastMessage(prepareMsg);
        
        // 处理自己的Prepare消息
        handlePrepare(prepareMsg);
    }
    
    // 处理Prepare消息
    private void handlePrepare(PBFTMessage message) {
        String msgKey = getMessageKey(message.getView(), message.getSequenceNumber());
        
        prepareMessages.computeIfAbsent(msgKey, k -> new HashSet<>()).add(message);
        
        // 检查是否收到足够的Prepare消息（2f个）
        if (prepareMessages.get(msgKey).size() >= 2 * faultTolerance && 
            prePrepareMessages.containsKey(msgKey)) {
            
            // 发送Commit消息
            PBFTMessage commitMsg = new PBFTMessage(
                MessageType.COMMIT, message.getView(), message.getSequenceNumber(),
                message.getDigest(), nodeId, message.getPayload()
            );
            
            System.out.println("节点 " + nodeId + " 发送Commit消息，序号: " + 
                              message.getSequenceNumber());
            
            broadcastMessage(commitMsg);
            
            // 处理自己的Commit消息
            handleCommit(commitMsg);
        }
    }
    
    // 处理Commit消息
    private void handleCommit(PBFTMessage message) {
        String msgKey = getMessageKey(message.getView(), message.getSequenceNumber());
        
        commitMessages.computeIfAbsent(msgKey, k -> new HashSet<>()).add(message);
        
        // 检查是否收到足够的Commit消息（2f+1个）
        if (commitMessages.get(msgKey).size() >= 2 * faultTolerance + 1 && 
            prePrepareMessages.containsKey(msgKey)) {
            
            // 执行请求
            executeRequest(message);
            
            // 取消定时器
            cancelTimer(msgKey);
        }
    }
    
    // 执行请求
    private void executeRequest(PBFTMessage message) {
        String requestId = findRequestId(message.getDigest());
        if (requestId != null && !executedRequests.contains(requestId)) {
            executedRequests.add(requestId);
            
            System.out.println("节点 " + nodeId + " 执行请求: " + message.getPayload() + 
                              ", 序号: " + message.getSequenceNumber());
            
            // 这里可以执行实际的业务逻辑
            applyOperation(message.getPayload());
        }
    }
    
    // 应用操作
    private void applyOperation(String operation) {
        // 模拟业务逻辑执行
        System.out.println("节点 " + nodeId + " 应用操作: " + operation);
    }
    
    // 验证Pre-prepare消息
    private boolean validatePrePrepare(PBFTMessage message) {
        // 检查视图
        if (message.getView() != currentView) {
            return false;
        }
        
        // 检查发送者是否是主节点
        if (!message.getSenderId().equals(primaryId)) {
            return false;
        }
        
        // 检查序号
        if (message.getSequenceNumber() <= 0) {
            return false;
        }
        
        // 检查是否已有相同序号的不同消息
        String msgKey = getMessageKey(message.getView(), message.getSequenceNumber());
        PBFTMessage existing = prePrepareMessages.get(msgKey);
        if (existing != null && !existing.getDigest().equals(message.getDigest())) {
            return false;
        }
        
        return true;
    }
    
    // 广播消息
    private void broadcastMessage(PBFTMessage message) {
        for (String peerId : clusterNodes) {
            if (!peerId.equals(nodeId)) {
                PBFTNode peer = peers.get(peerId);
                if (peer != null) {
                    scheduler.submit(() -> peer.handlePBFTMessage(message));
                }
            }
        }
    }
    
    // 视图变更处理
    private void handleViewChange(PBFTMessage message) {
        // 简化的视图变更实现
        System.out.println("节点 " + nodeId + " 处理视图变更消息");
    }
    
    private void handleNewView(PBFTMessage message) {
        // 简化的新视图处理
        System.out.println("节点 " + nodeId + " 处理新视图消息");
    }
    
    // 启动定时器
    private void startTimer(String key, long timeoutMs) {
        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            System.out.println("节点 " + nodeId + " 超时，键: " + key);
            handleTimeout(key);
        }, timeoutMs, TimeUnit.MILLISECONDS);
        
        timers.put(key, timer);
    }
    
    // 取消定时器
    private void cancelTimer(String key) {
        ScheduledFuture<?> timer = timers.remove(key);
        if (timer != null) {
            timer.cancel(false);
        }
    }
    
    // 处理超时
    private void handleTimeout(String key) {
        System.out.println("节点 " + nodeId + " 检测到超时，可能需要视图变更");
        // 这里可以触发视图变更
    }
    
    // 工具方法
    private String getMessageKey(int view, int sequenceNumber) {
        return view + "-" + sequenceNumber;
    }
    
    private String findRequestId(String digest) {
        for (Map.Entry<String, ClientRequest> entry : clientRequests.entrySet()) {
            if (entry.getValue().getDigest().equals(digest)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    private boolean isPrimary() {
        return nodeId.equals(primaryId);
    }
    
    // 获取节点状态
    public String getNodeId() {
        return nodeId;
    }
    
    public int getCurrentView() {
        return currentView;
    }
    
    public String getPrimaryId() {
        return primaryId;
    }
    
    public int getExecutedRequestsCount() {
        return executedRequests.size();
    }
    
    // 打印状态
    public void printStatus() {
        System.out.println("PBFT节点 " + nodeId + " 状态: 视图=" + currentView + 
                          ", 主节点=" + primaryId + 
                          ", 已执行请求数=" + executedRequests.size());
    }
    
    // 关闭节点
    public void shutdown() {
        scheduler.shutdown();
    }
}
```