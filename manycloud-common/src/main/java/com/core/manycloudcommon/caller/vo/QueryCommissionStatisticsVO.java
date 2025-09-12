package com.core.manycloudcommon.caller.vo;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class QueryCommissionStatisticsVO {
    /** 推广用户总数 */
    private Integer userNum;
    /** 产品总数 */
    private Integer productNum;
    /** 消费总额 */
    private BigDecimal consumptionTotal;
    /** 返佣总额 */
    private BigDecimal commissionTotal;
    /** 用户ID */
    private String userId;
    /** 用户账号 */
    private String account;
    /** 备注信息 */
    private String remark;
}