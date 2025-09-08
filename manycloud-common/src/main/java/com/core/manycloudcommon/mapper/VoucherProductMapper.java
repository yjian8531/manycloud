package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.VoucherProduct;

public interface VoucherProductMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(VoucherProduct record);

    int insertSelective(VoucherProduct record);

    VoucherProduct selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(VoucherProduct record);

    int updateByPrimaryKey(VoucherProduct record);
}