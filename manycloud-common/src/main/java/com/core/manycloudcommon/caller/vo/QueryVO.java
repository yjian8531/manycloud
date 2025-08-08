package com.core.manycloudcommon.caller.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class QueryVO {

    /** 0000:成功，其他失败 **/
    private String code;
    /** 描述 **/
    private String msg;

    /** 数据 **/
    private Map<String,QueryDetailVO> queryDetailMap;

}
