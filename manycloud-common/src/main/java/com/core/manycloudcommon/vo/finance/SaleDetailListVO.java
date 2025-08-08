package com.core.manycloudcommon.vo.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 市场提成列表VO
 */
@Data
public class SaleDetailListVO {

    /** 消费账号 **/
    private String account;

    /** 角色(0:普通用户,1:推广渠道) **/
    private Integer role;

    /** 角色等级 **/
    private String roleName;

    /** 类型(0:购买提成,1:续费提成) **/
    private Integer type;

    /** 产品ID **/
    private String productNo;

    /** 消费金额 **/
    private BigDecimal consumption;

    /** 提成金额 **/
    private BigDecimal reward;

    /** 提成比率 **/
    private BigDecimal ratio;

    /** 消费账号 **/
    private Date createTime;

}
