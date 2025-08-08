package com.core.manycloudadmin.so.node;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 添加资源平台
 */
@Data
public class AddPlatformInfoSO {

    /** 平台标签 **/
    private String label;

    /** 名称 **/
    private String name;

    /** 登录地址 **/
    private String urlAddress;

    /** 账号 **/
    private String loginAccount;

    /** 密码 **/
    private String loginPwd;

    /** 余额 **/
    private BigDecimal financeNum;

    /** 主机数量 **/
    private Integer mainNum;

    /** 自动(0:否,1:是) **/
    private Integer auto;

    /** 备注 **/
    private String remark;

}
