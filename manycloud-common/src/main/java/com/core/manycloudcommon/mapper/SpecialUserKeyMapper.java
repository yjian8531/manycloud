package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.SpecialUserKey;
import org.apache.ibatis.annotations.Param;

public interface SpecialUserKeyMapper {

    /** 按私钥查启用中的记录(status=0) */
    SpecialUserKey selectByPrivateKey(@Param("privateKey") String privateKey);

    /** 按userId查(校验一对一/是否已配私钥) */
    SpecialUserKey selectByUserId(@Param("userId") String userId);

    int insertSelective(SpecialUserKey record);

    int updateByPrimaryKeySelective(SpecialUserKey record);
}
