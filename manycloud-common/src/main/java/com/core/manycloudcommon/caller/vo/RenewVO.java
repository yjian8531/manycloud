package com.core.manycloudcommon.caller.vo;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RenewVO {

    /** 0000:成功，其他失败 **/
    private String code;
    /** 描述 **/
    private String msg;

}
