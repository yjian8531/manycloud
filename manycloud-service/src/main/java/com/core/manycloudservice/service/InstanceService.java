package com.core.manycloudservice.service;

import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudservice.so.instance.*;
import org.springframework.transaction.annotation.Transactional;

public interface InstanceService {

    /***
     * 查询用户实例列表
     * @param queryListBuUserSO
     * @return
     */
    ResultMessage queryListBuUser(String userId, QueryListBuUserSO queryListBuUserSO);


    /**
     * 查询实例详情信息
     * @param queryDetailSO
     * @return
     */
    ResultMessage queryDetail(QueryDetailSO queryDetailSO);


    /****
     * 修改别名
     * @param updateNikeSO
     * @return
     */
    ResultMessage updateNike(UpdateNikeSO updateNikeSO);


    /**
     * 添加产品分组信息
     * @param addGroupInfoSO
     * @return
     */
    ResultMessage addGroupInfo(String userId, AddGroupInfoSO addGroupInfoSO);


    /**
     * 更新产品分组信息
     * @param updateGroupInfoSO
     * @return
     */
    ResultMessage updateGroupInfo(UpdateGroupInfoSO updateGroupInfoSO);


    /**
     * 删除产品分组信息
     * @param id 产品分组ID
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    ResultMessage deleteGroupInfo(Integer id);


    /**
     * 查询当前用户所有的产品分组信息
     * @param userId
     * @return
     */
    ResultMessage queryGroupByUserAll(String userId);



    /**
     * 分页查询用户实例分组信息
     * @param userId
     * @return
     */
    ResultMessage queryGroupByUserList(String userId, QueryGroupByUserListSO queryGroupByUserListSO);


    /**
     * 添加实例分组关联
     * @param addGroupProductS0
     * @return
     */
   ResultMessage addGroupProduct(AddGroupProductS0 addGroupProductS0);


    /**
     * 移除实例分组关联
     * @param delGroupProductS0
     * @return
     */
    ResultMessage delGroupProduct(String userId, DelGroupProductS0 delGroupProductS0);


    /**
     * 主机电源操作
     * @param
     * @return
     */
    ResultMessage execPower(ExecPowerSO execPowerSO);


    /**
     * 更新实例密码
     * @param updatePwdSO
     * @return
     */
    ResultMessage updatePwd(UpdatePwdSO updatePwdSO);

    /**
     * 重装查询镜像
     * @param resetSO
     * @return
     */
    ResultMessage queryReset(ResetSO resetSO);


    /**
     * 重装系统
     * @return
     */
    ResultMessage reset(ResetSO resetSO);

}
