# Java线程池详解

## 目录
- [线程池概述](#线程池概述)
- [线程池核心参数](#线程池核心参数)
- [线程池工作原理](#线程池工作原理)
- [常见线程池类型](#常见线程池类型)
- [拒绝策略](#拒绝策略)
- [线程池监控](#线程池监控)
- [最佳实践](#最佳实践)
- [面试要点](#面试要点)

## 线程池概述

### 什么是线程池
线程池是一种多线程处理形式，预先创建若干个线程，放入线程池中，当需要使用线程时直接从池中获取，使用完毕后再放回池中。这样可以避免频繁创建和销毁线程的开销。

### 为什么使用线程池

#### 优势
1. **降低资源消耗**: 重复利用已创建的线程，减少线程创建和销毁的开销
2. **提高响应速度**: 任务到达时，无需等待线程创建即可立即执行
3. **提高线程的可管理性**: 统一分配、调优和监控线程
4. **控制并发数**: 避免无限制创建线程导致系统资源耗尽

#### 应用场景
- Web服务器处理请求
- 批量数据处理
- 异步任务执行
- 定时任务调度

### 线程池架构

```
┌─────────────────────────────────────────────────────────┐
│                    ThreadPoolExecutor                   │
├─────────────────────────────────────────────────────────┤
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐  │
│  │   Worker    │    │   Worker    │    │   Worker    │  │
│  │  Thread 1   │    │  Thread 2   │    │  Thread n   │  │
│  └─────────────┘    └─────────────┘    └─────────────┘  │
├─────────────────────────────────────────────────────────┤
│                   BlockingQueue                         │
│  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐      │
│  │Task1│ │Task2│ │Task3│ │Task4│ │Task5│ │ ... │      │
│  └─────┘ └─────┘ └─────┘ └─────┘ └─────┘ └─────┘      │
└─────────────────────────────────────────────────────────┘
```

## 线程池核心参数

### ThreadPoolExecutor构造参数

```java
public ThreadPoolExecutor(
    int corePoolSize,                   // 核心线程数
    int maximumPoolSize,                // 最大线程数
    long keepAliveTime,                 // 空闲线程存活时间
    TimeUnit unit,                      // 时间单位
    BlockingQueue<Runnable> workQueue,  // 工作队列
    ThreadFactory threadFactory,        // 线程工厂
    RejectedExecutionHandler handler    // 拒绝策略
)
```

### 参数详解

#### 1. corePoolSize (核心线程数)
- 线程池中始终保持的线程数量
- 即使线程空闲也不会被回收
- 可以通过`allowCoreThreadTimeOut(true)`允许核心线程超时

#### 2. maximumPoolSize (最大线程数)
- 线程池允许创建的最大线程数
- 当队列满了且当前线程数小于最大线程数时，会创建新线程

#### 3. keepAliveTime (空闲线程存活时间)
- 非核心线程空闲时的最大存活时间
- 超过这个时间的空闲线程会被回收

#### 4. workQueue (工作队列)
- 存储等待执行任务的队列
- 常见类型：ArrayBlockingQueue、LinkedBlockingQueue、SynchronousQueue

#### 5. threadFactory (线程工厂)
- 用于创建新线程的工厂
- 可以自定义线程名称、优先级、守护线程等属性

#### 6. handler (拒绝策略)
- 当线程池和队列都满了时的处理策略

### 参数配置示例

```java
public class ThreadPoolParameterExample {
    public static void main(String[] args) {
        // 自定义线程工厂
        ThreadFactory customThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "CustomPool-Thread-" + threadNumber.getAndIncrement());
                thread.setDaemon(false); // 设置为非守护线程
                thread.setPriority(Thread.NORM_PRIORITY);
                return thread;
            }
        };
        
        // 创建线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2,                              // 核心线程数
            4,                              // 最大线程数
            60L,                            // 空闲线程存活时间
            TimeUnit.SECONDS,               // 时间单位
            new ArrayBlockingQueue<>(10),   // 有界队列，容量10
            customThreadFactory,            // 自定义线程工厂
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );
        
        // 提交任务
        for (int i = 0; i < 20; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed by " + Thread.currentThread().getName());
                try {
                    Thread.sleep(2000); // 模拟任务执行
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // 关闭线程池
        executor.shutdown();
    }
}
```

## 线程池工作原理

### 任务提交流程

```
任务提交
    ↓
核心线程数是否已满？
    ↓ No
创建核心线程执行任务
    ↓ Yes
工作队列是否已满？
    ↓ No
任务加入工作队列
    ↓ Yes
是否达到最大线程数？
    ↓ No
创建非核心线程执行任务
    ↓ Yes
执行拒绝策略
```

### 详细执行流程

```java
public class ThreadPoolWorkflowExample {
    public static void main(String[] args) {
        // 创建线程池：核心2，最大4，队列容量3
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(3),
            new ThreadPoolExecutor.AbortPolicy()
        );
        
        // 监控线程池状态
        ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
        monitor.scheduleAtFixedRate(() -> {
            System.out.printf("Pool Size: %d, Active: %d, Queue Size: %d, Completed: %d%n",
                executor.getPoolSize(),
                executor.getActiveCount(),
                executor.getQueue().size(),
                executor.getCompletedTaskCount());
        }, 0, 1, TimeUnit.SECONDS);
        
        // 提交任务观察执行流程
        for (int i = 1; i <= 10; i++) {
            final int taskId = i;
            try {
                executor.submit(() -> {
                    System.out.println("Task " + taskId + " started by " + Thread.currentThread().getName());
                    try {
                        Thread.sleep(5000); // 模拟长时间任务
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    System.out.println("Task " + taskId + " completed");
                });
                System.out.println("Task " + taskId + " submitted");
                Thread.sleep(500); // 间隔提交
            } catch (RejectedExecutionException e) {
                System.out.println("Task " + taskId + " rejected: " + e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // 关闭监控和线程池
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        monitor.shutdown();
        executor.shutdown();
    }
}
```

## 常见线程池类型

### 1. FixedThreadPool (固定大小线程池)

```java
public class FixedThreadPoolExample {
    public static void main(String[] args) {
        // 创建固定大小的线程池
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // 等价于
        // ThreadPoolExecutor executor = new ThreadPoolExecutor(
        //     3, 3, 0L, TimeUnit.MILLISECONDS,
        //     new LinkedBlockingQueue<Runnable>()
        // );
        
        // 提交任务
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed by " + Thread.currentThread().getName());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
    }
}
```

**特点**:
- 核心线程数 = 最大线程数
- 使用无界队列LinkedBlockingQueue
- 适用于负载较重的服务器

### 2. CachedThreadPool (缓存线程池)

```java
public class CachedThreadPoolExample {
    public static void main(String[] args) {
        // 创建缓存线程池
        ExecutorService executor = Executors.newCachedThreadPool();
        
        // 等价于
        // ThreadPoolExecutor executor = new ThreadPoolExecutor(
        //     0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
        //     new SynchronousQueue<Runnable>()
        // );
        
        // 提交大量短任务
        for (int i = 0; i < 20; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed by " + Thread.currentThread().getName());
                try {
                    Thread.sleep(100); // 短任务
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
    }
}
```

**特点**:
- 核心线程数为0，最大线程数为Integer.MAX_VALUE
- 使用SynchronousQueue，不存储任务
- 适用于执行大量短期异步任务

### 3. SingleThreadExecutor (单线程池)

```java
public class SingleThreadExecutorExample {
    public static void main(String[] args) {
        // 创建单线程池
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        // 等价于
        // ThreadPoolExecutor executor = new ThreadPoolExecutor(
        //     1, 1, 0L, TimeUnit.MILLISECONDS,
        //     new LinkedBlockingQueue<Runnable>()
        // );
        
        // 提交任务，保证顺序执行
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed by " + Thread.currentThread().getName());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
    }
}
```

**特点**:
- 只有一个工作线程
- 保证任务按提交顺序执行
- 适用于需要顺序执行的任务

### 4. ScheduledThreadPool (定时线程池)

```java
public class ScheduledThreadPoolExample {
    public static void main(String[] args) {
        // 创建定时线程池
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
        
        // 延迟执行
        executor.schedule(() -> {
            System.out.println("Delayed task executed after 3 seconds");
        }, 3, TimeUnit.SECONDS);
        
        // 固定频率执行
        executor.scheduleAtFixedRate(() -> {
            System.out.println("Fixed rate task: " + new Date());
        }, 0, 2, TimeUnit.SECONDS);
        
        // 固定延迟执行
        executor.scheduleWithFixedDelay(() -> {
            System.out.println("Fixed delay task: " + new Date());
            try {
                Thread.sleep(1000); // 模拟任务执行时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, 1, 3, TimeUnit.SECONDS);
        
        // 运行10秒后关闭
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
    }
}
```

**特点**:
- 支持定时和周期性任务执行
- 核心线程数固定，最大线程数为Integer.MAX_VALUE
- 使用DelayedWorkQueue

### 5. WorkStealingPool (工作窃取线程池)

```java
public class WorkStealingPoolExample {
    public static void main(String[] args) {
        // 创建工作窃取线程池
        ExecutorService executor = Executors.newWorkStealingPool();
        
        // 提交不同执行时间的任务
        List<Future<String>> futures = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            Future<String> future = executor.submit(() -> {
                int sleepTime = (taskId % 3 + 1) * 1000; // 1-3秒不等
                Thread.sleep(sleepTime);
                return "Task " + taskId + " completed in " + sleepTime + "ms by " + Thread.currentThread().getName();
            });
            futures.add(future);
        }
        
        // 获取结果
        for (Future<String> future : futures) {
            try {
                System.out.println(future.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
    }
}
```

**特点**:
- 基于ForkJoinPool实现
- 支持工作窃取算法，提高CPU利用率
- 适用于CPU密集型任务

## 拒绝策略

### 内置拒绝策略

#### 1. AbortPolicy (默认策略)
```java
public class AbortPolicyExample {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1),
            new ThreadPoolExecutor.AbortPolicy() // 抛出异常
        );
        
        try {
            // 提交3个任务，第3个会被拒绝
            executor.submit(() -> sleep(3000));
            executor.submit(() -> sleep(3000));
            executor.submit(() -> sleep(3000)); // 抛出RejectedExecutionException
        } catch (RejectedExecutionException e) {
            System.out.println("Task rejected: " + e.getMessage());
        }
        
        executor.shutdown();
    }
    
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

#### 2. CallerRunsPolicy (调用者运行)
```java
public class CallerRunsPolicyExample {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1),
            new ThreadPoolExecutor.CallerRunsPolicy() // 调用者线程执行
        );
        
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed by " + Thread.currentThread().getName());
                sleep(2000);
            });
        }
        
        executor.shutdown();
    }
    
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

#### 3. DiscardPolicy (静默丢弃)
```java
public class DiscardPolicyExample {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1),
            new ThreadPoolExecutor.DiscardPolicy() // 静默丢弃
        );
        
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed");
                sleep(2000);
            });
            System.out.println("Task " + taskId + " submitted");
        }
        
        executor.shutdown();
    }
    
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

#### 4. DiscardOldestPolicy (丢弃最老任务)
```java
public class DiscardOldestPolicyExample {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(2),
            new ThreadPoolExecutor.DiscardOldestPolicy() // 丢弃最老的任务
        );
        
        for (int i = 0; i < 6; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed");
                sleep(2000);
            });
            System.out.println("Task " + taskId + " submitted");
        }
        
        executor.shutdown();
    }
    
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 自定义拒绝策略

```java
public class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        // 记录日志
        System.err.println("Task " + r.toString() + " rejected from " + executor.toString());
        
        // 可以选择不同的处理方式：
        // 1. 放入备用队列
        // 2. 写入数据库
        // 3. 发送到消息队列
        // 4. 降级处理
        
        // 这里选择在单独线程中执行
        new Thread(r, "Rejected-Task-Thread").start();
    }
}

public class CustomRejectionPolicyExample {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1),
            new CustomRejectedExecutionHandler()
        );
        
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Task " + taskId + " executed by " + Thread.currentThread().getName());
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
    }
}
```

## 线程池监控

### 监控指标

```java
public class ThreadPoolMonitor {
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService monitor;
    
    public ThreadPoolMonitor(ThreadPoolExecutor executor) {
        this.executor = executor;
        this.monitor = Executors.newScheduledThreadPool(1);
    }
    
    public void startMonitoring() {
        monitor.scheduleAtFixedRate(this::printStats, 0, 5, TimeUnit.SECONDS);
    }
    
    private void printStats() {
        System.out.println("=== Thread Pool Statistics ===");
        System.out.println("Pool Size: " + executor.getPoolSize());
        System.out.println("Core Pool Size: " + executor.getCorePoolSize());
        System.out.println("Maximum Pool Size: " + executor.getMaximumPoolSize());
        System.out.println("Active Count: " + executor.getActiveCount());
        System.out.println("Largest Pool Size: " + executor.getLargestPoolSize());
        System.out.println("Task Count: " + executor.getTaskCount());
        System.out.println("Completed Task Count: " + executor.getCompletedTaskCount());
        System.out.println("Queue Size: " + executor.getQueue().size());
        System.out.println("Queue Remaining Capacity: " + executor.getQueue().remainingCapacity());
        System.out.println("Is Shutdown: " + executor.isShutdown());
        System.out.println("Is Terminated: " + executor.isTerminated());
        System.out.println("Is Terminating: " + executor.isTerminating());
        System.out.println("==============================\n");
    }
    
    public void stopMonitoring() {
        monitor.shutdown();
    }
    
    public static void main(String[] args) throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        ThreadPoolMonitor monitor = new ThreadPoolMonitor(executor);
        monitor.startMonitoring();
        
        // 提交任务
        for (int i = 0; i < 20; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(3000);
                    System.out.println("Task " + taskId + " completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // 运行30秒
        Thread.sleep(30000);
        
        executor.shutdown();
        monitor.stopMonitoring();
    }
}
```

### JMX监控

```java
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;

public class ThreadPoolJMXMonitor {
    public interface ThreadPoolMXBean {
        int getPoolSize();
        int getActiveCount();
        long getCompletedTaskCount();
        long getTaskCount();
        int getQueueSize();
    }
    
    public static class ThreadPoolMXBeanImpl implements ThreadPoolMXBean {
        private final ThreadPoolExecutor executor;
        
        public ThreadPoolMXBeanImpl(ThreadPoolExecutor executor) {
            this.executor = executor;
        }
        
        @Override
        public int getPoolSize() {
            return executor.getPoolSize();
        }
        
        @Override
        public int getActiveCount() {
            return executor.getActiveCount();
        }
        
        @Override
        public long getCompletedTaskCount() {
            return executor.getCompletedTaskCount();
        }
        
        @Override
        public long getTaskCount() {
            return executor.getTaskCount();
        }
        
        @Override
        public int getQueueSize() {
            return executor.getQueue().size();
        }
    }
    
    public static void registerMBean(ThreadPoolExecutor executor, String name) {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName = new ObjectName("com.example:type=ThreadPool,name=" + name);
            ThreadPoolMXBeanImpl mbean = new ThreadPoolMXBeanImpl(executor);
            server.registerMBean(mbean, objectName);
            System.out.println("ThreadPool MBean registered: " + objectName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## 最佳实践

### 1. 合理设置线程池参数

```java
public class ThreadPoolBestPractices {
    
    // CPU密集型任务
    public static ThreadPoolExecutor createCpuIntensivePool() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
            corePoolSize,
            corePoolSize,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "CPU-Pool-" + threadNumber.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
    
    // IO密集型任务
    public static ThreadPoolExecutor createIoIntensivePool() {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        return new ThreadPoolExecutor(
            corePoolSize,
            corePoolSize * 2,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "IO-Pool-" + threadNumber.getAndIncrement());
                    t.setDaemon(false);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
```

### 2. 优雅关闭线程池

```java
public class GracefulShutdown {
    public static void shutdownGracefully(ExecutorService executor) {
        executor.shutdown(); // 不再接受新任务
        
        try {
            // 等待已提交任务完成
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                System.out.println("线程池未能在60秒内正常关闭，强制关闭");
                executor.shutdownNow(); // 强制关闭
                
                // 等待强制关闭完成
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("线程池未能正常终止");
                }
            }
        } catch (InterruptedException e) {
            System.out.println("等待线程池关闭时被中断，强制关闭");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 提交一些任务
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(3000);
                    System.out.println("Task " + taskId + " completed");
                } catch (InterruptedException e) {
                    System.out.println("Task " + taskId + " interrupted");
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // 优雅关闭
        shutdownGracefully(executor);
    }
}
```

### 3. 异常处理

```java
public class ThreadPoolExceptionHandling {
    public static void main(String[] args) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
            2, 4, 60L, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(10),
            new ThreadFactory() {
                private final AtomicInteger threadNumber = new AtomicInteger(1);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "Pool-" + threadNumber.getAndIncrement());
                    // 设置未捕获异常处理器
                    t.setUncaughtExceptionHandler((thread, ex) -> {
                        System.err.println("Thread " + thread.getName() + " threw exception: " + ex.getMessage());
                        ex.printStackTrace();
                    });
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        // 提交可能抛出异常的任务
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            
            // 使用submit提交，可以捕获异常
            Future<?> future = executor.submit(() -> {
                if (taskId == 2) {
                    throw new RuntimeException("Task " + taskId + " failed");
                }
                System.out.println("Task " + taskId + " completed successfully");
            });
            
            // 检查任务执行结果
            try {
                future.get(); // 这里会抛出ExecutionException包装原始异常
            } catch (ExecutionException e) {
                System.err.println("Task " + taskId + " execution failed: " + e.getCause().getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        executor.shutdown();
    }
}
```

### 4. 线程池隔离

```java
public class ThreadPoolIsolation {
    // 不同业务使用不同的线程池
    private static final ExecutorService userServicePool = 
        Executors.newFixedThreadPool(10, r -> new Thread(r, "UserService-"));
    
    private static final ExecutorService orderServicePool = 
        Executors.newFixedThreadPool(10, r -> new Thread(r, "OrderService-"));
    
    private static final ExecutorService notificationPool = 
        Executors.newFixedThreadPool(5, r -> new Thread(r, "Notification-"));
    
    public static void processUserRequest(Runnable task) {
        userServicePool.submit(task);
    }
    
    public static void processOrderRequest(Runnable task) {
        orderServicePool.submit(task);
    }
    
    public static void sendNotification(Runnable task) {
        notificationPool.submit(task);
    }
    
    public static void shutdown() {
        userServicePool.shutdown();
        orderServicePool.shutdown();
        notificationPool.shutdown();
    }
}
```

## 面试要点

### 高频问题

1. **线程池的核心参数有哪些？**
   - corePoolSize: 核心线程数
   - maximumPoolSize: 最大线程数
   - keepAliveTime: 空闲线程存活时间
   - workQueue: 工作队列
   - threadFactory: 线程工厂
   - handler: 拒绝策略

2. **线程池的执行流程？**
   - 核心线程数未满 → 创建核心线程
   - 核心线程数已满 → 任务入队
   - 队列已满 → 创建非核心线程
   - 达到最大线程数 → 执行拒绝策略

3. **常见的线程池类型？**
   - FixedThreadPool: 固定大小
   - CachedThreadPool: 缓存线程池
   - SingleThreadExecutor: 单线程
   - ScheduledThreadPool: 定时任务

4. **拒绝策略有哪些？**
   - AbortPolicy: 抛出异常
   - CallerRunsPolicy: 调用者执行
   - DiscardPolicy: 静默丢弃
   - DiscardOldestPolicy: 丢弃最老任务

### 深入问题

1. **为什么不建议使用Executors创建线程池？**
   - FixedThreadPool和SingleThreadExecutor使用无界队列，可能导致OOM
   - CachedThreadPool最大线程数为Integer.MAX_VALUE，可能创建大量线程

2. **如何合理设置线程池参数？**
   - CPU密集型：线程数 = CPU核心数
   - IO密集型：线程数 = CPU核心数 × 2
   - 混合型：根据具体情况调优

3. **线程池如何处理异常？**
   - submit方法：异常被包装在Future中
   - execute方法：异常由UncaughtExceptionHandler处理

4. **线程池的监控指标？**
   - 活跃线程数、队列长度、完成任务数、拒绝任务数等

### 实践经验
- 根据业务特点选择合适的线程池类型
- 合理设置核心参数，避免资源浪费或不足
- 实现完善的监控和告警机制
- 优雅关闭线程池，避免任务丢失
- 不同业务使用独立的线程池，避免相互影响

## 总结

线程池是Java并发编程中的重要工具，正确使用线程池可以显著提高应用程序的性能和稳定性：

1. **理解核心参数**：掌握各参数的作用和配置原则
2. **选择合适类型**：根据业务场景选择最适合的线程池
3. **监控和调优**：建立完善的监控体系，持续优化参数
4. **异常处理**：正确处理任务执行中的异常
5. **优雅关闭**：确保应用关闭时线程池能够正确清理资源

通过深入理解线程池的原理和最佳实践，可以构建高性能、高可用的并发应用程序。