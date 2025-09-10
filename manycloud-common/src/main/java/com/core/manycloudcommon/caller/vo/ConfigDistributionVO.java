package com.core.manycloudcommon.caller.vo;

import com.core.manycloudcommon.caller.Item.ConfigItemVO;
import lombok.Data;

import java.util.List;

@Data
public class ConfigDistributionVO {
    private String platform; // 平台名称（如 AWS）
    private List<ConfigItemVO> items; // 规格分布列表
}