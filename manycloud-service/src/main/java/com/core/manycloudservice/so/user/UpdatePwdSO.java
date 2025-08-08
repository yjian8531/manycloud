package com.core.manycloudservice.so.user;

import lombok.Data;

/**
 * 修改密码SO
 */
@Data
public class UpdatePwdSO {


    /** 验证码类型(0:公众号验证码，1:邮箱验证码) **/
    private Integer codeType;
    /** 验证码 **/
    private String code;
    /** 邮箱 **/
    private String email;
    /** 新密码 **/
    private String newPwd;

}
