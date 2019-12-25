package com.qunar.qchat.service.impl;

import com.qunar.qchat.dao.IHostUserDao;
import com.qunar.qchat.dao.model.UserFriendsModel;
import com.qunar.qchat.dao.model.UserFriendsRequestModel;
import com.qunar.qchat.model.request.UserFriendStepTwoRequest;
import com.qunar.qchat.service.IUserFriendsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class UserFriendsServiceImpl implements IUserFriendsService {

    @Autowired
    private IHostUserDao hostUserDao;

    @Override
    @Transactional
    public void saveUserFriends(UserFriendStepTwoRequest request) {
        // 同意添加
        if (request.getStatus() == 1) {
            UserFriendsRequestModel requestModel = new UserFriendsRequestModel();
            requestModel.setUsername(request.getUser());
            requestModel.setHost(request.getHost());
            requestModel.setFriend(request.getFriend());
            requestModel.setStatus(request.getStatus());
            requestModel.setUpdateAt(new Date());
            // 更新好友请求
            hostUserDao.updateUserFriendsRequest(requestModel);

            UserFriendsModel userFriendsModel = new UserFriendsModel();
            userFriendsModel.setUsername(request.getUser());
            userFriendsModel.setFriend(request.getFriend());
            userFriendsModel.setHost(request.getHost());
            userFriendsModel.setUserhost(request.getHost());
            userFriendsModel.setVersion(1);
            userFriendsModel.setRelationship(1);
            // 新增好友
            hostUserDao.insertUserFriends(userFriendsModel);

            // 拒绝添加
        } else if (request.getStatus() == 2) {
            UserFriendsRequestModel requestModel = new UserFriendsRequestModel();
            requestModel.setUsername(request.getUser());
            requestModel.setHost(request.getHost());
            requestModel.setFriend(request.getFriend());
            requestModel.setStatus(request.getStatus());
            requestModel.setUpdateAt(new Date());
            // 更新好友请求
            hostUserDao.updateUserFriendsRequest(requestModel);
        }
    }
}
