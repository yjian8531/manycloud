package com.core.manycloudservice.so.user;

import lombok.Data;

/**
 * 登录SO
 */
@Data
public class LoginSO {

    /** 账号 **/
    private String email;
    /** 密码 **/
    private String pwd;
    /** ip **/
    private String ip;
}
