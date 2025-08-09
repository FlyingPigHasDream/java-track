package com.javatrack.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * 线程池和并发工具类示例
 * 演示线程池的使用、并发工具类的应用
 */
public class ThreadPoolExample {
    
    public static void main(String[] args) {
        System.out.println("=== 线程池和并发工具类示例 ===");
        
        threadPoolBasicExample();
        customThreadPoolExample();
        concurrentUtilitiesExample();
        producerConsumerExample();
        completableFutureExample();
    }
    
    /**
     * 基础线程池示例
     */
    private static void threadPoolBasicExample() {
        System.out.println("\n1. 基础线程池示例:");
        
        // 固定大小线程池
        ExecutorService fixedPool = Executors.newFixedThreadPool(3);
        
        // 提交多个任务
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            fixedPool.submit(() -> {
                System.out.println("任务 " + taskId + " 开始执行，线程: " + 
                    Thread.currentThread().getName());
                try {
                    Thread.sleep(2000); // 模拟任务执行
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                System.out.println("任务 " + taskId + " 执行完成");
            });
        }
        
        // 关闭线程池
        fixedPool.shutdown();
        try {
            if (!fixedPool.awaitTermination(10, TimeUnit.SECONDS)) {
                fixedPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            fixedPool.shutdownNow();
        }
        
        System.out.println("固定线程池示例完成");
    }
    
    /**
     * 自定义线程池示例
     */
    private static void customThreadPoolExample() {
        System.out.println("\n2. 自定义线程池示例:");
        
        // 自定义线程工厂
        ThreadFactory customThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r, "CustomThread-" + threadNumber.getAndIncrement());
                thread.setDaemon(false);
                return thread;
            }
        };
        
        // 自定义拒绝策略
        RejectedExecutionHandler customHandler = (r, executor) -> {
            System.out.println("任务被拒绝: " + r.toString() + 
                ", 活跃线程数: " + executor.getActiveCount());
        };
        
        // 创建自定义线程池
        ThreadPoolExecutor customPool = new ThreadPoolExecutor(
            2,                              // 核心线程数
            4,                              // 最大线程数
            60L,                            // 空闲线程存活时间
            TimeUnit.SECONDS,               // 时间单位
            new ArrayBlockingQueue<>(2),    // 工作队列
            customThreadFactory,            // 线程工厂
            customHandler                   // 拒绝策略
        );
        
        // 提交任务测试拒绝策略
        for (int i = 1; i <= 8; i++) {
            final int taskId = i;
            try {
                customPool.submit(() -> {
                    System.out.println("自定义任务 " + taskId + " 执行，线程: " + 
                        Thread.currentThread().getName());
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
                Thread.sleep(100); // 稍微延迟提交
            } catch (Exception e) {
                System.out.println("提交任务 " + taskId + " 失败: " + e.getMessage());
            }
        }
        
        // 监控线程池状态
        System.out.println("线程池状态:");
        System.out.println("  核心线程数: " + customPool.getCorePoolSize());
        System.out.println("  最大线程数: " + customPool.getMaximumPoolSize());
        System.out.println("  当前线程数: " + customPool.getPoolSize());
        System.out.println("  活跃线程数: " + customPool.getActiveCount());
        System.out.println("  队列大小: " + customPool.getQueue().size());
        
        customPool.shutdown();
        try {
            customPool.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            customPool.shutdownNow();
        }
    }
    
    /**
     * 并发工具类示例
     */
    private static void concurrentUtilitiesExample() {
        System.out.println("\n3. 并发工具类示例:");
        
        // CountDownLatch示例
        countDownLatchExample();
        
        // CyclicBarrier示例
        cyclicBarrierExample();
        
        // Semaphore示例
        semaphoreExample();
    }
    
    /**
     * CountDownLatch示例
     */
    private static void countDownLatchExample() {
        System.out.println("\nCountDownLatch示例:");
        
        CountDownLatch latch = new CountDownLatch(3);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        // 启动3个工作线程
        for (int i = 1; i <= 3; i++) {
            final int workerId = i;
            executor.submit(() -> {
                try {
                    System.out.println("工作者 " + workerId + " 开始工作");
                    Thread.sleep(new Random().nextInt(3000) + 1000);
                    System.out.println("工作者 " + workerId + " 完成工作");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown(); // 计数减1
                }
            });
        }
        
        try {
            System.out.println("主线程等待所有工作者完成...");
            latch.await(); // 等待计数为0
            System.out.println("所有工作者完成，主线程继续执行");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        executor.shutdown();
    }
    
    /**
     * CyclicBarrier示例
     */
    private static void cyclicBarrierExample() {
        System.out.println("\nCyclicBarrier示例:");
        
        CyclicBarrier barrier = new CyclicBarrier(3, () -> {
            System.out.println("所有线程到达屏障，开始下一阶段!");
        });
        
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        for (int i = 1; i <= 3; i++) {
            final int runnerId = i;
            executor.submit(() -> {
                try {
                    System.out.println("跑步者 " + runnerId + " 准备中...");
                    Thread.sleep(new Random().nextInt(2000) + 1000);
                    System.out.println("跑步者 " + runnerId + " 准备完毕，等待其他人");
                    
                    barrier.await(); // 等待其他线程
                    
                    System.out.println("跑步者 " + runnerId + " 开始跑步!");
                } catch (InterruptedException | BrokenBarrierException e) {
                    e.printStackTrace();
                }
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
    
    /**
     * Semaphore示例
     */
    private static void semaphoreExample() {
        System.out.println("\nSemaphore示例:");
        
        Semaphore semaphore = new Semaphore(2); // 只允许2个线程同时访问
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        for (int i = 1; i <= 5; i++) {
            final int customerId = i;
            executor.submit(() -> {
                try {
                    System.out.println("顾客 " + customerId + " 等待进入银行...");
                    semaphore.acquire(); // 获取许可
                    
                    System.out.println("顾客 " + customerId + " 进入银行办理业务");
                    Thread.sleep(2000); // 模拟办理业务
                    System.out.println("顾客 " + customerId + " 办理完毕，离开银行");
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    semaphore.release(); // 释放许可
                }
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
    
    /**
     * 生产者消费者示例
     */
    private static void producerConsumerExample() {
        System.out.println("\n4. 生产者消费者示例:");
        
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        // 生产者
        for (int i = 1; i <= 2; i++) {
            final int producerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 1; j <= 5; j++) {
                        String product = "产品-" + producerId + "-" + j;
                        queue.put(product); // 阻塞式放入
                        System.out.println("生产者 " + producerId + " 生产了: " + product + 
                            ", 队列大小: " + queue.size());
                        Thread.sleep(500);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // 消费者
        for (int i = 1; i <= 2; i++) {
            final int consumerId = i;
            executor.submit(() -> {
                try {
                    for (int j = 1; j <= 5; j++) {
                        String product = queue.take(); // 阻塞式取出
                        System.out.println("消费者 " + consumerId + " 消费了: " + product + 
                            ", 队列大小: " + queue.size());
                        Thread.sleep(800);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
    
    /**
     * CompletableFuture示例
     */
    private static void completableFutureExample() {
        System.out.println("\n5. CompletableFuture示例:");
        
        // 异步执行任务
        CompletableFuture<String> future1 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "Hello";
        });
        
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "World";
        });
        
        // 组合两个异步任务
        CompletableFuture<String> combinedFuture = future1.thenCombine(future2, (s1, s2) -> {
            return s1 + " " + s2;
        });
        
        // 异步处理结果
        combinedFuture.thenAccept(result -> {
            System.out.println("组合结果: " + result);
        });
        
        // 等待所有任务完成
        CompletableFuture<Void> allTasks = CompletableFuture.allOf(future1, future2, combinedFuture);
        
        try {
            allTasks.get(5, TimeUnit.SECONDS);
            System.out.println("所有CompletableFuture任务完成");
        } catch (Exception e) {
            System.out.println("CompletableFuture任务执行异常: " + e.getMessage());
        }
        
        // 异常处理示例
        CompletableFuture<String> futureWithException = CompletableFuture.supplyAsync(() -> {
            if (new Random().nextBoolean()) {
                throw new RuntimeException("模拟异常");
            }
            return "成功";
        }).exceptionally(throwable -> {
            System.out.println("处理异常: " + throwable.getMessage());
            return "默认值";
        });
        
        try {
            String result = futureWithException.get();
            System.out.println("异常处理结果: " + result);
        } catch (Exception e) {
            System.out.println("获取结果异常: " + e.getMessage());
        }
        
        System.out.println("\n=== 示例结束 ===");
    }
}