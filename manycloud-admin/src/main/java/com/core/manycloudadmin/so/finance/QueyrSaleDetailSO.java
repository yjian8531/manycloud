package com.core.manycloudadmin.so.finance;

import lombok.Data;

/**
 * 查询市场人员提成列表SO
 */
@Data
public class QueyrSaleDetailSO {

    /** 市场人员账号ID **/
    private String userId;

    /** 月份 **/
    private String monthStr;

    private Integer page;
    private Integer pageSize;

}
