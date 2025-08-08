package com.core.manycloudcommon.enums;

/**
 * 实例电源状态枚举
 */
public enum PowerStateEnum {

    RUNNING("运行中","running"),
    HALTED("停止","halted"),
    EXECUTION("执行中","execution");

    private String name;

    private String val;

    PowerStateEnum(String name,String val){
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
