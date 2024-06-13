package com.mahjongserver.Login;

import com.mahjongserver.Entity.User;
import com.mahjongserver.Mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class LoginManager {
    @Autowired
    private UserMapper userMapper;

    // 登录方法，根据用户ID和密码进行登录验证
    public boolean login(String userid, String password) {
        // 使用 MyBatis 映射器查询用户信息
        User user = userMapper.selectUserById(Integer.parseInt(userid));
        // 验证用户密码是否匹配
        return user != null && user.getPassword().equals(password);
    }

    public String getUsernameById(String userid) {
        User user = userMapper.selectUserById(Integer.parseInt(userid));
        return user != null ? user.getUser_name() : null;
    }

    public int registerGuest() {
        User user = new User();
        user.setUser_name("游客" + new Random().nextInt(10000));
        user.setPassword(""); // 游客账号无密码
        userMapper.insertUser(user);
        return user.getUser_id();
    }
}
