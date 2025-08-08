package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.GroupInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface GroupInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(GroupInfo record);

    int insertSelective(GroupInfo record);

    GroupInfo selectByPrimaryKey(Integer id);

    List<GroupInfo> selectList(Map<String,Object> map);

    int updateByPrimaryKeySelective(GroupInfo record);

    int updateByPrimaryKey(GroupInfo record);

    int updateProductNum(@Param("id") Integer id);
}