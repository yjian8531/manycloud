package com.core.manycloudcommon.vo.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 查询用户返佣明细VO
 */
@Data
public class QueryCommissionDetailListVO {

    /** 返佣用户邮箱 **/
    private String email;
    /** 返佣用户昵称 **/
    private String name;
    /** 创建时间 **/
    private Date createTime;
    /** 返佣比率 **/
    private BigDecimal ratio;
    /** 返佣总数 **/
    private BigDecimal commission;
    /** 消费总额 **/
    private BigDecimal consumption;

    private String phone;
}
