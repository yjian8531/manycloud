package com.core.manycloudcommon.caller.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 重装系统
 */
@Data
@Builder
public class ReinstallVO {

    /** 0000:成功，其他失败 **/
    private String code;
    /** 描述 **/
    private String msg;

}
