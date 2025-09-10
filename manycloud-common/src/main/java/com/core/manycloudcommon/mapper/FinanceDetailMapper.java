package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.caller.Item.FinanceStatsItem;
import com.core.manycloudcommon.entity.FinanceDetail;
import com.core.manycloudcommon.vo.finance.FinanceDetailListVO;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface FinanceDetailMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(FinanceDetail record);

    int insertSelective(FinanceDetail record);

    FinanceDetail selectByPrimaryKey(Integer id);

    /** 查询用户消费金额 **/
    BigDecimal selectConsumptionByUser(@Param("userId") String userId);

    /** 查询多用户消费金额 **/
    BigDecimal selectConsumptionByUsers(@Param("list") List<String> list);

    FinanceDetail selectBuyByProduct(@Param("productNo") String productNo);

    List<FinanceDetail> selectProductByUser(@Param("userId") String userId,@Param("productNo") String productNo);

    int updateByPrimaryKeySelective(FinanceDetail record);

    int updateByPrimaryKey(FinanceDetail record);

    List<FinanceDetailListVO> selectList(@Param("userId") String userId, @Param("direction")Integer direction, @Param("tag")String tag,
                                         @Param("startTime")String startTime, @Param("endTime")String endTime,
                                         @Param("productNo")String productNo, @Param("email")String email);


    /**
     * 按时间粒度查询财务统计趋势数据
     * @param timeUnit 时间单位：day/month/year
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果列表
     */
    List<FinanceStatsItem> selectStatsByTimeUnit(@Param("timeUnit") String timeUnit,
                                                 @Param("startTime") Date startTime,
                                                 @Param("endTime") Date endTime);
}