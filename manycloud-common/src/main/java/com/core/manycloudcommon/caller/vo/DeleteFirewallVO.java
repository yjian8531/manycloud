package com.core.manycloudcommon.caller.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 防火墙删除返回
 */
@Data
@Builder
public class DeleteFirewallVO {

    /** 0000:成功，其他失败 **/
    private String code;

    /** 描述 **/
    private String msg;
}
