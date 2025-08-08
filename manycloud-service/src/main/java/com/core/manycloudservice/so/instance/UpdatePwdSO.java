package com.core.manycloudservice.so.instance;

import lombok.Data;

/***
 * 更新实例密码
 */
@Data
public class UpdatePwdSO {

    /** 实例ID **/
    private String instanceId;

    private String pwd;

}
