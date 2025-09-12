package com.core.manycloudcommon.mapper;


import com.core.manycloudcommon.caller.so.QueryUserLevelListSO;
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






    // 统计新增用户数
    Integer queryCreateNum(@Param("dateStyle") String dateStyle, @Param("dateStr") String dateStr);

    // 统计活跃用户数
    Integer queryActiveNum(@Param("dateStyle") String dateStyle, @Param("dateStr") String dateStr);

    //失活用户查询
    Integer queryInactiveNum( @Param("dateStyle") String dateStyle, @Param("dateStr") String dateStr);

//
//    // 查询用户列表（带等级信息）
//    List<UserListVO> selectListWithLevel(@Param("account") String account,
//                                         @Param("nick") String nick,
//                                         @Param("startTime") String startTime,
//                                         @Param("endTime") String endTime,
//                                         @Param("status") Integer status,
//                                         @Param("levelId") Integer levelId);


    // 查询用户列表（带等级信息）
    List<UserListVO> selectListWithLevel(QueryUserLevelListSO queryUserLevelListSO);








}