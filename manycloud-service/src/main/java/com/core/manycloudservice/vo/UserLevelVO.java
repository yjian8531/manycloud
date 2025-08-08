package com.core.manycloudservice.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class UserLevelVO {

    /** 用户等级 **/
    private Integer level;

    /** 等级名称 **/
    private String levelName;

    /** 当前等级条件 **/
    private BigDecimal requirement;

    /** 享受的折扣 **/
    private String discount;

    /** 总消费 **/
    private BigDecimal consumption;

    /** 一下级条件 **/
    private BigDecimal nextRequirement;

    /** 渠道等级名称 **/
    private String medalName;


}
