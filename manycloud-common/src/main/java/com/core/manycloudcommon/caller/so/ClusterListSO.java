// ClusterListSO.java
package com.core.manycloudcommon.caller.so;

import lombok.Data;

@Data
public class ClusterListSO {
    private Integer pageNum = 1; // 默认值
    private Integer pageSize = 20; // 默认值
    private String continent; // 如: North America
    private String countryCode; // 如: US
}
