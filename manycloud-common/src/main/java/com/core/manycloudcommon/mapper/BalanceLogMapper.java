package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.BalanceLog;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

public interface BalanceLogMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(BalanceLog record);

    int insertSelective(BalanceLog record);

    /** 标签(add：添加余额,minus：减去余额,unbind：解冻余额,seal：冻结余额) **/
    int insertChange(@Param("userId") String userId,@Param("type") String type,@Param("amount") BigDecimal amount,
                     @Param("balance") BigDecimal balance, @Param("remark")String remark);

    BalanceLog selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(BalanceLog record);

    int updateByPrimaryKey(BalanceLog record);
}