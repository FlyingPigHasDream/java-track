# 分布式事务算法

## 概述

分布式事务是指跨越多个数据库、消息队列或其他资源管理器的事务。在分布式系统中，保证数据一致性是一个重要挑战，分布式事务算法提供了解决方案。

### 核心概念

- **ACID特性**：原子性(Atomicity)、一致性(Consistency)、隔离性(Isolation)、持久性(Durability)
- **CAP定理**：一致性(Consistency)、可用性(Availability)、分区容错性(Partition tolerance)
- **BASE理论**：基本可用(Basically Available)、软状态(Soft state)、最终一致性(Eventually consistent)

## 两阶段提交协议(2PC)

### 算法原理

两阶段提交协议是最经典的分布式事务算法，包含两个阶段：
1. **准备阶段(Prepare Phase)**：协调者询问所有参与者是否可以提交事务
2. **提交阶段(Commit Phase)**：根据参与者的响应决定提交或回滚

### Java实现

```java
public class TwoPhaseCommitCoordinator {
    private final List<TransactionParticipant> participants;
    private final Map<String, TransactionStatus> transactionStatuses;
    private final ExecutorService executorService;
    
    public TwoPhaseCommitCoordinator(List<TransactionParticipant> participants) {
        this.participants = participants;
        this.transactionStatuses = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(participants.size());
    }
    
    public boolean executeTransaction(String transactionId, TransactionContext context) {
        System.out.println("开始执行分布式事务: " + transactionId);
        
        // 阶段1：准备阶段
        if (!preparePhase(transactionId, context)) {
            System.out.println("准备阶段失败，回滚事务: " + transactionId);
            abortTransaction(transactionId);
            return false;
        }
        
        // 阶段2：提交阶段
        return commitPhase(transactionId);
    }
    
    private boolean preparePhase(String transactionId, TransactionContext context) {
        System.out.println("=== 准备阶段开始 ===");
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (TransactionParticipant participant : participants) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    boolean canCommit = participant.prepare(transactionId, context);
                    System.out.println(participant.getName() + " 准备结果: " + 
                                     (canCommit ? "可以提交" : "无法提交"));
                    return canCommit;
                } catch (Exception e) {
                    System.err.println(participant.getName() + " 准备阶段异常: " + e.getMessage());
                    return false;
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // 等待所有参与者响应
        try {
            for (CompletableFuture<Boolean> future : futures) {
                if (!future.get(5, TimeUnit.SECONDS)) {
                    return false;
                }
            }
            
            transactionStatuses.put(transactionId, TransactionStatus.PREPARED);
            System.out.println("所有参与者准备完成");
            return true;
            
        } catch (Exception e) {
            System.err.println("准备阶段超时或异常: " + e.getMessage());
            return false;
        }
    }
    
    private boolean commitPhase(String transactionId) {
        System.out.println("=== 提交阶段开始 ===");
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (TransactionParticipant participant : participants) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    boolean committed = participant.commit(transactionId);
                    System.out.println(participant.getName() + " 提交结果: " + 
                                     (committed ? "成功" : "失败"));
                    return committed;
                } catch (Exception e) {
                    System.err.println(participant.getName() + " 提交阶段异常: " + e.getMessage());
                    return false;
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // 等待所有参与者提交完成
        boolean allCommitted = true;
        try {
            for (CompletableFuture<Boolean> future : futures) {
                if (!future.get(10, TimeUnit.SECONDS)) {
                    allCommitted = false;
                }
            }
        } catch (Exception e) {
            System.err.println("提交阶段超时或异常: " + e.getMessage());
            allCommitted = false;
        }
        
        if (allCommitted) {
            transactionStatuses.put(transactionId, TransactionStatus.COMMITTED);
            System.out.println("事务提交成功: " + transactionId);
        } else {
            transactionStatuses.put(transactionId, TransactionStatus.FAILED);
            System.out.println("事务提交失败: " + transactionId);
        }
        
        return allCommitted;
    }
    
    private void abortTransaction(String transactionId) {
        System.out.println("=== 回滚事务 ===");
        
        for (TransactionParticipant participant : participants) {
            try {
                participant.abort(transactionId);
                System.out.println(participant.getName() + " 回滚完成");
            } catch (Exception e) {
                System.err.println(participant.getName() + " 回滚异常: " + e.getMessage());
            }
        }
        
        transactionStatuses.put(transactionId, TransactionStatus.ABORTED);
    }
    
    public TransactionStatus getTransactionStatus(String transactionId) {
        return transactionStatuses.get(transactionId);
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}
```

### 事务参与者接口

```java
public interface TransactionParticipant {
    String getName();
    boolean prepare(String transactionId, TransactionContext context);
    boolean commit(String transactionId);
    boolean abort(String transactionId);
}

public class DatabaseParticipant implements TransactionParticipant {
    private final String name;
    private final Map<String, String> preparedData;
    private final Map<String, String> committedData;
    
    public DatabaseParticipant(String name) {
        this.name = name;
        this.preparedData = new ConcurrentHashMap<>();
        this.committedData = new ConcurrentHashMap<>();
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean prepare(String transactionId, TransactionContext context) {
        try {
            // 模拟数据库准备操作
            Thread.sleep(100 + (int)(Math.random() * 200));
            
            // 模拟准备失败的情况（10%概率）
            if (Math.random() < 0.1) {
                System.out.println(name + " 准备失败：资源不足");
                return false;
            }
            
            // 准备数据
            String data = context.getData(name);
            if (data != null) {
                preparedData.put(transactionId, data);
            }
            
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    @Override
    public boolean commit(String transactionId) {
        try {
            // 模拟数据库提交操作
            Thread.sleep(50 + (int)(Math.random() * 100));
            
            String data = preparedData.remove(transactionId);
            if (data != null) {
                committedData.put(transactionId, data);
            }
            
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    @Override
    public boolean abort(String transactionId) {
        // 清理准备的数据
        preparedData.remove(transactionId);
        return true;
    }
    
    public Map<String, String> getCommittedData() {
        return new HashMap<>(committedData);
    }
}
```

### 事务上下文

```java
public class TransactionContext {
    private final Map<String, String> data;
    private final long timestamp;
    
    public TransactionContext() {
        this.data = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public void putData(String key, String value) {
        data.put(key, value);
    }
    
    public String getData(String key) {
        return data.get(key);
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Map<String, String> getAllData() {
        return new HashMap<>(data);
    }
}

public enum TransactionStatus {
    STARTED,
    PREPARED,
    COMMITTED,
    ABORTED,
    FAILED
}
```

## 三阶段提交协议(3PC)

### 算法改进

三阶段提交协议在2PC基础上增加了一个预提交阶段，减少了阻塞时间：
1. **准备阶段(Prepare Phase)**
2. **预提交阶段(Pre-commit Phase)**
3. **提交阶段(Commit Phase)**

```java
public class ThreePhaseCommitCoordinator {
    private final List<TransactionParticipant> participants;
    private final Map<String, TransactionStatus> transactionStatuses;
    private final ExecutorService executorService;
    
    public ThreePhaseCommitCoordinator(List<TransactionParticipant> participants) {
        this.participants = participants;
        this.transactionStatuses = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(participants.size());
    }
    
    public boolean executeTransaction(String transactionId, TransactionContext context) {
        System.out.println("开始执行3PC分布式事务: " + transactionId);
        
        // 阶段1：准备阶段
        if (!preparePhase(transactionId, context)) {
            System.out.println("准备阶段失败，回滚事务: " + transactionId);
            abortTransaction(transactionId);
            return false;
        }
        
        // 阶段2：预提交阶段
        if (!preCommitPhase(transactionId)) {
            System.out.println("预提交阶段失败，回滚事务: " + transactionId);
            abortTransaction(transactionId);
            return false;
        }
        
        // 阶段3：提交阶段
        return commitPhase(transactionId);
    }
    
    private boolean preparePhase(String transactionId, TransactionContext context) {
        System.out.println("=== 3PC准备阶段开始 ===");
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (TransactionParticipant participant : participants) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    boolean canCommit = participant.prepare(transactionId, context);
                    System.out.println(participant.getName() + " 3PC准备结果: " + 
                                     (canCommit ? "可以提交" : "无法提交"));
                    return canCommit;
                } catch (Exception e) {
                    System.err.println(participant.getName() + " 3PC准备阶段异常: " + e.getMessage());
                    return false;
                }
            }, executorService);
            
            futures.add(future);
        }
        
        try {
            for (CompletableFuture<Boolean> future : futures) {
                if (!future.get(5, TimeUnit.SECONDS)) {
                    return false;
                }
            }
            
            transactionStatuses.put(transactionId, TransactionStatus.PREPARED);
            return true;
            
        } catch (Exception e) {
            System.err.println("3PC准备阶段超时或异常: " + e.getMessage());
            return false;
        }
    }
    
    private boolean preCommitPhase(String transactionId) {
        System.out.println("=== 3PC预提交阶段开始 ===");
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (TransactionParticipant participant : participants) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // 在实际实现中，这里会调用参与者的预提交方法
                    // 这里简化为模拟预提交操作
                    Thread.sleep(50);
                    System.out.println(participant.getName() + " 预提交完成");
                    return true;
                } catch (Exception e) {
                    System.err.println(participant.getName() + " 预提交异常: " + e.getMessage());
                    return false;
                }
            }, executorService);
            
            futures.add(future);
        }
        
        try {
            for (CompletableFuture<Boolean> future : futures) {
                if (!future.get(5, TimeUnit.SECONDS)) {
                    return false;
                }
            }
            
            transactionStatuses.put(transactionId, TransactionStatus.PRE_COMMITTED);
            return true;
            
        } catch (Exception e) {
            System.err.println("3PC预提交阶段超时或异常: " + e.getMessage());
            return false;
        }
    }
    
    private boolean commitPhase(String transactionId) {
        System.out.println("=== 3PC提交阶段开始 ===");
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (TransactionParticipant participant : participants) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    boolean committed = participant.commit(transactionId);
                    System.out.println(participant.getName() + " 3PC提交结果: " + 
                                     (committed ? "成功" : "失败"));
                    return committed;
                } catch (Exception e) {
                    System.err.println(participant.getName() + " 3PC提交阶段异常: " + e.getMessage());
                    return false;
                }
            }, executorService);
            
            futures.add(future);
        }
        
        boolean allCommitted = true;
        try {
            for (CompletableFuture<Boolean> future : futures) {
                if (!future.get(10, TimeUnit.SECONDS)) {
                    allCommitted = false;
                }
            }
        } catch (Exception e) {
            System.err.println("3PC提交阶段超时或异常: " + e.getMessage());
            allCommitted = false;
        }
        
        if (allCommitted) {
            transactionStatuses.put(transactionId, TransactionStatus.COMMITTED);
            System.out.println("3PC事务提交成功: " + transactionId);
        } else {
            transactionStatuses.put(transactionId, TransactionStatus.FAILED);
            System.out.println("3PC事务提交失败: " + transactionId);
        }
        
        return allCommitted;
    }
    
    private void abortTransaction(String transactionId) {
        System.out.println("=== 3PC回滚事务 ===");
        
        for (TransactionParticipant participant : participants) {
            try {
                participant.abort(transactionId);
                System.out.println(participant.getName() + " 3PC回滚完成");
            } catch (Exception e) {
                System.err.println(participant.getName() + " 3PC回滚异常: " + e.getMessage());
            }
        }
        
        transactionStatuses.put(transactionId, TransactionStatus.ABORTED);
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}

// 扩展TransactionStatus枚举
enum TransactionStatus {
    STARTED,
    PREPARED,
    PRE_COMMITTED,  // 3PC新增状态
    COMMITTED,
    ABORTED,
    FAILED
}
```

## TCC模式(Try-Confirm-Cancel)

### 算法原理

TCC是一种补偿型事务模式，包含三个阶段：
- **Try阶段**：尝试执行业务操作，预留资源
- **Confirm阶段**：确认执行，提交业务操作
- **Cancel阶段**：取消执行，释放预留资源

```java
public interface TccParticipant {
    String getName();
    boolean tryExecute(String transactionId, TccContext context);
    boolean confirmExecute(String transactionId);
    boolean cancelExecute(String transactionId);
}

public class TccCoordinator {
    private final List<TccParticipant> participants;
    private final Map<String, TccTransactionStatus> transactionStatuses;
    private final ExecutorService executorService;
    
    public TccCoordinator(List<TccParticipant> participants) {
        this.participants = participants;
        this.transactionStatuses = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(participants.size());
    }
    
    public boolean executeTransaction(String transactionId, TccContext context) {
        System.out.println("开始执行TCC事务: " + transactionId);
        
        // Try阶段
        if (!tryPhase(transactionId, context)) {
            System.out.println("Try阶段失败，执行Cancel: " + transactionId);
            cancelPhase(transactionId);
            return false;
        }
        
        // Confirm阶段
        return confirmPhase(transactionId);
    }
    
    private boolean tryPhase(String transactionId, TccContext context) {
        System.out.println("=== TCC Try阶段开始 ===");
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        List<TccParticipant> successParticipants = new ArrayList<>();
        
        for (TccParticipant participant : participants) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    boolean success = participant.tryExecute(transactionId, context);
                    System.out.println(participant.getName() + " Try结果: " + 
                                     (success ? "成功" : "失败"));
                    if (success) {
                        synchronized (successParticipants) {
                            successParticipants.add(participant);
                        }
                    }
                    return success;
                } catch (Exception e) {
                    System.err.println(participant.getName() + " Try阶段异常: " + e.getMessage());
                    return false;
                }
            }, executorService);
            
            futures.add(future);
        }
        
        boolean allSuccess = true;
        try {
            for (CompletableFuture<Boolean> future : futures) {
                if (!future.get(5, TimeUnit.SECONDS)) {
                    allSuccess = false;
                }
            }
        } catch (Exception e) {
            System.err.println("Try阶段超时或异常: " + e.getMessage());
            allSuccess = false;
        }
        
        if (allSuccess) {
            transactionStatuses.put(transactionId, new TccTransactionStatus(TccStatus.TRY_SUCCESS, successParticipants));
        } else {
            transactionStatuses.put(transactionId, new TccTransactionStatus(TccStatus.TRY_FAILED, successParticipants));
        }
        
        return allSuccess;
    }
    
    private boolean confirmPhase(String transactionId) {
        System.out.println("=== TCC Confirm阶段开始 ===");
        
        TccTransactionStatus status = transactionStatuses.get(transactionId);
        if (status == null || status.getStatus() != TccStatus.TRY_SUCCESS) {
            return false;
        }
        
        List<CompletableFuture<Boolean>> futures = new ArrayList<>();
        
        for (TccParticipant participant : status.getSuccessParticipants()) {
            CompletableFuture<Boolean> future = CompletableFuture.supplyAsync(() -> {
                try {
                    boolean confirmed = participant.confirmExecute(transactionId);
                    System.out.println(participant.getName() + " Confirm结果: " + 
                                     (confirmed ? "成功" : "失败"));
                    return confirmed;
                } catch (Exception e) {
                    System.err.println(participant.getName() + " Confirm阶段异常: " + e.getMessage());
                    return false;
                }
            }, executorService);
            
            futures.add(future);
        }
        
        boolean allConfirmed = true;
        try {
            for (CompletableFuture<Boolean> future : futures) {
                if (!future.get(10, TimeUnit.SECONDS)) {
                    allConfirmed = false;
                }
            }
        } catch (Exception e) {
            System.err.println("Confirm阶段超时或异常: " + e.getMessage());
            allConfirmed = false;
        }
        
        if (allConfirmed) {
            status.setStatus(TccStatus.CONFIRMED);
            System.out.println("TCC事务确认成功: " + transactionId);
        } else {
            status.setStatus(TccStatus.CONFIRM_FAILED);
            System.out.println("TCC事务确认失败: " + transactionId);
        }
        
        return allConfirmed;
    }
    
    private void cancelPhase(String transactionId) {
        System.out.println("=== TCC Cancel阶段开始 ===");
        
        TccTransactionStatus status = transactionStatuses.get(transactionId);
        if (status == null) {
            return;
        }
        
        for (TccParticipant participant : status.getSuccessParticipants()) {
            try {
                participant.cancelExecute(transactionId);
                System.out.println(participant.getName() + " Cancel完成");
            } catch (Exception e) {
                System.err.println(participant.getName() + " Cancel异常: " + e.getMessage());
            }
        }
        
        status.setStatus(TccStatus.CANCELLED);
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}
```

### TCC参与者实现

```java
public class AccountTccParticipant implements TccParticipant {
    private final String name;
    private final Map<String, BigDecimal> accounts;
    private final Map<String, BigDecimal> frozenAmounts;
    
    public AccountTccParticipant(String name) {
        this.name = name;
        this.accounts = new ConcurrentHashMap<>();
        this.frozenAmounts = new ConcurrentHashMap<>();
        
        // 初始化账户
        accounts.put("user1", new BigDecimal("1000.00"));
        accounts.put("user2", new BigDecimal("500.00"));
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public boolean tryExecute(String transactionId, TccContext context) {
        try {
            String fromAccount = context.getFromAccount();
            BigDecimal amount = context.getAmount();
            
            // 检查账户余额
            BigDecimal balance = accounts.get(fromAccount);
            if (balance == null || balance.compareTo(amount) < 0) {
                System.out.println(name + " Try失败：账户余额不足");
                return false;
            }
            
            // 冻结金额
            frozenAmounts.put(transactionId, amount);
            accounts.put(fromAccount, balance.subtract(amount));
            
            System.out.println(name + " Try成功：冻结金额 " + amount);
            return true;
            
        } catch (Exception e) {
            System.err.println(name + " Try异常: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean confirmExecute(String transactionId) {
        try {
            BigDecimal frozenAmount = frozenAmounts.remove(transactionId);
            if (frozenAmount != null) {
                System.out.println(name + " Confirm成功：确认转账 " + frozenAmount);
                return true;
            }
            return false;
        } catch (Exception e) {
            System.err.println(name + " Confirm异常: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean cancelExecute(String transactionId) {
        try {
            BigDecimal frozenAmount = frozenAmounts.remove(transactionId);
            if (frozenAmount != null) {
                // 恢复账户余额
                String fromAccount = "user1"; // 简化实现
                BigDecimal currentBalance = accounts.get(fromAccount);
                accounts.put(fromAccount, currentBalance.add(frozenAmount));
                
                System.out.println(name + " Cancel成功：恢复金额 " + frozenAmount);
            }
            return true;
        } catch (Exception e) {
            System.err.println(name + " Cancel异常: " + e.getMessage());
            return false;
        }
    }
    
    public Map<String, BigDecimal> getAccounts() {
        return new HashMap<>(accounts);
    }
}

public class TccContext {
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    
    public TccContext(String fromAccount, String toAccount, BigDecimal amount) {
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
    }
    
    // Getters and Setters
    public String getFromAccount() { return fromAccount; }
    public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }
    
    public String getToAccount() { return toAccount; }
    public void setToAccount(String toAccount) { this.toAccount = toAccount; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}

public class TccTransactionStatus {
    private TccStatus status;
    private List<TccParticipant> successParticipants;
    
    public TccTransactionStatus(TccStatus status, List<TccParticipant> successParticipants) {
        this.status = status;
        this.successParticipants = new ArrayList<>(successParticipants);
    }
    
    public TccStatus getStatus() { return status; }
    public void setStatus(TccStatus status) { this.status = status; }
    
    public List<TccParticipant> getSuccessParticipants() { return successParticipants; }
}

enum TccStatus {
    TRY_SUCCESS,
    TRY_FAILED,
    CONFIRMED,
    CONFIRM_FAILED,
    CANCELLED
}
```

## Saga模式

### 算法原理

Saga模式将长事务分解为多个本地事务，每个本地事务都有对应的补偿操作。有两种实现方式：
- **编排式(Orchestration)**：中央协调器控制事务流程
- **编舞式(Choreography)**：参与者之间通过事件通信

```java
public class SagaOrchestrator {
    private final List<SagaStep> steps;
    private final Map<String, SagaTransactionStatus> transactionStatuses;
    private final ExecutorService executorService;
    
    public SagaOrchestrator(List<SagaStep> steps) {
        this.steps = steps;
        this.transactionStatuses = new ConcurrentHashMap<>();
        this.executorService = Executors.newSingleThreadExecutor();
    }
    
    public boolean executeTransaction(String transactionId, SagaContext context) {
        System.out.println("开始执行Saga事务: " + transactionId);
        
        SagaTransactionStatus status = new SagaTransactionStatus();
        transactionStatuses.put(transactionId, status);
        
        // 顺序执行各个步骤
        for (int i = 0; i < steps.size(); i++) {
            SagaStep step = steps.get(i);
            
            try {
                System.out.println("执行步骤 " + (i + 1) + ": " + step.getName());
                boolean success = step.execute(transactionId, context);
                
                if (success) {
                    status.addCompletedStep(step);
                    System.out.println("步骤 " + (i + 1) + " 执行成功");
                } else {
                    System.out.println("步骤 " + (i + 1) + " 执行失败，开始补偿");
                    compensate(transactionId, status);
                    return false;
                }
                
            } catch (Exception e) {
                System.err.println("步骤 " + (i + 1) + " 执行异常: " + e.getMessage());
                compensate(transactionId, status);
                return false;
            }
        }
        
        status.setStatus(SagaStatus.COMPLETED);
        System.out.println("Saga事务执行成功: " + transactionId);
        return true;
    }
    
    private void compensate(String transactionId, SagaTransactionStatus status) {
        System.out.println("=== 开始Saga补偿 ===");
        
        List<SagaStep> completedSteps = status.getCompletedSteps();
        
        // 逆序执行补偿操作
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep step = completedSteps.get(i);
            
            try {
                System.out.println("补偿步骤: " + step.getName());
                step.compensate(transactionId);
                System.out.println("步骤 " + step.getName() + " 补偿成功");
            } catch (Exception e) {
                System.err.println("步骤 " + step.getName() + " 补偿失败: " + e.getMessage());
                // 补偿失败需要人工介入或重试
            }
        }
        
        status.setStatus(SagaStatus.COMPENSATED);
        System.out.println("Saga补偿完成: " + transactionId);
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}
```

### Saga步骤实现

```java
public interface SagaStep {
    String getName();
    boolean execute(String transactionId, SagaContext context);
    boolean compensate(String transactionId);
}

public class OrderSagaStep implements SagaStep {
    private final Map<String, OrderInfo> orders = new ConcurrentHashMap<>();
    
    @Override
    public String getName() {
        return "创建订单";
    }
    
    @Override
    public boolean execute(String transactionId, SagaContext context) {
        try {
            // 模拟创建订单
            Thread.sleep(100);
            
            OrderInfo order = new OrderInfo(transactionId, context.getUserId(), 
                                          context.getProductId(), context.getAmount());
            orders.put(transactionId, order);
            
            System.out.println("订单创建成功: " + transactionId);
            return true;
            
        } catch (Exception e) {
            System.err.println("订单创建失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean compensate(String transactionId) {
        try {
            OrderInfo order = orders.remove(transactionId);
            if (order != null) {
                System.out.println("订单取消成功: " + transactionId);
            }
            return true;
        } catch (Exception e) {
            System.err.println("订单取消失败: " + e.getMessage());
            return false;
        }
    }
    
    private static class OrderInfo {
        private final String orderId;
        private final String userId;
        private final String productId;
        private final BigDecimal amount;
        
        public OrderInfo(String orderId, String userId, String productId, BigDecimal amount) {
            this.orderId = orderId;
            this.userId = userId;
            this.productId = productId;
            this.amount = amount;
        }
        
        // Getters...
    }
}

public class PaymentSagaStep implements SagaStep {
    private final Map<String, PaymentInfo> payments = new ConcurrentHashMap<>();
    
    @Override
    public String getName() {
        return "处理支付";
    }
    
    @Override
    public boolean execute(String transactionId, SagaContext context) {
        try {
            // 模拟支付处理
            Thread.sleep(200);
            
            // 模拟支付失败（20%概率）
            if (Math.random() < 0.2) {
                System.out.println("支付失败：余额不足");
                return false;
            }
            
            PaymentInfo payment = new PaymentInfo(transactionId, context.getUserId(), context.getAmount());
            payments.put(transactionId, payment);
            
            System.out.println("支付处理成功: " + transactionId);
            return true;
            
        } catch (Exception e) {
            System.err.println("支付处理失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean compensate(String transactionId) {
        try {
            PaymentInfo payment = payments.remove(transactionId);
            if (payment != null) {
                System.out.println("支付退款成功: " + transactionId);
            }
            return true;
        } catch (Exception e) {
            System.err.println("支付退款失败: " + e.getMessage());
            return false;
        }
    }
    
    private static class PaymentInfo {
        private final String paymentId;
        private final String userId;
        private final BigDecimal amount;
        
        public PaymentInfo(String paymentId, String userId, BigDecimal amount) {
            this.paymentId = paymentId;
            this.userId = userId;
            this.amount = amount;
        }
        
        // Getters...
    }
}

public class InventorySagaStep implements SagaStep {
    private final Map<String, Integer> inventory = new ConcurrentHashMap<>();
    private final Map<String, InventoryReservation> reservations = new ConcurrentHashMap<>();
    
    public InventorySagaStep() {
        // 初始化库存
        inventory.put("product1", 100);
        inventory.put("product2", 50);
    }
    
    @Override
    public String getName() {
        return "扣减库存";
    }
    
    @Override
    public boolean execute(String transactionId, SagaContext context) {
        try {
            String productId = context.getProductId();
            int quantity = context.getQuantity();
            
            Integer currentStock = inventory.get(productId);
            if (currentStock == null || currentStock < quantity) {
                System.out.println("库存不足: " + productId);
                return false;
            }
            
            // 扣减库存
            inventory.put(productId, currentStock - quantity);
            reservations.put(transactionId, new InventoryReservation(productId, quantity));
            
            System.out.println("库存扣减成功: " + productId + ", 数量: " + quantity);
            return true;
            
        } catch (Exception e) {
            System.err.println("库存扣减失败: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean compensate(String transactionId) {
        try {
            InventoryReservation reservation = reservations.remove(transactionId);
            if (reservation != null) {
                // 恢复库存
                String productId = reservation.getProductId();
                int quantity = reservation.getQuantity();
                Integer currentStock = inventory.get(productId);
                inventory.put(productId, currentStock + quantity);
                
                System.out.println("库存恢复成功: " + productId + ", 数量: " + quantity);
            }
            return true;
        } catch (Exception e) {
            System.err.println("库存恢复失败: " + e.getMessage());
            return false;
        }
    }
    
    private static class InventoryReservation {
        private final String productId;
        private final int quantity;
        
        public InventoryReservation(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }
        
        public String getProductId() { return productId; }
        public int getQuantity() { return quantity; }
    }
}
```

### Saga上下文和状态

```java
public class SagaContext {
    private String userId;
    private String productId;
    private BigDecimal amount;
    private int quantity;
    
    public SagaContext(String userId, String productId, BigDecimal amount, int quantity) {
        this.userId = userId;
        this.productId = productId;
        this.amount = amount;
        this.quantity = quantity;
    }
    
    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}

public class SagaTransactionStatus {
    private SagaStatus status;
    private List<SagaStep> completedSteps;
    
    public SagaTransactionStatus() {
        this.status = SagaStatus.STARTED;
        this.completedSteps = new ArrayList<>();
    }
    
    public void addCompletedStep(SagaStep step) {
        completedSteps.add(step);
    }
    
    public SagaStatus getStatus() { return status; }
    public void setStatus(SagaStatus status) { this.status = status; }
    
    public List<SagaStep> getCompletedSteps() { return new ArrayList<>(completedSteps); }
}

enum SagaStatus {
    STARTED,
    COMPLETED,
    COMPENSATED,
    FAILED
}

## 测试示例

### 2PC测试

```java
public class TwoPhaseCommitTest {
    public static void main(String[] args) {
        // 创建参与者
        List<TransactionParticipant> participants = Arrays.asList(
            new DatabaseParticipant("数据库A"),
            new DatabaseParticipant("数据库B"),
            new DatabaseParticipant("数据库C")
        );
        
        TwoPhaseCommitCoordinator coordinator = new TwoPhaseCommitCoordinator(participants);
        
        // 创建事务上下文
        TransactionContext context = new TransactionContext();
        context.putData("数据库A", "用户数据更新");
        context.putData("数据库B", "订单数据插入");
        context.putData("数据库C", "库存数据扣减");
        
        // 执行事务
        String transactionId = "tx-" + System.currentTimeMillis();
        boolean success = coordinator.executeTransaction(transactionId, context);
        
        System.out.println("\n=== 事务执行结果 ===");
        System.out.println("事务ID: " + transactionId);
        System.out.println("执行结果: " + (success ? "成功" : "失败"));
        System.out.println("事务状态: " + coordinator.getTransactionStatus(transactionId));
        
        // 查看各参与者的数据
        for (TransactionParticipant participant : participants) {
            if (participant instanceof DatabaseParticipant) {
                DatabaseParticipant dbParticipant = (DatabaseParticipant) participant;
                System.out.println(participant.getName() + " 提交的数据: " + 
                                 dbParticipant.getCommittedData());
            }
        }
        
        coordinator.shutdown();
    }
}
```

### TCC测试

```java
public class TccTest {
    public static void main(String[] args) {
        // 创建TCC参与者
        List<TccParticipant> participants = Arrays.asList(
            new AccountTccParticipant("转出账户服务"),
            new AccountTccParticipant("转入账户服务")
        );
        
        TccCoordinator coordinator = new TccCoordinator(participants);
        
        // 创建TCC上下文
        TccContext context = new TccContext("user1", "user2", new BigDecimal("100.00"));
        
        // 执行TCC事务
        String transactionId = "tcc-" + System.currentTimeMillis();
        boolean success = coordinator.executeTransaction(transactionId, context);
        
        System.out.println("\n=== TCC事务执行结果 ===");
        System.out.println("事务ID: " + transactionId);
        System.out.println("执行结果: " + (success ? "成功" : "失败"));
        
        // 查看账户余额
        for (TccParticipant participant : participants) {
            if (participant instanceof AccountTccParticipant) {
                AccountTccParticipant accountParticipant = (AccountTccParticipant) participant;
                System.out.println(participant.getName() + " 账户余额: " + 
                                 accountParticipant.getAccounts());
            }
        }
        
        coordinator.shutdown();
    }
}
```

### Saga测试

```java
public class SagaTest {
    public static void main(String[] args) {
        // 创建Saga步骤
        List<SagaStep> steps = Arrays.asList(
            new OrderSagaStep(),
            new PaymentSagaStep(),
            new InventorySagaStep()
        );
        
        SagaOrchestrator orchestrator = new SagaOrchestrator(steps);
        
        // 创建Saga上下文
        SagaContext context = new SagaContext("user123", "product1", 
                                             new BigDecimal("99.99"), 2);
        
        // 执行Saga事务
        String transactionId = "saga-" + System.currentTimeMillis();
        boolean success = orchestrator.executeTransaction(transactionId, context);
        
        System.out.println("\n=== Saga事务执行结果 ===");
        System.out.println("事务ID: " + transactionId);
        System.out.println("执行结果: " + (success ? "成功" : "失败"));
        
        orchestrator.shutdown();
    }
}
```

## 性能对比

### 算法特性对比

| 特性 | 2PC | 3PC | TCC | Saga |
|------|-----|-----|-----|------|
| 一致性 | 强一致性 | 强一致性 | 最终一致性 | 最终一致性 |
| 可用性 | 低 | 中 | 高 | 高 |
| 性能 | 低 | 低 | 中 | 高 |
| 复杂度 | 低 | 中 | 高 | 中 |
| 阻塞性 | 阻塞 | 减少阻塞 | 非阻塞 | 非阻塞 |
| 网络开销 | 2轮 | 3轮 | 2轮 | 1轮/步骤 |

### 性能测试

```java
public class DistributedTransactionBenchmark {
    private static final int TRANSACTION_COUNT = 1000;
    private static final int PARTICIPANT_COUNT = 3;
    
    public static void main(String[] args) {
        System.out.println("=== 分布式事务性能测试 ===");
        
        // 测试2PC性能
        benchmark2PC();
        
        // 测试TCC性能
        benchmarkTCC();
        
        // 测试Saga性能
        benchmarkSaga();
    }
    
    private static void benchmark2PC() {
        List<TransactionParticipant> participants = new ArrayList<>();
        for (int i = 0; i < PARTICIPANT_COUNT; i++) {
            participants.add(new DatabaseParticipant("DB-" + i));
        }
        
        TwoPhaseCommitCoordinator coordinator = new TwoPhaseCommitCoordinator(participants);
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        
        for (int i = 0; i < TRANSACTION_COUNT; i++) {
            TransactionContext context = new TransactionContext();
            context.putData("DB-0", "data-" + i);
            
            String transactionId = "2pc-" + i;
            if (coordinator.executeTransaction(transactionId, context)) {
                successCount++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("\n2PC性能测试结果:");
        System.out.println("总事务数: " + TRANSACTION_COUNT);
        System.out.println("成功事务数: " + successCount);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均耗时: " + (duration / (double) TRANSACTION_COUNT) + "ms/事务");
        System.out.println("TPS: " + (TRANSACTION_COUNT * 1000.0 / duration));
        
        coordinator.shutdown();
    }
    
    private static void benchmarkTCC() {
        List<TccParticipant> participants = new ArrayList<>();
        for (int i = 0; i < PARTICIPANT_COUNT; i++) {
            participants.add(new AccountTccParticipant("Account-" + i));
        }
        
        TccCoordinator coordinator = new TccCoordinator(participants);
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        
        for (int i = 0; i < TRANSACTION_COUNT; i++) {
            TccContext context = new TccContext("user1", "user2", new BigDecimal("1.00"));
            
            String transactionId = "tcc-" + i;
            if (coordinator.executeTransaction(transactionId, context)) {
                successCount++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("\nTCC性能测试结果:");
        System.out.println("总事务数: " + TRANSACTION_COUNT);
        System.out.println("成功事务数: " + successCount);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均耗时: " + (duration / (double) TRANSACTION_COUNT) + "ms/事务");
        System.out.println("TPS: " + (TRANSACTION_COUNT * 1000.0 / duration));
        
        coordinator.shutdown();
    }
    
    private static void benchmarkSaga() {
        List<SagaStep> steps = Arrays.asList(
            new OrderSagaStep(),
            new PaymentSagaStep(),
            new InventorySagaStep()
        );
        
        SagaOrchestrator orchestrator = new SagaOrchestrator(steps);
        
        long startTime = System.currentTimeMillis();
        int successCount = 0;
        
        for (int i = 0; i < TRANSACTION_COUNT; i++) {
            SagaContext context = new SagaContext("user" + i, "product1", 
                                                 new BigDecimal("10.00"), 1);
            
            String transactionId = "saga-" + i;
            if (orchestrator.executeTransaction(transactionId, context)) {
                successCount++;
            }
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("\nSaga性能测试结果:");
        System.out.println("总事务数: " + TRANSACTION_COUNT);
        System.out.println("成功事务数: " + successCount);
        System.out.println("总耗时: " + duration + "ms");
        System.out.println("平均耗时: " + (duration / (double) TRANSACTION_COUNT) + "ms/事务");
        System.out.println("TPS: " + (TRANSACTION_COUNT * 1000.0 / duration));
        
        orchestrator.shutdown();
    }
}
```

## 最佳实践

### 1. 算法选择指南

```java
public class TransactionAlgorithmSelector {
    
    public enum TransactionType {
        TWO_PHASE_COMMIT,
        THREE_PHASE_COMMIT,
        TCC,
        SAGA
    }
    
    public static TransactionType selectAlgorithm(TransactionRequirement requirement) {
        // 强一致性要求
        if (requirement.isStrongConsistencyRequired()) {
            if (requirement.getParticipantCount() <= 3 && requirement.isHighAvailabilityRequired()) {
                return TransactionType.THREE_PHASE_COMMIT;
            } else {
                return TransactionType.TWO_PHASE_COMMIT;
            }
        }
        
        // 高性能要求
        if (requirement.isHighPerformanceRequired()) {
            if (requirement.isCompensationSupported()) {
                return TransactionType.SAGA;
            } else {
                return TransactionType.TCC;
            }
        }
        
        // 复杂业务流程
        if (requirement.isComplexWorkflow()) {
            return TransactionType.SAGA;
        }
        
        // 默认选择
        return TransactionType.TCC;
    }
    
    public static class TransactionRequirement {
        private boolean strongConsistencyRequired;
        private boolean highAvailabilityRequired;
        private boolean highPerformanceRequired;
        private boolean compensationSupported;
        private boolean complexWorkflow;
        private int participantCount;
        
        // Constructors, getters and setters...
        public TransactionRequirement(boolean strongConsistencyRequired, 
                                    boolean highAvailabilityRequired,
                                    boolean highPerformanceRequired, 
                                    boolean compensationSupported,
                                    boolean complexWorkflow, 
                                    int participantCount) {
            this.strongConsistencyRequired = strongConsistencyRequired;
            this.highAvailabilityRequired = highAvailabilityRequired;
            this.highPerformanceRequired = highPerformanceRequired;
            this.compensationSupported = compensationSupported;
            this.complexWorkflow = complexWorkflow;
            this.participantCount = participantCount;
        }
        
        // Getters
        public boolean isStrongConsistencyRequired() { return strongConsistencyRequired; }
        public boolean isHighAvailabilityRequired() { return highAvailabilityRequired; }
        public boolean isHighPerformanceRequired() { return highPerformanceRequired; }
        public boolean isCompensationSupported() { return compensationSupported; }
        public boolean isComplexWorkflow() { return complexWorkflow; }
        public int getParticipantCount() { return participantCount; }
    }
}
```

### 2. 超时和重试机制

```java
public class TransactionTimeoutManager {
    private final ScheduledExecutorService scheduler;
    private final Map<String, ScheduledFuture<?>> timeoutTasks;
    
    public TransactionTimeoutManager() {
        this.scheduler = Executors.newScheduledThreadPool(10);
        this.timeoutTasks = new ConcurrentHashMap<>();
    }
    
    public void scheduleTimeout(String transactionId, long timeoutMs, Runnable timeoutAction) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            System.out.println("事务超时: " + transactionId);
            timeoutAction.run();
            timeoutTasks.remove(transactionId);
        }, timeoutMs, TimeUnit.MILLISECONDS);
        
        timeoutTasks.put(transactionId, future);
    }
    
    public void cancelTimeout(String transactionId) {
        ScheduledFuture<?> future = timeoutTasks.remove(transactionId);
        if (future != null) {
            future.cancel(false);
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
}

public class TransactionRetryManager {
    private final int maxRetries;
    private final long retryDelayMs;
    
    public TransactionRetryManager(int maxRetries, long retryDelayMs) {
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }
    
    public boolean executeWithRetry(String operation, Supplier<Boolean> action) {
        int attempts = 0;
        
        while (attempts < maxRetries) {
            try {
                if (action.get()) {
                    return true;
                }
            } catch (Exception e) {
                System.err.println(operation + " 执行异常 (尝试 " + (attempts + 1) + "): " + e.getMessage());
            }
            
            attempts++;
            if (attempts < maxRetries) {
                try {
                    Thread.sleep(retryDelayMs * attempts); // 指数退避
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        
        System.err.println(operation + " 重试失败，已达到最大重试次数: " + maxRetries);
        return false;
    }
}
```

### 3. 监控和日志

```java
public class TransactionMonitor {
    private final Map<String, TransactionMetrics> metrics;
    private final AtomicLong totalTransactions;
    private final AtomicLong successfulTransactions;
    private final AtomicLong failedTransactions;
    
    public TransactionMonitor() {
        this.metrics = new ConcurrentHashMap<>();
        this.totalTransactions = new AtomicLong(0);
        this.successfulTransactions = new AtomicLong(0);
        this.failedTransactions = new AtomicLong(0);
    }
    
    public void recordTransactionStart(String transactionId, String type) {
        totalTransactions.incrementAndGet();
        metrics.put(transactionId, new TransactionMetrics(type, System.currentTimeMillis()));
        System.out.println("[MONITOR] 事务开始: " + transactionId + ", 类型: " + type);
    }
    
    public void recordTransactionEnd(String transactionId, boolean success) {
        TransactionMetrics metric = metrics.remove(transactionId);
        if (metric != null) {
            long duration = System.currentTimeMillis() - metric.getStartTime();
            metric.setDuration(duration);
            metric.setSuccess(success);
            
            if (success) {
                successfulTransactions.incrementAndGet();
            } else {
                failedTransactions.incrementAndGet();
            }
            
            System.out.println("[MONITOR] 事务结束: " + transactionId + 
                             ", 结果: " + (success ? "成功" : "失败") + 
                             ", 耗时: " + duration + "ms");
        }
    }
    
    public void printStatistics() {
        long total = totalTransactions.get();
        long successful = successfulTransactions.get();
        long failed = failedTransactions.get();
        
        System.out.println("\n=== 事务统计信息 ===");
        System.out.println("总事务数: " + total);
        System.out.println("成功事务数: " + successful);
        System.out.println("失败事务数: " + failed);
        System.out.println("成功率: " + (total > 0 ? (successful * 100.0 / total) : 0) + "%");
    }
    
    private static class TransactionMetrics {
        private final String type;
        private final long startTime;
        private long duration;
        private boolean success;
        
        public TransactionMetrics(String type, long startTime) {
            this.type = type;
            this.startTime = startTime;
        }
        
        // Getters and setters
        public String getType() { return type; }
        public long getStartTime() { return startTime; }
        public long getDuration() { return duration; }
        public void setDuration(long duration) { this.duration = duration; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
    }
}
```

## 面试要点

### 高频问题

1. **分布式事务的ACID特性如何保证？**
   - 原子性：通过协调者确保所有参与者要么全部提交，要么全部回滚
   - 一致性：通过两阶段提交等协议保证数据一致性
   - 隔离性：通过锁机制和版本控制实现
   - 持久性：各参与者本地事务的持久性保证

2. **2PC和3PC的区别？**
   - 2PC：两个阶段，存在阻塞问题
   - 3PC：三个阶段，减少阻塞时间，但增加了网络开销
   - 3PC在网络分区时可能出现数据不一致

3. **TCC和Saga的适用场景？**
   - TCC：适用于短事务，需要业务层实现Try/Confirm/Cancel逻辑
   - Saga：适用于长事务，通过补偿机制实现最终一致性

### 深入问题

1. **如何处理分布式事务中的网络分区？**
   ```java
   // 网络分区检测和处理
   public class NetworkPartitionHandler {
       public boolean detectPartition(List<TransactionParticipant> participants) {
           int unreachableCount = 0;
           for (TransactionParticipant participant : participants) {
               if (!isReachable(participant)) {
                   unreachableCount++;
               }
           }
           return unreachableCount > participants.size() / 2;
       }
       
       private boolean isReachable(TransactionParticipant participant) {
           // 实现网络连通性检测
           return true;
       }
   }
   ```

2. **分布式事务的性能优化策略？**
   - 减少参与者数量
   - 使用异步处理
   - 实现事务合并
   - 优化网络通信

3. **如何实现分布式事务的幂等性？**
   ```java
   public class IdempotentTransactionManager {
       private final Set<String> processedTransactions = ConcurrentHashMap.newKeySet();
       
       public boolean isProcessed(String transactionId) {
           return processedTransactions.contains(transactionId);
       }
       
       public void markProcessed(String transactionId) {
           processedTransactions.add(transactionId);
       }
   }
   ```

### 实践经验

1. **生产环境中如何选择分布式事务方案？**
   - 评估一致性要求
   - 考虑性能影响
   - 分析业务复杂度
   - 考虑运维成本

2. **分布式事务的监控指标？**
   - 事务成功率
   - 平均响应时间
   - 超时事务数
   - 补偿执行次数

3. **常见的分布式事务问题及解决方案？**
   - 悬挂事务：实现超时机制
   - 重复提交：实现幂等性
   - 数据不一致：加强监控和告警

## 总结

分布式事务是分布式系统中的核心问题，不同的算法适用于不同的场景：

- **2PC/3PC**：适用于强一致性要求的场景，但性能较低
- **TCC**：适用于对性能有要求且能实现补偿逻辑的场景
- **Saga**：适用于长事务和复杂业务流程的场景

在实际应用中，需要根据具体的业务需求、性能要求和一致性要求来选择合适的分布式事务算法。同时，还需要考虑监控、日志、超时处理等工程实践问题。
```