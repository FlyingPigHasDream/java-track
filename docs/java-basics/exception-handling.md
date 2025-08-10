# 异常处理

## 目录
- [异常概述](#异常概述)
- [异常体系结构](#异常体系结构)
- [异常处理机制](#异常处理机制)
- [自定义异常](#自定义异常)
- [异常处理最佳实践](#异常处理最佳实践)
- [常见异常类型](#常见异常类型)
- [性能考虑](#性能考虑)
- [面试要点](#面试要点)

## 异常概述

### 什么是异常
异常是程序执行过程中发生的不正常情况，它会中断程序的正常执行流程。Java通过异常处理机制来处理运行时错误，使程序更加健壮。

### 异常的作用
1. **错误报告**: 提供详细的错误信息
2. **程序恢复**: 允许程序从错误中恢复
3. **代码分离**: 将正常逻辑与错误处理分离
4. **调试支持**: 提供调用栈信息帮助调试

## 异常体系结构

### 异常类层次结构
```
Throwable
├── Error
│   ├── OutOfMemoryError
│   ├── StackOverflowError
│   └── VirtualMachineError
└── Exception
    ├── RuntimeException (非检查异常)
    │   ├── NullPointerException
    │   ├── IllegalArgumentException
    │   ├── IndexOutOfBoundsException
    │   └── ClassCastException
    └── 检查异常
        ├── IOException
        ├── SQLException
        ├── ClassNotFoundException
        └── InterruptedException
```

### 异常分类

#### 1. Error (错误)
- **定义**: 系统级错误，通常由JVM抛出
- **特点**: 程序无法处理，应该避免
- **示例**: OutOfMemoryError、StackOverflowError

#### 2. Exception (异常)
程序可以处理的异常，分为两类：

##### 检查异常 (Checked Exception)
- **定义**: 编译时必须处理的异常
- **处理方式**: 必须用try-catch捕获或throws声明
- **示例**: IOException、SQLException、ClassNotFoundException

##### 非检查异常 (Unchecked Exception)
- **定义**: 运行时异常，编译时不强制处理
- **继承关系**: 继承自RuntimeException
- **示例**: NullPointerException、IllegalArgumentException

## 异常处理机制

### try-catch-finally语句

#### 基本语法
```java
try {
    // 可能抛出异常的代码
} catch (ExceptionType1 e1) {
    // 处理ExceptionType1
} catch (ExceptionType2 e2) {
    // 处理ExceptionType2
} finally {
    // 无论是否发生异常都会执行
}
```

#### 执行顺序
1. 执行try块中的代码
2. 如果发生异常，跳转到对应的catch块
3. 无论是否发生异常，都执行finally块
4. 如果finally块中有return语句，会覆盖try/catch中的return

#### 示例代码
```java
public class ExceptionHandlingExample {
    
    public static void main(String[] args) {
        // 基本异常处理
        basicExceptionHandling();
        
        // 多重异常处理
        multipleExceptionHandling();
        
        // finally块的使用
        finallyBlockExample();
        
        // try-with-resources
        tryWithResourcesExample();
    }
    
    /**
     * 基本异常处理示例
     */
    public static void basicExceptionHandling() {
        System.out.println("=== 基本异常处理 ===");
        
        try {
            int result = 10 / 0; // 会抛出ArithmeticException
            System.out.println("结果: " + result);
        } catch (ArithmeticException e) {
            System.out.println("捕获到算术异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("程序继续执行\n");
    }
    
    /**
     * 多重异常处理示例
     */
    public static void multipleExceptionHandling() {
        System.out.println("=== 多重异常处理 ===");
        
        String[] array = {"1", "2", "abc", "4"};
        
        for (int i = 0; i <= array.length; i++) {
            try {
                int value = Integer.parseInt(array[i]);
                System.out.println("解析结果: " + value);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("数组越界: 索引 " + i + " 超出范围");
            } catch (NumberFormatException e) {
                System.out.println("数字格式错误: " + array[i] + " 不是有效数字");
            } catch (Exception e) {
                System.out.println("其他异常: " + e.getMessage());
            }
        }
        
        System.out.println();
    }
    
    /**
     * finally块示例
     */
    public static void finallyBlockExample() {
        System.out.println("=== finally块示例 ===");
        
        FileInputStream fis = null;
        try {
            fis = new FileInputStream("nonexistent.txt");
            // 读取文件内容
        } catch (FileNotFoundException e) {
            System.out.println("文件未找到: " + e.getMessage());
        } finally {
            // 确保资源被释放
            if (fis != null) {
                try {
                    fis.close();
                    System.out.println("文件流已关闭");
                } catch (IOException e) {
                    System.out.println("关闭文件流时发生错误: " + e.getMessage());
                }
            } else {
                System.out.println("文件流为null，无需关闭");
            }
        }
        
        System.out.println();
    }
    
    /**
     * try-with-resources示例 (Java 7+)
     */
    public static void tryWithResourcesExample() {
        System.out.println("=== try-with-resources示例 ===");
        
        // 自动资源管理
        try (FileInputStream fis = new FileInputStream("nonexistent.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            
            String line = reader.readLine();
            System.out.println("读取内容: " + line);
            
        } catch (FileNotFoundException e) {
            System.out.println("文件未找到: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO异常: " + e.getMessage());
        }
        // 资源会自动关闭，无需手动处理
        
        System.out.println();
    }
}
```

### throws关键字

#### 用法
```java
public void methodName() throws ExceptionType1, ExceptionType2 {
    // 可能抛出异常的代码
}
```

#### 示例
```java
public class ThrowsExample {
    
    /**
     * 声明可能抛出的异常
     */
    public void readFile(String fileName) throws IOException {
        FileReader file = new FileReader(fileName);
        BufferedReader reader = new BufferedReader(file);
        
        String line = reader.readLine();
        System.out.println(line);
        
        reader.close();
    }
    
    /**
     * 调用可能抛出异常的方法
     */
    public void processFile() {
        try {
            readFile("data.txt");
        } catch (IOException e) {
            System.out.println("文件处理失败: " + e.getMessage());
        }
    }
}
```

### throw关键字

#### 用法
```java
if (condition) {
    throw new ExceptionType("错误信息");
}
```

#### 示例
```java
public class ThrowExample {
    
    /**
     * 主动抛出异常
     */
    public void validateAge(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("年龄不能为负数: " + age);
        }
        if (age > 150) {
            throw new IllegalArgumentException("年龄不能超过150: " + age);
        }
        System.out.println("年龄验证通过: " + age);
    }
    
    /**
     * 测试方法
     */
    public static void main(String[] args) {
        ThrowExample example = new ThrowExample();
        
        try {
            example.validateAge(25);  // 正常
            example.validateAge(-5);  // 抛出异常
        } catch (IllegalArgumentException e) {
            System.out.println("参数错误: " + e.getMessage());
        }
    }
}
```

## 自定义异常

### 创建自定义异常

#### 检查异常
```java
/**
 * 自定义检查异常
 */
public class BusinessException extends Exception {
    private int errorCode;
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public BusinessException(String message, int errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
}
```

#### 非检查异常
```java
/**
 * 自定义运行时异常
 */
public class ValidationException extends RuntimeException {
    private String field;
    private Object value;
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String field, Object value, String message) {
        super(String.format("字段 '%s' 的值 '%s' %s", field, value, message));
        this.field = field;
        this.value = value;
    }
    
    public String getField() {
        return field;
    }
    
    public Object getValue() {
        return value;
    }
}
```

### 使用自定义异常
```java
public class UserService {
    
    /**
     * 用户注册
     */
    public void registerUser(String username, String email, int age) 
            throws BusinessException {
        
        // 参数验证
        validateUsername(username);
        validateEmail(email);
        validateAge(age);
        
        // 业务逻辑
        if (isUserExists(username)) {
            throw new BusinessException("用户名已存在: " + username, 1001);
        }
        
        // 保存用户
        saveUser(username, email, age);
        System.out.println("用户注册成功: " + username);
    }
    
    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new ValidationException("username", username, "不能为空");
        }
        if (username.length() < 3) {
            throw new ValidationException("username", username, "长度不能少于3个字符");
        }
    }
    
    private void validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new ValidationException("email", email, "格式不正确");
        }
    }
    
    private void validateAge(int age) {
        if (age < 18 || age > 100) {
            throw new ValidationException("age", age, "必须在18-100之间");
        }
    }
    
    private boolean isUserExists(String username) {
        // 模拟检查用户是否存在
        return "admin".equals(username);
    }
    
    private void saveUser(String username, String email, int age) {
        // 模拟保存用户
        System.out.println("保存用户信息到数据库");
    }
}
```

## 异常处理最佳实践

### 1. 具体化异常处理
```java
// 好的做法：具体的异常处理
try {
    // 业务代码
} catch (FileNotFoundException e) {
    // 处理文件未找到
    log.error("配置文件未找到: " + e.getMessage());
    useDefaultConfig();
} catch (IOException e) {
    // 处理其他IO异常
    log.error("文件读取失败: " + e.getMessage());
    throw new ServiceException("配置加载失败", e);
}

// 避免的做法：过于宽泛的异常处理
try {
    // 业务代码
} catch (Exception e) {
    // 过于宽泛，可能掩盖重要错误
    log.error("发生错误", e);
}
```

### 2. 异常信息要有意义
```java
// 好的做法：提供有用的错误信息
if (balance < amount) {
    throw new InsufficientFundsException(
        String.format("余额不足：当前余额 %.2f，尝试提取 %.2f", balance, amount));
}

// 避免的做法：信息不明确
if (balance < amount) {
    throw new Exception("操作失败");
}
```

### 3. 不要忽略异常
```java
// 好的做法：适当处理异常
try {
    riskyOperation();
} catch (SpecificException e) {
    log.warn("操作失败，使用默认值: " + e.getMessage());
    useDefaultValue();
}

// 避免的做法：空的catch块
try {
    riskyOperation();
} catch (Exception e) {
    // 什么都不做 - 这是危险的！
}
```

### 4. 及早失败原则
```java
public void processOrder(Order order) {
    // 及早验证参数
    if (order == null) {
        throw new IllegalArgumentException("订单不能为null");
    }
    if (order.getItems().isEmpty()) {
        throw new IllegalArgumentException("订单必须包含至少一个商品");
    }
    
    // 继续处理...
}
```

### 5. 使用异常链
```java
public void processData() throws DataProcessingException {
    try {
        // 底层操作
        lowLevelOperation();
    } catch (SQLException e) {
        // 包装底层异常，保留原始异常信息
        throw new DataProcessingException("数据处理失败", e);
    }
}
```

## 常见异常类型

### 运行时异常

#### NullPointerException
```java
// 常见场景
String str = null;
int length = str.length(); // NPE

// 防护措施
if (str != null) {
    int length = str.length();
}
// 或使用Optional
Optional.ofNullable(str).map(String::length).orElse(0);
```

#### IllegalArgumentException
```java
public void setAge(int age) {
    if (age < 0 || age > 150) {
        throw new IllegalArgumentException("年龄必须在0-150之间: " + age);
    }
    this.age = age;
}
```

#### IndexOutOfBoundsException
```java
List<String> list = Arrays.asList("a", "b", "c");
// 检查索引范围
if (index >= 0 && index < list.size()) {
    String item = list.get(index);
}
```

### 检查异常

#### IOException
```java
public String readFile(String fileName) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(Paths.get(fileName))) {
        return reader.lines().collect(Collectors.joining("\n"));
    }
}
```

#### SQLException
```java
public List<User> getUsers() throws SQLException {
    String sql = "SELECT * FROM users";
    try (Connection conn = getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {
        
        List<User> users = new ArrayList<>();
        while (rs.next()) {
            users.add(mapResultSetToUser(rs));
        }
        return users;
    }
}
```

## 性能考虑

### 异常的性能开销
1. **创建异常对象**: 需要填充调用栈
2. **抛出异常**: 中断正常执行流程
3. **捕获异常**: 查找匹配的catch块

### 性能优化建议

#### 1. 避免在循环中抛出异常
```java
// 低效的做法
for (String item : items) {
    try {
        processItem(item);
    } catch (ProcessingException e) {
        // 每次异常都有性能开销
        handleError(item, e);
    }
}

// 更好的做法
for (String item : items) {
    if (isValidItem(item)) {
        processItem(item);
    } else {
        handleInvalidItem(item);
    }
}
```

#### 2. 使用预定义异常
```java
// 重用异常实例（适用于频繁抛出的场景）
public class Constants {
    public static final IllegalArgumentException INVALID_PARAMETER = 
        new IllegalArgumentException("参数无效");
}

// 使用时
if (!isValid(param)) {
    throw Constants.INVALID_PARAMETER;
}
```

#### 3. 异常与返回值的选择
```java
// 对于预期的错误情况，考虑使用返回值而不是异常
public Optional<User> findUser(String id) {
    // 用户不存在是正常情况，不应该抛异常
    User user = database.findById(id);
    return Optional.ofNullable(user);
}

// 对于真正的异常情况，使用异常
public User getUser(String id) throws UserNotFoundException {
    User user = database.findById(id);
    if (user == null) {
        throw new UserNotFoundException("用户不存在: " + id);
    }
    return user;
}
```

## 面试要点

### 高频问题

1. **Java异常体系结构？**
   - Throwable -> Error/Exception
   - 检查异常 vs 非检查异常
   - 常见异常类型

2. **try-catch-finally的执行顺序？**
   - 详细描述执行流程
   - finally块的特殊情况
   - return语句的处理

3. **throw和throws的区别？**
   - throw: 主动抛出异常
   - throws: 声明可能抛出的异常

4. **什么时候使用检查异常，什么时候使用非检查异常？**
   - 检查异常：调用者可以合理处理的异常
   - 非检查异常：编程错误或系统异常

5. **如何自定义异常？**
   - 继承合适的异常类
   - 提供有意义的构造函数
   - 包含必要的错误信息

### 深入问题

1. **异常的性能影响？**
   - 创建、抛出、捕获的开销
   - 优化策略

2. **异常处理的最佳实践？**
   - 具体化异常处理
   - 异常信息的设计
   - 异常链的使用

3. **try-with-resources的原理？**
   - AutoCloseable接口
   - 编译器的语法糖
   - 异常抑制机制

4. **如何设计异常体系？**
   - 异常分层
   - 错误码设计
   - 国际化支持

### 实践经验
- 了解常见框架的异常处理机制
- 掌握异常监控和日志记录
- 理解分布式系统中的异常传播
- 熟悉异常处理的调试技巧

## 总结

异常处理是Java编程的重要组成部分，正确使用异常机制可以：

1. **提高代码健壮性**: 优雅处理错误情况
2. **改善代码可读性**: 分离正常逻辑和错误处理
3. **便于调试维护**: 提供详细的错误信息
4. **增强用户体验**: 友好的错误提示

掌握异常处理的原理和最佳实践，是成为优秀Java开发者的必备技能。在实际开发中，要根据具体场景选择合适的异常处理策略，平衡代码的可读性、性能和维护性。