package com.core.manycloudadmin.so.admin;

import lombok.Data;

/***
 * 管理员登录
 */
@Data
public class LoginSO {
    /** 账号 **/
    private String account;
    /** 密码 **/
    private String pwd;
}
