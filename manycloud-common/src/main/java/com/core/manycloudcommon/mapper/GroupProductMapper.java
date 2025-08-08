package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.GroupProduct;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GroupProductMapper {
    int deleteByPrimaryKey(Integer id);

    int insertBatch(@Param("groupId")Integer groupId, @Param("list") List<String> list);

    int insert(GroupProduct record);

    int insertSelective(GroupProduct record);

    GroupProduct selectByPrimaryKey(Integer id);

    GroupProduct selectByInstance(@Param("instanceId")String instanceId);

    int updateByPrimaryKeySelective(GroupProduct record);

    int updateByPrimaryKey(GroupProduct record);

    int deleteBatch(@Param("list")List<String> list);

    int deleteByGroupId(@Param("groupId") Integer groupId);
}