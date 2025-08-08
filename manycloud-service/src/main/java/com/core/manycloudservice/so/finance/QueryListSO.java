package com.core.manycloudservice.so.finance;

import lombok.Data;

/**
 * 查询账单明细列表数据SO
 */
@Data
public class QueryListSO {
    /** 用户ID **/
    private String userId;
    /** 0:收入,1:支出 **/
    private Integer direction;
    /** 标签(topup:充值,buy:购买,renew:续费,manage:人工操作) **/
    private String tag;
    /** 开始时间 **/
    private String startTime;
    /** 结束时间 **/
    private String endTime;
    /** 关联产品 **/
    private String productNo;
    /** 用户邮箱 **/
    private String email;

    private Integer page;
    private Integer pageSize;
    
}
