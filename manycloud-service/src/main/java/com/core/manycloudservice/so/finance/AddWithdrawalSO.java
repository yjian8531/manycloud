package com.core.manycloudservice.so.finance;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户提现申请SO
 */
@Data
public class AddWithdrawalSO {

    /** 用户ID **/
    private String userId;

    /** 提现金额 **/
    private BigDecimal moneyNum;

    /** 税点(%) **/
    private BigDecimal taxRatio;

    /** 税后金额 **/
    private BigDecimal moneyTax;

    /** 收款账号 **/
    private String account;

    /** 收款姓名 **/
    private String name;

    /** 收款方式(0:支付宝,1:微信,2:银行卡) **/
    private Integer way;

    /** 邮箱验证码 **/
    private String code;

}
