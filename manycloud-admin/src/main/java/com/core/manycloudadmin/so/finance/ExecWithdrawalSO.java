package com.core.manycloudadmin.so.finance;

import lombok.Data;

/**
 * 提现审核SO
 */
@Data
public class ExecWithdrawalSO {

    /** 编号ID **/
    private Integer id;

    /** 审核标记(Y:通过/N:驳回) **/
    private String tad;

    /** 收款方式(0:支付宝,1:微信,2:银行卡) **/
    private Integer way;
    /** 收款账号 **/
    private String account;

    /** 收款人姓名 **/
    private String name;

    /** 备注 **/
    private String remark;

}
