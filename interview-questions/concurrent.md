# Java并发编程面试题

## 线程基础

### 1. 创建线程的几种方式？各有什么优缺点？

**答案：**

**1. 继承Thread类：**
```java
class MyThread extends Thread {
    @Override
    public void run() {
        // 线程执行逻辑
    }
}
```
- 优点：简单直接
- 缺点：单继承限制，耦合性高

**2. 实现Runnable接口：**
```java
class MyRunnable implements Runnable {
    @Override
    public void run() {
        // 线程执行逻辑
    }
}
```
- 优点：避免单继承限制，代码解耦
- 缺点：无返回值

**3. 实现Callable接口：**
```java
class MyCallable implements Callable<String> {
    @Override
    public String call() throws Exception {
        return "result";
    }
}
```
- 优点：有返回值，可以抛出异常
- 缺点：需要配合ExecutorService使用

**4. 使用线程池：**
```java
ExecutorService executor = Executors.newFixedThreadPool(5);
executor.submit(() -> {
    // 线程执行逻辑
});
```
- 优点：资源管理，性能优化
- 缺点：相对复杂

### 2. 线程的生命周期？

**答案：**

```
NEW → RUNNABLE → BLOCKED/WAITING/TIMED_WAITING → TERMINATED
```

**状态详解：**
1. **NEW：** 线程创建但未启动
2. **RUNNABLE：** 线程正在运行或等待CPU调度
3. **BLOCKED：** 线程被阻塞等待监视器锁
4. **WAITING：** 线程无限期等待另一个线程执行特定操作
5. **TIMED_WAITING：** 线程等待指定时间
6. **TERMINATED：** 线程执行完毕

**状态转换触发条件：**
- NEW → RUNNABLE：调用start()方法
- RUNNABLE → BLOCKED：等待synchronized锁
- RUNNABLE → WAITING：调用wait()、join()等
- RUNNABLE → TIMED_WAITING：调用sleep()、wait(timeout)等
- 任何状态 → TERMINATED：run()方法执行完毕或异常终止

### 3. run()和start()方法的区别？

**答案：**

**start()方法：**
- 启动新线程，由JVM调用run()方法
- 线程状态从NEW变为RUNNABLE
- 只能调用一次，重复调用抛出IllegalThreadStateException

**run()方法：**
- 线程的执行体，包含线程要执行的代码
- 直接调用run()不会创建新线程，在当前线程中执行
- 可以多次调用

```java
Thread thread = new Thread(() -> {
    System.out.println("当前线程：" + Thread.currentThread().getName());
});

thread.run();   // 输出：当前线程：main
thread.start(); // 输出：当前线程：Thread-0
```

### 4. 如何正确停止一个线程？

**答案：**

**推荐方式：使用中断机制**
```java
public class StoppableThread extends Thread {
    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // 执行任务
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // 收到中断信号，重新设置中断标志
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

// 使用
StoppableThread thread = new StoppableThread();
thread.start();
thread.interrupt(); // 发送中断信号
```

**其他方式：**
1. **使用volatile标志位：**
```java
class StoppableTask implements Runnable {
    private volatile boolean stopped = false;
    
    public void stop() {
        stopped = true;
    }
    
    @Override
    public void run() {
        while (!stopped) {
            // 执行任务
        }
    }
}
```

**不推荐的方式：**
- stop()：已废弃，可能导致数据不一致
- suspend()/resume()：已废弃，可能导致死锁

## 同步机制

### 5. synchronized关键字的原理？

**答案：**

**实现原理：**
- 基于JVM的监视器锁（Monitor）实现
- 每个对象都有一个监视器锁
- 通过monitorenter和monitorexit字节码指令实现

**锁的类型：**
1. **对象锁：** 锁定实例方法或代码块
2. **类锁：** 锁定静态方法或Class对象

**使用方式：**
```java
// 1. 同步方法
public synchronized void method() {
    // 锁定当前对象
}

// 2. 同步静态方法
public static synchronized void staticMethod() {
    // 锁定Class对象
}

// 3. 同步代码块
public void method() {
    synchronized(this) {
        // 锁定当前对象
    }
    
    synchronized(SomeClass.class) {
        // 锁定Class对象
    }
}
```

**锁优化（JDK 1.6+）：**
1. **偏向锁：** 无竞争时的优化
2. **轻量级锁：** 少量竞争时的优化
3. **重量级锁：** 激烈竞争时使用
4. **锁消除：** 编译器优化
5. **锁粗化：** 合并相邻的同步块

### 6. volatile关键字的作用？

**答案：**

**主要作用：**
1. **保证可见性：** 一个线程修改的值对其他线程立即可见
2. **禁止指令重排序：** 保证有序性
3. **不保证原子性：** 复合操作仍需要同步

**实现原理：**
- 通过内存屏障（Memory Barrier）实现
- 写操作时立即刷新到主内存
- 读操作时从主内存读取最新值

**使用场景：**
```java
// 1. 状态标志
private volatile boolean flag = false;

// 2. 双重检查锁定
public class Singleton {
    private static volatile Singleton instance;
    
    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (Singleton.class) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}
```

**注意事项：**
- 不能保证复合操作的原子性
- 性能开销比普通变量大
- 不能用于计数器等需要原子性的场景

### 7. wait()和sleep()的区别？

**答案：**

| 特性 | wait() | sleep() |
|------|--------|----------|
| 所属类 | Object类 | Thread类 |
| 锁的处理 | 释放锁 | 不释放锁 |
| 唤醒方式 | notify()/notifyAll() | 时间到或interrupt() |
| 使用场景 | 线程间通信 | 暂停执行 |
| 调用位置 | 同步块内 | 任何地方 |
| 异常 | InterruptedException | InterruptedException |

**示例：**
```java
// wait()示例
synchronized (obj) {
    while (condition) {
        obj.wait(); // 释放obj的锁，等待通知
    }
}

// sleep()示例
Thread.sleep(1000); // 暂停1秒，不释放任何锁
```

### 8. ReentrantLock和synchronized的区别？

**答案：**

| 特性 | synchronized | ReentrantLock |
|------|-------------|---------------|
| 实现方式 | JVM内置 | JDK实现 |
| 性能 | 优化后性能好 | 性能好 |
| 功能 | 基础功能 | 功能丰富 |
| 可中断 | 不可中断 | 可中断 |
| 超时获取 | 不支持 | 支持 |
| 公平锁 | 非公平 | 支持公平/非公平 |
| 条件变量 | 单一条件 | 多个Condition |
| 释放锁 | 自动释放 | 手动释放 |

**ReentrantLock高级功能：**
```java
ReentrantLock lock = new ReentrantLock(true); // 公平锁

try {
    // 1. 可中断获取锁
    lock.lockInterruptibly();
    
    // 2. 尝试获取锁
    if (lock.tryLock()) {
        // 获取到锁
    }
    
    // 3. 超时获取锁
    if (lock.tryLock(5, TimeUnit.SECONDS)) {
        // 5秒内获取到锁
    }
    
} catch (InterruptedException e) {
    // 处理中断
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock(); // 必须手动释放
    }
}
```

## 线程池

### 9. 线程池的核心参数？

**答案：**

**ThreadPoolExecutor构造参数：**
```java
public ThreadPoolExecutor(
    int corePoolSize,           // 核心线程数
    int maximumPoolSize,        // 最大线程数
    long keepAliveTime,         // 空闲线程存活时间
    TimeUnit unit,              // 时间单位
    BlockingQueue<Runnable> workQueue,  // 工作队列
    ThreadFactory threadFactory,         // 线程工厂
    RejectedExecutionHandler handler     // 拒绝策略
)
```

**参数详解：**
1. **corePoolSize：** 核心线程数，即使空闲也不会被回收
2. **maximumPoolSize：** 最大线程数，包括核心线程和非核心线程
3. **keepAliveTime：** 非核心线程的空闲存活时间
4. **workQueue：** 任务队列，存储等待执行的任务
5. **threadFactory：** 创建线程的工厂
6. **handler：** 拒绝策略，队列满时的处理方式

**执行流程：**
1. 线程数 < corePoolSize：创建新线程
2. 线程数 >= corePoolSize：任务入队
3. 队列满 && 线程数 < maximumPoolSize：创建新线程
4. 队列满 && 线程数 >= maximumPoolSize：执行拒绝策略

### 10. 常见的线程池类型？

**答案：**

**1. FixedThreadPool：**
```java
ExecutorService executor = Executors.newFixedThreadPool(5);
// 特点：固定线程数，无界队列
// 适用：负载较重的服务器
```

**2. CachedThreadPool：**
```java
ExecutorService executor = Executors.newCachedThreadPool();
// 特点：无核心线程，最大线程数Integer.MAX_VALUE
// 适用：执行大量短期异步任务
```

**3. SingleThreadExecutor：**
```java
ExecutorService executor = Executors.newSingleThreadExecutor();
// 特点：单线程，保证任务顺序执行
// 适用：需要顺序执行的任务
```

**4. ScheduledThreadPool：**
```java
ScheduledExecutorService executor = Executors.newScheduledThreadPool(3);
// 特点：支持定时和周期性任务
// 适用：定时任务
```

**为什么不推荐使用Executors：**
1. FixedThreadPool和SingleThreadExecutor使用无界队列，可能导致OOM
2. CachedThreadPool最大线程数过大，可能创建大量线程
3. 推荐使用ThreadPoolExecutor自定义参数

### 11. 拒绝策略有哪些？

**答案：**

**1. AbortPolicy（默认）：**
```java
new ThreadPoolExecutor.AbortPolicy()
// 抛出RejectedExecutionException异常
```

**2. CallerRunsPolicy：**
```java
new ThreadPoolExecutor.CallerRunsPolicy()
// 由调用线程执行任务
```

**3. DiscardPolicy：**
```java
new ThreadPoolExecutor.DiscardPolicy()
// 静默丢弃任务
```

**4. DiscardOldestPolicy：**
```java
new ThreadPoolExecutor.DiscardOldestPolicy()
// 丢弃队列中最老的任务
```

**自定义拒绝策略：**
```java
RejectedExecutionHandler customHandler = (r, executor) -> {
    // 自定义处理逻辑
    System.out.println("任务被拒绝：" + r.toString());
};
```

## 并发工具类

### 12. CountDownLatch、CyclicBarrier、Semaphore的区别？

**答案：**

**CountDownLatch：**
```java
CountDownLatch latch = new CountDownLatch(3);

// 工作线程
new Thread(() -> {
    // 执行任务
    latch.countDown(); // 计数减1
}).start();

// 主线程等待
latch.await(); // 等待计数为0
```
- 用途：等待多个线程完成
- 特点：一次性使用，不可重置

**CyclicBarrier：**
```java
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    System.out.println("所有线程到达屏障");
});

// 工作线程
new Thread(() -> {
    try {
        // 执行任务
        barrier.await(); // 等待其他线程
    } catch (Exception e) {
        e.printStackTrace();
    }
}).start();
```
- 用途：多个线程相互等待
- 特点：可重复使用

**Semaphore：**
```java
Semaphore semaphore = new Semaphore(3); // 3个许可

new Thread(() -> {
    try {
        semaphore.acquire(); // 获取许可
        // 执行任务
    } catch (InterruptedException e) {
        e.printStackTrace();
    } finally {
        semaphore.release(); // 释放许可
    }
}).start();
```
- 用途：控制同时访问资源的线程数
- 特点：可控制并发数量

### 13. AQS（AbstractQueuedSynchronizer）的原理？

**答案：**

AQS是Java并发包的基础框架，为实现同步器提供了通用功能。

**核心思想：**
- 使用一个int类型的state表示同步状态
- 使用FIFO队列管理等待线程
- 支持独占模式和共享模式

**主要组件：**
1. **同步状态：** volatile int state
2. **等待队列：** CLH队列的变种
3. **条件队列：** Condition的实现

**工作流程：**
```java
// 获取锁
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&          // 尝试获取
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg)) // 入队等待
        selfInterrupt();
}

// 释放锁
public final boolean release(int arg) {
    if (tryRelease(arg)) {           // 尝试释放
        Node h = head;
        if (h != null && h.waitStatus != 0)
            unparkSuccessor(h);      // 唤醒后继节点
        return true;
    }
    return false;
}
```

**基于AQS的同步器：**
- ReentrantLock
- ReentrantReadWriteLock
- CountDownLatch
- Semaphore
- FutureTask

## 并发容器

### 14. ConcurrentHashMap的实现原理？

**答案：**

**JDK 1.7：分段锁**
- 将数据分成多个Segment
- 每个Segment继承ReentrantLock
- 不同Segment可以并发访问
- 默认16个Segment

**JDK 1.8：CAS + synchronized**
- 取消Segment设计
- 使用Node数组 + 链表/红黑树
- 使用CAS操作进行无锁更新
- 在链表头节点使用synchronized

**核心操作：**
```java
// put操作
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    int hash = spread(key.hashCode());
    int binCount = 0;
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
            tab = initTable();  // 初始化表
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
                break;  // CAS成功，跳出循环
        }
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);  // 帮助扩容
        else {
            V oldVal = null;
            synchronized (f) {  // 锁定头节点
                // 链表或红黑树操作
            }
        }
    }
}
```

### 15. BlockingQueue的实现原理？

**答案：**

BlockingQueue是支持阻塞操作的队列，当队列为空时获取操作会阻塞，当队列满时插入操作会阻塞。

**主要实现：**

**1. ArrayBlockingQueue：**
```java
public class ArrayBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {
    
    final Object[] items;        // 数组存储
    final ReentrantLock lock;    // 锁
    private final Condition notEmpty;  // 非空条件
    private final Condition notFull;   // 非满条件
    
    public void put(E e) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (count == items.length)
                notFull.await();     // 队列满时等待
            enqueue(e);
        } finally {
            lock.unlock();
        }
    }
}
```

**2. LinkedBlockingQueue：**
- 基于链表实现
- 可选容量限制
- 使用两个锁（takeLock和putLock）提高并发性

**3. PriorityBlockingQueue：**
- 基于优先级堆实现
- 无界队列
- 支持自定义比较器

**4. SynchronousQueue：**
- 没有存储空间
- 每个插入操作必须等待对应的删除操作
- 适用于传递性场景

## 死锁和性能

### 16. 如何避免死锁？

**答案：**

**死锁的四个必要条件：**
1. 互斥条件：资源不能被多个线程同时使用
2. 请求和保持条件：线程已获得资源，同时等待其他资源
3. 不剥夺条件：资源不能被强制剥夺
4. 循环等待条件：存在资源的循环等待链

**避免死锁的方法：**

**1. 避免嵌套锁：**
```java
// 错误示例
synchronized(lockA) {
    synchronized(lockB) {
        // 可能死锁
    }
}
```

**2. 按顺序获取锁：**
```java
// 正确示例
private static final Object lock1 = new Object();
private static final Object lock2 = new Object();

public void method1() {
    synchronized(lock1) {
        synchronized(lock2) {
            // 安全
        }
    }
}

public void method2() {
    synchronized(lock1) {  // 相同顺序
        synchronized(lock2) {
            // 安全
        }
    }
}
```

**3. 使用超时锁：**
```java
ReentrantLock lock1 = new ReentrantLock();
ReentrantLock lock2 = new ReentrantLock();

public void method() {
    boolean acquired1 = false, acquired2 = false;
    try {
        acquired1 = lock1.tryLock(5, TimeUnit.SECONDS);
        if (acquired1) {
            acquired2 = lock2.tryLock(5, TimeUnit.SECONDS);
            if (acquired2) {
                // 执行业务逻辑
            }
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        if (acquired2) lock2.unlock();
        if (acquired1) lock1.unlock();
    }
}
```

**4. 死锁检测：**
- 使用JConsole、VisualVM等工具
- 程序中使用ThreadMXBean检测

### 17. 如何提高并发性能？

**答案：**

**1. 减少锁的范围：**
```java
// 优化前
public synchronized void method() {
    // 大量非关键代码
    criticalSection();
    // 更多非关键代码
}

// 优化后
public void method() {
    // 非关键代码
    synchronized(this) {
        criticalSection();  // 只锁关键部分
    }
    // 非关键代码
}
```

**2. 使用读写锁：**
```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();
Lock readLock = rwLock.readLock();
Lock writeLock = rwLock.writeLock();

public String read() {
    readLock.lock();
    try {
        return data;  // 多个线程可以同时读
    } finally {
        readLock.unlock();
    }
}

public void write(String newData) {
    writeLock.lock();
    try {
        data = newData;  // 写操作独占
    } finally {
        writeLock.unlock();
    }
}
```

**3. 使用无锁数据结构：**
```java
// 使用ConcurrentHashMap替代Hashtable
Map<String, String> map = new ConcurrentHashMap<>();

// 使用AtomicInteger替代synchronized计数
AtomicInteger counter = new AtomicInteger(0);
counter.incrementAndGet();
```

**4. 使用线程本地存储：**
```java
ThreadLocal<SimpleDateFormat> dateFormat = ThreadLocal.withInitial(
    () -> new SimpleDateFormat("yyyy-MM-dd")
);

public String formatDate(Date date) {
    return dateFormat.get().format(date);  // 线程安全
}
```

**5. 合理设计并发策略：**
- 无状态设计
- 不可变对象
- 分段锁
- 异步处理
- 批量操作