package com.core.manycloudcommon.enums;

public enum PlatformLabelEnum {

    ALIYUN("阿里云","ALIYUN",true,true),
    AWSLS("AWS LightSail","AWSLS",false,false),
    UCLOUD("Ucloud","UCLOUD",false,true),
    RCLOUD("Rcloud","RCLOUD",false,true),
    DOPLA("数字海洋","DOPLA",false,false),
    AKMPLA("阿卡麦","AKMPLA",false,false),
    IPLIGHT("IPLIGHT","IPLIGHT",false,false);


    private String name;

    private String label;

    /** 创建延续任务(是/否) **/
    private boolean cteBl;

    /** 是否自动销毁(是/否) **/
    private boolean destroyBl;

    PlatformLabelEnum(String name,String label,boolean cteBl,boolean destroyBl){
        this.name = name;
        this.label = label;
        this.cteBl = cteBl;
        this.destroyBl = destroyBl;
    }

    public static PlatformLabelEnum getByLabel(String label){
        PlatformLabelEnum pl = null;
        for(PlatformLabelEnum ple : PlatformLabelEnum.values()){
            if(ple.getLabel().equals(label)){
                pl = ple;
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

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isCteBl() {
        return cteBl;
    }

    public void setCteBl(boolean cteBl) {
        this.cteBl = cteBl;
    }

    public boolean isDestroyBl() {
        return destroyBl;
    }

    public void setDestroyBl(boolean destroyBl) {
        this.destroyBl = destroyBl;
    }
}
