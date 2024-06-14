package com.mahjongserver.util;

import redis.clients.jedis.Jedis;

public class RedisUtil {
    private static final String REDIS_HOST = "localhost"; // 修改为你的Redis服务器地址
    private static final int REDIS_PORT = 6379; // Redis服务器端口
    private static final String REDIS_PASSWORD = null; // 如果Redis有密码，则设置

    public static Jedis getJedis() {
        Jedis jedis = new Jedis(REDIS_HOST, REDIS_PORT);
        if (REDIS_PASSWORD != null && !REDIS_PASSWORD.isEmpty()) {
            jedis.auth(REDIS_PASSWORD);
        }
        return jedis;
    }
}
