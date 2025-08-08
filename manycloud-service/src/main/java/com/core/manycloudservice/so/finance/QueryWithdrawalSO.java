package com.core.manycloudservice.so.finance;

import lombok.Data;

/**
 * 查询提现列表SO
 */
@Data
public class QueryWithdrawalSO {

    private String userId;

    /** 状态(0:申请中,1:审核失败,2:提现成功) **/
    private Integer status;

    private Integer page;

    private Integer pageSize;

}
