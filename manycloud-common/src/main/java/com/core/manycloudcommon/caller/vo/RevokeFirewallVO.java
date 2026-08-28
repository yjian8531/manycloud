package com.core.manycloudcommon.caller.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 主机安全组解绑+清理返回
 */
@Data
@Builder
public class RevokeFirewallVO {

    /** 0000:成功，其他失败 **/
    private String code;

    /** 描述 **/
    private String msg;
}
