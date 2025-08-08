package com.core.manycloudcommon.entity;

import java.math.BigDecimal;
import java.util.Date;

public class OrderInfo {
    private Integer id;

    private String orderNo;

    private String userId;

    private Integer num;

    private Integer period;

    private Integer duration;

    private BigDecimal price;

    private BigDecimal onlyPrice;

    private BigDecimal discount;

    private Integer type;

    private String label;

    private Integer accountId;

    private Integer nodeId;

    private Integer modelId;

    private String cpu;

    private String ram;

    private BigDecimal sysDisk;

    private BigDecimal dataDisk;

    private BigDecimal bandwidth;

    private BigDecimal flow;

    private String image;

    private Integer imageId;

    private Integer status;

    private String remark;

    private Date createTime;

    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo == null ? null : orderNo.trim();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId == null ? null : userId.trim();
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public void setDiscount(BigDecimal discount) {
        this.discount = discount;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
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

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu == null ? null : cpu.trim();
    }

    public String getRam() {
        return ram;
    }

    public void setRam(String ram) {
        this.ram = ram == null ? null : ram.trim();
    }

    public BigDecimal getSysDisk() {
        return sysDisk;
    }

    public void setSysDisk(BigDecimal sysDisk) {
        this.sysDisk = sysDisk;
    }

    public BigDecimal getDataDisk() {
        return dataDisk;
    }

    public void setDataDisk(BigDecimal dataDisk) {
        this.dataDisk = dataDisk;
    }

    public BigDecimal getBandwidth() {
        return bandwidth;
    }

    public void setBandwidth(BigDecimal bandwidth) {
        this.bandwidth = bandwidth;
    }

    public BigDecimal getFlow() {
        return flow;
    }

    public void setFlow(BigDecimal flow) {
        this.flow = flow;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image == null ? null : image.trim();
    }

    public Integer getImageId() {
        return imageId;
    }

    public void setImageId(Integer imageId) {
        this.imageId = imageId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark == null ? null : remark.trim();
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

    public BigDecimal getOnlyPrice() {
        return onlyPrice;
    }

    public void setOnlyPrice(BigDecimal onlyPrice) {
        this.onlyPrice = onlyPrice;
    }
}