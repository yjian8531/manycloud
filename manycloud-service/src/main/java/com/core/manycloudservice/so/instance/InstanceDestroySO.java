package com.core.manycloudservice.so.instance;

import lombok.Data;

/**
 * 实例销毁SO
 */
@Data
public class InstanceDestroySO {

    /** 实例ID **/
    private String instanceId;

    /** 用户ID **/
    private String userId;
}
