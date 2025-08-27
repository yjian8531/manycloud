// ClusterVO.java
package com.core.manycloudcommon.caller.vo;

import com.core.manycloudcommon.caller.Item.ClusterItem;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClusterVO {
    private Integer total;
    private List<ClusterItem> rows;
    private String code;
    private String msg;
}
