package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.InstanceInfo;
import com.core.manycloudcommon.vo.finance.UserProductNumVO;
import com.core.manycloudcommon.vo.instance.InstanceUserVO;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface InstanceInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(InstanceInfo record);

    int insertSelective(InstanceInfo record);

    /**
     * 查询订单完成确认
     * @param orderNo 订单编号
     * @return 大于 0 代表未完成
     */
    Integer selectOrderConfirm(String orderNo);

    InstanceInfo selectByPrimaryKey(Integer id);

    InstanceInfo selectById(@Param("instanceId") String instanceId);

    List<InstanceInfo> selectByStatus(@Param("status") Integer status);

    Integer selectNumByUsers(@Param("list") List<String> list);

    List<UserProductNumVO> selectListByUserSNum(@Param("list") List<String> list);

    int updateByPrimaryKeySelective(InstanceInfo record);

    int updateByPrimaryKey(InstanceInfo record);

    /**
     * 查询用户实例列表
     * @param userId 用户ID
     * @param instanceId 实例ID 或 公网IP
     * @param powerState 电源状态：halted(停止)、running(运行中),execution(执行中)
     * @param status 状态(0:待创建,1:创建中,3:使用中,4:待续费,5:已过期)
     * @param groupId 分组ID
     * @param sort 排序(null:创建时间倒序，0:到期时间升序，1:到期时间倒序)
     * @return
     */
    List<InstanceUserVO> selectListByUser(@Param("userId")String userId,@Param("instanceId")String instanceId,@Param("powerState")String powerState,
                                          @Param("status")Integer status,@Param("groupId")Integer groupId,@Param("sort")Integer sort);


    /**
     * 查询实例列表
     * @param account 用户账号
     * @param instanceId 实例ID 或 公网IP
     * @param powerState 电源状态：halted(停止)、running(运行中),execution(执行中)
     * @param status 状态(0:待创建,1:创建中,3:使用中,4:待续费,5:已过期,6:已销毁,7:创建失败)
     * @param sort 排序(null:创建时间倒序，0:到期时间升序，1:到期时间倒序)selectList
     * @return
     */
    List<InstanceUserVO> selectList(@Param("account")String account,@Param("instanceId")String instanceId,@Param("powerState")String powerState,
                                          @Param("status")Integer status,@Param("sort")Integer sort);


    /**
     * 统计平台总实例数
     * @return 总实例数
     */
    Integer countTotalInstances();

    /**
     * 统计已过期实例数
     * @return 已过期实例数
     */
    Integer countExpiredInstances();

    /**
     * 统计使用中实例数
     * @return 使用中实例数
     */
    Integer countInUseInstances();

    /**
     * 统计待续费实例数
     * @return 待续费实例数
     */
    Integer countToBeRenewedInstances();

    /**
     * 按平台统计实例数
     * @param label 平台标签
     * @return 该平台的实例数
     */
    Integer countInstancesByPlatform(@Param("label") String label);

    /**
     * 按平台统计已过期实例数
     * @param label 平台标签
     * @return 该平台的已过期实例数
     */
    Integer countExpiredInstancesByPlatform(@Param("label") String label);

    /**
     * 按平台统计使用中实例数
     * @param label 平台标签
     * @return 该平台的使用中实例数
     */
    Integer countInUseInstancesByPlatform(@Param("label") String label);

    /**
     * 按平台统计待续费实例数
     * @param label 平台标签
     * @return 该平台的待续费实例数
     */
    Integer countToBeRenewedInstancesByPlatform(@Param("label") String label);


    /**
     * 按平台统计配置分布
     * @param platform
     * @return 该平台配置分布
     */
    List<Map<String, Object>> selectConfigDistribution(@Param("platform") String platform);


}