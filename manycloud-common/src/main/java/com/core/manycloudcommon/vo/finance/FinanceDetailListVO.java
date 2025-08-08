package com.core.manycloudcommon.vo.finance;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class FinanceDetailListVO {

    private Integer id;
    private String financeNo;
    private Integer direction;
    private String tag;
    private BigDecimal moneyNum;
    private Date createTime;
    private String email;
    private String ip;
    private String instanceId;

}
