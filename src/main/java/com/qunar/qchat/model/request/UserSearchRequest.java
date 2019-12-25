package com.qunar.qchat.model.request;

import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * @auth zdl
 * @Date 2019/12/25
 */
public class UserSearchRequest implements IRequest {

    private String user;

    private String host;

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

    @Override
    public boolean isRequestValid() {
        if (StringUtils.isBlank(user)) {
            return false;
        }

        if (StringUtils.isBlank(host)) {
            return false;
        }

        return true;
    }

}
