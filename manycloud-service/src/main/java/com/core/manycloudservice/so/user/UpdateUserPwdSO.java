package com.core.manycloudservice.so.user;

import lombok.Data;

/**
 * 修改用户密码O
 */
@Data
public class UpdateUserPwdSO {

    private String newPwd;

    private String loginPwd;

}
