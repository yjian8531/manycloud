package com.core.manycloudcommon.caller.so;

import lombok.Data;

@Data
public class CreateSecuritySO {
     /**
     * 实例ID（关联的主机实例ID）
     */
    private String instanceId;

    /**
     * 安全组名称
     */
    private String name;

    /**
     * 开放端口
     */
    private String port;

    /**
     * 安全组id
     */
    private String fwId;

    /**
     * 协议
     */
    private String protocol;
}
