# Java锁机制详解

## 目录
- [锁的概述](#锁的概述)
- [synchronized关键字](#synchronized关键字)
- [volatile关键字](#volatile关键字)
- [Lock接口](#lock接口)
- [ReentrantLock](#reentrantlock)
- [ReadWriteLock](#readwritelock)
- [StampedLock](#stampedlock)
- [锁的分类](#锁的分类)
- [锁优化技术](#锁优化技术)
- [死锁问题](#死锁问题)
- [最佳实践](#最佳实践)
- [面试要点](#面试要点)

## 锁的概述

### 什么是锁
锁是一种同步机制，用于控制多个线程对共享资源的访问，确保在同一时刻只有一个线程能够访问被保护的资源，从而保证线程安全。

### 为什么需要锁

#### 并发问题
1. **竞态条件**: 多个线程同时访问和修改共享数据
2. **数据不一致**: 读取到中间状态的数据
3. **内存可见性**: 一个线程的修改对其他线程不可见
4. **指令重排序**: 编译器和处理器的优化可能改变执行顺序

#### 示例：不使用锁的问题

```java
public class UnsafeCounter {
    private int count = 0;
    
    public void increment() {
        count++; // 非原子操作：读取 -> 计算 -> 写入
    }
    
    public int getCount() {
        return count;
    }
    
    public static void main(String[] args) throws InterruptedException {
        UnsafeCounter counter = new UnsafeCounter();
        
        // 创建1000个线程，每个线程执行1000次increment
        Thread[] threads = new Thread[1000];
        for (int i = 0; i < 1000; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment();
                }
            });
            threads[i].start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 期望结果：1000000，实际结果：小于1000000
        System.out.println("Final count: " + counter.getCount());
    }
}
```

### 锁的基本特性

1. **互斥性**: 同一时刻只有一个线程能持有锁
2. **可见性**: 释放锁时，修改对其他线程可见
3. **有序性**: 防止指令重排序
4. **可重入性**: 同一线程可以多次获取同一把锁

## synchronized关键字

### 基本用法

#### 1. 同步方法

```java
public class SynchronizedMethodExample {
    private int count = 0;
    
    // 实例方法同步 - 锁对象是this
    public synchronized void increment() {
        count++;
    }
    
    // 静态方法同步 - 锁对象是Class对象
    public static synchronized void staticMethod() {
        System.out.println("Static synchronized method");
    }
    
    public synchronized int getCount() {
        return count;
    }
}
```

#### 2. 同步代码块

```java
public class SynchronizedBlockExample {
    private int count = 0;
    private final Object lock = new Object();
    
    public void increment() {
        synchronized (this) { // 使用this作为锁
            count++;
        }
    }
    
    public void incrementWithCustomLock() {
        synchronized (lock) { // 使用自定义对象作为锁
            count++;
        }
    }
    
    public void incrementWithClassLock() {
        synchronized (SynchronizedBlockExample.class) { // 使用Class对象作为锁
            count++;
        }
    }
    
    public int getCount() {
        synchronized (this) {
            return count;
        }
    }
}
```

### synchronized的实现原理

#### 字节码层面

```java
public class SynchronizedBytecode {
    private int count = 0;
    
    public void increment() {
        synchronized (this) {
            count++;
        }
    }
}

// 对应的字节码（简化版）：
// monitorenter  // 获取锁
// iload_0       // 加载this
// dup           // 复制栈顶
// getfield count // 获取count字段
// iconst_1      // 压入常数1
// iadd          // 加法
// putfield count // 设置count字段
// monitorexit   // 释放锁
```

#### JVM层面的锁优化

```java
public class SynchronizedOptimization {
    private int count = 0;
    
    // 偏向锁示例
    public synchronized void biasedLock() {
        // 只有一个线程访问，JVM会使用偏向锁
        count++;
    }
    
    // 轻量级锁示例
    public synchronized void lightweightLock() {
        // 少量线程竞争，JVM会使用轻量级锁
        count++;
    }
    
    // 重量级锁示例
    public synchronized void heavyweightLock() {
        // 大量线程竞争，JVM会升级为重量级锁
        try {
            Thread.sleep(100); // 模拟长时间持有锁
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        count++;
    }
}
```

### synchronized的特点

1. **自动释放**: 无论正常退出还是异常退出都会释放锁
2. **不可中断**: 等待锁的线程不能被中断
3. **非公平**: 不保证等待时间最长的线程优先获得锁
4. **可重入**: 同一线程可以多次获取同一把锁

```java
public class SynchronizedReentrant {
    public synchronized void method1() {
        System.out.println("Method1");
        method2(); // 可重入，不会死锁
    }
    
    public synchronized void method2() {
        System.out.println("Method2");
    }
}
```

## volatile关键字

### volatile的作用

1. **保证可见性**: 修改立即对其他线程可见
2. **禁止指令重排序**: 保证有序性
3. **不保证原子性**: 复合操作仍需要同步

### volatile使用示例

```java
public class VolatileExample {
    private volatile boolean flag = false;
    private volatile int count = 0;
    
    // 线程1执行
    public void writer() {
        count = 42;
        flag = true; // volatile写，确保count的修改对其他线程可见
    }
    
    // 线程2执行
    public void reader() {
        if (flag) { // volatile读
            System.out.println("Count: " + count); // 保证能看到42
        }
    }
    
    // 错误用法：volatile不保证原子性
    public void wrongIncrement() {
        count++; // 非原子操作，仍然不安全
    }
    
    // 正确用法：简单的状态标记
    public void correctUsage() {
        flag = !flag; // 简单赋值操作是安全的
    }
}
```

### volatile的实现原理

#### 内存屏障

```java
public class VolatileMemoryBarrier {
    private volatile int volatileVar = 0;
    private int normalVar = 0;
    
    public void write() {
        normalVar = 1;     // 普通写
        volatileVar = 2;   // volatile写，插入StoreStore和StoreLoad屏障
    }
    
    public void read() {
        int temp1 = volatileVar; // volatile读，插入LoadLoad和LoadStore屏障
        int temp2 = normalVar;   // 普通读
    }
}
```

### volatile vs synchronized

| 特性 | volatile | synchronized |
|------|----------|-------------|
| 原子性 | 不保证 | 保证 |
| 可见性 | 保证 | 保证 |
| 有序性 | 部分保证 | 保证 |
| 阻塞性 | 非阻塞 | 阻塞 |
| 适用场景 | 状态标记 | 复合操作 |

```java
public class VolatileVsSynchronized {
    private volatile boolean volatileFlag = false;
    private boolean syncFlag = false;
    private volatile int volatileCount = 0;
    private int syncCount = 0;
    
    // volatile适用场景：简单状态标记
    public void setVolatileFlag(boolean flag) {
        volatileFlag = flag; // 原子操作，volatile足够
    }
    
    public boolean getVolatileFlag() {
        return volatileFlag;
    }
    
    // synchronized适用场景：复合操作
    public synchronized void setSyncFlag(boolean flag) {
        syncFlag = flag;
    }
    
    public synchronized boolean getSyncFlag() {
        return syncFlag;
    }
    
    // volatile不适用：非原子操作
    public void wrongVolatileIncrement() {
        volatileCount++; // 仍然不安全
    }
    
    // synchronized适用：复合操作
    public synchronized void correctSyncIncrement() {
        syncCount++; // 安全
    }
}
```

## Lock接口

### Lock接口概述

Lock接口提供了比synchronized更灵活的锁操作，支持可中断、可超时、非阻塞的锁获取。

```java
public interface Lock {
    void lock();                                    // 获取锁
    void lockInterruptibly() throws InterruptedException; // 可中断获取锁
    boolean tryLock();                             // 尝试获取锁
    boolean tryLock(long time, TimeUnit unit) throws InterruptedException; // 超时获取锁
    void unlock();                                 // 释放锁
    Condition newCondition();                      // 创建条件变量
}
```

### Lock vs synchronized

| 特性 | Lock | synchronized |
|------|------|-------------|
| 灵活性 | 高 | 低 |
| 可中断 | 支持 | 不支持 |
| 超时 | 支持 | 不支持 |
| 非阻塞 | 支持 | 不支持 |
| 条件变量 | 多个 | 一个 |
| 自动释放 | 手动 | 自动 |

### Lock基本使用模式

```java
public class LockBasicUsage {
    private final Lock lock = new ReentrantLock();
    private int count = 0;
    
    // 基本使用模式
    public void increment() {
        lock.lock();
        try {
            count++;
        } finally {
            lock.unlock(); // 必须在finally中释放锁
        }
    }
    
    // 可中断锁
    public void incrementInterruptibly() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            count++;
        } finally {
            lock.unlock();
        }
    }
    
    // 尝试获取锁
    public boolean tryIncrement() {
        if (lock.tryLock()) {
            try {
                count++;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
    
    // 超时获取锁
    public boolean incrementWithTimeout(long timeout, TimeUnit unit) throws InterruptedException {
        if (lock.tryLock(timeout, unit)) {
            try {
                count++;
                return true;
            } finally {
                lock.unlock();
            }
        }
        return false;
    }
    
    public int getCount() {
        lock.lock();
        try {
            return count;
        } finally {
            lock.unlock();
        }
    }
}
```

## ReentrantLock

### ReentrantLock特性

1. **可重入**: 同一线程可以多次获取同一把锁
2. **公平性**: 支持公平锁和非公平锁
3. **可中断**: 支持中断等待锁的线程
4. **条件变量**: 支持多个条件变量

### 公平锁 vs 非公平锁

```java
public class FairVsUnfairLock {
    // 非公平锁（默认）
    private final ReentrantLock unfairLock = new ReentrantLock();
    
    // 公平锁
    private final ReentrantLock fairLock = new ReentrantLock(true);
    
    public void testUnfairLock() {
        unfairLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " acquired unfair lock");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            unfairLock.unlock();
        }
    }
    
    public void testFairLock() {
        fairLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " acquired fair lock");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            fairLock.unlock();
        }
    }
    
    public static void main(String[] args) {
        FairVsUnfairLock example = new FairVsUnfairLock();
        
        // 测试非公平锁
        System.out.println("=== Unfair Lock ===");
        for (int i = 0; i < 5; i++) {
            new Thread(() -> example.testUnfairLock(), "Thread-" + i).start();
        }
        
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 测试公平锁
        System.out.println("\n=== Fair Lock ===");
        for (int i = 0; i < 5; i++) {
            new Thread(() -> example.testFairLock(), "Thread-" + i).start();
        }
    }
}
```

### 可重入性示例

```java
public class ReentrantExample {
    private final ReentrantLock lock = new ReentrantLock();
    private int count = 0;
    
    public void method1() {
        lock.lock();
        try {
            System.out.println("Method1: Hold count = " + lock.getHoldCount());
            count++;
            method2(); // 重入
        } finally {
            lock.unlock();
        }
    }
    
    public void method2() {
        lock.lock();
        try {
            System.out.println("Method2: Hold count = " + lock.getHoldCount());
            count++;
            method3(); // 再次重入
        } finally {
            lock.unlock();
        }
    }
    
    public void method3() {
        lock.lock();
        try {
            System.out.println("Method3: Hold count = " + lock.getHoldCount());
            count++;
        } finally {
            lock.unlock();
        }
    }
    
    public static void main(String[] args) {
        ReentrantExample example = new ReentrantExample();
        example.method1();
        System.out.println("Final count: " + example.count);
    }
}
```

### Condition条件变量

```java
public class ConditionExample {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Condition notFull = lock.newCondition();
    private final Object[] items = new Object[10];
    private int putIndex = 0, takeIndex = 0, count = 0;
    
    // 生产者
    public void put(Object item) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length) {
                notFull.await(); // 队列满，等待
            }
            items[putIndex] = item;
            if (++putIndex == items.length) {
                putIndex = 0;
            }
            count++;
            System.out.println("Produced: " + item + ", count: " + count);
            notEmpty.signal(); // 通知消费者
        } finally {
            lock.unlock();
        }
    }
    
    // 消费者
    public Object take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) {
                notEmpty.await(); // 队列空，等待
            }
            Object item = items[takeIndex];
            items[takeIndex] = null;
            if (++takeIndex == items.length) {
                takeIndex = 0;
            }
            count--;
            System.out.println("Consumed: " + item + ", count: " + count);
            notFull.signal(); // 通知生产者
            return item;
        } finally {
            lock.unlock();
        }
    }
    
    public static void main(String[] args) {
        ConditionExample queue = new ConditionExample();
        
        // 启动生产者线程
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    queue.put("Item-" + i);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // 启动消费者线程
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    queue.take();
                    Thread.sleep(150);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

## ReadWriteLock

### ReadWriteLock概述

ReadWriteLock维护一对锁：读锁和写锁。多个线程可以同时持有读锁，但写锁是排他的。

```java
public interface ReadWriteLock {
    Lock readLock();   // 返回读锁
    Lock writeLock();  // 返回写锁
}
```

### ReentrantReadWriteLock使用示例

```java
public class ReadWriteLockExample {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private final Map<String, String> cache = new HashMap<>();
    
    // 读操作
    public String get(String key) {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " reading: " + key);
            Thread.sleep(100); // 模拟读操作耗时
            return cache.get(key);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            readLock.unlock();
        }
    }
    
    // 写操作
    public void put(String key, String value) {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " writing: " + key + "=" + value);
            Thread.sleep(200); // 模拟写操作耗时
            cache.put(key, value);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            writeLock.unlock();
        }
    }
    
    // 获取所有数据（读操作）
    public Map<String, String> getAll() {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " reading all data");
            return new HashMap<>(cache);
        } finally {
            readLock.unlock();
        }
    }
    
    // 清空缓存（写操作）
    public void clear() {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " clearing cache");
            cache.clear();
        } finally {
            writeLock.unlock();
        }
    }
    
    public static void main(String[] args) {
        ReadWriteLockExample cache = new ReadWriteLockExample();
        
        // 初始化数据
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        
        // 启动多个读线程
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                for (int j = 0; j < 3; j++) {
                    cache.get("key1");
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "Reader-" + i).start();
        }
        
        // 启动写线程
        new Thread(() -> {
            for (int i = 0; i < 2; i++) {
                cache.put("key3", "value3-" + i);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "Writer-1").start();
    }
}
```

### 锁降级示例

```java
public class LockDowngradeExample {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private volatile boolean cacheValid = false;
    private String data;
    
    public void processData() {
        readLock.lock();
        try {
            if (!cacheValid) {
                // 需要更新数据，释放读锁，获取写锁
                readLock.unlock();
                writeLock.lock();
                try {
                    // 双重检查
                    if (!cacheValid) {
                        data = loadDataFromDatabase(); // 模拟从数据库加载数据
                        cacheValid = true;
                        System.out.println("Data loaded: " + data);
                    }
                    // 锁降级：在释放写锁之前获取读锁
                    readLock.lock();
                } finally {
                    writeLock.unlock(); // 释放写锁
                }
            }
            
            // 使用数据（持有读锁）
            useData(data);
        } finally {
            readLock.unlock();
        }
    }
    
    private String loadDataFromDatabase() {
        try {
            Thread.sleep(1000); // 模拟数据库查询
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Data from database at " + System.currentTimeMillis();
    }
    
    private void useData(String data) {
        System.out.println(Thread.currentThread().getName() + " using data: " + data);
    }
    
    public void invalidateCache() {
        writeLock.lock();
        try {
            cacheValid = false;
            System.out.println("Cache invalidated");
        } finally {
            writeLock.unlock();
        }
    }
    
    public static void main(String[] args) {
        LockDowngradeExample example = new LockDowngradeExample();
        
        // 启动多个线程访问数据
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                example.processData();
            }, "Thread-" + i).start();
        }
        
        // 2秒后使缓存失效
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                example.invalidateCache();
                Thread.sleep(1000);
                example.processData();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Invalidator").start();
    }
}
```

## StampedLock

### StampedLock概述

StampedLock是Java 8引入的新锁，提供了三种锁模式：
1. **写锁**: 排他锁
2. **悲观读锁**: 共享锁
3. **乐观读锁**: 无锁读取

### StampedLock使用示例

```java
public class StampedLockExample {
    private final StampedLock stampedLock = new StampedLock();
    private double x, y;
    
    // 写操作
    public void write(double newX, double newY) {
        long stamp = stampedLock.writeLock();
        try {
            System.out.println(Thread.currentThread().getName() + " writing: (" + newX + ", " + newY + ")");
            x = newX;
            y = newY;
            Thread.sleep(100); // 模拟写操作耗时
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            stampedLock.unlockWrite(stamp);
        }
    }
    
    // 乐观读
    public double distanceFromOrigin() {
        long stamp = stampedLock.tryOptimisticRead();
        double curX = x, curY = y;
        
        if (!stampedLock.validate(stamp)) {
            // 乐观读失败，升级为悲观读
            stamp = stampedLock.readLock();
            try {
                curX = x;
                curY = y;
                System.out.println(Thread.currentThread().getName() + " upgraded to pessimistic read");
            } finally {
                stampedLock.unlockRead(stamp);
            }
        } else {
            System.out.println(Thread.currentThread().getName() + " optimistic read succeeded");
        }
        
        return Math.sqrt(curX * curX + curY * curY);
    }
    
    // 悲观读
    public double distanceFromOriginPessimistic() {
        long stamp = stampedLock.readLock();
        try {
            System.out.println(Thread.currentThread().getName() + " pessimistic reading");
            Thread.sleep(50); // 模拟读操作耗时
            return Math.sqrt(x * x + y * y);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 0;
        } finally {
            stampedLock.unlockRead(stamp);
        }
    }
    
    // 读锁升级为写锁
    public void moveIfAtOrigin(double newX, double newY) {
        long stamp = stampedLock.readLock();
        try {
            while (x == 0.0 && y == 0.0) {
                // 尝试升级为写锁
                long writeStamp = stampedLock.tryConvertToWriteLock(stamp);
                if (writeStamp != 0L) {
                    stamp = writeStamp;
                    x = newX;
                    y = newY;
                    System.out.println(Thread.currentThread().getName() + " upgraded to write lock");
                    break;
                } else {
                    // 升级失败，释放读锁，获取写锁
                    stampedLock.unlockRead(stamp);
                    stamp = stampedLock.writeLock();
                }
            }
        } finally {
            stampedLock.unlock(stamp);
        }
    }
    
    public static void main(String[] args) {
        StampedLockExample point = new StampedLockExample();
        
        // 初始化
        point.write(3.0, 4.0);
        
        // 启动多个读线程
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                for (int j = 0; j < 3; j++) {
                    double distance = point.distanceFromOrigin();
                    System.out.println(Thread.currentThread().getName() + " distance: " + distance);
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }, "OptimisticReader-" + i).start();
        }
        
        // 启动悲观读线程
        new Thread(() -> {
            for (int i = 0; i < 2; i++) {
                double distance = point.distanceFromOriginPessimistic();
                System.out.println(Thread.currentThread().getName() + " distance: " + distance);
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "PessimisticReader").start();
        
        // 启动写线程
        new Thread(() -> {
            try {
                Thread.sleep(300);
                point.write(1.0, 1.0);
                Thread.sleep(300);
                point.write(2.0, 2.0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Writer").start();
    }
}
```

## 锁的分类

### 按锁的特性分类

#### 1. 可重入锁 vs 不可重入锁

```java
// 可重入锁示例
public class ReentrantLockDemo {
    private final ReentrantLock lock = new ReentrantLock();
    
    public void method1() {
        lock.lock();
        try {
            System.out.println("Method1");
            method2(); // 可重入
        } finally {
            lock.unlock();
        }
    }
    
    public void method2() {
        lock.lock();
        try {
            System.out.println("Method2");
        } finally {
            lock.unlock();
        }
    }
}

// 不可重入锁示例（自定义实现）
public class NonReentrantLock {
    private boolean isLocked = false;
    private Thread lockingThread = null;
    
    public synchronized void lock() throws InterruptedException {
        while (isLocked) {
            wait();
        }
        isLocked = true;
        lockingThread = Thread.currentThread();
    }
    
    public synchronized void unlock() {
        if (Thread.currentThread() == lockingThread) {
            isLocked = false;
            lockingThread = null;
            notify();
        }
    }
}
```

#### 2. 公平锁 vs 非公平锁

```java
public class FairLockComparison {
    private final ReentrantLock fairLock = new ReentrantLock(true);
    private final ReentrantLock unfairLock = new ReentrantLock(false);
    
    public void fairLockTest() {
        fairLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " acquired fair lock");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            fairLock.unlock();
        }
    }
    
    public void unfairLockTest() {
        unfairLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " acquired unfair lock");
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            unfairLock.unlock();
        }
    }
}
```

#### 3. 共享锁 vs 排他锁

```java
public class SharedVsExclusiveLock {
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();   // 共享锁
    private final Lock writeLock = rwLock.writeLock(); // 排他锁
    private String data = "initial data";
    
    // 共享锁：多个线程可以同时读
    public String read() {
        readLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " reading: " + data);
            Thread.sleep(1000);
            return data;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            readLock.unlock();
        }
    }
    
    // 排他锁：只有一个线程可以写
    public void write(String newData) {
        writeLock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + " writing: " + newData);
            this.data = newData;
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            writeLock.unlock();
        }
    }
}
```

### 按锁的实现分类

#### 1. 自旋锁

```java
public class SpinLock {
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    
    public void lock() {
        Thread currentThread = Thread.currentThread();
        // 自旋等待
        while (!owner.compareAndSet(null, currentThread)) {
            // 空循环，消耗CPU
        }
    }
    
    public void unlock() {
        Thread currentThread = Thread.currentThread();
        owner.compareAndSet(currentThread, null);
    }
}

public class SpinLockExample {
    private final SpinLock spinLock = new SpinLock();
    private int count = 0;
    
    public void increment() {
        spinLock.lock();
        try {
            count++;
            System.out.println(Thread.currentThread().getName() + " count: " + count);
        } finally {
            spinLock.unlock();
        }
    }
    
    public static void main(String[] args) {
        SpinLockExample example = new SpinLockExample();
        
        for (int i = 0; i < 5; i++) {
            new Thread(() -> {
                for (int j = 0; j < 3; j++) {
                    example.increment();
                }
            }, "Thread-" + i).start();
        }
    }
}
```

#### 2. 适应性自旋锁

```java
public class AdaptiveSpinLock {
    private final AtomicReference<Thread> owner = new AtomicReference<>();
    private volatile int spinCount = 0;
    private static final int MAX_SPIN_COUNT = 1000;
    
    public void lock() {
        Thread currentThread = Thread.currentThread();
        int spins = 0;
        
        while (!owner.compareAndSet(null, currentThread)) {
            spins++;
            if (spins > spinCount && spins > MAX_SPIN_COUNT) {
                // 自旋次数过多，让出CPU
                Thread.yield();
                spins = 0;
            }
        }
        
        // 更新自旋次数
        spinCount = (spinCount + spins) / 2;
    }
    
    public void unlock() {
        Thread currentThread = Thread.currentThread();
        owner.compareAndSet(currentThread, null);
    }
}
```

## 锁优化技术

### JVM锁优化

#### 1. 锁消除

```java
public class LockElimination {
    // JVM会消除这个锁，因为sb是局部变量，不会被其他线程访问
    public String concatString(String s1, String s2, String s3) {
        StringBuffer sb = new StringBuffer();
        sb.append(s1); // StringBuffer的append方法是同步的
        sb.append(s2);
        sb.append(s3);
        return sb.toString();
    }
    
    // 优化后等价于
    public String concatStringOptimized(String s1, String s2, String s3) {
        StringBuilder sb = new StringBuilder(); // 使用非同步的StringBuilder
        sb.append(s1);
        sb.append(s2);
        sb.append(s3);
        return sb.toString();
    }
}
```

#### 2. 锁粗化

```java
public class LockCoarsening {
    private final Object lock = new Object();
    
    // 原始代码：频繁加锁解锁
    public void originalCode() {
        synchronized (lock) {
            // 操作1
        }
        synchronized (lock) {
            // 操作2
        }
        synchronized (lock) {
            // 操作3
        }
    }
    
    // JVM优化后：锁粗化
    public void optimizedCode() {
        synchronized (lock) {
            // 操作1
            // 操作2
            // 操作3
        }
    }
}
```

#### 3. 偏向锁

```java
public class BiasedLockExample {
    private int count = 0;
    
    // 只有一个线程访问，JVM使用偏向锁
    public synchronized void increment() {
        count++;
    }
    
    public static void main(String[] args) {
        BiasedLockExample example = new BiasedLockExample();
        
        // 单线程访问，偏向锁生效
        for (int i = 0; i < 1000000; i++) {
            example.increment();
        }
        
        System.out.println("Count: " + example.count);
    }
}
```

### 减少锁竞争的技术

#### 1. 锁分段

```java
public class SegmentedLock {
    private static final int SEGMENT_COUNT = 16;
    private final Object[] locks = new Object[SEGMENT_COUNT];
    private final List<String>[] segments = new List[SEGMENT_COUNT];
    
    public SegmentedLock() {
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            locks[i] = new Object();
            segments[i] = new ArrayList<>();
        }
    }
    
    private int getSegmentIndex(String key) {
        return Math.abs(key.hashCode()) % SEGMENT_COUNT;
    }
    
    public void add(String key, String value) {
        int segmentIndex = getSegmentIndex(key);
        synchronized (locks[segmentIndex]) {
            segments[segmentIndex].add(value);
        }
    }
    
    public boolean remove(String key, String value) {
        int segmentIndex = getSegmentIndex(key);
        synchronized (locks[segmentIndex]) {
            return segments[segmentIndex].remove(value);
        }
    }
    
    public int size() {
        int totalSize = 0;
        // 按顺序获取所有锁，避免死锁
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            synchronized (locks[i]) {
                totalSize += segments[i].size();
            }
        }
        return totalSize;
    }
}
```

#### 2. 锁分离

```java
public class LockSeparation {
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();
    
    // 分离读写操作的锁
    private final Map<String, String> cache = new HashMap<>();
    
    public String get(String key) {
        readLock.lock(); // 读操作使用读锁
        try {
            return cache.get(key);
        } finally {
            readLock.unlock();
        }
    }
    
    public void put(String key, String value) {
        writeLock.lock(); // 写操作使用写锁
        try {
            cache.put(key, value);
        } finally {
            writeLock.unlock();
        }
    }
}
```

## 死锁问题

### 死锁的产生条件

1. **互斥条件**: 资源不能被多个线程同时使用
2. **请求和保持条件**: 线程已获得资源，同时等待其他资源
3. **不剥夺条件**: 资源不能被强制剥夺
4. **环路等待条件**: 存在资源等待环路

### 死锁示例

```java
public class DeadlockExample {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    
    public static void main(String[] args) {
        Thread thread1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("Thread1 acquired lock1");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                System.out.println("Thread1 waiting for lock2");
                synchronized (lock2) {
                    System.out.println("Thread1 acquired lock2");
                }
            }
        });
        
        Thread thread2 = new Thread(() -> {
            synchronized (lock2) {
                System.out.println("Thread2 acquired lock2");
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                System.out.println("Thread2 waiting for lock1");
                synchronized (lock1) {
                    System.out.println("Thread2 acquired lock1");
                }
            }
        });
        
        thread1.start();
        thread2.start();
    }
}
```

### 死锁检测

```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public class DeadlockDetection {
    public static void detectDeadlock() {
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = threadMXBean.findDeadlockedThreads();
        
        if (deadlockedThreads != null) {
            ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(deadlockedThreads);
            System.out.println("Detected deadlock:");
            for (ThreadInfo threadInfo : threadInfos) {
                System.out.println("Thread: " + threadInfo.getThreadName());
                System.out.println("Blocked on: " + threadInfo.getLockName());
                System.out.println("Owned by: " + threadInfo.getLockOwnerName());
                System.out.println();
            }
        } else {
            System.out.println("No deadlock detected");
        }
    }
    
    public static void main(String[] args) {
        // 启动死锁检测线程
        Thread detector = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(5000);
                    detectDeadlock();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        detector.setDaemon(true);
        detector.start();
        
        // 创建可能产生死锁的代码
        // ...
    }
}
```

### 避免死锁的方法

#### 1. 锁排序

```java
public class LockOrdering {
    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    
    // 总是按照相同的顺序获取锁
    public void method1() {
        synchronized (lock1) {
            synchronized (lock2) {
                // 业务逻辑
            }
        }
    }
    
    public void method2() {
        synchronized (lock1) { // 相同的顺序
            synchronized (lock2) {
                // 业务逻辑
            }
        }
    }
}
```

#### 2. 锁超时

```java
public class LockTimeout {
    private final ReentrantLock lock1 = new ReentrantLock();
    private final ReentrantLock lock2 = new ReentrantLock();
    
    public boolean transferMoney(int amount) {
        boolean acquired1 = false, acquired2 = false;
        
        try {
            acquired1 = lock1.tryLock(1, TimeUnit.SECONDS);
            if (!acquired1) {
                return false;
            }
            
            acquired2 = lock2.tryLock(1, TimeUnit.SECONDS);
            if (!acquired2) {
                return false;
            }
            
            // 执行转账操作
            System.out.println("Transfer " + amount + " successfully");
            return true;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (acquired2) lock2.unlock();
            if (acquired1) lock1.unlock();
        }
    }
}
```

#### 3. 开放调用

```java
public class OpenCall {
    private final Set<String> users = new HashSet<>();
    private final Set<String> admins = new HashSet<>();
    
    // 错误的做法：嵌套锁调用
    public synchronized void wrongAddUser(String user) {
        users.add(user);
        notifyAdmins(user); // 在持有锁的情况下调用外部方法
    }
    
    private synchronized void notifyAdmins(String user) {
        for (String admin : admins) {
            // 通知管理员
        }
    }
    
    // 正确的做法：开放调用
    public void correctAddUser(String user) {
        Set<String> adminsCopy;
        synchronized (this) {
            users.add(user);
            adminsCopy = new HashSet<>(admins); // 复制数据
        }
        
        // 在锁外部调用
        for (String admin : adminsCopy) {
            // 通知管理员
        }
    }
}
```

## 最佳实践

### 1. 选择合适的锁

```java
public class LockSelection {
    // 简单的互斥访问：使用synchronized
    private int simpleCounter = 0;
    
    public synchronized void incrementSimple() {
        simpleCounter++;
    }
    
    // 需要超时、中断：使用ReentrantLock
    private final ReentrantLock timeoutLock = new ReentrantLock();
    private int timeoutCounter = 0;
    
    public boolean incrementWithTimeout() {
        try {
            if (timeoutLock.tryLock(1, TimeUnit.SECONDS)) {
                try {
                    timeoutCounter++;
                    return true;
                } finally {
                    timeoutLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }
    
    // 读多写少：使用ReadWriteLock
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Map<String, String> cache = new HashMap<>();
    
    public String get(String key) {
        rwLock.readLock().lock();
        try {
            return cache.get(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    
    public void put(String key, String value) {
        rwLock.writeLock().lock();
        try {
            cache.put(key, value);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
    
    // 高性能读取：使用StampedLock
    private final StampedLock stampedLock = new StampedLock();
    private double x, y;
    
    public double distanceFromOrigin() {
        long stamp = stampedLock.tryOptimisticRead();
        double curX = x, curY = y;
        
        if (!stampedLock.validate(stamp)) {
            stamp = stampedLock.readLock();
            try {
                curX = x;
                curY = y;
            } finally {
                stampedLock.unlockRead(stamp);
            }
        }
        
        return Math.sqrt(curX * curX + curY * curY);
    }
}
```

### 2. 减少锁的持有时间

```java
public class ReduceLockTime {
    private final Object lock = new Object();
    private final Map<String, String> cache = new HashMap<>();
    
    // 错误：锁持有时间过长
    public void wrongMethod(String key) {
        synchronized (lock) {
            String value = cache.get(key);
            
            // 耗时操作在锁内执行
            if (value == null) {
                value = loadFromDatabase(key); // 耗时操作
                cache.put(key, value);
            }
            
            processValue(value); // 另一个耗时操作
        }
    }
    
    // 正确：减少锁持有时间
    public void correctMethod(String key) {
        String value;
        synchronized (lock) {
            value = cache.get(key);
        }
        
        if (value == null) {
            value = loadFromDatabase(key); // 在锁外执行耗时操作
            
            synchronized (lock) {
                // 双重检查
                String existing = cache.get(key);
                if (existing == null) {
                    cache.put(key, value);
                } else {
                    value = existing;
                }
            }
        }
        
        processValue(value); // 在锁外处理
    }
    
    private String loadFromDatabase(String key) {
        // 模拟数据库查询
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "value-" + key;
    }
    
    private void processValue(String value) {
        // 模拟值处理
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 3. 使用并发集合

```java
import java.util.concurrent.*;

public class ConcurrentCollections {
    // 使用ConcurrentHashMap替代synchronized HashMap
    private final ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
    
    // 使用CopyOnWriteArrayList替代synchronized List（读多写少场景）
    private final CopyOnWriteArrayList<String> copyOnWriteList = new CopyOnWriteArrayList<>();
    
    // 使用BlockingQueue替代wait/notify
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    
    public void putInMap(String key, String value) {
        concurrentMap.put(key, value); // 线程安全，无需额外同步
    }
    
    public String getFromMap(String key) {
        return concurrentMap.get(key);
    }
    
    public void addToList(String item) {
        copyOnWriteList.add(item); // 适合读多写少
    }
    
    public void producer(String item) {
        try {
            queue.put(item); // 阻塞式添加
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public String consumer() {
        try {
            return queue.take(); // 阻塞式获取
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
```

## 面试要点

### 高频问题

1. **synchronized和Lock的区别？**
   - synchronized是关键字，Lock是接口
   - synchronized自动释放锁，Lock需要手动释放
   - Lock支持超时、中断、非阻塞获取锁
   - Lock支持公平锁，synchronized是非公平的
   - Lock支持多个条件变量

2. **volatile的作用？**
   - 保证可见性：修改立即对其他线程可见
   - 禁止指令重排序：保证有序性
   - 不保证原子性：复合操作仍需要同步

3. **什么是死锁？如何避免？**
   - 死锁：多个线程相互等待对方释放资源
   - 避免方法：锁排序、锁超时、开放调用

4. **ReentrantLock的特性？**
   - 可重入：同一线程可多次获取
   - 公平性：支持公平锁和非公平锁
   - 可中断：支持中断等待锁的线程
   - 条件变量：支持多个Condition

5. **ReadWriteLock的使用场景？**
   - 读多写少的场景
   - 读操作可以并发执行
   - 写操作是排他的

### 深入问题

1. **synchronized的实现原理？**
   - 基于monitor对象实现
   - 字节码层面使用monitorenter/monitorexit
   - JVM层面有锁优化：偏向锁、轻量级锁、重量级锁

2. **volatile的实现原理？**
   - 基于内存屏障实现
   - 禁止CPU缓存和指令重排序
   - 保证内存可见性

3. **AQS（AbstractQueuedSynchronizer）原理？**
   - ReentrantLock、CountDownLatch等的基础
   - 基于FIFO队列和CAS操作
   - 支持独占和共享两种模式

4. **锁升级过程？**
   - 偏向锁 → 轻量级锁 → 重量级锁
   - 根据竞争程度自动升级
   - 不可逆过程

### 实践经验
- 优先使用高级并发工具（如并发集合）
- 减少锁的粒度和持有时间
- 避免在锁内调用外部方法
- 使用锁排序避免死锁
- 根据场景选择合适的锁类型

## 总结

Java锁机制是并发编程的核心，正确理解和使用锁对于编写高性能、线程安全的程序至关重要：

1. **基础锁机制**：掌握synchronized和volatile的使用
2. **高级锁特性**：理解Lock接口提供的灵活性
3. **读写分离**：在读多写少场景使用ReadWriteLock
4. **性能优化**：了解JVM锁优化和减少锁竞争的技术
5. **死锁预防**：掌握死锁的原因和预防方法
6. **最佳实践**：选择合适的锁，减少锁的粒度和持有时间

通过深入理解锁的原理和最佳实践，可以编写出高效、安全的并发程序。