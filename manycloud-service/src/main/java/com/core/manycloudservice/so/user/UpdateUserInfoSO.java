package com.core.manycloudservice.so.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserInfoSO {

    private Integer id;//

    private String email;//邮件

    private String nickName;//昵称

    private String loginPwd;//登录密码

    private String phone;//手机

    private Integer type;//用户类型(0:普通用户,1:公司内部账号)

    private Integer status;//状态(0:正常,1:禁用)

    private String remark;//备注

}
