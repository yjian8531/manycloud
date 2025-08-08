package com.core.manycloudadmin.so.user;

import lombok.Data;

/**
 * 查询用户列表SO
 */
@Data
public class QueryUserListSO {

    /** 账号 **/
    private String account;
    /** 昵称 **/
    private String nick;
    /** 开始时间 **/
    private String startTime;
    /** 结束时间 **/
    private String endTime;

    private Integer page;
    private Integer pageSize;

}
