package com.core.manycloudcommon.enums;

/**
 * 系统日志类型枚举
 */
public enum SyslogTypeEnum {

    RECHARGE("充值","recharge"),
    CONSUME("消费","consume"),
    COMMISSION("佣金","commission"),
    NOTIFY("通知","notify");

    private String name;

    private String val;

    SyslogTypeEnum(String name,String val){
        this.name = name;
        this.val = val;
    }

    public static PowerStateEnum getByVal(String val){
        PowerStateEnum pl = null;
        for(PowerStateEnum ps : PowerStateEnum.values()){
            if(ps.getVal().equals(val)){
                pl = ps;
                break;
            }
        }
        return pl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVal() {
        return val;
    }
    public void setVal(String val) {
        this.val = val;
    }

    @Override
    public String toString() {
        return val;
    }

}
