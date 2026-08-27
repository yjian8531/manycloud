
package com.core.manycloudcommon.caller.vo;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
/**
 * 获取 UCloud 套餐信息
 */
public class UcloudBundleVO {
    private String code;
    private String msg;
    private List<BundleInfo> bundles;

    @Data
    public static class BundleInfo {
        private String bundleId;   // 对应 API 返回的 BundleId (即 ProductType)
        private String name;       // 套餐名称
        private int cpu;           // CPU核数
        private int memory;        // 内存 (MB)
        private int disk;          // 系统盘 (GB)
        private int bandwidth;     // 带宽 (Mbps)
        private int traffic;       // 流量包 (GB)
        private double price;      // 价格
    }
}