# Java集合框架

## 概述

Java集合框架提供了一套性能优良、使用方便的接口和类，用于存储和操作对象集合。

## 集合框架结构

```
Collection (接口)
├── List (接口) - 有序、可重复
│   ├── ArrayList - 动态数组
│   ├── LinkedList - 双向链表
│   └── Vector - 线程安全的动态数组
├── Set (接口) - 无序、不可重复
│   ├── HashSet - 基于HashMap
│   ├── LinkedHashSet - 保持插入顺序
│   └── TreeSet - 有序集合
└── Queue (接口) - 队列
    ├── PriorityQueue - 优先队列
    └── Deque (接口) - 双端队列
        └── ArrayDeque - 基于数组的双端队列

Map (接口) - 键值对映射
├── HashMap - 基于哈希表
├── LinkedHashMap - 保持插入顺序
├── TreeMap - 有序映射
└── ConcurrentHashMap - 线程安全的HashMap
```

## List接口

### ArrayList

**特点：**
- 基于动态数组实现
- 随机访问速度快 O(1)
- 插入删除慢 O(n)
- 线程不安全

**源码分析：**
```java
public class ArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    
    private static final int DEFAULT_CAPACITY = 10;
    private Object[] elementData;
    private int size;
    
    // 扩容机制
    private void grow(int minCapacity) {
        int oldCapacity = elementData.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1); // 1.5倍扩容
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        elementData = Arrays.copyOf(elementData, newCapacity);
    }
}
```

**使用示例：**
```java
List<String> list = new ArrayList<>();
list.add("Apple");
list.add("Banana");
list.add(1, "Orange"); // 指定位置插入

// 遍历方式
// 1. 增强for循环
for (String fruit : list) {
    System.out.println(fruit);
}

// 2. Iterator
Iterator<String> iterator = list.iterator();
while (iterator.hasNext()) {
    System.out.println(iterator.next());
}

// 3. Stream API
list.stream().forEach(System.out::println);
```

### LinkedList

**特点：**
- 基于双向链表实现
- 插入删除快 O(1)
- 随机访问慢 O(n)
- 实现了Deque接口

**源码分析：**
```java
public class LinkedList<E> extends AbstractSequentialList<E>
        implements List<E>, Deque<E>, Cloneable, java.io.Serializable {
    
    transient Node<E> first;
    transient Node<E> last;
    
    private static class Node<E> {
        E item;
        Node<E> next;
        Node<E> prev;
        
        Node(Node<E> prev, E element, Node<E> next) {
            this.item = element;
            this.next = next;
            this.prev = prev;
        }
    }
}
```

**使用示例：**
```java
LinkedList<String> linkedList = new LinkedList<>();
linkedList.addFirst("First");
linkedList.addLast("Last");
linkedList.offer("Queue Element"); // 作为队列使用
linkedList.push("Stack Element");  // 作为栈使用
```

## Set接口

### HashSet

**特点：**
- 基于HashMap实现
- 无序、不重复
- 允许null值
- 线程不安全

**实现原理：**
```java
public class HashSet<E> extends AbstractSet<E>
        implements Set<E>, Cloneable, java.io.Serializable {
    
    private transient HashMap<E,Object> map;
    private static final Object PRESENT = new Object();
    
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }
}
```

**使用示例：**
```java
Set<String> set = new HashSet<>();
set.add("Apple");
set.add("Banana");
set.add("Apple"); // 重复元素，不会被添加

System.out.println(set.size()); // 输出: 2
```

### TreeSet

**特点：**
- 基于红黑树实现
- 有序集合
- 不允许null值
- 线程不安全

**使用示例：**
```java
TreeSet<Integer> treeSet = new TreeSet<>();
treeSet.add(3);
treeSet.add(1);
treeSet.add(2);

System.out.println(treeSet); // 输出: [1, 2, 3]

// 自定义排序
TreeSet<String> customSet = new TreeSet<>((a, b) -> b.compareTo(a));
customSet.add("Apple");
customSet.add("Banana");
System.out.println(customSet); // 输出: [Banana, Apple]
```

## Map接口

### HashMap

**特点：**
- 基于哈希表实现
- 允许null键和null值
- 无序
- 线程不安全

**底层实现（JDK 1.8+）：**
- 数组 + 链表 + 红黑树
- 当链表长度超过8时转换为红黑树
- 当红黑树节点少于6时转换回链表

**源码分析：**
```java
public class HashMap<K,V> extends AbstractMap<K,V>
        implements Map<K,V>, Cloneable, Serializable {
    
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // 16
    static final int MAXIMUM_CAPACITY = 1 << 30;
    static final float DEFAULT_LOAD_FACTOR = 0.75f;
    static final int TREEIFY_THRESHOLD = 8;
    
    transient Node<K,V>[] table;
    transient int size;
    int threshold;
    final float loadFactor;
    
    // 哈希函数
    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }
}
```

**使用示例：**
```java
Map<String, Integer> map = new HashMap<>();
map.put("Apple", 10);
map.put("Banana", 20);
map.put("Orange", 15);

// 遍历方式
// 1. entrySet
for (Map.Entry<String, Integer> entry : map.entrySet()) {
    System.out.println(entry.getKey() + ": " + entry.getValue());
}

// 2. keySet
for (String key : map.keySet()) {
    System.out.println(key + ": " + map.get(key));
}

// 3. Stream API
map.forEach((key, value) -> System.out.println(key + ": " + value));
```

### ConcurrentHashMap

**特点：**
- 线程安全的HashMap
- 分段锁机制（JDK 1.7）
- CAS + synchronized（JDK 1.8+）

**JDK 1.8实现原理：**
```java
public class ConcurrentHashMap<K,V> extends AbstractMap<K,V>
        implements ConcurrentMap<K,V>, Serializable {
    
    // 使用CAS操作保证线程安全
    final V putVal(K key, V value, boolean onlyIfAbsent) {
        if (key == null || value == null) throw new NullPointerException();
        int hash = spread(key.hashCode());
        int binCount = 0;
        for (Node<K,V>[] tab = table;;) {
            // CAS操作和synchronized结合使用
        }
    }
}
```

## 集合工具类

### Collections类

```java
List<Integer> list = Arrays.asList(3, 1, 4, 1, 5);

// 排序
Collections.sort(list);
Collections.sort(list, Collections.reverseOrder());

// 查找
int index = Collections.binarySearch(list, 3);

// 最值
Integer max = Collections.max(list);
Integer min = Collections.min(list);

// 不可变集合
List<String> immutableList = Collections.unmodifiableList(list);

// 同步集合
List<String> syncList = Collections.synchronizedList(new ArrayList<>());
Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());

// 空集合
List<String> emptyList = Collections.emptyList();
Set<String> emptySet = Collections.emptySet();
Map<String, String> emptyMap = Collections.emptyMap();
```

### Arrays类

```java
int[] array = {3, 1, 4, 1, 5};

// 排序
Arrays.sort(array);

// 查找
int index = Arrays.binarySearch(array, 3);

// 转换为List
List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

// 填充
int[] newArray = new int[5];
Arrays.fill(newArray, 10);

// 比较
boolean equal = Arrays.equals(array1, array2);

// 转换为字符串
String str = Arrays.toString(array);
```

## 性能对比

| 操作 | ArrayList | LinkedList | HashMap | TreeMap |
|------|-----------|------------|---------|----------|
| 插入 | O(n) | O(1) | O(1) | O(log n) |
| 删除 | O(n) | O(1) | O(1) | O(log n) |
| 查找 | O(1) | O(n) | O(1) | O(log n) |
| 遍历 | O(n) | O(n) | O(n) | O(n) |

## 选择指南

### List选择
- **ArrayList**：频繁随机访问，较少插入删除
- **LinkedList**：频繁插入删除，较少随机访问
- **Vector**：需要线程安全的List

### Set选择
- **HashSet**：不需要排序，性能要求高
- **LinkedHashSet**：需要保持插入顺序
- **TreeSet**：需要排序

### Map选择
- **HashMap**：一般场景，性能要求高
- **LinkedHashMap**：需要保持插入顺序
- **TreeMap**：需要排序
- **ConcurrentHashMap**：多线程环境

## 常见面试题

1. **ArrayList和LinkedList的区别？**
2. **HashMap的实现原理？**
3. **ConcurrentHashMap如何保证线程安全？**
4. **HashMap和HashTable的区别？**
5. **为什么HashMap的容量总是2的幂次方？**
6. **HashMap在JDK 1.8中的优化？**
7. **如何解决HashMap的线程安全问题？**

## 最佳实践

1. **优先使用接口类型声明变量**
2. **根据使用场景选择合适的集合类型**
3. **注意集合的线程安全性**
4. **合理设置HashMap的初始容量**
5. **重写equals()和hashCode()方法**
6. **使用泛型避免类型转换**
7. **及时清理不用的集合引用**