# Java线程基础

## 目录
- [线程概述](#线程概述)
- [线程创建方式](#线程创建方式)
- [线程生命周期](#线程生命周期)
- [线程控制方法](#线程控制方法)
- [线程同步](#线程同步)
- [线程通信](#线程通信)
- [线程安全](#线程安全)
- [最佳实践](#最佳实践)
- [面试要点](#面试要点)

## 线程概述

### 什么是线程
线程是程序执行的最小单位，是进程中的一个执行路径。一个进程可以包含多个线程，这些线程共享进程的内存空间和系统资源。

### 进程 vs 线程

| 特性 | 进程 | 线程 |
|------|------|------|
| 定义 | 程序的一次执行 | 进程中的执行单元 |
| 内存空间 | 独立的内存空间 | 共享进程内存空间 |
| 创建开销 | 大 | 小 |
| 通信方式 | IPC（管道、消息队列等） | 共享内存、同步机制 |
| 崩溃影响 | 不影响其他进程 | 可能影响整个进程 |

### 并发 vs 并行

```
并发 (Concurrency):
CPU1: [A1][B1][A2][B2][A3][B3]
      任务A和B交替执行

并行 (Parallelism):
CPU1: [A1][A2][A3][A4]
CPU2: [B1][B2][B3][B4]
      任务A和B同时执行
```

- **并发**: 多个任务在同一时间段内执行，但不一定同时执行
- **并行**: 多个任务在同一时刻同时执行

### 多线程的优势
1. **提高程序响应性**: UI线程不被阻塞
2. **充分利用CPU**: 多核CPU并行处理
3. **提高吞吐量**: 同时处理多个任务
4. **资源共享**: 线程间共享内存和文件句柄

### 多线程的挑战
1. **线程安全问题**: 数据竞争、死锁
2. **性能开销**: 上下文切换、同步开销
3. **调试困难**: 非确定性执行
4. **复杂性增加**: 程序逻辑复杂化

## 线程创建方式

### 1. 继承Thread类

```java
public class MyThread extends Thread {
    private String threadName;
    
    public MyThread(String name) {
        this.threadName = name;
    }
    
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(threadName + " - Count: " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println(threadName + " interrupted.");
                return;
            }
        }
        System.out.println(threadName + " finished.");
    }
    
    public static void main(String[] args) {
        MyThread thread1 = new MyThread("Thread-1");
        MyThread thread2 = new MyThread("Thread-2");
        
        thread1.start(); // 启动线程
        thread2.start();
    }
}
```

### 2. 实现Runnable接口

```java
public class MyRunnable implements Runnable {
    private String taskName;
    
    public MyRunnable(String name) {
        this.taskName = name;
    }
    
    @Override
    public void run() {
        for (int i = 0; i < 5; i++) {
            System.out.println(taskName + " - Count: " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println(taskName + " interrupted.");
                return;
            }
        }
        System.out.println(taskName + " finished.");
    }
    
    public static void main(String[] args) {
        Thread thread1 = new Thread(new MyRunnable("Task-1"));
        Thread thread2 = new Thread(new MyRunnable("Task-2"));
        
        thread1.start();
        thread2.start();
    }
}
```

### 3. 实现Callable接口

```java
import java.util.concurrent.*;

public class MyCallable implements Callable<String> {
    private String taskName;
    
    public MyCallable(String name) {
        this.taskName = name;
    }
    
    @Override
    public String call() throws Exception {
        int sum = 0;
        for (int i = 1; i <= 100; i++) {
            sum += i;
            Thread.sleep(10); // 模拟耗时操作
        }
        return taskName + " result: " + sum;
    }
    
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        
        Future<String> future1 = executor.submit(new MyCallable("Task-1"));
        Future<String> future2 = executor.submit(new MyCallable("Task-2"));
        
        try {
            // 获取执行结果
            String result1 = future1.get();
            String result2 = future2.get();
            
            System.out.println(result1);
            System.out.println(result2);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdown();
        }
    }
}
```

### 4. 使用Lambda表达式

```java
public class LambdaThreadExample {
    public static void main(String[] args) {
        // 使用Lambda表达式创建线程
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Lambda Thread - Count: " + i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        
        thread1.start();
        
        // 使用方法引用
        Thread thread2 = new Thread(LambdaThreadExample::printNumbers);
        thread2.start();
    }
    
    private static void printNumbers() {
        for (int i = 0; i < 5; i++) {
            System.out.println("Method Reference Thread - Count: " + i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}
```

### 创建方式对比

| 方式 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| 继承Thread | 简单直接 | 无法继承其他类 | 简单的线程任务 |
| 实现Runnable | 可以继承其他类，代码复用性好 | 无返回值 | 大多数情况 |
| 实现Callable | 有返回值，可以抛出异常 | 需要配合ExecutorService | 需要返回结果的任务 |
| Lambda表达式 | 代码简洁 | 只适用于简单任务 | 简单的匿名任务 |

## 线程生命周期

### 线程状态图

```
┌─────────┐    start()    ┌─────────┐
│   NEW   │──────────────→│RUNNABLE │
└─────────┘               └─────────┘
                               │ ↑
                               │ │
                          获取锁失败│ │获取到锁
                               │ │
                               ↓ │
                          ┌─────────┐
                          │ BLOCKED │
                          └─────────┘
                               ↑
                               │
                          ┌─────────┐
                          │ WAITING │←──── wait()
                          └─────────┘      join()
                               ↑           LockSupport.park()
                               │
                          ┌─────────┐
                          │TIMED_   │←──── sleep()
                          │WAITING  │      wait(timeout)
                          └─────────┘      join(timeout)
                               │
                               │ 时间到/被唤醒
                               ↓
                          ┌─────────┐
                          │RUNNABLE │
                          └─────────┘
                               │
                               │ run()方法执行完毕
                               ↓
                          ┌─────────┐
                          │TERMINATED│
                          └─────────┘
```

### 线程状态详解

#### 1. NEW (新建)
- 线程对象已创建，但还未调用start()方法
- 此时线程还没有开始执行

```java
Thread thread = new Thread(() -> System.out.println("Hello"));
// 此时线程状态为NEW
System.out.println(thread.getState()); // NEW
```

#### 2. RUNNABLE (可运行)
- 调用start()方法后，线程进入RUNNABLE状态
- 包括正在运行和准备运行两种情况
- 可能正在等待CPU时间片

```java
thread.start();
// 此时线程状态为RUNNABLE
System.out.println(thread.getState()); // RUNNABLE
```

#### 3. BLOCKED (阻塞)
- 线程等待获取同步锁时的状态
- 通常发生在synchronized代码块或方法中

```java
public class BlockedExample {
    private static final Object lock = new Object();
    
    public static void main(String[] args) {
        Thread thread1 = new Thread(() -> {
            synchronized (lock) {
                try {
                    Thread.sleep(5000); // 持有锁5秒
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        Thread thread2 = new Thread(() -> {
            synchronized (lock) { // 等待获取锁，状态为BLOCKED
                System.out.println("Thread2 got the lock");
            }
        });
        
        thread1.start();
        thread2.start();
        
        try {
            Thread.sleep(1000);
            System.out.println("Thread2 state: " + thread2.getState()); // BLOCKED
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

#### 4. WAITING (等待)
- 线程无限期等待另一个线程执行特定操作
- 通过wait()、join()、LockSupport.park()等方法进入

```java
public class WaitingExample {
    private static final Object lock = new Object();
    
    public static void main(String[] args) {
        Thread waitingThread = new Thread(() -> {
            synchronized (lock) {
                try {
                    lock.wait(); // 进入WAITING状态
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        waitingThread.start();
        
        try {
            Thread.sleep(1000);
            System.out.println("Waiting thread state: " + waitingThread.getState()); // WAITING
            
            synchronized (lock) {
                lock.notify(); // 唤醒等待的线程
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

#### 5. TIMED_WAITING (超时等待)
- 线程等待指定时间后自动返回
- 通过sleep()、wait(timeout)、join(timeout)等方法进入

```java
public class TimedWaitingExample {
    public static void main(String[] args) {
        Thread timedWaitingThread = new Thread(() -> {
            try {
                Thread.sleep(5000); // 进入TIMED_WAITING状态
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        timedWaitingThread.start();
        
        try {
            Thread.sleep(1000);
            System.out.println("Timed waiting thread state: " + timedWaitingThread.getState()); // TIMED_WAITING
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

#### 6. TERMINATED (终止)
- 线程执行完毕或因异常退出
- 线程不能从TERMINATED状态转换到其他状态

```java
Thread thread = new Thread(() -> System.out.println("Task completed"));
thread.start();

try {
    thread.join(); // 等待线程执行完毕
    System.out.println("Thread state: " + thread.getState()); // TERMINATED
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

## 线程控制方法

### 1. start() vs run()

```java
public class StartVsRunExample {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            System.out.println("Current thread: " + Thread.currentThread().getName());
        });
        
        // 直接调用run()方法
        thread.run(); // 输出: Current thread: main
        
        // 调用start()方法
        thread.start(); // 输出: Current thread: Thread-0
    }
}
```

**区别**:
- `run()`: 在当前线程中执行，不会创建新线程
- `start()`: 创建新线程并在新线程中执行run()方法

### 2. sleep() 方法

```java
public class SleepExample {
    public static void main(String[] args) {
        Thread thread = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Count: " + i);
                try {
                    Thread.sleep(1000); // 暂停1秒
                } catch (InterruptedException e) {
                    System.out.println("Thread was interrupted");
                    Thread.currentThread().interrupt(); // 重新设置中断标志
                    return;
                }
            }
        });
        
        thread.start();
        
        // 3秒后中断线程
        try {
            Thread.sleep(3000);
            thread.interrupt();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

**特点**:
- 使当前线程暂停执行指定时间
- 不会释放持有的锁
- 可以被interrupt()方法中断

### 3. join() 方法

```java
public class JoinExample {
    public static void main(String[] args) {
        Thread worker = new Thread(() -> {
            System.out.println("Worker thread started");
            try {
                Thread.sleep(3000); // 模拟工作3秒
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Worker thread finished");
        });
        
        worker.start();
        
        try {
            System.out.println("Main thread waiting for worker...");
            worker.join(); // 等待worker线程执行完毕
            System.out.println("Main thread continues");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

**join()方法变体**:
```java
// 无限等待
thread.join();

// 等待指定时间
thread.join(5000); // 最多等待5秒

// 等待指定时间（精确到纳秒）
thread.join(5000, 500000); // 等待5秒500毫秒
```

### 4. interrupt() 方法

```java
public class InterruptExample {
    public static void main(String[] args) {
        // 示例1：中断阻塞线程
        Thread blockingThread = new Thread(() -> {
            try {
                System.out.println("Thread is sleeping...");
                Thread.sleep(10000); // 长时间睡眠
            } catch (InterruptedException e) {
                System.out.println("Thread was interrupted during sleep");
                return;
            }
            System.out.println("Thread finished normally");
        });
        
        blockingThread.start();
        
        try {
            Thread.sleep(2000);
            blockingThread.interrupt(); // 中断线程
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 示例2：中断运行中的线程
        Thread runningThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("Working...");
                // 模拟工作
                for (int i = 0; i < 1000000; i++) {
                    Math.sqrt(i);
                }
            }
            System.out.println("Thread stopped due to interruption");
        });
        
        runningThread.start();
        
        try {
            Thread.sleep(1000);
            runningThread.interrupt(); // 中断线程
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

**中断机制**:
- `interrupt()`: 设置线程的中断标志
- `isInterrupted()`: 检查线程是否被中断
- `interrupted()`: 检查并清除当前线程的中断标志

### 5. yield() 方法

```java
public class YieldExample {
    public static void main(String[] args) {
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Thread 1 - Count: " + i);
                Thread.yield(); // 让出CPU时间片
            }
        });
        
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                System.out.println("Thread 2 - Count: " + i);
                Thread.yield(); // 让出CPU时间片
            }
        });
        
        thread1.start();
        thread2.start();
    }
}
```

**特点**:
- 建议当前线程让出CPU时间片
- 只是建议，具体是否让出由操作系统决定
- 不会释放持有的锁

## 线程同步

### 1. synchronized关键字

#### 同步方法
```java
public class SynchronizedMethodExample {
    private int count = 0;
    
    // 同步实例方法
    public synchronized void increment() {
        count++;
    }
    
    // 同步静态方法
    public static synchronized void staticMethod() {
        System.out.println("Static synchronized method");
    }
    
    public synchronized int getCount() {
        return count;
    }
    
    public static void main(String[] args) {
        SynchronizedMethodExample example = new SynchronizedMethodExample();
        
        // 创建多个线程同时访问
        Thread[] threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    example.increment();
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("Final count: " + example.getCount()); // 应该是10000
    }
}
```

#### 同步代码块
```java
public class SynchronizedBlockExample {
    private int count = 0;
    private final Object lock = new Object();
    
    public void increment() {
        synchronized (lock) { // 使用自定义锁对象
            count++;
        }
    }
    
    public void decrement() {
        synchronized (this) { // 使用this作为锁对象
            count--;
        }
    }
    
    public static void staticMethod() {
        synchronized (SynchronizedBlockExample.class) { // 使用Class对象作为锁
            System.out.println("Static synchronized block");
        }
    }
    
    public int getCount() {
        synchronized (lock) {
            return count;
        }
    }
}
```

### 2. volatile关键字

```java
public class VolatileExample {
    private volatile boolean flag = false;
    private volatile int counter = 0;
    
    public void writer() {
        counter = 42;
        flag = true; // volatile写操作
    }
    
    public void reader() {
        if (flag) { // volatile读操作
            System.out.println("Counter: " + counter); // 保证能看到42
        }
    }
    
    public static void main(String[] args) {
        VolatileExample example = new VolatileExample();
        
        Thread writerThread = new Thread(example::writer);
        Thread readerThread = new Thread(() -> {
            while (!example.flag) {
                // 等待flag变为true
            }
            example.reader();
        });
        
        readerThread.start();
        writerThread.start();
    }
}
```

**volatile特性**:
- **可见性**: 保证变量的修改对所有线程立即可见
- **有序性**: 禁止指令重排序
- **不保证原子性**: 不能替代synchronized

## 线程通信

### 1. wait() 和 notify()

```java
public class WaitNotifyExample {
    private final Object lock = new Object();
    private boolean condition = false;
    
    public void waitForCondition() {
        synchronized (lock) {
            while (!condition) {
                try {
                    System.out.println("Waiting for condition...");
                    lock.wait(); // 释放锁并等待
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            System.out.println("Condition met, proceeding...");
        }
    }
    
    public void setCondition() {
        synchronized (lock) {
            condition = true;
            System.out.println("Condition set to true");
            lock.notify(); // 唤醒一个等待的线程
            // lock.notifyAll(); // 唤醒所有等待的线程
        }
    }
    
    public static void main(String[] args) {
        WaitNotifyExample example = new WaitNotifyExample();
        
        Thread waiterThread = new Thread(example::waitForCondition);
        Thread setterThread = new Thread(() -> {
            try {
                Thread.sleep(2000); // 等待2秒后设置条件
                example.setCondition();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        waiterThread.start();
        setterThread.start();
    }
}
```

### 2. 生产者-消费者模式

```java
import java.util.LinkedList;
import java.util.Queue;

public class ProducerConsumerExample {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity = 5;
    private final Object lock = new Object();
    
    public void produce() {
        int value = 0;
        while (true) {
            synchronized (lock) {
                while (queue.size() == capacity) {
                    try {
                        System.out.println("Queue is full, producer waiting...");
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                
                queue.offer(value);
                System.out.println("Produced: " + value);
                value++;
                
                lock.notifyAll(); // 唤醒消费者
            }
            
            try {
                Thread.sleep(1000); // 模拟生产时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
    
    public void consume() {
        while (true) {
            synchronized (lock) {
                while (queue.isEmpty()) {
                    try {
                        System.out.println("Queue is empty, consumer waiting...");
                        lock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                
                int value = queue.poll();
                System.out.println("Consumed: " + value);
                
                lock.notifyAll(); // 唤醒生产者
            }
            
            try {
                Thread.sleep(1500); // 模拟消费时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
    
    public static void main(String[] args) {
        ProducerConsumerExample example = new ProducerConsumerExample();
        
        Thread producer = new Thread(example::produce);
        Thread consumer = new Thread(example::consume);
        
        producer.start();
        consumer.start();
    }
}
```

## 线程安全

### 什么是线程安全
线程安全是指在多线程环境下，对共享资源的访问不会导致数据不一致或程序行为异常。

### 线程安全的实现方式

#### 1. 无状态设计
```java
// 线程安全：没有共享状态
public class StatelessService {
    public int add(int a, int b) {
        return a + b; // 只使用方法参数，没有实例变量
    }
}
```

#### 2. 不可变对象
```java
// 线程安全：不可变对象
public final class ImmutableCounter {
    private final int count;
    
    public ImmutableCounter(int count) {
        this.count = count;
    }
    
    public int getCount() {
        return count;
    }
    
    public ImmutableCounter increment() {
        return new ImmutableCounter(count + 1); // 返回新对象
    }
}
```

#### 3. 线程本地存储
```java
public class ThreadLocalExample {
    private static final ThreadLocal<Integer> threadLocal = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };
    
    public void increment() {
        threadLocal.set(threadLocal.get() + 1);
    }
    
    public int get() {
        return threadLocal.get();
    }
    
    public static void main(String[] args) {
        ThreadLocalExample example = new ThreadLocalExample();
        
        Thread thread1 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                example.increment();
                System.out.println("Thread 1: " + example.get());
            }
        });
        
        Thread thread2 = new Thread(() -> {
            for (int i = 0; i < 5; i++) {
                example.increment();
                System.out.println("Thread 2: " + example.get());
            }
        });
        
        thread1.start();
        thread2.start();
    }
}
```

## 最佳实践

### 1. 线程命名
```java
public class ThreadNamingExample {
    public static void main(String[] args) {
        // 为线程设置有意义的名称
        Thread worker = new Thread(() -> {
            System.out.println("Worker thread: " + Thread.currentThread().getName());
        }, "DataProcessor-Worker");
        
        worker.start();
    }
}
```

### 2. 异常处理
```java
public class ThreadExceptionHandling {
    public static void main(String[] args) {
        // 设置未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            System.err.println("Uncaught exception in thread " + thread.getName() + ": " + exception.getMessage());
            exception.printStackTrace();
        });
        
        Thread thread = new Thread(() -> {
            throw new RuntimeException("Something went wrong!");
        }, "ErrorProneThread");
        
        thread.start();
    }
}
```

### 3. 优雅关闭
```java
public class GracefulShutdownExample {
    private volatile boolean running = true;
    
    public void worker() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                // 执行工作
                System.out.println("Working...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                System.out.println("Worker interrupted, shutting down...");
                Thread.currentThread().interrupt(); // 重新设置中断标志
                break;
            }
        }
        System.out.println("Worker stopped gracefully");
    }
    
    public void shutdown() {
        running = false;
    }
    
    public static void main(String[] args) {
        GracefulShutdownExample example = new GracefulShutdownExample();
        Thread worker = new Thread(example::worker);
        
        worker.start();
        
        // 5秒后关闭
        try {
            Thread.sleep(5000);
            example.shutdown();
            worker.interrupt();
            worker.join(2000); // 等待最多2秒
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

## 面试要点

### 高频问题

1. **线程的创建方式有哪些？**
   - 继承Thread类
   - 实现Runnable接口
   - 实现Callable接口
   - 使用Lambda表达式

2. **run()和start()的区别？**
   - run()：在当前线程中执行
   - start()：创建新线程并执行run()方法

3. **线程的生命周期？**
   - NEW → RUNNABLE → BLOCKED/WAITING/TIMED_WAITING → TERMINATED

4. **sleep()和wait()的区别？**
   - sleep()：不释放锁，Thread类的静态方法
   - wait()：释放锁，Object类的实例方法

5. **如何停止一个线程？**
   - 使用interrupt()方法
   - 使用标志位
   - 避免使用stop()方法（已废弃）

### 深入问题

1. **什么是线程安全？如何实现？**
   - 定义：多线程环境下正确性
   - 实现：同步、不可变、线程本地存储、原子类

2. **synchronized的原理？**
   - 基于监视器锁（Monitor）
   - 可重入性
   - 内存可见性

3. **volatile的作用？**
   - 保证可见性
   - 禁止指令重排序
   - 不保证原子性

4. **死锁的条件和预防？**
   - 四个必要条件：互斥、占有且等待、不可抢占、循环等待
   - 预防：破坏任一条件

### 实践经验
- 了解常见的并发问题和解决方案
- 掌握线程池的使用
- 理解JMM（Java内存模型）
- 熟悉并发工具类的使用

## 总结

线程是Java并发编程的基础，掌握线程的基本概念和使用方法对于编写高质量的并发程序至关重要：

1. **正确创建和管理线程**
2. **理解线程生命周期和状态转换**
3. **掌握线程同步和通信机制**
4. **避免常见的并发问题**
5. **遵循最佳实践**

建议通过实际编程练习来加深对线程概念的理解，这样在面试和实际工作中才能游刃有余。