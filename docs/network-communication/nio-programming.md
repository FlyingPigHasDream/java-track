# Java NIO编程详解

## 目录
- [NIO概述](#nio概述)
- [核心组件](#核心组件)
- [Channel详解](#channel详解)
- [Buffer详解](#buffer详解)
- [Selector详解](#selector详解)
- [NIO编程实例](#nio编程实例)
- [NIO vs BIO对比](#nio-vs-bio对比)
- [性能优化](#性能优化)
- [最佳实践](#最佳实践)
- [面试要点](#面试要点)

## NIO概述

### 什么是NIO
NIO (New I/O 或 Non-blocking I/O) 是Java 1.4引入的新的I/O API，提供了与传统I/O不同的工作方式：
- **非阻塞I/O**：线程不会被I/O操作阻塞
- **面向缓冲区**：数据读取到缓冲区中，可以在缓冲区中前后移动
- **选择器**：单个线程可以监控多个输入通道

### NIO的优势
1. **高并发**：一个线程可以处理多个连接
2. **内存效率**：使用直接内存，减少数据拷贝
3. **可扩展性**：适合处理大量连接的场景
4. **灵活性**：提供了更多的控制选项

### 适用场景
- 高并发服务器
- 聊天服务器
- 游戏服务器
- 代理服务器
- 需要处理大量连接的应用

## 核心组件

NIO的核心组件包括：
- **Channel（通道）**：数据传输的管道
- **Buffer（缓冲区）**：数据容器
- **Selector（选择器）**：多路复用器

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Channel   │◄──►│   Buffer    │    │  Selector   │
│   (通道)     │    │   (缓冲区)   │    │  (选择器)    │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       │                   │                   │
   数据传输通道          数据存储容器         多路复用监控
```

## Channel详解

### Channel概念
Channel类似于传统I/O中的流，但有以下区别：
- Channel是双向的，可以读也可以写
- Channel可以异步读写
- Channel总是从Buffer读取数据或写入数据

### 主要Channel类型

#### 1. FileChannel
文件操作通道，用于文件读写操作。

```java
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class FileChannelExample {
    public static void main(String[] args) {
        try {
            // 读取文件
            RandomAccessFile file = new RandomAccessFile("data.txt", "rw");
            FileChannel channel = file.getChannel();
            
            // 创建缓冲区
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            
            // 从通道读取数据到缓冲区
            int bytesRead = channel.read(buffer);
            while (bytesRead != -1) {
                buffer.flip(); // 切换到读模式
                
                while (buffer.hasRemaining()) {
                    System.out.print((char) buffer.get());
                }
                
                buffer.clear(); // 清空缓冲区
                bytesRead = channel.read(buffer);
            }
            
            file.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 2. SocketChannel
TCP Socket通道，用于TCP网络通信。

```java
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketChannelExample {
    public static void main(String[] args) {
        try {
            // 创建SocketChannel
            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress("localhost", 8080));
            
            // 发送数据
            String message = "Hello NIO Server";
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            buffer.put(message.getBytes());
            buffer.flip();
            
            socketChannel.write(buffer);
            
            // 读取响应
            buffer.clear();
            int bytesRead = socketChannel.read(buffer);
            if (bytesRead > 0) {
                buffer.flip();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                System.out.println("服务器响应: " + new String(data));
            }
            
            socketChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 3. ServerSocketChannel
TCP服务器Socket通道，用于监听TCP连接。

```java
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class ServerSocketChannelExample {
    public static void main(String[] args) {
        try {
            // 创建ServerSocketChannel
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(8080));
            serverChannel.configureBlocking(false); // 设置为非阻塞模式
            
            System.out.println("服务器启动，监听端口8080");
            
            while (true) {
                SocketChannel clientChannel = serverChannel.accept();
                if (clientChannel != null) {
                    handleClient(clientChannel);
                }
                
                // 避免CPU空转
                Thread.sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void handleClient(SocketChannel clientChannel) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            int bytesRead = clientChannel.read(buffer);
            
            if (bytesRead > 0) {
                buffer.flip();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                String message = new String(data);
                System.out.println("收到客户端消息: " + message);
                
                // 回复客户端
                String response = "Echo: " + message;
                ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
                clientChannel.write(responseBuffer);
            }
            
            clientChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

#### 4. DatagramChannel
UDP通道，用于UDP网络通信。

```java
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class DatagramChannelExample {
    public static void main(String[] args) {
        try {
            // 创建DatagramChannel
            DatagramChannel channel = DatagramChannel.open();
            channel.bind(new InetSocketAddress(8080));
            
            System.out.println("UDP服务器启动，监听端口8080");
            
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            
            while (true) {
                buffer.clear();
                InetSocketAddress clientAddress = (InetSocketAddress) channel.receive(buffer);
                
                if (clientAddress != null) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    String message = new String(data);
                    System.out.println("收到来自 " + clientAddress + " 的消息: " + message);
                    
                    // 回复客户端
                    String response = "Echo: " + message;
                    ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
                    channel.send(responseBuffer, clientAddress);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

## Buffer详解

### Buffer概念
Buffer是一个对象，包含一些要写入或读出的数据。在NIO中，所有数据都是用Buffer处理的。

### Buffer的重要属性
- **capacity**：缓冲区容量，创建后不能改变
- **limit**：缓冲区限制，表示可以操作数据的边界
- **position**：当前位置，下一个要读写的元素索引
- **mark**：标记位置，可以通过reset()方法回到这个位置

```
关系：0 <= mark <= position <= limit <= capacity
```

### Buffer的主要方法

```java
import java.nio.ByteBuffer;

public class BufferExample {
    public static void main(String[] args) {
        // 创建缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(10);
        System.out.println("初始状态: " + bufferStatus(buffer));
        
        // 写入数据
        buffer.put("Hello".getBytes());
        System.out.println("写入数据后: " + bufferStatus(buffer));
        
        // 切换到读模式
        buffer.flip();
        System.out.println("flip后: " + bufferStatus(buffer));
        
        // 读取数据
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        System.out.println("读取的数据: " + new String(data));
        System.out.println("读取数据后: " + bufferStatus(buffer));
        
        // 清空缓冲区
        buffer.clear();
        System.out.println("clear后: " + bufferStatus(buffer));
        
        // 重新写入数据
        buffer.put("World".getBytes());
        System.out.println("重新写入后: " + bufferStatus(buffer));
        
        // 压缩缓冲区
        buffer.compact();
        System.out.println("compact后: " + bufferStatus(buffer));
    }
    
    private static String bufferStatus(ByteBuffer buffer) {
        return String.format("position=%d, limit=%d, capacity=%d", 
                           buffer.position(), buffer.limit(), buffer.capacity());
    }
}
```

### 直接缓冲区 vs 非直接缓冲区

```java
import java.nio.ByteBuffer;

public class DirectBufferExample {
    public static void main(String[] args) {
        // 非直接缓冲区（堆内存）
        ByteBuffer heapBuffer = ByteBuffer.allocate(1024);
        System.out.println("堆缓冲区是否为直接缓冲区: " + heapBuffer.isDirect());
        
        // 直接缓冲区（堆外内存）
        ByteBuffer directBuffer = ByteBuffer.allocateDirect(1024);
        System.out.println("直接缓冲区是否为直接缓冲区: " + directBuffer.isDirect());
        
        // 性能测试
        long startTime, endTime;
        int iterations = 1000000;
        
        // 测试堆缓冲区性能
        startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            heapBuffer.putInt(0, i);
        }
        endTime = System.currentTimeMillis();
        System.out.println("堆缓冲区耗时: " + (endTime - startTime) + "ms");
        
        // 测试直接缓冲区性能
        startTime = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            directBuffer.putInt(0, i);
        }
        endTime = System.currentTimeMillis();
        System.out.println("直接缓冲区耗时: " + (endTime - startTime) + "ms");
    }
}
```

## Selector详解

### Selector概念
Selector是NIO的核心组件，它可以监控多个Channel的状态，实现单线程管理多个网络连接。

### SelectionKey
SelectionKey表示Selector和Channel之间的注册关系，包含以下操作类型：
- **OP_READ**：读操作
- **OP_WRITE**：写操作
- **OP_CONNECT**：连接操作
- **OP_ACCEPT**：接受连接操作

### Selector使用示例

```java
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class SelectorExample {
    public static void main(String[] args) {
        try {
            // 创建Selector
            Selector selector = Selector.open();
            
            // 创建ServerSocketChannel
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(8080));
            serverChannel.configureBlocking(false);
            
            // 将ServerSocketChannel注册到Selector
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            System.out.println("NIO服务器启动，监听端口8080");
            
            while (true) {
                // 阻塞等待事件
                int readyChannels = selector.select();
                if (readyChannels == 0) {
                    continue;
                }
                
                // 获取就绪的SelectionKey
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    
                    if (key.isAcceptable()) {
                        handleAccept(key, selector);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                    
                    keyIterator.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void handleAccept(SelectionKey key, Selector selector) {
        try {
            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
            SocketChannel clientChannel = serverChannel.accept();
            
            if (clientChannel != null) {
                clientChannel.configureBlocking(false);
                clientChannel.register(selector, SelectionKey.OP_READ);
                System.out.println("新客户端连接: " + clientChannel.getRemoteAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void handleRead(SelectionKey key) {
        try {
            SocketChannel clientChannel = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            
            int bytesRead = clientChannel.read(buffer);
            if (bytesRead > 0) {
                buffer.flip();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                String message = new String(data);
                System.out.println("收到消息: " + message);
                
                // 回复客户端
                String response = "Echo: " + message;
                ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
                clientChannel.write(responseBuffer);
            } else if (bytesRead == -1) {
                // 客户端断开连接
                System.out.println("客户端断开连接: " + clientChannel.getRemoteAddress());
                key.cancel();
                clientChannel.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            key.cancel();
        }
    }
}
```

## NIO编程实例

### 完整的NIO聊天服务器

```java
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NIOChatServer {
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private Map<SocketChannel, String> clients = new ConcurrentHashMap<>();
    private static final int PORT = 8080;
    
    public NIOChatServer() {
        try {
            selector = Selector.open();
            serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            
            System.out.println("聊天服务器启动，监听端口: " + PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void start() {
        try {
            while (true) {
                int readyChannels = selector.select();
                if (readyChannels == 0) {
                    continue;
                }
                
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
                
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    }
                    
                    keyIterator.remove();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleAccept(SelectionKey key) {
        try {
            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
            SocketChannel clientChannel = serverChannel.accept();
            
            if (clientChannel != null) {
                clientChannel.configureBlocking(false);
                clientChannel.register(selector, SelectionKey.OP_READ);
                
                String clientAddress = clientChannel.getRemoteAddress().toString();
                clients.put(clientChannel, clientAddress);
                
                System.out.println("新用户加入聊天室: " + clientAddress);
                broadcast(clientAddress + " 加入了聊天室", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void handleRead(SelectionKey key) {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        try {
            int bytesRead = clientChannel.read(buffer);
            if (bytesRead > 0) {
                buffer.flip();
                byte[] data = new byte[buffer.remaining()];
                buffer.get(data);
                String message = new String(data).trim();
                
                String clientAddress = clients.get(clientChannel);
                System.out.println(clientAddress + ": " + message);
                
                // 广播消息给所有客户端
                broadcast(clientAddress + ": " + message, clientChannel);
            } else if (bytesRead == -1) {
                // 客户端断开连接
                String clientAddress = clients.remove(clientChannel);
                System.out.println("用户离开聊天室: " + clientAddress);
                broadcast(clientAddress + " 离开了聊天室", null);
                
                key.cancel();
                clientChannel.close();
            }
        } catch (Exception e) {
            // 处理异常断开
            String clientAddress = clients.remove(clientChannel);
            System.out.println("用户异常断开: " + clientAddress);
            
            key.cancel();
            try {
                clientChannel.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private void broadcast(String message, SocketChannel sender) {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        
        for (SocketChannel client : clients.keySet()) {
            if (client != sender && client.isConnected()) {
                try {
                    buffer.rewind();
                    client.write(buffer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public static void main(String[] args) {
        new NIOChatServer().start();
    }
}
```

### NIO聊天客户端

```java
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class NIOChatClient {
    private SocketChannel socketChannel;
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    
    public NIOChatClient() {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(HOST, PORT));
            socketChannel.configureBlocking(false);
            
            System.out.println("连接到聊天服务器: " + HOST + ":" + PORT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void start() {
        // 启动接收消息的线程
        new Thread(this::receiveMessages).start();
        
        // 主线程处理用户输入
        sendMessages();
    }
    
    private void receiveMessages() {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        while (true) {
            try {
                buffer.clear();
                int bytesRead = socketChannel.read(buffer);
                
                if (bytesRead > 0) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    System.out.println(new String(data));
                } else if (bytesRead == -1) {
                    System.out.println("服务器断开连接");
                    break;
                }
                
                Thread.sleep(100); // 避免CPU空转
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }
    
    private void sendMessages() {
        Scanner scanner = new Scanner(System.in);
        
        while (scanner.hasNextLine()) {
            String message = scanner.nextLine();
            if ("quit".equalsIgnoreCase(message)) {
                break;
            }
            
            try {
                ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
                socketChannel.write(buffer);
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
        
        try {
            socketChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        scanner.close();
    }
    
    public static void main(String[] args) {
        new NIOChatClient().start();
    }
}
```

## NIO vs BIO对比

| 特性 | BIO | NIO |
|------|-----|-----|
| 阻塞性 | 阻塞I/O | 非阻塞I/O |
| 线程模型 | 一个连接一个线程 | 一个线程处理多个连接 |
| 内存使用 | 较高（每个线程需要栈空间） | 较低 |
| 上下文切换 | 频繁 | 较少 |
| 适用场景 | 连接数较少 | 高并发场景 |
| 编程复杂度 | 简单 | 复杂 |
| 性能 | 连接数少时性能好 | 高并发时性能好 |

### 性能对比测试

```java
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PerformanceComparison {
    private static final int PORT = 8080;
    private static final int CLIENT_COUNT = 1000;
    private static final String MESSAGE = "Hello Server";
    
    // BIO服务器
    static class BIOServer {
        public static void start() {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                ExecutorService executor = Executors.newFixedThreadPool(100);
                
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(() -> handleClient(clientSocket));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private static void handleClient(Socket clientSocket) {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(
                    clientSocket.getOutputStream(), true)) {
                
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    out.println("Echo: " + inputLine);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    // NIO服务器
    static class NIOServer {
        public static void start() {
            try {
                Selector selector = Selector.open();
                ServerSocketChannel serverChannel = ServerSocketChannel.open();
                serverChannel.bind(new InetSocketAddress(PORT));
                serverChannel.configureBlocking(false);
                serverChannel.register(selector, SelectionKey.OP_ACCEPT);
                
                while (true) {
                    selector.select();
                    
                    for (SelectionKey key : selector.selectedKeys()) {
                        if (key.isAcceptable()) {
                            handleAccept(key, selector);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        }
                    }
                    
                    selector.selectedKeys().clear();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private static void handleAccept(SelectionKey key, Selector selector) {
            try {
                ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                SocketChannel clientChannel = serverChannel.accept();
                clientChannel.configureBlocking(false);
                clientChannel.register(selector, SelectionKey.OP_READ);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private static void handleRead(SelectionKey key) {
            try {
                SocketChannel clientChannel = (SocketChannel) key.channel();
                ByteBuffer buffer = ByteBuffer.allocate(1024);
                
                int bytesRead = clientChannel.read(buffer);
                if (bytesRead > 0) {
                    buffer.flip();
                    String message = new String(buffer.array(), 0, buffer.remaining());
                    String response = "Echo: " + message;
                    clientChannel.write(ByteBuffer.wrap(response.getBytes()));
                }
            } catch (Exception e) {
                key.cancel();
            }
        }
    }
    
    // 性能测试客户端
    public static void performanceTest() {
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < CLIENT_COUNT; i++) {
            new Thread(() -> {
                try (Socket socket = new Socket("localhost", PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()))) {
                    
                    out.println(MESSAGE);
                    String response = in.readLine();
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("处理 " + CLIENT_COUNT + " 个客户端耗时: " + (endTime - startTime) + "ms");
    }
}
```

## 性能优化

### 1. 缓冲区优化

```java
public class BufferOptimization {
    // 使用直接缓冲区提高性能
    public static ByteBuffer createOptimizedBuffer(int size) {
        return ByteBuffer.allocateDirect(size);
    }
    
    // 缓冲区池化
    private static final ThreadLocal<ByteBuffer> BUFFER_POOL = 
        ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(8192));
    
    public static ByteBuffer getPooledBuffer() {
        ByteBuffer buffer = BUFFER_POOL.get();
        buffer.clear();
        return buffer;
    }
    
    // 批量读写
    public static void batchRead(SocketChannel channel, ByteBuffer buffer) throws Exception {
        while (buffer.hasRemaining() && channel.read(buffer) > 0) {
            // 继续读取直到缓冲区满或没有更多数据
        }
    }
}
```

### 2. Selector优化

```java
public class SelectorOptimization {
    // 使用多个Selector分担负载
    private Selector[] selectors;
    private int selectorCount;
    private int currentSelector = 0;
    
    public SelectorOptimization(int selectorCount) throws Exception {
        this.selectorCount = selectorCount;
        this.selectors = new Selector[selectorCount];
        
        for (int i = 0; i < selectorCount; i++) {
            selectors[i] = Selector.open();
        }
    }
    
    public Selector getNextSelector() {
        return selectors[currentSelector++ % selectorCount];
    }
    
    // 设置合适的超时时间
    public void selectWithTimeout(Selector selector) throws Exception {
        selector.select(1000); // 1秒超时，避免无限阻塞
    }
}
```

### 3. 线程模型优化

```java
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadModelOptimization {
    // Boss线程处理连接，Worker线程处理I/O
    private ExecutorService bossGroup;
    private ExecutorService workerGroup;
    
    public ThreadModelOptimization() {
        // Boss线程数量通常为1
        bossGroup = Executors.newFixedThreadPool(1);
        // Worker线程数量通常为CPU核心数的2倍
        workerGroup = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }
    
    public void handleAccept(SelectionKey key) {
        bossGroup.submit(() -> {
            // 处理连接接受逻辑
        });
    }
    
    public void handleIO(SelectionKey key) {
        workerGroup.submit(() -> {
            // 处理I/O操作
        });
    }
}
```

## 最佳实践

### 1. 资源管理
```java
public class ResourceManagement {
    // 正确关闭资源
    public static void closeQuietly(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            if (closeable != null) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    // 记录日志但不抛出异常
                    System.err.println("关闭资源时发生错误: " + e.getMessage());
                }
            }
        }
    }
    
    // 使用try-with-resources
    public static void useResources() {
        try (Selector selector = Selector.open();
             ServerSocketChannel serverChannel = ServerSocketChannel.open()) {
            
            // 使用资源
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 2. 异常处理
```java
public class ExceptionHandling {
    public static void handleSelectionKey(SelectionKey key) {
        try {
            if (key.isReadable()) {
                // 处理读操作
            }
        } catch (Exception e) {
            // 取消key并关闭channel
            key.cancel();
            try {
                key.channel().close();
            } catch (Exception ex) {
                // 记录日志
            }
        }
    }
}
```

### 3. 内存管理
```java
public class MemoryManagement {
    // 避免频繁创建ByteBuffer
    private static final ThreadLocal<ByteBuffer> THREAD_LOCAL_BUFFER = 
        ThreadLocal.withInitial(() -> ByteBuffer.allocate(8192));
    
    public static ByteBuffer getBuffer() {
        ByteBuffer buffer = THREAD_LOCAL_BUFFER.get();
        buffer.clear();
        return buffer;
    }
    
    // 及时释放直接内存
    public static void releaseDirectBuffer(ByteBuffer buffer) {
        if (buffer.isDirect()) {
            ((sun.nio.ch.DirectBuffer) buffer).cleaner().clean();
        }
    }
}
```

## 面试要点

### 高频问题
1. **NIO和BIO的区别？**
   - 阻塞性：BIO阻塞，NIO非阻塞
   - 线程模型：BIO一对一，NIO一对多
   - 性能：NIO在高并发场景下性能更好

2. **NIO的核心组件？**
   - Channel：数据传输通道
   - Buffer：数据缓冲区
   - Selector：多路复用器

3. **Buffer的工作原理？**
   - 四个重要属性：capacity、limit、position、mark
   - 读写模式切换：flip()、clear()、compact()

4. **Selector的作用？**
   - 单线程监控多个Channel
   - 事件驱动模型
   - 提高并发处理能力

### 深入问题
1. **直接缓冲区和非直接缓冲区的区别？**
   - 内存位置：堆内 vs 堆外
   - 性能：直接缓冲区I/O性能更好
   - 创建成本：直接缓冲区创建成本更高

2. **NIO的零拷贝是什么？**
   - 减少数据在内核空间和用户空间之间的拷贝
   - FileChannel.transferTo()方法
   - 提高文件传输性能

3. **如何处理NIO中的半包和粘包问题？**
   - 固定长度分隔
   - 特殊字符分隔
   - 长度字段分隔

### 实践经验
1. **NIO编程的难点？**
   - 编程复杂度高
   - 需要处理各种边界情况
   - 调试困难

2. **NIO性能优化方法？**
   - 使用直接缓冲区
   - 合理设置缓冲区大小
   - 使用多Selector
   - 优化线程模型

3. **实际项目中的应用？**
   - 高并发服务器
   - 消息中间件
   - 代理服务器
   - 文件传输服务

## 总结

NIO是Java提供的高性能I/O编程模型，通过非阻塞I/O和多路复用技术，能够在单线程中处理大量并发连接。虽然编程复杂度较高，但在高并发场景下具有显著的性能优势。

掌握NIO编程需要：
- 深入理解Channel、Buffer、Selector三大核心组件
- 熟练使用各种Channel类型
- 掌握Buffer的读写模式切换
- 理解Selector的事件驱动模型
- 注意资源管理和异常处理
- 进行合理的性能优化

NIO为后续学习Netty等高性能网络框架奠定了重要基础。