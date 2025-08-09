# Java并发编程

## 目录

1. [线程基础](./thread-basics.md)
2. [线程池](./thread-pool.md)
3. [锁机制](./locks.md)
4. [并发容器](./concurrent-collections.md)
5. [原子操作](./atomic-operations.md)
6. [volatile关键字](./volatile.md)
7. [JMM内存模型](./jmm.md)
8. [AQS框架](./aqs.md)
9. [并发工具类](./concurrent-utilities.md)
10. [并发设计模式](./concurrent-patterns.md)

## 学习目标

通过本模块的学习，你将掌握：

- Java多线程编程的基础知识
- 线程池的原理和使用
- 各种锁机制的原理和应用场景
- 并发容器的实现原理
- 原子操作的使用
- volatile关键字的作用机制
- Java内存模型的工作原理
- AQS框架的设计思想
- 常用并发工具类的使用
- 并发编程的设计模式

## 核心概念

### 并发 vs 并行
- **并发（Concurrency）**：同一时间段内处理多个任务
- **并行（Parallelism）**：同一时刻处理多个任务

### 线程安全
- **原子性（Atomicity）**：操作不可分割
- **可见性（Visibility）**：一个线程的修改对其他线程可见
- **有序性（Ordering）**：程序执行的顺序按照代码的先后顺序执行

### 常见问题
- **竞态条件（Race Condition）**
- **死锁（Deadlock）**
- **活锁（Livelock）**
- **饥饿（Starvation）**

## 重点知识点

### 线程创建方式
1. 继承Thread类
2. 实现Runnable接口
3. 实现Callable接口
4. 使用线程池

### 同步机制
1. synchronized关键字
2. ReentrantLock
3. ReadWriteLock
4. StampedLock
5. Condition

### 并发工具
1. CountDownLatch
2. CyclicBarrier
3. Semaphore
4. Exchanger
5. Phaser

### 并发容器
1. ConcurrentHashMap
2. CopyOnWriteArrayList
3. BlockingQueue系列
4. ConcurrentSkipListMap

## 学习路径

```
线程基础 → 同步机制 → 线程池 → 并发容器 → 原子操作 → 内存模型 → AQS框架 → 并发工具类 → 设计模式
```

## 实践建议

1. **从简单到复杂**：先掌握基础概念，再学习高级特性
2. **理论结合实践**：每个知识点都要编写代码验证
3. **关注性能**：了解不同并发机制的性能特点
4. **避免常见陷阱**：学习并发编程的最佳实践
5. **阅读源码**：深入理解JUC包的实现原理

## 推荐资源

- 《Java并发编程实战》
- 《Java并发编程的艺术》
- JUC源码
- Doug Lea的论文