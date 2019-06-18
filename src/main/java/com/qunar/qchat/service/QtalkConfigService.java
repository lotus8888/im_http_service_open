package com.qunar.qchat.service;

import com.qunar.qchat.dao.IQtalkConfigDao;
import com.qunar.qchat.dao.model.QtalkConfigModel;
import com.qunar.qchat.model.JsonResult;
import com.qunar.qchat.utils.JacksonUtils;
import com.qunar.qchat.utils.JsonResultUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class QtalkConfigService {
    private static final Logger LOGGER = LoggerFactory.getLogger(QtalkConfigService.class);


    @Resource
    private IQtalkConfigDao qtalkConfigDao;

    public JsonResult<?> insertConfig(Map<String, String> stringMap) {
        List<QtalkConfigModel> configModels = new ArrayList<>();
        stringMap.entrySet().stream().forEach(entry ->
                configModels.add(new QtalkConfigModel(entry.getKey(), entry.getValue())));

        int insertResult = qtalkConfigDao.insertConfigs(configModels);
        LOGGER.info("insertConfig result:{}", insertResult);
        return insertResult == 1 ? JsonResultUtils.success() : JsonResultUtils.fail(500, "server端错误");

    }

    public JsonResult<?> selectConfig() {
        List<QtalkConfigModel> configMap = qtalkConfigDao.getConfigMap();
        LOGGER.info("selectConfig result:{}", JacksonUtils.obj2String(configMap));
        return JsonResultUtils.success(configMap);

    }
    public JsonResult<?> insertOrUpdateConfig(List<QtalkConfigModel> list) {
        list.stream().forEach(qtalkConfigModel -> qtalkConfigDao.insertOrUpdateConfig(qtalkConfigModel));
//        int updateConfig = qtalkConfigDao.insertOrUpdateConfig(list);
        //LOGGER.info("insertOrUpdateConfig result:{}", updateConfig);
        return JsonResultUtils.success();

    }
}
