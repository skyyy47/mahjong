package com.mahjongserver.Mapper;

import com.mahjongserver.Entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


@Mapper
public interface UserMapper {

    User selectUserById(int id);

    User selectUserByName(String userName);
    void insertUser(User user);

    List<User> selectAllUsers();
}
