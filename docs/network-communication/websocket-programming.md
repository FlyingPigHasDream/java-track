# WebSocket编程详解

## 目录
- [WebSocket概述](#websocket概述)
- [WebSocket协议](#websocket协议)
- [Java WebSocket实现](#java-websocket实现)
- [Spring WebSocket](#spring-websocket)
- [Netty WebSocket](#netty-websocket)
- [实时应用开发](#实时应用开发)
- [性能优化](#性能优化)
- [安全考虑](#安全考虑)
- [最佳实践](#最佳实践)
- [面试要点](#面试要点)

## WebSocket概述

### 什么是WebSocket
WebSocket是一种在单个TCP连接上进行全双工通信的协议。它使得客户端和服务器之间的数据交换变得更加简单，允许服务端主动向客户端推送数据。

### WebSocket的特点
1. **全双工通信**：客户端和服务器可以同时发送数据
2. **持久连接**：连接建立后保持开放状态
3. **低延迟**：无需HTTP请求/响应的开销
4. **跨域支持**：支持跨域通信
5. **协议升级**：从HTTP协议升级而来

### 应用场景
- **实时聊天**：即时消息应用
- **在线游戏**：实时游戏状态同步
- **股票行情**：实时数据推送
- **协作编辑**：多人实时编辑文档
- **监控系统**：实时状态监控
- **直播弹幕**：实时评论显示

### WebSocket vs HTTP

| 特性 | HTTP | WebSocket |
|------|------|----------|
| 连接类型 | 短连接 | 长连接 |
| 通信方式 | 请求-响应 | 全双工 |
| 协议开销 | 较大 | 较小 |
| 实时性 | 差 | 好 |
| 服务器推送 | 不支持 | 支持 |
| 状态 | 无状态 | 有状态 |

## WebSocket协议

### 握手过程

#### 1. 客户端握手请求
```http
GET /websocket HTTP/1.1
Host: example.com
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
```

#### 2. 服务器握手响应
```http
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
```

### 数据帧格式
```
 0                   1                   2                   3
 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
+-+-+-+-+-------+-+-------------+-------------------------------+
|F|R|R|R| opcode|M| Payload len |    Extended payload length    |
|I|S|S|S|  (4)  |A|     (7)     |             (16/64)           |
|N|V|V|V|       |S|             |   (if payload len==126/127)   |
| |1|2|3|       |K|             |                               |
+-+-+-+-+-------+-+-------------+ - - - - - - - - - - - - - - - +
|     Extended payload length continued, if payload len == 127  |
+ - - - - - - - - - - - - - - - +-------------------------------+
|                               |Masking-key, if MASK set to 1  |
+-------------------------------+-------------------------------+
| Masking-key (continued)       |          Payload Data         |
+-------------------------------- - - - - - - - - - - - - - - - +
:                     Payload Data continued ...                :
+ - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - +
|                     Payload Data continued ...                |
+---------------------------------------------------------------+
```

### 操作码类型
- **0x0**：继续帧
- **0x1**：文本帧
- **0x2**：二进制帧
- **0x8**：关闭帧
- **0x9**：Ping帧
- **0xA**：Pong帧

## Java WebSocket实现

### 1. Java API for WebSocket (JSR-356)

#### 服务器端点

```java
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ServerEndpoint("/websocket")
public class WebSocketServer {
    
    // 存储所有连接的会话
    private static Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        System.out.println("新连接建立: " + session.getId());
        
        // 发送欢迎消息
        try {
            session.getBasicRemote().sendText("欢迎连接WebSocket服务器！");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("收到消息: " + message + " 来自: " + session.getId());
        
        // 广播消息给所有连接的客户端
        broadcast("用户 " + session.getId() + ": " + message);
    }
    
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        sessions.remove(session);
        System.out.println("连接关闭: " + session.getId() + ", 原因: " + closeReason.getReasonPhrase());
        
        // 通知其他用户
        broadcast("用户 " + session.getId() + " 离开了聊天室");
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket错误: " + throwable.getMessage());
        throwable.printStackTrace();
    }
    
    // 广播消息给所有连接的客户端
    private void broadcast(String message) {
        synchronized (sessions) {
            for (Session session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    // 发送消息给特定客户端
    public static void sendToSession(String sessionId, String message) {
        synchronized (sessions) {
            for (Session session : sessions) {
                if (session.getId().equals(sessionId) && session.isOpen()) {
                    try {
                        session.getBasicRemote().sendText(message);
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
```

#### 客户端实现

```java
import javax.websocket.*;
import java.net.URI;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

@ClientEndpoint
public class WebSocketClient {
    
    private Session session;
    private CountDownLatch latch = new CountDownLatch(1);
    
    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("连接到WebSocket服务器成功");
        latch.countDown();
    }
    
    @OnMessage
    public void onMessage(String message) {
        System.out.println("收到服务器消息: " + message);
    }
    
    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("连接关闭: " + closeReason.getReasonPhrase());
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket错误: " + throwable.getMessage());
    }
    
    public void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static void main(String[] args) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            WebSocketClient client = new WebSocketClient();
            
            // 连接到WebSocket服务器
            Session session = container.connectToServer(client, new URI("ws://localhost:8080/websocket"));
            
            // 等待连接建立
            client.latch.await();
            
            // 发送消息
            Scanner scanner = new Scanner(System.in);
            System.out.println("输入消息（输入'quit'退出）:");
            
            while (scanner.hasNextLine()) {
                String message = scanner.nextLine();
                if ("quit".equalsIgnoreCase(message)) {
                    break;
                }
                client.sendMessage(message);
            }
            
            scanner.close();
            session.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 2. 配置WebSocket服务器

```java
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;
import java.util.HashSet;
import java.util.Set;

public class WebSocketConfig implements ServerApplicationConfig {
    
    @Override
    public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
        return new HashSet<>();
    }
    
    @Override
    public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
        Set<Class<?>> endpoints = new HashSet<>();
        endpoints.add(WebSocketServer.class);
        return endpoints;
    }
}
```

## Spring WebSocket

### 1. Spring WebSocket配置

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册WebSocket处理器
        registry.addHandler(new ChatWebSocketHandler(), "/chat")
                .setAllowedOrigins("*"); // 允许跨域
    }
}
```

### 2. Spring WebSocket处理器

```java
import org.springframework.web.socket.*;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

public class ChatWebSocketHandler implements WebSocketHandler {
    
    // 存储所有WebSocket会话
    private static CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("新连接建立: " + session.getId());
        
        // 发送欢迎消息
        session.sendMessage(new TextMessage("欢迎加入聊天室！"));
        
        // 通知其他用户
        broadcast("用户 " + session.getId() + " 加入了聊天室", session);
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            System.out.println("收到消息: " + payload + " 来自: " + session.getId());
            
            // 广播消息
            broadcast(session.getId() + ": " + payload, null);
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("传输错误: " + exception.getMessage());
        sessions.remove(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        sessions.remove(session);
        System.out.println("连接关闭: " + session.getId());
        
        // 通知其他用户
        broadcast("用户 " + session.getId() + " 离开了聊天室", null);
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    // 广播消息
    private void broadcast(String message, WebSocketSession excludeSession) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen() && !session.equals(excludeSession)) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
```

### 3. STOMP协议支持

```java
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理
        config.enableSimpleBroker("/topic", "/queue");
        // 设置应用程序目的地前缀
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点
        registry.addEndpoint("/ws")
                .setAllowedOrigins("*")
                .withSockJS(); // 启用SockJS支持
    }
}
```

### 4. STOMP消息控制器

```java
import org.springframework.messaging.handler.annotation.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.beans.factory.annotation.Autowired;

@Controller
public class ChatController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // 处理发送到/app/chat的消息
    @MessageMapping("/chat")
    @SendTo("/topic/messages")
    public ChatMessage sendMessage(ChatMessage message) {
        // 处理消息并广播给所有订阅者
        message.setTimestamp(System.currentTimeMillis());
        return message;
    }
    
    // 处理私聊消息
    @MessageMapping("/private")
    public void sendPrivateMessage(@Payload ChatMessage message, 
                                 @Header("simpSessionId") String sessionId) {
        // 发送私聊消息给特定用户
        messagingTemplate.convertAndSendToUser(
            message.getTo(), "/queue/private", 
            message
        );
    }
    
    // 用户连接事件
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        System.out.println("新用户连接: " + event.getMessage());
    }
    
    // 用户断开事件
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        System.out.println("用户断开连接: " + event.getSessionId());
        
        // 通知其他用户
        ChatMessage message = new ChatMessage();
        message.setType("LEAVE");
        message.setSender("System");
        message.setContent("用户离开了聊天室");
        
        messagingTemplate.convertAndSend("/topic/messages", message);
    }
}

// 消息实体类
class ChatMessage {
    private String type;
    private String content;
    private String sender;
    private String to;
    private long timestamp;
    
    // 构造函数、getter和setter方法
    public ChatMessage() {}
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    
    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
```

## Netty WebSocket

### 1. Netty WebSocket服务器

```java
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;

public class NettyWebSocketServer {
    
    private final int port;
    
    public NettyWebSocketServer(int port) {
        this.port = port;
    }
    
    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new WebSocketServerInitializer())
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            
            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.println("WebSocket服务器启动，监听端口: " + port);
            
            future.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) throws Exception {
        new NettyWebSocketServer(8080).start();
    }
}

class WebSocketServerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        
        // HTTP编解码器
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new ChunkedWriteHandler());
        
        // WebSocket处理器
        pipeline.addLast(new WebSocketServerHandler());
    }
}

class WebSocketServerHandler extends SimpleChannelInboundHandler<Object> {
    
    private WebSocketServerHandshaker handshaker;
    private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebSocketFrame(ctx, (WebSocketFrame) msg);
        }
    }
    
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
        // 检查是否为WebSocket升级请求
        if (!req.decoderResult().isSuccess() || 
            !"websocket".equals(req.headers().get("Upgrade"))) {
            sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        
        // 创建WebSocket握手
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                "ws://localhost:8080/websocket", null, false);
        handshaker = wsFactory.newHandshaker(req);
        
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
            channels.add(ctx.channel());
            System.out.println("WebSocket连接建立: " + ctx.channel().remoteAddress());
        }
    }
    
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        // 处理关闭帧
        if (frame instanceof CloseWebSocketFrame) {
            handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
            return;
        }
        
        // 处理Ping帧
        if (frame instanceof PingWebSocketFrame) {
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        
        // 处理文本帧
        if (frame instanceof TextWebSocketFrame) {
            String text = ((TextWebSocketFrame) frame).text();
            System.out.println("收到消息: " + text);
            
            // 广播消息给所有连接的客户端
            broadcast("Echo: " + text);
        }
    }
    
    private void broadcast(String message) {
        TextWebSocketFrame frame = new TextWebSocketFrame(message);
        channels.writeAndFlush(frame);
    }
    
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest req, FullHttpResponse res) {
        if (res.status().code() != 200) {
            ByteBuf buf = Unpooled.copiedBuffer(res.status().toString(), CharsetUtil.UTF_8);
            res.content().writeBytes(buf);
            buf.release();
            HttpUtil.setContentLength(res, res.content().readableBytes());
        }
        
        ChannelFuture f = ctx.channel().writeAndFlush(res);
        if (!HttpUtil.isKeepAlive(req) || res.status().code() != 200) {
            f.addListener(ChannelFutureListener.CLOSE);
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        channels.remove(ctx.channel());
        System.out.println("WebSocket连接关闭: " + ctx.channel().remoteAddress());
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
```

## 实时应用开发

### 1. 实时股票行情推送

```java
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StockPriceService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, Double> stockPrices = new ConcurrentHashMap<>();
    private final Random random = new Random();
    
    public StockPriceService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        
        // 初始化股票价格
        stockPrices.put("AAPL", 150.0);
        stockPrices.put("GOOGL", 2800.0);
        stockPrices.put("MSFT", 300.0);
        stockPrices.put("TSLA", 800.0);
    }
    
    @Scheduled(fixedRate = 1000) // 每秒更新一次
    public void updateStockPrices() {
        for (String symbol : stockPrices.keySet()) {
            double currentPrice = stockPrices.get(symbol);
            // 随机变化 -2% 到 +2%
            double change = (random.nextDouble() - 0.5) * 0.04;
            double newPrice = currentPrice * (1 + change);
            
            stockPrices.put(symbol, newPrice);
            
            // 推送价格更新
            StockPrice stockPrice = new StockPrice(symbol, newPrice, change);
            messagingTemplate.convertAndSend("/topic/stocks/" + symbol, stockPrice);
        }
    }
}

class StockPrice {
    private String symbol;
    private double price;
    private double change;
    private long timestamp;
    
    public StockPrice(String symbol, double price, double change) {
        this.symbol = symbol;
        this.price = price;
        this.change = change;
        this.timestamp = System.currentTimeMillis();
    }
    
    // getter和setter方法
    public String getSymbol() { return symbol; }
    public double getPrice() { return price; }
    public double getChange() { return change; }
    public long getTimestamp() { return timestamp; }
}
```

### 2. 实时协作编辑器

```java
@Controller
public class CollaborativeEditorController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, Document> documents = new ConcurrentHashMap<>();
    
    public CollaborativeEditorController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    @MessageMapping("/document/{documentId}/edit")
    public void handleEdit(@DestinationVariable String documentId, 
                          @Payload EditOperation operation,
                          @Header("simpSessionId") String sessionId) {
        
        // 获取或创建文档
        Document document = documents.computeIfAbsent(documentId, k -> new Document());
        
        // 应用编辑操作
        document.applyOperation(operation);
        
        // 广播编辑操作给其他用户（排除发送者）
        operation.setUserId(sessionId);
        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/edits", operation);
    }
    
    @MessageMapping("/document/{documentId}/cursor")
    public void handleCursorMove(@DestinationVariable String documentId,
                               @Payload CursorPosition cursor,
                               @Header("simpSessionId") String sessionId) {
        
        cursor.setUserId(sessionId);
        // 广播光标位置给其他用户
        messagingTemplate.convertAndSend("/topic/document/" + documentId + "/cursors", cursor);
    }
}

class Document {
    private StringBuilder content = new StringBuilder();
    private int version = 0;
    
    public synchronized void applyOperation(EditOperation operation) {
        switch (operation.getType()) {
            case "INSERT":
                content.insert(operation.getPosition(), operation.getText());
                break;
            case "DELETE":
                content.delete(operation.getPosition(), 
                             operation.getPosition() + operation.getLength());
                break;
        }
        version++;
    }
    
    public String getContent() { return content.toString(); }
    public int getVersion() { return version; }
}

class EditOperation {
    private String type;
    private int position;
    private String text;
    private int length;
    private String userId;
    
    // 构造函数和getter/setter方法
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    
    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}

class CursorPosition {
    private int line;
    private int column;
    private String userId;
    
    // 构造函数和getter/setter方法
    public int getLine() { return line; }
    public void setLine(int line) { this.line = line; }
    
    public int getColumn() { return column; }
    public void setColumn(int column) { this.column = column; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
```

### 3. 实时游戏状态同步

```java
@Controller
public class GameController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentHashMap<String, GameRoom> gameRooms = new ConcurrentHashMap<>();
    
    public GameController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }
    
    @MessageMapping("/game/{roomId}/join")
    public void joinGame(@DestinationVariable String roomId,
                        @Payload Player player,
                        @Header("simpSessionId") String sessionId) {
        
        GameRoom room = gameRooms.computeIfAbsent(roomId, k -> new GameRoom(roomId));
        player.setSessionId(sessionId);
        room.addPlayer(player);
        
        // 通知房间内所有玩家
        messagingTemplate.convertAndSend("/topic/game/" + roomId + "/players", room.getPlayers());
        messagingTemplate.convertAndSend("/topic/game/" + roomId + "/events", 
                                        new GameEvent("PLAYER_JOINED", player.getName() + " 加入游戏"));
    }
    
    @MessageMapping("/game/{roomId}/move")
    public void playerMove(@DestinationVariable String roomId,
                          @Payload PlayerMove move,
                          @Header("simpSessionId") String sessionId) {
        
        GameRoom room = gameRooms.get(roomId);
        if (room != null) {
            move.setSessionId(sessionId);
            room.processMove(move);
            
            // 广播移动给房间内所有玩家
            messagingTemplate.convertAndSend("/topic/game/" + roomId + "/moves", move);
            
            // 如果游戏状态发生变化，广播新状态
            if (room.isStateChanged()) {
                messagingTemplate.convertAndSend("/topic/game/" + roomId + "/state", room.getGameState());
            }
        }
    }
    
    @EventListener
    public void handlePlayerDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        
        // 从所有游戏房间中移除断开连接的玩家
        for (GameRoom room : gameRooms.values()) {
            Player removedPlayer = room.removePlayerBySession(sessionId);
            if (removedPlayer != null) {
                messagingTemplate.convertAndSend("/topic/game/" + room.getRoomId() + "/players", room.getPlayers());
                messagingTemplate.convertAndSend("/topic/game/" + room.getRoomId() + "/events",
                                                new GameEvent("PLAYER_LEFT", removedPlayer.getName() + " 离开游戏"));
            }
        }
    }
}

class GameRoom {
    private String roomId;
    private List<Player> players = new CopyOnWriteArrayList<>();
    private GameState gameState = new GameState();
    private boolean stateChanged = false;
    
    public GameRoom(String roomId) {
        this.roomId = roomId;
    }
    
    public void addPlayer(Player player) {
        players.add(player);
        stateChanged = true;
    }
    
    public Player removePlayerBySession(String sessionId) {
        for (Player player : players) {
            if (sessionId.equals(player.getSessionId())) {
                players.remove(player);
                stateChanged = true;
                return player;
            }
        }
        return null;
    }
    
    public void processMove(PlayerMove move) {
        // 处理玩家移动逻辑
        gameState.updatePlayerPosition(move.getSessionId(), move.getX(), move.getY());
        stateChanged = true;
    }
    
    // getter方法
    public String getRoomId() { return roomId; }
    public List<Player> getPlayers() { return new ArrayList<>(players); }
    public GameState getGameState() { 
        stateChanged = false;
        return gameState; 
    }
    public boolean isStateChanged() { return stateChanged; }
}

class Player {
    private String name;
    private String sessionId;
    private int x, y;
    
    // 构造函数和getter/setter方法
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
}

class PlayerMove {
    private int x, y;
    private String sessionId;
    
    // getter/setter方法
    public int getX() { return x; }
    public void setX(int x) { this.x = x; }
    
    public int getY() { return y; }
    public void setY(int y) { this.y = y; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}

class GameState {
    private Map<String, PlayerPosition> playerPositions = new ConcurrentHashMap<>();
    
    public void updatePlayerPosition(String sessionId, int x, int y) {
        playerPositions.put(sessionId, new PlayerPosition(x, y));
    }
    
    public Map<String, PlayerPosition> getPlayerPositions() {
        return new HashMap<>(playerPositions);
    }
}

class PlayerPosition {
    private int x, y;
    
    public PlayerPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
}

class GameEvent {
    private String type;
    private String message;
    private long timestamp;
    
    public GameEvent(String type, String message) {
        this.type = type;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
    
    // getter方法
    public String getType() { return type; }
    public String getMessage() { return message; }
    public long getTimestamp() { return timestamp; }
}
```

## 性能优化

### 1. 连接管理优化

```java
@Component
public class WebSocketConnectionManager {
    
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    @PostConstruct
    public void init() {
        // 定期清理无效连接
        scheduler.scheduleAtFixedRate(this::cleanupInactiveSessions, 30, 30, TimeUnit.SECONDS);
        
        // 定期发送心跳
        scheduler.scheduleAtFixedRate(this::sendHeartbeat, 60, 60, TimeUnit.SECONDS);
    }
    
    public void addSession(String sessionId, WebSocketSession session) {
        sessions.put(sessionId, session);
    }
    
    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }
    
    private void cleanupInactiveSessions() {
        sessions.entrySet().removeIf(entry -> {
            WebSocketSession session = entry.getValue();
            if (!session.isOpen()) {
                System.out.println("清理无效连接: " + entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    private void sendHeartbeat() {
        TextMessage heartbeat = new TextMessage("{\"type\":\"heartbeat\"}");
        sessions.values().parallelStream()
                .filter(WebSocketSession::isOpen)
                .forEach(session -> {
                    try {
                        session.sendMessage(heartbeat);
                    } catch (Exception e) {
                        System.err.println("发送心跳失败: " + e.getMessage());
                    }
                });
    }
    
    public int getActiveConnectionCount() {
        return (int) sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }
    
    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
```

### 2. 消息批处理

```java
@Service
public class MessageBatchProcessor {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final BlockingQueue<BatchMessage> messageQueue = new LinkedBlockingQueue<>();
    private final ScheduledExecutorService batchProcessor = Executors.newSingleThreadScheduledExecutor();
    
    public MessageBatchProcessor(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        startBatchProcessing();
    }
    
    public void addMessage(String destination, Object message) {
        messageQueue.offer(new BatchMessage(destination, message));
    }
    
    private void startBatchProcessing() {
        batchProcessor.scheduleAtFixedRate(() -> {
            List<BatchMessage> batch = new ArrayList<>();
            messageQueue.drainTo(batch, 100); // 每次最多处理100条消息
            
            if (!batch.isEmpty()) {
                processBatch(batch);
            }
        }, 0, 10, TimeUnit.MILLISECONDS); // 每10ms处理一次
    }
    
    private void processBatch(List<BatchMessage> batch) {
        // 按目标地址分组
        Map<String, List<Object>> groupedMessages = batch.stream()
                .collect(Collectors.groupingBy(
                    BatchMessage::getDestination,
                    Collectors.mapping(BatchMessage::getMessage, Collectors.toList())
                ));
        
        // 批量发送
        groupedMessages.forEach((destination, messages) -> {
            if (messages.size() == 1) {
                messagingTemplate.convertAndSend(destination, messages.get(0));
            } else {
                // 发送批量消息
                messagingTemplate.convertAndSend(destination, 
                    Map.of("type", "batch", "messages", messages));
            }
        });
    }
    
    private static class BatchMessage {
        private final String destination;
        private final Object message;
        
        public BatchMessage(String destination, Object message) {
            this.destination = destination;
            this.message = message;
        }
        
        public String getDestination() { return destination; }
        public Object getMessage() { return message; }
    }
}
```

### 3. 内存优化

```java
@Configuration
public class WebSocketMemoryConfig {
    
    @Bean
    public TaskScheduler webSocketTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(Runtime.getRuntime().availableProcessors());
        scheduler.setThreadNamePrefix("websocket-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        return scheduler;
    }
    
    @Bean
    public WebSocketConfigurer webSocketConfigurer() {
        return new WebSocketConfigurer() {
            @Override
            public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
                registry.addHandler(new OptimizedWebSocketHandler(), "/ws")
                        .setAllowedOrigins("*")
                        .addInterceptors(new WebSocketInterceptor());
            }
        };
    }
}

class OptimizedWebSocketHandler implements WebSocketHandler {
    
    // 使用弱引用避免内存泄漏
    private final Set<WeakReference<WebSocketSession>> sessions = 
            Collections.synchronizedSet(new HashSet<>());
    
    // 消息池，重用消息对象
    private final ObjectPool<TextMessage> messagePool = new GenericObjectPool<>(
            new BasePooledObjectFactory<TextMessage>() {
                @Override
                public TextMessage create() {
                    return new TextMessage("");
                }
                
                @Override
                public PooledObject<TextMessage> wrap(TextMessage obj) {
                    return new DefaultPooledObject<>(obj);
                }
            });
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(new WeakReference<>(session));
        
        // 设置会话属性以优化内存使用
        session.getAttributes().put("lastActivity", System.currentTimeMillis());
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) {
        // 更新最后活动时间
        session.getAttributes().put("lastActivity", System.currentTimeMillis());
        
        // 处理消息...
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) {
        // 清理会话引用
        sessions.removeIf(ref -> {
            WebSocketSession s = ref.get();
            return s == null || s.equals(session);
        });
    }
    
    // 定期清理无效引用
    @Scheduled(fixedRate = 60000)
    public void cleanupWeakReferences() {
        sessions.removeIf(ref -> ref.get() == null);
    }
}
```

### 4. 负载均衡和集群支持

```java
@Configuration
@EnableRedisWebSocketSession
public class ClusteredWebSocketConfig {
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setDefaultSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }
    
    @Bean
    public MessageBroker messageBroker(RedisTemplate<String, Object> redisTemplate) {
        return new RedisMessageBroker(redisTemplate);
    }
}

@Service
public class RedisMessageBroker {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    
    public RedisMessageBroker(RedisTemplate<String, Object> redisTemplate,
                             SimpMessagingTemplate messagingTemplate) {
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        
        // 订阅Redis消息
        subscribeToRedisMessages();
    }
    
    public void broadcastToCluster(String destination, Object message) {
        // 发送到Redis，其他节点会接收到
        ClusterMessage clusterMessage = new ClusterMessage(destination, message);
        redisTemplate.convertAndSend("websocket:broadcast", clusterMessage);
    }
    
    private void subscribeToRedisMessages() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisTemplate.getConnectionFactory());
        
        container.addMessageListener((message, pattern) -> {
            try {
                ClusterMessage clusterMessage = (ClusterMessage) redisTemplate
                        .getDefaultSerializer().deserialize(message.getBody());
                
                // 转发到本地WebSocket连接
                messagingTemplate.convertAndSend(
                    clusterMessage.getDestination(), 
                    clusterMessage.getMessage()
                );
            } catch (Exception e) {
                System.err.println("处理集群消息失败: " + e.getMessage());
            }
        }, new ChannelTopic("websocket:broadcast"));
        
        container.start();
    }
    
    private static class ClusterMessage {
        private String destination;
        private Object message;
        
        public ClusterMessage() {}
        
        public ClusterMessage(String destination, Object message) {
            this.destination = destination;
            this.message = message;
        }
        
        // getter/setter方法
        public String getDestination() { return destination; }
        public void setDestination(String destination) { this.destination = destination; }
        
        public Object getMessage() { return message; }
        public void setMessage(Object message) { this.message = message; }
    }
}
```

## 安全考虑

### 1. 身份认证和授权

```java
@Component
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, 
                                 ServerHttpResponse response,
                                 WebSocketHandler wsHandler, 
                                 Map<String, Object> attributes) {
        
        // 从请求中获取认证信息
        String token = extractToken(request);
        
        if (token == null || !validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        
        // 将用户信息存储到会话属性中
        UserInfo userInfo = getUserInfoFromToken(token);
        attributes.put("userInfo", userInfo);
        attributes.put("authenticated", true);
        
        return true;
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, 
                             ServerHttpResponse response,
                             WebSocketHandler wsHandler, 
                             Exception exception) {
        // 握手后处理
    }
    
    private String extractToken(ServerHttpRequest request) {
        // 从查询参数中获取token
        String query = request.getURI().getQuery();
        if (query != null && query.contains("token=")) {
            return query.substring(query.indexOf("token=") + 6);
        }
        
        // 从Header中获取token
        List<String> authHeaders = request.getHeaders().get("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String authHeader = authHeaders.get(0);
            if (authHeader.startsWith("Bearer ")) {
                return authHeader.substring(7);
            }
        }
        
        return null;
    }
    
    private boolean validateToken(String token) {
        // 实现token验证逻辑
        try {
            // 这里应该调用你的token验证服务
            return JwtUtil.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }
    
    private UserInfo getUserInfoFromToken(String token) {
        // 从token中解析用户信息
        return JwtUtil.parseUserInfo(token);
    }
}

// 消息级别的权限控制
@Component
public class WebSocketSecurityHandler {
    
    public boolean hasPermission(WebSocketSession session, String destination, Object message) {
        UserInfo userInfo = (UserInfo) session.getAttributes().get("userInfo");
        
        if (userInfo == null) {
            return false;
        }
        
        // 检查用户是否有权限访问特定目标
        if (destination.startsWith("/topic/admin/")) {
            return userInfo.hasRole("ADMIN");
        }
        
        if (destination.startsWith("/topic/private/")) {
            // 检查是否是用户自己的私有频道
            return destination.contains(userInfo.getUserId());
        }
        
        return true; // 默认允许
    }
}
```

### 2. 输入验证和消息过滤

```java
@Component
public class MessageValidator {
    
    private static final int MAX_MESSAGE_LENGTH = 1000;
    private static final Pattern SAFE_CONTENT_PATTERN = Pattern.compile("^[\\w\\s.,!?-]*$");
    
    public ValidationResult validateMessage(Object message) {
        if (message == null) {
            return ValidationResult.invalid("消息不能为空");
        }
        
        if (message instanceof String) {
            return validateTextMessage((String) message);
        }
        
        if (message instanceof ChatMessage) {
            return validateChatMessage((ChatMessage) message);
        }
        
        return ValidationResult.valid();
    }
    
    private ValidationResult validateTextMessage(String text) {
        if (text.length() > MAX_MESSAGE_LENGTH) {
            return ValidationResult.invalid("消息长度超过限制");
        }
        
        if (containsHarmfulContent(text)) {
            return ValidationResult.invalid("消息包含不当内容");
        }
        
        return ValidationResult.valid();
    }
    
    private ValidationResult validateChatMessage(ChatMessage chatMessage) {
        if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
            return ValidationResult.invalid("消息内容不能为空");
        }
        
        if (chatMessage.getSender() == null || chatMessage.getSender().trim().isEmpty()) {
            return ValidationResult.invalid("发送者不能为空");
        }
        
        return validateTextMessage(chatMessage.getContent());
    }
    
    private boolean containsHarmfulContent(String text) {
        // 实现内容过滤逻辑
        String[] bannedWords = {"spam", "abuse", "harmful"};
        String lowerText = text.toLowerCase();
        
        for (String word : bannedWords) {
            if (lowerText.contains(word)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        
        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() { return valid; }
        public String getErrorMessage() { return errorMessage; }
    }
}
```

### 3. 速率限制

```java
@Component
public class WebSocketRateLimiter {
    
    private final Map<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // 每个用户每分钟最多发送60条消息
    private static final int MAX_MESSAGES_PER_MINUTE = 60;
    private static final long WINDOW_SIZE_MS = 60000; // 1分钟
    
    @PostConstruct
    public void init() {
        // 定期清理过期的限制信息
        cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 60, 60, TimeUnit.SECONDS);
    }
    
    public boolean isAllowed(String userId) {
        long now = System.currentTimeMillis();
        
        RateLimitInfo info = rateLimitMap.computeIfAbsent(userId, k -> new RateLimitInfo());
        
        synchronized (info) {
            // 清理过期的时间戳
            info.timestamps.removeIf(timestamp -> now - timestamp > WINDOW_SIZE_MS);
            
            if (info.timestamps.size() >= MAX_MESSAGES_PER_MINUTE) {
                return false; // 超过限制
            }
            
            info.timestamps.add(now);
            return true;
        }
    }
    
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        rateLimitMap.entrySet().removeIf(entry -> {
            RateLimitInfo info = entry.getValue();
            synchronized (info) {
                info.timestamps.removeIf(timestamp -> now - timestamp > WINDOW_SIZE_MS);
                return info.timestamps.isEmpty();
            }
        });
    }
    
    private static class RateLimitInfo {
        private final List<Long> timestamps = new ArrayList<>();
    }
    
    @PreDestroy
    public void shutdown() {
        cleanupExecutor.shutdown();
    }
}
```

### 4. SSL/TLS配置

```java
@Configuration
public class WebSocketSSLConfig {
    
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        
        tomcat.addAdditionalTomcatConnectors(redirectConnector());
        return tomcat;
    }
    
    private Connector redirectConnector() {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setScheme("http");
        connector.setPort(8080);
        connector.setSecure(false);
        connector.setRedirectPort(8443);
        return connector;
    }
}
```

## 最佳实践

### 1. 资源管理

```java
@Component
public class WebSocketResourceManager {
    
    private final Set<WebSocketSession> activeSessions = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService resourceMonitor = Executors.newSingleThreadScheduledExecutor();
    
    @PostConstruct
    public void init() {
        // 监控资源使用情况
        resourceMonitor.scheduleAtFixedRate(this::monitorResources, 30, 30, TimeUnit.SECONDS);
    }
    
    public void registerSession(WebSocketSession session) {
        activeSessions.add(session);
        
        // 设置会话超时
        session.getAttributes().put("createdTime", System.currentTimeMillis());
        
        // 设置最大空闲时间
        if (session instanceof StandardWebSocketSession) {
            try {
                ((StandardWebSocketSession) session).setMaxIdleTimeout(300000); // 5分钟
            } catch (Exception e) {
                System.err.println("设置会话超时失败: " + e.getMessage());
            }
        }
    }
    
    public void unregisterSession(WebSocketSession session) {
        activeSessions.remove(session);
        
        // 清理会话资源
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.NORMAL);
            }
        } catch (Exception e) {
            System.err.println("关闭会话失败: " + e.getMessage());
        }
    }
    
    private void monitorResources() {
        int activeCount = activeSessions.size();
        long memoryUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        
        System.out.println(String.format(
            "WebSocket资源监控 - 活跃连接: %d, 内存使用: %d MB",
            activeCount, memoryUsed / 1024 / 1024
        ));
        
        // 如果连接数过多，进行清理
        if (activeCount > 1000) {
            cleanupOldSessions();
        }
    }
    
    private void cleanupOldSessions() {
        long now = System.currentTimeMillis();
        long maxAge = 3600000; // 1小时
        
        activeSessions.removeIf(session -> {
            Long createdTime = (Long) session.getAttributes().get("createdTime");
            if (createdTime != null && now - createdTime > maxAge) {
                try {
                    session.close(CloseStatus.GOING_AWAY);
                    return true;
                } catch (Exception e) {
                    return true; // 移除有问题的会话
                }
            }
            return false;
        });
    }
    
    @PreDestroy
    public void shutdown() {
        // 关闭所有活跃会话
        activeSessions.forEach(session -> {
            try {
                session.close(CloseStatus.SERVICE_RESTART);
            } catch (Exception e) {
                // 忽略关闭异常
            }
        });
        
        resourceMonitor.shutdown();
        try {
            if (!resourceMonitor.awaitTermination(5, TimeUnit.SECONDS)) {
                resourceMonitor.shutdownNow();
            }
        } catch (InterruptedException e) {
            resourceMonitor.shutdownNow();
        }
    }
}
```

### 2. 错误处理和重连机制

```java
// 客户端重连机制
public class ReconnectingWebSocketClient {
    
    private WebSocketSession session;
    private final String url;
    private final WebSocketHandler handler;
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean shouldReconnect = true;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 10;
    private static final long INITIAL_RECONNECT_DELAY = 1000; // 1秒
    
    public ReconnectingWebSocketClient(String url, WebSocketHandler handler) {
        this.url = url;
        this.handler = handler;
    }
    
    public void connect() {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            this.session = container.connectToServer(new ClientEndpoint() {
                
                @OnOpen
                public void onOpen(Session session) {
                    reconnectAttempts = 0; // 重置重连计数
                    handler.afterConnectionEstablished(new StandardWebSocketSession(
                        session.getRequestHeaders(), session.getRequestParameterMap(),
                        session.getPathParameters(), session.getUserProperties()));
                }
                
                @OnMessage
                public void onMessage(String message) {
                    handler.handleMessage(session, new TextMessage(message));
                }
                
                @OnClose
                public void onClose(Session session, CloseReason closeReason) {
                    handler.afterConnectionClosed(session, 
                        new CloseStatus(closeReason.getCloseCode().getCode(), 
                                      closeReason.getReasonPhrase()));
                    
                    // 如果不是正常关闭，尝试重连
                    if (shouldReconnect && closeReason.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE) {
                        scheduleReconnect();
                    }
                }
                
                @OnError
                public void onError(Session session, Throwable throwable) {
                    handler.handleTransportError(session, throwable);
                    
                    if (shouldReconnect) {
                        scheduleReconnect();
                    }
                }
            }, URI.create(url));
            
        } catch (Exception e) {
            System.err.println("连接失败: " + e.getMessage());
            if (shouldReconnect) {
                scheduleReconnect();
            }
        }
    }
    
    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            System.err.println("达到最大重连次数，停止重连");
            return;
        }
        
        long delay = INITIAL_RECONNECT_DELAY * (1L << reconnectAttempts); // 指数退避
        reconnectAttempts++;
        
        System.out.println(String.format("第%d次重连将在%d毫秒后开始", reconnectAttempts, delay));
        
        reconnectExecutor.schedule(() -> {
            if (shouldReconnect) {
                connect();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }
    
    public void disconnect() {
        shouldReconnect = false;
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (Exception e) {
                System.err.println("关闭连接失败: " + e.getMessage());
            }
        }
        reconnectExecutor.shutdown();
    }
}
```

### 3. 监控和日志

```java
@Component
public class WebSocketMonitor {
    
    private final MeterRegistry meterRegistry;
    private final Counter connectionCounter;
    private final Counter messageCounter;
    private final Timer messageProcessingTimer;
    private final Gauge activeConnectionsGauge;
    
    private final AtomicInteger activeConnections = new AtomicInteger(0);
    
    public WebSocketMonitor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.connectionCounter = Counter.builder("websocket.connections.total")
                .description("Total WebSocket connections")
                .register(meterRegistry);
        this.messageCounter = Counter.builder("websocket.messages.total")
                .description("Total WebSocket messages")
                .register(meterRegistry);
        this.messageProcessingTimer = Timer.builder("websocket.message.processing.time")
                .description("WebSocket message processing time")
                .register(meterRegistry);
        this.activeConnectionsGauge = Gauge.builder("websocket.connections.active")
                .description("Active WebSocket connections")
                .register(meterRegistry, this, monitor -> monitor.activeConnections.get());
    }
    
    public void recordConnection() {
        connectionCounter.increment();
        activeConnections.incrementAndGet();
    }
    
    public void recordDisconnection() {
        activeConnections.decrementAndGet();
    }
    
    public void recordMessage(String messageType) {
        messageCounter.increment(Tags.of("type", messageType));
    }
    
    public Timer.Sample startMessageProcessing() {
        return Timer.start(meterRegistry);
    }
    
    public void stopMessageProcessing(Timer.Sample sample) {
        sample.stop(messageProcessingTimer);
    }
}

@Aspect
@Component
public class WebSocketLoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketLoggingAspect.class);
    
    @Around("@annotation(org.springframework.messaging.handler.annotation.MessageMapping)")
    public Object logMessageHandling(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        
        logger.info("处理WebSocket消息: {} 参数: {}", methodName, args);
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            logger.info("WebSocket消息处理完成: {} 耗时: {}ms", methodName, duration);
            return result;
        } catch (Exception e) {
            logger.error("WebSocket消息处理失败: {} 错误: {}", methodName, e.getMessage(), e);
            throw e;
        }
    }
}
```

## 面试要点

### 高频问题

1. **WebSocket与HTTP的区别？**
   - 连接方式：HTTP是短连接，WebSocket是长连接
   - 通信模式：HTTP是请求-响应模式，WebSocket是全双工通信
   - 协议开销：WebSocket握手后开销更小
   - 实时性：WebSocket支持服务器主动推送

2. **WebSocket的握手过程？**
   - 客户端发送HTTP升级请求
   - 服务器返回101状态码确认升级
   - 协议从HTTP切换到WebSocket
   - 建立持久连接

3. **如何处理WebSocket连接断开？**
   - 实现心跳机制检测连接状态
   - 客户端自动重连机制
   - 服务器端连接池管理
   - 优雅关闭处理

4. **WebSocket的安全考虑？**
   - 身份认证和授权
   - 输入验证和消息过滤
   - 速率限制防止滥用
   - 使用WSS（WebSocket Secure）

### 深入问题

1. **WebSocket在集群环境下如何实现？**
   - 使用Redis等消息中间件进行节点间通信
   - 会话粘性（Session Affinity）
   - 负载均衡策略
   - 分布式会话管理

2. **如何优化WebSocket性能？**
   - 连接池管理
   - 消息批处理
   - 内存优化
   - 异步处理

3. **WebSocket的消息格式和协议扩展？**
   - 文本帧和二进制帧
   - 控制帧（Ping/Pong/Close）
   - 子协议支持（如STOMP）
   - 自定义协议设计

### 实践经验

1. **项目中如何选择WebSocket实现方案？**
   - 评估实时性要求
   - 考虑并发连接数
   - 选择合适的框架（原生API、Spring WebSocket、Netty）
   - 考虑部署和运维复杂度

2. **WebSocket常见问题及解决方案**
   - 连接频繁断开：实现重连机制和心跳检测
   - 内存泄漏：正确管理连接生命周期
   - 消息丢失：实现消息确认和重发机制
   - 性能瓶颈：优化消息处理和连接管理

3. **WebSocket监控和调试**
   - 连接数监控
   - 消息吞吐量统计
   - 错误率监控
   - 性能指标收集

## 总结

WebSocket编程是现代Web应用中实现实时通信的重要技术。掌握WebSocket需要理解：

1. **协议特性**：全双工通信、持久连接、低延迟
2. **实现方式**：Java API、Spring WebSocket、Netty等
3. **应用场景**：实时聊天、在线游戏、股票行情、协作编辑
4. **性能优化**：连接管理、消息批处理、内存优化、集群支持
5. **安全考虑**：身份认证、输入验证、速率限制、SSL/TLS
6. **最佳实践**：资源管理、错误处理、监控日志

在实际项目中，需要根据具体需求选择合适的实现方案，并注重性能优化和安全防护。通过合理的架构设计和实现，WebSocket可以为用户提供优秀的实时交互体验。
```