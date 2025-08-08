package com.core.manycloudcommon.entity;

import java.math.BigDecimal;
import java.util.Date;

public class InstanceInfo {
    private Integer id;

    private String instanceId;

    private String nike;

    private String orderNo;

    private String userId;

    private Integer type;

    private String label;

    private Integer accountId;

    private Integer nodeId;

    private String serviceNo;

    private String publicIp;

    private String privateIp;

    private String accelerateAdd;

    private Integer connectPort;

    private String connectAccount;

    private String connectPwd;

    private Integer modelId;

    private String cpu;

    private String ram;

    private BigDecimal sysDisk;

    private BigDecimal dataDisk;

    private BigDecimal bandwidth;

    private BigDecimal flow;

    private String image;

    private Integer imageId;

    private String powerState;

    private Integer status;

    private Integer period;

    private String remark;

    private Date createTime;

    private Date endTime;

    private Date updateTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId == null ? null : instanceId.trim();
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

    public String getServiceNo() {
        return serviceNo;
    }

    public void setServiceNo(String serviceNo) {
        this.serviceNo = serviceNo == null ? null : serviceNo.trim();
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp == null ? null : publicIp.trim();
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public void setPrivateIp(String privateIp) {
        this.privateIp = privateIp == null ? null : privateIp.trim();
    }

    public String getAccelerateAdd() {
        return accelerateAdd;
    }

    public void setAccelerateAdd(String accelerateAdd) {
        this.accelerateAdd = accelerateAdd == null ? null : accelerateAdd.trim();
    }

    public Integer getConnectPort() {
        return connectPort;
    }

    public void setConnectPort(Integer connectPort) {
        this.connectPort = connectPort;
    }

    public String getConnectAccount() {
        return connectAccount;
    }

    public void setConnectAccount(String connectAccount) {
        this.connectAccount = connectAccount == null ? null : connectAccount.trim();
    }

    public String getConnectPwd() {
        return connectPwd;
    }

    public void setConnectPwd(String connectPwd) {
        this.connectPwd = connectPwd == null ? null : connectPwd.trim();
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

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getPeriod() {
        return period;
    }

    public void setPeriod(Integer period) {
        this.period = period;
    }

    public String getNike() {
        return nike;
    }

    public void setNike(String nike) {
        this.nike = nike;
    }

    public String getPowerState() {
        return powerState;
    }

    public void setPowerState(String powerState) {
        this.powerState = powerState;
    }
}