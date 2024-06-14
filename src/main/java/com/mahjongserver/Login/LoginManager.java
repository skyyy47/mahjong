package com.mahjongserver.Login;

import com.mahjongserver.Entity.User;
import com.mahjongserver.Mapper.UserMapper;
import com.mahjongserver.util.RedisUtil;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Service
public class LoginManager {
    @Autowired
    private UserMapper userMapper;

    private final SqlSessionFactory sqlSessionFactory;

    @Autowired
    public LoginManager(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    // 登录方法，根据用户ID和密码进行登录验证
    public boolean login(String userId, String password) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            UserMapper userMapper = session.getMapper(UserMapper.class);
            // 使用 MyBatis 映射器查询用户信息
            User user = userMapper.selectUserById(Integer.parseInt(userId));
            // 验证用户密码是否匹配
            if (user != null && user.getPassword().equals(password)) {
                try (Jedis jedis = RedisUtil.getJedis()) {
                    jedis.hset("userCache", String.valueOf(user.getUserId()), password);
                }
                return true;
            }
            return false;
        }
    }

    // 根据用户ID获取用户名
    public String getUsernameById(String userId) {
        User user = userMapper.selectUserById(Integer.parseInt(userId));
        return user != null ? user.getUserName() : null;
    }

    // 注册游客账号
    public int registerGuest() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            UserMapper userMapper = session.getMapper(UserMapper.class);
            User user = new User();
            user.setUserName("游客" + new Random().nextInt(10000));
            user.setPassword(""); // 游客账号无密码
            userMapper.insertUser(user);
            session.commit();
            try (Jedis jedis = RedisUtil.getJedis()) {
                jedis.hset("userCache", String.valueOf(user.getUserId()), user.getPassword());
            }
            return user.getUserId();
        }
    }

    // 获取所有用户
    public List<User> getAllUsers() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            UserMapper userMapper = session.getMapper(UserMapper.class);
            return userMapper.selectAllUsers();
        }
    }

    // 获取缓存中的用户信息
    public Map<String, String> getUserCache() {
        try (Jedis jedis = RedisUtil.getJedis()) {
            Map<String, String> userCache = new HashMap<>();
            Map<String, String> redisCache = jedis.hgetAll("userCache");
            for (Map.Entry<String, String> entry : redisCache.entrySet()) {
                userCache.put(entry.getKey(), entry.getValue().toString());
            }
            return userCache;
        }
    }
}
