
package com.core.manycloudcommon.caller.so;

import lombok.Data;

@Data
public class QueryUserLevelListSO {
    private Integer pageNum;
    private Integer pageSize;
    private String account;
    private String nick;
    private String startTime;
    private String endTime;
    private Integer status; // 0:正常, 1:禁用
    private Integer levelId; // 用户等级ID

}
