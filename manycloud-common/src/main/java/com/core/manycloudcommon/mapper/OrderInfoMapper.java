package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.OrderInfo;
import com.core.manycloudcommon.vo.order.ShoppingListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface OrderInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(OrderInfo record);

    int insertSelective(OrderInfo record);

    OrderInfo selectByPrimaryKey(Integer id);

    OrderInfo selectByNo(@Param("orderNo") String orderNo);

    int updateByPrimaryKeySelective(OrderInfo record);

    int updateByPrimaryKey(OrderInfo record);

    int deleteBatch(List<Integer> list);

    List<ShoppingListVO> selectShoppingList(String userId);
}