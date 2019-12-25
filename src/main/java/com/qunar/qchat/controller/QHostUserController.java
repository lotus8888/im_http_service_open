package com.qunar.qchat.controller;

import com.alibaba.fastjson.JSONObject;
import com.qunar.qchat.constants.Config;
import com.qunar.qchat.constants.QChatConstant;
import com.qunar.qchat.constants.TableConstants;
import com.qunar.qchat.dao.IHostInfoDao;
import com.qunar.qchat.dao.IHostUserDao;
import com.qunar.qchat.dao.model.HostInfoModel;
import com.qunar.qchat.dao.model.HostUserModel;
import com.qunar.qchat.dao.model.UserFriendsModel;
import com.qunar.qchat.model.JsonResult;
import com.qunar.qchat.model.request.IncreUsersRequest;
import com.qunar.qchat.model.request.UserSearchRequest;
import com.qunar.qchat.utils.JsonResultUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * @auth dongzd.zhang
 * @Date 2018/10/18 14:35
 */
@RestController
@RequestMapping("/newapi/user")
public class QHostUserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(QHostUserController.class);

    @Autowired
    private IHostUserDao hostUserDao;
    @Autowired
    private IHostInfoDao hostInfoDao;

    /**
     * 精确搜索用户信息
     * @param request
     * @return
     */
    @RequestMapping(value = "/find_user.qunar", method = RequestMethod.POST)
    public JsonResult<?> findUser(@RequestBody UserSearchRequest request) {
        LOGGER.info("------ 精确搜索用户信息请求参数：" + JSONObject.toJSONString(request));
        try {
            if(!request.isRequestValid()) {
                return JsonResultUtils.fail(1, "参数错误");
            }

            // 查询host信息
            HostInfoModel hostInfoModel = hostInfoDao.selectHostInfoByHostName(request.getHost());
            if (Objects.isNull(hostInfoModel)) {
                return JsonResultUtils.fail(1, "host [" + request.getHost() + "] 不存在");
            }

            // 获取用户信息
            HostUserModel hostUserModel = hostUserDao.selectUserInfoByUserId(request.getUser(), hostInfoModel.getId());
            if (Objects.isNull(hostUserModel)) {
                return JsonResultUtils.fail(1, "user [" + request.getUser() + "] 不存在");
            }

            LOGGER.info("------ 精确搜索用户信息返回结果：" + JSONObject.toJSONString(hostUserModel));

            return JsonResultUtils.success(hostUserModel);
        } catch (Exception e) {
            LOGGER.error("----- 精确搜索用户信息接口 catch error: {}", ExceptionUtils.getStackTrace(e));
            return JsonResultUtils.fail(0, "服务器操作异常");
        }
    }

    /**
     * 获取好友列表
     * @param request
     * @return
     */
    @RequestMapping(value = "/get_friends.qunar", method = RequestMethod.POST)
    public JsonResult<?> getFriends(@RequestBody UserSearchRequest request) {
        LOGGER.info("------ 获取好友列表请求参数：" + JSONObject.toJSONString(request));
        try {
            if(!request.isRequestValid()) {
                return JsonResultUtils.fail(1, "参数错误");
            }

            // 查询host信息
            HostInfoModel hostInfoModel = hostInfoDao.selectHostInfoByHostName(request.getHost());
            if (Objects.isNull(hostInfoModel)) {
                return JsonResultUtils.fail(1, "host [" + request.getHost() + "] 不存在");
            }

            // 获取好友列表
            List<UserFriendsModel> friendList = hostUserDao.selectUserFriendsByUserId(request.getUser(), request.getHost());
            if (friendList == null) {
                friendList = new ArrayList<>();
            }

            LOGGER.info("------ 获取好友列表返回结果：" + JSONObject.toJSONString(friendList));

            return JsonResultUtils.success(friendList);
        } catch (Exception e) {
            LOGGER.error("----- 获取好友列表接口 catch error: {}", ExceptionUtils.getStackTrace(e));
            return JsonResultUtils.fail(0, "服务器操作异常");
        }
    }

    @RequestMapping(value = "/get_increment_users.qunar", method = RequestMethod.POST)
    public JsonResult<?> getIncrementUsers(@RequestBody IncreUsersRequest request) {
        try {
            if (Objects.isNull(request) || !request.isRequestValid()) {
                return JsonResultUtils.fail(1, "参数错误");
            }

            //不支持QChat访问
            if (QChatConstant.ENVIRONMENT_QCHAT.equals(Config.CURRENT_ENV)) {
                return JsonResultUtils.fail(1, "不支持的操作");
            }

            //查询host信息
            HostInfoModel hostInfoModel = hostInfoDao.selectHostInfoByHostName(request.getDomain());

            if (Objects.isNull(hostInfoModel)) {
                return JsonResultUtils.fail(1, "domain [" + request.getDomain() + "] 不存在");
            }

            String tableName = TableConstants.TABLE_HOSTUSERS;
            Integer version = request.getVersion();
            String domain = request.getDomain();

            Integer maxVersion = hostUserDao.selectMaxVersion(tableName);

            if (version >= maxVersion) {
                return JsonResultUtils.success(Collections.EMPTY_LIST);
            } else {

                List<HostUserModel> hostUserModelList = hostUserDao.selectIncrementByVersion(tableName, version, hostInfoModel.getId());
                List<Map<String, String>> dataList = new ArrayList<>();
                hostUserModelList.stream().forEach(hostUser -> {
                    Map<String, String> rowData = new HashMap<>();
                    rowData.put("U", hostUser.getUserId());
                    rowData.put("Domain", domain);
                    rowData.put("N", hostUser.getUserName());
                    rowData.put("V", String.valueOf(hostUser.getVersion()));
                    rowData.put("D", hostUser.getDepartment());
                    rowData.put("T", hostUser.getUserType());
                    rowData.put("F", hostUser.getPinyin());
                    rowData.put("S", hostUser.getPinyin());
                    rowData.put("H", String.valueOf(hostUser.getHireFlag()));
                    dataList.add(rowData);
                });

                return JsonResultUtils.success(dataList);
            }

        } catch (Exception ex) {
            LOGGER.error("catch error {}", ex);
            return JsonResultUtils.fail(0, "服务器异常：" + ExceptionUtils.getStackTrace(ex));
        }
    }


    /**
     * [{"domain":"ejabhost1","users":[{"user":"malin.ma","version":0}]}]
     * <p>
     * {"ret":true,"errcode":0,"errmsg":"","data":[{"domain":"ejabhost1","users":[{"type":"qunar_emp","loginName":"malin.ma","email":"","gender":"1","nickname":"马林malin","V":"6","imageurl":"file/v2/download/avatar/ea03ab510c88e51932e5fbbc54deb001.jpg?file=file/ea03ab510c88e51932e5fbbc54deb001.jpg&FileName=file/ea03ab510c88e51932e5fbbc54deb001.jpg&name=ea03ab510c88e51932e5fbbc54deb001.jpg","uid":0,"username":"malin.ma","domain":"ejabhost1","commenturl":"https://qt.qunar.com/dianping/user_comment.php"}]}]}
     */
    public JsonResult<?> getVCardInfo() {

        return JsonResultUtils.success();
    }

}
