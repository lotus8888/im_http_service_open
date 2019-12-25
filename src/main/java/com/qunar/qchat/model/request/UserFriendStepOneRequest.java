package com.qunar.qchat.model.request;

import org.apache.commons.lang3.StringUtils;

/**
 * @auth zdl
 * @Date 2019/12/25
 */
public class UserFriendStepOneRequest implements IRequest {

    private String user;

    private String host;

    private String friend;

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getFriend() {
        return friend;
    }

    public void setFriend(String friend) {
        this.friend = friend;
    }

    @Override
    public boolean isRequestValid() {
        if (StringUtils.isBlank(user)) {
            return false;
        }

        if (StringUtils.isBlank(host)) {
            return false;
        }

        if (StringUtils.isBlank(friend)) {
            return false;
        }

        return true;
    }

}
