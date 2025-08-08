package com.core.manycloudadmin.so.finance;

import lombok.Data;

@Data
public class QueryWithdrawalListSO {

    private String account;

    /** 状态(0:申请中,1:审核失败,2:提现成功) **/
    private Integer status;

    private Integer page;

    private Integer pageSize;

}
