package com.core.manycloudadmin.service;

import com.core.manycloudadmin.so.BaseDelByIdSO;
import com.core.manycloudadmin.so.node.*;
import com.core.manycloudcommon.utils.ResultMessage;
import org.springframework.transaction.annotation.Transactional;

public interface NodeService {

    /**
     * 添加资源平台信息
     * @param addPlatformInfoSO
     * @return
     */
    ResultMessage addPlatformInfo(AddPlatformInfoSO addPlatformInfoSO);

    /**
     * 更新资源平台信息
     * @param updatePlatformInfoSO
     * @return
     */
    ResultMessage updatePlatformInfo(UpdatePlatformInfoSO updatePlatformInfoSO);


    /**
     * 分页查询资源平台列表
     * @param queryPlatformInfoListSO
     * @return
     */
    ResultMessage queryPlatformInfoList(QueryPlatformInfoListSO queryPlatformInfoListSO);

    /**
     * 查询资源平台绑定功能信息
     * @param queryPlatformBdFunctionSO
     * @return
     */
    ResultMessage queryPlatformBdFunction(QueryPlatformBdFunctionSO queryPlatformBdFunctionSO);


    /**
     * 绑定平台功能
     * @return
     */
    ResultMessage bindingPlatformFunction(BindingPlatformFunctionSO bindingPlatformFunctionSO);


    /**
     * 获取资源平台下拉选择数据
     * @return
     */
    ResultMessage getPlatformInfoSelect();


    /**
     * 添加资源平台账号信息
     * @param addPlatformAccountS0
     * @return
     */
    ResultMessage addPlatformAccount(AddPlatformAccountS0 addPlatformAccountS0);


    /**
     * 更新资源平台账号信息
     * @param updatePlatformAccountSO
     * @return
     */
    ResultMessage updatePlatformAccount(UpdatePlatformAccountSO updatePlatformAccountSO);


    /**
     * 分页查询资源平台账号列表
     * @param queryPlatformAccountListSO
     * @return
     */
    ResultMessage queryPlatformAccountList(QueryPlatformAccountListSO queryPlatformAccountListSO);


    /**
     * 设置资源平台默认账号
     * @param setPlatformAccountDefaultSO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    ResultMessage setPlatformAccountDefault(SetPlatformAccountDefaultSO setPlatformAccountDefaultSO);


    /**
     * 添加可用区节点
     * @param addNodeInfoSO
     * @return
     */
    ResultMessage addNodeInfo(AddNodeInfoSO addNodeInfoSO);


    /**
     * 更新可用区节点信息SO
     * @param updateNodeInfoSO
     * @return
     */
    ResultMessage updateNodeInfo(UpdateNodeInfoSO updateNodeInfoSO);


    /**
     * 删除可用区节点信息
     * @param delByIdSO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    ResultMessage delNodeInfo(BaseDelByIdSO delByIdSO);



    /**
     * 分页查询可用区列表
     * @param queryNodeInfoListSO
     * @return
     */
    ResultMessage queryNodeInfoList(QueryNodeInfoListSO queryNodeInfoListSO);



    /**
     * 添加节点规格配置
     * @param addNodeModelSO
     * @return
     */
    ResultMessage addNodeModel(AddNodeModelSO addNodeModelSO);


    /**
     * 更新节点规格配置
     * @param updateNodeModelSO
     * @return
     */
    ResultMessage updateNodeModel(UpdateNodeModelSO updateNodeModelSO);


    /**
     * 更新节点规格配置
     * @param delByIdSO
     * @return
     */
    ResultMessage delNodeModel(BaseDelByIdSO delByIdSO);


    /**
     * 查询节点规格配置列表
     * @param queryNodeModelListSO
     * @return
     */
    ResultMessage queryNodeModelList(QueryNodeModelListSO queryNodeModelListSO);


    /**
     * 查询节点规格配置选项
     * @param queryNodeModelConfigSO
     * @return
     */
    ResultMessage queryNodeModelConfig(QueryNodeModelConfigSO queryNodeModelConfigSO);


    /**
     * 添加节点磁盘配置
     * @param addNodeDiskSO
     * @return
     */
    ResultMessage addNodeDisk(AddNodeDiskSO addNodeDiskSO);


    /**
     * 更新节点磁盘配置
     * @param updateNodeDiskSO
     * @return
     */
    ResultMessage updateNodeDisk(UpdateNodeDiskSO updateNodeDiskSO);


    /**
     * 删除节点磁盘配置
     * @param delByIdSO
     * @return
     */
    ResultMessage delNodeDisk(BaseDelByIdSO delByIdSO);


    /**
     * 查询节点磁盘配置列表
     * @param queryNodeDiskListSO
     * @return
     */
    ResultMessage queryNodeDiskList(QueryNodeDiskListSO queryNodeDiskListSO);

    /**
     * 添加节点网络配置
     * @param addNodeNetworkSO
     * @return
     */
    ResultMessage addNodeNetwork(AddNodeNetworkSO addNodeNetworkSO);


    /**
     * 更新节点网络配置
     * @param updateNodeNetworkSO
     * @return
     */
    ResultMessage updateNodeNetwork(UpdateNodeNetworkSO updateNodeNetworkSO);


    /**
     * 删除节点网络配置
     * @param delByIdSO
     * @return
     */
    ResultMessage delNodeNetwork(BaseDelByIdSO delByIdSO);


    /**
     * 查询节点网络配置列表
     * @param queryNodeNetworkListSO
     * @return
     */
    ResultMessage queryNodeNetworkList(QueryNodeNetworkListSO queryNodeNetworkListSO);


    /**
     * 添加节点镜像信息
     * @param addNodeImageSO
     * @return
     */
    ResultMessage addNodeImage(AddNodeImageSO addNodeImageSO);



    /**
     * 更新节点镜像信息
     * @param updateNodeImageSO
     * @return
     */
    ResultMessage updateNodeImage(UpdateNodeImageSO updateNodeImageSO);


    /**
     * 删除节点镜像信息
     * @param delByIdSO
     * @return
     */
    ResultMessage delNodeImage(BaseDelByIdSO delByIdSO);


    /**
     * 查询节点镜像列表
     * @param queryNodeImageListSO
     * @return
     */
    ResultMessage queryNodeImageList(QueryNodeImageListSO queryNodeImageListSO);


    /**
     * 添加节点价格配置
     * @param addNodePriceSO
     * @return
     */
    ResultMessage addNodePrice(AddNodePriceSO addNodePriceSO);


    /**
     * 更新节点价格配置
     * @param updateNodePriceSO
     * @return
     */
    ResultMessage updateNodePrice(UpdateNodePriceSO updateNodePriceSO);


    /**
     * 删除节点价格配置
     * @param delByIdSO
     * @return
     */
    ResultMessage delNodePrice(BaseDelByIdSO delByIdSO);


    /**
     * 查询节点价格配置列表
     * @param queryNodePriceListSO
     * @return
     */
    ResultMessage queryNodePriceList(QueryNodePriceListSO queryNodePriceListSO);


    /**
     * 查询节点配置选择
     * @param queryConfigSelectSO
     * @return
     */
    ResultMessage queryConfigSelect(QueryConfigSelectSO queryConfigSelectSO);
}
