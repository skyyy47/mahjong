package com.mahjongserver.Mapper;

import com.mahjongserver.Entity.User;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface UserMapper {

    User selectUserById(int id);

    void insertUser(User user);
}
