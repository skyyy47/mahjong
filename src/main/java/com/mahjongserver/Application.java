package com.mahjongserver;

import com.mahjongserver.Login.LoginManager;
import com.mahjongserver.websocket.NettyWebSocketServer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@MapperScan("com.mahjongserver.Mapper")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            // 从 Spring 上下文获取 LoginManager Bean
            LoginManager loginManager = ctx.getBean(LoginManager.class);

            // 启动 Netty 服务器
            try {
                NettyWebSocketServer server = new NettyWebSocketServer(8081, loginManager);
                server.start();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.out.println("Server was interrupted.");
            }
        };
    }
}