package com.qunar.qchat.controller;


import com.fasterxml.jackson.core.type.TypeReference;
import com.qunar.qchat.model.JsonResult;
import com.qunar.qchat.model.request.GetUserStatusRequest;
import com.qunar.qchat.model.request.QtalkConfigRequest;
import com.qunar.qchat.service.LdapAdService;
import com.qunar.qchat.service.QtalkConfigService;
import com.qunar.qchat.utils.JacksonUtils;
import com.qunar.qchat.utils.JsonResultUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RequestMapping("/newapi/config/")
@RestController
public class QtalkConfigController {

    private static final Logger LOGGER = LoggerFactory.getLogger(QtalkConfigController.class);

    @Autowired
    QtalkConfigService qtalkConfigService;
    @Autowired
    LdapAdService ldapAdService;


    @RequestMapping(value = "/saveLdapConfig.qunar", method = RequestMethod.POST)
    public JsonResult<?> saveLdapConfig(@RequestBody String json) {

        try {
            QtalkConfigRequest configRequest = JacksonUtils.string2Obj(json, QtalkConfigRequest.class);
            if (configRequest == null || StringUtils.isNotEmpty(configRequest.check())) {
                return JsonResultUtils.fail(411, "param error");
            }
            Map<String, String> stringMap = JacksonUtils.string2Obj(json, new TypeReference<Map<String, String>>() {
            });

            ldapAdService.setQtalkConfig(stringMap);
            JsonResult<?> jsonResult = ldapAdService.synchronizeAdUsers(false, false);
            LOGGER.info("saveLdapConfig synchronizeAdUsers result:{}", jsonResult.getErrmsg());
            if (jsonResult.isRet()) {
                return qtalkConfigService.insertConfig(stringMap);
            } else {
                return jsonResult;
            }
        } catch (Exception e) {
            LOGGER.error("saveLdapConfig error", e);
            return JsonResultUtils.fail(500, "server端错误");
        }

    }

}
