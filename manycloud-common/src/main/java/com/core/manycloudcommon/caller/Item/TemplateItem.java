package com.core.manycloudcommon.caller.Item;// com.core.manycloudcommon.caller.Item.TemplateItem
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TemplateItem {
    private Integer id;
    private String templateId;
    private Integer clusterId;
    private String model;
    private Integer cpuCores;
    private String cpuModel;
    private Integer memory;
    private Integer diskCapacity;
    private String diskModel;
    private Integer ioWrite;
    private Integer ioRead;
    private Integer bandwidth;
    private Integer internetTraffic;
    private String ipV4;
    private String ipV6;
    private String refId;
    private BigDecimal priceMonthly;
    private BigDecimal priceQuarterly;
    private BigDecimal priceSemiAnnually;
    private BigDecimal priceAnnually;
}
