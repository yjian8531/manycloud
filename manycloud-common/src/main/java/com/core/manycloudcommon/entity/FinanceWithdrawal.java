package com.core.manycloudcommon.entity;

import java.math.BigDecimal;
import java.util.Date;

public class FinanceWithdrawal {
    private Integer id;

    private String userId;

    private String withdrawalNo;

    private BigDecimal moneyComm;

    private BigDecimal moneyBalance;

    private BigDecimal moneyNum;

    private BigDecimal moneyTax;

    private BigDecimal taxRatio;

    private String account;

    private String name;

    private Integer way;

    private Integer status;

    private String remark;

    private Date createTime;

    private Date updateTime;

    private String email;

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

    public String getWithdrawalNo() {
        return withdrawalNo;
    }

    public void setWithdrawalNo(String withdrawalNo) {
        this.withdrawalNo = withdrawalNo == null ? null : withdrawalNo.trim();
    }

    public BigDecimal getMoneyComm() {
        return moneyComm;
    }

    public void setMoneyComm(BigDecimal moneyComm) {
        this.moneyComm = moneyComm;
    }

    public BigDecimal getMoneyBalance() {
        return moneyBalance;
    }

    public void setMoneyBalance(BigDecimal moneyBalance) {
        this.moneyBalance = moneyBalance;
    }

    public BigDecimal getMoneyNum() {
        return moneyNum;
    }

    public void setMoneyNum(BigDecimal moneyNum) {
        this.moneyNum = moneyNum;
    }

    public BigDecimal getMoneyTax() {
        return moneyTax;
    }

    public void setMoneyTax(BigDecimal moneyTax) {
        this.moneyTax = moneyTax;
    }

    public BigDecimal getTaxRatio() {
        return taxRatio;
    }

    public void setTaxRatio(BigDecimal taxRatio) {
        this.taxRatio = taxRatio;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account == null ? null : account.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}