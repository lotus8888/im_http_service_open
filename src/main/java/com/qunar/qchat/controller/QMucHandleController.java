package com.qunar.qchat.controller;

import com.alibaba.fastjson.JSONObject;
import com.qunar.qchat.constants.BasicConstant;
import com.qunar.qchat.dao.IGetMsgDao;
import com.qunar.qchat.dao.IMucInfoDao;
import com.qunar.qchat.model.JsonResult;
import com.qunar.qchat.model.request.MemberMuteStatusRequest;
import com.qunar.qchat.model.request.MutedMemberRequest;
import com.qunar.qchat.model.request.UpdateMucNickRequest;
import com.qunar.qchat.utils.CookieUtils;
import com.qunar.qchat.utils.JsonResultUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/newapi/muc/")
@RestController
public class QMucHandleController {

    private static final Logger LOGGER = LoggerFactory.getLogger(QMucHandleController.class);

    @Autowired
    private IMucInfoDao iMucInfoDao;

    @Autowired
    private IGetMsgDao iGetMsgDao;

    /**
     * 群成员禁言
     * @param request
     * @return
     */
    @RequestMapping(value = "/muted_members.qunar", method = RequestMethod.POST)
    public Object mutedMembers(HttpServletRequest httpRequest, @RequestBody MutedMemberRequest request) {
        LOGGER.info("------ 群成员禁言请求参数：" + JSONObject.toJSONString(request));
        try {
            if(!request.isRequestValid()) {
                return JsonResultUtils.fail(1, "参数错误");
            }

            // 群名称
            String tempMucName = "";
            if (request.getMuc_name().indexOf("@") == -1) {
                tempMucName = request.getMuc_name();
            } else {
                tempMucName = request.getMuc_name().substring(0, request.getMuc_name().indexOf("@"));
            }

            // 校验群是否存在
            int exitCount = iMucInfoDao.checkMucExist(tempMucName);
            if(exitCount == 0) {
                LOGGER.error("------ 群成员禁言接口，群：{} 不存在", request.getMuc_name());
                return JsonResultUtils.fail(1, "群" + request.getMuc_name() + "不存在");
            }

            // 获取 cookie 信息
            Map<String, Object> cookie = CookieUtils.getUserbyCookie(httpRequest);
            String domain = cookie.get("d").toString();

            // 设置为禁言状态
            iGetMsgDao.updateMucRoomUsersByUser(request.getMuted_type(), request.getMuc_name(), request.getMember_name(), domain);

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("ret", true);
            resultMap.put("errcode", 0);
            resultMap.put("errmsg", "");

            return resultMap;

        } catch (Exception e) {
            LOGGER.error("----- 群成员禁言接口 catch error: {}", ExceptionUtils.getStackTrace(e));
            return JsonResultUtils.fail(0, "服务器操作异常");
        }

    }

    /**
     * 获取群成员禁言状态
     * @param request
     * @return
     */
    @RequestMapping(value = "/member_muted_status.qunar", method = RequestMethod.POST)
    public Object getMembersMuteStatus(HttpServletRequest httpRequest, @RequestBody MemberMuteStatusRequest request) {
        LOGGER.info("------ 获取群成员禁言状态请求参数：" + JSONObject.toJSONString(request));
        try {
            if(!request.isRequestValid()) {
                return JsonResultUtils.fail(1, "参数错误");
            }

            // 群名称
            String tempMucName = "";
            if (request.getMuc_name().indexOf("@") == -1) {
                tempMucName = request.getMuc_name();
            } else {
                tempMucName = request.getMuc_name().substring(0, request.getMuc_name().indexOf("@"));
            }

            // 校验群是否存在
            int exitCount = iMucInfoDao.checkMucExist(tempMucName);
            if(exitCount == 0) {
                LOGGER.error("------ 获取群成员禁言状态接口，群：{} 不存在", request.getMuc_name());
                return JsonResultUtils.fail(1, "群" + request.getMuc_name() + "不存在");
            }

            // 获取 cookie 信息
            Map<String, Object> cookie = CookieUtils.getUserbyCookie(httpRequest);
            String domain = cookie.get("d").toString();

            // 设置为禁言状态
            List<Map<String, Object>> list = iGetMsgDao.getMucUsersStatus(domain, request.getMuc_name(), request.getMember_list());

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("ret", true);
            resultMap.put("errcode", 0);
            resultMap.put("errmsg", "");
            resultMap.put("data", list);

            return resultMap;

        } catch (Exception e) {
            LOGGER.error("----- 群成员禁言接口 catch error: {}", ExceptionUtils.getStackTrace(e));
            return JsonResultUtils.fail(0, "服务器操作异常");
        }

    }
}
