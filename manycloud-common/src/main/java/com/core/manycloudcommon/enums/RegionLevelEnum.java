package com.core.manycloudcommon.enums;

/**
 * 地域级别
 */
public enum RegionLevelEnum {

    CONTINENT("大洲",1),
    COUNTRY("国家",2),
    PROVINCE("省份",3),
    CITY("城市",4);

    private String name;

    private int level;

    RegionLevelEnum(String name,int level){
        this.name = name;
        this.level = level;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}
