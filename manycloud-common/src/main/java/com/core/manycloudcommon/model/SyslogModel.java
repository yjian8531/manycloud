package com.core.manycloudcommon.model;

import com.core.manycloudcommon.entity.SysLog;
import com.core.manycloudcommon.enums.SyslogTypeEnum;
import com.core.manycloudcommon.utils.DateUtil;

import java.math.BigDecimal;
import java.util.Date;

/***
 * 系统日志模型
 */
public class SyslogModel {

    /***
     * 输出消费系统日志
     * @param userId 用户ID
     * @param content 日志内容
     * @return
     */
    public static SysLog output(String userId,SyslogTypeEnum syslogTypeEnum,String content){
        SysLog sysLog = new SysLog();
        sysLog.setType(syslogTypeEnum.getVal());
        sysLog.setUserId(userId);
        sysLog.setContent(content);
        sysLog.setStatus(0);
        sysLog.setCreateTime(new Date());
        return sysLog;
    }




}
