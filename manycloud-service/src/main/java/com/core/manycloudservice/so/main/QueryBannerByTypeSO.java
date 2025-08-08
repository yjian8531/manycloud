package com.core.manycloudservice.so.main;

import lombok.Data;

/***
 * 根据类型查询Banner图信息
 */
@Data
public class QueryBannerByTypeSO {

    /** 类型(1:首页,2:关于我们,3:帮助中心) **/
    private Integer type;

}
