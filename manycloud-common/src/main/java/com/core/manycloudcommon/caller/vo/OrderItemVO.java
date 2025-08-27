package com.core.manycloudcommon.caller.vo;


import lombok.Data;

@Data
public class OrderItemVO {
    private String orderNo;
    private String hostIp;
    private String vpsId;
    private String vpsCode;
    private String vpsName;
    private Integer clusterId;
    private String hostTemplateId;
    private String monthNumber;
    private String continent;
    private String countryCode;
    private String state;
    private String effectiveDatetime;
    private String expireDatetime;
    private String model;
    private Integer cpuCores;
    private String cpuModel;
    private Integer memory;
    private String diskModel;
    private Integer diskCapacity;
    private Integer ioWrite;
    private Integer ioRead;
    private Integer bandwidth;
    private Integer internetTraffic;
    private String ipV4;
    private String ipV6;

}
