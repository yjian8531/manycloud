package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.VoucherLog;

public interface VoucherLogMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(VoucherLog record);

    int insertSelective(VoucherLog record);

    VoucherLog selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(VoucherLog record);

    int updateByPrimaryKey(VoucherLog record);
}