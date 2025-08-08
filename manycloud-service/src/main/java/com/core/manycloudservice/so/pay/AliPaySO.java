package com.core.manycloudservice.so.pay;

import lombok.Data;

import java.util.List;

/**
 * 阿里支付SO
 */
@Data
public class AliPaySO {

    /** 用户ID **/
    private String userId;

    /** 订单编号 **/
    private List<String> orderNos;

    /** 金额 **/
    private String amount;

}
