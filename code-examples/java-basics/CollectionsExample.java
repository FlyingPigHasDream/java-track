package com.javatrack.basics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Java集合框架示例
 * 演示List、Set、Map等集合的使用和特性
 */
public class CollectionsExample {
    
    public static void main(String[] args) {
        System.out.println("=== Java集合框架示例 ===");
        
        listExamples();
        setExamples();
        mapExamples();
        queueExamples();
        collectionsUtilityExamples();
        performanceComparison();
        concurrentCollectionsExample();
        streamApiExample();
    }
    
    /**
     * List接口示例
     */
    private static void listExamples() {
        System.out.println("\n1. List接口示例:");
        
        // ArrayList示例
        System.out.println("ArrayList示例:");
        List<String> arrayList = new ArrayList<>();
        arrayList.add("Apple");
        arrayList.add("Banana");
        arrayList.add("Cherry");
        arrayList.add(1, "Orange"); // 指定位置插入
        
        System.out.println("ArrayList: " + arrayList);
        System.out.println("Get index 2: " + arrayList.get(2));
        System.out.println("Index of Banana: " + arrayList.indexOf("Banana"));
        
        // LinkedList示例
        System.out.println("\nLinkedList示例:");
        LinkedList<String> linkedList = new LinkedList<>();
        linkedList.addFirst("First");
        linkedList.addLast("Last");
        linkedList.add("Middle");
        
        System.out.println("LinkedList: " + linkedList);
        System.out.println("First element: " + linkedList.getFirst());
        System.out.println("Last element: " + linkedList.getLast());
        
        // 作为栈使用
        linkedList.push("Stack Top");
        System.out.println("After push: " + linkedList);
        String popped = linkedList.pop();
        System.out.println("Popped: " + popped + ", List: " + linkedList);
        
        // 作为队列使用
        linkedList.offer("Queue Element");
        System.out.println("After offer: " + linkedList);
        String polled = linkedList.poll();
        System.out.println("Polled: " + polled + ", List: " + linkedList);
        
        // Vector示例（线程安全）
        System.out.println("\nVector示例:");
        Vector<Integer> vector = new Vector<>();
        vector.add(1);
        vector.add(2);
        vector.add(3);
        System.out.println("Vector: " + vector);
        System.out.println("Capacity: " + vector.capacity());
    }
    
    /**
     * Set接口示例
     */
    private static void setExamples() {
        System.out.println("\n2. Set接口示例:");
        
        // HashSet示例
        System.out.println("HashSet示例:");
        Set<String> hashSet = new HashSet<>();
        hashSet.add("Apple");
        hashSet.add("Banana");
        hashSet.add("Cherry");
        hashSet.add("Apple"); // 重复元素，不会被添加
        
        System.out.println("HashSet: " + hashSet);
        System.out.println("Size: " + hashSet.size());
        System.out.println("Contains Apple: " + hashSet.contains("Apple"));
        
        // LinkedHashSet示例（保持插入顺序）
        System.out.println("\nLinkedHashSet示例:");
        Set<String> linkedHashSet = new LinkedHashSet<>();
        linkedHashSet.add("Third");
        linkedHashSet.add("First");
        linkedHashSet.add("Second");
        linkedHashSet.add("Third"); // 重复元素
        
        System.out.println("LinkedHashSet: " + linkedHashSet);
        
        // TreeSet示例（自动排序）
        System.out.println("\nTreeSet示例:");
        TreeSet<Integer> treeSet = new TreeSet<>();
        treeSet.add(5);
        treeSet.add(2);
        treeSet.add(8);
        treeSet.add(1);
        treeSet.add(9);
        
        System.out.println("TreeSet: " + treeSet);
        System.out.println("First: " + treeSet.first());
        System.out.println("Last: " + treeSet.last());
        System.out.println("Higher than 5: " + treeSet.higher(5));
        System.out.println("Lower than 5: " + treeSet.lower(5));
        
        // 自定义排序的TreeSet
        TreeSet<String> customTreeSet = new TreeSet<>((a, b) -> b.compareTo(a)); // 降序
        customTreeSet.add("Apple");
        customTreeSet.add("Banana");
        customTreeSet.add("Cherry");
        System.out.println("Custom sorted TreeSet: " + customTreeSet);
    }
    
    /**
     * Map接口示例
     */
    private static void mapExamples() {
        System.out.println("\n3. Map接口示例:");
        
        // HashMap示例
        System.out.println("HashMap示例:");
        Map<String, Integer> hashMap = new HashMap<>();
        hashMap.put("Apple", 10);
        hashMap.put("Banana", 20);
        hashMap.put("Cherry", 15);
        hashMap.put("Apple", 12); // 覆盖原值
        
        System.out.println("HashMap: " + hashMap);
        System.out.println("Get Apple: " + hashMap.get("Apple"));
        System.out.println("Get or default: " + hashMap.getOrDefault("Orange", 0));
        
        // 遍历Map的几种方式
        System.out.println("\n遍历HashMap:");
        
        // 方式1: entrySet
        System.out.println("使用entrySet:");
        for (Map.Entry<String, Integer> entry : hashMap.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue());
        }
        
        // 方式2: keySet
        System.out.println("使用keySet:");
        for (String key : hashMap.keySet()) {
            System.out.println("  " + key + ": " + hashMap.get(key));
        }
        
        // 方式3: forEach (Java 8+)
        System.out.println("使用forEach:");
        hashMap.forEach((key, value) -> System.out.println("  " + key + ": " + value));
        
        // LinkedHashMap示例（保持插入顺序）
        System.out.println("\nLinkedHashMap示例:");
        Map<String, Integer> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put("Third", 3);
        linkedHashMap.put("First", 1);
        linkedHashMap.put("Second", 2);
        System.out.println("LinkedHashMap: " + linkedHashMap);
        
        // TreeMap示例（自动排序）
        System.out.println("\nTreeMap示例:");
        TreeMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("Zebra", 26);
        treeMap.put("Apple", 1);
        treeMap.put("Monkey", 13);
        
        System.out.println("TreeMap: " + treeMap);
        System.out.println("First key: " + treeMap.firstKey());
        System.out.println("Last key: " + treeMap.lastKey());
        System.out.println("Higher key than 'M': " + treeMap.higherKey("M"));
        
        // Map的高级操作 (Java 8+)
        System.out.println("\nMap高级操作:");
        Map<String, Integer> advancedMap = new HashMap<>();
        advancedMap.put("A", 1);
        advancedMap.put("B", 2);
        
        // putIfAbsent
        advancedMap.putIfAbsent("C", 3);
        advancedMap.putIfAbsent("A", 10); // 不会覆盖
        System.out.println("After putIfAbsent: " + advancedMap);
        
        // compute
        advancedMap.compute("A", (key, value) -> value * 2);
        System.out.println("After compute: " + advancedMap);
        
        // merge
        advancedMap.merge("D", 4, Integer::sum);
        advancedMap.merge("A", 5, Integer::sum);
        System.out.println("After merge: " + advancedMap);
    }
    
    /**
     * Queue接口示例
     */
    private static void queueExamples() {
        System.out.println("\n4. Queue接口示例:");
        
        // PriorityQueue示例
        System.out.println("PriorityQueue示例:");
        PriorityQueue<Integer> priorityQueue = new PriorityQueue<>();
        priorityQueue.offer(5);
        priorityQueue.offer(2);
        priorityQueue.offer(8);
        priorityQueue.offer(1);
        
        System.out.println("PriorityQueue: " + priorityQueue);
        while (!priorityQueue.isEmpty()) {
            System.out.println("Poll: " + priorityQueue.poll());
        }
        
        // 自定义比较器的PriorityQueue
        PriorityQueue<String> customPriorityQueue = new PriorityQueue<>(
            (a, b) -> Integer.compare(b.length(), a.length()) // 按长度降序
        );
        customPriorityQueue.offer("Apple");
        customPriorityQueue.offer("Banana");
        customPriorityQueue.offer("Cherry");
        customPriorityQueue.offer("Date");
        
        System.out.println("\nCustom PriorityQueue (by length desc):");
        while (!customPriorityQueue.isEmpty()) {
            System.out.println("Poll: " + customPriorityQueue.poll());
        }
        
        // ArrayDeque示例（双端队列）
        System.out.println("\nArrayDeque示例:");
        Deque<String> deque = new ArrayDeque<>();
        deque.addFirst("First");
        deque.addLast("Last");
        deque.addFirst("New First");
        deque.addLast("New Last");
        
        System.out.println("Deque: " + deque);
        System.out.println("Remove first: " + deque.removeFirst());
        System.out.println("Remove last: " + deque.removeLast());
        System.out.println("Final deque: " + deque);
    }
    
    /**
     * Collections工具类示例
     */
    private static void collectionsUtilityExamples() {
        System.out.println("\n5. Collections工具类示例:");
        
        List<Integer> numbers = new ArrayList<>(Arrays.asList(3, 1, 4, 1, 5, 9, 2, 6));
        System.out.println("Original list: " + numbers);
        
        // 排序
        Collections.sort(numbers);
        System.out.println("After sort: " + numbers);
        
        // 反转
        Collections.reverse(numbers);
        System.out.println("After reverse: " + numbers);
        
        // 打乱
        Collections.shuffle(numbers);
        System.out.println("After shuffle: " + numbers);
        
        // 查找
        Collections.sort(numbers); // 二分查找需要有序
        int index = Collections.binarySearch(numbers, 5);
        System.out.println("Binary search for 5: index " + index);
        
        // 最值
        System.out.println("Max: " + Collections.max(numbers));
        System.out.println("Min: " + Collections.min(numbers));
        
        // 频率
        System.out.println("Frequency of 1: " + Collections.frequency(numbers, 1));
        
        // 不可变集合
        List<String> immutableList = Collections.unmodifiableList(
            Arrays.asList("A", "B", "C")
        );
        System.out.println("Immutable list: " + immutableList);
        
        // 同步集合
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());
        
        // 空集合
        List<String> emptyList = Collections.emptyList();
        Set<String> emptySet = Collections.emptySet();
        Map<String, String> emptyMap = Collections.emptyMap();
        
        System.out.println("Empty collections created");
    }
    
    /**
     * 性能比较示例
     */
    private static void performanceComparison() {
        System.out.println("\n6. 性能比较示例:");
        
        int size = 100000;
        
        // ArrayList vs LinkedList 插入性能
        System.out.println("插入性能比较 (" + size + " elements):");
        
        // ArrayList 尾部插入
        long start = System.currentTimeMillis();
        List<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            arrayList.add(i);
        }
        long arrayListTime = System.currentTimeMillis() - start;
        
        // LinkedList 尾部插入
        start = System.currentTimeMillis();
        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < size; i++) {
            linkedList.add(i);
        }
        long linkedListTime = System.currentTimeMillis() - start;
        
        System.out.println("ArrayList add time: " + arrayListTime + "ms");
        System.out.println("LinkedList add time: " + linkedListTime + "ms");
        
        // 随机访问性能
        System.out.println("\n随机访问性能比较:");
        Random random = new Random();
        
        // ArrayList 随机访问
        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            arrayList.get(random.nextInt(size));
        }
        long arrayListAccessTime = System.currentTimeMillis() - start;
        
        // LinkedList 随机访问
        start = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            linkedList.get(random.nextInt(size));
        }
        long linkedListAccessTime = System.currentTimeMillis() - start;
        
        System.out.println("ArrayList random access time: " + arrayListAccessTime + "ms");
        System.out.println("LinkedList random access time: " + linkedListAccessTime + "ms");
    }
    
    /**
     * 并发集合示例
     */
    private static void concurrentCollectionsExample() {
        System.out.println("\n7. 并发集合示例:");
        
        // ConcurrentHashMap
        Map<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        concurrentMap.put("A", 1);
        concurrentMap.put("B", 2);
        concurrentMap.put("C", 3);
        
        // 原子操作
        concurrentMap.compute("A", (key, value) -> value + 10);
        concurrentMap.merge("D", 4, Integer::sum);
        
        System.out.println("ConcurrentHashMap: " + concurrentMap);
        
        // 并发修改安全
        concurrentMap.forEach((key, value) -> {
            if (value > 5) {
                concurrentMap.put(key + "_modified", value * 2);
            }
        });
        
        System.out.println("After concurrent modification: " + concurrentMap);
    }
    
    /**
     * Stream API示例
     */
    private static void streamApiExample() {
        System.out.println("\n8. Stream API示例:");
        
        List<String> fruits = Arrays.asList(
            "Apple", "Banana", "Cherry", "Date", "Elderberry", "Fig", "Grape"
        );
        
        System.out.println("Original list: " + fruits);
        
        // 过滤和转换
        List<String> result = fruits.stream()
            .filter(fruit -> fruit.length() > 5)
            .map(String::toUpperCase)
            .sorted()
            .collect(Collectors.toList());
        
        System.out.println("Filtered and transformed: " + result);
        
        // 分组
        Map<Integer, List<String>> groupedByLength = fruits.stream()
            .collect(Collectors.groupingBy(String::length));
        
        System.out.println("Grouped by length: " + groupedByLength);
        
        // 统计
        long count = fruits.stream()
            .filter(fruit -> fruit.startsWith("A"))
            .count();
        
        System.out.println("Fruits starting with 'A': " + count);
        
        // 查找
        Optional<String> longest = fruits.stream()
            .max(Comparator.comparing(String::length));
        
        longest.ifPresent(fruit -> System.out.println("Longest fruit name: " + fruit));
        
        // 归约
        String concatenated = fruits.stream()
            .reduce("", (a, b) -> a + ", " + b);
        
        System.out.println("Concatenated: " + concatenated.substring(2)); // 去掉开头的", "
    }
}