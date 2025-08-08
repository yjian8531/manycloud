package com.core.manycloudcommon.enums;

public enum TaskTypeEnum {

    AHZ_BUY("异步创建监控",1),
    EXECPOWER("主机电源操作",2),
    AHZ_RENEW("异步续费监控",3),
    AHZ_BUY_CTE("异步创建监控延续",4),
    RESET("重装电源操作",5);

    private String name;

    private Integer type;

    TaskTypeEnum(String name,Integer type){
        this.name = name;
        this.type = type;
    }

    public static TaskTypeEnum getByType(Integer type){
        TaskTypeEnum t = null;
        for(TaskTypeEnum te : TaskTypeEnum.values()){
            if(te.getType().equals(type)){
                t = te;
                break;
            }
        }
        return t;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }
}
