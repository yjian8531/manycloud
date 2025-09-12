package com.core.manycloudcommon.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class OfflineFinance {
    /**
     * 主键id
     */
    private Integer id;
    
    /**
     * 金额
     */
    private BigDecimal amountNum;
    
    /**
     * 收款方式(0:S-支付宝,1:S-微信,2:S-银行卡,3:G-微信,4:G-支付宝)
     */
    private Integer way;
    
    /**
     * 0:收入,1:支出
     */
    private Integer direction;
    
    /**
     * 标签(0:渠道返佣,1:退款,2:采购支出)
     */
    private Integer tag;
    
    /**
     * 关联信息
     */
    private String association;
    
    /**
     * 备注
     */
    private String remark;
    
    /**
     * 发生时间
     */
    private Date occurTime;
    
    /**
     * 创建时间
     */
    private Date createTime;
}