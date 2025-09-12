
package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.caller.vo.StatisticsExpenditureVO;
import com.core.manycloudcommon.entity.OfflineFinance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 线下财务数据访问接口
 */
@Mapper
public interface OfflineFinanceTOMapper {

    /**
     * 根据主键删除线下财务记录
     * @param id 主键ID
     * @return 影响记录数
     */
    int deleteByPrimaryKey(Integer id);

    /**
     * 插入完整的线下财务记录
     * @param record 线下财务实体
     * @return 影响记录数
     */
    int insert(OfflineFinance record);

    /**
     * 选择性插入线下财务记录（仅插入非空字段）
     * @param record 线下财务实体
     * @return 影响记录数
     */
    int insertSelective(OfflineFinance record);

    /**
     * 根据主键查询线下财务记录
     * @param id 主键ID
     * @return 线下财务实体
     */
    OfflineFinance selectByPrimaryKey(Integer id);

    /**
     * 选择性更新线下财务记录（仅更新非空字段）
     * @param record 线下财务实体
     * @return 影响记录数
     */
    int updateByPrimaryKeySelective(OfflineFinance record);

    /**
     * 完全替换更新线下财务记录
     * @param record 线下财务实体
     * @return 影响记录数
     */
    int updateByPrimaryKey(OfflineFinance record);

    /**
     * 分页/条件查询线下财务记录列表
     * @param map 查询条件（包含pageNum/pageSize等分页参数）
     * @return 线下财务实体集合
     */
    List<OfflineFinance> selectList(Map<String,Object> map);

    /**
     * 统计指定时间区间内的总收入
     * @param startDay 起始日期（格式：yyyy-MM-dd）
     * @param endDay 结束日期（格式：yyyy-MM-dd）
     * @return 收入金额
     */
    BigDecimal selectStatisticsByIncome(@Param("startDay") String startDay, @Param("endDay") String endDay);

    /**
     * 统计指定时间区间内的支出详情
     * @param startDay 起始日期（格式：yyyy-MM-dd）
     * @param endDay 结束日期（格式：yyyy-MM-dd）
     * @return 包含详细支出信息的VO对象
     */
    StatisticsExpenditureVO selectStatisticsByExpenditure(@Param("startDay") String startDay, @Param("endDay") String endDay);

    /**
     * 查询指定账户列表在特定月份的退款总额
     * @param list 账户ID集合
     * @param monthStr 月份（格式：yyyy-MM）
     * @return 退款金额
     */
    BigDecimal selectRefundByAccount(@Param("list") List<String> list, @Param("monthStr") String monthStr);
}