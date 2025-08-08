package com.core.manycloudcommon.entity;

import java.math.BigDecimal;
import java.util.Date;

public class UserFinance {
    private Integer id;

    private String userId;

    private BigDecimal totalNum;

    private BigDecimal validNum;

    private BigDecimal frozenNum;

    private Integer type;

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

    public BigDecimal getTotalNum() {
        return totalNum;
    }

    public void setTotalNum(BigDecimal totalNum) {
        this.totalNum = totalNum;
    }

    public BigDecimal getValidNum() {
        return validNum;
    }

    public void setValidNum(BigDecimal validNum) {
        this.validNum = validNum;
    }

    public BigDecimal getFrozenNum() {
        return frozenNum;
    }

    public void setFrozenNum(BigDecimal frozenNum) {
        this.frozenNum = frozenNum;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
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