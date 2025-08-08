package com.core.manycloudadmin.so.region;

import lombok.Data;

/**
 * 查询地域下拉选择信息SO
 */
@Data
public class QueryRegionSelectSO {

    /** 地域级别(1:大洲,2:国家,3:省份,4:城市) **/
    private Integer superiorLevel;

}
