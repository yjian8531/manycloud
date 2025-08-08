package com.core.manycloudservice.controller;

import com.core.manycloudcommon.caller.*;
import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.enums.RegionLevelEnum;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.model.AccountApi;

import com.core.manycloudcommon.utils.StringUtils;
import com.core.manycloudservice.util.WeiXinCaller;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private NodeInfoMapper nodeInfoMapper;

    @Autowired
    private NodeDiskMapper nodeDiskMapper;

    @Autowired
    private NodeImageMapper nodeImageMapper;

    @Autowired
    private NodeModelMapper nodeModelMapper;

    @Autowired
    private NodeNetworkMapper nodeNetworkMapper;

    @Autowired
    private RegionCityMapper regionCityMapper;

    @Autowired
    private RegionProvinceMapper regionProvinceMapper;

    @Autowired
    private RegionCountryMapper regionCountryMapper;

    @Autowired
    private RegionContinentMapper regionContinentMapper;

    @Autowired
    private NodePriceMapper nodePriceMapper;

    @Autowired
    private PlatformAccountMapper platformAccountMapper;

    @Autowired
    private WeiXinCaller weiXinCaller;

    @GetMapping("/wx")
    public void exText(){
        weiXinCaller.sendRenewSuccess("55f4a781c5f84eca81f5f487c4d14172",BigDecimal.valueOf(100),"洛杉矶-1C1G",
                new Date(),2);
    }



    //@GetMapping("/ru/sys")
    public void aliYunSys(){

        List<NodeInfo> list = nodeInfoMapper.selectByLabel("RCLOUD");
        for(NodeInfo nodeInfo : list){


            //获取默认资源平台账号
            PlatformAccount platformAccount = platformAccountMapper.selectDefault(nodeInfo.getLabel());

            /** 节点可用区 **/
            //项目ID
            String projectId = null;
            if(StringUtils.isNotEmpty(nodeInfo.getNodeParam())){
                JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
                projectId = param.get("projectId") == null ? null:param.getString("projectId");
            }
            AccountApi accountApi = AccountApi.builder()
                    .regionId(nodeInfo.getNodeVal())
                    .label(nodeInfo.getLabel())
                    .account(platformAccount.getAccount())
                    .keyNo(platformAccount.getKeyNo())
                    .keySecret(platformAccount.getKeySecret())
                    .baseUrl(platformAccount.getUrl())
                    .projectId(projectId)
                    .build();

            RcloudCaller caller = RcloudCaller.getClient(accountApi);
            String str = caller.getImage();
            System.out.println(str);
            JSONArray imageList = JSONObject.fromObject(str).getJSONArray("ImageSet");
            for(Object obj : imageList){
                JSONObject image = JSONObject.fromObject(obj);
                String version = image.getString("ImageName");
                String type= null;//image.getString("vendor");
                String id = image.getString("ImageId");


                if(version.toLowerCase().indexOf("Ubuntu".toLowerCase()) > -1){
                    type = "Ubuntu";
                    version = version.replace("Ubuntu","").trim();
                }else if(version.toLowerCase().indexOf("Debian".toLowerCase()) > -1){
                    type = "Debian";
                    version = version.replace("Debian","").trim();
                }else if(version.toLowerCase().indexOf("CentOS".toLowerCase()) > -1){
                    type = "CentOS";
                    version = version.replace("CentOS","").trim();
                }else if(version.toLowerCase().indexOf("Windows".toLowerCase()) > -1){
                    type = "Windows";
                    version = version.replace("Windows","").trim();
                }

                if(type != null){
                    if(type.equals("Windows") || type.equals("Ubuntu") || type.equals("Fedora") || type.equals("Debian") || type.equals("CentOS")){
                        log.info("{} - {} - {}",type,version,id);
                    NodeImage nodeImage = new NodeImage();
                    nodeImage.setNodeId(nodeInfo.getId());
                    nodeImage.setImageType(type);
                    nodeImage.setImageVersion(version);
                    nodeImage.setImageParam(id);
                    if(type.equals("Windows")){
                        nodeImage.setStatus(0);
                    }else{
                        nodeImage.setStatus(0);
                    }
                    nodeImage.setCreateTime(new Date());
                    nodeImage.setUpdateTime(new Date());
                    nodeImageMapper.insertSelective(nodeImage);
                    }
                }


            }


        }

    }

    @Transactional(rollbackFor = Exception.class)
    //@GetMapping("/akam/exec")
    public void aliYun(){

        String[] nodeArray = {"阿姆斯特丹:nl-ams","亚特兰大:us-southeast","芝加哥:us-ord","达拉斯:us-central","法兰克福:eu-central",
                "弗里蒙特:us-west","钦奈:in-maa","雅加达:id-cgk","伦敦:eu-west","洛杉矶:us-lax",
                "马德里:es-mad","墨尔本:au-mel","迈阿密:us-mia","米兰:it-mil","新加坡:ap-south",
                "西雅图:us-sea","巴黎:fr-par","大板:jp-osa","纽瓦克:us-east","孟买:ap-west",
                "斯德哥尔摩:se-sto","悉尼:ap-southeast","圣保罗:br-gru","东京:jp-tyo-3","多伦多:ca-central",
                "华盛顿:us-iad"};

        String nodeJsonStr = "[\n" +
                "\t\"1:1:25:SSD:40:Mpbs:1:TB:53.57:g6-nanode-1\",\n" +
                "\t\"1:2:50:SSD:40:Mpbs:2:TB:128.57:g6-standard-1\",\n" +
                "\t\"2:4:80:SSD:40:Mpbs:4:TB:257.14:g6-standard-2\",\n" +
                "\t\"4:8:160:SSD:40:Mpbs:5:TB:514.29:g6-standard-4\",\n" +
                "\t\"6:16:320:SSD:40:Mpbs:8:TB:1028.57:g6-standard-6\",\n" +
                "\t\"8:32:640:SSD:40:Mpbs:16:TB:2057.14:g6-standard-8\",\n" +
                "\t\"16:64:1280:SSD:40:Mpbs:20:TB:4114.29:g6-standard-16\"\n" +
                "]";

        for(String str : nodeArray){

            String name = str.split(":")[0];
            String val = str.split(":")[1];
            NodeInfo nodeInfo = new NodeInfo();
            nodeInfo.setLabel("AKMPLA");
            nodeInfo.setNodeVal(val);
            nodeInfo.setNodeName(name+"M区");
            nodeInfo.setSorting(100);

            RegionCity city = regionCityMapper.selectByName(name);
            if(city == null){
                log.info(name +"->  null");
            }
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

            nodeInfo.setCityId(city.getId());
            nodeInfo.setStatus(0);
            nodeInfo.setCreateTime(new Date());
            nodeInfo.setUpdateTime(new Date());

            int i = nodeInfoMapper.insertSelective(nodeInfo);
            if(i > 0){

                JSONArray nodeJson = JSONArray.fromObject(nodeJsonStr);
                for(Object obj : nodeJson){
                    String[] pz = obj.toString().trim().split(":");
                    String cpu = pz[0];
                    String ram = pz[1];
                    String disk = pz[2];
                    String diskType = pz[3];
                    String network = pz[4];
                    String networkType = pz[5];
                    String bandwidth = pz[6];
                    String bandwidthType = pz[7];
                    String price = pz[8];
                    String model = pz[9];


                    NodeModel nodeModel = new NodeModel();
                    nodeModel.setNodeId(nodeInfo.getId());
                    nodeModel.setCpuVal(cpu);
                    nodeModel.setRamVal(ram);
                    nodeModel.setModelParam(model);
                    nodeModel.setRegular("Y");
                    nodeModel.setStatus(0);
                    nodeModel.setCreateTime(new Date());
                    nodeModel.setUpdateTime(new Date());
                    int k = nodeModelMapper.insertSelective(nodeModel);
                    if(k > 0){
                        NodeDisk nodeDisk = new NodeDisk();
                        nodeDisk.setNodeId(nodeInfo.getId());
                        nodeDisk.setModelId(nodeModel.getId());
                        nodeDisk.setDiskType(diskType);
                        nodeDisk.setMinNum(new BigDecimal(disk));
                        nodeDisk.setMaxNum(new BigDecimal(disk));
                        nodeDisk.setItemNum(BigDecimal.valueOf(1));
                        nodeDisk.setGiveNum(BigDecimal.valueOf(0));
                        nodeDisk.setExtendBl("N");
                        nodeDisk.setDataBl("N");
                        nodeDisk.setStatus(0);
                        nodeDisk.setCreateTime(new Date());
                        nodeDisk.setUpdateTime(new Date());
                        nodeDiskMapper.insertSelective(nodeDisk);


                        NodeNetwork networkObj = new NodeNetwork();
                        networkObj.setNodeId(nodeInfo.getId());
                        networkObj.setModelId(nodeModel.getId());
                        networkObj.setType(0);
                        networkObj.setNetworkType(networkType);
                        if("N".equals(network)){
                            networkObj.setExtendBl("N");
                            networkObj.setMinNum(BigDecimal.valueOf(0));
                            networkObj.setMaxNum(BigDecimal.valueOf(0));
                        }else{
                            networkObj.setExtendBl("Y");
                            networkObj.setMinNum(new BigDecimal(network));
                            networkObj.setMaxNum(new BigDecimal(network));
                        }
                        networkObj.setItemNum(BigDecimal.valueOf(1));
                        networkObj.setStatus(0);
                        networkObj.setCreateTime(new Date());
                        networkObj.setUpdateTime(new Date());
                        nodeNetworkMapper.insertSelective(networkObj);


                        NodeNetwork  bandwidthObj = new NodeNetwork();
                        bandwidthObj.setNodeId(nodeInfo.getId());
                        bandwidthObj.setModelId(nodeModel.getId());
                        bandwidthObj.setType(1);
                        bandwidthObj.setNetworkType(bandwidthType);
                        if("N".equals(bandwidth)){
                            bandwidthObj.setExtendBl("N");
                            bandwidthObj.setMinNum(BigDecimal.valueOf(0));
                            bandwidthObj.setMaxNum(BigDecimal.valueOf(0));
                        }else{
                            bandwidthObj.setExtendBl("Y");
                            bandwidthObj.setMinNum(new BigDecimal(bandwidth));
                            bandwidthObj.setMaxNum(new BigDecimal(bandwidth));
                        }
                        bandwidthObj.setItemNum(BigDecimal.valueOf(1));
                        bandwidthObj.setStatus(0);
                        bandwidthObj.setCreateTime(new Date());
                        bandwidthObj.setUpdateTime(new Date());
                        nodeNetworkMapper.insertSelective(bandwidthObj);

                        NodePrice nodePrice = new NodePrice();
                        nodePrice.setNodeId(nodeInfo.getId());
                        nodePrice.setConfigType("model");
                        nodePrice.setConfigId(nodeModel.getId());
                        nodePrice.setPeriod(1);
                        nodePrice.setItem(BigDecimal.valueOf(1));
                        nodePrice.setPrice(new BigDecimal(price));
                        nodePrice.setStatus(0);
                        nodePrice.setCreateTime(new Date());
                        nodePrice.setUpdateTime(new Date());
                        nodePriceMapper.insertSelective(nodePrice);

                    }

                }

            }

        }


    }


}
