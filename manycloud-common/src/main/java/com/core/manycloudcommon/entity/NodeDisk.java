package com.core.manycloudcommon.entity;

import java.math.BigDecimal;
import java.util.Date;

public class NodeDisk {
    private Integer id;

    private Integer nodeId;

    private Integer modelId;

    private String diskType;

    private String modelParam;

    private BigDecimal minNum;

    private BigDecimal maxNum;

    private BigDecimal itemNum;

    private BigDecimal giveNum;

    private String extendBl;

    private String dataBl;

    private String remark;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    private BigDecimal item;

    private BigDecimal price;

    private String modelConfig;

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

    public Integer getModelId() {
        return modelId;
    }

    public void setModelId(Integer modelId) {
        this.modelId = modelId;
    }

    public String getDiskType() {
        return diskType;
    }

    public void setDiskType(String diskType) {
        this.diskType = diskType == null ? null : diskType.trim();
    }

    public String getModelParam() {
        return modelParam;
    }

    public void setModelParam(String modelParam) {
        this.modelParam = modelParam == null ? null : modelParam.trim();
    }

    public BigDecimal getMinNum() {
        return minNum;
    }

    public void setMinNum(BigDecimal minNum) {
        this.minNum = minNum;
    }

    public BigDecimal getMaxNum() {
        return maxNum;
    }

    public void setMaxNum(BigDecimal maxNum) {
        this.maxNum = maxNum;
    }

    public BigDecimal getItemNum() {
        return itemNum;
    }

    public void setItemNum(BigDecimal itemNum) {
        this.itemNum = itemNum;
    }

    public BigDecimal getGiveNum() {
        return giveNum;
    }

    public void setGiveNum(BigDecimal giveNum) {
        this.giveNum = giveNum;
    }

    public String getExtendBl() {
        return extendBl;
    }

    public void setExtendBl(String extendBl) {
        this.extendBl = extendBl == null ? null : extendBl.trim();
    }

    public String getDataBl() {
        return dataBl;
    }

    public void setDataBl(String dataBl) {
        this.dataBl = dataBl == null ? null : dataBl.trim();
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

    public BigDecimal getItem() {
        return item;
    }

    public void setItem(BigDecimal item) {
        this.item = item;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getModelConfig() {
        return modelConfig;
    }

    public void setModelConfig(String modelConfig) {
        this.modelConfig = modelConfig;
    }
}