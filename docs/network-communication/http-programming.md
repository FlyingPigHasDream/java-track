# Java HTTP编程详解

## 目录
- [Java HTTP编程详解](#java-http编程详解)
  - [目录](#目录)
  - [HTTP协议基础](#http协议基础)
    - [HTTP协议概述](#http协议概述)
    - [HTTP请求结构](#http请求结构)
    - [HTTP响应结构](#http响应结构)
    - [HTTP方法](#http方法)
    - [HTTP状态码](#http状态码)
  - [Java HTTP客户端](#java-http客户端)
    - [客户端选择对比](#客户端选择对比)
  - [HttpURLConnection详解](#httpurlconnection详解)
    - [基本GET请求](#基本get请求)
    - [高级HttpURLConnection封装](#高级httpurlconnection封装)
  - [Apache HttpClient](#apache-httpclient)
    - [基本使用](#基本使用)
  - [OkHttp客户端](#okhttp客户端)
    - [基本使用](#基本使用-1)
  - [Java 11 HTTP Client](#java-11-http-client)
    - [基本使用](#基本使用-2)
  - [HTTP服务器实现](#http服务器实现)
    - [使用HttpServer（Java内置）](#使用httpserverjava内置)
  - [RESTful API开发](#restful-api开发)
    - [RESTful API服务器实现](#restful-api服务器实现)
  - [HTTP性能优化](#http性能优化)
    - [连接池优化](#连接池优化)
    - [异步HTTP处理](#异步http处理)
  - [安全考虑](#安全考虑)
    - [SSL/TLS配置](#ssltls配置)
    - [请求验证和限流](#请求验证和限流)
  - [最佳实践](#最佳实践)
    - [1. 资源管理](#1-资源管理)
    - [2. 错误处理](#2-错误处理)
    - [3. 性能优化](#3-性能优化)
    - [4. 安全考虑](#4-安全考虑)
  - [面试要点](#面试要点)
    - [高频问题](#高频问题)
    - [深入问题](#深入问题)
    - [实践经验](#实践经验)
  - [总结](#总结)

## HTTP协议基础

### HTTP协议概述
HTTP（HyperText Transfer Protocol）是应用层协议，基于TCP/IP，用于Web浏览器和Web服务器之间的通信。

### HTTP请求结构
```
GET /api/users HTTP/1.1
Host: example.com
User-Agent: Mozilla/5.0
Accept: application/json
Content-Type: application/json
Content-Length: 123

{"name": "John", "age": 30}
```

### HTTP响应结构
```
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: 456
Server: Apache/2.4.41
Date: Mon, 01 Jan 2024 12:00:00 GMT

{"id": 1, "name": "John", "age": 30}
```

### HTTP方法
- **GET**：获取资源
- **POST**：创建资源
- **PUT**：更新资源（完整更新）
- **PATCH**：更新资源（部分更新）
- **DELETE**：删除资源
- **HEAD**：获取响应头
- **OPTIONS**：获取支持的方法

### HTTP状态码
- **1xx**：信息性状态码
- **2xx**：成功状态码（200 OK, 201 Created, 204 No Content）
- **3xx**：重定向状态码（301 Moved Permanently, 302 Found, 304 Not Modified）
- **4xx**：客户端错误（400 Bad Request, 401 Unauthorized, 404 Not Found）
- **5xx**：服务器错误（500 Internal Server Error, 502 Bad Gateway, 503 Service Unavailable）

## Java HTTP客户端

### 客户端选择对比

| 特性 | HttpURLConnection | Apache HttpClient | OkHttp | Java 11 HTTP Client |
|------|-------------------|-------------------|--------|----------------------|
| JDK内置 | ✓ | ✗ | ✗ | ✓ (Java 11+) |
| 异步支持 | ✗ | ✓ | ✓ | ✓ |
| 连接池 | 有限 | ✓ | ✓ | ✓ |
| HTTP/2 | ✗ | ✓ | ✓ | ✓ |
| 易用性 | 低 | 中 | 高 | 高 |
| 性能 | 中 | 高 | 高 | 高 |

## HttpURLConnection详解

### 基本GET请求

```java
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class HttpURLConnectionExample {
    
    public static String sendGetRequest(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // 设置请求方法
            connection.setRequestMethod("GET");
            
            // 设置请求头
            connection.setRequestProperty("User-Agent", "Java HTTP Client");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            
            // 设置超时
            connection.setConnectTimeout(5000); // 5秒连接超时
            connection.setReadTimeout(10000);   // 10秒读取超时
            
            // 获取响应码
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            // 读取响应
            InputStream inputStream = (responseCode >= 200 && responseCode < 300) 
                ? connection.getInputStream() 
                : connection.getErrorStream();
            
            return readInputStream(inputStream);
            
        } finally {
            connection.disconnect();
        }
    }
    
    public static String sendPostRequest(String urlString, String jsonData) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // 设置请求方法
            connection.setRequestMethod("POST");
            
            // 设置请求头
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty("User-Agent", "Java HTTP Client");
            
            // 启用输出
            connection.setDoOutput(true);
            
            // 设置超时
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(10000);
            
            // 发送请求体
            try (OutputStream outputStream = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                outputStream.write(input, 0, input.length);
            }
            
            // 获取响应
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);
            
            InputStream inputStream = (responseCode >= 200 && responseCode < 300) 
                ? connection.getInputStream() 
                : connection.getErrorStream();
            
            return readInputStream(inputStream);
            
        } finally {
            connection.disconnect();
        }
    }
    
    private static String readInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            
            return response.toString();
        }
    }
    
    // 文件上传示例
    public static String uploadFile(String urlString, File file) throws IOException {
        String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
        
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000); // 文件上传需要更长超时
            
            try (OutputStream outputStream = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(
                     new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)) {
                
                // 写入文件部分
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                      .append(file.getName()).append("\"").append("\r\n");
                writer.append("Content-Type: application/octet-stream\r\n");
                writer.append("\r\n");
                writer.flush();
                
                // 写入文件内容
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                
                outputStream.flush();
                writer.append("\r\n");
                writer.append("--").append(boundary).append("--\r\n");
                writer.flush();
            }
            
            int responseCode = connection.getResponseCode();
            System.out.println("Upload Response Code: " + responseCode);
            
            InputStream inputStream = (responseCode >= 200 && responseCode < 300) 
                ? connection.getInputStream() 
                : connection.getErrorStream();
            
            return readInputStream(inputStream);
            
        } finally {
            connection.disconnect();
        }
    }
    
    // 下载文件示例
    public static void downloadFile(String urlString, String outputPath) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(30000);
            
            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("Failed to download file: " + responseCode);
            }
            
            long contentLength = connection.getContentLengthLong();
            System.out.println("File size: " + contentLength + " bytes");
            
            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(outputPath)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;
                
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    if (contentLength > 0) {
                        int progress = (int) ((totalBytesRead * 100) / contentLength);
                        System.out.print("\rDownload progress: " + progress + "%");
                    }
                }
                
                System.out.println("\nDownload completed: " + outputPath);
            }
            
        } finally {
            connection.disconnect();
        }
    }
    
    public static void main(String[] args) {
        try {
            // GET请求示例
            String getResponse = sendGetRequest("https://jsonplaceholder.typicode.com/posts/1");
            System.out.println("GET Response:\n" + getResponse);
            
            // POST请求示例
            String jsonData = "{\"title\": \"foo\", \"body\": \"bar\", \"userId\": 1}";
            String postResponse = sendPostRequest("https://jsonplaceholder.typicode.com/posts", jsonData);
            System.out.println("POST Response:\n" + postResponse);
            
        } catch (IOException e) {
            System.err.println("HTTP request failed: " + e.getMessage());
        }
    }
}
```

### 高级HttpURLConnection封装

```java
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class AdvancedHttpClient {
    private final int connectTimeout;
    private final int readTimeout;
    private final Map<String, String> defaultHeaders;
    private final Executor executor;
    
    public AdvancedHttpClient() {
        this(5000, 10000);
    }
    
    public AdvancedHttpClient(int connectTimeout, int readTimeout) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.defaultHeaders = new HashMap<>();
        this.executor = Executors.newCachedThreadPool();
        
        // 设置默认请求头
        defaultHeaders.put("User-Agent", "Advanced Java HTTP Client/1.0");
        defaultHeaders.put("Accept", "application/json");
        defaultHeaders.put("Accept-Charset", "UTF-8");
    }
    
    public static class HttpRequest {
        private final String url;
        private final String method;
        private final Map<String, String> headers;
        private final String body;
        
        private HttpRequest(Builder builder) {
            this.url = builder.url;
            this.method = builder.method;
            this.headers = new HashMap<>(builder.headers);
            this.body = builder.body;
        }
        
        public static class Builder {
            private String url;
            private String method = "GET";
            private Map<String, String> headers = new HashMap<>();
            private String body;
            
            public Builder url(String url) {
                this.url = url;
                return this;
            }
            
            public Builder method(String method) {
                this.method = method;
                return this;
            }
            
            public Builder header(String name, String value) {
                this.headers.put(name, value);
                return this;
            }
            
            public Builder headers(Map<String, String> headers) {
                this.headers.putAll(headers);
                return this;
            }
            
            public Builder body(String body) {
                this.body = body;
                return this;
            }
            
            public Builder json(String json) {
                this.body = json;
                this.headers.put("Content-Type", "application/json");
                return this;
            }
            
            public HttpRequest build() {
                if (url == null) {
                    throw new IllegalArgumentException("URL is required");
                }
                return new HttpRequest(this);
            }
        }
        
        // Getter methods
        public String getUrl() { return url; }
        public String getMethod() { return method; }
        public Map<String, String> getHeaders() { return headers; }
        public String getBody() { return body; }
    }
    
    public static class HttpResponse {
        private final int statusCode;
        private final String statusMessage;
        private final Map<String, List<String>> headers;
        private final String body;
        
        public HttpResponse(int statusCode, String statusMessage, 
                          Map<String, List<String>> headers, String body) {
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
            this.headers = headers;
            this.body = body;
        }
        
        public int getStatusCode() { return statusCode; }
        public String getStatusMessage() { return statusMessage; }
        public Map<String, List<String>> getHeaders() { return headers; }
        public String getBody() { return body; }
        
        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }
        
        public String getHeader(String name) {
            List<String> values = headers.get(name);
            return values != null && !values.isEmpty() ? values.get(0) : null;
        }
    }
    
    public HttpResponse execute(HttpRequest request) throws IOException {
        URL url = new URL(request.getUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            // 设置请求方法
            connection.setRequestMethod(request.getMethod());
            
            // 设置超时
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            
            // 设置默认请求头
            for (Map.Entry<String, String> entry : defaultHeaders.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            
            // 设置请求头
            for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }
            
            // 发送请求体
            if (request.getBody() != null && !request.getBody().isEmpty()) {
                connection.setDoOutput(true);
                try (OutputStream outputStream = connection.getOutputStream()) {
                    byte[] input = request.getBody().getBytes(StandardCharsets.UTF_8);
                    outputStream.write(input);
                }
            }
            
            // 获取响应
            int statusCode = connection.getResponseCode();
            String statusMessage = connection.getResponseMessage();
            Map<String, List<String>> headers = connection.getHeaderFields();
            
            InputStream inputStream = (statusCode >= 200 && statusCode < 300) 
                ? connection.getInputStream() 
                : connection.getErrorStream();
            
            String body = readInputStream(inputStream);
            
            return new HttpResponse(statusCode, statusMessage, headers, body);
            
        } finally {
            connection.disconnect();
        }
    }
    
    public CompletableFuture<HttpResponse> executeAsync(HttpRequest request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return execute(request);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }
    
    private String readInputStream(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return "";
        }
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            
            StringBuilder response = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
            
            return response.toString();
        }
    }
    
    // 便捷方法
    public HttpResponse get(String url) throws IOException {
        HttpRequest request = new HttpRequest.Builder()
            .url(url)
            .method("GET")
            .build();
        return execute(request);
    }
    
    public HttpResponse post(String url, String json) throws IOException {
        HttpRequest request = new HttpRequest.Builder()
            .url(url)
            .method("POST")
            .json(json)
            .build();
        return execute(request);
    }
    
    public HttpResponse put(String url, String json) throws IOException {
        HttpRequest request = new HttpRequest.Builder()
            .url(url)
            .method("PUT")
            .json(json)
            .build();
        return execute(request);
    }
    
    public HttpResponse delete(String url) throws IOException {
        HttpRequest request = new HttpRequest.Builder()
            .url(url)
            .method("DELETE")
            .build();
        return execute(request);
    }
    
    public void close() {
        if (executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }
    
    // 使用示例
    public static void main(String[] args) {
        AdvancedHttpClient client = new AdvancedHttpClient();
        
        try {
            // 简单GET请求
            HttpResponse response = client.get("https://jsonplaceholder.typicode.com/posts/1");
            System.out.println("Status: " + response.getStatusCode());
            System.out.println("Body: " + response.getBody());
            
            // 复杂POST请求
            HttpRequest request = new HttpRequest.Builder()
                .url("https://jsonplaceholder.typicode.com/posts")
                .method("POST")
                .header("Authorization", "Bearer token123")
                .json("{\"title\": \"foo\", \"body\": \"bar\", \"userId\": 1}")
                .build();
            
            HttpResponse postResponse = client.execute(request);
            System.out.println("POST Status: " + postResponse.getStatusCode());
            System.out.println("POST Body: " + postResponse.getBody());
            
            // 异步请求
            CompletableFuture<HttpResponse> futureResponse = client.executeAsync(request);
            futureResponse.thenAccept(resp -> {
                System.out.println("Async response: " + resp.getStatusCode());
            }).exceptionally(throwable -> {
                System.err.println("Async request failed: " + throwable.getMessage());
                return null;
            });
            
        } catch (IOException e) {
            System.err.println("Request failed: " + e.getMessage());
        } finally {
            client.close();
        }
    }
}
```

## Apache HttpClient

### 基本使用

```java
// 需要添加依赖
// <dependency>
//     <groupId>org.apache.httpcomponents</groupId>
//     <artifactId>httpclient</artifactId>
//     <version>4.5.14</version>
// </dependency>

import org.apache.http.*;
import org.apache.http.client.*;
import org.apache.http.client.config.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.*;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.*;
import org.apache.http.util.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class ApacheHttpClientExample {
    
    public static void basicExample() throws IOException {
        // 创建HttpClient
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            
            // GET请求
            HttpGet httpGet = new HttpGet("https://jsonplaceholder.typicode.com/posts/1");
            httpGet.setHeader("Accept", "application/json");
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                System.out.println("Status: " + response.getStatusLine());
                
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity, StandardCharsets.UTF_8);
                    System.out.println("Response: " + result);
                }
            }
            
            // POST请求
            HttpPost httpPost = new HttpPost("https://jsonplaceholder.typicode.com/posts");
            httpPost.setHeader("Content-Type", "application/json");
            
            String json = "{\"title\": \"foo\", \"body\": \"bar\", \"userId\": 1}";
            StringEntity entity = new StringEntity(json, StandardCharsets.UTF_8);
            httpPost.setEntity(entity);
            
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                System.out.println("POST Status: " + response.getStatusLine());
                
                HttpEntity responseEntity = response.getEntity();
                if (responseEntity != null) {
                    String result = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);
                    System.out.println("POST Response: " + result);
                }
            }
        }
    }
    
    public static void advancedExample() throws IOException {
        // 配置请求配置
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(5000)
            .setSocketTimeout(10000)
            .setConnectionRequestTimeout(3000)
            .build();
        
        // 配置连接池
        PoolingHttpClientConnectionManager connectionManager = 
            new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(100);
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.closeExpiredConnections();
        connectionManager.closeIdleConnections(30, TimeUnit.SECONDS);
        
        // 创建自定义HttpClient
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
                .build()) {
            
            // 执行请求
            HttpGet httpGet = new HttpGet("https://httpbin.org/delay/2");
            
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                System.out.println("Status: " + response.getStatusLine());
                
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    String result = EntityUtils.toString(entity);
                    System.out.println("Response: " + result);
                }
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            basicExample();
            System.out.println("\n" + "=".repeat(50) + "\n");
            advancedExample();
        } catch (IOException e) {
            System.err.println("HTTP request failed: " + e.getMessage());
        }
    }
}
```

## OkHttp客户端

### 基本使用

```java
// 需要添加依赖
// <dependency>
//     <groupId>com.squareup.okhttp3</groupId>
//     <artifactId>okhttp</artifactId>
//     <version>4.12.0</version>
// </dependency>

import okhttp3.*;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OkHttpExample {
    
    public static void basicExample() throws IOException {
        OkHttpClient client = new OkHttpClient();
        
        // GET请求
        Request request = new Request.Builder()
            .url("https://jsonplaceholder.typicode.com/posts/1")
            .addHeader("Accept", "application/json")
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            System.out.println("Status: " + response.code());
            System.out.println("Response: " + response.body().string());
        }
        
        // POST请求
        String json = "{\"title\": \"foo\", \"body\": \"bar\", \"userId\": 1}";
        RequestBody body = RequestBody.create(json, MediaType.get("application/json"));
        
        Request postRequest = new Request.Builder()
            .url("https://jsonplaceholder.typicode.com/posts")
            .post(body)
            .build();
        
        try (Response response = client.newCall(postRequest).execute()) {
            System.out.println("POST Status: " + response.code());
            System.out.println("POST Response: " + response.body().string());
        }
    }
    
    public static void advancedExample() throws IOException {
        // 自定义OkHttpClient
        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(new LoggingInterceptor())
            .build();
        
        Request request = new Request.Builder()
            .url("https://httpbin.org/delay/2")
            .build();
        
        try (Response response = client.newCall(request).execute()) {
            System.out.println("Status: " + response.code());
            System.out.println("Headers: " + response.headers());
            System.out.println("Response: " + response.body().string());
        }
    }
    
    // 自定义拦截器
    static class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            
            long startTime = System.nanoTime();
            System.out.println("Sending request: " + request.url());
            
            Response response = chain.proceed(request);
            
            long endTime = System.nanoTime();
            System.out.println("Received response in " + 
                             (endTime - startTime) / 1e6d + "ms");
            
            return response;
        }
    }
    
    public static void asyncExample() {
        OkHttpClient client = new OkHttpClient();
        
        Request request = new Request.Builder()
            .url("https://jsonplaceholder.typicode.com/posts/1")
            .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                System.err.println("Request failed: " + e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    System.out.println("Async Status: " + response.code());
                    System.out.println("Async Response: " + responseBody.string());
                }
            }
        });
        
        // 等待异步请求完成
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public static void main(String[] args) {
        try {
            basicExample();
            System.out.println("\n" + "=".repeat(50) + "\n");
            advancedExample();
            System.out.println("\n" + "=".repeat(50) + "\n");
            asyncExample();
        } catch (IOException e) {
            System.err.println("HTTP request failed: " + e.getMessage());
        }
    }
}
```

## Java 11 HTTP Client

### 基本使用

```java
// Java 11+
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class Java11HttpClientExample {
    
    public static void basicExample() throws Exception {
        // 创建HttpClient
        HttpClient client = HttpClient.newHttpClient();
        
        // GET请求
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://jsonplaceholder.typicode.com/posts/1"))
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(10))
            .build();
        
        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Status: " + response.statusCode());
        System.out.println("Headers: " + response.headers().map());
        System.out.println("Response: " + response.body());
        
        // POST请求
        String json = "{\"title\": \"foo\", \"body\": \"bar\", \"userId\": 1}";
        
        HttpRequest postRequest = HttpRequest.newBuilder()
            .uri(URI.create("https://jsonplaceholder.typicode.com/posts"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        
        HttpResponse<String> postResponse = client.send(postRequest, 
            HttpResponse.BodyHandlers.ofString());
        
        System.out.println("POST Status: " + postResponse.statusCode());
        System.out.println("POST Response: " + postResponse.body());
    }
    
    public static void advancedExample() throws Exception {
        // 自定义HttpClient
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://httpbin.org/delay/2"))
            .timeout(Duration.ofSeconds(30))
            .build();
        
        HttpResponse<String> response = client.send(request, 
            HttpResponse.BodyHandlers.ofString());
        
        System.out.println("Advanced Status: " + response.statusCode());
        System.out.println("Advanced Response: " + response.body());
    }
    
    public static void asyncExample() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://jsonplaceholder.typicode.com/posts/1"))
            .build();
        
        // 异步请求
        CompletableFuture<HttpResponse<String>> futureResponse = 
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        
        futureResponse.thenAccept(response -> {
            System.out.println("Async Status: " + response.statusCode());
            System.out.println("Async Response: " + response.body());
        }).exceptionally(throwable -> {
            System.err.println("Async request failed: " + throwable.getMessage());
            return null;
        });
        
        // 等待异步请求完成
        Thread.sleep(3000);
    }
    
    public static void fileDownloadExample() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://httpbin.org/json"))
            .build();
        
        // 下载到文件
        HttpResponse<Path> response = client.send(request, 
            HttpResponse.BodyHandlers.ofFile(Paths.get("downloaded.json")));
        
        System.out.println("File downloaded: " + response.body());
        System.out.println("Status: " + response.statusCode());
    }
    
    public static void main(String[] args) {
        try {
            basicExample();
            System.out.println("\n" + "=".repeat(50) + "\n");
            advancedExample();
            System.out.println("\n" + "=".repeat(50) + "\n");
            asyncExample();
            System.out.println("\n" + "=".repeat(50) + "\n");
            fileDownloadExample();
        } catch (Exception e) {
            System.err.println("HTTP request failed: " + e.getMessage());
        }
    }
}
```

## HTTP服务器实现

### 使用HttpServer（Java内置）

```java
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class SimpleHttpServer {
    public static void main(String[] args) throws IOException {
        // 创建HTTP服务器，监听8080端口
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // 添加处理器
        server.createContext("/hello", new HelloHandler());
        server.createContext("/api/users", new UserHandler());
        
        // 设置线程池
        server.setExecutor(null); // 使用默认线程池
        
        // 启动服务器
        server.start();
        System.out.println("服务器启动在端口8080");
    }
    
    static class HelloHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            
            if ("GET".equals(method)) {
                handleGet(exchange);
            } else if ("POST".equals(method)) {
                handlePost(exchange);
            } else {
                // 方法不支持
                exchange.sendResponseHeaders(405, -1);
            }
        }
        
        private void handleGet(HttpExchange exchange) throws IOException {
            String response = "Hello, World!";
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        private void handlePost(HttpExchange exchange) throws IOException {
            // 读取请求体
            String requestBody = readRequestBody(exchange);
            
            String response = "收到POST数据: " + requestBody;
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        private String readRequestBody(HttpExchange exchange) throws IOException {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        }
    }
}
```

## RESTful API开发

### RESTful API服务器实现

```java
import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RestApiServer {
    private static final Map<String, User> users = new ConcurrentHashMap<>();
    
    static {
        users.put("1", new User("1", "张三", "zhang@example.com"));
        users.put("2", new User("2", "李四", "li@example.com"));
    }
    
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/users", new UserHandler());
        server.start();
        System.out.println("REST API服务器启动在端口8080");
    }
    
    static class UserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            
            try {
                switch (method) {
                    case "GET":
                        handleGet(exchange, path);
                        break;
                    case "POST":
                        handlePost(exchange);
                        break;
                    case "PUT":
                        handlePut(exchange, path);
                        break;
                    case "DELETE":
                        handleDelete(exchange, path);
                        break;
                    default:
                        sendResponse(exchange, 405, "Method Not Allowed");
                }
            } catch (Exception e) {
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        }
        
        private void handleGet(HttpExchange exchange, String path) throws IOException {
            if (path.equals("/api/users")) {
                // 获取所有用户
                String json = usersToJson(new ArrayList<>(users.values()));
                sendJsonResponse(exchange, 200, json);
            } else if (path.startsWith("/api/users/")) {
                // 获取特定用户
                String userId = path.substring("/api/users/".length());
                User user = users.get(userId);
                if (user != null) {
                    String json = userToJson(user);
                    sendJsonResponse(exchange, 200, json);
                } else {
                    sendResponse(exchange, 404, "User not found");
                }
            }
        }
        
        private void handlePost(HttpExchange exchange) throws IOException {
            String requestBody = readRequestBody(exchange);
            User user = jsonToUser(requestBody);
            
            if (user.getId() == null || user.getId().isEmpty()) {
                user.setId(UUID.randomUUID().toString());
            }
            
            users.put(user.getId(), user);
            String json = userToJson(user);
            sendJsonResponse(exchange, 201, json);
        }
        
        private void handlePut(HttpExchange exchange, String path) throws IOException {
            if (path.startsWith("/api/users/")) {
                String userId = path.substring("/api/users/".length());
                if (users.containsKey(userId)) {
                    String requestBody = readRequestBody(exchange);
                    User user = jsonToUser(requestBody);
                    user.setId(userId);
                    users.put(userId, user);
                    String json = userToJson(user);
                    sendJsonResponse(exchange, 200, json);
                } else {
                    sendResponse(exchange, 404, "User not found");
                }
            }
        }
        
        private void handleDelete(HttpExchange exchange, String path) throws IOException {
            if (path.startsWith("/api/users/")) {
                String userId = path.substring("/api/users/".length());
                if (users.remove(userId) != null) {
                    sendResponse(exchange, 204, "");
                } else {
                    sendResponse(exchange, 404, "User not found");
                }
            }
        }
        
        private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(statusCode, json.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
            exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        private String readRequestBody(HttpExchange exchange) throws IOException {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        }
        
        // 简单的JSON序列化（实际项目中建议使用Jackson或Gson）
        private String userToJson(User user) {
            return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"email\":\"%s\"}",
                user.getId(), user.getName(), user.getEmail()
            );
        }
        
        private String usersToJson(List<User> userList) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < userList.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(userToJson(userList.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        
        private User jsonToUser(String json) {
            // 简单的JSON解析（实际项目中建议使用Jackson或Gson）
            User user = new User();
            // 这里简化处理，实际应该用正确的JSON解析
            return user;
        }
    }
    
    static class User {
        private String id;
        private String name;
        private String email;
        
        public User() {}
        
        public User(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
        
        // getter和setter方法
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
```

## HTTP性能优化

### 连接池优化

```java
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.client.config.RequestConfig;
import java.util.concurrent.TimeUnit;

public class HttpClientPool {
    private static CloseableHttpClient httpClient;
    
    static {
        // 创建连接池管理器
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200); // 最大连接数
        cm.setDefaultMaxPerRoute(20); // 每个路由的最大连接数
        cm.closeExpiredConnections();
        cm.closeIdleConnections(30, TimeUnit.SECONDS);
        
        // 请求配置
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000) // 连接超时
                .setSocketTimeout(10000) // 读取超时
                .setConnectionRequestTimeout(3000) // 从连接池获取连接超时
                .build();
        
        // 创建HttpClient
        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
    
    public static CloseableHttpClient getHttpClient() {
        return httpClient;
    }
    
    // 应用关闭时调用
    public static void shutdown() {
        try {
            if (httpClient != null) {
                httpClient.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 异步HTTP处理

```java
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;

public class AsyncHttpExample {
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    
    public static void main(String[] args) {
        // 并发发送多个请求
        List<String> urls = List.of(
            "https://api.github.com/users/octocat",
            "https://api.github.com/users/defunkt",
            "https://api.github.com/users/pjhyett"
        );
        
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        for (String url : urls) {
            CompletableFuture<String> future = sendAsyncRequest(url);
            futures.add(future);
        }
        
        // 等待所有请求完成
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        allFutures.thenRun(() -> {
            System.out.println("所有请求完成");
            futures.forEach(future -> {
                try {
                    System.out.println("响应: " + future.get());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).join();
    }
    
    private static CompletableFuture<String> sendAsyncRequest(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "Java HTTP Client")
                .GET()
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .exceptionally(throwable -> {
                    System.err.println("请求失败: " + url + ", 错误: " + throwable.getMessage());
                    return "请求失败";
                });
    }
}
```

## 安全考虑

### SSL/TLS配置

```java
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.net.http.HttpClient;
import java.time.Duration;

public class HttpsClientExample {
    
    public static HttpClient createSecureClient() {
        try {
            // 创建信任所有证书的TrustManager（仅用于测试）
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
                    
        } catch (Exception e) {
            throw new RuntimeException("Failed to create HTTPS client", e);
        }
    }
    
    // 生产环境推荐的安全配置
    public static HttpClient createProductionClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                // 使用默认的SSL配置，会验证证书
                .build();
    }
}
```

### 请求验证和限流

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class RateLimiter {
    private final ConcurrentHashMap<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    private final int maxRequestsPerMinute;
    
    public RateLimiter(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }
    
    public boolean isAllowed(String clientId) {
        LocalDateTime now = LocalDateTime.now();
        RequestCounter counter = requestCounts.computeIfAbsent(clientId, 
            k -> new RequestCounter(now));
        
        // 如果超过1分钟，重置计数器
        if (ChronoUnit.MINUTES.between(counter.startTime, now) >= 1) {
            counter.reset(now);
        }
        
        return counter.increment() <= maxRequestsPerMinute;
    }
    
    private static class RequestCounter {
        private AtomicInteger count = new AtomicInteger(0);
        private LocalDateTime startTime;
        
        public RequestCounter(LocalDateTime startTime) {
            this.startTime = startTime;
        }
        
        public int increment() {
            return count.incrementAndGet();
        }
        
        public void reset(LocalDateTime newStartTime) {
            this.count.set(0);
            this.startTime = newStartTime;
        }
    }
}
```

## 最佳实践

### 1. 资源管理
- 使用try-with-resources确保资源正确关闭
- 合理配置连接池大小
- 设置合适的超时时间

### 2. 错误处理
- 区分不同类型的HTTP错误
- 实现重试机制
- 记录详细的错误日志

### 3. 性能优化
- 使用连接池避免频繁创建连接
- 启用HTTP/2支持
- 合理使用缓存
- 压缩请求和响应数据

### 4. 安全考虑
- 验证SSL证书
- 使用HTTPS传输敏感数据
- 防止SSRF攻击
- 限制请求大小和频率

## 面试要点

### 高频问题
1. **HTTP协议特点**
   - 无状态、请求-响应模式
   - 支持多种方法（GET、POST、PUT、DELETE等）
   - 状态码含义

2. **Java HTTP客户端对比**
   - HttpURLConnection：轻量级，JDK内置
   - Apache HttpClient：功能丰富，企业级
   - OkHttp：现代化，性能优秀
   - Java 11 HTTP Client：官方推荐，支持HTTP/2

3. **连接池的作用**
   - 减少连接创建开销
   - 控制并发连接数
   - 提高性能和资源利用率

### 深入问题
1. **HTTP/1.1 vs HTTP/2**
   - 多路复用、服务器推送、头部压缩
   - 二进制协议、流控制

2. **HTTPS握手过程**
   - SSL/TLS协议
   - 证书验证
   - 密钥交换

3. **HTTP缓存机制**
   - 强缓存：Expires、Cache-Control
   - 协商缓存：Last-Modified、ETag

### 实践经验
1. **如何处理大文件上传下载**
   - 分块传输
   - 断点续传
   - 进度监控

2. **如何实现HTTP重试机制**
   - 指数退避算法
   - 幂等性考虑
   - 最大重试次数

3. **如何监控HTTP性能**
   - 响应时间统计
   - 错误率监控
   - 连接池状态监控

## 总结

Java HTTP编程提供了多种客户端选择，从简单的HttpURLConnection到功能强大的第三方库。选择合适的HTTP客户端需要考虑项目需求、性能要求和团队技术栈。

对于简单的HTTP请求，HttpURLConnection足够使用；对于复杂的企业级应用，推荐使用Apache HttpClient或OkHttp；对于Java 11+的项目，新的HTTP Client API是很好的选择。

在实际开发中，还需要注意资源管理、错误处理、性能优化和安全考虑等方面，确保HTTP通信的稳定性和安全性。
```