package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.SaleDetail;
import com.core.manycloudcommon.vo.finance.SaleDetailConunt;
import com.core.manycloudcommon.vo.finance.SaleDetailListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SaleDetailMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(SaleDetail record);

    int insertSelective(SaleDetail record);

    SaleDetail selectByPrimaryKey(Integer id);

    List<SaleDetailListVO> selectList(@Param("userId") String userId, @Param("monthStr")String monthStr);

    SaleDetailConunt selectListCount(@Param("userId") String userId, @Param("monthStr")String monthStr);

    int updateByPrimaryKeySelective(SaleDetail record);

    int updateByPrimaryKey(SaleDetail record);
}