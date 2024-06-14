package com.mahjongserver.websocket;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mahjongserver.Entity.User;
import com.mahjongserver.pojo.GetUserCacheMessage;
import com.mahjongserver.pojo.LoginMessage;
import com.mahjongserver.pojo.RegisterGuestMessage;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import com.google.gson.Gson;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mahjongserver.Login.LoginManager;

public class NettyWebSocketServer {

    private final int port; // 服务器端口
    private final LoginManager loginManager; // 登录管理器
    private static final Gson gson = new Gson(); // 用于JSON处理的Gson对象

    // 构造函数，初始化服务器端口和登录管理器
    public NettyWebSocketServer(int port, LoginManager loginManager) {
        this.port = port;
        this.loginManager = loginManager;
    }

    // 启动 Netty 服务器
    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new HttpServerCodec());
                            ch.pipeline().addLast(new ChunkedWriteHandler());
                            ch.pipeline().addLast(new HttpObjectAggregator(8192));
                            ch.pipeline().addLast(new WebSocketServerProtocolHandler("/ws"));
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<TextWebSocketFrame>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
                                    handleWebSocketFrame(ctx, msg);
                                }

                                @Override
                                public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
                                    System.out.println("Client connected: " + ctx.channel().id().asShortText());
                                }

                                @Override
                                public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
                                    System.out.println("Client disconnected: " + ctx.channel().id().asShortText());
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    cause.printStackTrace();
                                    ctx.close();
                                }
                            });
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(port).sync();
            System.out.println("WebSocket server started on port " + port);
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    // 处理 WebSocket 帧
    private void handleWebSocketFrame(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String request = frame.text();
        Map<String, Object> response = new HashMap<>();

        try {
            if (request == null || request.isEmpty()) {
                throw new IllegalArgumentException("Request string is null or empty");
            }

            JsonObject jsonObject = JsonParser.parseString(request).getAsJsonObject();
            String action = jsonObject.get("action").getAsString();

            if ("registerGuest".equals(action)) {
                RegisterGuestMessage registerGuestMessage = gson.fromJson(jsonObject, RegisterGuestMessage.class);
                int guestId = loginManager.registerGuest();
                response.put("action", "registerGuest");
                response.put("success", true);
                response.put("id", guestId); // 返回生成的ID，确保是整数类型
            } else if ("login".equals(action)) {
                LoginMessage loginMessage = gson.fromJson(jsonObject, LoginMessage.class);
                String userid = loginMessage.getUserid();
                String password = loginMessage.getPassword();

                System.out.println("用户Id: " + userid + "，密码: " + password);

                boolean loginSuccess = loginManager.login(userid, password);
                response.put("action", "login");
                response.put("success", loginSuccess);

                if (loginSuccess) {
                    String username = loginManager.getUsernameById(userid);
                    response.put("username", username);
                    response.put("userid", userid);
                    System.out.println("登录成功，用户Id: " + userid + ", 用户名: " + username);
                } else {
                    response.put("message", "Invalid userid or password.");
                    System.out.println("Login failed: Invalid userid or password.");
                }
            } else if ("getUserCache".equals(action)) {
                GetUserCacheMessage getUserCacheMessage = gson.fromJson(jsonObject, GetUserCacheMessage.class);
                Map<String, String> userCache = loginManager.getUserCache();
                response.put("action", "getUserCache");
                response.put("success", true);
                response.put("userCache", userCache); // 返回实际的数据
            } else {
                response.put("action", "unknown");
                response.put("success", false);
                response.put("message", "Unknown action: " + action);
            }
        } catch (IllegalArgumentException e) {
            response.put("action", "error");
            response.put("success", false);
            response.put("message", e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            response.put("action", "error");
            response.put("success", false);
            response.put("message", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            response.put("action", "error");
            response.put("success", false);
            response.put("message", "Internal server error: " + e.getMessage());
        }

        ctx.channel().writeAndFlush(new TextWebSocketFrame(gson.toJson(response)));
    }
}