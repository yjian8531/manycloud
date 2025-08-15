package com.core.manycloudservice.vo;

import lombok.Data;

/**
 * 创建充值收款订单VO
 */
@Data
public class CeratePayOrderVO {

    /** ID **/
    private String intentId;
    /** 签名 **/
    private String clientSecret;
    /** 支付页面链接 **/
    private String paymentUrl;
    /** 币种 **/
    private String currency;
    /** 金额 **/
    private double baseAmount;

}
