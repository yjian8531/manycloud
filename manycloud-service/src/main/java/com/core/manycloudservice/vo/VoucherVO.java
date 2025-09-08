package com.core.manycloudservice.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 代价卷VO
 */
@Data
@Builder
public class VoucherVO {

    /** 状态（0:有效，1：无效） **/
    private Integer status;

    /** 代金券ID **/
    private Integer id;

    /** 代金券可用金额 **/
    private BigDecimal amount;

}
