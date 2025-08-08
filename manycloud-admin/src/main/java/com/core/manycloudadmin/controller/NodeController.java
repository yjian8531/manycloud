package com.core.manycloudadmin.controller;

import com.core.manycloudadmin.service.NodeService;
import com.core.manycloudadmin.so.BaseDelByIdSO;
import com.core.manycloudadmin.so.node.*;
import com.core.manycloudcommon.controller.BaseController;
import com.core.manycloudcommon.utils.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/node")
public class NodeController extends BaseController {

    @Autowired
    private NodeService nodeService;

    /**
     * 添加资源平台信息
     * @param addPlatformInfoSO
     * @return
     */
    @PostMapping(value = "/add/platforminfo",produces = {"application/json"})
    public ResultMessage addPlatformInfo(@RequestBody AddPlatformInfoSO addPlatformInfoSO){
        return nodeService.addPlatformInfo(addPlatformInfoSO);
    }

    /**
     * 更新资源平台信息
     * @param updatePlatformInfoSO
     * @return
     */
    @PostMapping(value = "/update/platforminfo",produces = {"application/json"})
    public ResultMessage updatePlatformInfo(@RequestBody UpdatePlatformInfoSO updatePlatformInfoSO){
        return nodeService.updatePlatformInfo(updatePlatformInfoSO);
    }


    /**
     * 分页查询资源平台列表
     * @param queryPlatformInfoListSO
     * @return
     */
    @PostMapping(value = "/query/platforminfo/list",produces = {"application/json"})
    public ResultMessage queryPlatformInfoList(@RequestBody QueryPlatformInfoListSO queryPlatformInfoListSO){
        return nodeService.queryPlatformInfoList(queryPlatformInfoListSO);
    }


    /**
     * 查询资源平台绑定功能信息
     * @param queryPlatformBdFunctionSO
     * @return
     */
    @PostMapping(value = "/query/platform/function",produces = {"application/json"})
    public ResultMessage queryPlatformBdFunction(@RequestBody QueryPlatformBdFunctionSO queryPlatformBdFunctionSO){
        return nodeService.queryPlatformBdFunction(queryPlatformBdFunctionSO);
    }


    /**
     * 绑定平台功能
     * @return
     */
    @PostMapping(value = "/binding/platform/function",produces = {"application/json"})
    public ResultMessage bindingPlatformFunction(@RequestBody BindingPlatformFunctionSO bindingPlatformFunctionSO){
        return nodeService.bindingPlatformFunction(bindingPlatformFunctionSO);
    }


    /**
     * 获取资源平台下拉选择数据
     * @return
     */
    @GetMapping(value = "/get/platforminfo/select",produces = {"application/json"})
    public ResultMessage getPlatformInfoSelect(){
        return nodeService.getPlatformInfoSelect();
    }


    /**
     * 添加资源平台账号信息
     * @param addPlatformAccountS0
     * @return
     */
    @PostMapping(value = "/add/platformaccount",produces = {"application/json"})
    public ResultMessage addPlatformAccount(@RequestBody AddPlatformAccountS0 addPlatformAccountS0){
        return nodeService.addPlatformAccount(addPlatformAccountS0);
    }


    /**
     * 更新资源平台账号信息
     * @param updatePlatformAccountSO
     * @return
     */
    @PostMapping(value = "/update/platformaccount",produces = {"application/json"})
    public ResultMessage updatePlatformAccount(@RequestBody UpdatePlatformAccountSO updatePlatformAccountSO){
        return nodeService.updatePlatformAccount(updatePlatformAccountSO);
    }


    /**
     * 分页查询资源平台账号列表
     * @param queryPlatformAccountListSO
     * @return
     */
    @PostMapping(value = "/query/platformaccount/list",produces = {"application/json"})
    public ResultMessage queryPlatformAccountList(@RequestBody QueryPlatformAccountListSO queryPlatformAccountListSO){
        return nodeService.queryPlatformAccountList(queryPlatformAccountListSO);
    }


    /**
     * 设置资源平台默认账号
     * @param setPlatformAccountDefaultSO
     * @return
     */
    @PostMapping(value = "/set/platformaccount/default",produces = {"application/json"})
    public ResultMessage setPlatformAccountDefault(@RequestBody SetPlatformAccountDefaultSO setPlatformAccountDefaultSO){
        return nodeService.setPlatformAccountDefault(setPlatformAccountDefaultSO);
    }

    /**
     * 添加可用区节点
     * @param addNodeInfoSO
     * @return
     */
    @PostMapping(value = "/add/nodeinfo",produces = {"application/json"})
    public ResultMessage addNodeInfo(@RequestBody AddNodeInfoSO addNodeInfoSO){
        return nodeService.addNodeInfo(addNodeInfoSO);
    }


    /**
     * 更新可用区节点信息SO
     * @param updateNodeInfoSO
     * @return
     */
    @PostMapping(value = "/update/nodeinfo",produces = {"application/json"})
    public ResultMessage updateNodeInfo(@RequestBody UpdateNodeInfoSO updateNodeInfoSO){
        return nodeService.updateNodeInfo(updateNodeInfoSO);
    }


    /**
     * 删除可用区节点信息
     * @param delByIdSO
     * @return
     */
    @PostMapping(value = "/del/nodeinfo",produces = {"application/json"})
    public ResultMessage delNodeInfo(@RequestBody BaseDelByIdSO delByIdSO){
        return nodeService.delNodeInfo(delByIdSO);
    }



    /**
     * 分页查询可用区列表
     * @param queryNodeInfoListSO
     * @return
     */
    @PostMapping(value = "/query/nodeinfo/list",produces = {"application/json"})
    public ResultMessage queryNodeInfoList(@RequestBody QueryNodeInfoListSO queryNodeInfoListSO){
        return nodeService.queryNodeInfoList(queryNodeInfoListSO);
    }


    /**
     * 添加节点规格配置
     * @param addNodeModelSO
     * @return
     */
    @PostMapping(value = "/add/nodemodel",produces = {"application/json"})
    public ResultMessage addNodeModel(@RequestBody AddNodeModelSO addNodeModelSO){
        return nodeService.addNodeModel(addNodeModelSO);
    }


    /**
     * 更新节点规格配置
     * @param updateNodeModelSO
     * @return
     */
    @PostMapping(value = "/update/nodemodel",produces = {"application/json"})
    public ResultMessage updateNodeModel(@RequestBody UpdateNodeModelSO updateNodeModelSO){
        return nodeService.updateNodeModel(updateNodeModelSO);
    }


    /**
     * 更新节点规格配置
     * @param delByIdSO
     * @return
     */
    @PostMapping(value = "/del/nodemodel",produces = {"application/json"})
    public ResultMessage delNodeModel(@RequestBody BaseDelByIdSO delByIdSO){
        return nodeService.delNodeModel(delByIdSO);
    }


    /**
     * 查询节点规格配置列表
     * @param queryNodeModelListSO
     * @return
     */
    @PostMapping(value = "/query/nodemodel/list",produces = {"application/json"})
    public ResultMessage queryNodeModelList(@RequestBody QueryNodeModelListSO queryNodeModelListSO){
        return nodeService.queryNodeModelList(queryNodeModelListSO);
    }


    /**
     * 查询节点规格配置选项
     * @param queryNodeModelConfigSO
     * @return
     */
    @PostMapping(value = "/query/nodemodel/config",produces = {"application/json"})
    public ResultMessage queryNodeModelConfig(@RequestBody QueryNodeModelConfigSO queryNodeModelConfigSO){
        return nodeService.queryNodeModelConfig(queryNodeModelConfigSO);
    }


    /**
     * 添加节点磁盘配置
     * @param addNodeDiskSO
     * @return
     */
    @PostMapping(value = "/add/nodedisk",produces = {"application/json"})
    public ResultMessage addNodeDisk(@RequestBody AddNodeDiskSO addNodeDiskSO){
        return nodeService.addNodeDisk(addNodeDiskSO);
    }


    /**
     * 更新节点磁盘配置
     * @param updateNodeDiskSO
     * @return
     */
    @PostMapping(value = "/update/nodedisk",produces = {"application/json"})
    public ResultMessage updateNodeDisk(@RequestBody UpdateNodeDiskSO updateNodeDiskSO){
        return nodeService.updateNodeDisk(updateNodeDiskSO);
    }


    /**
     * 删除节点磁盘配置
     * @param delByIdSO
     * @return
     */
    @PostMapping(value = "/del/nodedisk",produces = {"application/json"})
    public ResultMessage delNodeDisk(@RequestBody BaseDelByIdSO delByIdSO){
        return nodeService.delNodeDisk(delByIdSO);
    }


    /**
     * 查询节点磁盘配置列表
     * @param queryNodeDiskListSO
     * @return
     */
    @PostMapping(value = "/query/nodedisk/list",produces = {"application/json"})
    public ResultMessage queryNodeDiskList(@RequestBody QueryNodeDiskListSO queryNodeDiskListSO){
        return nodeService.queryNodeDiskList(queryNodeDiskListSO);
    }

    /**
     * 添加节点网络配置
     * @param addNodeNetworkSO
     * @return
     */
    @PostMapping(value = "/add/nodenetwork",produces = {"application/json"})
    public ResultMessage addNodeNetwork(@RequestBody AddNodeNetworkSO addNodeNetworkSO){
        return nodeService.addNodeNetwork(addNodeNetworkSO);
    }


    /**
     * 更新节点网络配置
     * @param updateNodeNetworkSO
     * @return
     */
    @PostMapping(value = "/update/nodenetwork",produces = {"application/json"})
    public ResultMessage updateNodeNetwork(@RequestBody UpdateNodeNetworkSO updateNodeNetworkSO){
        return nodeService.updateNodeNetwork(updateNodeNetworkSO);
    }


    /**
     * 删除节点网络配置
     * @param delByIdSO
     * @return
     */
    @PostMapping(value = "/del/nodenetwork",produces = {"application/json"})
    public ResultMessage delNodeNetwork(@RequestBody BaseDelByIdSO delByIdSO){
        return nodeService.delNodeNetwork(delByIdSO);
    }


    /**
     * 查询节点网络配置列表
     * @param queryNodeNetworkListSO
     * @return
     */
    @PostMapping(value = "/query/nodenetwork/list",produces = {"application/json"})
    public ResultMessage queryNodeNetworkList(@RequestBody QueryNodeNetworkListSO queryNodeNetworkListSO){
        return nodeService.queryNodeNetworkList(queryNodeNetworkListSO);
    }


    /**
     * 添加节点镜像信息
     * @param addNodeImageSO
     * @return
     */
    @PostMapping(value = "/add/nodeimage",produces = {"application/json"})
    public ResultMessage addNodeImage(@RequestBody AddNodeImageSO addNodeImageSO){
        return nodeService.addNodeImage(addNodeImageSO);
    }



    /**
     * 更新节点镜像信息
     * @param updateNodeImageSO
     * @return
     */
    @PostMapping(value = "/update/nodeimage",produces = {"application/json"})
    public ResultMessage updateNodeImage(@RequestBody UpdateNodeImageSO updateNodeImageSO){
        return nodeService.updateNodeImage(updateNodeImageSO);
    }


    /**
     * 删除节点镜像信息
     * @param delByIdSO
     * @return
     */
    @PostMapping(value = "/del/nodeimage",produces = {"application/json"})
    public ResultMessage delNodeImage(@RequestBody BaseDelByIdSO delByIdSO){
        return nodeService.delNodeImage(delByIdSO);
    }


    /**
     * 查询节点镜像列表
     * @param queryNodeImageListSO
     * @return
     */
    @PostMapping(value = "/query/nodeimage/list",produces = {"application/json"})
    public ResultMessage queryNodeImageList(@RequestBody QueryNodeImageListSO queryNodeImageListSO){
        return nodeService.queryNodeImageList(queryNodeImageListSO);
    }


    /**
     * 添加节点价格配置
     * @param addNodePriceSO
     * @return
     */
    @PostMapping(value = "/add/nodeprice",produces = {"application/json"})
    public ResultMessage addNodePrice(@RequestBody AddNodePriceSO addNodePriceSO){
        return nodeService.addNodePrice(addNodePriceSO);
    }


    /**
     * 更新节点价格配置
     * @param updateNodePriceSO
     * @return
     */
    @PostMapping(value = "/update/nodeprice",produces = {"application/json"})
    public ResultMessage updateNodePrice(@RequestBody UpdateNodePriceSO updateNodePriceSO){
        return nodeService.updateNodePrice(updateNodePriceSO);
    }


    /**
     * 删除节点价格配置
     * @param delByIdSO
     * @return
     */
    @PostMapping(value = "/del/nodeprice",produces = {"application/json"})
    public ResultMessage delNodePrice(@RequestBody BaseDelByIdSO delByIdSO){
        return nodeService.delNodePrice(delByIdSO);
    }


    /**
     * 查询节点价格配置列表
     * @param queryNodePriceListSO
     * @return
     */
    @PostMapping(value = "/query/nodeprice/list",produces = {"application/json"})
    public ResultMessage queryNodePriceList(@RequestBody QueryNodePriceListSO queryNodePriceListSO){
        return nodeService.queryNodePriceList(queryNodePriceListSO);
    }



    /**
     * 查询节点配置选择
     * @param queryConfigSelectSO
     * @return
     */
    @PostMapping(value = "/query/config/select",produces = {"application/json"})
    public ResultMessage queryConfigSelect(@RequestBody QueryConfigSelectSO queryConfigSelectSO){
        return nodeService.queryConfigSelect(queryConfigSelectSO);
    }

}
