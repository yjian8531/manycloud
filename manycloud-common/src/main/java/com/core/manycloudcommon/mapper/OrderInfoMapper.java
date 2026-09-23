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

    /**
     * 仅当订单当前状态为0时才更新为2
     * @return 影响行数（0=状态不满足，抢锁失败；1=成功）
     */
    int casStatusByOrderNo(@Param("orderNo") String orderNo,
                           @Param("fromStatus") Integer fromStatus,
                           @Param("toStatus") Integer toStatus);

    int updateByPrimaryKeySelective(OrderInfo record);

    int updateByPrimaryKey(OrderInfo record);

    int deleteBatch(List<Integer> list);

    List<ShoppingListVO> selectShoppingList(String userId);
}