package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.VoucherBind;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface VoucherBindMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(VoucherBind record);

    int insertSelective(VoucherBind record);

    VoucherBind selectByPrimaryKey(Integer id);

    List<VoucherBind> selectByUserId(@Param("proUserId") String proUserId);

    int updateByPrimaryKeySelective(VoucherBind record);

    int updateByPrimaryKey(VoucherBind record);
}