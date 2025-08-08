package com.core.manycloudservice.service.impl;

import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.vo.main.*;
import com.core.manycloudservice.service.MainService;
import com.core.manycloudservice.so.main.*;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.soap.Node;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MainServiceImpl implements MainService {

    @Autowired
    private BannerInfoMapper bannerInfoMapper;

    @Autowired
    private RegionContinentMapper regionContinentMapper;

    @Autowired
    private RegionCountryMapper regionCountryMapper;

    @Autowired
    private RegionProvinceMapper regionProvinceMapper;

    @Autowired
    private RegionCityMapper regionCityMapper;

    @Autowired
    private CustomerServiceMapper customerServiceMapper;

    @Autowired
    private NodeAttributeMapper attributeMapper;

    @Autowired
    private NodePriceMapper nodePriceMapper;

    @Autowired
    private NodeNetworkMapper nodeNetworkMapper;

    @Autowired
    private NodeModelMapper nodeModelMapper;

    @Autowired
    private NodeDiskMapper nodeDiskMapper;

    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    @Autowired
    private NodeImageMapper nodeImageMapper;

    @Autowired
    private AttributeInfoMapper attributeInfoMapper;




    /**
     * 根据类型查询Banner图信息
     * @param queryBannerByTypeSO
     * @return
     */
    public ResultMessage queryBannerByType(QueryBannerByTypeSO queryBannerByTypeSO){
        List<BannerInfo> list = bannerInfoMapper.selectByType(queryBannerByTypeSO.getType());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
    }

    /**
     * 查询有效的大洲信息
     * @return
     */
    public ResultMessage queryContinentValid(){
        List<RegionContinent> list = regionContinentMapper.selectValid();
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
    }

    /**
     * 根据大洲查询国家信息
     * @return
     */
    public ResultMessage queryCountryByContinent(QueryCountryByContinentSO queryCountryByContinentSO){
        List<RegionCountry> list;
        if(queryCountryByContinentSO.getContinentId() == -1){
            list = regionCountryMapper.selectAll();
        }else{
            list = regionCountryMapper.selectByContinent(queryCountryByContinentSO.getContinentId());
        }
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
    }


    /**
     * 根据国家查询省份信息
     * @return
     */
    public ResultMessage queryProvinceByCountry(QueryProvinceByCountrySO queryProvinceByCountrySO){
        List<RegionProvince> list = regionProvinceMapper.selectByCountry(queryProvinceByCountrySO.getCountryId());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
    }


    /**
     * 根据省份查询城市信息
     * @return
     */
    public ResultMessage querCityByProvince(QuerCityByProvinceSO querCityByProvinceSO){
        List<RegionCity> list = regionCityMapper.selectByProvince(querCityByProvinceSO.getProvinceId());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
    }

    /***
     * 查询特性信息
     * @return
     */
    public ResultMessage queryAttribute(){
        List<AttributeInfo> list = attributeInfoMapper.selectValidByType(0);//特性类型
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
    }




    /**
     * 查询客服信息
     * @return
     */
    public ResultMessage queryCustomerServiceInfo(){

        Map<Integer,List<CustomerService>> result = new HashMap<>();
        List<CustomerService> list = customerServiceMapper.selectValid();
        for(CustomerService cs : list){
            List<CustomerService> typeList = result.get(cs.getType());
            if(typeList == null){
                typeList = new ArrayList<>();
            }
            typeList.add(cs);

            result.put(cs.getType(),typeList);
        }
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }


    /**
     * 全球云服务器产品查询
     * @param queryBaseProductSO
     * @return
     */
    public ResultMessage queryBaseProduct(QueryBaseProductSO queryBaseProductSO){

        /** 通过特性查询关联的区域节点 **/
        List<Integer> nodeIds = null;
        if(queryBaseProductSO.getAttributeIds() != null && queryBaseProductSO.getAttributeIds().size() > 0){
            nodeIds = attributeMapper.selectByAttributeIds(queryBaseProductSO.getAttributeIds(),queryBaseProductSO.getAttributeIds().size());
            if(nodeIds.size() == 0){
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("total",0);
                resultMap.put("list",nodeIds);
                return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
            }
        }

        /** 分页查询数据 **/
        PageHelper.startPage(queryBaseProductSO.getPage(), queryBaseProductSO.getPageSize());
        Page<BaseProductVO> page = (com.github.pagehelper.Page<BaseProductVO>)nodeInfoMapper.selectBaseProduct(queryBaseProductSO.getContinentId(),queryBaseProductSO.getCountryId()
                ,queryBaseProductSO.getScreen(),nodeIds);

        List<BaseProductVO> list =  page.getResult();
        for(BaseProductVO baseProductVO : list){
            /** 最低价格 **/
            BigDecimal minPrice;
            /** 产品规格数量 **/
            int productNum;
            if(baseProductVO.getCityId() == null || baseProductVO.getCityId() == 0){
                minPrice = nodeInfoMapper.queryNodeMinPrice(3,baseProductVO.getProvinceId(),nodeIds);
                productNum = nodeInfoMapper.queryNodeProductNum(3,baseProductVO.getProvinceId(),nodeIds);
            }else{
                minPrice = nodeInfoMapper.queryNodeMinPrice(4,baseProductVO.getCityId(),nodeIds);
                productNum = nodeInfoMapper.queryNodeProductNum(4,baseProductVO.getCityId(),nodeIds);
            }
            baseProductVO.setMinPrice(minPrice);
            baseProductVO.setProductNum(productNum);

        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",list);
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 查询地域导航信息
     * @param queryRegionNavigationSO
     * @return
     */
    public ResultMessage queryRegionNavigation(QueryRegionNavigationSO queryRegionNavigationSO){

        List<RegionNavigationVO> list = new ArrayList<>();

        int id = queryRegionNavigationSO.getRegionId();
        int type = queryRegionNavigationSO.getRegionType();

        while (type > 0){
            if(type == 1){
                RegionContinent regionContinent = regionContinentMapper.selectByPrimaryKey(id);
                if(regionContinent == null){
                    break;
                }
                list.add(RegionNavigationVO.builder()
                        .regionId(regionContinent.getId())
                        .regionType(type)
                        .name(regionContinent.getContinentName())
                        .build());
                type = 0;
            }else if(type == 2){
                RegionCountry regionCountry = regionCountryMapper.selectByPrimaryKey(id);
                if(regionCountry == null){
                    break;
                }
                list.add(RegionNavigationVO.builder()
                        .regionId(regionCountry.getId())
                        .regionType(type)
                        .name(regionCountry.getCountryName())
                        .build());
                id = regionCountry.getSuperiorId();
                type = 1;
            }else if(type == 3){
                RegionProvince regionProvince = regionProvinceMapper.selectByPrimaryKey(id);
                if(regionProvince == null){
                    break;
                }
                list.add(RegionNavigationVO.builder()
                        .regionId(regionProvince.getId())
                        .regionType(type)
                        .name(regionProvince.getProvinceName())
                        .build());
                id = regionProvince.getSuperiorId();
                type = regionProvince.getSuperiorLevel();
            }else if(type == 4){
                RegionCity regionCity = regionCityMapper.selectByPrimaryKey(id);
                if(regionCity == null){
                    break;
                }
                list.add(RegionNavigationVO.builder()
                        .regionId(regionCity.getId())
                        .regionType(type)
                        .name(regionCity.getCityName())
                        .build());
                id = regionCity.getSuperiorId();
                type = regionCity.getSuperiorLevel();
            }
        }

        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
    }


    /**
     * 查询地域下级信息
     * @param queryRegionSubordinateSO
     * @return
     */
    public ResultMessage queryRegionSubordinate(QueryRegionSubordinateSO queryRegionSubordinateSO){

        List<RegionNavigationVO> list = new ArrayList<>();

        if(queryRegionSubordinateSO.getRegionType() == 1){
            List<RegionCountry> countryList = regionCountryMapper.selectByContinent(queryRegionSubordinateSO.getRegionId());
            for(RegionCountry country : countryList){
                list.add(RegionNavigationVO.builder()
                        .regionId(country.getId()).regionType(2).name(country.getCountryName()).build());
            }

            List<RegionProvince> provinceList = regionProvinceMapper.selectBySuperior(queryRegionSubordinateSO.getRegionId(),1);
            for(RegionProvince province : provinceList){
                list.add(RegionNavigationVO.builder()
                        .regionId(province.getId()).regionType(3).name(province.getProvinceName()).build());
            }

            List<RegionCity> cityList = regionCityMapper.selectBySuperior(queryRegionSubordinateSO.getRegionId(),1);

            for(RegionCity city : cityList){
                list.add(RegionNavigationVO.builder()
                        .regionId(city.getId()).regionType(4).name(city.getCityName()).build());
            }
        }else if(queryRegionSubordinateSO.getRegionType() == 2){
            List<RegionProvince> provinceList = regionProvinceMapper.selectBySuperior(queryRegionSubordinateSO.getRegionId(),2);
            for(RegionProvince province : provinceList){
                list.add(RegionNavigationVO.builder()
                        .regionId(province.getId()).regionType(3).name(province.getProvinceName()).build());
            }

            List<RegionCity> cityList = regionCityMapper.selectBySuperior(queryRegionSubordinateSO.getRegionId(),2);

            for(RegionCity city : cityList){
                list.add(RegionNavigationVO.builder()
                        .regionId(city.getId()).regionType(4).name(city.getCityName()).build());
            }
        }else if(queryRegionSubordinateSO.getRegionType() == 3){

            List<RegionCity> cityList = regionCityMapper.selectBySuperior(queryRegionSubordinateSO.getRegionId(),3);
            for(RegionCity city : cityList){
                list.add(RegionNavigationVO.builder()
                        .regionId(city.getId()).regionType(4).name(city.getCityName()).build());
            }
        }
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);

    }


    /**
     * 根据上级ID 查询节点区域信息
     * @param queryNodeShowSO
     * @return
     */
    public ResultMessage queryNodeShow(QueryNodeShowSO queryNodeShowSO){

        List<NodeInfo> list = nodeInfoMapper.selectBdAttByRegionId(queryNodeShowSO.getRegionType(),queryNodeShowSO.getRegionId(),queryNodeShowSO.getAttributeIds());

        List<NodeShowVO> result = new ArrayList<>();
        int liminNum = queryNodeShowSO.getAttributeIds() == null ? 0 : queryNodeShowSO.getAttributeIds().size();
        for(NodeInfo nodeInfo : list){

            if(nodeInfo.getBdNum() >= liminNum) {//判断绑定特性 数量 大于等于 筛选的特性数量

                /** 节点最小配置价格 **/
                BigDecimal minPrice = nodeInfoMapper.queryNodeMinPrice(5, nodeInfo.getId(), null);

                List<AttributeInfo> attributeList = attributeInfoMapper.selectByNodeId(nodeInfo.getId(), 0);
                List<String> attributes = attributeList.stream().map(a -> a.getName()).collect(Collectors.toList());
                NodeShowVO nodeShowVO = NodeShowVO.builder()
                        .nodeId(nodeInfo.getId())
                        .nodeName(nodeInfo.getNodeName())
                        .attributeList(attributes)
                        .minPrice(minPrice)
                        .build();
                result.add(nodeShowVO);
            }

        }
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }


    /**
     * 查询产品列表数据
     * @param queryProductDetailSO
     * @return
     */
    public ResultMessage queryProductDetail(QueryProductDetailSO queryProductDetailSO){

        List<NodeInfo> nodeList = nodeInfoMapper.selectBdAttByRegionId(queryProductDetailSO.getRegionType(),queryProductDetailSO.getRegionId(),queryProductDetailSO.getAttributeIds());
        List<Integer> nodeIds = null;
        int liminNum = queryProductDetailSO.getAttributeIds() == null ? 0 : queryProductDetailSO.getAttributeIds().size();
        for(NodeInfo nodeInfo : nodeList){
            if(nodeInfo.getBdNum() >= liminNum){//判断绑定特性 数量 大于等于 筛选的特性数量
                if(nodeIds == null){
                    nodeIds = new ArrayList<>();
                }
                nodeIds.add(nodeInfo.getId());
            }
        }

        /** 分页查询节点配置数据 **/
        PageHelper.startPage(queryProductDetailSO.getPage(), queryProductDetailSO.getPageSize());
        Page<QueryProductDetailVO> page = (com.github.pagehelper.Page<QueryProductDetailVO>)nodeModelMapper.selectProductDetail(nodeIds,queryProductDetailSO.getSort());

        List<QueryProductDetailVO> list = page.getResult();


        if(nodeIds != null && nodeIds.size() > 0){

            /** 节点磁盘数据 **/
            List<NodeDisk> diskList = nodeDiskMapper.selectByNodeIds(nodeIds);
            Map<String,NodeDisk> diskMap = new HashMap<>();
            for(NodeDisk nodeDisk : diskList){
                String key = nodeDisk.getModelId() != null && nodeDisk.getModelId() > 0 ? nodeDisk.getNodeId()+"-"+nodeDisk.getModelId() : nodeDisk.getNodeId() + "";
                diskMap.put(key,nodeDisk);
            }

            /** 节点网络数据 **/
            List<NodeNetwork> networkList = nodeNetworkMapper.selectByNodeIds(nodeIds);
            Map<String,NodeNetwork> networMap = new HashMap<>();
            for(NodeNetwork nodeNetwork : networkList){
                String key = nodeNetwork.getModelId() != null && nodeNetwork.getModelId() > 0 ? nodeNetwork.getNodeId()+"-"+nodeNetwork.getModelId()+"-"+nodeNetwork.getType() : nodeNetwork.getNodeId() + "-"+nodeNetwork.getType();
                networMap.put(key,nodeNetwork);
            }

            for(QueryProductDetailVO pd : list){
                BigDecimal totalPrice = pd.getModelPrice() == null ? BigDecimal.valueOf(0) : pd.getModelPrice();
                String key = pd.getRegular().equals("Y")? pd.getNodeId()+"-"+pd.getModelId() : pd.getNodeId() + "";

                /** 计算磁盘默认配置价格 **/
                NodeDisk nodeDisk = diskMap.get(key);
                if(nodeDisk != null){
                    Integer diskNum = nodeDisk.getMinNum().subtract(nodeDisk.getGiveNum()).intValue();
                    Integer diskItem = nodeDisk.getItem().intValue();
                    BigDecimal diskPrice = nodeDisk.getPrice();

                    pd.setDiskNum(diskNum);
                    pd.setDiskItem(diskItem);
                    pd.setDiskPrice(diskPrice);
                    BigDecimal p = diskNum == 0 || diskItem == 0? BigDecimal.valueOf(0) : BigDecimal.valueOf(diskNum).divide( BigDecimal.valueOf(diskItem),2,BigDecimal.ROUND_DOWN);
                    totalPrice =  totalPrice.add(p.multiply(diskPrice));
                }

                /** 计算带宽默认配置价格 **/
                NodeNetwork bandwidth = networMap.get(key+"-0");
                if(bandwidth != null){
                    Integer bandwidthNum = bandwidth.getMinNum().intValue();
                    Integer bandwidthItem = bandwidth.getItem().intValue();
                    BigDecimal bandwidthPrice = bandwidth.getPrice();

                    pd.setBandwidthExtend(bandwidth.getExtendBl());
                    pd.setBandwidthNum(bandwidthNum);
                    pd.setBandwidthItem(bandwidthItem);
                    pd.setBandwidthPrice(bandwidthPrice);
                    pd.setBandwidthType(bandwidth.getNetworkType());
                    BigDecimal p = bandwidthNum == 0 || bandwidthItem == 0? BigDecimal.valueOf(0) : BigDecimal.valueOf(bandwidthNum).divide( BigDecimal.valueOf(bandwidthItem),2,BigDecimal.ROUND_DOWN);
                    totalPrice =  totalPrice.add(p.multiply(bandwidthPrice));
                }


                /** 计算流量默认配置价格 **/
                NodeNetwork flow = networMap.get(key+"-1");
                if(flow != null){
                    Integer flowNum = flow.getMinNum().intValue();
                    Integer flowItem = flow.getItem().intValue();
                    BigDecimal flowPrice = flow.getPrice();

                    pd.setFlowExtend(flow.getExtendBl());
                    pd.setFlowNum(flowNum);
                    pd.setFlowItem(flowItem);
                    pd.setFlowPrice(flowPrice);
                    pd.setFlowType(flow.getNetworkType());
                    BigDecimal p = flowNum == 0 || flowItem == 0? BigDecimal.valueOf(0) : BigDecimal.valueOf(flowNum).divide( BigDecimal.valueOf(flowItem),2,BigDecimal.ROUND_DOWN);
                    totalPrice =  totalPrice.add(p.multiply(flowPrice));
                }

                pd.setTotalPrice(totalPrice);

            }

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("total",page.getTotal());
            resultMap.put("list",list);
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);

        }else{
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("total",0);
            resultMap.put("list",new ArrayList<>());
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
        }


    }


    /**
     * 查询节点测试信息
     * @param queryNodeTestSO
     * @return
     */
    public ResultMessage queryNodeTest(QueryNodeTestSO queryNodeTestSO){

        List<NodeInfo> list = nodeInfoMapper.selectByRegionId(4,queryNodeTestSO.getCityId());

        List<ContrastNodeVO> contrastList = attributeInfoMapper.selectContrastNode(queryNodeTestSO.getCityId());

        Map<Integer,String> attributeMap = new HashMap<>();
        Map<String,String> bindingMap = new HashMap<>();
        for(ContrastNodeVO contrast : contrastList){
            if(attributeMap.get(contrast.getId()) == null){
                attributeMap.put(contrast.getId(),contrast.getName());
            }

            if(bindingMap.get(contrast.getId()+"-"+contrast.getNodeId()) == null){
                bindingMap.put(contrast.getId()+"-"+contrast.getNodeId(),contrast.getParamStr());
            }
        }

        List<List<String>> result = new ArrayList<>();

        List<String> titleList = new ArrayList<>();
        titleList.add("功能参数");
        for(NodeInfo nodeInfo : list){
            titleList.add(nodeInfo.getNodeName());
        }
        result.add(titleList);


        for(Integer key : attributeMap.keySet()){
            List<String> contentList = new ArrayList<>();
            contentList.add(attributeMap.get(key));
            for(NodeInfo nodeInfo : list){
                String newKey =  key + "-" + nodeInfo.getId();
                String param = bindingMap.get(newKey);
                contentList.add(param == null ? "不支持" : param);
            }
            result.add(contentList);
        }

        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }


    /**
     * 根据城市ID 查询节点可用区信息
     * @param queryBuyNodeSO
     * @return
     */
    public ResultMessage queryBuyNode(QueryBuyNodeSO queryBuyNodeSO){
        List<NodeInfo> list = nodeInfoMapper.selectByRegionId(4,queryBuyNodeSO.getCityId());
        List<Map<String,Object>> result = new ArrayList<>();
        for(NodeInfo nodeInfo : list){
            Map<String,Object> map = new HashMap<>();
            map.put("nodeId",nodeInfo.getId());
            map.put("nodeName",nodeInfo.getNodeName());
            result.add(map);
        }
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }


    /**
     * 询节点可用区的配置信息
     * @param queryBuyNodeModelSO
     * @return
     */
    public ResultMessage queryBuyNodeModel(QueryBuyNodeModelSO queryBuyNodeModelSO){
        List<NodeModel> list = nodeModelMapper.selectByNode(queryBuyNodeModelSO.getNodeId());

        Map<String,List<Map<String,Object>>> result = new LinkedHashMap<>();

        for(NodeModel nodeModel : list){
            List<Map<String,Object>> ramList = result.get(nodeModel.getCpuVal());
            if(ramList == null){
                ramList = new ArrayList<>();
            }
            Map<String,Object> ramMap = new HashMap<>();
            ramMap.put("ram",nodeModel.getRamVal());
            ramMap.put("id",nodeModel.getId());
            ramList.add(ramMap);

            result.put(nodeModel.getCpuVal(),ramList);
        }
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }

    /**
     * 查询节点售卖磁盘信息
     * @param queryBuyNodeDetailSO
     * @return
     */
    public ResultMessage queryBuyNodeDisk(QueryBuyNodeDetailSO queryBuyNodeDetailSO){

        NodeModel model;
        if(queryBuyNodeDetailSO.getModelId() != null){
            model = nodeModelMapper.selectByPrimaryKey(queryBuyNodeDetailSO.getModelId());
        }else{
            model = nodeModelMapper.selectByConfig(queryBuyNodeDetailSO.getNodeId(),queryBuyNodeDetailSO.getCpu(),queryBuyNodeDetailSO.getRam());
        }

        NodeDisk nodeDisk;//磁盘数据

        if("Y".equals(model.getRegular())){
            nodeDisk = nodeDiskMapper.selectByNode(queryBuyNodeDetailSO.getNodeId(),model.getId());
        }else{
            nodeDisk = nodeDiskMapper.selectByNode(queryBuyNodeDetailSO.getNodeId(),null);
        }

        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,nodeDisk);
    }


    /**
     * 查询节点售卖网络信息
     * @param queryBuyNodeDetailSO
     * @return
     */
    public ResultMessage queryBuyNodeNetwork(QueryBuyNodeDetailSO queryBuyNodeDetailSO){
        NodeModel model;
        if(queryBuyNodeDetailSO.getModelId() != null && queryBuyNodeDetailSO.getModelId() != 0){
            model = nodeModelMapper.selectByPrimaryKey(queryBuyNodeDetailSO.getModelId());
        }else{
            model = nodeModelMapper.selectByConfig(queryBuyNodeDetailSO.getNodeId(),queryBuyNodeDetailSO.getCpu(),queryBuyNodeDetailSO.getRam());
        }

        NodeNetwork bandwidth;//带宽数据
        NodeNetwork flow;//流量数据

        if("Y".equals(model.getRegular())){

            bandwidth = nodeNetworkMapper.selectByNode(queryBuyNodeDetailSO.getNodeId(),0,model.getId());
            flow = nodeNetworkMapper.selectByNode(queryBuyNodeDetailSO.getNodeId(),1,model.getId());
        }else{

            bandwidth = nodeNetworkMapper.selectByNode(queryBuyNodeDetailSO.getNodeId(),0,null);
            flow = nodeNetworkMapper.selectByNode(queryBuyNodeDetailSO.getNodeId(),1,null);
        }
        Map<String,NodeNetwork> result = new HashMap<>();
        result.put("bandwidth",bandwidth);
        result.put("flow",flow);
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }

    /**
     * 查询节点镜像信息
     * @param queryBuyNodeDetailSO
     * @return
     */
    public ResultMessage queryNodeImage(QueryBuyNodeDetailSO queryBuyNodeDetailSO){

        List<NodeImage> list = nodeImageMapper.selectByNode(queryBuyNodeDetailSO.getNodeId());
        Map<String, List<NodeImage>> result = new HashMap<>();
        for(NodeImage nodeModel : list){
            List<NodeImage> images = result.get(nodeModel.getImageType());
            if(images == null){
                images = new ArrayList<>();
            }
            images.add(nodeModel);
            result.put(nodeModel.getImageType(),images);
        }

        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,result);
    }


}
