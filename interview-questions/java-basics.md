# Java基础面试题

## 面向对象编程

### 1. 面向对象的四大特性是什么？请详细解释。

**答案：**

**封装（Encapsulation）：**
- 将数据和操作数据的方法绑定在一起
- 隐藏内部实现细节，只暴露必要的接口
- 通过访问修饰符控制访问权限
- 提供getter/setter方法控制属性访问

**继承（Inheritance）：**
- 子类可以继承父类的属性和方法
- 实现代码复用，建立类之间的层次关系
- 使用extends关键字实现继承
- 支持方法重写（Override）

**多态（Polymorphism）：**
- 同一个接口可以有多种不同的实现
- 编译时多态：方法重载
- 运行时多态：方法重写，通过动态绑定实现
- 父类引用指向子类对象

**抽象（Abstraction）：**
- 隐藏复杂的实现细节，只暴露必要的功能
- 通过抽象类和接口实现
- 定义规范，强制子类实现特定方法

### 2. 抽象类和接口的区别？

**答案：**

| 特性 | 抽象类 | 接口 |
|------|--------|------|
| 关键字 | abstract class | interface |
| 继承 | 单继承（extends） | 多实现（implements） |
| 构造器 | 可以有构造器 | 不能有构造器 |
| 成员变量 | 可以有实例变量 | 只能有public static final常量 |
| 方法 | 可以有抽象和具体方法 | 默认抽象方法，Java 8+支持default和static方法 |
| 访问修饰符 | 可以有各种访问修饰符 | 方法默认public abstract |
| 实例化 | 不能直接实例化 | 不能直接实例化 |

**使用场景：**
- 抽象类：有共同代码需要复用，is-a关系
- 接口：定义规范，can-do关系，多重继承

### 3. 重载（Overload）和重写（Override）的区别？

**答案：**

**重载（Overload）：**
- 同一个类中方法名相同，参数列表不同
- 编译时多态
- 参数个数、类型、顺序不同
- 返回值类型可以不同
- 访问修饰符可以不同

```java
public class Calculator {
    public int add(int a, int b) { return a + b; }
    public double add(double a, double b) { return a + b; }
    public int add(int a, int b, int c) { return a + b + c; }
}
```

**重写（Override）：**
- 子类重新实现父类的方法
- 运行时多态
- 方法名、参数列表、返回值类型必须相同
- 访问修饰符不能更严格
- 不能重写private、static、final方法

```java
class Animal {
    public void makeSound() { System.out.println("Animal sound"); }
}

class Dog extends Animal {
    @Override
    public void makeSound() { System.out.println("Woof!"); }
}
```

### 4. 内部类有哪几种？各有什么特点？

**答案：**

**1. 成员内部类：**
```java
class Outer {
    private String name = "Outer";
    
    class Inner {
        public void display() {
            System.out.println(name); // 可以访问外部类私有成员
        }
    }
}
```
- 可以访问外部类的所有成员
- 不能定义静态成员
- 需要外部类实例才能创建

**2. 静态内部类：**
```java
class Outer {
    static class StaticInner {
        public void display() {
            System.out.println("Static inner");
        }
    }
}
```
- 不能访问外部类的非静态成员
- 可以定义静态成员
- 不需要外部类实例就能创建

**3. 局部内部类：**
```java
class Outer {
    public void method() {
        class LocalInner {
            public void display() {
                System.out.println("Local inner");
            }
        }
    }
}
```
- 定义在方法内部
- 只能在定义的方法内使用
- 可以访问final或effectively final的局部变量

**4. 匿名内部类：**
```java
Runnable r = new Runnable() {
    @Override
    public void run() {
        System.out.println("Anonymous inner class");
    }
};
```
- 没有类名
- 通常用于实现接口或继承类
- 只能使用一次

## 集合框架

### 5. ArrayList和LinkedList的区别？

**答案：**

| 特性 | ArrayList | LinkedList |
|------|-----------|------------|
| 底层实现 | 动态数组 | 双向链表 |
| 随机访问 | O(1) | O(n) |
| 插入删除（头部） | O(n) | O(1) |
| 插入删除（尾部） | O(1) | O(1) |
| 插入删除（中间） | O(n) | O(1) |
| 内存占用 | 较少 | 较多（存储指针） |
| 缓存友好性 | 好 | 差 |

**使用场景：**
- ArrayList：频繁随机访问，较少插入删除
- LinkedList：频繁插入删除，较少随机访问

### 6. HashMap的实现原理？

**答案：**

**JDK 1.8之前：**
- 数组 + 链表结构
- 哈希冲突时使用链表存储

**JDK 1.8及之后：**
- 数组 + 链表 + 红黑树
- 当链表长度超过8时转换为红黑树
- 当红黑树节点少于6时转换回链表

**核心参数：**
- 初始容量：16
- 负载因子：0.75
- 扩容阈值：容量 × 负载因子

**哈希计算：**
```java
static final int hash(Object key) {
    int h;
    return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
}
```

**扩容机制：**
- 当元素个数超过阈值时扩容
- 扩容为原来的2倍
- 重新计算所有元素的位置

### 7. ConcurrentHashMap如何保证线程安全？

**答案：**

**JDK 1.7：分段锁**
- 将数据分成多个段（Segment）
- 每个段有独立的锁
- 不同段可以并发访问

**JDK 1.8：CAS + synchronized**
- 取消分段锁设计
- 使用CAS操作进行无锁更新
- 在链表头节点或红黑树根节点上使用synchronized
- 只锁定当前操作的节点，提高并发性

**核心机制：**
```java
// 使用CAS操作
if (casTabAt(tab, i, null, new Node<K,V>(hash, key, value, null)))
    break;

// 在冲突时使用synchronized
synchronized (f) {
    // 链表或红黑树操作
}
```

### 8. 为什么HashMap的容量总是2的幂次方？

**答案：**

**原因：**
1. **位运算优化：** `hash & (length - 1)` 等价于 `hash % length`，但位运算更快
2. **均匀分布：** 保证哈希值能够均匀分布在数组中
3. **扩容优化：** 扩容时元素要么在原位置，要么在原位置+旧容量的位置

**示例：**
```java
// 假设容量为16（10000），length-1为15（01111）
hash1 = 5  (00101) & 15 (01111) = 5
hash2 = 21 (10101) & 15 (01111) = 5  // 冲突

// 扩容到32（100000），length-1为31（011111）
hash1 = 5  (000101) & 31 (011111) = 5   // 原位置
hash2 = 21 (010101) & 31 (011111) = 21  // 原位置+16
```

## 异常处理

### 9. 检查异常和非检查异常的区别？

**答案：**

**检查异常（Checked Exception）：**
- 编译时必须处理的异常
- 继承自Exception但不继承自RuntimeException
- 必须用try-catch捕获或throws声明
- 例：IOException, SQLException, ClassNotFoundException

**非检查异常（Unchecked Exception）：**
- 运行时异常，编译时不强制处理
- 继承自RuntimeException
- 通常是程序逻辑错误导致
- 例：NullPointerException, ArrayIndexOutOfBoundsException

**Error：**
- 系统级错误，程序无法处理
- 例：OutOfMemoryError, StackOverflowError

### 10. try-catch-finally的执行顺序？

**答案：**

**正常情况：**
```java
try {
    // 1. 执行try块
    return "try";
} catch (Exception e) {
    // 2. 如果有异常，执行catch块
    return "catch";
} finally {
    // 3. 无论如何都执行finally块
    return "finally"; // 会覆盖前面的返回值
}
```

**特殊情况：**
1. finally中的return会覆盖try/catch中的return
2. finally中的异常会覆盖try/catch中的异常
3. System.exit()会阻止finally执行
4. 如果try/catch中有return，finally仍会执行，但不能改变返回的对象的引用

## JVM相关

### 11. Java内存模型（JMM）是什么？

**答案：**

Java内存模型定义了Java程序中变量的访问规则，以及在JVM中将变量存储到内存和从内存中读取变量的底层细节。

**核心概念：**
1. **主内存：** 所有变量都存储在主内存中
2. **工作内存：** 每个线程有自己的工作内存，存储主内存变量的副本
3. **内存间交互：** 通过8种原子操作实现

**三大特性：**
1. **原子性：** 基本数据类型的读写是原子的
2. **可见性：** 一个线程修改的变量对其他线程可见
3. **有序性：** 程序执行的顺序按照代码的先后顺序

**happens-before规则：**
- 程序顺序规则
- 监视器锁规则
- volatile变量规则
- 传递性规则
- 线程启动规则
- 线程终止规则

### 12. 垃圾回收机制？

**答案：**

**垃圾回收算法：**
1. **标记-清除：** 标记垃圾对象，然后清除
2. **复制算法：** 将存活对象复制到另一块内存
3. **标记-整理：** 标记垃圾对象，整理内存碎片
4. **分代收集：** 根据对象年龄采用不同算法

**内存分代：**
- **新生代：** Eden区 + 2个Survivor区
- **老年代：** 长期存活的对象
- **永久代/元空间：** 类信息、常量池

**垃圾收集器：**
- Serial GC：单线程
- Parallel GC：多线程
- CMS GC：并发标记清除
- G1 GC：低延迟
- ZGC：超低延迟

## 其他重要概念

### 13. equals()和hashCode()的关系？

**答案：**

**约定：**
1. 如果两个对象equals()返回true，那么hashCode()必须相等
2. 如果两个对象hashCode()相等，equals()不一定返回true
3. 重写equals()必须重写hashCode()

**原因：**
- HashMap等集合依赖hashCode()进行快速查找
- 如果不遵守约定，会导致集合行为异常

**实现示例：**
```java
@Override
public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    Person person = (Person) obj;
    return age == person.age && Objects.equals(name, person.name);
}

@Override
public int hashCode() {
    return Objects.hash(name, age);
}
```

### 14. String、StringBuilder、StringBuffer的区别？

**答案：**

| 特性 | String | StringBuilder | StringBuffer |
|------|--------|---------------|-------------|
| 可变性 | 不可变 | 可变 | 可变 |
| 线程安全 | 安全 | 不安全 | 安全 |
| 性能 | 差（频繁创建对象） | 好 | 中等（同步开销） |
| 使用场景 | 少量字符串操作 | 单线程大量操作 | 多线程大量操作 |

**String不可变的好处：**
1. 线程安全
2. 可以缓存hashCode
3. 字符串常量池优化
4. 安全性（作为参数时不会被修改）

### 15. 反射的原理和应用？

**答案：**

**原理：**
- 在运行时获取类的信息
- 动态创建对象和调用方法
- 通过Class对象访问类的元数据

**主要API：**
```java
// 获取Class对象
Class<?> clazz = Class.forName("com.example.Person");
Class<?> clazz = Person.class;
Class<?> clazz = person.getClass();

// 创建实例
Object obj = clazz.newInstance();
Constructor<?> constructor = clazz.getConstructor(String.class);
Object obj = constructor.newInstance("John");

// 调用方法
Method method = clazz.getMethod("getName");
Object result = method.invoke(obj);

// 访问字段
Field field = clazz.getDeclaredField("name");
field.setAccessible(true);
field.set(obj, "Jane");
```

**应用场景：**
1. 框架开发（Spring IOC、ORM）
2. 动态代理
3. 注解处理
4. 序列化/反序列化
5. 单元测试

**缺点：**
1. 性能开销
2. 破坏封装性
3. 编译时无法检查
4. 安全性问题