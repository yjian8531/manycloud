// ClusterItem.java
package com.core.manycloudcommon.caller.Item;

import lombok.Data;

@Data
public class ClusterItem {
    private Integer id;
    private String clusterName;
    private String clusterType;
    private String continent;
    private String countryCode;
    private String stateCode;
}
