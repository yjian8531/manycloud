package com.core.manycloudservice.controller;

import com.core.manycloudcommon.controller.BaseController;
import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudservice.service.InstanceService;
import com.core.manycloudservice.so.instance.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/instance")
public class InstanceController extends BaseController {

    @Autowired
    private InstanceService instanceService;

    /***
     * 查询用户实例列表
     * @param queryListBuUserSO
     * @return
     */
    @PostMapping("/query/list/user")
    public ResultMessage queryListBuUser(@RequestBody QueryListBuUserSO queryListBuUserSO){
        UserInfo userInfo = getLoginUser();
        return instanceService.queryListBuUser(userInfo.getUserId(),queryListBuUserSO);
    }


    /**
     * 查询实例详情信息
     * @param queryDetailSO
     * @return
     */
    @PostMapping("/query/detail")
    public ResultMessage queryDetail(@RequestBody QueryDetailSO queryDetailSO){
        return instanceService.queryDetail(queryDetailSO);
    }


    /****
     * 修改别名
     * @param updateNikeSO
     * @return
     */
    @PostMapping("/update/nike")
    public ResultMessage updateNike(@RequestBody UpdateNikeSO updateNikeSO){
        return instanceService.updateNike(updateNikeSO);
    }


    /**
     * 添加产品分组信息
     * @param addGroupInfoSO
     * @return
     */
    @PostMapping(value = "/add/groupinfo",produces = {"application/json"})
    public ResultMessage addGroupInfo(@RequestBody AddGroupInfoSO addGroupInfoSO){
        UserInfo userInfo = this.getLoginUser();
        return instanceService.addGroupInfo(userInfo.getUserId(),addGroupInfoSO);
    }

    /**
     * 更新产品分组信息
     * @param updateGroupInfoSO
     * @return
     */
    @PostMapping(value = "/update/groupinfo",produces = {"application/json"})
    public ResultMessage updateGroupInfo(@RequestBody UpdateGroupInfoSO updateGroupInfoSO){
        return instanceService.updateGroupInfo(updateGroupInfoSO);
    }


    /**
     * 删除产品分组信息
     * @param deleteGroupInfoSO
     * @return
     */
    @PostMapping(value = "/del/groupinfo",produces = {"application/json"})
    public ResultMessage deleteGroupInfo(@RequestBody DeleteGroupInfoSO deleteGroupInfoSO){
        return instanceService.deleteGroupInfo(deleteGroupInfoSO.getId());
    }


    /**
     * 查询当前用户所有的产品分组信息
     * @return
     */
    @GetMapping(value = "/query/groupinfo/user",produces = {"application/json"})
    public ResultMessage queryGroupByUserAll(){
        UserInfo userInfo = this.getLoginUser();
        return instanceService.queryGroupByUserAll(userInfo.getUserId());
    }


    /**
     * 分页查询用户产品分组信息
     * @return
     */
    @PostMapping(value = "/query/groupinfo/user/list",produces = {"application/json"})
    public ResultMessage queryGroupInfoByUserList(@RequestBody QueryGroupByUserListSO queryGroupByUserListSO){
        UserInfo userInfo = this.getLoginUser();
        return instanceService.queryGroupByUserList(userInfo.getUserId(),queryGroupByUserListSO);
    }

    /**
     * 添加产品分组关联
     * @param addGroupProductS0
     * @return
     */
    @PostMapping(value = "/add/groupproduct",produces = {"application/json"})
    public ResultMessage addGroupProduct(@RequestBody AddGroupProductS0 addGroupProductS0){
        return instanceService.addGroupProduct(addGroupProductS0);
    }


    /**
     * 移除产品分组关联
     * @param delGroupProductS0
     * @return
     */
    @PostMapping(value = "/del/groupproduct",produces = {"application/json"})
    public ResultMessage delGroupProduct(@RequestBody DelGroupProductS0 delGroupProductS0){
        UserInfo userInfo = this.getLoginUser();
        return instanceService.delGroupProduct(userInfo.getUserId(),delGroupProductS0);
    }


    /**
     * 主机电源操作
     * @param
     * @return
     */
    @PostMapping(value = "/exec/power",produces = {"application/json"})
    public ResultMessage execPower(@RequestBody ExecPowerSO execPowerSO){
        return instanceService.execPower(execPowerSO);
    }

    /**
     * 更新实例密码
     * @param updatePwdSO
     * @return
     */
    @PostMapping(value = "/update/pwd",produces = {"application/json"})
    public ResultMessage updatePwd(@RequestBody UpdatePwdSO updatePwdSO){
        return instanceService.updatePwd(updatePwdSO);
    }


    /**
     * 重装查询镜像
     * @param resetSO
     * @return
     */
    @PostMapping(value = "/query/reset",produces = {"application/json"})
    public ResultMessage queryReset(@RequestBody ResetSO resetSO){
        return instanceService.queryReset(resetSO);
    }


    /**
     * 重装系统
     * @return
     */
    @PostMapping(value = "/reset",produces = {"application/json"})
    public ResultMessage reset(@RequestBody ResetSO resetSO){
        return instanceService.reset(resetSO);
    }

}
