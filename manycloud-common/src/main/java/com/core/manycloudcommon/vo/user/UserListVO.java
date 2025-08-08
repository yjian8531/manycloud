package com.core.manycloudcommon.vo.user;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户列表VO
 */
@Data
public class UserListVO {

    private String userId;
    /** 账号 **/
    private String account;
    /** 昵称 **/
    private String nick;
    /** 用户等级 **/
    private String levelName;
    /** 推广等级 **/
    private String medalName;
    /** 推荐人 **/
    private String proAccount;
    /** 余额 **/
    private BigDecimal balance;
    /** 总消费 **/
    private BigDecimal consumption;
    /** 主机数量 **/
    private Integer num;
    /** 备注 **/
    private String remark;
    /** 创建时间 **/
    private Date createTime;

}
