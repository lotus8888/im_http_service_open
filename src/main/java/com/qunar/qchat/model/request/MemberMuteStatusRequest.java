package com.qunar.qchat.model.request;

import lombok.Data;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @auth zdl
 * @Date 2019/12/13 10:00
 */
@Data
@ToString
public class MemberMuteStatusRequest implements IRequest {

    private String muc_name;
    private List<String> member_list;

    @Override
    public boolean isRequestValid() {
        if(StringUtils.isBlank(this.muc_name)) {
            return false;
        }

        if(CollectionUtils.isEmpty(member_list)) {
            return false;
        }

        return true;
    }
}
