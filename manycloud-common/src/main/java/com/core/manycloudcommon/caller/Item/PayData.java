package com.core.manycloudcommon.caller.Item;

import lombok.Data;

@Data
public class PayData {
    private String paymentType;
    private String paymentName;
    private Integer orderId;
    private Integer status;
    private String payMessage;

}