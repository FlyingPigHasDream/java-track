# Java并发编程模块

## 模块概述

本模块深入讲解Java并发编程的核心概念、技术和最佳实践，涵盖从基础的线程操作到高级的并发工具类，帮助开发者掌握多线程编程的精髓。

## 目录结构

### 📚 核心文档
- [线程基础](thread-basics.md) - 线程创建、生命周期、基本操作
- [线程池](thread-pool.md) - 线程池原理、配置、最佳实践
- [锁机制](locks.md) - synchronized、Lock接口、各种锁类型
- [原子类](atomic-classes.md) - 原子操作、CAS、无锁编程

### 🔧 实践代码
- [examples/](../examples/concurrent/) - 并发编程示例代码
- [demos/](../demos/concurrent/) - 实际应用演示

## 学习目标

### 基础目标
- 理解线程的概念和Java中的线程模型
- 掌握线程的创建方式和生命周期管理
- 了解线程安全问题和解决方案
- 熟练使用synchronized关键字

### 进阶目标
- 深入理解JMM（Java内存模型）
- 掌握各种锁的使用场景和性能特点
- 熟练使用线程池和并发工具类
- 理解CAS原理和原子类的应用

### 高级目标
- 能够设计高性能的并发程序
- 掌握无锁编程技术
- 理解并发框架的设计原理
- 能够进行并发程序的性能调优

## 核心概念

### 1. 并发与并行
```
并发（Concurrency）：多个任务在同一时间段内执行
并行（Parallelism）：多个任务在同一时刻执行

单核CPU：只能实现并发，通过时间片轮转
多核CPU：可以实现真正的并行执行
```

### 2. 线程安全
```
定义：多个线程访问同一资源时，不会产生数据竞争和不一致状态

实现方式：
- 互斥同步（synchronized、Lock）
- 非阻塞同步（CAS、原子类）
- 无同步方案（ThreadLocal、不可变对象）
```

### 3. Java内存模型（JMM）
```
主要特性：
- 原子性（Atomicity）
- 可见性（Visibility）
- 有序性（Ordering）

关键概念：
- 工作内存与主内存
- happens-before规则
- 内存屏障
```

## 重点知识点

### 线程基础
- **线程创建**：Thread类、Runnable接口、Callable接口
- **线程状态**：NEW、RUNNABLE、BLOCKED、WAITING、TIMED_WAITING、TERMINATED
- **线程控制**：start()、join()、interrupt()、sleep()、yield()
- **线程通信**：wait()、notify()、notifyAll()

### 同步机制
- **synchronized关键字**：方法同步、代码块同步、类锁、对象锁
- **volatile关键字**：保证可见性、禁止重排序
- **Lock接口**：ReentrantLock、ReadWriteLock、StampedLock
- **条件变量**：Condition接口的使用

### 线程池
- **核心参数**：corePoolSize、maximumPoolSize、keepAliveTime、workQueue
- **执行流程**：任务提交、线程创建、任务执行、线程回收
- **拒绝策略**：AbortPolicy、CallerRunsPolicy、DiscardPolicy、DiscardOldestPolicy
- **监控调优**：线程池状态监控、参数调优

### 并发工具类
- **CountDownLatch**：等待多个线程完成
- **CyclicBarrier**：同步屏障，等待所有线程到达
- **Semaphore**：信号量，控制并发数量
- **Exchanger**：线程间数据交换

### 原子类
- **基本原子类**：AtomicInteger、AtomicLong、AtomicBoolean
- **数组原子类**：AtomicIntegerArray、AtomicLongArray
- **引用原子类**：AtomicReference、AtomicStampedReference
- **字段更新器**：AtomicIntegerFieldUpdater
- **累加器**：LongAdder、LongAccumulator

### 并发集合
- **ConcurrentHashMap**：线程安全的HashMap
- **CopyOnWriteArrayList**：读多写少场景的List
- **BlockingQueue**：阻塞队列，生产者-消费者模式
- **ConcurrentLinkedQueue**：无锁队列

## 学习路径

### 第一阶段：基础入门（1-2周）
1. 学习线程基础概念和创建方式
2. 理解线程生命周期和状态转换
3. 掌握synchronized的基本使用
4. 了解线程安全问题和解决思路

### 第二阶段：核心技术（2-3周）
1. 深入学习synchronized和volatile
2. 掌握Lock接口和各种锁类型
3. 学习线程池的原理和使用
4. 了解线程间通信机制

### 第三阶段：高级特性（2-3周）
1. 学习原子类和CAS原理
2. 掌握并发工具类的使用
3. 了解并发集合的特点和应用
4. 学习Java内存模型

### 第四阶段：实践应用（1-2周）
1. 设计并发程序解决实际问题
2. 进行性能测试和调优
3. 学习并发框架的设计模式
4. 总结最佳实践

## 实践项目

### 初级项目
1. **多线程下载器**
   - 实现文件的多线程下载
   - 使用线程池管理下载任务
   - 实现下载进度统计

2. **生产者-消费者模式**
   - 使用BlockingQueue实现
   - 支持多个生产者和消费者
   - 实现优雅关闭机制

### 中级项目
1. **线程安全的缓存系统**
   - 使用ConcurrentHashMap实现
   - 支持LRU淘汰策略
   - 实现缓存统计功能

2. **并发Web爬虫**
   - 使用线程池处理爬取任务
   - 实现URL去重和深度控制
   - 支持限流和重试机制

### 高级项目
1. **高性能计数器服务**
   - 使用LongAdder实现高并发计数
   - 支持多维度统计
   - 实现实时监控和报警

2. **无锁数据结构**
   - 实现无锁队列
   - 实现无锁栈
   - 性能对比和分析

## 性能考虑

### 线程创建开销
```java
// 避免频繁创建线程
// 不好的做法
new Thread(() -> doWork()).start();

// 好的做法
ExecutorService executor = Executors.newFixedThreadPool(10);
executor.submit(() -> doWork());
```

### 锁竞争优化
```java
// 减少锁的粒度
// 不好的做法
synchronized(this) {
    updateField1();
    updateField2();
}

// 好的做法
synchronized(lock1) {
    updateField1();
}
synchronized(lock2) {
    updateField2();
}
```

### 选择合适的并发工具
```java
// 高并发计数场景
// AtomicLong vs LongAdder
LongAdder adder = new LongAdder(); // 更适合高并发
AtomicLong atomic = new AtomicLong(); // 适合低并发
```

## 常见问题和解决方案

### 1. 死锁问题
```java
// 问题：锁顺序不一致
public void method1() {
    synchronized(lock1) {
        synchronized(lock2) {
            // 业务逻辑
        }
    }
}

public void method2() {
    synchronized(lock2) {
        synchronized(lock1) {
            // 业务逻辑
        }
    }
}

// 解决：统一锁顺序
public void method1() {
    synchronized(lock1) {
        synchronized(lock2) {
            // 业务逻辑
        }
    }
}

public void method2() {
    synchronized(lock1) {
        synchronized(lock2) {
            // 业务逻辑
        }
    }
}
```

### 2. 内存泄漏
```java
// 问题：ThreadLocal未清理
ThreadLocal<Object> threadLocal = new ThreadLocal<>();

// 解决：及时清理
try {
    threadLocal.set(value);
    // 业务逻辑
} finally {
    threadLocal.remove();
}
```

### 3. 线程池配置不当
```java
// 问题：无界队列可能导致OOM
ExecutorService executor = Executors.newFixedThreadPool(10);

// 解决：使用有界队列
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    10, 20, 60L, TimeUnit.SECONDS,
    new ArrayBlockingQueue<>(1000),
    new ThreadPoolExecutor.CallerRunsPolicy()
);
```

## 调试和监控

### JVM工具
- **jstack**：查看线程堆栈
- **jconsole**：监控线程状态
- **VisualVM**：可视化监控
- **JProfiler**：性能分析

### 代码监控
```java
// 线程池监控
ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
System.out.println("Active threads: " + executor.getActiveCount());
System.out.println("Completed tasks: " + executor.getCompletedTaskCount());
System.out.println("Queue size: " + executor.getQueue().size());
```

### 死锁检测
```java
// 编程方式检测死锁
ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
if (deadlockedThreads != null) {
    ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreads);
    for (ThreadInfo threadInfo : threadInfos) {
        System.out.println("Deadlocked thread: " + threadInfo.getThreadName());
    }
}
```

## 最佳实践

### 1. 线程命名
```java
// 为线程设置有意义的名称
Thread thread = new Thread(() -> doWork(), "WorkerThread-1");
thread.start();

// 线程池中的线程命名
ThreadFactory threadFactory = new ThreadFactoryBuilder()
    .setNameFormat("MyPool-%d")
    .build();
ExecutorService executor = Executors.newFixedThreadPool(10, threadFactory);
```

### 2. 异常处理
```java
// 设置未捕获异常处理器
Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
    logger.error("Uncaught exception in thread " + thread.getName(), exception);
});
```

### 3. 优雅关闭
```java
// 线程池优雅关闭
executor.shutdown();
try {
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
    }
} catch (InterruptedException e) {
    executor.shutdownNow();
    Thread.currentThread().interrupt();
}
```

### 4. 避免共享可变状态
```java
// 使用不可变对象
public final class ImmutablePoint {
    private final int x, y;
    
    public ImmutablePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
}

// 使用ThreadLocal
ThreadLocal<SimpleDateFormat> dateFormat = ThreadLocal.withInitial(
    () -> new SimpleDateFormat("yyyy-MM-dd")
);
```

## 推荐资源

### 书籍
- 《Java并发编程实战》- Brian Goetz
- 《Java并发编程的艺术》- 方腾飞
- 《实战Java高并发程序设计》- 葛一鸣

### 在线资源
- [Oracle Java并发教程](https://docs.oracle.com/javase/tutorial/essential/concurrency/)
- [Java并发编程网](http://ifeve.com/)
- [并发编程博客](https://www.cnblogs.com/dolphin0520/category/602384.html)

### 工具和框架
- **Disruptor**：高性能并发框架
- **Akka**：Actor模型框架
- **RxJava**：响应式编程
- **CompletableFuture**：异步编程

## 面试重点

### 高频问题
1. synchronized和Lock的区别
2. volatile的作用和原理
3. 线程池的工作原理
4. CAS的原理和ABA问题
5. ThreadLocal的实现原理

### 深入问题
1. Java内存模型的happens-before规则
2. 锁升级的过程
3. AQS的实现原理
4. 并发集合的实现原理
5. 分段锁的设计思想

### 实践经验
1. 如何设计高并发系统
2. 线程池参数如何调优
3. 如何排查死锁问题
4. 并发程序的性能测试方法
5. 生产环境的并发问题处理经验

## 总结

Java并发编程是一个复杂而重要的主题，需要理论与实践相结合。通过系统学习本模块的内容，你将能够：

- 深入理解Java并发编程的核心概念
- 熟练使用各种并发工具和技术
- 设计高性能的并发程序
- 解决实际开发中的并发问题
- 在面试中展现扎实的并发编程功底

记住，并发编程需要大量的实践和经验积累，建议多做项目练习，多分析开源框架的并发设计，不断提升自己的并发编程能力。