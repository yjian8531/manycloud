package com.core.manycloudcommon.caller.so;

import lombok.Data;

/**
 * 查询线下财务订单列表SO
 */
@Data
public class SelectOfflineListSO {

    /** 交易方式(0:银行卡,1:微信,2:支付宝) **/
    private Integer way;

    /** 0:收入,1:支出 **/
    private Integer direction;

    /** 标签(0:渠道返佣,1:退款,2:采购支出) **/
    private Integer tag;

    /** 关联信息 **/
    private String association;

    /** 备注 **/
    private String remark;

    private String startTime;

    private String endTime;

    private Integer pageNum;

    private Integer pageSize;

}
