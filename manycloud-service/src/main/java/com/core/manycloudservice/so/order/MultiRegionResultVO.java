package com.core.manycloudservice.so.order;

import lombok.Data;
import java.util.List;

/**
 * 多地区下单返回数据类
 */
@Data
public class MultiRegionResultVO {
    private String orderNo;           // 订单号
    private Integer successNum;        // 成功数量
    private String totalAmount;       // 总金额
    private List<InstanceInfo> instances;  // 成功的实例
    private List<FailedOrder> failed;       // 失败的订单

    @Data
    public static class InstanceInfo {
        private String instanceId;   // 主机编号
        private String region;       // 地区名称
        private String unitPrice;    // 单价
    }

    @Data
    public static class FailedOrder {
        private Integer nodeId;      // 节点ID
        private String region;       // 地区名称
        private Integer quantity;     // 数量
        private String error;        // 错误信息
    }
}
