package com.core.manycloudcommon.entity;

import java.util.Date;

/**
 * 特殊用户私钥（/api/ 开放接口鉴权用）
 * 一个特殊用户(user.type=4)对应一把启用私钥
 */
public class SpecialUserKey {

    private Integer id;

    /** 特殊用户userId */
    private String userId;

    /** 请求头 X-Private-Key 带的私钥 */
    private String privateKey;

    /** 私钥编号/名称(便于后台管理) */
    private String keyNo;

    /** 0启用 1禁用 */
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

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey == null ? null : privateKey.trim();
    }

    public String getKeyNo() {
        return keyNo;
    }

    public void setKeyNo(String keyNo) {
        this.keyNo = keyNo == null ? null : keyNo.trim();
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
