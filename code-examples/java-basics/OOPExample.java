package com.javatrack.basics;

/**
 * 面向对象编程示例
 * 演示封装、继承、多态、抽象的基本概念
 */

// 抽象类示例
abstract class Animal {
    protected String name;
    protected int age;
    
    public Animal(String name, int age) {
        this.name = name;
        this.age = age;
    }
    
    // 抽象方法
    public abstract void makeSound();
    public abstract void move();
    
    // 具体方法
    public void eat() {
        System.out.println(name + " is eating");
    }
    
    public void sleep() {
        System.out.println(name + " is sleeping");
    }
    
    // 封装：getter和setter方法
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
        if (age >= 0) {
            this.age = age;
        }
    }
}

// 接口示例
interface Flyable {
    void fly();
    
    // Java 8+ 默认方法
    default void land() {
        System.out.println("Landing...");
    }
    
    // Java 8+ 静态方法
    static void checkWeather() {
        System.out.println("Checking weather conditions for flying");
    }
}

interface Swimmable {
    void swim();
}

// 继承示例
class Dog extends Animal {
    private String breed;
    
    public Dog(String name, int age, String breed) {
        super(name, age); // 调用父类构造器
        this.breed = breed;
    }
    
    // 实现抽象方法
    @Override
    public void makeSound() {
        System.out.println(name + " barks: Woof! Woof!");
    }
    
    @Override
    public void move() {
        System.out.println(name + " runs on four legs");
    }
    
    // 子类特有方法
    public void wagTail() {
        System.out.println(name + " is wagging tail");
    }
    
    public String getBreed() {
        return breed;
    }
}

class Bird extends Animal implements Flyable {
    private double wingspan;
    
    public Bird(String name, int age, double wingspan) {
        super(name, age);
        this.wingspan = wingspan;
    }
    
    @Override
    public void makeSound() {
        System.out.println(name + " chirps: Tweet! Tweet!");
    }
    
    @Override
    public void move() {
        System.out.println(name + " flies in the sky");
    }
    
    @Override
    public void fly() {
        System.out.println(name + " is flying with wingspan of " + wingspan + " meters");
    }
    
    public double getWingspan() {
        return wingspan;
    }
}

class Duck extends Animal implements Flyable, Swimmable {
    public Duck(String name, int age) {
        super(name, age);
    }
    
    @Override
    public void makeSound() {
        System.out.println(name + " quacks: Quack! Quack!");
    }
    
    @Override
    public void move() {
        System.out.println(name + " waddles on land");
    }
    
    @Override
    public void fly() {
        System.out.println(name + " flies over the pond");
    }
    
    @Override
    public void swim() {
        System.out.println(name + " swims gracefully in water");
    }
}

// 内部类示例
class Zoo {
    private String name;
    private static int totalAnimals = 0;
    
    public Zoo(String name) {
        this.name = name;
    }
    
    // 成员内部类
    public class Enclosure {
        private String type;
        private int capacity;
        
        public Enclosure(String type, int capacity) {
            this.type = type;
            this.capacity = capacity;
        }
        
        public void addAnimal(Animal animal) {
            System.out.println("Adding " + animal.getName() + " to " + type + " enclosure in " + name);
            totalAnimals++; // 可以访问外部类的静态变量
        }
        
        public void displayInfo() {
            System.out.println("Enclosure: " + type + ", Capacity: " + capacity + ", Zoo: " + name);
        }
    }
    
    // 静态内部类
    public static class ZooStatistics {
        public static void displayTotalAnimals() {
            System.out.println("Total animals in all zoos: " + totalAnimals);
        }
    }
    
    // 方法内部类示例
    public void organizeEvent(String eventType) {
        class Event {
            private String name;
            private String date;
            
            public Event(String name, String date) {
                this.name = name;
                this.date = date;
            }
            
            public void announce() {
                System.out.println("Event: " + name + " at " + Zoo.this.name + " on " + date);
            }
        }
        
        Event event = new Event(eventType, "2024-01-15");
        event.announce();
    }
}

// 主类演示多态
public class OOPExample {
    public static void main(String[] args) {
        System.out.println("=== 面向对象编程示例 ===");
        
        // 创建不同类型的动物
        Animal[] animals = {
            new Dog("Buddy", 3, "Golden Retriever"),
            new Bird("Robin", 1, 0.3),
            new Duck("Donald", 2)
        };
        
        System.out.println("\n1. 多态演示:");
        // 多态：同一个方法调用，不同的实现
        for (Animal animal : animals) {
            animal.makeSound(); // 运行时多态
            animal.move();
            animal.eat();
            System.out.println();
        }
        
        System.out.println("2. 接口演示:");
        // 接口使用
        Flyable.checkWeather(); // 静态方法调用
        
        for (Animal animal : animals) {
            if (animal instanceof Flyable) {
                Flyable flyable = (Flyable) animal;
                flyable.fly();
                flyable.land(); // 默认方法
            }
            
            if (animal instanceof Swimmable) {
                Swimmable swimmable = (Swimmable) animal;
                swimmable.swim();
            }
        }
        
        System.out.println("\n3. 类型检查和转换:");
        for (Animal animal : animals) {
            System.out.println(animal.getName() + " is instance of:");
            System.out.println("  Animal: " + (animal instanceof Animal));
            System.out.println("  Flyable: " + (animal instanceof Flyable));
            System.out.println("  Swimmable: " + (animal instanceof Swimmable));
            
            // 安全的类型转换
            if (animal instanceof Dog) {
                Dog dog = (Dog) animal;
                dog.wagTail();
                System.out.println("  Breed: " + dog.getBreed());
            }
            System.out.println();
        }
        
        System.out.println("4. 内部类演示:");
        Zoo zoo = new Zoo("Central Zoo");
        
        // 成员内部类
        Zoo.Enclosure mammalEnclosure = zoo.new Enclosure("Mammal", 10);
        Zoo.Enclosure birdEnclosure = zoo.new Enclosure("Bird", 20);
        
        mammalEnclosure.displayInfo();
        birdEnclosure.displayInfo();
        
        // 添加动物到围栏
        for (Animal animal : animals) {
            if (animal instanceof Dog) {
                mammalEnclosure.addAnimal(animal);
            } else {
                birdEnclosure.addAnimal(animal);
            }
        }
        
        // 静态内部类
        Zoo.ZooStatistics.displayTotalAnimals();
        
        // 方法内部类
        zoo.organizeEvent("Animal Feeding Show");
        
        System.out.println("\n5. 匿名内部类演示:");
        // 匿名内部类实现接口
        Flyable helicopter = new Flyable() {
            @Override
            public void fly() {
                System.out.println("Helicopter is flying with rotors");
            }
        };
        helicopter.fly();
        helicopter.land();
        
        // Lambda表达式（Java 8+）
        Flyable rocket = () -> System.out.println("Rocket is flying to space");
        rocket.fly();
        
        System.out.println("\n=== 示例结束 ===");
    }
}