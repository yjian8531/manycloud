package com.core.manycloudadmin.service;

import com.core.manycloudadmin.so.admin.LoginSO;
import com.core.manycloudadmin.so.admin.QueryAdminLogSO;
import com.core.manycloudcommon.utils.ResultMessage;

public interface AdminService {

    /**
     * 登录
     * @param loginSO
     * @return
     */
    ResultMessage login(LoginSO loginSO);

    /**
     * 获取系统操作日志列表
     * @param queryAdminLogSO
     * @return
     */
    ResultMessage queryAdminLog(QueryAdminLogSO queryAdminLogSO);

}
