package com.qunar.qchat.dao;

import com.qunar.qchat.dao.model.HostUserModel;
import com.qunar.qchat.dao.model.UserInfoQtalk;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IHostUserDao {

    List<HostUserModel> selectIncrementByVersion(@Param("table") String table,
                                                 @Param("version") Integer version,
                                                 @Param("hostId") Integer hostId);

    Integer selectMaxVersion(@Param("table") String table);
    List<UserInfoQtalk> selectOnJobUserFromHostUser(@Param("hostID") Integer hostId);

    void insertUser(@Param("UserInfoQtalk") UserInfoQtalk userInfoQtalk);

    void updateHostUserHireType(@Param("UserInfoQtalk") UserInfoQtalk userInfoQtalk);


    int updateHostUser(@Param("UserInfoQtalk") UserInfoQtalk userInfoQtalk);
    int deleteHostUsers();
}
