package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.OrderExce;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderExceMapper {

    int insertSelective(OrderExce record);

    OrderExce selectByPrimaryKey(Integer id);

    List<OrderExce> selectList(@Param("status") Integer status);

    int updateByPrimaryKeySelective(OrderExce record);
}
