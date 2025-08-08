package com.core.manycloudservice.so.instance;

import lombok.Data;

/**
 * 查询用户实例列表SO
 */
@Data
public class QueryListBuUserSO {

    /** 实例ID 或 公网IP **/
    private String instanceId;
    /** 电源状态：halted(停止)、running(运行中),execution(执行中) **/
    private String powerState;
    /** 状态(0:待创建,1:创建中,3:使用中,4:待续费,5:已过期) **/
    private Integer status;
    /** 分组ID **/
    private Integer groupId;
    /** 排序(null:创建时间倒序，0:到期时间升序，1:到期时间倒序) **/
    private Integer sort;

    private Integer page;

    private Integer pageSize;

}
