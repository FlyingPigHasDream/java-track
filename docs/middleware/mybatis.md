# MyBatis 持久层框架

## 目录
- [MyBatis概述](#mybatis概述)
- [MyBatis核心概念](#mybatis核心概念)
- [Spring Boot集成MyBatis](#spring-boot集成mybatis)
- [基础使用](#基础使用)
- [动态SQL](#动态sql)
- [高级特性](#高级特性)
- [MyBatis-Plus](#mybatis-plus)
- [性能优化](#性能优化)
- [最佳实践](#最佳实践)
- [面试要点](#面试要点)
- [总结](#总结)

## MyBatis概述

### 什么是MyBatis
MyBatis是一款优秀的持久层框架，它支持自定义SQL、存储过程以及高级映射。MyBatis免除了几乎所有的JDBC代码以及设置参数和获取结果集的工作。MyBatis可以通过简单的XML或注解来配置和映射原始类型、接口和Java POJO为数据库中的记录。

### MyBatis特点
1. **简单易学**：基于SQL语句编程，相当灵活
2. **SQL与代码分离**：将SQL语句配置在XML文件中
3. **提供映射标签**：支持对象与数据库的ORM字段关系映射
4. **提供对象关系映射标签**：支持对象关系组件维护
5. **提供XML标签**：支持编写动态SQL语句

### 应用场景
- 需要灵活控制SQL的项目
- 对性能要求较高的系统
- 复杂查询较多的业务场景
- 需要与现有数据库系统集成的项目

## MyBatis核心概念

### 1. SqlSessionFactory
SqlSessionFactory是MyBatis的核心类之一，用于创建SqlSession实例。

```java
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;

public class MyBatisUtils {
    private static SqlSessionFactory sqlSessionFactory;
    
    static {
        try {
            String resource = "mybatis-config.xml";
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static SqlSessionFactory getSqlSessionFactory() {
        return sqlSessionFactory;
    }
}
```

### 2. SqlSession
SqlSession是MyBatis工作的主要顶层API，表示和数据库交互的会话。

```java
import org.apache.ibatis.session.SqlSession;

public class UserService {
    
    public User getUserById(Long id) {
        try (SqlSession session = MyBatisUtils.getSqlSessionFactory().openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            return mapper.selectById(id);
        }
    }
    
    public void insertUser(User user) {
        try (SqlSession session = MyBatisUtils.getSqlSessionFactory().openSession()) {
            UserMapper mapper = session.getMapper(UserMapper.class);
            mapper.insert(user);
            session.commit();  // 手动提交事务
        }
    }
}
```

### 3. Mapper接口
Mapper接口定义了数据库操作的方法签名。

```java
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface UserMapper {
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    User selectById(@Param("id") Long id);
    
    @Select("SELECT * FROM users")
    List<User> selectAll();
    
    @Insert("INSERT INTO users(name, email, age) VALUES(#{name}, #{email}, #{age})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);
    
    @Update("UPDATE users SET name=#{name}, email=#{email}, age=#{age} WHERE id=#{id}")
    int update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
}
```

### 4. 实体类
定义与数据库表对应的Java对象。

```java
import java.time.LocalDateTime;

public class User {
    private Long id;
    private String name;
    private String email;
    private Integer age;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 构造函数
    public User() {}
    
    public User(String name, String email, Integer age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }
    
    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", age=" + age +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
```

## Spring Boot集成MyBatis

### 1. 添加依赖

```xml
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- MyBatis Spring Boot Starter -->
    <dependency>
        <groupId>org.mybatis.spring.boot</groupId>
        <artifactId>mybatis-spring-boot-starter</artifactId>
        <version>3.0.3</version>
    </dependency>
    
    <!-- MySQL驱动 -->
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <scope>runtime</scope>
    </dependency>
    
    <!-- 连接池 -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid-spring-boot-starter</artifactId>
        <version>1.2.20</version>
    </dependency>
</dependencies>
```

### 2. 配置文件

```yaml
# application.yml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mybatis_demo?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2B8
    username: root
    password: password
    
    # Druid连接池配置
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      
# MyBatis配置
mybatis:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: true
    lazy-loading-enabled: true
    multiple-result-sets-enabled: true
    use-column-label: true
    use-generated-keys: false
    auto-mapping-behavior: partial
    default-executor-type: simple
    default-statement-timeout: 25
    default-fetch-size: 100
    safe-row-bounds-enabled: false
    local-cache-scope: session
    jdbc-type-for-null: other
    lazy-load-trigger-methods: equals,clone,hashCode,toString
    
# 日志配置
logging:
  level:
    com.example.mapper: debug
```

### 3. MyBatis配置类

```java
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.mapper")
public class MyBatisConfig {
    // MyBatis相关配置
}
```

### 4. 启动类

```java
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MyBatisDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyBatisDemoApplication.class, args);
    }
}
```

## 基础使用

### 1. XML方式配置Mapper

```xml
<!-- UserMapper.xml -->
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN" 
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.example.mapper.UserMapper">
    
    <!-- 结果映射 -->
    <resultMap id="UserResultMap" type="User">
        <id property="id" column="id"/>
        <result property="name" column="name"/>
        <result property="email" column="email"/>
        <result property="age" column="age"/>
        <result property="createTime" column="create_time"/>
        <result property="updateTime" column="update_time"/>
    </resultMap>
    
    <!-- 查询单个用户 -->
    <select id="selectById" parameterType="long" resultMap="UserResultMap">
        SELECT id, name, email, age, create_time, update_time
        FROM users
        WHERE id = #{id}
    </select>
    
    <!-- 查询所有用户 -->
    <select id="selectAll" resultMap="UserResultMap">
        SELECT id, name, email, age, create_time, update_time
        FROM users
        ORDER BY create_time DESC
    </select>
    
    <!-- 根据条件查询用户 -->
    <select id="selectByCondition" parameterType="User" resultMap="UserResultMap">
        SELECT id, name, email, age, create_time, update_time
        FROM users
        WHERE 1=1
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="email != null and email != ''">
            AND email = #{email}
        </if>
        <if test="age != null">
            AND age = #{age}
        </if>
    </select>
    
    <!-- 插入用户 -->
    <insert id="insert" parameterType="User" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (name, email, age, create_time, update_time)
        VALUES (#{name}, #{email}, #{age}, NOW(), NOW())
    </insert>
    
    <!-- 批量插入用户 -->
    <insert id="insertBatch" parameterType="list">
        INSERT INTO users (name, email, age, create_time, update_time)
        VALUES
        <foreach collection="list" item="user" separator=",">
            (#{user.name}, #{user.email}, #{user.age}, NOW(), NOW())
        </foreach>
    </insert>
    
    <!-- 更新用户 -->
    <update id="update" parameterType="User">
        UPDATE users
        SET name = #{name},
            email = #{email},
            age = #{age},
            update_time = NOW()
        WHERE id = #{id}
    </update>
    
    <!-- 动态更新用户 -->
    <update id="updateSelective" parameterType="User">
        UPDATE users
        <set>
            <if test="name != null and name != ''">
                name = #{name},
            </if>
            <if test="email != null and email != ''">
                email = #{email},
            </if>
            <if test="age != null">
                age = #{age},
            </if>
            update_time = NOW()
        </set>
        WHERE id = #{id}
    </update>
    
    <!-- 删除用户 -->
    <delete id="deleteById" parameterType="long">
        DELETE FROM users WHERE id = #{id}
    </delete>
    
    <!-- 批量删除用户 -->
    <delete id="deleteByIds" parameterType="list">
        DELETE FROM users
        WHERE id IN
        <foreach collection="list" item="id" open="(" separator="," close=")">
            #{id}
        </foreach>
    </delete>
    
</mapper>
```

### 2. Mapper接口

```java
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface UserMapper {
    
    // 基础CRUD操作
    User selectById(@Param("id") Long id);
    List<User> selectAll();
    List<User> selectByCondition(User user);
    
    int insert(User user);
    int insertBatch(@Param("list") List<User> users);
    
    int update(User user);
    int updateSelective(User user);
    
    int deleteById(@Param("id") Long id);
    int deleteByIds(@Param("list") List<Long> ids);
    
    // 分页查询
    List<User> selectByPage(@Param("offset") int offset, @Param("limit") int limit);
    int countTotal();
    
    // 复杂查询
    List<User> selectByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);
    List<User> selectByNamePattern(@Param("pattern") String pattern);
}
```

### 3. Service层实现

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 根据ID查询用户
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        return userMapper.selectById(id);
    }
    
    /**
     * 查询所有用户
     */
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }
    
    /**
     * 根据条件查询用户
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByCondition(User condition) {
        return userMapper.selectByCondition(condition);
    }
    
    /**
     * 创建用户
     */
    public User createUser(User user) {
        validateUser(user);
        int result = userMapper.insert(user);
        if (result > 0) {
            return user;  // 返回包含生成ID的用户对象
        }
        throw new RuntimeException("创建用户失败");
    }
    
    /**
     * 批量创建用户
     */
    public int createUsers(List<User> users) {
        if (users == null || users.isEmpty()) {
            throw new IllegalArgumentException("用户列表不能为空");
        }
        
        // 验证每个用户
        users.forEach(this::validateUser);
        
        return userMapper.insertBatch(users);
    }
    
    /**
     * 更新用户
     */
    public boolean updateUser(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        validateUser(user);
        
        int result = userMapper.update(user);
        return result > 0;
    }
    
    /**
     * 选择性更新用户
     */
    public boolean updateUserSelective(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        int result = userMapper.updateSelective(user);
        return result > 0;
    }
    
    /**
     * 删除用户
     */
    public boolean deleteUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        int result = userMapper.deleteById(id);
        return result > 0;
    }
    
    /**
     * 批量删除用户
     */
    public int deleteUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("用户ID列表不能为空");
        }
        
        return userMapper.deleteByIds(ids);
    }
    
    /**
     * 分页查询用户
     */
    @Transactional(readOnly = true)
    public PageResult<User> getUsersByPage(int page, int size) {
        if (page < 1 || size < 1) {
            throw new IllegalArgumentException("页码和页大小必须大于0");
        }
        
        int offset = (page - 1) * size;
        List<User> users = userMapper.selectByPage(offset, size);
        int total = userMapper.countTotal();
        
        return new PageResult<>(users, total, page, size);
    }
    
    /**
     * 根据年龄范围查询用户
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByAgeRange(Integer minAge, Integer maxAge) {
        if (minAge != null && maxAge != null && minAge > maxAge) {
            throw new IllegalArgumentException("最小年龄不能大于最大年龄");
        }
        
        return userMapper.selectByAgeRange(minAge, maxAge);
    }
    
    /**
     * 根据姓名模式查询用户
     */
    @Transactional(readOnly = true)
    public List<User> getUsersByNamePattern(String pattern) {
        if (pattern == null || pattern.trim().isEmpty()) {
            throw new IllegalArgumentException("姓名模式不能为空");
        }
        
        return userMapper.selectByNamePattern(pattern);
    }
    
    /**
     * 验证用户数据
     */
    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("用户对象不能为空");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("用户姓名不能为空");
        }
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("用户邮箱不能为空");
        }
        if (user.getAge() == null || user.getAge() < 0 || user.getAge() > 150) {
            throw new IllegalArgumentException("用户年龄必须在0-150之间");
        }
    }
    
    /**
     * 分页结果封装类
     */
    public static class PageResult<T> {
        private List<T> data;
        private int total;
        private int page;
        private int size;
        private int totalPages;
        
        public PageResult(List<T> data, int total, int page, int size) {
            this.data = data;
            this.total = total;
            this.page = page;
            this.size = size;
            this.totalPages = (int) Math.ceil((double) total / size);
        }
        
        // getters and setters
        public List<T> getData() { return data; }
        public void setData(List<T> data) { this.data = data; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getSize() { return size; }
        public void setSize(int size) { this.size = size; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        
        @Override
        public String toString() {
            return "PageResult{" +
                    "data=" + data +
                    ", total=" + total +
                    ", page=" + page +
                    ", size=" + size +
                    ", totalPages=" + totalPages +
                    '}';
        }
    }
}
```

### 4. Controller层实现

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    /**
     * 根据ID查询用户
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        try {
            User user = userService.getUserById(id);
            if (user != null) {
                return ResponseEntity.ok(user);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 查询所有用户
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        try {
            List<User> users = userService.getAllUsers();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 分页查询用户
     */
    @GetMapping("/page")
    public ResponseEntity<UserService.PageResult<User>> getUsersByPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            UserService.PageResult<User> result = userService.getUsersByPage(page, size);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据条件查询用户
     */
    @PostMapping("/search")
    public ResponseEntity<List<User>> getUsersByCondition(@RequestBody User condition) {
        try {
            List<User> users = userService.getUsersByCondition(condition);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 创建用户
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return ResponseEntity.ok(createdUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 批量创建用户
     */
    @PostMapping("/batch")
    public ResponseEntity<String> createUsers(@RequestBody List<User> users) {
        try {
            int count = userService.createUsers(users);
            return ResponseEntity.ok("成功创建 " + count + " 个用户");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("创建用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public ResponseEntity<String> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            user.setId(id);
            boolean success = userService.updateUser(user);
            if (success) {
                return ResponseEntity.ok("用户更新成功");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 选择性更新用户
     */
    @PatchMapping("/{id}")
    public ResponseEntity<String> updateUserSelective(@PathVariable Long id, @RequestBody User user) {
        try {
            user.setId(id);
            boolean success = userService.updateUserSelective(user);
            if (success) {
                return ResponseEntity.ok("用户更新成功");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("更新用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        try {
            boolean success = userService.deleteUser(id);
            if (success) {
                return ResponseEntity.ok("用户删除成功");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量删除用户
     */
    @DeleteMapping("/batch")
    public ResponseEntity<String> deleteUsers(@RequestBody List<Long> ids) {
        try {
            int count = userService.deleteUsers(ids);
            return ResponseEntity.ok("成功删除 " + count + " 个用户");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除用户失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据年龄范围查询用户
     */
    @GetMapping("/age-range")
    public ResponseEntity<List<User>> getUsersByAgeRange(
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge) {
        try {
            List<User> users = userService.getUsersByAgeRange(minAge, maxAge);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 根据姓名模式查询用户
     */
    @GetMapping("/search-by-name")
    public ResponseEntity<List<User>> getUsersByNamePattern(@RequestParam String pattern) {
        try {
            List<User> users = userService.getUsersByNamePattern(pattern);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
```

## 动态SQL

### 1. if标签

```xml
<!-- 根据条件动态查询 -->
<select id="selectByCondition" parameterType="User" resultMap="UserResultMap">
    SELECT * FROM users
    WHERE 1=1
    <if test="name != null and name != ''">
        AND name LIKE CONCAT('%', #{name}, '%')
    </if>
    <if test="email != null and email != ''">
        AND email = #{email}
    </if>
    <if test="age != null">
        AND age = #{age}
    </if>
    <if test="minAge != null">
        AND age >= #{minAge}
    </if>
    <if test="maxAge != null">
        AND age <= #{maxAge}
    </if>
</select>
```

### 2. choose、when、otherwise标签

```xml
<!-- 根据不同条件选择不同的查询方式 -->
<select id="selectByDynamicCondition" parameterType="map" resultMap="UserResultMap">
    SELECT * FROM users
    WHERE 1=1
    <choose>
        <when test="searchType == 'name' and keyword != null">
            AND name LIKE CONCAT('%', #{keyword}, '%')
        </when>
        <when test="searchType == 'email' and keyword != null">
            AND email LIKE CONCAT('%', #{keyword}, '%')
        </when>
        <when test="searchType == 'age' and keyword != null">
            AND age = #{keyword}
        </when>
        <otherwise>
            AND 1=1
        </otherwise>
    </choose>
</select>
```

### 3. where标签

```xml
<!-- where标签自动处理AND/OR -->
<select id="selectByConditionWithWhere" parameterType="User" resultMap="UserResultMap">
    SELECT * FROM users
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="email != null and email != ''">
            AND email = #{email}
        </if>
        <if test="age != null">
            AND age = #{age}
        </if>
    </where>
</select>
```

### 4. set标签

```xml
<!-- set标签用于动态更新 -->
<update id="updateSelective" parameterType="User">
    UPDATE users
    <set>
        <if test="name != null and name != ''">
            name = #{name},
        </if>
        <if test="email != null and email != ''">
            email = #{email},
        </if>
        <if test="age != null">
            age = #{age},
        </if>
        update_time = NOW()
    </set>
    WHERE id = #{id}
</update>
```

### 5. foreach标签

```xml
<!-- 批量插入 -->
<insert id="insertBatch" parameterType="list">
    INSERT INTO users (name, email, age, create_time, update_time)
    VALUES
    <foreach collection="list" item="user" separator=",">
        (#{user.name}, #{user.email}, #{user.age}, NOW(), NOW())
    </foreach>
</insert>

<!-- IN查询 -->
<select id="selectByIds" parameterType="list" resultMap="UserResultMap">
    SELECT * FROM users
    WHERE id IN
    <foreach collection="list" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>

<!-- 批量更新 -->
<update id="updateBatch" parameterType="list">
    <foreach collection="list" item="user" separator=";">
        UPDATE users
        SET name = #{user.name}, email = #{user.email}, age = #{user.age}, update_time = NOW()
        WHERE id = #{user.id}
    </foreach>
</update>
```

### 6. trim标签

```xml
<!-- trim标签更灵活的条件处理 -->
<select id="selectByConditionWithTrim" parameterType="User" resultMap="UserResultMap">
    SELECT * FROM users
    <trim prefix="WHERE" prefixOverrides="AND |OR ">
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="email != null and email != ''">
            AND email = #{email}
        </if>
        <if test="age != null">
            AND age = #{age}
        </if>
    </trim>
</select>

<!-- 动态SET -->
<update id="updateSelectiveWithTrim" parameterType="User">
    UPDATE users
    <trim prefix="SET" suffixOverrides=",">
        <if test="name != null and name != ''">
            name = #{name},
        </if>
        <if test="email != null and email != ''">
            email = #{email},
        </if>
        <if test="age != null">
            age = #{age},
        </if>
        update_time = NOW()
    </trim>
    WHERE id = #{id}
</update>
```

### 7. SQL片段复用

```xml
<!-- 定义SQL片段 -->
<sql id="userColumns">
    id, name, email, age, create_time, update_time
</sql>

<sql id="userConditions">
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="email != null and email != ''">
            AND email = #{email}
        </if>
        <if test="age != null">
            AND age = #{age}
        </if>
    </where>
</sql>

<!-- 使用SQL片段 -->
<select id="selectByConditionWithFragment" parameterType="User" resultMap="UserResultMap">
    SELECT <include refid="userColumns"/>
    FROM users
    <include refid="userConditions"/>
</select>
```

## 高级特性

### 1. 结果映射

```xml
<!-- 复杂结果映射 -->
<resultMap id="UserWithOrdersResultMap" type="User">
    <id property="id" column="user_id"/>
    <result property="name" column="user_name"/>
    <result property="email" column="user_email"/>
    <result property="age" column="user_age"/>
    
    <!-- 一对多关联 -->
    <collection property="orders" ofType="Order">
        <id property="id" column="order_id"/>
        <result property="orderNo" column="order_no"/>
        <result property="amount" column="order_amount"/>
        <result property="status" column="order_status"/>
    </collection>
</resultMap>

<!-- 关联查询 -->
<select id="selectUserWithOrders" parameterType="long" resultMap="UserWithOrdersResultMap">
    SELECT 
        u.id as user_id,
        u.name as user_name,
        u.email as user_email,
        u.age as user_age,
        o.id as order_id,
        o.order_no,
        o.amount as order_amount,
        o.status as order_status
    FROM users u
    LEFT JOIN orders o ON u.id = o.user_id
    WHERE u.id = #{id}
</select>
```

### 2. 延迟加载

```xml
<!-- 延迟加载配置 -->
<resultMap id="UserLazyResultMap" type="User">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <result property="email" column="email"/>
    <result property="age" column="age"/>
    
    <!-- 延迟加载订单信息 -->
    <collection property="orders" ofType="Order" 
                select="selectOrdersByUserId" 
                column="id" 
                fetchType="lazy"/>
</resultMap>

<!-- 查询用户订单 -->
<select id="selectOrdersByUserId" parameterType="long" resultType="Order">
    SELECT id, order_no, amount, status, user_id
    FROM orders
    WHERE user_id = #{userId}
</select>
```

### 3. 缓存配置

```xml
<!-- 开启二级缓存 -->
<cache eviction="LRU" flushInterval="60000" size="512" readOnly="true"/>

<!-- 或者使用第三方缓存 -->
<cache type="org.mybatis.caches.redis.RedisCache"/>

<!-- 查询使用缓存 -->
<select id="selectById" parameterType="long" resultMap="UserResultMap" useCache="true">
    SELECT * FROM users WHERE id = #{id}
</select>

<!-- 更新刷新缓存 -->
<update id="update" parameterType="User" flushCache="true">
    UPDATE users
    SET name = #{name}, email = #{email}, age = #{age}, update_time = NOW()
    WHERE id = #{id}
</update>
```

### 4. 自定义类型处理器

```java
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 自定义LocalDateTime类型处理器
 */
@MappedTypes(LocalDateTime.class)
@MappedJdbcTypes(JdbcType.VARCHAR)
public class LocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.format(FORMATTER));
    }
    
    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String value = rs.getString(columnName);
        return value != null ? LocalDateTime.parse(value, FORMATTER) : null;
    }
    
    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String value = rs.getString(columnIndex);
        return value != null ? LocalDateTime.parse(value, FORMATTER) : null;
    }
    
    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String value = cs.getString(columnIndex);
        return value != null ? LocalDateTime.parse(value, FORMATTER) : null;
    }
}
```

### 5. 插件开发

```java
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Properties;

/**
 * 性能监控插件
 */
@Intercepts({
    @Signature(type = Executor.class, method = "query", args = {
        MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class
    }),
    @Signature(type = Executor.class, method = "update", args = {
        MappedStatement.class, Object.class
    })
})
public class PerformanceInterceptor implements Interceptor {
    
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            // 执行原方法
            Object result = invocation.proceed();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            // 记录执行时间
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            String sqlId = mappedStatement.getId();
            
            System.out.println(String.format("SQL执行时间监控 - ID: %s, 耗时: %d ms", sqlId, duration));
            
            // 慢SQL告警
            if (duration > 1000) {
                System.err.println(String.format("慢SQL告警 - ID: %s, 耗时: %d ms", sqlId, duration));
            }
            
            return result;
            
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
            String sqlId = mappedStatement.getId();
            
            System.err.println(String.format("SQL执行异常 - ID: %s, 耗时: %d ms, 异常: %s", 
                                            sqlId, duration, e.getMessage()));
            
            throw e;
        }
    }
    
    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }
    
    @Override
    public void setProperties(Properties properties) {
        // 设置插件属性
    }
}
```

## MyBatis-Plus

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.4</version>
</dependency>
```

### 2. 配置

```yaml
# MyBatis-Plus配置
mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: com.example.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    call-setters-on-nulls: true
    jdbc-type-for-null: 'null'
  global-config:
    db-config:
      id-type: ASSIGN_ID
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      update-strategy: NOT_NULL
      insert-strategy: NOT_NULL
      select-strategy: NOT_EMPTY
```

### 3. 实体类

```java
import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("users")
public class User {
    
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    
    @TableField("name")
    private String name;
    
    @TableField("email")
    private String email;
    
    @TableField("age")
    private Integer age;
    
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
    
    @Version
    @TableField("version")
    private Integer version;
    
    // 构造函数、getters、setters...
}
```

### 4. Mapper接口

```java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper extends BaseMapper<User> {
    
    // 自定义查询方法
    List<User> selectByAgeRange(@Param("minAge") Integer minAge, @Param("maxAge") Integer maxAge);
    
    // 分页查询
    IPage<User> selectUserPage(Page<User> page, @Param("name") String name);
    
    // 统计查询
    Long countByAge(@Param("age") Integer age);
}
```

### 5. Service层

```java
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {
    
    /**
     * 根据条件查询用户
     */
    public List<User> getUsersByCondition(String name, Integer age) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(name != null, "name", name)
                   .eq(age != null, "age", age)
                   .orderByDesc("create_time");
        
        return list(queryWrapper);
    }
    
    /**
     * 分页查询用户
     */
    public IPage<User> getUsersByPage(int current, int size, String name) {
        Page<User> page = new Page<>(current, size);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(name != null, "name", name)
                   .orderByDesc("create_time");
        
        return page(page, queryWrapper);
    }
    
    /**
     * 批量更新用户状态
     */
    public boolean updateUserStatus(List<Long> ids, Integer status) {
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.in("id", ids)
                    .set("status", status);
        
        return update(updateWrapper);
    }
    
    /**
     * 根据年龄范围查询用户
     */
    public List<User> getUsersByAgeRange(Integer minAge, Integer maxAge) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge(minAge != null, "age", minAge)
                   .le(maxAge != null, "age", maxAge);
        
        return list(queryWrapper);
    }
    
    /**
     * 软删除用户
     */
    public boolean softDeleteUser(Long id) {
        return removeById(id);  // MyBatis-Plus自动处理逻辑删除
    }
    
    /**
     * 批量插入用户
     */
    public boolean batchInsertUsers(List<User> users) {
        return saveBatch(users);
    }
}
```

### 6. 自动填充处理器

```java
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
        this.strictInsertFill(metaObject, "version", Integer.class, 1);
    }
    
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

### 7. 分页插件配置

```java
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {
    
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        
        // 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        return interceptor;
    }
}
```

## 性能优化

### 1. SQL优化

```xml
<!-- 避免SELECT * -->
<select id="selectUserInfo" resultType="User">
    SELECT id, name, email, age FROM users WHERE id = #{id}
</select>

<!-- 使用索引字段查询 -->
<select id="selectByEmail" resultType="User">
    SELECT id, name, email, age FROM users WHERE email = #{email}
</select>

<!-- 分页查询优化 -->
<select id="selectByPageOptimized" resultType="User">
    SELECT id, name, email, age 
    FROM users 
    WHERE id > #{lastId}
    ORDER BY id 
    LIMIT #{pageSize}
</select>
```

### 2. 批量操作优化

```java
@Service
public class BatchOptimizationService {
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 批量插入优化
     */
    @Transactional
    public void batchInsertOptimized(List<User> users) {
        int batchSize = 1000;
        for (int i = 0; i < users.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, users.size());
            List<User> batch = users.subList(i, endIndex);
            userMapper.insertBatch(batch);
        }
    }
    
    /**
     * 批量更新优化
     */
    @Transactional
    public void batchUpdateOptimized(List<User> users) {
        for (User user : users) {
            userMapper.updateSelective(user);
        }
    }
}
```

### 3. 连接池优化

```yaml
# Druid连接池优化配置
spring:
  datasource:
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 50
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      pool-prepared-statements: true
      max-pool-prepared-statement-per-connection-size: 20
      # 监控配置
      stat-view-servlet:
        enabled: true
        url-pattern: /druid/*
        login-username: admin
        login-password: admin
      web-stat-filter:
        enabled: true
        url-pattern: /*
        exclusions: "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*"
```

### 4. 缓存优化

```java
/**
 * Redis缓存优化
 */
@Service
public class CachedUserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String USER_CACHE_PREFIX = "user:";
    private static final long CACHE_EXPIRE_TIME = 3600; // 1小时
    
    /**
     * 缓存查询用户
     */
    public User getUserByIdWithCache(Long id) {
        String cacheKey = USER_CACHE_PREFIX + id;
        
        // 先从缓存获取
        User user = (User) redisTemplate.opsForValue().get(cacheKey);
        if (user != null) {
            return user;
        }
        
        // 缓存未命中，查询数据库
        user = userMapper.selectById(id);
        if (user != null) {
            // 存入缓存
            redisTemplate.opsForValue().set(cacheKey, user, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }
        
        return user;
    }
    
    /**
     * 更新用户并清除缓存
     */
    @Transactional
    public boolean updateUserAndClearCache(User user) {
        boolean success = userMapper.update(user) > 0;
        if (success) {
            // 清除缓存
            String cacheKey = USER_CACHE_PREFIX + user.getId();
            redisTemplate.delete(cacheKey);
        }
        return success;
    }
}
```

### 5. 懒加载优化

```yaml
# MyBatis懒加载配置
mybatis:
  configuration:
    lazy-loading-enabled: true
    aggressive-lazy-loading: false
    lazy-load-trigger-methods: equals,clone,hashCode,toString
```

```xml
<!-- 懒加载关联查询 -->
<resultMap id="UserWithOrdersLazyMap" type="User">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <result property="email" column="email"/>
    <result property="age" column="age"/>
    
    <!-- 懒加载订单信息 -->
    <collection property="orders" ofType="Order" 
                select="selectOrdersByUserId" 
                column="id" 
                fetchType="lazy"/>
</resultMap>
```

## 最佳实践

### 1. 项目结构规范

```
src/main/java/
├── com/example/
│   ├── entity/          # 实体类
│   │   ├── User.java
│   │   └── Order.java
│   ├── mapper/          # Mapper接口
│   │   ├── UserMapper.java
│   │   └── OrderMapper.java
│   ├── service/         # 服务层
│   │   ├── UserService.java
│   │   └── OrderService.java
│   ├── controller/      # 控制层
│   │   ├── UserController.java
│   │   └── OrderController.java
│   └── config/          # 配置类
│       └── MyBatisConfig.java
│
src/main/resources/
├── mapper/              # Mapper XML文件
│   ├── UserMapper.xml
│   └── OrderMapper.xml
├── application.yml      # 配置文件
└── mybatis-config.xml   # MyBatis配置文件（可选）
```

### 2. 命名规范

```java
/**
 * 命名规范示例
 */
public interface UserMapper {
    
    // 查询方法：select + By + 条件
    User selectById(Long id);
    List<User> selectByName(String name);
    List<User> selectByAgeRange(Integer minAge, Integer maxAge);
    
    // 插入方法：insert + 描述
    int insert(User user);
    int insertBatch(List<User> users);
    int insertSelective(User user);
    
    // 更新方法：update + 描述
    int update(User user);
    int updateSelective(User user);
    int updateBatch(List<User> users);
    
    // 删除方法：delete + By + 条件
    int deleteById(Long id);
    int deleteByIds(List<Long> ids);
    
    // 统计方法：count + By + 条件
    int countByAge(Integer age);
    int countTotal();
}
```

### 3. 异常处理

```java
@Service
@Transactional
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 创建用户（带异常处理）
     */
    public User createUser(User user) {
        try {
            // 参数验证
            validateUser(user);
            
            // 检查邮箱是否已存在
            User existingUser = userMapper.selectByEmail(user.getEmail());
            if (existingUser != null) {
                throw new BusinessException("邮箱已存在");
            }
            
            // 插入用户
            int result = userMapper.insert(user);
            if (result <= 0) {
                throw new DataAccessException("插入用户失败");
            }
            
            return user;
            
        } catch (BusinessException e) {
            // 业务异常，直接抛出
            throw e;
        } catch (Exception e) {
            // 系统异常，包装后抛出
            throw new SystemException("创建用户时发生系统错误", e);
        }
    }
    
    /**
     * 批量操作异常处理
     */
    @Transactional(rollbackFor = Exception.class)
    public BatchResult batchCreateUsers(List<User> users) {
        BatchResult result = new BatchResult();
        
        for (int i = 0; i < users.size(); i++) {
            try {
                User user = users.get(i);
                createUser(user);
                result.addSuccess(i, user);
                
            } catch (Exception e) {
                result.addFailure(i, users.get(i), e.getMessage());
                // 根据业务需求决定是否继续处理
                if (result.getFailureCount() > 10) {
                    throw new BusinessException("批量操作失败次数过多，终止处理");
                }
            }
        }
        
        return result;
    }
    
    private void validateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("用户对象不能为空");
        }
        if (StringUtils.isBlank(user.getName())) {
            throw new IllegalArgumentException("用户姓名不能为空");
        }
        if (StringUtils.isBlank(user.getEmail())) {
            throw new IllegalArgumentException("用户邮箱不能为空");
        }
        if (!EmailValidator.isValid(user.getEmail())) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
    }
}

/**
 * 批量操作结果封装
 */
public class BatchResult {
    private List<BatchItem> successItems = new ArrayList<>();
    private List<BatchItem> failureItems = new ArrayList<>();
    
    public void addSuccess(int index, Object data) {
        successItems.add(new BatchItem(index, data, null));
    }
    
    public void addFailure(int index, Object data, String error) {
        failureItems.add(new BatchItem(index, data, error));
    }
    
    public int getSuccessCount() {
        return successItems.size();
    }
    
    public int getFailureCount() {
        return failureItems.size();
    }
    
    // getters and setters...
    
    public static class BatchItem {
        private int index;
        private Object data;
        private String error;
        
        public BatchItem(int index, Object data, String error) {
            this.index = index;
            this.data = data;
            this.error = error;
        }
        
        // getters and setters...
    }
}
```

### 4. 事务管理

```java
@Service
public class TransactionService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private OrderMapper orderMapper;
    
    /**
     * 声明式事务
     */
    @Transactional(rollbackFor = Exception.class)
    public void createUserWithOrder(User user, Order order) {
        // 创建用户
        userMapper.insert(user);
        
        // 创建订单
        order.setUserId(user.getId());
        orderMapper.insert(order);
        
        // 如果发生异常，事务会自动回滚
    }
    
    /**
     * 编程式事务
     */
    @Autowired
    private TransactionTemplate transactionTemplate;
    
    public void createUserWithOrderProgrammatic(User user, Order order) {
        transactionTemplate.execute(status -> {
            try {
                // 创建用户
                userMapper.insert(user);
                
                // 创建订单
                order.setUserId(user.getId());
                orderMapper.insert(order);
                
                return null;
                
            } catch (Exception e) {
                // 手动回滚
                status.setRollbackOnly();
                throw new RuntimeException("创建用户和订单失败", e);
            }
        });
    }
    
    /**
     * 嵌套事务
     */
    @Transactional(rollbackFor = Exception.class)
    public void outerTransaction(User user) {
        userMapper.insert(user);
        
        try {
            // 调用内部事务
            innerTransaction(user.getId());
        } catch (Exception e) {
            // 内部事务失败，但不影响外部事务
            System.err.println("内部事务失败: " + e.getMessage());
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void innerTransaction(Long userId) {
        // 这是一个新的事务，独立于外部事务
        Order order = new Order();
        order.setUserId(userId);
        orderMapper.insert(order);
    }
}
```

### 5. 日志配置

```yaml
# 日志配置
logging:
  level:
    # MyBatis SQL日志
    com.example.mapper: debug
    # 连接池日志
    com.alibaba.druid: info
    # 事务日志
    org.springframework.transaction: debug
    # 根日志级别
    root: info
  
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{50} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{50} - %msg%n"
  
  file:
    name: logs/mybatis-demo.log
    max-size: 100MB
    max-history: 30
```

## 面试要点

### 高频问题

1. **MyBatis和Hibernate的区别？**
   - MyBatis是半自动ORM，需要手写SQL；Hibernate是全自动ORM
   - MyBatis更灵活，适合复杂查询；Hibernate更简单，适合标准CRUD
   - MyBatis性能更可控；Hibernate功能更丰富

2. **MyBatis的一级缓存和二级缓存？**
   - 一级缓存：SqlSession级别，默认开启，生命周期与SqlSession相同
   - 二级缓存：Mapper级别，需要配置开启，可以跨SqlSession共享

3. **MyBatis的动态SQL有哪些标签？**
   - if、choose/when/otherwise、where、set、foreach、trim、sql

4. **MyBatis的延迟加载原理？**
   - 通过代理对象实现，当访问关联属性时才执行SQL查询
   - 需要配置lazy-loading-enabled=true

5. **MyBatis的插件机制？**
   - 基于JDK动态代理和责任链模式
   - 可以拦截Executor、StatementHandler、ParameterHandler、ResultSetHandler

### 深入问题

1. **MyBatis的SQL执行流程？**
   ```
   SqlSession -> Executor -> StatementHandler -> ParameterHandler -> 数据库
                                                                    ↓
   结果集 <- ResultSetHandler <- StatementHandler <- Executor <- SqlSession
   ```

2. **MyBatis如何防止SQL注入？**
   - 使用#{}参数占位符，会使用PreparedStatement预编译
   - 避免使用${}字符串拼接

3. **MyBatis的批量操作优化？**
   - 使用foreach标签批量插入
   - 使用ExecutorType.BATCH
   - 控制批次大小，避免内存溢出

4. **MyBatis的分页实现方式？**
   - 逻辑分页：查询所有数据，在内存中分页
   - 物理分页：使用LIMIT等数据库分页语句
   - 插件分页：如PageHelper插件

### 实践经验

1. **性能优化经验**
   - 避免N+1查询问题
   - 合理使用缓存
   - 优化SQL语句
   - 使用连接池

2. **常见问题解决**
   - 结果映射问题：检查resultMap配置
   - 参数传递问题：使用@Param注解
   - 事务问题：检查事务配置和异常处理

3. **最佳实践总结**
   - 统一命名规范
   - 合理的项目结构
   - 完善的异常处理
   - 充分的单元测试

## 总结

MyBatis作为一款优秀的持久层框架，具有以下特点：

### 优势
1. **灵活性高**：支持自定义SQL，适合复杂查询
2. **学习成本低**：基于SQL，容易上手
3. **性能可控**：可以精确控制SQL执行
4. **功能丰富**：支持动态SQL、缓存、插件等

### 适用场景
1. 需要复杂查询的业务系统
2. 对性能要求较高的应用
3. 需要与现有数据库系统集成
4. 团队熟悉SQL的项目

### 学习建议
1. **掌握基础**：理解核心概念和基本用法
2. **深入原理**：学习执行流程和底层机制
3. **实践应用**：在项目中积累经验
4. **性能优化**：关注性能问题和优化方案
5. **扩展学习**：了解MyBatis-Plus等扩展框架

MyBatis是Java开发中不可或缺的技术之一，掌握其核心概念和最佳实践，能够帮助开发者构建高效、可维护的数据访问层。