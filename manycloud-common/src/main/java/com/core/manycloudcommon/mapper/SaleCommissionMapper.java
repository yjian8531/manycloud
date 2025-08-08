package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.SaleCommission;

public interface SaleCommissionMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(SaleCommission record);

    int insertSelective(SaleCommission record);

    SaleCommission selectByPrimaryKey(Integer id);

    /**
     * 查询提成比率
     * @param userType 内部用户类型
     * @param role 角色(0:普通用户,1:推广渠道)
     * @param levelId 角色等级ID
     * @return
     */
    SaleCommission selectRatio(Integer userType,Integer role,Integer levelId);

    int updateByPrimaryKeySelective(SaleCommission record);

    int updateByPrimaryKey(SaleCommission record);
}