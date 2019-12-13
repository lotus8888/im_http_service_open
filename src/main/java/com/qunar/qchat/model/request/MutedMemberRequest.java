package com.qunar.qchat.model.request;

import lombok.Data;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * @auth zdl
 * @Date 2019/12/13 10:00
 */
@Data
@ToString
public class MutedMemberRequest implements IRequest {

    private String muc_name;
    private String member_name;
    private Integer muted_type;

    @Override
    public boolean isRequestValid() {
        if(StringUtils.isBlank(this.muc_name)) {
            return false;
        }

        if(StringUtils.isBlank(this.member_name)) {
            return false;
        }

        if (this.muted_type == null || (this.muted_type != 0 && this.muted_type != 1)) {
            return false;
        }

        return true;
    }
}
