package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.caller.vo.QueryCommissionStatisticsVO;
import com.core.manycloudcommon.caller.vo.QueryUserProListVOTO;
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
    /** 查询推广明细列表 */
    List<QueryUserProListVO> selectList(Map<String ,Object> param);

    int updateByPrimaryKeySelective(UserPro record);

    int updateByPrimaryKey(UserPro record);

    /** 查询推广用户基础信息（用户数、账号、备注等） */
    List<QueryCommissionStatisticsVO> selectListByStatistics(@Param("account") String account);

    /** 查询推广明细列表 */
    List<QueryUserProListVOTO> selectListUser(Map<String, Object> param);


    // 批量查询多个推广人的下属被推广人ID
    List<String> selectSubUserIdsByPromoterIds(List<String> promoterIds);

    // 查询单个推广人的下属被推广人ID
    List<String> selectSubUserIdsByPromoterId(String promoterId);
}