package com.mahjongserver.Club;

import org.springframework.stereotype.Service;

@Service
public class ClubService {

    public boolean createClub(String clubName) {
        // 在这里实现将俱乐部信息插入数据库的逻辑
        // 例如：使用JDBC或JPA插入数据库

        // 示例代码（请根据实际数据库操作修改）
        // return clubRepository.save(new Club(clubName)) != null;

        // 假设操作成功
        return true;
    }
}