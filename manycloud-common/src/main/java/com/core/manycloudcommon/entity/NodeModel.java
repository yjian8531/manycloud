package com.core.manycloudcommon.entity;

import java.util.Date;

public class NodeModel {
    private Integer id;

    private Integer nodeId;

    private String cpuVal;

    private String ramVal;

    private String modelParam;

    private String regular;

    private String remark;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNodeId() {
        return nodeId;
    }

    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }

    public String getCpuVal() {
        return cpuVal;
    }

    public void setCpuVal(String cpuVal) {
        this.cpuVal = cpuVal == null ? null : cpuVal.trim();
    }

    public String getRamVal() {
        return ramVal;
    }

    public void setRamVal(String ramVal) {
        this.ramVal = ramVal == null ? null : ramVal.trim();
    }

    public String getModelParam() {
        return modelParam;
    }

    public void setModelParam(String modelParam) {
        this.modelParam = modelParam == null ? null : modelParam.trim();
    }

    public String getRegular() {
        return regular;
    }

    public void setRegular(String regular) {
        this.regular = regular == null ? null : regular.trim();
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark == null ? null : remark.trim();
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}