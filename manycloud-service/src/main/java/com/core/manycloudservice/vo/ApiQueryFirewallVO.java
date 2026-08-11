package com.core.manycloudservice.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * API开放接口 - 查询防火墙规则响应VO（友好格式）
 * 专为 /api/ 接口设计，响应简洁清晰
 */
@Data
@Builder
public class ApiQueryFirewallVO {

    /**
     * 实例ID/防火墙ID
     */
    private String instanceId;

    /**
     * 防火墙规则列表
     */
    private List<ApiFirewallRuleVO> rules;

    /**
     * 规则总数
     */
    private Integer total;
}
