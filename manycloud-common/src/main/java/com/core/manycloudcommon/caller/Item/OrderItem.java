package com.core.manycloudcommon.caller.Item;


import com.core.manycloudcommon.caller.vo.OrderItemVO;
import lombok.Data;

import java.util.List;


@Data
public class OrderItem {
    private String orderNo;
    private Integer quantity;
    private Double totalAmount;
    private String currency;
    private Integer paidStatus;
    private String payMethod;
    private String effectiveDatetime;
    private String expireDatetime;
    private List<OrderItemVO> orderItems;

}
