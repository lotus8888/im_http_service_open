package com.qunar.qchat.dao;


import com.qunar.qchat.dao.model.UserInfoQtalk;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;


/**
 * @author binz.zhang
 */
@Component
public interface IUserInfo {


    /**
     * select max version from the host_users
     *
     * @return
     */
    int selectMaxVersion(@Param("hostID") Integer host);

    /**
     * 获取公共域 host信息
     *
     * @param host
     * @return
     */
    Integer getHostInfo(@Param("hostID") String host);

    String getDomain(@Param("hostID") Integer host);

    /**
     * qtalk qunar 域获取员工信息
     *
     * @param version
     * @return
     */
    List<UserInfoQtalk> getQtalkUsersByVersion(@Param("version") Integer version, @Param("hostID") Integer hostID);

    List<String> getAllUsersByHost(@Param("hostId") Integer hostId);

    /**
     * 获取手机号
     * @param userId
     * @param hostId
     * @return
     */
    String getUserMobile(@Param("userId")String userId,@Param("hostId")Integer hostId);

    /**
     * 获取领导
     * @param userId
     * @param hostId
     * @return
     */
    String getUserLeader(@Param("userId")String userId,@Param("hostId")Integer hostId);

    /**
     * 获取就职状态 1在职 0离职
     * @param userId
     * @param hostId
     * @return
     */
    Integer getUserHireFlag(@Param("userId")String userId,@Param("hostId")Integer hostId);


    UserInfoQtalk selectUserByUserId(@Param("userId")String userId,@Param("hostId")Integer hostId);

    int insertVcardVersion(@Param("username") String username,
                           @Param("host") String host,
                           @Param("url") String url,
                           @Param("gender") Integer gender);

    int deleteVcard();
}
