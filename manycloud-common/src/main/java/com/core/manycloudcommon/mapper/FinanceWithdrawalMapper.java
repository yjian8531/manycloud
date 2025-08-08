package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.FinanceWithdrawal;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinanceWithdrawalMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(FinanceWithdrawal record);

    int insertSelective(FinanceWithdrawal record);

    FinanceWithdrawal selectByPrimaryKey(Integer id);

    List<FinanceWithdrawal> selectByUserId(@Param("userId") String userId,@Param("status")Integer status);


    List<FinanceWithdrawal> selectList(@Param("account") String account,@Param("status")Integer status);

    int updateByPrimaryKeySelective(FinanceWithdrawal record);

    int updateByPrimaryKey(FinanceWithdrawal record);
}