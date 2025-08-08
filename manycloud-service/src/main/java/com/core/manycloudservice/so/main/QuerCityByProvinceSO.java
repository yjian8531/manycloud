package com.core.manycloudservice.so.main;

import lombok.Data;

/**
 * 根据省份查询城市信息
 * @return
 */
@Data
public class QuerCityByProvinceSO {
    /** 省份ID **/
    private Integer provinceId;
}

