package com.core.manycloudcommon.caller.vo;


import com.core.manycloudcommon.caller.Item.PayData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PayVO {
    private String code;
    private String msg;
    private PayData data;

}
