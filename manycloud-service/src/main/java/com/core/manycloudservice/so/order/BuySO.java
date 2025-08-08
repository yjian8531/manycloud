package com.core.manycloudservice.so.order;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单购买SO
 */
@Data
public class BuySO {

    /** 订单集合 **/
    private List<String> orderNos;

    /** 金额 **/
    private BigDecimal amount;

}
