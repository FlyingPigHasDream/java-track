# 面向对象编程 (OOP)

## 概述

面向对象编程是Java的核心特性，它基于四个基本原则：封装、继承、多态和抽象。

## 四大特性

### 1. 封装 (Encapsulation)

封装是将数据和操作数据的方法绑定在一起，并隐藏内部实现细节。

**核心概念：**
- 数据隐藏
- 访问控制修饰符
- getter/setter方法

**访问修饰符：**
- `private`：只能在当前类中访问
- `default`：包级别访问
- `protected`：包级别 + 子类访问
- `public`：公共访问

**示例：**
```java
public class Student {
    private String name;
    private int age;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        if (name != null && !name.trim().isEmpty()) {
            this.name = name;
        }
    }
    
    public int getAge() {
        return age;
    }
    
    public void setAge(int age) {
        if (age > 0 && age < 150) {
            this.age = age;
        }
    }
}
```

### 2. 继承 (Inheritance)

继承允许一个类获得另一个类的属性和方法。

**核心概念：**
- 父类（超类）和子类
- `extends` 关键字
- 方法重写 (Override)
- `super` 关键字

**示例：**
```java
// 父类
public class Animal {
    protected String name;
    
    public void eat() {
        System.out.println(name + " is eating");
    }
    
    public void sleep() {
        System.out.println(name + " is sleeping");
    }
}

// 子类
public class Dog extends Animal {
    public Dog(String name) {
        this.name = name;
    }
    
    @Override
    public void eat() {
        System.out.println(name + " is eating dog food");
    }
    
    public void bark() {
        System.out.println(name + " is barking");
    }
}
```

### 3. 多态 (Polymorphism)

多态允许同一个接口表示不同的底层数据类型。

**类型：**
- 编译时多态（方法重载）
- 运行时多态（方法重写）

**示例：**
```java
public class PolymorphismDemo {
    public static void main(String[] args) {
        Animal animal1 = new Dog("旺财");
        Animal animal2 = new Cat("咪咪");
        
        // 运行时多态
        animal1.eat(); // 输出：旺财 is eating dog food
        animal2.eat(); // 输出：咪咪 is eating cat food
        
        // 方法重载示例
        Calculator calc = new Calculator();
        calc.add(1, 2);        // int版本
        calc.add(1.0, 2.0);    // double版本
        calc.add(1, 2, 3);     // 三参数版本
    }
}

class Calculator {
    public int add(int a, int b) {
        return a + b;
    }
    
    public double add(double a, double b) {
        return a + b;
    }
    
    public int add(int a, int b, int c) {
        return a + b + c;
    }
}
```

### 4. 抽象 (Abstraction)

抽象是隐藏复杂实现细节，只暴露必要的接口。

**实现方式：**
- 抽象类 (`abstract class`)
- 接口 (`interface`)

**抽象类示例：**
```java
abstract class Shape {
    protected String color;
    
    public Shape(String color) {
        this.color = color;
    }
    
    // 抽象方法
    public abstract double getArea();
    public abstract double getPerimeter();
    
    // 具体方法
    public void displayColor() {
        System.out.println("Color: " + color);
    }
}

class Circle extends Shape {
    private double radius;
    
    public Circle(String color, double radius) {
        super(color);
        this.radius = radius;
    }
    
    @Override
    public double getArea() {
        return Math.PI * radius * radius;
    }
    
    @Override
    public double getPerimeter() {
        return 2 * Math.PI * radius;
    }
}
```

**接口示例：**
```java
interface Drawable {
    void draw();
    
    // Java 8+ 默认方法
    default void print() {
        System.out.println("Printing...");
    }
    
    // Java 8+ 静态方法
    static void info() {
        System.out.println("This is Drawable interface");
    }
}

interface Resizable {
    void resize(double factor);
}

// 实现多个接口
class Rectangle implements Drawable, Resizable {
    private double width, height;
    
    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }
    
    @Override
    public void draw() {
        System.out.println("Drawing rectangle: " + width + "x" + height);
    }
    
    @Override
    public void resize(double factor) {
        width *= factor;
        height *= factor;
    }
}
```

## 重要概念

### 构造器

```java
public class Person {
    private String name;
    private int age;
    
    // 默认构造器
    public Person() {
        this("Unknown", 0);
    }
    
    // 参数化构造器
    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // 拷贝构造器
    public Person(Person other) {
        this.name = other.name;
        this.age = other.age;
    }
}
```

### 内部类

```java
public class OuterClass {
    private String outerField = "Outer";
    
    // 成员内部类
    class InnerClass {
        public void display() {
            System.out.println("Accessing: " + outerField);
        }
    }
    
    // 静态内部类
    static class StaticInnerClass {
        public void display() {
            System.out.println("Static inner class");
        }
    }
    
    // 方法内部类
    public void method() {
        class LocalClass {
            public void display() {
                System.out.println("Local class");
            }
        }
        
        LocalClass local = new LocalClass();
        local.display();
    }
}
```

## 设计原则

### SOLID原则

1. **单一职责原则 (SRP)**：一个类应该只有一个引起它变化的原因
2. **开闭原则 (OCP)**：对扩展开放，对修改关闭
3. **里氏替换原则 (LSP)**：子类应该能够替换父类
4. **接口隔离原则 (ISP)**：不应该强迫客户依赖它们不使用的方法
5. **依赖倒置原则 (DIP)**：高层模块不应该依赖低层模块

## 常见面试题

1. **抽象类和接口的区别？**
2. **重载和重写的区别？**
3. **多态的实现原理？**
4. **内部类的分类和使用场景？**
5. **构造器的执行顺序？**

## 最佳实践

1. **优先使用组合而不是继承**
2. **面向接口编程**
3. **保持类的单一职责**
4. **合理使用访问修饰符**
5. **重写equals()时同时重写hashCode()**