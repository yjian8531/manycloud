package com.core.manycloudcommon.caller.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GrantFirewallVO {
    private boolean success;
    private String code;
    private String msg;

    //阿里云需要参数
    private String requestId;
    private String taskId;

}
