package com.core.manycloudcommon.entity;

import java.util.Date;

public class NodeInfo {
    private Integer id;

    private String label;

    private String nodeVal;

    private String nodeParam;

    private Integer continentId;

    private Integer countryId;

    private Integer provinceId;

    private Integer cityId;

    private String nodeName;

    private Integer sorting;

    private String remark;

    private Integer status;

    private Date createTime;

    private Date updateTime;

    private Integer bdNum;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label == null ? null : label.trim();
    }

    public String getNodeVal() {
        return nodeVal;
    }

    public void setNodeVal(String nodeVal) {
        this.nodeVal = nodeVal == null ? null : nodeVal.trim();
    }

    public String getNodeParam() {
        return nodeParam;
    }

    public void setNodeParam(String nodeParam) {
        this.nodeParam = nodeParam == null ? null : nodeParam.trim();
    }

    public Integer getContinentId() {
        return continentId;
    }

    public void setContinentId(Integer continentId) {
        this.continentId = continentId;
    }

    public Integer getCountryId() {
        return countryId;
    }

    public void setCountryId(Integer countryId) {
        this.countryId = countryId;
    }

    public Integer getProvinceId() {
        return provinceId;
    }

    public void setProvinceId(Integer provinceId) {
        this.provinceId = provinceId;
    }

    public Integer getCityId() {
        return cityId;
    }

    public void setCityId(Integer cityId) {
        this.cityId = cityId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName == null ? null : nodeName.trim();
    }

    public Integer getSorting() {
        return sorting;
    }

    public void setSorting(Integer sorting) {
        this.sorting = sorting;
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

    public Integer getBdNum() {
        return bdNum;
    }

    public void setBdNum(Integer bdNum) {
        this.bdNum = bdNum;
    }
}