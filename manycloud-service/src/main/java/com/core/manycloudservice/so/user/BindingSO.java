package com.core.manycloudservice.so.user;

import lombok.Data;

/**
 * 绑定微信操作
 */
@Data
public class BindingSO {

    /** 账号ID **/
    private String account;

    /** openID **/
    private String code;

    /** 登录密码 **/
    private String logPwd;

}
