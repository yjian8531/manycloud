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

    /**
     * 响应数据（根据接口实际返回定义）
     * 可能是订单ID、完整订单信息等
     */
    private  Object data;

}
