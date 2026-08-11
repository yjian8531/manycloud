package com.core.manycloudservice.so.order;

import lombok.Data;
import java.util.List;

/**
 * 多地区下单参数类
 */
@Data
public class MultiRegionOrderSO {
    private List<RegionOrder> orders;

    @Data
    public static class RegionOrder {
        private Integer nodeId;      // 节点ID
        private Integer modelId;      // 模版ID
        private String imageId;       // 镜像ID
        private Integer period;       // 购买时长
        private Integer quantity;     // 数量
    }
}
