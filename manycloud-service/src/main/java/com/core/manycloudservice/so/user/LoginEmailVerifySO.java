package com.core.manycloudservice.so.user;

import lombok.Data;

/**
 * 获取邮箱注册验证码SO
 */
@Data
public class LoginEmailVerifySO {

    /** 图形验证码 **/
    private String code;
    /** 邮箱 **/
    private String email;

}
