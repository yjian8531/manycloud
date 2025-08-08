package com.core.manycloudadmin.service.impl;

import com.core.manycloudadmin.service.RegionService;
import com.core.manycloudadmin.so.admin.*;
import com.core.manycloudadmin.so.region.*;
import com.core.manycloudadmin.so.BaseDelByIdSO;
import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.vo.admin.NodeAttributeBdVO;
import com.core.manycloudcommon.vo.admin.QueryRegionSelectVO;
import com.core.manycloudcommon.vo.main.RegionNavigationVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 地域业务类
 */
@Slf4j
@Service
public class RegionServiceImpl implements RegionService {

    @Autowired
    private RegionContinentMapper regionContinentMapper;

    @Autowired
    private RegionCountryMapper regionCountryMapper;

    @Autowired
    private RegionProvinceMapper regionProvinceMapper;

    @Autowired
    private RegionCityMapper regionCityMapper;

    @Autowired
    private AttributeInfoMapper attributeInfoMapper;

    @Autowired
    private NodeAttributeMapper nodeAttributeMapper;

    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    /**
     * 添加大洲信息
     * @param addContinentSO
     * @return
     */
    public ResultMessage addContinent(AddContinentSO addContinentSO){
        RegionContinent regionContinent = new RegionContinent();
        CommonUtil.copyProperties(addContinentSO,regionContinent);
        regionContinent.setStatus(0);
        regionContinent.setCreateTime(new Date());
        regionContinent.setUpdateTime(new Date());
        int i = regionContinentMapper.insertSelective(regionContinent);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 更新大洲信息
     * @param updateContinentSO
     * @return
     */
    public ResultMessage updateContinent(UpdateContinentSO updateContinentSO){
        RegionContinent regionContinent = new RegionContinent();
        CommonUtil.copyProperties(updateContinentSO,regionContinent);
        regionContinent.setUpdateTime(new Date());
        int i = regionContinentMapper.updateByPrimaryKeySelective(regionContinent);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 删除大洲信息
     * @param delByIdSO
     * @return
     */
    public ResultMessage delContinent(BaseDelByIdSO delByIdSO){
        int i = regionContinentMapper.deleteByPrimaryKey(delByIdSO.getId());
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 分页查询大洲列表
     * @param queryContinentListSO
     * @return
     */
    public ResultMessage queryContinentList(QueryContinentListSO queryContinentListSO){
        PageHelper.startPage(queryContinentListSO.getPage(), queryContinentListSO.getPageSize());
        Page<RegionContinent> page = (Page<RegionContinent>)regionContinentMapper.selectList(queryContinentListSO.getContinentName(),queryContinentListSO.getStatus());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 添加区域国家信息
     * @param addCountrySO
     * @return
     */
    public ResultMessage addCountry(AddCountrySO addCountrySO){
        RegionCountry regionCountry = new RegionCountry();
        CommonUtil.copyProperties(addCountrySO,regionCountry);
        regionCountry.setStatus(0);
        regionCountry.setCreateTime(new Date());
        regionCountry.setUpdateTime(new Date());
        int i = regionCountryMapper.insertSelective(regionCountry);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }

    }

    /**
     * 更新区域国家信息
     * @param updateCountrySO
     * @return
     */
    public ResultMessage updateCountry(UpdateCountrySO updateCountrySO){
        RegionCountry regionCountry = new RegionCountry();
        CommonUtil.copyProperties(updateCountrySO,regionCountry);
        regionCountry.setUpdateTime(new Date());
        int i = regionCountryMapper.updateByPrimaryKeySelective(regionCountry);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 删除区域国家信息
     * @param delByIdSO
     * @return
     */
    public ResultMessage delCountry(BaseDelByIdSO delByIdSO){
        int i = regionCountryMapper.deleteByPrimaryKey(delByIdSO.getId());
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 分页查询国家列表
     * @param queryCountryListSO
     * @return
     */
    public ResultMessage queryCountryList(QueryCountryListSO queryCountryListSO){
        PageHelper.startPage(queryCountryListSO.getPage(), queryCountryListSO.getPageSize());
        Page<RegionCountry> page = (Page<RegionCountry>)regionCountryMapper.selectList(queryCountryListSO.getSuperiorId(),queryCountryListSO.getCountryName(),queryCountryListSO.getStatus());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }

    /**
     * 添加省份信息
     * @param addProvinceSO
     * @return
     */
    public ResultMessage addProvince(AddProvinceSO addProvinceSO){
        RegionProvince regionProvince = new RegionProvince();
        CommonUtil.copyProperties(addProvinceSO,regionProvince);
        regionProvince.setStatus(0);
        regionProvince.setCreateTime(new Date());
        regionProvince.setUpdateTime(new Date());
        int i = regionProvinceMapper.insertSelective(regionProvince);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 更新省份信息
     * @param updateProvinceSO
     * @return
     */
    public ResultMessage updateProvince(UpdateProvinceSO updateProvinceSO){
        RegionProvince regionProvince = new RegionProvince();
        CommonUtil.copyProperties(updateProvinceSO,regionProvince);
        regionProvince.setUpdateTime(new Date());
        int i = regionProvinceMapper.updateByPrimaryKeySelective(regionProvince);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 删除省份信息
     * @param delByIdSO
     * @return
     */
    public ResultMessage delProvince(BaseDelByIdSO delByIdSO){
        int i = regionProvinceMapper.deleteByPrimaryKey(delByIdSO.getId());
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 分页查询省份列表
     * @param queryProvinceListSO
     * @return
     */
    public ResultMessage queryProvinceList(QueryProvinceListSO queryProvinceListSO){
        PageHelper.startPage(queryProvinceListSO.getPage(), queryProvinceListSO.getPageSize());
        Page<RegionProvince> page = (Page<RegionProvince>)regionProvinceMapper.selectList(queryProvinceListSO.getSuperiorId(),queryProvinceListSO.getSuperiorLevel(),queryProvinceListSO.getProvinceName(),queryProvinceListSO.getStatus());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }

    /**
     * 添加城市SO
     * @param addCitySO
     * @return
     */
    public ResultMessage addCity(AddCitySO addCitySO){
        RegionCity regionCity = new RegionCity();
        CommonUtil.copyProperties(addCitySO,regionCity);
        regionCity.setStatus(0);
        regionCity.setCreateTime(new Date());
        regionCity.setUpdateTime(new Date());
        int i = regionCityMapper.insertSelective(regionCity);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 更新城市信息
     * @param updateCitySO
     * @return
     */
    public ResultMessage updateCity(UpdateCitySO updateCitySO){
        RegionCity regionCity = new RegionCity();
        CommonUtil.copyProperties(updateCitySO,regionCity);
        regionCity.setUpdateTime(new Date());
        int i = regionCityMapper.updateByPrimaryKeySelective(regionCity);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 删除城市信息
     * @param delByIdSO
     * @return
     */
    public ResultMessage delCity(BaseDelByIdSO delByIdSO){
        int i = regionCityMapper.deleteByPrimaryKey(delByIdSO.getId());
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 分页查询城市信息
     * @param queryCityListSO
     * @return
     */
    public ResultMessage queryCityList(QueryCityListSO queryCityListSO){
        PageHelper.startPage(queryCityListSO.getPage(), queryCityListSO.getPageSize());
        Page<RegionCity> page = (Page<RegionCity>)regionCityMapper.selectList(queryCityListSO.getSuperiorId(),queryCityListSO.getSuperiorLevel(),queryCityListSO.getCityName(),queryCityListSO.getStatus());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }

    /**
     * 查询地域下拉选择信息
     * @param queryRegionSelectSO
     * @return
     */
    public ResultMessage queryRegionSelect(QueryRegionSelectSO queryRegionSelectSO){

        List<QueryRegionSelectVO> list = new ArrayList<>();

        int level = queryRegionSelectSO.getSuperiorLevel();

        if(level == 1){
            List<RegionContinent> continents = regionContinentMapper.selectAll();
            for(RegionContinent rc : continents){
                QueryRegionSelectVO qrs = QueryRegionSelectVO.builder()
                        .name(rc.getContinentName())
                        .superiorId(rc.getId())
                        .build();
                list.add(qrs);
            }

        }else if(level == 2){
            List<RegionCountry> countrys = regionCountryMapper.selectAll();
            for(RegionCountry rc : countrys){
                QueryRegionSelectVO qrs = QueryRegionSelectVO.builder()
                        .name(rc.getCountryName())
                        .superiorId(rc.getId())
                        .build();
                list.add(qrs);
            }
        }else if(level == 3){
            List<RegionProvince> regionProvinces = regionProvinceMapper.selectAll();
            for(RegionProvince rr : regionProvinces){
                QueryRegionSelectVO qrs = QueryRegionSelectVO.builder()
                        .name(rr.getProvinceName())
                        .superiorId(rr.getId())
                        .build();
                list.add(qrs);
            }
        }else if(level == 4){
            List<RegionCity> regionCitys = regionCityMapper.selectAll();
            for(RegionCity rc : regionCitys){
                QueryRegionSelectVO qrs = QueryRegionSelectVO.builder()
                        .name(rc.getCityName())
                        .superiorId(rc.getId())
                        .build();
                list.add(qrs);
            }
        }

        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
    }


    /**
     * 查询产品特性协议列表
     * @param queryAttributeListSO
     * @return
     */
    public ResultMessage queryAttributeList(QueryAttributeListSO queryAttributeListSO){

        PageHelper.startPage(queryAttributeListSO.getPage(), queryAttributeListSO.getPageSize());
        Page<AttributeInfo> page = (Page<AttributeInfo>)attributeInfoMapper.selectList(queryAttributeListSO.getType(),queryAttributeListSO.getName());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);

    }


    /**
     * 添加产品特性协议
     * @param addAttributeSO
     * @return
     */
    public ResultMessage addAttribute(AddAttributeSO addAttributeSO){

        AttributeInfo attributeInfo = new AttributeInfo();
        CommonUtil.copyProperties(addAttributeSO,attributeInfo);
        attributeInfo.setStatus(0);
        attributeInfo.setCreateTime(new Date());
        attributeInfo.setUpdateTime(new Date());
        int i = attributeInfoMapper.insertSelective(attributeInfo);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }

    }


    /**
     * 更新产品特性协议
     * @param updateAttributeSO
     * @return
     */
    public ResultMessage updateAttribute(UpdateAttributeSO updateAttributeSO){

        AttributeInfo attributeInfo = new AttributeInfo();
        CommonUtil.copyProperties(updateAttributeSO,attributeInfo);
        attributeInfo.setUpdateTime(new Date());
        int i = attributeInfoMapper.updateByPrimaryKeySelective(attributeInfo);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }

    }


    /**
     * 删除产品特性协议
     * @param delAttributeSO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage delAttribute(DelAttributeSO delAttributeSO){

        int i = attributeInfoMapper.deleteByPrimaryKey(delAttributeSO.getId());
        if(i > 0){

            nodeAttributeMapper.deleteByAttributeId(delAttributeSO.getId());

            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }

    }


    /**
     * 查询特性协议绑定信息
     * @param queryAttributeBindingSO
     * @return
     */
    public ResultMessage queryAttributeBinding(QueryAttributeBindingSO queryAttributeBindingSO){

        List<NodeAttributeBdVO> list = nodeAttributeMapper.selectNodeByAttributeId(queryAttributeBindingSO.getId(),queryAttributeBindingSO.getLabel());
        Map<Integer,NodeAttributeBdVO> resultMap = list.stream() .collect(Collectors.toMap(nb -> nb.getNodeId(), nb -> nb));

        List<NodeInfo> nodeList = nodeInfoMapper.selectByLabel(queryAttributeBindingSO.getLabel());

        List<NodeAttributeBdVO> result = new ArrayList<>();

        for(NodeInfo nodeInfo : nodeList){
            NodeAttributeBdVO entity =   resultMap.get(nodeInfo.getId());
            if(entity == null){
                entity = new NodeAttributeBdVO();
                entity.setNodeId(nodeInfo.getId());
                entity.setAttributeId(queryAttributeBindingSO.getId());
                entity.setNodeName(nodeInfo.getNodeName());
                entity.setTad("N");
            }
            result.add(entity);

        }
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }

    /**
     * 绑定特性协议
     * @param bindingAttributeSO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage bindingAttribute(BindingAttributeSO bindingAttributeSO){

        /** 获取当前绑定信息 **/
        List<NodeAttributeBdVO> list = nodeAttributeMapper.selectNodeByAttributeId(bindingAttributeSO.getId(),bindingAttributeSO.getLabel());
        Map<Integer,NodeAttributeBdVO> map = list.stream() .collect(Collectors.toMap(na -> na.getNodeId() , na -> na));



        /** 需要删除的绑定集合 **/
        List<Integer> delList = new ArrayList<>();

        /** 需要添加的绑定集合 **/
        List<NodeAttribute> addList = new ArrayList<>();

        /** 批量添加 **/
        for(Integer nodeId : bindingAttributeSO.getNodeIds()){
            NodeAttributeBdVO item = map.get(nodeId);
            if(item == null){
                NodeAttribute nodeAttribute = new NodeAttribute();
                nodeAttribute.setAttributeId(bindingAttributeSO.getId());
                nodeAttribute.setNodeId(nodeId);
                nodeAttribute.setStatus(0);
                nodeAttribute.setCreateTime(new Date());
                nodeAttribute.setUpdateTime(new Date());
                addList.add(nodeAttribute);
            }else{
                item.setTad("O");
                map.put(nodeId,item);
            }
        }
        if(addList.size() > 0){
            nodeAttributeMapper.insertList(addList);
        }



        /** 批量删除 **/
        for(Integer key : map.keySet()){
            NodeAttributeBdVO nodeAttributeBdVO = map.get(key);
            if(!"O".equals(nodeAttributeBdVO.getTad())){
                delList.add(nodeAttributeBdVO.getId());
            }
        }
        if(delList.size() > 0){
            nodeAttributeMapper.deleteByIds(delList);
        }


        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
    }



}
