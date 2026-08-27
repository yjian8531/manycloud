package com.core.manycloudcommon.caller.so;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 查询Ucloud轻量应用主机套餐列表参数
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DescribeULHostBundlesSO {

    /** 镜像类型 (可选) */
    private String imageType;

}
