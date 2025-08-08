package com.core.manycloudcommon.enums;

public enum  MainEnum {
    /** 产品订单 **/
    ORDER("LTO"),
    MAIN("LTM");
    private final String name;

    MainEnum(String name)
    {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
