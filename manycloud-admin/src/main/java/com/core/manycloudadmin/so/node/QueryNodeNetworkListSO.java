package com.core.manycloudadmin.so.node;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

/**
 * 查询节点网络配置列表
 */
@Data
public class QueryNodeNetworkListSO extends BaseQueryPageSO {

    private Integer nodeId;

}
