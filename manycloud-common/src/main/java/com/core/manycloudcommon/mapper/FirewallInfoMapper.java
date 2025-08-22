package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.FirewallInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
@Mapper
public interface FirewallInfoMapper {
    //添加
    int insert(FirewallInfo firewallInfo);
    //修改
    int updateByPrimaryKey(FirewallInfo firewallInfo);
    //删除
    int deleteByPrimaryKey(Integer id);
    //查询
    FirewallInfo selectByPrimaryKey(Integer id);
    //根据实例ID查询
    List<FirewallInfo> selectByInstanceId(@Param("instanceId") String instanceId);
    //根据用户ID查询
    List<FirewallInfo> selectByUserId(@Param("userId") String userId);
    //根据防火墙ID查询
    FirewallInfo selectByFirewallId(@Param("firewallId") String firewallId);
}
