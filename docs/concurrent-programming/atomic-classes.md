# Java原子类详解

## 目录
- [原子类概述](#原子类概述)
- [基本原子类](#基本原子类)
- [数组原子类](#数组原子类)
- [引用原子类](#引用原子类)
- [字段更新器](#字段更新器)
- [累加器类](#累加器类)
- [CAS原理](#cas原理)
- [ABA问题](#aba问题)
- [性能对比](#性能对比)
- [最佳实践](#最佳实践)
- [面试要点](#面试要点)

## 原子类概述

### 什么是原子类
原子类是Java并发包（java.util.concurrent.atomic）中提供的一组类，它们提供了线程安全的原子操作，无需使用锁就能保证操作的原子性。

### 为什么需要原子类

#### 传统同步的问题
```java
public class TraditionalCounter {
    private int count = 0;
    
    // 使用synchronized保证线程安全，但性能较低
    public synchronized void increment() {
        count++;
    }
    
    public synchronized int getCount() {
        return count;
    }
}
```

#### 原子类的优势
```java
import java.util.concurrent.atomic.AtomicInteger;

public class AtomicCounter {
    private final AtomicInteger count = new AtomicInteger(0);
    
    // 无锁操作，性能更高
    public void increment() {
        count.incrementAndGet();
    }
    
    public int getCount() {
        return count.get();
    }
}
```

### 原子类的分类

1. **基本原子类**: AtomicInteger, AtomicLong, AtomicBoolean
2. **数组原子类**: AtomicIntegerArray, AtomicLongArray, AtomicReferenceArray
3. **引用原子类**: AtomicReference, AtomicStampedReference, AtomicMarkableReference
4. **字段更新器**: AtomicIntegerFieldUpdater, AtomicLongFieldUpdater, AtomicReferenceFieldUpdater
5. **累加器类**: LongAdder, DoubleAdder, LongAccumulator, DoubleAccumulator

## 基本原子类

### AtomicInteger

```java
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AtomicIntegerExample {
    private final AtomicInteger atomicInt = new AtomicInteger(0);
    
    public void basicOperations() {
        // 基本操作
        System.out.println("Initial value: " + atomicInt.get());
        
        // 设置值
        atomicInt.set(10);
        System.out.println("After set(10): " + atomicInt.get());
        
        // 获取并设置
        int oldValue = atomicInt.getAndSet(20);
        System.out.println("getAndSet(20) returned: " + oldValue + ", current: " + atomicInt.get());
        
        // 增加操作
        int newValue = atomicInt.incrementAndGet(); // ++i
        System.out.println("incrementAndGet(): " + newValue);
        
        int oldValue2 = atomicInt.getAndIncrement(); // i++
        System.out.println("getAndIncrement() returned: " + oldValue2 + ", current: " + atomicInt.get());
        
        // 减少操作
        atomicInt.decrementAndGet(); // --i
        atomicInt.getAndDecrement(); // i--
        
        // 加法操作
        atomicInt.addAndGet(5); // i += 5
        atomicInt.getAndAdd(3); // 返回旧值，然后加3
        
        System.out.println("Final value: " + atomicInt.get());
    }
    
    // CAS操作
    public void casOperations() {
        atomicInt.set(100);
        
        // 比较并设置：如果当前值等于期望值，则设置为新值
        boolean success = atomicInt.compareAndSet(100, 200);
        System.out.println("CAS(100, 200) success: " + success + ", current: " + atomicInt.get());
        
        // 失败的CAS
        boolean failed = atomicInt.compareAndSet(100, 300);
        System.out.println("CAS(100, 300) success: " + failed + ", current: " + atomicInt.get());
        
        // 弱CAS（可能虚假失败）
        while (!atomicInt.weakCompareAndSet(200, 250)) {
            // 重试直到成功
        }
        System.out.println("After weak CAS: " + atomicInt.get());
    }
    
    // 并发测试
    public void concurrencyTest() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger(0);
        final int threadCount = 10;
        final int incrementsPerThread = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    counter.incrementAndGet();
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        long endTime = System.currentTimeMillis();
        
        System.out.println("Expected: " + (threadCount * incrementsPerThread));
        System.out.println("Actual: " + counter.get());
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
    }
    
    public static void main(String[] args) throws InterruptedException {
        AtomicIntegerExample example = new AtomicIntegerExample();
        
        System.out.println("=== Basic Operations ===");
        example.basicOperations();
        
        System.out.println("\n=== CAS Operations ===");
        example.casOperations();
        
        System.out.println("\n=== Concurrency Test ===");
        example.concurrencyTest();
    }
}
```

### AtomicLong

```java
import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongExample {
    private final AtomicLong atomicLong = new AtomicLong(0L);
    
    public void demonstrateOperations() {
        // 基本操作
        atomicLong.set(1000000000L);
        System.out.println("Initial: " + atomicLong.get());
        
        // 大数值操作
        long result = atomicLong.addAndGet(2000000000L);
        System.out.println("After adding 2 billion: " + result);
        
        // 乘法操作（通过updateAndGet实现）
        long multiplied = atomicLong.updateAndGet(value -> value * 2);
        System.out.println("After multiplying by 2: " + multiplied);
        
        // 条件更新
        long updated = atomicLong.accumulateAndGet(1000000000L, (current, delta) -> {
            return current > 5000000000L ? current - delta : current + delta;
        });
        System.out.println("After conditional update: " + updated);
    }
    
    public static void main(String[] args) {
        AtomicLongExample example = new AtomicLongExample();
        example.demonstrateOperations();
    }
}
```

### AtomicBoolean

```java
import java.util.concurrent.atomic.AtomicBoolean;

public class AtomicBooleanExample {
    private final AtomicBoolean flag = new AtomicBoolean(false);
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    
    // 一次性初始化
    public void initializeOnce() {
        if (initialized.compareAndSet(false, true)) {
            System.out.println("Initializing resources...");
            // 执行初始化逻辑
            try {
                Thread.sleep(1000); // 模拟初始化耗时
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println("Initialization completed");
        } else {
            System.out.println("Already initialized");
        }
    }
    
    // 开关控制
    public void toggleSwitch() {
        boolean oldValue = flag.getAndSet(!flag.get());
        System.out.println("Switch toggled from " + oldValue + " to " + flag.get());
    }
    
    // 条件执行
    public void conditionalExecution() {
        if (flag.compareAndSet(false, true)) {
            try {
                System.out.println("Executing critical section...");
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                flag.set(false); // 重置标志
            }
        } else {
            System.out.println("Critical section is busy");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        AtomicBooleanExample example = new AtomicBooleanExample();
        
        // 测试一次性初始化
        for (int i = 0; i < 3; i++) {
            new Thread(example::initializeOnce).start();
        }
        
        Thread.sleep(2000);
        
        // 测试开关控制
        example.toggleSwitch();
        example.toggleSwitch();
        
        // 测试条件执行
        for (int i = 0; i < 3; i++) {
            new Thread(example::conditionalExecution).start();
        }
    }
}
```

## 数组原子类

### AtomicIntegerArray

```java
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AtomicIntegerArrayExample {
    private final AtomicIntegerArray atomicArray;
    
    public AtomicIntegerArrayExample(int size) {
        this.atomicArray = new AtomicIntegerArray(size);
    }
    
    public void basicOperations() {
        // 设置值
        atomicArray.set(0, 10);
        atomicArray.set(1, 20);
        atomicArray.set(2, 30);
        
        System.out.println("Initial array:");
        printArray();
        
        // 原子操作
        int oldValue = atomicArray.getAndSet(0, 100);
        System.out.println("getAndSet(0, 100) returned: " + oldValue);
        
        atomicArray.incrementAndGet(1);
        atomicArray.addAndGet(2, 5);
        
        System.out.println("After operations:");
        printArray();
        
        // CAS操作
        boolean success = atomicArray.compareAndSet(0, 100, 200);
        System.out.println("CAS(0, 100, 200) success: " + success);
        
        System.out.println("Final array:");
        printArray();
    }
    
    // 并发累加测试
    public void concurrentSum() throws InterruptedException {
        final int arraySize = 10;
        final int threadCount = 5;
        final int operationsPerThread = 1000;
        
        AtomicIntegerArray array = new AtomicIntegerArray(arraySize);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // 每个线程对数组的不同位置进行操作
        for (int i = 0; i < threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    int index = threadIndex % arraySize;
                    array.incrementAndGet(index);
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        System.out.println("Concurrent sum results:");
        int totalSum = 0;
        for (int i = 0; i < arraySize; i++) {
            int value = array.get(i);
            System.out.println("Index " + i + ": " + value);
            totalSum += value;
        }
        System.out.println("Total sum: " + totalSum);
        System.out.println("Expected: " + (threadCount * operationsPerThread));
    }
    
    // 数组元素的条件更新
    public void conditionalUpdate() {
        atomicArray.set(0, 5);
        
        // 只有当值大于3时才更新
        int result = atomicArray.updateAndGet(0, value -> value > 3 ? value * 2 : value);
        System.out.println("Conditional update result: " + result);
        
        // 累积操作
        atomicArray.accumulateAndGet(0, 10, Integer::max);
        System.out.println("After max accumulation: " + atomicArray.get(0));
    }
    
    private void printArray() {
        for (int i = 0; i < atomicArray.length(); i++) {
            System.out.print(atomicArray.get(i) + " ");
        }
        System.out.println();
    }
    
    public static void main(String[] args) throws InterruptedException {
        AtomicIntegerArrayExample example = new AtomicIntegerArrayExample(5);
        
        System.out.println("=== Basic Operations ===");
        example.basicOperations();
        
        System.out.println("\n=== Conditional Update ===");
        example.conditionalUpdate();
        
        System.out.println("\n=== Concurrent Sum Test ===");
        example.concurrentSum();
    }
}
```

### AtomicReferenceArray

```java
import java.util.concurrent.atomic.AtomicReferenceArray;

public class AtomicReferenceArrayExample {
    private final AtomicReferenceArray<String> atomicRefArray;
    
    public AtomicReferenceArrayExample(int size) {
        this.atomicRefArray = new AtomicReferenceArray<>(size);
    }
    
    public void demonstrateOperations() {
        // 初始化
        atomicRefArray.set(0, "Hello");
        atomicRefArray.set(1, "World");
        atomicRefArray.set(2, "Java");
        
        System.out.println("Initial array:");
        printArray();
        
        // 原子替换
        String oldValue = atomicRefArray.getAndSet(0, "Hi");
        System.out.println("Replaced '" + oldValue + "' with 'Hi'");
        
        // CAS操作
        boolean success = atomicRefArray.compareAndSet(1, "World", "Universe");
        System.out.println("CAS 'World' to 'Universe': " + success);
        
        // 条件更新
        atomicRefArray.updateAndGet(2, value -> value != null ? value.toUpperCase() : null);
        
        System.out.println("Final array:");
        printArray();
    }
    
    private void printArray() {
        for (int i = 0; i < atomicRefArray.length(); i++) {
            System.out.print("[" + i + "]: " + atomicRefArray.get(i) + " ");
        }
        System.out.println();
    }
    
    public static void main(String[] args) {
        AtomicReferenceArrayExample example = new AtomicReferenceArrayExample(3);
        example.demonstrateOperations();
    }
}
```

## 引用原子类

### AtomicReference

```java
import java.util.concurrent.atomic.AtomicReference;

public class AtomicReferenceExample {
    
    static class Person {
        private final String name;
        private final int age;
        
        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() { return name; }
        public int getAge() { return age; }
        
        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + "}";
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Person person = (Person) obj;
            return age == person.age && name.equals(person.name);
        }
    }
    
    private final AtomicReference<Person> atomicPerson = new AtomicReference<>();
    
    public void basicOperations() {
        // 设置初始值
        Person john = new Person("John", 25);
        atomicPerson.set(john);
        System.out.println("Initial: " + atomicPerson.get());
        
        // 原子替换
        Person jane = new Person("Jane", 30);
        Person oldPerson = atomicPerson.getAndSet(jane);
        System.out.println("Replaced: " + oldPerson + " with " + atomicPerson.get());
        
        // CAS操作
        Person bob = new Person("Bob", 35);
        boolean success = atomicPerson.compareAndSet(jane, bob);
        System.out.println("CAS success: " + success + ", current: " + atomicPerson.get());
        
        // 条件更新
        atomicPerson.updateAndGet(person -> 
            person.getAge() > 30 ? new Person(person.getName(), person.getAge() + 1) : person
        );
        System.out.println("After conditional update: " + atomicPerson.get());
    }
    
    // 实现无锁栈
    static class LockFreeStack<T> {
        private final AtomicReference<Node<T>> top = new AtomicReference<>();
        
        private static class Node<T> {
            final T data;
            final Node<T> next;
            
            Node(T data, Node<T> next) {
                this.data = data;
                this.next = next;
            }
        }
        
        public void push(T item) {
            Node<T> newNode = new Node<>(item, null);
            Node<T> currentTop;
            do {
                currentTop = top.get();
                newNode.next = currentTop;
            } while (!top.compareAndSet(currentTop, newNode));
        }
        
        public T pop() {
            Node<T> currentTop;
            Node<T> newTop;
            do {
                currentTop = top.get();
                if (currentTop == null) {
                    return null;
                }
                newTop = currentTop.next;
            } while (!top.compareAndSet(currentTop, newTop));
            
            return currentTop.data;
        }
        
        public boolean isEmpty() {
            return top.get() == null;
        }
    }
    
    public void testLockFreeStack() {
        LockFreeStack<String> stack = new LockFreeStack<>();
        
        // 推入元素
        stack.push("First");
        stack.push("Second");
        stack.push("Third");
        
        // 弹出元素
        System.out.println("Popped: " + stack.pop());
        System.out.println("Popped: " + stack.pop());
        System.out.println("Popped: " + stack.pop());
        System.out.println("Popped: " + stack.pop()); // null
    }
    
    public static void main(String[] args) {
        AtomicReferenceExample example = new AtomicReferenceExample();
        
        System.out.println("=== Basic Operations ===");
        example.basicOperations();
        
        System.out.println("\n=== Lock-Free Stack ===");
        example.testLockFreeStack();
    }
}
```

### AtomicStampedReference

```java
import java.util.concurrent.atomic.AtomicStampedReference;

public class AtomicStampedReferenceExample {
    
    // 解决ABA问题的示例
    public void solveABAProblem() {
        String initialValue = "A";
        int initialStamp = 0;
        
        AtomicStampedReference<String> atomicStampedRef = 
            new AtomicStampedReference<>(initialValue, initialStamp);
        
        System.out.println("Initial: value=" + atomicStampedRef.getReference() + 
                          ", stamp=" + atomicStampedRef.getStamp());
        
        // 线程1：A -> B -> A
        Thread thread1 = new Thread(() -> {
            int[] stampHolder = new int[1];
            String value = atomicStampedRef.get(stampHolder);
            int stamp = stampHolder[0];
            
            System.out.println("Thread1: Read value=" + value + ", stamp=" + stamp);
            
            // 模拟一些处理时间
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // A -> B
            boolean success1 = atomicStampedRef.compareAndSet(value, "B", stamp, stamp + 1);
            System.out.println("Thread1: A->B success=" + success1);
            
            // B -> A
            if (success1) {
                boolean success2 = atomicStampedRef.compareAndSet("B", "A", stamp + 1, stamp + 2);
                System.out.println("Thread1: B->A success=" + success2);
            }
        });
        
        // 线程2：尝试基于初始状态进行CAS
        Thread thread2 = new Thread(() -> {
            int[] stampHolder = new int[1];
            String value = atomicStampedRef.get(stampHolder);
            int stamp = stampHolder[0];
            
            System.out.println("Thread2: Read value=" + value + ", stamp=" + stamp);
            
            // 等待线程1完成ABA操作
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 尝试基于初始状态进行CAS
            boolean success = atomicStampedRef.compareAndSet(value, "C", stamp, stamp + 1);
            System.out.println("Thread2: A->C success=" + success + " (should be false due to stamp change)");
            
            // 获取当前状态
            String currentValue = atomicStampedRef.get(stampHolder);
            int currentStamp = stampHolder[0];
            System.out.println("Thread2: Current value=" + currentValue + ", stamp=" + currentStamp);
        });
        
        thread1.start();
        thread2.start();
        
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Final: value=" + atomicStampedRef.getReference() + 
                          ", stamp=" + atomicStampedRef.getStamp());
    }
    
    // 版本控制示例
    static class VersionedData {
        private final AtomicStampedReference<String> data;
        
        public VersionedData(String initialData) {
            this.data = new AtomicStampedReference<>(initialData, 1);
        }
        
        public boolean updateData(String expectedData, String newData) {
            int[] stampHolder = new int[1];
            String currentData = data.get(stampHolder);
            int currentVersion = stampHolder[0];
            
            if (currentData.equals(expectedData)) {
                boolean success = data.compareAndSet(currentData, newData, currentVersion, currentVersion + 1);
                if (success) {
                    System.out.println("Updated data from '" + expectedData + "' to '" + newData + 
                                     "', version: " + currentVersion + " -> " + (currentVersion + 1));
                }
                return success;
            }
            return false;
        }
        
        public String getData() {
            return data.getReference();
        }
        
        public int getVersion() {
            return data.getStamp();
        }
    }
    
    public void testVersionedData() {
        VersionedData versionedData = new VersionedData("Initial Data");
        
        System.out.println("Initial: " + versionedData.getData() + ", version: " + versionedData.getVersion());
        
        // 正确的更新
        versionedData.updateData("Initial Data", "Updated Data");
        
        // 基于过期数据的更新（应该失败）
        boolean success = versionedData.updateData("Initial Data", "Wrong Update");
        System.out.println("Update with stale data success: " + success);
        
        // 正确的更新
        versionedData.updateData("Updated Data", "Final Data");
        
        System.out.println("Final: " + versionedData.getData() + ", version: " + versionedData.getVersion());
    }
    
    public static void main(String[] args) {
        AtomicStampedReferenceExample example = new AtomicStampedReferenceExample();
        
        System.out.println("=== Solving ABA Problem ===");
        example.solveABAProblem();
        
        System.out.println("\n=== Versioned Data Example ===");
        example.testVersionedData();
    }
}
```

### AtomicMarkableReference

```java
import java.util.concurrent.atomic.AtomicMarkableReference;

public class AtomicMarkableReferenceExample {
    
    // 实现带标记的无锁链表节点删除
    static class Node {
        final String data;
        final AtomicMarkableReference<Node> next;
        
        Node(String data) {
            this.data = data;
            this.next = new AtomicMarkableReference<>(null, false);
        }
        
        @Override
        public String toString() {
            return "Node{" + data + "}";
        }
    }
    
    static class LockFreeLinkedList {
        private final Node head;
        
        public LockFreeLinkedList() {
            this.head = new Node("HEAD");
        }
        
        public void add(String data) {
            Node newNode = new Node(data);
            while (true) {
                Node current = head;
                while (current.next.getReference() != null) {
                    current = current.next.getReference();
                }
                
                if (current.next.compareAndSet(null, newNode, false, false)) {
                    System.out.println("Added: " + data);
                    break;
                }
            }
        }
        
        public boolean remove(String data) {
            while (true) {
                Node prev = head;
                Node current = prev.next.getReference();
                
                while (current != null) {
                    Node next = current.next.getReference();
                    boolean marked = current.next.isMarked();
                    
                    if (marked) {
                        // 节点已被标记删除，帮助完成删除
                        prev.next.compareAndSet(current, next, false, false);
                        current = next;
                        continue;
                    }
                    
                    if (current.data.equals(data)) {
                        // 找到目标节点，先标记为删除
                        if (current.next.compareAndSet(next, next, false, true)) {
                            // 标记成功，尝试物理删除
                            prev.next.compareAndSet(current, next, false, false);
                            System.out.println("Removed: " + data);
                            return true;
                        }
                        // 标记失败，重试
                        break;
                    }
                    
                    prev = current;
                    current = next;
                }
                
                if (current == null) {
                    System.out.println("Not found: " + data);
                    return false;
                }
            }
        }
        
        public void printList() {
            Node current = head.next.getReference();
            System.out.print("List: ");
            while (current != null) {
                if (!current.next.isMarked()) {
                    System.out.print(current.data + " ");
                }
                current = current.next.getReference();
            }
            System.out.println();
        }
    }
    
    public void testLockFreeLinkedList() {
        LockFreeLinkedList list = new LockFreeLinkedList();
        
        // 添加元素
        list.add("A");
        list.add("B");
        list.add("C");
        list.printList();
        
        // 删除元素
        list.remove("B");
        list.printList();
        
        list.remove("D"); // 不存在的元素
        list.printList();
        
        list.remove("A");
        list.printList();
    }
    
    // 简单的标记示例
    public void basicMarkableExample() {
        AtomicMarkableReference<String> markableRef = 
            new AtomicMarkableReference<>("Initial", false);
        
        System.out.println("Initial: value=" + markableRef.getReference() + 
                          ", marked=" + markableRef.isMarked());
        
        // 设置标记
        String value = markableRef.getReference();
        boolean success = markableRef.compareAndSet(value, value, false, true);
        System.out.println("Mark success: " + success);
        System.out.println("After marking: value=" + markableRef.getReference() + 
                          ", marked=" + markableRef.isMarked());
        
        // 更新值和标记
        success = markableRef.compareAndSet(value, "Updated", true, false);
        System.out.println("Update success: " + success);
        System.out.println("After update: value=" + markableRef.getReference() + 
                          ", marked=" + markableRef.isMarked());
        
        // 尝试基于错误的标记状态进行更新
        success = markableRef.compareAndSet("Updated", "Wrong", true, false);
        System.out.println("Wrong mark update success: " + success);
    }
    
    public static void main(String[] args) {
        AtomicMarkableReferenceExample example = new AtomicMarkableReferenceExample();
        
        System.out.println("=== Basic Markable Example ===");
        example.basicMarkableExample();
        
        System.out.println("\n=== Lock-Free Linked List ===");
        example.testLockFreeLinkedList();
    }
}
```

## 字段更新器

### AtomicIntegerFieldUpdater

```java
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class AtomicFieldUpdaterExample {
    
    static class Counter {
        volatile int count = 0;
        volatile long longValue = 0L;
        volatile String name = "initial";
        
        @Override
        public String toString() {
            return "Counter{count=" + count + ", longValue=" + longValue + ", name='" + name + "'}";
        }
    }
    
    // 字段更新器
    private static final AtomicIntegerFieldUpdater<Counter> COUNT_UPDATER = 
        AtomicIntegerFieldUpdater.newUpdater(Counter.class, "count");
    
    private static final AtomicLongFieldUpdater<Counter> LONG_UPDATER = 
        AtomicLongFieldUpdater.newUpdater(Counter.class, "longValue");
    
    private static final AtomicReferenceFieldUpdater<Counter, String> NAME_UPDATER = 
        AtomicReferenceFieldUpdater.newUpdater(Counter.class, String.class, "name");
    
    public void demonstrateFieldUpdaters() {
        Counter counter = new Counter();
        System.out.println("Initial: " + counter);
        
        // 使用AtomicIntegerFieldUpdater
        int oldCount = COUNT_UPDATER.getAndIncrement(counter);
        System.out.println("After increment: count changed from " + oldCount + " to " + counter.count);
        
        COUNT_UPDATER.addAndGet(counter, 5);
        System.out.println("After adding 5: " + counter);
        
        // CAS操作
        boolean success = COUNT_UPDATER.compareAndSet(counter, 6, 10);
        System.out.println("CAS(6, 10) success: " + success + ", current count: " + counter.count);
        
        // 使用AtomicLongFieldUpdater
        LONG_UPDATER.set(counter, 1000000L);
        long oldLong = LONG_UPDATER.getAndAdd(counter, 2000000L);
        System.out.println("Long value changed from " + oldLong + " to " + counter.longValue);
        
        // 使用AtomicReferenceFieldUpdater
        String oldName = NAME_UPDATER.getAndSet(counter, "updated");
        System.out.println("Name changed from '" + oldName + "' to '" + counter.name + "'");
        
        System.out.println("Final: " + counter);
    }
    
    // 性能对比示例
    public void performanceComparison() {
        final int iterations = 1000000;
        
        // 使用AtomicInteger
        AtomicInteger atomicInt = new AtomicInteger(0);
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            atomicInt.incrementAndGet();
        }
        long atomicTime = System.nanoTime() - start;
        
        // 使用AtomicIntegerFieldUpdater
        Counter counter = new Counter();
        start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            COUNT_UPDATER.incrementAndGet(counter);
        }
        long updaterTime = System.nanoTime() - start;
        
        System.out.println("Performance comparison (" + iterations + " iterations):");
        System.out.println("AtomicInteger: " + atomicTime / 1000000 + "ms");
        System.out.println("FieldUpdater: " + updaterTime / 1000000 + "ms");
        System.out.println("Results: AtomicInteger=" + atomicInt.get() + ", FieldUpdater=" + counter.count);
    }
    
    // 多线程测试
    public void concurrencyTest() throws InterruptedException {
        final Counter counter = new Counter();
        final int threadCount = 10;
        final int incrementsPerThread = 1000;
        
        Thread[] threads = new Thread[threadCount];
        
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < incrementsPerThread; j++) {
                    COUNT_UPDATER.incrementAndGet(counter);
                }
            });
        }
        
        long start = System.currentTimeMillis();
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        for (Thread thread : threads) {
            thread.join();
        }
        
        long end = System.currentTimeMillis();
        
        System.out.println("Concurrency test results:");
        System.out.println("Expected: " + (threadCount * incrementsPerThread));
        System.out.println("Actual: " + counter.count);
        System.out.println("Time taken: " + (end - start) + "ms");
    }
    
    public static void main(String[] args) throws InterruptedException {
        AtomicFieldUpdaterExample example = new AtomicFieldUpdaterExample();
        
        System.out.println("=== Field Updater Demo ===");
        example.demonstrateFieldUpdaters();
        
        System.out.println("\n=== Performance Comparison ===");
        example.performanceComparison();
        
        System.out.println("\n=== Concurrency Test ===");
        example.concurrencyTest();
    }
}
```

## 累加器类

### LongAdder

```java
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LongAdderExample {
    
    public void basicOperations() {
        LongAdder adder = new LongAdder();
        
        // 基本操作
        adder.add(10);
        adder.increment();
        adder.add(5);
        
        System.out.println("Sum: " + adder.sum());
        System.out.println("Sum and reset: " + adder.sumThenReset());
        System.out.println("After reset: " + adder.sum());
        
        // 重新累加
        adder.add(100);
        adder.decrement();
        System.out.println("Final sum: " + adder.sum());
    }
    
    // 性能对比：LongAdder vs AtomicLong
    public void performanceComparison() throws InterruptedException {
        final int threadCount = 10;
        final int operationsPerThread = 100000;
        
        // 测试AtomicLong
        AtomicLong atomicLong = new AtomicLong(0);
        long start = System.currentTimeMillis();
        
        ExecutorService executor1 = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor1.submit(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    atomicLong.incrementAndGet();
                }
            });
        }
        
        executor1.shutdown();
        executor1.awaitTermination(30, TimeUnit.SECONDS);
        long atomicTime = System.currentTimeMillis() - start;
        
        // 测试LongAdder
        LongAdder longAdder = new LongAdder();
        start = System.currentTimeMillis();
        
        ExecutorService executor2 = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor2.submit(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    longAdder.increment();
                }
            });
        }
        
        executor2.shutdown();
        executor2.awaitTermination(30, TimeUnit.SECONDS);
        long adderTime = System.currentTimeMillis() - start;
        
        System.out.println("Performance comparison (" + threadCount + " threads, " + 
                          operationsPerThread + " ops each):");
        System.out.println("AtomicLong: " + atomicTime + "ms, result: " + atomicLong.get());
        System.out.println("LongAdder: " + adderTime + "ms, result: " + longAdder.sum());
        System.out.println("LongAdder is " + (atomicTime / (double) adderTime) + "x faster");
    }
    
    // 实际应用：计数器服务
    static class CounterService {
        private final LongAdder requestCount = new LongAdder();
        private final LongAdder errorCount = new LongAdder();
        private final LongAdder totalResponseTime = new LongAdder();
        
        public void recordRequest(long responseTime, boolean isError) {
            requestCount.increment();
            totalResponseTime.add(responseTime);
            if (isError) {
                errorCount.increment();
            }
        }
        
        public void printStatistics() {
            long requests = requestCount.sum();
            long errors = errorCount.sum();
            long totalTime = totalResponseTime.sum();
            
            System.out.println("=== Service Statistics ===");
            System.out.println("Total requests: " + requests);
            System.out.println("Error count: " + errors);
            System.out.println("Error rate: " + (requests > 0 ? (errors * 100.0 / requests) : 0) + "%");
            System.out.println("Average response time: " + (requests > 0 ? (totalTime / requests) : 0) + "ms");
        }
        
        public void reset() {
            requestCount.reset();
            errorCount.reset();
            totalResponseTime.reset();
        }
    }
    
    public void testCounterService() throws InterruptedException {
        CounterService service = new CounterService();
        final int threadCount = 5;
        final int requestsPerThread = 1000;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    // 模拟请求处理
                    long responseTime = 50 + (long) (Math.random() * 200); // 50-250ms
                    boolean isError = Math.random() < 0.05; // 5%错误率
                    
                    service.recordRequest(responseTime, isError);
                    
                    // 模拟请求间隔
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                System.out.println("Thread " + threadId + " completed");
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        
        service.printStatistics();
    }
    
    public static void main(String[] args) throws InterruptedException {
        LongAdderExample example = new LongAdderExample();
        
        System.out.println("=== Basic Operations ===");
        example.basicOperations();
        
        System.out.println("\n=== Performance Comparison ===");
        example.performanceComparison();
        
        System.out.println("\n=== Counter Service Test ===");
        example.testCounterService();
    }
}
```

### LongAccumulator

```java
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LongAccumulatorExample {
    
    public void basicOperations() {
        // 求最大值
        LongAccumulator maxAccumulator = new LongAccumulator(Long::max, Long.MIN_VALUE);
        maxAccumulator.accumulate(10);
        maxAccumulator.accumulate(5);
        maxAccumulator.accumulate(20);
        maxAccumulator.accumulate(15);
        System.out.println("Max value: " + maxAccumulator.get());
        
        // 求最小值
        LongAccumulator minAccumulator = new LongAccumulator(Long::min, Long.MAX_VALUE);
        minAccumulator.accumulate(10);
        minAccumulator.accumulate(5);
        minAccumulator.accumulate(20);
        minAccumulator.accumulate(15);
        System.out.println("Min value: " + minAccumulator.get());
        
        // 求乘积
        LongAccumulator productAccumulator = new LongAccumulator((x, y) -> x * y, 1);
        productAccumulator.accumulate(2);
        productAccumulator.accumulate(3);
        productAccumulator.accumulate(4);
        System.out.println("Product: " + productAccumulator.get());
        
        // 自定义操作：计算平方和
        LongAccumulator squareSumAccumulator = new LongAccumulator((x, y) -> x + y * y, 0);
        squareSumAccumulator.accumulate(1); // 0 + 1*1 = 1
        squareSumAccumulator.accumulate(2); // 1 + 2*2 = 5
        squareSumAccumulator.accumulate(3); // 5 + 3*3 = 14
        System.out.println("Square sum: " + squareSumAccumulator.get());
    }
    
    // 并发统计示例
    public void concurrentStatistics() throws InterruptedException {
        final LongAccumulator maxTemp = new LongAccumulator(Long::max, Long.MIN_VALUE);
        final LongAccumulator minTemp = new LongAccumulator(Long::min, Long.MAX_VALUE);
        final LongAccumulator sumTemp = new LongAccumulator(Long::sum, 0);
        final LongAccumulator count = new LongAccumulator(Long::sum, 0);
        
        final int sensorCount = 10;
        final int readingsPerSensor = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(sensorCount);
        
        for (int i = 0; i < sensorCount; i++) {
            final int sensorId = i;
            executor.submit(() -> {
                for (int j = 0; j < readingsPerSensor; j++) {
                    // 模拟温度读数 (0-40度)
                    long temperature = (long) (Math.random() * 40);
                    
                    maxTemp.accumulate(temperature);
                    minTemp.accumulate(temperature);
                    sumTemp.accumulate(temperature);
                    count.accumulate(1);
                    
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                System.out.println("Sensor " + sensorId + " completed readings");
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
        
        long totalReadings = count.get();
        System.out.println("\n=== Temperature Statistics ===");
        System.out.println("Total readings: " + totalReadings);
        System.out.println("Max temperature: " + maxTemp.get() + "°C");
        System.out.println("Min temperature: " + minTemp.get() + "°C");
        System.out.println("Average temperature: " + (sumTemp.get() / (double) totalReadings) + "°C");
    }
    
    // 实现并发计数器，支持不同的聚合函数
    static class FlexibleCounter {
        private final LongAccumulator accumulator;
        private final String operation;
        
        public FlexibleCounter(String operation, LongBinaryOperator function, long identity) {
            this.operation = operation;
            this.accumulator = new LongAccumulator(function, identity);
        }
        
        public void update(long value) {
            accumulator.accumulate(value);
        }
        
        public long getResult() {
            return accumulator.get();
        }
        
        public void reset() {
            accumulator.reset();
        }
        
        @Override
        public String toString() {
            return operation + ": " + getResult();
        }
    }
    
    public void testFlexibleCounter() throws InterruptedException {
        FlexibleCounter sumCounter = new FlexibleCounter("Sum", Long::sum, 0);
        FlexibleCounter maxCounter = new FlexibleCounter("Max", Long::max, Long.MIN_VALUE);
        FlexibleCounter minCounter = new FlexibleCounter("Min", Long::min, Long.MAX_VALUE);
        FlexibleCounter productCounter = new FlexibleCounter("Product", (x, y) -> x * y, 1);
        
        final int threadCount = 5;
        final int valuesPerThread = 20;
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < valuesPerThread; j++) {
                    long value = threadId * valuesPerThread + j + 1; // 1, 2, 3, ..., 100
                    
                    sumCounter.update(value);
                    maxCounter.update(value);
                    minCounter.update(value);
                    
                    // 只对小值计算乘积，避免溢出
                    if (value <= 10) {
                        productCounter.update(value);
                    }
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        System.out.println("\n=== Flexible Counter Results ===");
        System.out.println(sumCounter);
        System.out.println(maxCounter);
        System.out.println(minCounter);
        System.out.println(productCounter);
    }
    
    public static void main(String[] args) throws InterruptedException {
        LongAccumulatorExample example = new LongAccumulatorExample();
        
        System.out.println("=== Basic Operations ===");
        example.basicOperations();
        
        System.out.println("\n=== Concurrent Statistics ===");
        example.concurrentStatistics();
        
        System.out.println("\n=== Flexible Counter Test ===");
        example.testFlexibleCounter();
    }
}
```