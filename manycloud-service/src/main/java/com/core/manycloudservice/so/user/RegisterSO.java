package com.core.manycloudservice.so.user;

import lombok.Data;

/***
 * 用户注册SO
 */
@Data
public class RegisterSO {

    /** 注册验证码 **/
    private String code;
    /** 验证码类型(0:公众号验证码，1:邮箱验证码) **/
    private Integer codeType;
    /** 手机号 **/
    private String phone;
    /** 邮箱 **/
    private String email;
    /** 密码 **/
    private String pwd;
    /** 昵称 **/
    private String name;
    /** 推广码 **/
    private String market;
    /** 类型 **/
    private Integer type;
    /** IP地址 **/
    private String ip;
    /** 关注微信公众号openId **/
    private String openId;

}
