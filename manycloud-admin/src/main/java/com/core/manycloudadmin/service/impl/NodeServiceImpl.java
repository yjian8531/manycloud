package com.core.manycloudadmin.service.impl;


import com.core.manycloudadmin.service.NodeService;
import com.core.manycloudadmin.so.BaseDelByIdSO;
import com.core.manycloudadmin.so.node.*;
import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.enums.RegionLevelEnum;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.vo.admin.PlatformBdFunctionVO;
import com.core.manycloudcommon.vo.node.NodeModelConfigVO;
import com.core.manycloudcommon.vo.node.PlatformInfoSelectVO;
import com.core.manycloudcommon.vo.node.QueryNodeListVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class NodeServiceImpl implements NodeService {

    @Autowired
    private RegionContinentMapper regionContinentMapper;

    @Autowired
    private RegionCountryMapper regionCountryMapper;

    @Autowired
    private RegionProvinceMapper regionProvinceMapper;

    @Autowired
    private RegionCityMapper regionCityMapper;

    @Autowired
    private PlatformInfoMapper platformInfoMapper;

    @Autowired
    private PlatformAccountMapper platformAccountMapper;

    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    @Autowired
    private NodeModelMapper nodeModelMapper;

    @Autowired
    private NodeDiskMapper nodeDiskMapper;

    @Autowired
    private NodeNetworkMapper nodeNetworkMapper;

    @Autowired
    private NodeImageMapper nodeImageMapper;

    @Autowired
    private NodePriceMapper nodePriceMapper;

    @Autowired
    private FunctionPlatformMapper functionPlatformMapper;

    @Autowired
    private FunctionInfoMapper functionInfoMapper;


    /**
     * 添加资源平台信息
     * @param addPlatformInfoSO
     * @return
     */
    public ResultMessage addPlatformInfo(AddPlatformInfoSO addPlatformInfoSO){
        PlatformInfo platformInfo = new PlatformInfo();
        CommonUtil.copyProperties(addPlatformInfoSO,platformInfo);
        int i = platformInfoMapper.insertSelective(platformInfo);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 更新资源平台信息
     * @param updatePlatformInfoSO
     * @return
     */
    public ResultMessage updatePlatformInfo(UpdatePlatformInfoSO updatePlatformInfoSO){
        PlatformInfo platformInfo = new PlatformInfo();
        CommonUtil.copyProperties(updatePlatformInfoSO,platformInfo);
        int i = platformInfoMapper.updateByPrimaryKeySelective(platformInfo);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 分页查询资源平台列表
     * @param queryPlatformInfoListSO
     * @return
     */
    public ResultMessage queryPlatformInfoList(QueryPlatformInfoListSO queryPlatformInfoListSO){

        PageHelper.startPage(queryPlatformInfoListSO.getPage(), queryPlatformInfoListSO.getPageSize());
        Page<PlatformInfo> page = (Page<PlatformInfo>)platformInfoMapper.selectList(queryPlatformInfoListSO.getLabel(),queryPlatformInfoListSO.getName());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);

    }


    /**
     * 查询资源平台绑定功能信息
     * @param queryPlatformBdFunctionSO
     * @return
     */
    public ResultMessage queryPlatformBdFunction(QueryPlatformBdFunctionSO queryPlatformBdFunctionSO){
        List<FunctionPlatform> list = functionPlatformMapper.selectByPlatform(queryPlatformBdFunctionSO.getId());
        Map<Integer,Object> map = list.stream() .collect(Collectors.toMap(fp -> fp.getFunctionId() , fp -> fp.getPlatformId()));
        List<FunctionInfo> functionInfoList = functionInfoMapper.selectAll();

        List<PlatformBdFunctionVO> result = new ArrayList<>();
        for(FunctionInfo functionInfo : functionInfoList){
            String tad = map.get(functionInfo.getId()) != null ? "Y":"N";

            PlatformBdFunctionVO pfVo = PlatformBdFunctionVO.builder()
                    .platformId(queryPlatformBdFunctionSO.getId())
                    .functionId(functionInfo.getId())
                    .functionName(functionInfo.getName())
                    .tad(tad)
                    .build();
            result.add(pfVo);
        }
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }


    /**
     * 绑定平台功能
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage bindingPlatformFunction(BindingPlatformFunctionSO bindingPlatformFunctionSO){

        /** 获取当前绑定信息 **/
        List<FunctionPlatform> list = functionPlatformMapper.selectByPlatform(bindingPlatformFunctionSO.getId());
        Map<Integer,PlatformBdFunctionVO> map = new HashMap<>();
        for(FunctionPlatform functionPlatform : list){
            map.put(functionPlatform.getFunctionId(),PlatformBdFunctionVO.builder()
                    .id(functionPlatform.getId())
                    .platformId(functionPlatform.getPlatformId())
                    .functionId(functionPlatform.getFunctionId())
                    .tad("Y")
                    .build());
        }


        /** 需要删除的绑定集合 **/
        List<Integer> delList = new ArrayList<>();

        /** 需要添加的绑定集合 **/
        List<FunctionPlatform> addList = new ArrayList<>();


        for(Integer functionId : bindingPlatformFunctionSO.getFunctionIds()){
            PlatformBdFunctionVO platformBdFunctionVO = map.get(functionId);
            if(platformBdFunctionVO == null){
                FunctionPlatform functionPlatform = new FunctionPlatform();
                functionPlatform.setPlatformId(bindingPlatformFunctionSO.getId());
                functionPlatform.setFunctionId(functionId);
                functionPlatform.setCreateTime(new Date());
                addList.add(functionPlatform);
            }else{
                platformBdFunctionVO.setTad("O");
                map.put(functionId,platformBdFunctionVO);
            }
        }
        if(addList.size() > 0){
            functionPlatformMapper.insertList(addList);
        }


        /** 批量删除绑定 **/
        for(Integer key : map.keySet()){
            PlatformBdFunctionVO platformBdFunctionVO = map.get(key);
            if(!"O".equals(platformBdFunctionVO.getTad())){
                delList.add(platformBdFunctionVO.getId());
            }
        }
        if(delList.size() > 0){
            functionPlatformMapper.deleteByIds(delList);
        }


        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
    }



    /**
     * 获取资源平台下拉选择数据
     * @return
     */
    public ResultMessage getPlatformInfoSelect(){
        List<PlatformInfoSelectVO> result = new ArrayList<>();
        List<PlatformInfo> list = platformInfoMapper.selectAll();
        for(PlatformInfo platformInfo : list){
            PlatformInfoSelectVO vo = PlatformInfoSelectVO.builder()
                    .label(platformInfo.getLabel())
                    .name(platformInfo.getName())
                    .build();
            result.add(vo);
        }
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }


    /**
     * 添加资源平台账号信息
     * @param addPlatformAccountS0
     * @return
     */
    public ResultMessage addPlatformAccount(AddPlatformAccountS0 addPlatformAccountS0){
        PlatformAccount platformAccount = new PlatformAccount();
        CommonUtil.copyProperties(addPlatformAccountS0,platformAccount);
        int i = platformAccountMapper.insertSelective(platformAccount);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 更新资源平台账号信息
     * @param updatePlatformAccountSO
     * @return
     */
    public ResultMessage updatePlatformAccount(UpdatePlatformAccountSO updatePlatformAccountSO){
        PlatformAccount platformAccount = new PlatformAccount();
        CommonUtil.copyProperties(updatePlatformAccountSO,platformAccount);
        int i = platformAccountMapper.updateByPrimaryKeySelective(platformAccount);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }


    /**
     * 分页查询资源平台账号列表
     * @param queryPlatformAccountListSO
     * @return
     */
    public ResultMessage queryPlatformAccountList(QueryPlatformAccountListSO queryPlatformAccountListSO){
        PageHelper.startPage(queryPlatformAccountListSO.getPage(), queryPlatformAccountListSO.getPageSize());
        Page<PlatformAccount> page = (Page<PlatformAccount>)platformAccountMapper.selectList(queryPlatformAccountListSO.getLabel(),queryPlatformAccountListSO.getAccount());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 设置资源平台默认账号
     * @param setPlatformAccountDefaultSO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage setPlatformAccountDefault(SetPlatformAccountDefaultSO setPlatformAccountDefaultSO){

        PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(setPlatformAccountDefaultSO.getId());
        platformAccountMapper.updateDefaultByPlatform(platformAccount.getLabel());

        PlatformAccount entity = new PlatformAccount();
        entity.setId(platformAccount.getId());
        entity.setDel(1);//默认标记
        entity.setUpdateTime(new Date());

        int i = platformAccountMapper.updateByPrimaryKeySelective(entity);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }


    /**
     * 添加可用区节点
     * @param addNodeInfoSO
     * @return
     */
    public ResultMessage addNodeInfo(AddNodeInfoSO addNodeInfoSO){
        NodeInfo nodeInfo = new NodeInfo();
        CommonUtil.copyProperties(addNodeInfoSO,nodeInfo);

        RegionCity city = regionCityMapper.selectByPrimaryKey(addNodeInfoSO.getCityId());
        if(RegionLevelEnum.PROVINCE.getLevel() == city.getSuperiorLevel()){
            RegionProvince province = regionProvinceMapper.selectByPrimaryKey(city.getSuperiorId());
            nodeInfo.setProvinceId(province.getId());
            if(RegionLevelEnum.COUNTRY.getLevel() == province.getSuperiorLevel()){
                RegionCountry country = regionCountryMapper.selectByPrimaryKey(province.getSuperiorId());
                nodeInfo.setCountryId(country.getId());
                RegionContinent continent = regionContinentMapper.selectByPrimaryKey(country.getSuperiorId());
                nodeInfo.setContinentId(continent.getId());

            }else if(RegionLevelEnum.CONTINENT.getLevel() == province.getSuperiorLevel()){
                RegionContinent continent = regionContinentMapper.selectByPrimaryKey(province.getSuperiorId());
                nodeInfo.setContinentId(continent.getId());
            }

        }else if(RegionLevelEnum.COUNTRY.getLevel() == city.getSuperiorLevel()){
            RegionCountry country = regionCountryMapper.selectByPrimaryKey(city.getSuperiorId());
            nodeInfo.setCountryId(country.getId());
            RegionContinent continent = regionContinentMapper.selectByPrimaryKey(country.getSuperiorId());
            nodeInfo.setContinentId(continent.getId());
        }else if(RegionLevelEnum.CONTINENT.getLevel() == city.getSuperiorLevel()){
            RegionContinent continent = regionContinentMapper.selectByPrimaryKey(city.getSuperiorId());
            nodeInfo.setContinentId(continent.getId());
        }

        nodeInfo.setStatus(0);
        nodeInfo.setCreateTime(new Date());
        nodeInfo.setUpdateTime(new Date());

        int i = nodeInfoMapper.insertSelective(nodeInfo);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 更新可用区节点信息SO
     * @param updateNodeInfoSO
     * @return
     */
    public ResultMessage updateNodeInfo(UpdateNodeInfoSO updateNodeInfoSO){
        NodeInfo nodeInfo = new NodeInfo();
        CommonUtil.copyProperties(updateNodeInfoSO,nodeInfo);
        if(updateNodeInfoSO.getCityId() != null){
            RegionCity city = regionCityMapper.selectByPrimaryKey(updateNodeInfoSO.getCityId());
            if(RegionLevelEnum.PROVINCE.getLevel() == city.getSuperiorLevel()){
                RegionProvince province = regionProvinceMapper.selectByPrimaryKey(city.getSuperiorId());
                nodeInfo.setProvinceId(province.getId());
                if(RegionLevelEnum.COUNTRY.getLevel() == province.getSuperiorLevel()){
                    RegionCountry country = regionCountryMapper.selectByPrimaryKey(province.getSuperiorId());
                    nodeInfo.setCountryId(country.getId());
                    RegionContinent continent = regionContinentMapper.selectByPrimaryKey(country.getSuperiorId());
                    nodeInfo.setContinentId(continent.getId());

                }else if(RegionLevelEnum.CONTINENT.getLevel() == province.getSuperiorLevel()){
                    RegionContinent continent = regionContinentMapper.selectByPrimaryKey(province.getSuperiorId());
                    nodeInfo.setContinentId(continent.getId());
                }

            }else if(RegionLevelEnum.COUNTRY.getLevel() == city.getSuperiorLevel()){
                RegionCountry country = regionCountryMapper.selectByPrimaryKey(city.getSuperiorId());
                nodeInfo.setCountryId(country.getId());
                RegionContinent continent = regionContinentMapper.selectByPrimaryKey(country.getSuperiorId());
                nodeInfo.setContinentId(continent.getId());
            }else if(RegionLevelEnum.CONTINENT.getLevel() == city.getSuperiorLevel()){
                RegionContinent continent = regionContinentMapper.selectByPrimaryKey(city.getSuperiorId());
                nodeInfo.setContinentId(continent.getId());
            }
        }

        nodeInfo.setUpdateTime(new Date());

        int i = nodeInfoMapper.updateByPrimaryKeySelective(nodeInfo);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }

    }

    /**
     * 删除可用区节点信息
     * @param delByIdSO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage delNodeInfo(BaseDelByIdSO delByIdSO){
        int i = nodeInfoMapper.deleteByPrimaryKey(delByIdSO.getId());
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }


    /**
     * 分页查询可用区列表
     * @param queryNodeInfoListSO
     * @return
     */
    public ResultMessage queryNodeInfoList(QueryNodeInfoListSO queryNodeInfoListSO){
        PageHelper.startPage(queryNodeInfoListSO.getPage(), queryNodeInfoListSO.getPageSize());
        Page<QueryNodeListVO> page = (Page<QueryNodeListVO>)nodeInfoMapper.selectList(queryNodeInfoListSO.getContinentId(),queryNodeInfoListSO.getCountryId(),queryNodeInfoListSO.getProvinceId(),
                queryNodeInfoListSO.getCityId(),queryNodeInfoListSO.getName(),queryNodeInfoListSO.getStatus());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 添加节点规格配置
     * @param addNodeModelSO
     * @return
     */
    public ResultMessage addNodeModel(AddNodeModelSO addNodeModelSO){
        NodeModel nodeModel = new NodeModel();
        CommonUtil.copyProperties(addNodeModelSO,nodeModel);
        nodeModel.setStatus(0);
        nodeModel.setCreateTime(new Date());
        nodeModel.setUpdateTime(new Date());
        int i = nodeModelMapper.insertSelective(nodeModel);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }


    /**
     * 更新节点规格配置
     * @param updateNodeModelSO
     * @return
     */
    public ResultMessage updateNodeModel(UpdateNodeModelSO updateNodeModelSO){
        NodeModel nodeModel = new NodeModel();
        CommonUtil.copyProperties(updateNodeModelSO,nodeModel);
        nodeModel.setUpdateTime(new Date());
        int i = nodeModelMapper.updateByPrimaryKeySelective(nodeModel);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 删除节点规格配置
     * @param delByIdSO
     * @return
     */
    public ResultMessage delNodeModel(BaseDelByIdSO delByIdSO){
        int i = nodeModelMapper.deleteByPrimaryKey(delByIdSO.getId());
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 查询节点规格配置列表
     * @param queryNodeModelListSO
     * @return
     */
    public ResultMessage queryNodeModelList(QueryNodeModelListSO queryNodeModelListSO){
        PageHelper.startPage(queryNodeModelListSO.getPage(), queryNodeModelListSO.getPageSize());
        Page<NodeModel> page = (Page<NodeModel>)nodeModelMapper.selectList(queryNodeModelListSO.getNodeId(),queryNodeModelListSO.getCpu(),queryNodeModelListSO.getRam(),queryNodeModelListSO.getStatus());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 查询节点规格配置选项
     * @param queryNodeModelConfigSO
     * @return
     */
    public ResultMessage queryNodeModelConfig(QueryNodeModelConfigSO queryNodeModelConfigSO){
        List<NodeModelConfigVO> list = nodeModelMapper.selectConfigByNodeId(queryNodeModelConfigSO.getNodeId());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
    }


    /**
     * 添加节点磁盘配置
     * @param addNodeDiskSO
     * @return
     */
    public ResultMessage addNodeDisk(AddNodeDiskSO addNodeDiskSO){
        NodeDisk nodeDisk = new NodeDisk();
        CommonUtil.copyProperties(addNodeDiskSO,nodeDisk);
        nodeDisk.setStatus(0);
        nodeDisk.setCreateTime(new Date());
        nodeDisk.setUpdateTime(new Date());
        int i = nodeDiskMapper.insertSelective(nodeDisk);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }


    /**
     * 更新节点磁盘配置
     * @param updateNodeDiskSO
     * @return
     */
    public ResultMessage updateNodeDisk(UpdateNodeDiskSO updateNodeDiskSO){
        NodeDisk nodeDisk = new NodeDisk();
        CommonUtil.copyProperties(updateNodeDiskSO,nodeDisk);
        nodeDisk.setUpdateTime(new Date());
        int i = nodeDiskMapper.updateByPrimaryKeySelective(nodeDisk);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 删除节点磁盘配置
     * @param delByIdSO
     * @return
     */
    public ResultMessage delNodeDisk(BaseDelByIdSO delByIdSO){
        int i = nodeDiskMapper.deleteByPrimaryKey(delByIdSO.getId());
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }


    /**
     * 查询节点磁盘配置列表
     * @param queryNodeDiskListSO
     * @return
     */
    public ResultMessage queryNodeDiskList(QueryNodeDiskListSO queryNodeDiskListSO){
        PageHelper.startPage(queryNodeDiskListSO.getPage(), queryNodeDiskListSO.getPageSize());
        Page<NodeDisk> page = (Page<NodeDisk>)nodeDiskMapper.selectList(queryNodeDiskListSO.getNodeId());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 添加节点网络配置
     * @param addNodeNetworkSO
     * @return
     */
    public ResultMessage addNodeNetwork(AddNodeNetworkSO addNodeNetworkSO){
        NodeNetwork nodeNetwork = new NodeNetwork();
        CommonUtil.copyProperties(addNodeNetworkSO,nodeNetwork);
        nodeNetwork.setStatus(0);
        nodeNetwork.setCreateTime(new Date());
        nodeNetwork.setUpdateTime(new Date());
        int i = nodeNetworkMapper.insertSelective(nodeNetwork);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }


    /**
     * 更新节点网络配置
     * @param updateNodeNetworkSO
     * @return
     */
    public ResultMessage updateNodeNetwork(UpdateNodeNetworkSO updateNodeNetworkSO){
        NodeNetwork nodeNetwork = new NodeNetwork();
        CommonUtil.copyProperties(updateNodeNetworkSO,nodeNetwork);
        nodeNetwork.setUpdateTime(new Date());
        int i = nodeNetworkMapper.updateByPrimaryKeySelective(nodeNetwork);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 删除节点网络配置
     * @param delByIdSO
     * @return
     */
    public ResultMessage delNodeNetwork(BaseDelByIdSO delByIdSO){
        int i = nodeNetworkMapper.deleteByPrimaryKey(delByIdSO.getId());
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }


    /**
     * 查询节点网络配置列表
     * @param queryNodeNetworkListSO
     * @return
     */
    public ResultMessage queryNodeNetworkList(QueryNodeNetworkListSO queryNodeNetworkListSO){
        PageHelper.startPage(queryNodeNetworkListSO.getPage(), queryNodeNetworkListSO.getPageSize());
        Page<NodeNetwork> page = (Page<NodeNetwork>)nodeNetworkMapper.selectList(queryNodeNetworkListSO.getNodeId());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 添加节点镜像信息
     * @param addNodeImageSO
     * @return
     */
    public ResultMessage addNodeImage(AddNodeImageSO addNodeImageSO){
        NodeImage nodeImage = new NodeImage();
        CommonUtil.copyProperties(addNodeImageSO,nodeImage);
        nodeImage.setStatus(0);
        nodeImage.setCreateTime(new Date());
        nodeImage.setUpdateTime(new Date());
        int i = nodeImageMapper.insertSelective(nodeImage);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }


    /**
     * 更新节点镜像信息
     * @param updateNodeImageSO
     * @return
     */
    public ResultMessage updateNodeImage(UpdateNodeImageSO updateNodeImageSO){
        NodeImage nodeImage = new NodeImage();
        CommonUtil.copyProperties(updateNodeImageSO,nodeImage);
        nodeImage.setUpdateTime(new Date());
        int i = nodeImageMapper.updateByPrimaryKeySelective(nodeImage);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }


    /**
     * 删除节点镜像信息
     * @param delByIdSO
     * @return
     */
    public ResultMessage delNodeImage(BaseDelByIdSO delByIdSO){
        int i = nodeImageMapper.deleteByPrimaryKey(delByIdSO.getId());
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }


    /**
     * 查询节点镜像列表
     * @param queryNodeImageListSO
     * @return
     */
    public ResultMessage queryNodeImageList(QueryNodeImageListSO queryNodeImageListSO){
        PageHelper.startPage(queryNodeImageListSO.getPage(), queryNodeImageListSO.getPageSize());
        Page<NodeImage> page = (Page<NodeImage>)nodeImageMapper.selectList(queryNodeImageListSO.getNodeId(),queryNodeImageListSO.getImageType());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 添加节点价格配置
     * @param addNodePriceSO
     * @return
     */
    public ResultMessage addNodePrice(AddNodePriceSO addNodePriceSO){
        NodePrice nodePrice = new NodePrice();
        CommonUtil.copyProperties(addNodePriceSO,nodePrice);
        nodePrice.setStatus(0);
        nodePrice.setCreateTime(new Date());
        nodePrice.setUpdateTime(new Date());
        int i = nodePriceMapper.insertSelective(nodePrice);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 更新节点价格配置
     * @param updateNodePriceSO
     * @return
     */
    public ResultMessage updateNodePrice(UpdateNodePriceSO updateNodePriceSO){
        NodePrice nodePrice = new NodePrice();
        CommonUtil.copyProperties(updateNodePriceSO,nodePrice);
        nodePrice.setUpdateTime(new Date());
        int i = nodePriceMapper.updateByPrimaryKeySelective(nodePrice);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 删除节点价格配置
     * @param delByIdSO
     * @return
     */
    public ResultMessage delNodePrice(BaseDelByIdSO delByIdSO){
        int i = nodePriceMapper.deleteByPrimaryKey(delByIdSO.getId());
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 查询节点价格配置列表
     * @param queryNodePriceListSO
     * @return
     */
    public ResultMessage queryNodePriceList(QueryNodePriceListSO queryNodePriceListSO){
        PageHelper.startPage(queryNodePriceListSO.getPage(), queryNodePriceListSO.getPageSize());
        Page<NodePrice> page = (Page<NodePrice>)nodePriceMapper.selectList(queryNodePriceListSO.getNodeId(),queryNodePriceListSO.getConfigType(),queryNodePriceListSO.getStatus());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 查询节点配置选择
     * @param queryConfigSelectSO
     * @return
     */
    public ResultMessage queryConfigSelect(QueryConfigSelectSO queryConfigSelectSO){
        List<NodeModelConfigVO> list = null;
        if("model".equals(queryConfigSelectSO.getConfigType().toLowerCase())){
            list = nodeModelMapper.selectConfigByNodeId(queryConfigSelectSO.getNodeId());
        }else if("disk".equals(queryConfigSelectSO.getConfigType().toLowerCase())){
            list = nodeDiskMapper.selectConfigByNodeId(queryConfigSelectSO.getNodeId());
        }else if("network".equals(queryConfigSelectSO.getConfigType().toLowerCase())){
            list = nodeNetworkMapper.selectConfigByNodeId(queryConfigSelectSO.getNodeId());
        }
        if(list != null){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"无效的配置类型",list);
        }


    }




}
