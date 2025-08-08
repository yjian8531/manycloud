package com.core.manycloudadmin.so.node;

import lombok.Data;

/**
 * 更新节点镜像信息
 */
@Data
public class UpdateNodeImageSO {

    private Integer id;

    /** 节点ID **/
    private Integer nodeId;

    /** 镜像类型 **/
    private String imageType;

    /** 镜像版本 **/
    private String imageVersion;

    /** 镜像参数 **/
    private String imageParam;

    /** 备注 **/
    private String remark;

    /** 状态(0:正常,1:禁用) **/
    private Integer status;

}
