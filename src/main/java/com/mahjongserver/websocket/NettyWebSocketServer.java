package com.mahjongserver.websocket;

import com.mahjongserver.websocket.LoginHandler;
import com.mahjongserver.Login.LoginManager;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;

public class NettyWebSocketServer {

    private final int port;
    private final LoginManager loginManager;

    public NettyWebSocketServer(int port, LoginManager loginManager) {
        this.port = port;
        this.loginManager = loginManager;
    }

    public void start() throws InterruptedException {
        BaseWebSocketServer server = new BaseWebSocketServer(port);
        server.start(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new HttpServerCodec());
                ch.pipeline().addLast(new ChunkedWriteHandler());
                ch.pipeline().addLast(new HttpObjectAggregator(8192));
                ch.pipeline().addLast(new WebSocketServerProtocolHandler("/ws"));
                ch.pipeline().addLast(new LoginHandler(loginManager)); // 添加登录处理程序
            }
        });
    }
}
