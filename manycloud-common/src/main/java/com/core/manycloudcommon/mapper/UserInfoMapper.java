package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.vo.user.UserListVO;
import com.core.manycloudcommon.vo.user.UserSelectVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(UserInfo record);

    int insertSelective(UserInfo record);

    UserInfo selectByPrimaryKey(Integer id);

    List<UserSelectVO> selectSaleByType();

    UserInfo selectById(@Param("userId") String userId);

    UserInfo selectByAccount(@Param("account") String account);

    UserInfo selectByMarket(@Param("market") String market);

    List<UserListVO> selectList(@Param("account")String account ,@Param("nick")String nick,@Param("startTime")String startTime,@Param("endTime")String endTime);

    int updateByPrimaryKeySelective(UserInfo record);

    int updateByPrimaryKey(UserInfo record);
}