package com.core.manycloudadmin.so.user;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 更新用户余额
 */
@Data
public class UpdateUserFinanceSO {

    private String userId;

    /** 标签(add：添加余额,minus：减去余额,unbind：解冻余额,seal：冻结余额) **/
    private String tad;

    /** 金额 **/
    private BigDecimal moneyNum;

    /** 备注 **/
    private String remark;

}
