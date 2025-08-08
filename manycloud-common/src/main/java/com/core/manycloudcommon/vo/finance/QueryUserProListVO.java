package com.core.manycloudcommon.vo.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 查询用户推广明细VO
 */
@Data
public class QueryUserProListVO {
    private String userId;
    /** 推广用户手机 **/
    private String phone;
    /** 推广用户邮箱 **/
    private String email;
    /** 推广用户昵称 **/
    private String name;
    /** 创建时间 **/
    private Date createTime;
    /** 产品总数 **/
    private Integer productNum;
    /** 消费总额 **/
    private BigDecimal consumptionTotal;
    /** 返佣总额 **/
    private BigDecimal commissionTotal;
}
