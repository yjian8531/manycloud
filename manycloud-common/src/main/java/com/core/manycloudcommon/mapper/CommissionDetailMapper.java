package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.CommissionDetail;
import com.core.manycloudcommon.vo.finance.QueryCommissionDetailListVO;
import com.core.manycloudcommon.vo.finance.QueryCommissionStatisticsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CommissionDetailMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(CommissionDetail record);

    int insertSelective(CommissionDetail record);

    CommissionDetail selectByPrimaryKey(Integer id);

    QueryCommissionStatisticsVO selectStatisticsByUser(@Param("userId") String userId);

    int updateByPrimaryKeySelective(CommissionDetail record);

    int updateByPrimaryKey(CommissionDetail record);

    List<QueryCommissionDetailListVO> selectList(Map<String,Object> param);
}