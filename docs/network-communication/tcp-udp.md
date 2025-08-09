# TCP/UDP协议详解

## 目录
- [协议概述](#协议概述)
- [TCP协议](#tcp协议)
- [UDP协议](#udp协议)
- [TCP vs UDP对比](#tcp-vs-udp对比)
- [Java网络编程实现](#java网络编程实现)
- [性能优化](#性能优化)
- [常见问题](#常见问题)
- [面试要点](#面试要点)

## 协议概述

### 传输层协议
传输层是OSI七层模型的第四层，主要负责端到端的数据传输。主要协议包括：
- **TCP (Transmission Control Protocol)**: 面向连接的可靠传输协议
- **UDP (User Datagram Protocol)**: 无连接的不可靠传输协议

### 端口概念
- **端口号范围**: 0-65535
- **知名端口**: 0-1023 (HTTP:80, HTTPS:443, FTP:21, SSH:22)
- **注册端口**: 1024-49151
- **动态端口**: 49152-65535

## TCP协议

### 特点
1. **面向连接**: 通信前需要建立连接
2. **可靠传输**: 保证数据完整性和顺序
3. **流量控制**: 防止发送方发送过快
4. **拥塞控制**: 防止网络拥塞
5. **全双工通信**: 双方可同时发送和接收数据

### TCP报文格式
```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|          Source Port          |       Destination Port        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                        Sequence Number                        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|                    Acknowledgment Number                      |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|  Data |           |U|A|P|R|S|F|                               |
| Offset| Reserved  |R|C|S|S|Y|I|            Window             |
|       |           |G|K|H|T|N|N|                               |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
|           Checksum            |         Urgent Pointer        |
+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
```

### 三次握手 (Three-Way Handshake)
```
客户端                                    服务器
   |                                        |
   |  1. SYN (seq=x)                       |
   |--------------------------------------->|
   |                                        |
   |  2. SYN+ACK (seq=y, ack=x+1)         |
   |<---------------------------------------|
   |                                        |
   |  3. ACK (seq=x+1, ack=y+1)           |
   |--------------------------------------->|
   |                                        |
   |        连接建立完成                     |
```

**为什么需要三次握手？**
1. 确认双方的发送和接收能力
2. 防止已失效的连接请求报文段突然又传送到服务器
3. 同步双方的初始序列号

### 四次挥手 (Four-Way Handshake)
```
客户端                                    服务器
   |                                        |
   |  1. FIN (seq=u)                       |
   |--------------------------------------->|
   |                                        |
   |  2. ACK (ack=u+1)                     |
   |<---------------------------------------|
   |                                        |
   |  3. FIN (seq=v)                       |
   |<---------------------------------------|
   |                                        |
   |  4. ACK (ack=v+1)                     |
   |--------------------------------------->|
   |                                        |
   |        连接关闭完成                     |
```

**为什么需要四次挥手？**
- TCP是全双工协议，需要分别关闭两个方向的连接
- 服务器收到FIN后，可能还有数据要发送，所以ACK和FIN分开发送

### TCP状态转换图
```
CLOSED -> LISTEN (服务器监听)
CLOSED -> SYN_SENT (客户端发起连接)
LISTEN -> SYN_RCVD (服务器收到SYN)
SYN_SENT -> ESTABLISHED (客户端收到SYN+ACK)
SYN_RCVD -> ESTABLISHED (服务器收到ACK)
ESTABLISHED -> FIN_WAIT_1 (主动关闭)
ESTABLISHED -> CLOSE_WAIT (被动关闭)
...
```

### 流量控制
- **滑动窗口机制**: 接收方通过窗口大小控制发送方的发送速率
- **零窗口**: 接收方缓冲区满时，发送窗口大小为0
- **窗口探测**: 发送方定期发送探测报文检查窗口状态

### 拥塞控制
1. **慢启动**: 指数增长发送窗口
2. **拥塞避免**: 线性增长发送窗口
3. **快重传**: 收到3个重复ACK立即重传
4. **快恢复**: 快重传后进入拥塞避免阶段

## UDP协议

### 特点
1. **无连接**: 发送数据前不需要建立连接
2. **不可靠**: 不保证数据到达和顺序
3. **面向报文**: 保持应用层数据边界
4. **开销小**: 头部只有8字节
5. **支持广播和多播**

### UDP报文格式
```
 0      7 8     15 16    23 24    31
+--------+--------+--------+--------+
|     Source      |   Destination   |
|      Port       |      Port       |
+--------+--------+--------+--------+
|                 |                 |
|     Length      |    Checksum     |
+--------+--------+--------+--------+
|                                   |
|              Data                 |
+-----------------------------------+
```

### 适用场景
- **实时应用**: 视频直播、在线游戏
- **简单查询**: DNS查询
- **广播通信**: 网络发现协议
- **文件传输**: TFTP

## TCP vs UDP对比

| 特性 | TCP | UDP |
|------|-----|-----|
| 连接性 | 面向连接 | 无连接 |
| 可靠性 | 可靠传输 | 不可靠传输 |
| 速度 | 较慢 | 较快 |
| 头部开销 | 20字节 | 8字节 |
| 流量控制 | 有 | 无 |
| 拥塞控制 | 有 | 无 |
| 数据边界 | 字节流 | 保持报文边界 |
| 广播支持 | 不支持 | 支持 |
| 应用场景 | 文件传输、网页浏览 | 视频直播、游戏 |

## Java网络编程实现

### TCP编程示例

#### 服务器端
```java
public class TCPServer {
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(8080)) {
            System.out.println("服务器启动，监听端口8080");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // 为每个客户端创建新线程
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
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
                System.out.println("收到: " + inputLine);
                out.println("Echo: " + inputLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

#### 客户端
```java
public class TCPClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8080);
             PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
             BufferedReader stdIn = new BufferedReader(
                new InputStreamReader(System.in))) {
            
            String userInput;
            while ((userInput = stdIn.readLine()) != null) {
                out.println(userInput);
                System.out.println("服务器响应: " + in.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### UDP编程示例

#### 服务器端
```java
public class UDPServer {
    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket(8080)) {
            System.out.println("UDP服务器启动，监听端口8080");
            
            byte[] buffer = new byte[1024];
            
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("收到: " + received);
                
                // 回复客户端
                String response = "Echo: " + received;
                byte[] responseData = response.getBytes();
                DatagramPacket responsePacket = new DatagramPacket(
                    responseData, responseData.length,
                    packet.getAddress(), packet.getPort());
                socket.send(responsePacket);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

#### 客户端
```java
public class UDPClient {
    public static void main(String[] args) {
        try (DatagramSocket socket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {
            
            InetAddress serverAddress = InetAddress.getByName("localhost");
            
            while (scanner.hasNextLine()) {
                String message = scanner.nextLine();
                byte[] data = message.getBytes();
                
                // 发送数据
                DatagramPacket packet = new DatagramPacket(
                    data, data.length, serverAddress, 8080);
                socket.send(packet);
                
                // 接收响应
                byte[] buffer = new byte[1024];
                DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(responsePacket);
                
                String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                System.out.println("服务器响应: " + response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

## 性能优化

### TCP优化
1. **Nagle算法**: 减少小包发送
2. **TCP_NODELAY**: 禁用Nagle算法，适用于实时应用
3. **SO_KEEPALIVE**: 保持连接活跃
4. **缓冲区大小**: 调整发送和接收缓冲区

```java
// 设置TCP选项
socket.setTcpNoDelay(true);  // 禁用Nagle算法
socket.setKeepAlive(true);   // 启用保活机制
socket.setSendBufferSize(64 * 1024);    // 设置发送缓冲区
socket.setReceiveBufferSize(64 * 1024); // 设置接收缓冲区
```

### UDP优化
1. **缓冲区大小**: 增大接收缓冲区防止丢包
2. **批量处理**: 一次处理多个数据包
3. **多线程**: 分离接收和处理逻辑

```java
// 设置UDP缓冲区
socket.setReceiveBufferSize(256 * 1024);
socket.setSendBufferSize(256 * 1024);
```

## 常见问题

### TCP常见问题
1. **TIME_WAIT状态过多**: 调整系统参数或使用连接池
2. **连接超时**: 设置合适的超时时间
3. **粘包/拆包**: 定义消息边界或使用固定长度

### UDP常见问题
1. **数据包丢失**: 实现重传机制
2. **数据包乱序**: 添加序列号
3. **缓冲区溢出**: 增大接收缓冲区

## 面试要点

### 高频问题
1. **TCP和UDP的区别？**
   - 连接性、可靠性、速度、开销等方面对比

2. **TCP三次握手的过程？**
   - 详细描述握手过程和状态变化

3. **为什么TCP需要三次握手？**
   - 确认双方能力、防止失效连接、同步序列号

4. **TCP四次挥手的过程？**
   - 详细描述挥手过程和TIME_WAIT状态

5. **TCP如何保证可靠传输？**
   - 序列号、确认应答、重传机制、流量控制、拥塞控制

6. **什么是粘包和拆包？如何解决？**
   - 原因分析和解决方案

7. **UDP适用于什么场景？**
   - 实时性要求高、可以容忍数据丢失的场景

### 深入问题
1. **TCP的拥塞控制算法？**
2. **TCP的滑动窗口机制？**
3. **如何优化TCP性能？**
4. **网络编程中的IO模型？**
5. **如何处理网络异常？**

### 实践经验
- 了解常用的网络调试工具：netstat、tcpdump、wireshark
- 熟悉网络编程的最佳实践
- 理解不同IO模型的适用场景
- 掌握网络性能调优方法