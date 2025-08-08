package com.core.manycloudservice.so.user;

import lombok.Data;

/**
 * 修改用户邮箱SO
 */
@Data
public class UpdateUserEmailSO {

    private String email;

    private String loginPwd;

}
