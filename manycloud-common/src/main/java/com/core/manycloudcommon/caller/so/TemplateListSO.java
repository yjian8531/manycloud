package com.core.manycloudcommon.caller.so;


import lombok.Data;

@Data
public class TemplateListSO {
    private Integer clusterId;        // 必选：集群ID
    private String templateId;     // 可选：模板ID
    private Integer pageNum;       // 分页页码
    private Integer pageSize;      // 每页数量


}
