package com.core.manycloudservice.so.instance;

import lombok.Data;

/**
 * 重装系统SO
 */
@Data
public class ResetSO {

    /** 实例ID **/
    private String instanceId;

    /** 新系统参数 **/
    private String os;

}
