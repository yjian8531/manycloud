package com.core.manycloudadmin.so.node;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

/**
 * 查询节点磁盘配置列表
 */
@Data
public class QueryNodeDiskListSO extends BaseQueryPageSO {

    private Integer nodeId;

}
