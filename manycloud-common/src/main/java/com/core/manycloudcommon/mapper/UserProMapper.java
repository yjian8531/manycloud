package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.UserPro;
import com.core.manycloudcommon.vo.finance.QueryUserProListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface UserProMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(UserPro record);

    int insertSelective(UserPro record);

    UserPro selectByPrimaryKey(Integer id);

    UserPro selectByUserId(@Param("userId") String userId);

    List<UserPro> selectByUserIds(@Param("list")List<String> list);

    List<UserPro> selectByProUserId(@Param("proUserId") String proUserId);

    List<UserPro> selectByProUserIds(@Param("list")List<String> list);

    List<QueryUserProListVO> selectList(Map<String ,Object> param);

    int updateByPrimaryKeySelective(UserPro record);

    int updateByPrimaryKey(UserPro record);
}