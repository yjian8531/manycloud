package com.core.manycloudservice.so.main;

import lombok.Data;

/**
 * 根据国家查询省份信息SO
 * @return
 */
@Data
public class QueryProvinceByCountrySO {

    /** 国家ID **/
    private Integer countryId;

}
