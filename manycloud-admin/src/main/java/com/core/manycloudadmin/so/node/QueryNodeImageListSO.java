package com.core.manycloudadmin.so.node;

import com.core.manycloudadmin.so.BaseQueryPageSO;
import lombok.Data;

/**
 * 查询节点镜像列表SO
 */
@Data
public class QueryNodeImageListSO extends BaseQueryPageSO {

    /** 节点ID **/
    private Integer nodeId;

    /** 镜像类型 **/
    private String imageType;

}
