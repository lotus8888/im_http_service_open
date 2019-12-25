package com.qunar.qchat.dao.model;

import java.util.Date;

/**
 * @auth zdl
 * @Date 2019/12/25
 */
public class UserFriendsRequestModel {

    private String username;
    private String friend;
    private String host;
    private String userhost;
    /**
     * 状态（0：待添加，1：已添加，2：已拒绝）
     */
    private Integer status;
    private Date createAt;
    private Date updateAt;

    public UserFriendsRequestModel(){}

    public UserFriendsRequestModel(String username, String friend, String host, String userhost) {
        this.username = username;
        this.friend = friend;
        this.host = host;
        this.userhost = userhost;
        this.status = 0;
        this.createAt = new Date();
        this.updateAt = new Date();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFriend() {
        return friend;
    }

    public void setFriend(String friend) {
        this.friend = friend;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUserhost() {
        return userhost;
    }

    public void setUserhost(String userhost) {
        this.userhost = userhost;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateAt() {
        return createAt;
    }

    public void setCreateAt(Date createAt) {
        this.createAt = createAt;
    }

    public Date getUpdateAt() {
        return updateAt;
    }

    public void setUpdateAt(Date updateAt) {
        this.updateAt = updateAt;
    }
}
