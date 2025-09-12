package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.caller.vo.QueryCommissionStatisticsVO;
import com.core.manycloudcommon.entity.CommissionDetail;
import com.core.manycloudcommon.vo.finance.QueryCommissionDetailListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CommissionDetailMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(CommissionDetail record);

    int insertSelective(CommissionDetail record);

    CommissionDetail selectByPrimaryKey(Integer id);

    com.core.manycloudcommon.vo.finance.QueryCommissionStatisticsVO selectStatisticsByUser(@Param("userId") String userId);

    int updateByPrimaryKeySelective(CommissionDetail record);

    int updateByPrimaryKey(CommissionDetail record);

    List<QueryCommissionDetailListVO> selectList(Map<String,Object> param);



    /** 查询用户消费、返佣总额 */
    List<QueryCommissionStatisticsVO> selectStatisticsByUserIds(@Param("userIds") List<String> userIds);

    List<com.core.manycloudcommon.vo.finance.QueryCommissionStatisticsVO> selectStatisticsByPromoterId(@Param("promoterId") String promoterId);

    List<QueryCommissionDetailListVO> selectListUser(Map<String,Object> param);
}