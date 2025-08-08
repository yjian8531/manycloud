package com.core.manycloudcommon.caller.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreateVO{

    /** 0000:成功，其他失败 **/
    private String code;
    /** 描述 **/
    private String msg;
    /** 实例ID集合 **/
    private List<String> instanceIds;

}
