package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.UserFinance;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Map;

public interface UserFinanceMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(UserFinance record);

    int insertSelective(UserFinance record);

    UserFinance selectByPrimaryKey(Integer id);

    UserFinance selectByUserId(@Param("userId") String userId);

    /**
     *
     * @param userId 用户ID
     * @param tad 标签(add：添加余额,minus：减去余额,unbind：解冻余额,seal：冻结余额)
     * @param num 金额
     * @return
     */
    int updateBalanceByUserId(String userId, String tad, BigDecimal num);

    int updateByPrimaryKeySelective(UserFinance record);

    int updateByPrimaryKey(UserFinance record);
}