package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.FirewallRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface FirewallRuleMapper {
    // 新增
    int insert(FirewallRule firewallRule);
    // 删除
    int deleteByFirewallId(@Param("firewallId") String firewallId);
    // 查询
    List<FirewallRule> selectByFirewallId(@Param("firewallId") String firewallId);
}