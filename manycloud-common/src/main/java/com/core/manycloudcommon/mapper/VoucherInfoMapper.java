package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.VoucherInfo;
import org.apache.ibatis.annotations.Param;

public interface VoucherInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(VoucherInfo record);

    int insertSelective(VoucherInfo record);

    VoucherInfo selectByPrimaryKey(Integer id);

    VoucherInfo selectByUserId(@Param("userId") String userId);

    int updateByPrimaryKeySelective(VoucherInfo record);

    int updateByPrimaryKey(VoucherInfo record);
}