package com.core.manycloudcommon.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountApi {

    private String label;

    /** 账号 **/
    private String account;

    /** key **/
    private String keyNo;

    /** 私钥 **/
    private String keySecret;

    /** 地域标识 **/
    private String  regionId;

    /** 项目标识 **/
    private String projectId;

    /** 默认接口地址 **/
    private String baseUrl;

}
