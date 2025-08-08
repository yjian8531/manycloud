package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

/**
 * 更改密码
 */
@Data
@Builder
public class UpdatePwdSO {

    /** 实例ID **/
    private String instanceId;

    /** 密码 **/
    private String pwd;

}
