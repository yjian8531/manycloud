package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

/**
 * 重装系统
 */
@Data
@Builder
public class ReinstallSO {
    /** 实例ID **/
    private String instanceId;
    /** 镜像ID **/
    private String imageId;
    /** 密码 **/
    private String pwd;

}
