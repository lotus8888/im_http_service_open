package com.qunar.qchat.dao.model;

import java.util.Date;

/**
 * @auth zdl
 * @Date 2019/12/25
 */
public class UserFriendsModel {

    private String username;
    private String friend;
    private Integer relationship;
    private Integer version;
    private String host;
    private String userhost;

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

    public Integer getRelationship() {
        return relationship;
    }

    public void setRelationship(Integer relationship) {
        this.relationship = relationship;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
}
