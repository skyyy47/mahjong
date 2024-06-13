package com.mahjongserver.websocket;


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

import java.util.HashMap;
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
        Map<String, String> response = new HashMap<>();

        try {
            Map<String, String> message = gson.fromJson(request, Map.class);
            String action = message.get("action");

            if ("registerGuest".equals(action)) {
                int guestId = loginManager.registerGuest();
                response.put("action", "registerGuest");
                response.put("success", "true");
                response.put("id", String.valueOf(guestId)); // 返回生成的ID，确保是字符串
            } else if ("login".equals(action)) {
                String userid = message.get("userid");
                String password = message.get("password");
                boolean loginSuccess = loginManager.login(userid, password);
                response.put("action", "login");
                response.put("success", String.valueOf(loginSuccess));

                if (loginSuccess) {
                    String username = loginManager.getUsernameById(userid);
                    response.put("username", username);
                    response.put("userid", userid);
                    System.out.println("登录成功，用户Id: " + userid + ", 用户名: " + username);
                } else {
                    response.put("message", "Invalid userid or password.");
                    System.out.println("Login failed: Invalid userid or password.");
                }
            } else {
                response.put("action", "unknown");
                response.put("success", "false");
                response.put("message", "Unknown action: " + action);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("action", "error");
            response.put("success", "false");
            response.put("message", e.getMessage());
        }

        ctx.channel().writeAndFlush(new TextWebSocketFrame(gson.toJson(response)));
    }
}