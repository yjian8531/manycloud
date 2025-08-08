package com.core.manycloudcommon.entity;

import java.math.BigDecimal;
import java.util.Date;

public class TopupInfo {
    private Integer id;

    private String userId;

    private String topupNo;

    private String intentId;

    private String orderNo;

    private Integer type;

    private BigDecimal moneyNum;

    private Integer way;

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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId == null ? null : userId.trim();
    }

    public String getTopupNo() {
        return topupNo;
    }

    public void setTopupNo(String topupNo) {
        this.topupNo = topupNo == null ? null : topupNo.trim();
    }

    public String getIntentId() {
        return intentId;
    }

    public void setIntentId(String intentId) {
        this.intentId = intentId == null ? null : intentId.trim();
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo == null ? null : orderNo.trim();
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public BigDecimal getMoneyNum() {
        return moneyNum;
    }

    public void setMoneyNum(BigDecimal moneyNum) {
        this.moneyNum = moneyNum;
    }

    public Integer getWay() {
        return way;
    }

    public void setWay(Integer way) {
        this.way = way;
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
}