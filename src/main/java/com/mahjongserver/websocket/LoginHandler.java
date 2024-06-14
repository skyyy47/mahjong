package com.mahjongserver.websocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mahjongserver.Login.LoginManager;
import com.mahjongserver.pojo.GetUserCacheMessage;
import com.mahjongserver.pojo.LoginMessage;
import com.mahjongserver.pojo.RegisterGuestMessage;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class LoginHandler extends SimpleChannelInboundHandler<TextWebSocketFrame>{

    private final LoginManager loginManager;
    private static final Gson gson = new Gson();

    public LoginHandler(LoginManager loginManager) {
        this.loginManager = loginManager;
    }
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
        String request = msg.text();
        Map<String, Object> response = new HashMap<>();

        try {
            if (request == null || request.isEmpty()) {
                throw new IllegalArgumentException("Request string is null or empty");
            }

            JsonObject jsonObject = JsonParser.parseString(request).getAsJsonObject();
            Map<String, String> message = gson.fromJson(request, Map.class);
            String action = message.get("action");

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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}

