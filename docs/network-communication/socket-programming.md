# Java Socket编程详解

## 目录
- [Socket概述](#socket概述)
- [TCP Socket编程](#tcp-socket编程)
- [UDP Socket编程](#udp-socket编程)
- [Socket选项配置](#socket选项配置)
- [多线程Socket服务器](#多线程socket服务器)
- [NIO Socket编程](#nio-socket编程)
- [Socket连接池](#socket连接池)
- [性能优化](#性能优化)
- [常见问题](#常见问题)
- [最佳实践](#最佳实践)
- [面试要点](#面试要点)

## Socket概述

### 什么是Socket
Socket（套接字）是网络编程的基础，它提供了进程间通信的端点。在Java中，Socket是对TCP/IP协议的封装，提供了简单易用的网络编程接口。

### Socket类型
- **TCP Socket**：基于TCP协议，提供可靠的、面向连接的通信
- **UDP Socket**：基于UDP协议，提供不可靠的、无连接的通信
- **Unix Domain Socket**：用于同一主机上的进程间通信

### Java中的Socket类
- **Socket**：TCP客户端Socket
- **ServerSocket**：TCP服务器Socket
- **DatagramSocket**：UDP Socket
- **MulticastSocket**：多播Socket

## TCP Socket编程

### 基本TCP客户端

```java
import java.io.*;
import java.net.*;

public class TCPClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT)) {
            System.out.println("Connected to server: " + socket.getRemoteSocketAddress());
            
            // 获取输入输出流
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            PrintWriter writer = new PrintWriter(
                socket.getOutputStream(), true
            );
            
            // 发送消息
            writer.println("Hello, Server!");
            
            // 接收响应
            String response = reader.readLine();
            System.out.println("Server response: " + response);
            
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
```

### 基本TCP服务器

```java
import java.io.*;
import java.net.*;

public class TCPServer {
    private static final int PORT = 8080;
    
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            
            while (true) {
                // 等待客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
                
                // 处理客户端请求
                handleClient(clientSocket);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
    
    private static void handleClient(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter writer = new PrintWriter(
                clientSocket.getOutputStream(), true)) {
            
            String message = reader.readLine();
            System.out.println("Received: " + message);
            
            // 发送响应
            writer.println("Echo: " + message);
            
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
}
```

### 改进的TCP客户端（支持交互）

```java
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class InteractiveTCPClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private Scanner scanner;
    
    public InteractiveTCPClient(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        scanner = new Scanner(System.in);
        
        System.out.println("Connected to " + host + ":" + port);
    }
    
    public void start() {
        // 启动接收消息的线程
        Thread receiveThread = new Thread(this::receiveMessages);
        receiveThread.setDaemon(true);
        receiveThread.start();
        
        // 主线程处理用户输入
        sendMessages();
    }
    
    private void receiveMessages() {
        try {
            String message;
            while ((message = reader.readLine()) != null) {
                System.out.println("Server: " + message);
            }
        } catch (IOException e) {
            System.err.println("Error receiving messages: " + e.getMessage());
        }
    }
    
    private void sendMessages() {
        System.out.println("Enter messages (type 'quit' to exit):");
        
        String input;
        while (!(input = scanner.nextLine()).equals("quit")) {
            writer.println(input);
        }
        
        close();
    }
    
    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (scanner != null) {
                scanner.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing client: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        try {
            InteractiveTCPClient client = new InteractiveTCPClient("localhost", 8080);
            client.start();
        } catch (IOException e) {
            System.err.println("Failed to connect: " + e.getMessage());
        }
    }
}
```

## UDP Socket编程

### UDP客户端

```java
import java.io.*;
import java.net.*;

public class UDPClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final int BUFFER_SIZE = 1024;
    
    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket()) {
            // 准备发送数据
            String message = "Hello, UDP Server!";
            byte[] sendData = message.getBytes();
            
            InetAddress serverAddress = InetAddress.getByName(SERVER_HOST);
            DatagramPacket sendPacket = new DatagramPacket(
                sendData, sendData.length, serverAddress, SERVER_PORT
            );
            
            // 发送数据包
            socket.send(sendPacket);
            System.out.println("Sent: " + message);
            
            // 接收响应
            byte[] receiveData = new byte[BUFFER_SIZE];
            DatagramPacket receivePacket = new DatagramPacket(
                receiveData, receiveData.length
            );
            
            socket.receive(receivePacket);
            String response = new String(
                receivePacket.getData(), 0, receivePacket.getLength()
            );
            
            System.out.println("Received: " + response);
            System.out.println("From: " + receivePacket.getAddress() + 
                             ":" + receivePacket.getPort());
            
        } catch (IOException e) {
            System.err.println("UDP Client error: " + e.getMessage());
        }
    }
}
```

### UDP服务器

```java
import java.io.*;
import java.net.*;

public class UDPServer {
    private static final int PORT = 8080;
    private static final int BUFFER_SIZE = 1024;
    
    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("UDP Server started on port " + PORT);
            
            byte[] buffer = new byte[BUFFER_SIZE];
            
            while (true) {
                // 接收数据包
                DatagramPacket receivePacket = new DatagramPacket(
                    buffer, buffer.length
                );
                socket.receive(receivePacket);
                
                String message = new String(
                    receivePacket.getData(), 0, receivePacket.getLength()
                );
                
                System.out.println("Received: " + message);
                System.out.println("From: " + receivePacket.getAddress() + 
                                 ":" + receivePacket.getPort());
                
                // 发送响应
                String response = "Echo: " + message;
                byte[] responseData = response.getBytes();
                
                DatagramPacket sendPacket = new DatagramPacket(
                    responseData, responseData.length,
                    receivePacket.getAddress(), receivePacket.getPort()
                );
                
                socket.send(sendPacket);
                System.out.println("Sent response: " + response);
            }
            
        } catch (IOException e) {
            System.err.println("UDP Server error: " + e.getMessage());
        }
    }
}
```

### UDP文件传输示例

```java
import java.io.*;
import java.net.*;
import java.nio.file.*;

public class UDPFileTransfer {
    private static final int PACKET_SIZE = 1024;
    private static final int HEADER_SIZE = 8; // 4 bytes for sequence + 4 bytes for total packets
    private static final int DATA_SIZE = PACKET_SIZE - HEADER_SIZE;
    
    // 文件发送端
    public static class FileSender {
        private DatagramSocket socket;
        private InetAddress receiverAddress;
        private int receiverPort;
        
        public FileSender(String receiverHost, int receiverPort) throws IOException {
            this.socket = new DatagramSocket();
            this.receiverAddress = InetAddress.getByName(receiverHost);
            this.receiverPort = receiverPort;
        }
        
        public void sendFile(String filePath) throws IOException {
            byte[] fileData = Files.readAllBytes(Paths.get(filePath));
            int totalPackets = (int) Math.ceil((double) fileData.length / DATA_SIZE);
            
            System.out.println("Sending file: " + filePath);
            System.out.println("File size: " + fileData.length + " bytes");
            System.out.println("Total packets: " + totalPackets);
            
            for (int i = 0; i < totalPackets; i++) {
                int offset = i * DATA_SIZE;
                int length = Math.min(DATA_SIZE, fileData.length - offset);
                
                // 创建数据包
                byte[] packet = new byte[HEADER_SIZE + length];
                
                // 写入序列号和总包数
                writeInt(packet, 0, i);
                writeInt(packet, 4, totalPackets);
                
                // 写入文件数据
                System.arraycopy(fileData, offset, packet, HEADER_SIZE, length);
                
                // 发送数据包
                DatagramPacket datagramPacket = new DatagramPacket(
                    packet, packet.length, receiverAddress, receiverPort
                );
                socket.send(datagramPacket);
                
                System.out.println("Sent packet " + (i + 1) + "/" + totalPackets);
                
                // 简单的流控
                Thread.sleep(1);
            }
            
            System.out.println("File sent successfully!");
        }
        
        private void writeInt(byte[] array, int offset, int value) {
            array[offset] = (byte) (value >>> 24);
            array[offset + 1] = (byte) (value >>> 16);
            array[offset + 2] = (byte) (value >>> 8);
            array[offset + 3] = (byte) value;
        }
        
        public void close() {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
    
    // 文件接收端
    public static class FileReceiver {
        private DatagramSocket socket;
        private byte[][] packets;
        private boolean[] received;
        private int totalPackets = -1;
        private int receivedCount = 0;
        
        public FileReceiver(int port) throws IOException {
            this.socket = new DatagramSocket(port);
        }
        
        public void receiveFile(String outputPath) throws IOException {
            System.out.println("Waiting for file...");
            
            byte[] buffer = new byte[PACKET_SIZE];
            
            while (receivedCount < totalPackets || totalPackets == -1) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                // 解析包头
                int sequence = readInt(packet.getData(), 0);
                int total = readInt(packet.getData(), 4);
                
                if (totalPackets == -1) {
                    totalPackets = total;
                    packets = new byte[totalPackets][];
                    received = new boolean[totalPackets];
                    System.out.println("Expecting " + totalPackets + " packets");
                }
                
                if (sequence < totalPackets && !received[sequence]) {
                    // 存储数据
                    int dataLength = packet.getLength() - HEADER_SIZE;
                    packets[sequence] = new byte[dataLength];
                    System.arraycopy(packet.getData(), HEADER_SIZE, 
                                   packets[sequence], 0, dataLength);
                    
                    received[sequence] = true;
                    receivedCount++;
                    
                    System.out.println("Received packet " + (sequence + 1) + "/" + totalPackets);
                }
            }
            
            // 重组文件
            try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                for (byte[] packetData : packets) {
                    fos.write(packetData);
                }
            }
            
            System.out.println("File received and saved to: " + outputPath);
        }
        
        private int readInt(byte[] array, int offset) {
            return ((array[offset] & 0xFF) << 24) |
                   ((array[offset + 1] & 0xFF) << 16) |
                   ((array[offset + 2] & 0xFF) << 8) |
                   (array[offset + 3] & 0xFF);
        }
        
        public void close() {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java UDPFileTransfer <send|receive> [options]");
            return;
        }
        
        if ("send".equals(args[0])) {
            if (args.length < 4) {
                System.out.println("Usage: java UDPFileTransfer send <host> <port> <file>");
                return;
            }
            
            FileSender sender = new FileSender(args[1], Integer.parseInt(args[2]));
            try {
                sender.sendFile(args[3]);
            } finally {
                sender.close();
            }
            
        } else if ("receive".equals(args[0])) {
            if (args.length < 3) {
                System.out.println("Usage: java UDPFileTransfer receive <port> <output_file>");
                return;
            }
            
            FileReceiver receiver = new FileReceiver(Integer.parseInt(args[1]));
            try {
                receiver.receiveFile(args[2]);
            } finally {
                receiver.close();
            }
        }
    }
}
```

## Socket选项配置

### 常用Socket选项

```java
import java.io.*;
import java.net.*;

public class SocketOptionsExample {
    
    public static void configureServerSocket(ServerSocket serverSocket) throws SocketException {
        // 设置地址重用（避免TIME_WAIT状态下的地址占用）
        serverSocket.setReuseAddress(true);
        
        // 设置接收缓冲区大小
        serverSocket.setReceiveBufferSize(64 * 1024); // 64KB
        
        // 设置性能偏好：连接时间、延迟、带宽
        serverSocket.setPerformancePreferences(1, 2, 0);
        
        System.out.println("ServerSocket options configured:");
        System.out.println("  ReuseAddress: " + serverSocket.getReuseAddress());
        System.out.println("  ReceiveBufferSize: " + serverSocket.getReceiveBufferSize());
    }
    
    public static void configureClientSocket(Socket socket) throws SocketException {
        // 设置TCP无延迟（禁用Nagle算法）
        socket.setTcpNoDelay(true);
        
        // 设置保持连接活跃
        socket.setKeepAlive(true);
        
        // 设置发送缓冲区大小
        socket.setSendBufferSize(64 * 1024); // 64KB
        
        // 设置接收缓冲区大小
        socket.setReceiveBufferSize(64 * 1024); // 64KB
        
        // 设置SO_LINGER选项（控制close()行为）
        socket.setSoLinger(true, 30); // 30秒超时
        
        // 设置SO_TIMEOUT（读取超时）
        socket.setSoTimeout(30000); // 30秒超时
        
        // 设置流量类别（QoS）
        socket.setTrafficClass(0x04); // IPTOS_RELIABILITY
        
        System.out.println("Client Socket options configured:");
        System.out.println("  TcpNoDelay: " + socket.getTcpNoDelay());
        System.out.println("  KeepAlive: " + socket.getKeepAlive());
        System.out.println("  SendBufferSize: " + socket.getSendBufferSize());
        System.out.println("  ReceiveBufferSize: " + socket.getReceiveBufferSize());
        System.out.println("  SoLinger: " + socket.getSoLinger());
        System.out.println("  SoTimeout: " + socket.getSoTimeout());
        System.out.println("  TrafficClass: " + socket.getTrafficClass());
    }
    
    public static void configureDatagramSocket(DatagramSocket socket) throws SocketException {
        // 设置广播模式
        socket.setBroadcast(true);
        
        // 设置接收缓冲区大小
        socket.setReceiveBufferSize(128 * 1024); // 128KB
        
        // 设置发送缓冲区大小
        socket.setSendBufferSize(128 * 1024); // 128KB
        
        // 设置接收超时
        socket.setSoTimeout(5000); // 5秒超时
        
        // 设置流量类别
        socket.setTrafficClass(0x02); // IPTOS_LOWDELAY
        
        System.out.println("DatagramSocket options configured:");
        System.out.println("  Broadcast: " + socket.getBroadcast());
        System.out.println("  ReceiveBufferSize: " + socket.getReceiveBufferSize());
        System.out.println("  SendBufferSize: " + socket.getSendBufferSize());
        System.out.println("  SoTimeout: " + socket.getSoTimeout());
        System.out.println("  TrafficClass: " + socket.getTrafficClass());
    }
    
    public static void main(String[] args) {
        try {
            // 配置ServerSocket
            ServerSocket serverSocket = new ServerSocket();
            configureServerSocket(serverSocket);
            serverSocket.close();
            
            System.out.println();
            
            // 配置客户端Socket
            Socket clientSocket = new Socket();
            configureClientSocket(clientSocket);
            clientSocket.close();
            
            System.out.println();
            
            // 配置DatagramSocket
            DatagramSocket datagramSocket = new DatagramSocket();
            configureDatagramSocket(datagramSocket);
            datagramSocket.close();
            
        } catch (IOException e) {
            System.err.println("Error configuring sockets: " + e.getMessage());
        }
    }
}
```

## 多线程Socket服务器

### 基于线程池的TCP服务器

```java
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ThreadPoolTCPServer {
    private final int port;
    private final ExecutorService threadPool;
    private final AtomicInteger clientCounter;
    private volatile boolean running;
    private ServerSocket serverSocket;
    
    public ThreadPoolTCPServer(int port, int threadPoolSize) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
        this.clientCounter = new AtomicInteger(0);
        this.running = false;
    }
    
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        
        // 配置ServerSocket选项
        serverSocket.setReuseAddress(true);
        serverSocket.setReceiveBufferSize(64 * 1024);
        
        running = true;
        System.out.println("Server started on port " + port);
        
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                int clientId = clientCounter.incrementAndGet();
                
                System.out.println("Client " + clientId + " connected: " + 
                                 clientSocket.getRemoteSocketAddress());
                
                // 提交客户端处理任务到线程池
                threadPool.submit(new ClientHandler(clientSocket, clientId));
                
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        }
    }
    
    public void stop() {
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        
        threadPool.shutdown();
        try {
            if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Server stopped");
    }
    
    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private final int clientId;
        
        public ClientHandler(Socket clientSocket, int clientId) {
            this.clientSocket = clientSocket;
            this.clientId = clientId;
        }
        
        @Override
        public void run() {
            try {
                // 配置客户端Socket选项
                clientSocket.setTcpNoDelay(true);
                clientSocket.setKeepAlive(true);
                clientSocket.setSoTimeout(30000); // 30秒超时
                
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())
                );
                PrintWriter writer = new PrintWriter(
                    clientSocket.getOutputStream(), true
                );
                
                // 发送欢迎消息
                writer.println("Welcome! You are client #" + clientId);
                
                String message;
                while ((message = reader.readLine()) != null) {
                    System.out.println("Client " + clientId + ": " + message);
                    
                    if ("bye".equalsIgnoreCase(message.trim())) {
                        writer.println("Goodbye!");
                        break;
                    }
                    
                    // 回显消息
                    writer.println("Echo: " + message);
                }
                
            } catch (SocketTimeoutException e) {
                System.out.println("Client " + clientId + " timed out");
            } catch (IOException e) {
                System.err.println("Error handling client " + clientId + ": " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                    System.out.println("Client " + clientId + " disconnected");
                } catch (IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                }
            }
        }
    }
    
    public static void main(String[] args) {
        ThreadPoolTCPServer server = new ThreadPoolTCPServer(8080, 10);
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
        }
    }
}
```

### 聊天室服务器示例

```java
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private final int port;
    private final Set<ClientHandler> clients;
    private final ExecutorService threadPool;
    private volatile boolean running;
    private ServerSocket serverSocket;
    
    public ChatServer(int port) {
        this.port = port;
        this.clients = ConcurrentHashMap.newKeySet();
        this.threadPool = Executors.newCachedThreadPool();
        this.running = false;
    }
    
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        
        running = true;
        System.out.println("Chat server started on port " + port);
        
        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                clients.add(clientHandler);
                threadPool.submit(clientHandler);
                
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting client: " + e.getMessage());
                }
            }
        }
    }
    
    public void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }
    
    public void removeClient(ClientHandler client) {
        clients.remove(client);
        broadcast(client.getNickname() + " left the chat", null);
        System.out.println(client.getNickname() + " disconnected. Active clients: " + clients.size());
    }
    
    public void stop() {
        running = false;
        
        // 通知所有客户端服务器关闭
        for (ClientHandler client : clients) {
            client.sendMessage("Server is shutting down...");
            client.close();
        }
        clients.clear();
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
        
        threadPool.shutdown();
        System.out.println("Chat server stopped");
    }
    
    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private final ChatServer server;
        private BufferedReader reader;
        private PrintWriter writer;
        private String nickname;
        
        public ClientHandler(Socket socket, ChatServer server) {
            this.socket = socket;
            this.server = server;
        }
        
        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);
                
                // 获取用户昵称
                writer.println("Enter your nickname:");
                nickname = reader.readLine();
                if (nickname == null || nickname.trim().isEmpty()) {
                    nickname = "Anonymous_" + socket.getPort();
                }
                
                System.out.println(nickname + " joined the chat from " + socket.getRemoteSocketAddress());
                writer.println("Welcome to the chat, " + nickname + "!");
                server.broadcast(nickname + " joined the chat", this);
                
                String message;
                while ((message = reader.readLine()) != null) {
                    if (message.trim().isEmpty()) {
                        continue;
                    }
                    
                    if ("/quit".equalsIgnoreCase(message.trim())) {
                        break;
                    }
                    
                    if ("/list".equalsIgnoreCase(message.trim())) {
                        writer.println("Active users: " + server.clients.size());
                        continue;
                    }
                    
                    // 广播消息
                    String formattedMessage = nickname + ": " + message;
                    System.out.println(formattedMessage);
                    server.broadcast(formattedMessage, this);
                }
                
            } catch (IOException e) {
                System.err.println("Error handling client " + nickname + ": " + e.getMessage());
            } finally {
                close();
                server.removeClient(this);
            }
        }
        
        public void sendMessage(String message) {
            if (writer != null) {
                writer.println(message);
            }
        }
        
        public String getNickname() {
            return nickname != null ? nickname : "Unknown";
        }
        
        public void close() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }
    
    public static void main(String[] args) {
        ChatServer server = new ChatServer(8080);
        
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start chat server: " + e.getMessage());
        }
    }
}
```

## NIO Socket编程

### 基本NIO服务器

```java
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class NIOServer {
    private final int port;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean running;
    
    public NIOServer(int port) {
        this.port = port;
        this.running = false;
    }
    
    public void start() throws IOException {
        // 创建选择器
        selector = Selector.open();
        
        // 创建服务器通道
        serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().setReuseAddress(true);
        serverChannel.bind(new InetSocketAddress(port));
        
        // 注册接受连接事件
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        running = true;
        System.out.println("NIO Server started on port " + port);
        
        while (running) {
            // 等待事件
            int readyChannels = selector.select(1000); // 1秒超时
            
            if (readyChannels == 0) {
                continue;
            }
            
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();
            
            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();
                keyIterator.remove();
                
                try {
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                } catch (IOException e) {
                    System.err.println("Error handling key: " + e.getMessage());
                    key.cancel();
                    if (key.channel() != null) {
                        key.channel().close();
                    }
                }
            }
        }
    }
    
    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);
            
            System.out.println("Client connected: " + clientChannel.getRemoteAddress());
        }
    }
    
    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        
        int bytesRead = clientChannel.read(buffer);
        
        if (bytesRead == -1) {
            // 客户端关闭连接
            System.out.println("Client disconnected: " + clientChannel.getRemoteAddress());
            key.cancel();
            clientChannel.close();
            return;
        }
        
        if (bytesRead > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            
            String message = new String(data).trim();
            System.out.println("Received: " + message + " from " + clientChannel.getRemoteAddress());
            
            // 准备回显消息
            String response = "Echo: " + message;
            ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
            key.attach(responseBuffer);
            
            // 注册写事件
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }
    
    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        
        if (buffer != null) {
            clientChannel.write(buffer);
            
            if (!buffer.hasRemaining()) {
                // 写完成，重新注册读事件
                key.attach(null);
                key.interestOps(SelectionKey.OP_READ);
            }
        }
    }
    
    public void stop() {
        running = false;
        
        try {
            if (selector != null && selector.isOpen()) {
                selector.close();
            }
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }
        } catch (IOException e) {
            System.err.println("Error stopping server: " + e.getMessage());
        }
        
        System.out.println("NIO Server stopped");
    }
    
    public static void main(String[] args) {
        NIOServer server = new NIOServer(8080);
        
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start NIO server: " + e.getMessage());
        }
    }
}
```

### NIO客户端

```java
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class NIOClient {
    private final String host;
    private final int port;
    private SocketChannel socketChannel;
    
    public NIOClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    public void connect() throws IOException {
        socketChannel = SocketChannel.open();
        socketChannel.connect(new InetSocketAddress(host, port));
        
        System.out.println("Connected to " + host + ":" + port);
    }
    
    public void sendMessage(String message) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(message.getBytes());
        socketChannel.write(buffer);
    }
    
    public String receiveMessage() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int bytesRead = socketChannel.read(buffer);
        
        if (bytesRead > 0) {
            buffer.flip();
            byte[] data = new byte[buffer.remaining()];
            buffer.get(data);
            return new String(data);
        }
        
        return null;
    }
    
    public void close() {
        try {
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing client: " + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        NIOClient client = new NIOClient("localhost", 8080);
        Scanner scanner = new Scanner(System.in);
        
        try {
            client.connect();
            
            System.out.println("Enter messages (type 'quit' to exit):");
            
            String input;
            while (!(input = scanner.nextLine()).equals("quit")) {
                client.sendMessage(input);
                String response = client.receiveMessage();
                if (response != null) {
                    System.out.println("Server: " + response);
                }
            }
            
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            client.close();
            scanner.close();
        }
    }
}
```

## Socket连接池

### 简单的Socket连接池实现

```java
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketPool {
    private final String host;
    private final int port;
    private final int maxPoolSize;
    private final int connectionTimeout;
    private final BlockingQueue<Socket> availableConnections;
    private final AtomicInteger currentPoolSize;
    private volatile boolean closed;
    
    public SocketPool(String host, int port, int maxPoolSize, int connectionTimeout) {
        this.host = host;
        this.port = port;
        this.maxPoolSize = maxPoolSize;
        this.connectionTimeout = connectionTimeout;
        this.availableConnections = new LinkedBlockingQueue<>();
        this.currentPoolSize = new AtomicInteger(0);
        this.closed = false;
    }
    
    public Socket getConnection() throws IOException, InterruptedException {
        if (closed) {
            throw new IllegalStateException("Socket pool is closed");
        }
        
        // 尝试从池中获取连接
        Socket socket = availableConnections.poll();
        
        if (socket != null && isSocketValid(socket)) {
            return socket;
        }
        
        // 如果没有可用连接且未达到最大连接数，创建新连接
        if (currentPoolSize.get() < maxPoolSize) {
            socket = createNewConnection();
            if (socket != null) {
                currentPoolSize.incrementAndGet();
                return socket;
            }
        }
        
        // 等待可用连接
        socket = availableConnections.poll(connectionTimeout, TimeUnit.MILLISECONDS);
        if (socket == null) {
            throw new IOException("Timeout waiting for available connection");
        }
        
        if (!isSocketValid(socket)) {
            currentPoolSize.decrementAndGet();
            return getConnection(); // 递归重试
        }
        
        return socket;
    }
    
    public void returnConnection(Socket socket) {
        if (closed || socket == null || !isSocketValid(socket)) {
            if (socket != null) {
                closeSocket(socket);
                currentPoolSize.decrementAndGet();
            }
            return;
        }
        
        if (!availableConnections.offer(socket)) {
            // 池已满，关闭连接
            closeSocket(socket);
            currentPoolSize.decrementAndGet();
        }
    }
    
    private Socket createNewConnection() {
        try {
            Socket socket = new Socket(host, port);
            
            // 配置Socket选项
            socket.setTcpNoDelay(true);
            socket.setKeepAlive(true);
            socket.setSoTimeout(30000); // 30秒读取超时
            
            System.out.println("Created new connection to " + host + ":" + port);
            return socket;
            
        } catch (IOException e) {
            System.err.println("Failed to create connection: " + e.getMessage());
            return null;
        }
    }
    
    private boolean isSocketValid(Socket socket) {
        return socket != null && 
               socket.isConnected() && 
               !socket.isClosed() && 
               !socket.isInputShutdown() && 
               !socket.isOutputShutdown();
    }
    
    private void closeSocket(Socket socket) {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing socket: " + e.getMessage());
        }
    }
    
    public void close() {
        closed = true;
        
        // 关闭所有连接
        Socket socket;
        while ((socket = availableConnections.poll()) != null) {
            closeSocket(socket);
        }
        
        currentPoolSize.set(0);
        System.out.println("Socket pool closed");
    }
    
    public int getCurrentPoolSize() {
        return currentPoolSize.get();
    }
    
    public int getAvailableConnections() {
        return availableConnections.size();
    }
    
    // 使用示例
    public static class SocketPoolExample {
        public static void main(String[] args) throws Exception {
            SocketPool pool = new SocketPool("localhost", 8080, 10, 5000);
            
            try {
                // 获取连接
                Socket socket = pool.getConnection();
                
                // 使用连接进行通信
                // ...
                
                // 归还连接
                pool.returnConnection(socket);
                
                System.out.println("Pool size: " + pool.getCurrentPoolSize());
                System.out.println("Available: " + pool.getAvailableConnections());
                
            } finally {
                pool.close();
            }
        }
    }
}
```

## 性能优化

### Socket性能优化技巧

```java
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketPerformanceOptimization {
    
    // 1. 使用缓冲区优化I/O
    public static class BufferedSocketIO {
        private final Socket socket;
        private final BufferedInputStream bufferedInput;
        private final BufferedOutputStream bufferedOutput;
        
        public BufferedSocketIO(Socket socket, int bufferSize) throws IOException {
            this.socket = socket;
            this.bufferedInput = new BufferedInputStream(
                socket.getInputStream(), bufferSize
            );
            this.bufferedOutput = new BufferedOutputStream(
                socket.getOutputStream(), bufferSize
            );
        }
        
        public void sendData(byte[] data) throws IOException {
            bufferedOutput.write(data);
            bufferedOutput.flush(); // 确保数据发送
        }
        
        public byte[] receiveData(int expectedLength) throws IOException {
            byte[] buffer = new byte[expectedLength];
            int totalRead = 0;
            
            while (totalRead < expectedLength) {
                int bytesRead = bufferedInput.read(
                    buffer, totalRead, expectedLength - totalRead
                );
                if (bytesRead == -1) {
                    throw new IOException("Connection closed unexpectedly");
                }
                totalRead += bytesRead;
            }
            
            return buffer;
        }
        
        public void close() throws IOException {
            bufferedInput.close();
            bufferedOutput.close();
            socket.close();
        }
    }
    
    // 2. 批量数据传输
    public static class BatchDataTransfer {
        private final Socket socket;
        private final DataOutputStream output;
        private final DataInputStream input;
        
        public BatchDataTransfer(Socket socket) throws IOException {
            this.socket = socket;
            this.output = new DataOutputStream(
                new BufferedOutputStream(socket.getOutputStream(), 64 * 1024)
            );
            this.input = new DataInputStream(
                new BufferedInputStream(socket.getInputStream(), 64 * 1024)
            );
        }
        
        public void sendBatch(String[] messages) throws IOException {
            // 发送批次大小
            output.writeInt(messages.length);
            
            // 发送所有消息
            for (String message : messages) {
                output.writeUTF(message);
            }
            
            output.flush();
        }
        
        public String[] receiveBatch() throws IOException {
            // 接收批次大小
            int batchSize = input.readInt();
            String[] messages = new String[batchSize];
            
            // 接收所有消息
            for (int i = 0; i < batchSize; i++) {
                messages[i] = input.readUTF();
            }
            
            return messages;
        }
        
        public void close() throws IOException {
            output.close();
            input.close();
            socket.close();
        }
    }
    
    // 3. NIO零拷贝文件传输
    public static class ZeroCopyFileTransfer {
        
        public static void sendFile(SocketChannel socketChannel, String filePath) 
                throws IOException {
            
            try (RandomAccessFile file = new RandomAccessFile(filePath, "r");
                 FileChannel fileChannel = file.getChannel()) {
                
                long fileSize = fileChannel.size();
                long position = 0;
                
                // 发送文件大小
                ByteBuffer sizeBuffer = ByteBuffer.allocate(8);
                sizeBuffer.putLong(fileSize);
                sizeBuffer.flip();
                socketChannel.write(sizeBuffer);
                
                // 零拷贝传输文件
                while (position < fileSize) {
                    long transferred = fileChannel.transferTo(
                        position, fileSize - position, socketChannel
                    );
                    position += transferred;
                }
                
                System.out.println("File sent: " + filePath + " (" + fileSize + " bytes)");
            }
        }
        
        public static void receiveFile(SocketChannel socketChannel, String outputPath) 
                throws IOException {
            
            // 接收文件大小
            ByteBuffer sizeBuffer = ByteBuffer.allocate(8);
            socketChannel.read(sizeBuffer);
            sizeBuffer.flip();
            long fileSize = sizeBuffer.getLong();
            
            try (RandomAccessFile file = new RandomAccessFile(outputPath, "rw");
                 FileChannel fileChannel = file.getChannel()) {
                
                long position = 0;
                
                // 零拷贝接收文件
                while (position < fileSize) {
                    long transferred = fileChannel.transferFrom(
                        socketChannel, position, fileSize - position
                    );
                    position += transferred;
                }
                
                System.out.println("File received: " + outputPath + " (" + fileSize + " bytes)");
            }
        }
    }
    
    // 4. 连接复用和保持活跃
    public static class ConnectionManager {
        private final String host;
        private final int port;
        private Socket socket;
        private long lastUsed;
        private final long maxIdleTime;
        
        public ConnectionManager(String host, int port, long maxIdleTime) {
            this.host = host;
            this.port = port;
            this.maxIdleTime = maxIdleTime;
        }
        
        public synchronized Socket getConnection() throws IOException {
            long currentTime = System.currentTimeMillis();
            
            if (socket == null || socket.isClosed() || 
                (currentTime - lastUsed) > maxIdleTime) {
                
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                
                socket = createOptimizedSocket();
                System.out.println("Created new optimized connection");
            }
            
            lastUsed = currentTime;
            return socket;
        }
        
        private Socket createOptimizedSocket() throws IOException {
            Socket socket = new Socket();
            
            // 优化Socket选项
            socket.setTcpNoDelay(true);           // 禁用Nagle算法
            socket.setKeepAlive(true);            // 启用保持活跃
            socket.setSendBufferSize(128 * 1024); // 128KB发送缓冲区
            socket.setReceiveBufferSize(128 * 1024); // 128KB接收缓冲区
            socket.setSoLinger(false, 0);         // 立即关闭
            socket.setReuseAddress(true);         // 地址重用
            
            // 连接超时设置
            socket.connect(new InetSocketAddress(host, port), 5000);
            socket.setSoTimeout(30000); // 30秒读取超时
            
            return socket;
        }
        
        public synchronized void close() {
            if (socket != null && !socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing connection: " + e.getMessage());
                }
            }
        }
    }
    
    // 性能测试示例
    public static void performanceTest() {
        try {
            Socket socket = new Socket("localhost", 8080);
            
            // 测试不同缓冲区大小的性能
            int[] bufferSizes = {1024, 4096, 8192, 16384, 32768, 65536};
            
            for (int bufferSize : bufferSizes) {
                long startTime = System.currentTimeMillis();
                
                BufferedSocketIO bufferedIO = new BufferedSocketIO(socket, bufferSize);
                
                // 发送测试数据
                byte[] testData = new byte[1024 * 1024]; // 1MB
                for (int i = 0; i < 100; i++) {
                    bufferedIO.sendData(testData);
                }
                
                long endTime = System.currentTimeMillis();
                System.out.println("Buffer size: " + bufferSize + 
                                 ", Time: " + (endTime - startTime) + "ms");
                
                bufferedIO.close();
            }
            
        } catch (IOException e) {
            System.err.println("Performance test error: " + e.getMessage());
        }
    }
}
```

## 常见问题

### 1. 连接超时问题

```java
public class ConnectionTimeoutHandling {
    
    public static Socket createSocketWithTimeout(String host, int port, int timeout) 
            throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeout);
        return socket;
    }
    
    public static void handleReadTimeout(Socket socket) {
        try {
            socket.setSoTimeout(10000); // 10秒读取超时
            
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            
            String line = reader.readLine(); // 可能抛出SocketTimeoutException
            System.out.println("Received: " + line);
            
        } catch (SocketTimeoutException e) {
            System.err.println("Read timeout occurred");
        } catch (IOException e) {
            System.err.println("IO error: " + e.getMessage());
        }
    }
}
```

### 2. 连接重试机制

```java
public class ConnectionRetry {
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = 1000; // 1秒
    
    public static Socket connectWithRetry(String host, int port) throws IOException {
        IOException lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                System.out.println("Connection attempt " + attempt + "/" + MAX_RETRIES);
                return new Socket(host, port);
                
            } catch (IOException e) {
                lastException = e;
                System.err.println("Attempt " + attempt + " failed: " + e.getMessage());
                
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY * attempt); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Connection interrupted", ie);
                    }
                }
            }
        }
        
        throw new IOException("Failed to connect after " + MAX_RETRIES + " attempts", lastException);
    }
}
```

### 3. 内存泄漏防护

```java
public class MemoryLeakPrevention {
    
    // 使用try-with-resources确保资源关闭
    public static void safeSocketOperation(String host, int port) {
        try (Socket socket = new Socket(host, port);
             BufferedReader reader = new BufferedReader(
                 new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(
                 socket.getOutputStream(), true)) {
            
            // 执行Socket操作
            writer.println("Hello");
            String response = reader.readLine();
            System.out.println("Response: " + response);
            
        } catch (IOException e) {
            System.err.println("Socket operation failed: " + e.getMessage());
        }
        // 资源会自动关闭
    }
    
    // 手动资源管理（不推荐）
    public static void manualResourceManagement(String host, int port) {
        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;
        
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);
            
            // 执行操作
            writer.println("Hello");
            String response = reader.readLine();
            System.out.println("Response: " + response);
            
        } catch (IOException e) {
            System.err.println("Socket operation failed: " + e.getMessage());
        } finally {
            // 手动关闭资源
            if (reader != null) {
                try { reader.close(); } catch (IOException e) { /* ignore */ }
            }
            if (writer != null) {
                writer.close();
            }
            if (socket != null) {
                try { socket.close(); } catch (IOException e) { /* ignore */ }
            }
        }
    }
}
```

### 4. 数据完整性保证

```java
public class DataIntegrityAssurance {
    
    // 确保完整读取指定长度的数据
    public static byte[] readFully(InputStream input, int length) throws IOException {
        byte[] buffer = new byte[length];
        int totalRead = 0;
        
        while (totalRead < length) {
            int bytesRead = input.read(buffer, totalRead, length - totalRead);
            if (bytesRead == -1) {
                throw new IOException("Unexpected end of stream");
            }
            totalRead += bytesRead;
        }
        
        return buffer;
    }
    
    // 带校验和的数据传输
    public static class ChecksumDataTransfer {
        private final DataOutputStream output;
        private final DataInputStream input;
        
        public ChecksumDataTransfer(Socket socket) throws IOException {
            this.output = new DataOutputStream(socket.getOutputStream());
            this.input = new DataInputStream(socket.getInputStream());
        }
        
        public void sendData(byte[] data) throws IOException {
            // 计算校验和
            long checksum = calculateChecksum(data);
            
            // 发送数据长度、校验和和数据
            output.writeInt(data.length);
            output.writeLong(checksum);
            output.write(data);
            output.flush();
        }
        
        public byte[] receiveData() throws IOException {
            // 接收数据长度和校验和
            int length = input.readInt();
            long expectedChecksum = input.readLong();
            
            // 接收数据
            byte[] data = readFully(input, length);
            
            // 验证校验和
            long actualChecksum = calculateChecksum(data);
            if (actualChecksum != expectedChecksum) {
                throw new IOException("Data corruption detected");
            }
            
            return data;
        }
        
        private long calculateChecksum(byte[] data) {
            long checksum = 0;
            for (byte b : data) {
                checksum += b & 0xFF;
            }
            return checksum;
        }
    }
}
```

## 最佳实践

### 1. 资源管理
- **使用try-with-resources**：确保Socket和流资源正确关闭
- **设置超时**：避免无限等待
- **连接池**：复用连接，减少创建开销

### 2. 性能优化
- **缓冲区大小**：根据数据量调整缓冲区大小
- **TCP_NODELAY**：对于小数据包，禁用Nagle算法
- **批量传输**：减少网络往返次数

### 3. 错误处理
- **重试机制**：处理临时网络故障
- **超时处理**：避免程序挂起
- **优雅关闭**：正确处理连接关闭

### 4. 安全考虑
- **输入验证**：验证接收到的数据
- **资源限制**：防止资源耗尽攻击
- **加密传输**：敏感数据使用SSL/TLS

## 面试要点

### 高频问题

1. **TCP和UDP的区别**
   - TCP：面向连接、可靠传输、有序、流控制
   - UDP：无连接、不可靠、无序、开销小

2. **Socket的工作原理**
   - 客户端创建Socket连接服务器
   - 服务器监听端口，接受连接
   - 通过输入输出流进行数据交换

3. **如何处理并发连接**
   - 多线程：每个连接一个线程
   - 线程池：限制线程数量
   - NIO：单线程处理多个连接

### 深入问题

1. **NIO与传统IO的区别**
   - 阻塞vs非阻塞
   - 面向流vs面向缓冲区
   - 选择器机制

2. **Socket选项的作用**
   - TCP_NODELAY：禁用Nagle算法
   - SO_KEEPALIVE：保持连接活跃
   - SO_TIMEOUT：设置超时时间

3. **如何优化Socket性能**
   - 调整缓冲区大小
   - 使用连接池
   - 批量数据传输
   - 零拷贝技术

### 实践经验

1. **网络编程中遇到的问题**
   - 连接超时
   - 数据丢失
   - 内存泄漏
   - 并发处理

2. **性能调优经验**
   - 监控网络指标
   - 压力测试
   - 资源使用优化

3. **故障排查方法**
   - 网络抓包分析
   - 日志分析
   - 性能监控

## 总结

Socket编程是Java网络编程的基础，掌握以下要点：

1. **基础概念**：理解TCP/UDP协议差异
2. **编程模型**：掌握阻塞IO和NIO编程
3. **性能优化**：合理配置Socket选项和缓冲区
4. **并发处理**：选择合适的并发模型
5. **错误处理**：实现健壮的错误处理机制
6. **最佳实践**：遵循资源管理和安全编程原则

通过实践项目加深理解，如聊天室、文件传输、简单HTTP服务器等。