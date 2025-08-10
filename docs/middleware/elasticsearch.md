# Elasticsearch 搜索引擎

## 目录
- [Elasticsearch概述](#elasticsearch概述)
- [核心概念](#核心概念)
- [Spring Boot集成Elasticsearch](#spring-boot集成elasticsearch)
- [基础操作](#基础操作)
- [查询DSL](#查询dsl)
- [聚合分析](#聚合分析)
- [实际应用场景](#实际应用场景)
- [性能优化](#性能优化)
- [集群管理](#集群管理)
- [最佳实践](#最佳实践)
- [面试要点](#面试要点)
- [总结](#总结)

## Elasticsearch概述

### 什么是Elasticsearch
Elasticsearch是一个基于Lucene的分布式、RESTful风格的搜索和数据分析引擎。它能够解决不断涌现出的各种用例，包括全文搜索、结构化搜索、分析等。

### Elasticsearch特点
1. **分布式**：天然支持分布式，可以横向扩展
2. **实时性**：近实时搜索和分析
3. **高可用**：支持副本机制，保证数据安全
4. **RESTful API**：简单易用的HTTP接口
5. **Schema-free**：动态映射，无需预定义结构
6. **多租户**：支持多索引操作

### 应用场景
- 全文搜索引擎
- 日志分析系统
- 实时数据分析
- 推荐系统
- 地理位置搜索
- 安全分析

## 核心概念

### 1. 集群(Cluster)
一个或多个节点组成的集合，共同持有数据并提供联合的索引和搜索功能。

### 2. 节点(Node)
集群中的单个服务器，存储数据并参与集群的索引和搜索功能。

### 3. 索引(Index)
具有相似特征的文档集合，类似于关系数据库中的数据库。

### 4. 类型(Type)
索引中的逻辑分类，在7.x版本后已废弃。

### 5. 文档(Document)
可以被索引的基础信息单元，以JSON格式表示。

### 6. 分片(Shard)
索引的物理分割，每个分片都是一个独立的Lucene索引。

### 7. 副本(Replica)
分片的备份，提供高可用性和搜索性能。

### 8. 映射(Mapping)
定义文档及其字段的存储和索引方式。

## Spring Boot集成Elasticsearch

### 1. 添加依赖

```xml
<dependencies>
    <!-- Spring Boot Starter -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Elasticsearch -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
    </dependency>
    
    <!-- Elasticsearch High Level REST Client -->
    <dependency>
        <groupId>org.elasticsearch.client</groupId>
        <artifactId>elasticsearch-rest-high-level-client</artifactId>
        <version>7.17.9</version>
    </dependency>
    
    <!-- Jackson -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

### 2. 配置文件

```yaml
# application.yml
spring:
  elasticsearch:
    rest:
      uris: http://localhost:9200
      username: elastic
      password: password
      connection-timeout: 5s
      read-timeout: 30s
  
  data:
    elasticsearch:
      repositories:
        enabled: true

# Elasticsearch配置
elasticsearch:
  cluster:
    name: elasticsearch
    nodes: localhost:9300
  index:
    number-of-shards: 1
    number-of-replicas: 0

# 日志配置
logging:
  level:
    org.elasticsearch: DEBUG
    org.springframework.data.elasticsearch: DEBUG
```

### 3. Elasticsearch配置类

```java
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.example.repository")
public class ElasticsearchConfig extends AbstractElasticsearchConfiguration {
    
    @Value("${spring.elasticsearch.rest.uris}")
    private String elasticsearchUrl;
    
    @Override
    @Bean
    public RestHighLevelClient elasticsearchClient() {
        return new RestHighLevelClient(
            RestClient.builder(
                HttpHost.create(elasticsearchUrl)
            ).setRequestConfigCallback(requestConfigBuilder -> 
                requestConfigBuilder
                    .setConnectTimeout(5000)
                    .setSocketTimeout(30000)
            ).setHttpClientConfigCallback(httpClientBuilder -> 
                httpClientBuilder
                    .setMaxConnTotal(100)
                    .setMaxConnPerRoute(100)
            )
        );
    }
}
```

### 4. 实体类定义

```java
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "products", shards = 1, replicas = 0)
@Setting(settingPath = "/elasticsearch/product-settings.json")
public class Product {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String name;
    
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String description;
    
    @Field(type = FieldType.Keyword)
    private String category;
    
    @Field(type = FieldType.Double)
    private Double price;
    
    @Field(type = FieldType.Integer)
    private Integer stock;
    
    @Field(type = FieldType.Keyword)
    private String brand;
    
    @Field(type = FieldType.Keyword)
    private List<String> tags;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime createTime;
    
    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second)
    private LocalDateTime updateTime;
    
    @Field(type = FieldType.Boolean)
    private Boolean available;
    
    @Field(type = FieldType.Object)
    private ProductAttribute attributes;
    
    // 构造函数
    public Product() {}
    
    public Product(String name, String description, String category, Double price) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.available = true;
    }
    
    // getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }
    public ProductAttribute getAttributes() { return attributes; }
    public void setAttributes(ProductAttribute attributes) { this.attributes = attributes; }
    
    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", category='" + category + '\'' +
                ", price=" + price +
                ", stock=" + stock +
                ", brand='" + brand + '\'' +
                ", tags=" + tags +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", available=" + available +
                ", attributes=" + attributes +
                '}';
    }
    
    /**
     * 产品属性内嵌对象
     */
    public static class ProductAttribute {
        private String color;
        private String size;
        private String material;
        private Double weight;
        
        // 构造函数、getters、setters
        public ProductAttribute() {}
        
        public ProductAttribute(String color, String size, String material, Double weight) {
            this.color = color;
            this.size = size;
            this.material = material;
            this.weight = weight;
        }
        
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public String getMaterial() { return material; }
        public void setMaterial(String material) { this.material = material; }
        public Double getWeight() { return weight; }
        public void setWeight(Double weight) { this.weight = weight; }
        
        @Override
        public String toString() {
            return "ProductAttribute{" +
                    "color='" + color + '\'' +
                    ", size='" + size + '\'' +
                    ", material='" + material + '\'' +
                    ", weight=" + weight +
                    '}';
        }
    }
}
```

### 5. Repository接口

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends ElasticsearchRepository<Product, String> {
    
    // 根据名称查询
    List<Product> findByName(String name);
    
    // 根据分类查询
    List<Product> findByCategory(String category);
    
    // 根据品牌查询
    List<Product> findByBrand(String brand);
    
    // 价格范围查询
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);
    
    // 根据可用性查询
    List<Product> findByAvailable(Boolean available);
    
    // 根据标签查询
    List<Product> findByTagsContaining(String tag);
    
    // 复合查询
    List<Product> findByCategoryAndPriceBetween(String category, Double minPrice, Double maxPrice);
    
    // 分页查询
    Page<Product> findByCategory(String category, Pageable pageable);
    
    // 自定义查询
    @Query("{\"bool\": {\"must\": [{\"match\": {\"name\": \"?0\"}}]}}")
    List<Product> findByCustomQuery(String name);
    
    // 模糊查询
    @Query("{\"bool\": {\"should\": [{\"match\": {\"name\": \"?0\"}}, {\"match\": {\"description\": \"?0\"}}]}}")
    List<Product> findByNameOrDescription(String keyword);
    
    // 多字段查询
    @Query("{\"multi_match\": {\"query\": \"?0\", \"fields\": [\"name^2\", \"description\", \"category\"]}}")
    List<Product> findByMultiField(String keyword);
}
```

## 基础操作

### 1. Service层实现

```java
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private RestHighLevelClient elasticsearchClient;
    
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    
    /**
     * 保存产品
     */
    public Product saveProduct(Product product) {
        if (product.getId() == null) {
            product.setCreateTime(LocalDateTime.now());
        }
        product.setUpdateTime(LocalDateTime.now());
        return productRepository.save(product);
    }
    
    /**
     * 批量保存产品
     */
    public Iterable<Product> saveProducts(List<Product> products) {
        products.forEach(product -> {
            if (product.getId() == null) {
                product.setCreateTime(LocalDateTime.now());
            }
            product.setUpdateTime(LocalDateTime.now());
        });
        return productRepository.saveAll(products);
    }
    
    /**
     * 根据ID查询产品
     */
    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }
    
    /**
     * 查询所有产品
     */
    public Iterable<Product> getAllProducts() {
        return productRepository.findAll();
    }
    
    /**
     * 根据分类查询产品
     */
    public List<Product> getProductsByCategory(String category) {
        return productRepository.findByCategory(category);
    }
    
    /**
     * 根据价格范围查询产品
     */
    public List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice) {
        return productRepository.findByPriceBetween(minPrice, maxPrice);
    }
    
    /**
     * 分页查询产品
     */
    public Page<Product> getProductsByPage(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productRepository.findByCategory(category, pageable);
    }
    
    /**
     * 全文搜索
     */
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameOrDescription(keyword);
    }
    
    /**
     * 多字段搜索
     */
    public List<Product> multiFieldSearch(String keyword) {
        return productRepository.findByMultiField(keyword);
    }
    
    /**
     * 更新产品
     */
    public Product updateProduct(Product product) {
        product.setUpdateTime(LocalDateTime.now());
        return productRepository.save(product);
    }
    
    /**
     * 删除产品
     */
    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }
    
    /**
     * 批量删除产品
     */
    public void deleteProducts(List<String> ids) {
        productRepository.deleteAllById(ids);
    }
    
    /**
     * 使用原生客户端查询
     */
    public List<Product> searchWithNativeClient(String keyword) throws IOException {
        SearchRequest searchRequest = new SearchRequest("products");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        
        searchSourceBuilder.query(QueryBuilders.multiMatchQuery(keyword, "name", "description"));
        searchSourceBuilder.size(100);
        
        searchRequest.source(searchSourceBuilder);
        
        SearchResponse searchResponse = elasticsearchClient.search(searchRequest, RequestOptions.DEFAULT);
        
        List<Product> products = new ArrayList<>();
        for (SearchHit hit : searchResponse.getHits().getHits()) {
            // 这里需要手动解析JSON到Product对象
            // 可以使用Jackson或其他JSON库
        }
        
        return products;
    }
    
    /**
     * 复杂查询示例
     */
    public SearchHits<Product> complexSearch(String keyword, String category, Double minPrice, Double maxPrice) {
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
            .withQuery(QueryBuilders.boolQuery()
                .must(QueryBuilders.multiMatchQuery(keyword, "name", "description"))
                .filter(QueryBuilders.termQuery("category", category))
                .filter(QueryBuilders.rangeQuery("price").gte(minPrice).lte(maxPrice))
            )
            .build();
        
        return elasticsearchOperations.search(searchQuery, Product.class);
    }
    
    /**
     * 统计查询
     */
    public long countByCategory(String category) {
        return productRepository.findByCategory(category).size();
    }
    
    /**
     * 检查产品是否存在
     */
    public boolean existsById(String id) {
        return productRepository.existsById(id);
    }
}
```

### 2. Controller层实现

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    /**
     * 创建产品
     */
    @PostMapping
    public ResponseEntity<Product> createProduct(@RequestBody Product product) {
        try {
            Product savedProduct = productService.saveProduct(product);
            return ResponseEntity.ok(savedProduct);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 批量创建产品
     */
    @PostMapping("/batch")
    public ResponseEntity<String> createProducts(@RequestBody List<Product> products) {
        try {
            productService.saveProducts(products);
            return ResponseEntity.ok("成功创建 " + products.size() + " 个产品");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("创建产品失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据ID查询产品
     */
    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable String id) {
        Optional<Product> product = productService.getProductById(id);
        return product.map(ResponseEntity::ok)
                     .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 查询所有产品
     */
    @GetMapping
    public ResponseEntity<Iterable<Product>> getAllProducts() {
        Iterable<Product> products = productService.getAllProducts();
        return ResponseEntity.ok(products);
    }
    
    /**
     * 根据分类查询产品
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<List<Product>> getProductsByCategory(@PathVariable String category) {
        List<Product> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(products);
    }
    
    /**
     * 根据价格范围查询产品
     */
    @GetMapping("/price-range")
    public ResponseEntity<List<Product>> getProductsByPriceRange(
            @RequestParam Double minPrice,
            @RequestParam Double maxPrice) {
        List<Product> products = productService.getProductsByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(products);
    }
    
    /**
     * 分页查询产品
     */
    @GetMapping("/page")
    public ResponseEntity<Page<Product>> getProductsByPage(
            @RequestParam String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<Product> products = productService.getProductsByPage(category, page, size);
        return ResponseEntity.ok(products);
    }
    
    /**
     * 全文搜索
     */
    @GetMapping("/search")
    public ResponseEntity<List<Product>> searchProducts(@RequestParam String keyword) {
        List<Product> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(products);
    }
    
    /**
     * 多字段搜索
     */
    @GetMapping("/multi-search")
    public ResponseEntity<List<Product>> multiFieldSearch(@RequestParam String keyword) {
        List<Product> products = productService.multiFieldSearch(keyword);
        return ResponseEntity.ok(products);
    }
    
    /**
     * 复杂搜索
     */
    @GetMapping("/complex-search")
    public ResponseEntity<SearchHits<Product>> complexSearch(
            @RequestParam String keyword,
            @RequestParam String category,
            @RequestParam Double minPrice,
            @RequestParam Double maxPrice) {
        SearchHits<Product> searchHits = productService.complexSearch(keyword, category, minPrice, maxPrice);
        return ResponseEntity.ok(searchHits);
    }
    
    /**
     * 更新产品
     */
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable String id, @RequestBody Product product) {
        try {
            product.setId(id);
            Product updatedProduct = productService.updateProduct(product);
            return ResponseEntity.ok(updatedProduct);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 删除产品
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable String id) {
        try {
            if (productService.existsById(id)) {
                productService.deleteProduct(id);
                return ResponseEntity.ok("产品删除成功");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除产品失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量删除产品
     */
    @DeleteMapping("/batch")
    public ResponseEntity<String> deleteProducts(@RequestBody List<String> ids) {
        try {
            productService.deleteProducts(ids);
            return ResponseEntity.ok("成功删除 " + ids.size() + " 个产品");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("删除产品失败: " + e.getMessage());
        }
    }
    
    /**
     * 统计分类下的产品数量
     */
    @GetMapping("/count/{category}")
    public ResponseEntity<Long> countByCategory(@PathVariable String category) {
        long count = productService.countByCategory(category);
        return ResponseEntity.ok(count);
    }
}
```