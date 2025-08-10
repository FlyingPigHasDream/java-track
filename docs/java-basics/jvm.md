# JVM虚拟机

## 目录
- [JVM概述](#jvm概述)
- [JVM内存结构](#jvm内存结构)
- [类加载机制](#类加载机制)
- [垃圾回收机制](#垃圾回收机制)
- [JVM调优](#jvm调优)
- [常用JVM工具](#常用jvm工具)
- [面试要点](#面试要点)

## JVM概述

### 什么是JVM
JVM (Java Virtual Machine) 是Java虚拟机，是Java程序的运行环境。它负责将Java字节码转换为特定平台的机器码，实现了Java的"一次编写，到处运行"特性。

### JVM的作用
1. **平台无关性**: 屏蔽底层操作系统差异
2. **内存管理**: 自动管理内存分配和回收
3. **安全性**: 提供安全的执行环境
4. **性能优化**: 运行时优化和即时编译

### JVM、JRE、JDK的关系
```
┌─────────────────────────────────────┐
│                JDK                  │
│  ┌───────────────────────────────┐  │
│  │             JRE               │  │
│  │  ┌─────────────────────────┐  │  │
│  │  │         JVM             │  │  │
│  │  │                         │  │  │
│  │  └─────────────────────────┘  │  │
│  │  + Java核心类库              │  │
│  └───────────────────────────────┘  │
│  + 开发工具 (javac, jar, etc.)      │
└─────────────────────────────────────┘
```

## JVM内存结构

### 整体内存布局
```
┌─────────────────────────────────────┐
│              JVM内存                │
├─────────────────┬───────────────────┤
│    线程共享区    │    线程私有区      │
├─────────────────┼───────────────────┤
│   方法区(元空间) │    程序计数器      │
│                 │                   │
│      堆         │    Java虚拟机栈    │
│                 │                   │
│                 │    本地方法栈      │
└─────────────────┴───────────────────┘
```

### 1. 程序计数器 (Program Counter Register)

#### 特点
- **线程私有**: 每个线程都有独立的程序计数器
- **最小内存区域**: 占用内存很小
- **唯一不会OOM**: 不会出现OutOfMemoryError

#### 作用
- 记录当前线程执行的字节码指令地址
- 线程切换时保存和恢复执行位置
- 多线程时分片轮转的依据

### 2. Java虚拟机栈 (Java Virtual Machine Stack)

#### 特点
- **线程私有**: 每个线程都有独立的虚拟机栈
- **生命周期**: 与线程相同
- **存储内容**: 局部变量表、操作数栈、动态链接、方法出口

#### 栈帧结构
```
┌─────────────────────────────────┐
│           栈帧 (Stack Frame)     │
├─────────────────────────────────┤
│         局部变量表              │
├─────────────────────────────────┤
│         操作数栈                │
├─────────────────────────────────┤
│         动态链接                │
├─────────────────────────────────┤
│         方法返回地址            │
└─────────────────────────────────┘
```

#### 局部变量表
- 存储方法参数和局部变量
- 以变量槽(Slot)为最小单位
- long和double占用2个槽，其他类型占用1个槽
- 编译期确定大小

#### 可能的异常
- **StackOverflowError**: 栈深度超过限制
- **OutOfMemoryError**: 动态扩展时内存不足

### 3. 本地方法栈 (Native Method Stack)

#### 特点
- 为Native方法服务
- 与虚拟机栈类似，但服务于native方法
- HotSpot虚拟机中与Java虚拟机栈合并

### 4. 堆 (Heap)

#### 特点
- **线程共享**: 所有线程共享堆内存
- **最大内存区域**: JVM中最大的内存区域
- **垃圾回收主要区域**: GC主要针对堆内存
- **存储内容**: 对象实例和数组

#### 堆内存分代
```
┌─────────────────────────────────────────┐
│                  堆内存                  │
├─────────────────────┬───────────────────┤
│       新生代         │      老年代        │
├─────────┬───────────┤                   │
│  Eden   │ Survivor  │                   │
│         ├─────┬─────┤                   │
│         │ S0  │ S1  │                   │
└─────────┴─────┴─────┴───────────────────┘
```

#### 新生代 (Young Generation)
- **Eden区**: 新对象分配的区域
- **Survivor区**: 分为S0和S1两个区域，存放经过一次GC后存活的对象
- **特点**: 对象朝生夕死，GC频繁但速度快

#### 老年代 (Old Generation)
- 存放长期存活的对象
- 从新生代晋升的对象
- GC频率低但耗时长

#### 对象分配过程
```java
// 对象分配流程示例
public class ObjectAllocation {
    public static void main(String[] args) {
        // 1. 新对象在Eden区分配
        Object obj1 = new Object();
        
        // 2. Eden区满时触发Minor GC
        // 3. 存活对象移到Survivor区
        // 4. 经过多次GC后移到老年代
        
        // 大对象直接进入老年代
        byte[] bigArray = new byte[1024 * 1024 * 10]; // 10MB
    }
}
```

### 5. 方法区 (Method Area) / 元空间 (Metaspace)

#### Java 8之前：永久代 (PermGen)
- 存储类信息、常量、静态变量
- 容易发生OutOfMemoryError
- 大小固定，难以调优

#### Java 8之后：元空间 (Metaspace)
- 使用本地内存，不再受JVM堆大小限制
- 自动扩容，减少OOM风险
- 更好的垃圾回收机制

#### 存储内容
- 类的元数据信息
- 运行时常量池
- 静态变量 (Java 8后移到堆中)
- 即时编译器编译后的代码

### 6. 直接内存 (Direct Memory)

#### 特点
- 不属于JVM内存，但被JVM使用
- NIO操作中的DirectByteBuffer
- 避免Java堆和Native堆之间的数据复制

#### 示例
```java
// 直接内存使用示例
ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024);
// 直接在堆外分配内存，减少GC压力
```

## 类加载机制

### 类加载过程

#### 1. 加载 (Loading)
- 通过类的全限定名获取二进制字节流
- 将字节流转换为方法区的运行时数据结构
- 在内存中生成Class对象

#### 2. 验证 (Verification)
- **文件格式验证**: 验证字节流格式
- **元数据验证**: 验证类的元数据信息
- **字节码验证**: 验证字节码的语义
- **符号引用验证**: 验证符号引用的正确性

#### 3. 准备 (Preparation)
- 为类变量分配内存并设置初始值
- 只处理static变量，不包括实例变量
- 初始值是数据类型的零值，不是代码中的赋值

```java
public class PrepareExample {
    private static int value = 123; // 准备阶段value=0，初始化阶段value=123
    private static final int CONSTANT = 456; // 编译期常量，准备阶段就是456
}
```

#### 4. 解析 (Resolution)
- 将符号引用替换为直接引用
- 类或接口的解析
- 字段解析
- 方法解析

#### 5. 初始化 (Initialization)
- 执行类构造器`<clinit>()`方法
- 按照代码顺序收集类变量赋值和静态代码块
- 父类的`<clinit>()`先于子类执行

### 类加载器

#### 类加载器层次
```
┌─────────────────────────┐
│    启动类加载器          │
│   (Bootstrap ClassLoader)│
└─────────────────────────┘
            │
┌─────────────────────────┐
│    扩展类加载器          │
│  (Extension ClassLoader) │
└─────────────────────────┘
            │
┌─────────────────────────┐
│    应用程序类加载器      │
│ (Application ClassLoader)│
└─────────────────────────┘
            │
┌─────────────────────────┐
│    自定义类加载器        │
│   (Custom ClassLoader)   │
└─────────────────────────┘
```

#### 双亲委派模型
```java
// 双亲委派模型的实现
protected Class<?> loadClass(String name, boolean resolve) {
    synchronized (getClassLoadingLock(name)) {
        // 首先检查类是否已经被加载
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                if (parent != null) {
                    // 委派给父类加载器
                    c = parent.loadClass(name, false);
                } else {
                    // 委派给启动类加载器
                    c = findBootstrapClassOrNull(name);
                }
            } catch (ClassNotFoundException e) {
                // 父类加载器无法加载
            }
            
            if (c == null) {
                // 父类加载器无法加载，自己尝试加载
                c = findClass(name);
            }
        }
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }
}
```

#### 自定义类加载器
```java
public class CustomClassLoader extends ClassLoader {
    private String classPath;
    
    public CustomClassLoader(String classPath) {
        this.classPath = classPath;
    }
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            byte[] classData = loadClassData(name);
            return defineClass(name, classData, 0, classData.length);
        } catch (IOException e) {
            throw new ClassNotFoundException("Class not found: " + name, e);
        }
    }
    
    private byte[] loadClassData(String className) throws IOException {
        String fileName = classPath + File.separator + 
                         className.replace('.', File.separatorChar) + ".class";
        return Files.readAllBytes(Paths.get(fileName));
    }
}
```

## 垃圾回收机制

### 垃圾回收概述

#### 什么是垃圾回收
垃圾回收(Garbage Collection, GC)是JVM自动管理内存的机制，负责回收不再使用的对象所占用的内存空间。

#### GC的优点
- 自动内存管理，减少内存泄漏
- 简化编程，无需手动释放内存
- 提高程序安全性

#### GC的缺点
- 性能开销，GC时会暂停程序执行
- 不可预测的暂停时间
- 内存使用效率相对较低

### 对象存活判断

#### 1. 引用计数法
```java
// 引用计数法的问题：循环引用
public class ReferenceCountingGC {
    public Object instance = null;
    
    public static void main(String[] args) {
        ReferenceCountingGC objA = new ReferenceCountingGC();
        ReferenceCountingGC objB = new ReferenceCountingGC();
        
        objA.instance = objB;
        objB.instance = objA; // 循环引用
        
        objA = null;
        objB = null;
        // 两个对象无法被回收，因为它们相互引用
    }
}
```

#### 2. 可达性分析算法
```
GC Roots
    │
    ├── 对象A ──→ 对象B ──→ 对象C
    │
    └── 对象D ──→ 对象E
    
对象F ←──→ 对象G  (不可达，可以回收)
```

#### GC Roots包括
- 虚拟机栈中引用的对象
- 方法区中静态属性引用的对象
- 方法区中常量引用的对象
- 本地方法栈中JNI引用的对象
- JVM内部引用
- 同步锁持有的对象

### 引用类型

#### 1. 强引用 (Strong Reference)
```java
// 强引用示例
Object obj = new Object(); // obj是强引用
// 只要强引用存在，对象就不会被回收
```

#### 2. 软引用 (Soft Reference)
```java
// 软引用示例
SoftReference<Object> softRef = new SoftReference<>(new Object());
// 内存不足时会被回收
Object obj = softRef.get(); // 可能返回null
```

#### 3. 弱引用 (Weak Reference)
```java
// 弱引用示例
WeakReference<Object> weakRef = new WeakReference<>(new Object());
// 下次GC时就会被回收
Object obj = weakRef.get(); // 可能返回null
```

#### 4. 虚引用 (Phantom Reference)
```java
// 虚引用示例
ReferenceQueue<Object> queue = new ReferenceQueue<>();
PhantomReference<Object> phantomRef = new PhantomReference<>(new Object(), queue);
// 无法通过虚引用获取对象，主要用于跟踪对象回收
```

### 垃圾回收算法

#### 1. 标记-清除算法 (Mark-Sweep)
```
标记阶段：
[A][B][C][D][E][F][G][H]
 ✓  ✗  ✓  ✗  ✓  ✗  ✓  ✗

清除阶段：
[A][ ][C][ ][E][ ][G][ ]
```

**优点**: 实现简单
**缺点**: 产生内存碎片，效率不高

#### 2. 复制算法 (Copying)
```
回收前：
From空间: [A][B][C][D][E][F][G][H]
          ✓  ✗  ✓  ✗  ✓  ✗  ✓  ✗
To空间:   [ ][ ][ ][ ][ ][ ][ ][ ]

回收后：
From空间: [ ][ ][ ][ ][ ][ ][ ][ ]
To空间:   [A][C][E][G][ ][ ][ ][ ]
```

**优点**: 无内存碎片，效率高
**缺点**: 内存使用率只有50%

#### 3. 标记-整理算法 (Mark-Compact)
```
标记阶段：
[A][B][C][D][E][F][G][H]
 ✓  ✗  ✓  ✗  ✓  ✗  ✓  ✗

整理阶段：
[A][C][E][G][ ][ ][ ][ ]
```

**优点**: 无内存碎片，内存利用率高
**缺点**: 整理过程开销大

### 分代收集算法

#### 新生代GC (Minor GC)
- 使用复制算法
- Eden区满时触发
- 存活对象复制到Survivor区
- 频繁但快速

#### 老年代GC (Major GC)
- 使用标记-清除或标记-整理算法
- 老年代空间不足时触发
- 耗时较长

#### 完整GC (Full GC)
- 清理整个堆空间
- 包括新生代、老年代、方法区
- 耗时最长，应尽量避免

### 常见垃圾回收器

#### 1. Serial收集器
- 单线程收集器
- 适用于客户端应用
- 简单高效

#### 2. Parallel收集器
- 多线程收集器
- 适用于服务器应用
- 注重吞吐量

#### 3. CMS收集器
- 并发标记清除
- 低延迟
- 适用于对响应时间敏感的应用

#### 4. G1收集器
- 面向服务端应用
- 低延迟、高吞吐量
- 可预测的停顿时间

#### 5. ZGC收集器 (Java 11+)
- 超低延迟收集器
- 停顿时间不超过10ms
- 适用于大内存应用

## JVM调优

### 常用JVM参数

#### 堆内存设置
```bash
# 设置堆的初始大小和最大大小
-Xms2g -Xmx4g

# 设置新生代大小
-Xmn1g

# 设置新生代与老年代的比例
-XX:NewRatio=2

# 设置Eden与Survivor的比例
-XX:SurvivorRatio=8
```

#### 垃圾回收器选择
```bash
# 使用G1收集器
-XX:+UseG1GC

# 使用CMS收集器
-XX:+UseConcMarkSweepGC

# 使用Parallel收集器
-XX:+UseParallelGC
```

#### GC调优参数
```bash
# 设置G1的目标停顿时间
-XX:MaxGCPauseMillis=200

# 设置并行GC线程数
-XX:ParallelGCThreads=4

# 启用GC日志
-XX:+PrintGC -XX:+PrintGCDetails -XX:+PrintGCTimeStamps
```

### 性能调优步骤

#### 1. 性能监控
```java
// 使用JVM监控工具
// jstat -gc pid 1000  // 每秒输出GC信息
// jmap -histo pid     // 查看对象分布
// jstack pid          // 查看线程堆栈
```

#### 2. 问题分析
- 分析GC日志
- 查找性能瓶颈
- 确定调优目标

#### 3. 参数调整
- 调整堆大小
- 选择合适的垃圾回收器
- 优化GC参数

#### 4. 效果验证
- 压力测试
- 性能对比
- 持续监控

### 常见性能问题

#### 1. 内存泄漏
```java
// 内存泄漏示例
public class MemoryLeak {
    private static List<Object> list = new ArrayList<>();
    
    public void addObject() {
        list.add(new Object()); // 对象一直被引用，无法回收
    }
}
```

#### 2. 频繁Full GC
- 老年代空间不足
- 方法区空间不足
- 大对象直接进入老年代

#### 3. GC停顿时间过长
- 堆内存过大
- 垃圾回收器选择不当
- 应用程序问题

## 常用JVM工具

### 命令行工具

#### jps - 查看Java进程
```bash
# 查看所有Java进程
jps -l

# 查看进程和主类
jps -lv
```

#### jstat - 性能监控
```bash
# 查看GC信息
jstat -gc pid 1000

# 查看堆内存使用情况
jstat -gccapacity pid

# 查看新生代GC信息
jstat -gcnew pid
```

#### jmap - 内存分析
```bash
# 生成堆转储文件
jmap -dump:format=b,file=heap.hprof pid

# 查看对象分布
jmap -histo pid

# 查看堆内存使用情况
jmap -heap pid
```

#### jstack - 线程分析
```bash
# 查看线程堆栈
jstack pid

# 检测死锁
jstack -l pid
```

### 图形化工具

#### JConsole
- JDK自带的监控工具
- 可以监控内存、线程、类加载等信息
- 支持远程监控

#### JVisualVM
- 功能强大的性能分析工具
- 支持堆转储分析
- 支持CPU性能分析
- 插件扩展功能

#### MAT (Memory Analyzer Tool)
- Eclipse出品的内存分析工具
- 专门用于分析堆转储文件
- 可以找出内存泄漏问题

## 面试要点

### 高频问题

1. **JVM内存结构？**
   - 程序计数器、虚拟机栈、本地方法栈、堆、方法区
   - 线程私有 vs 线程共享
   - 各区域的作用和特点

2. **垃圾回收算法？**
   - 标记-清除、复制、标记-整理
   - 分代收集算法
   - 各算法的优缺点

3. **垃圾回收器有哪些？**
   - Serial、Parallel、CMS、G1、ZGC
   - 各收集器的特点和适用场景

4. **类加载过程？**
   - 加载、验证、准备、解析、初始化
   - 双亲委派模型
   - 类加载器层次

5. **如何判断对象可以被回收？**
   - 引用计数法 vs 可达性分析
   - GC Roots的概念
   - 引用类型

### 深入问题

1. **JVM调优经验？**
   - 常用JVM参数
   - 性能问题分析方法
   - 调优案例

2. **内存泄漏如何排查？**
   - 使用工具分析堆转储
   - 常见内存泄漏场景
   - 预防措施

3. **Full GC频繁的原因？**
   - 老年代空间不足
   - 方法区空间不足
   - 大对象分配

4. **G1收集器的原理？**
   - 分区概念
   - 并发标记过程
   - 混合收集

### 实践经验
- 了解生产环境JVM配置
- 掌握性能监控和分析方法
- 熟悉常见性能问题的解决方案
- 理解不同应用场景下的JVM调优策略

## 总结

JVM是Java技术栈的核心，深入理解JVM原理对于Java开发者至关重要：

1. **内存管理**: 理解内存结构有助于编写高效代码
2. **性能优化**: 掌握GC原理可以进行有效的性能调优
3. **问题排查**: 熟悉JVM工具能够快速定位和解决问题
4. **架构设计**: JVM知识有助于做出更好的技术决策

建议通过实际项目和性能调优实践来加深对JVM的理解，这样才能在面试和工作中游刃有余。