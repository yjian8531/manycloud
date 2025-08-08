package com.core.manycloudadmin.so.user;

import lombok.Data;

/**
 * 更新用户备注
 */
@Data
public class UpdateUserRemarkSO {

    /** 用户ID **/
    private String userId;

    /** 备注 **/
    private String remar;
}
