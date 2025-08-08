package com.core.manycloudadmin.so.node;

import lombok.Data;

/**
 * 更新资源平台账号信息
 */
@Data
public class UpdatePlatformAccountSO {

    private Integer id;

    /** 资源平台标签 **/
    private String label;

    /** 账号 **/
    private String account;

    /** Key **/
    private String keyNo;

    /** 私钥 **/
    private String keySecret;

    /** 接口地址 **/
    private String url;

    /** 默认标记(0:否,1:是) **/
    private Integer del;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

    /** 备注 **/
    private String remark;

}
