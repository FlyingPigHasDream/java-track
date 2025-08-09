# 线程基础

## 概述

线程是程序执行的最小单位，Java从语言层面支持多线程编程，使得开发者可以编写并发程序来提高程序的性能和响应性。

## 线程的生命周期

```
新建(NEW) → 就绪(RUNNABLE) → 运行(RUNNING) → 阻塞(BLOCKED/WAITING/TIMED_WAITING) → 终止(TERMINATED)
```

### 线程状态详解

```java
public enum State {
    NEW,           // 新建状态
    RUNNABLE,      // 就绪/运行状态
    BLOCKED,       // 阻塞状态
    WAITING,       // 等待状态
    TIMED_WAITING, // 超时等待状态
    TERMINATED;    // 终止状态
}
```

**状态转换示例：**
```java
public class ThreadStateDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(2000); // TIMED_WAITING
                synchronized (ThreadStateDemo.class) {
                    ThreadStateDemo.class.wait(); // WAITING
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        System.out.println("创建后: " + thread.getState()); // NEW
        
        thread.start();
        System.out.println("启动后: " + thread.getState()); // RUNNABLE
        
        Thread.sleep(100);
        System.out.println("睡眠中: " + thread.getState()); // TIMED_WAITING
        
        Thread.sleep(2000);
        System.out.println("等待中: " + thread.getState()); // WAITING
        
        synchronized (ThreadStateDemo.class) {
            ThreadStateDemo.class.notify();
        }
        
        thread.join();
        System.out.println("结束后: " + thread.getState()); // TERMINATED
    }
}
```

## 创建线程的方式

### 1. 继承Thread类

```java
class MyThread extends Thread {
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
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

// 使用
MyThread thread1 = new MyThread("Thread-1");
MyThread thread2 = new MyThread("Thread-2");
thread1.start();
thread2.start();
```

### 2. 实现Runnable接口

```java
class MyRunnable implements Runnable {
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
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

// 使用
Thread thread1 = new Thread(new MyRunnable("Task-1"));
Thread thread2 = new Thread(new MyRunnable("Task-2"));
thread1.start();
thread2.start();

// Lambda表达式
Thread thread3 = new Thread(() -> {
    System.out.println("Lambda thread: " + Thread.currentThread().getName());
});
thread3.start();
```

### 3. 实现Callable接口

```java
import java.util.concurrent.*;

class MyCallable implements Callable<String> {
    private String taskName;
    
    public MyCallable(String name) {
        this.taskName = name;
    }
    
    @Override
    public String call() throws Exception {
        Thread.sleep(2000);
        return taskName + " completed at " + System.currentTimeMillis();
    }
}

// 使用
ExecutorService executor = Executors.newFixedThreadPool(2);

Future<String> future1 = executor.submit(new MyCallable("Task-1"));
Future<String> future2 = executor.submit(new MyCallable("Task-2"));

try {
    String result1 = future1.get(); // 阻塞等待结果
    String result2 = future2.get(3, TimeUnit.SECONDS); // 超时等待
    
    System.out.println(result1);
    System.out.println(result2);
} catch (InterruptedException | ExecutionException | TimeoutException e) {
    e.printStackTrace();
} finally {
    executor.shutdown();
}
```

### 4. 使用线程池

```java
// 固定大小线程池
ExecutorService fixedPool = Executors.newFixedThreadPool(3);

// 缓存线程池
ExecutorService cachedPool = Executors.newCachedThreadPool();

// 单线程池
ExecutorService singlePool = Executors.newSingleThreadExecutor();

// 定时线程池
ScheduledExecutorService scheduledPool = Executors.newScheduledThreadPool(2);

// 提交任务
fixedPool.submit(() -> System.out.println("Fixed pool task"));
cachedPool.execute(() -> System.out.println("Cached pool task"));
singlePool.submit(() -> System.out.println("Single pool task"));

// 定时任务
scheduledPool.schedule(() -> System.out.println("Delayed task"), 2, TimeUnit.SECONDS);
scheduledPool.scheduleAtFixedRate(() -> System.out.println("Periodic task"), 0, 1, TimeUnit.SECONDS);
```

## 线程的基本操作

### 线程控制方法

```java
public class ThreadControlDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("线程被中断，退出循环");
                    break;
                }
                System.out.println("Working: " + i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    System.out.println("睡眠被中断");
                    Thread.currentThread().interrupt(); // 重新设置中断标志
                    break;
                }
            }
        });
        
        worker.start();
        
        // 主线程等待2秒后中断worker线程
        Thread.sleep(2000);
        worker.interrupt();
        
        // 等待worker线程结束
        worker.join();
        System.out.println("主线程结束");
    }
}
```

### 线程优先级

```java
Thread highPriorityThread = new Thread(() -> {
    for (int i = 0; i < 5; i++) {
        System.out.println("High priority: " + i);
    }
});

Thread lowPriorityThread = new Thread(() -> {
    for (int i = 0; i < 5; i++) {
        System.out.println("Low priority: " + i);
    }
});

highPriorityThread.setPriority(Thread.MAX_PRIORITY); // 10
lowPriorityThread.setPriority(Thread.MIN_PRIORITY);  // 1

lowPriorityThread.start();
highPriorityThread.start();
```

### 守护线程

```java
Thread daemonThread = new Thread(() -> {
    while (true) {
        System.out.println("守护线程运行中...");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            break;
        }
    }
});

// 设置为守护线程
daemonThread.setDaemon(true);
daemonThread.start();

// 主线程睡眠3秒后结束，守护线程也会随之结束
Thread.sleep(3000);
System.out.println("主线程结束");
```

## 线程间通信

### wait/notify机制

```java
class SharedResource {
    private boolean flag = false;
    
    public synchronized void waitForFlag() throws InterruptedException {
        while (!flag) {
            System.out.println(Thread.currentThread().getName() + " 等待标志...");
            wait(); // 释放锁并等待
        }
        System.out.println(Thread.currentThread().getName() + " 收到通知，继续执行");
    }
    
    public synchronized void setFlag() {
        flag = true;
        System.out.println(Thread.currentThread().getName() + " 设置标志并通知");
        notifyAll(); // 通知所有等待的线程
    }
}

public class WaitNotifyDemo {
    public static void main(String[] args) throws InterruptedException {
        SharedResource resource = new SharedResource();
        
        // 创建等待线程
        Thread waiter1 = new Thread(() -> {
            try {
                resource.waitForFlag();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Waiter-1");
        
        Thread waiter2 = new Thread(() -> {
            try {
                resource.waitForFlag();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Waiter-2");
        
        // 创建通知线程
        Thread notifier = new Thread(() -> {
            try {
                Thread.sleep(2000);
                resource.setFlag();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Notifier");
        
        waiter1.start();
        waiter2.start();
        notifier.start();
        
        waiter1.join();
        waiter2.join();
        notifier.join();
    }
}
```

### 生产者消费者模式

```java
import java.util.LinkedList;
import java.util.Queue;

class ProducerConsumer {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity = 5;
    
    public void produce() throws InterruptedException {
        int value = 0;
        while (true) {
            synchronized (this) {
                while (queue.size() == capacity) {
                    wait(); // 队列满了，等待消费
                }
                
                queue.offer(++value);
                System.out.println("生产者生产: " + value + ", 队列大小: " + queue.size());
                
                notifyAll(); // 通知消费者
                Thread.sleep(1000);
            }
        }
    }
    
    public void consume() throws InterruptedException {
        while (true) {
            synchronized (this) {
                while (queue.isEmpty()) {
                    wait(); // 队列空了，等待生产
                }
                
                int value = queue.poll();
                System.out.println("消费者消费: " + value + ", 队列大小: " + queue.size());
                
                notifyAll(); // 通知生产者
                Thread.sleep(1500);
            }
        }
    }
}

public class ProducerConsumerDemo {
    public static void main(String[] args) {
        ProducerConsumer pc = new ProducerConsumer();
        
        Thread producer = new Thread(() -> {
            try {
                pc.produce();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");
        
        Thread consumer = new Thread(() -> {
            try {
                pc.consume();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");
        
        producer.start();
        consumer.start();
    }
}
```

## 线程安全问题

### 竞态条件示例

```java
class Counter {
    private int count = 0;
    
    // 非线程安全的方法
    public void increment() {
        count++; // 这不是原子操作
    }
    
    // 线程安全的方法
    public synchronized void safeIncrement() {
        count++;
    }
    
    public int getCount() {
        return count;
    }
}

public class RaceConditionDemo {
    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();
        
        // 创建多个线程同时增加计数
        Thread[] threads = new Thread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    counter.increment(); // 可能出现竞态条件
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        System.out.println("最终计数: " + counter.getCount()); // 可能小于10000
    }
}
```

## 线程中断机制

### 正确处理中断

```java
public class InterruptDemo {
    public static void main(String[] args) throws InterruptedException {
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 模拟工作
                    Thread.sleep(1000);
                    System.out.println("工作中...");
                } catch (InterruptedException e) {
                    System.out.println("收到中断信号");
                    // 重新设置中断标志
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            System.out.println("线程正常退出");
        });
        
        worker.start();
        
        // 3秒后中断线程
        Thread.sleep(3000);
        worker.interrupt();
        
        worker.join();
        System.out.println("主线程结束");
    }
}
```

## 常见面试题

1. **线程和进程的区别？**
2. **创建线程的几种方式？**
3. **run()和start()方法的区别？**
4. **线程的生命周期？**
5. **如何正确停止一个线程？**
6. **wait()和sleep()的区别？**
7. **守护线程的作用？**
8. **线程优先级的作用？**

## 最佳实践

1. **优先使用线程池而不是直接创建线程**
2. **正确处理InterruptedException**
3. **避免使用stop()、suspend()等已废弃的方法**
4. **合理设置线程名称便于调试**
5. **注意线程安全问题**
6. **避免在构造器中启动线程**
7. **使用ThreadLocal时注意内存泄漏**