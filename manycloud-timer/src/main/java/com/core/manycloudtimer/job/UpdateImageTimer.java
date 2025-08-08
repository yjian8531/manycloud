//package com.core.manycloudtimer.job;
//
//
//import com.core.manycloudcommon.caller.UcloudCaller;
//import com.core.manycloudcommon.entity.NodeImage;
//import com.core.manycloudcommon.entity.NodeInfo;
//import com.core.manycloudcommon.entity.PlatformAccount;
//import com.core.manycloudcommon.enums.PlatformLabelEnum;
//import com.core.manycloudcommon.mapper.NodeImageMapper;
//import com.core.manycloudcommon.mapper.NodeInfoMapper;
//import com.core.manycloudcommon.mapper.PlatformAccountMapper;
//import com.core.manycloudcommon.model.AccountApi;
//import com.core.manycloudcommon.utils.StringUtils;
//import lombok.extern.slf4j.Slf4j;
//import net.sf.json.JSONArray;
//import net.sf.json.JSONObject;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.EnableScheduling;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Slf4j
//@Component      //1.主要用于标记配置类，兼备Component的效果。
//@EnableScheduling   // 2.开启定时任务
//public class UpdateImageTimer {
//
//
//    @Autowired
//    private NodeInfoMapper nodeInfoMapper;
//
//    @Autowired
//    private NodeImageMapper nodeImageMapper;
//
//    @Autowired
//    private PlatformAccountMapper platformAccountMapper;
//
//
//    @Scheduled(cron = "0 */1 * * * ?")
//    public void listenImageTime(){
//
//        List<NodeInfo> list =  nodeInfoMapper.selectByLabel(PlatformLabelEnum.UCLOUD.getLabel());
//        for(NodeInfo nodeInfo : list){
//            try{
//
//                //获取默认资源平台账号
//                PlatformAccount platformAccount = platformAccountMapper.selectDefault(nodeInfo.getLabel());
//
//                /** 节点可用区 **/
//                //项目ID
//                String projectId = null;
//                if(StringUtils.isNotEmpty(nodeInfo.getNodeParam())){
//                    JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
//                    projectId = param.get("projectId") == null ? null:param.getString("projectId");
//                }
//                AccountApi accountApi = AccountApi.builder()
//                        .regionId(nodeInfo.getNodeVal())
//                        .label(nodeInfo.getLabel())
//                        .account(platformAccount.getAccount())
//                        .keyNo(platformAccount.getKeyNo())
//                        .keySecret(platformAccount.getKeySecret())
//                        .baseUrl(platformAccount.getUrl())
//                        .projectId(projectId)
//                        .build();
//
//                UcloudCaller ucloudCaller = UcloudCaller.getClient(accountApi);
//                String str = ucloudCaller.getImage();
//
//                Map<String,String> linMap = new HashMap<>();
//
//                JSONArray imageList = JSONObject.fromObject(str).getJSONArray("ImageSet");
//                for(Object obj : imageList){
//                    JSONObject image = JSONObject.fromObject(obj);
//                    String version = image.getString("ImageName");
//                    String type= null;
//                    String id = image.getString("ImageId");
//
//                    if(version.toLowerCase().indexOf("Windows".toLowerCase()) == -1){
//                        if(nodeInfo.getNodeName().indexOf("台北") > -1 || nodeInfo.getNodeName().indexOf("东京") > -1 || nodeInfo.getNodeName().indexOf("首尔") > -1){
//                            if(version.indexOf("高内核") > -1){
//                                continue;
//                            }
//                        }else{
//                            if(version.indexOf("高内核") == -1){
//                                continue;
//                            }
//                        }
//                    }
//
//
//                    if(version.toLowerCase().indexOf("Ubuntu".toLowerCase()) > -1){
//                        type = "Ubuntu";
//                        version = version.replace("Ubuntu","").trim();
//                    }else if(version.toLowerCase().indexOf("Debian".toLowerCase()) > -1){
//                        type = "Debian";
//                        version = version.replace("Debian","").trim();
//                    }else if(version.toLowerCase().indexOf("CentOS".toLowerCase()) > -1){
//                        type = "CentOS";
//                        version = version.replace("CentOS","").trim();
//                    }else if(version.toLowerCase().indexOf("Windows".toLowerCase()) > -1){
//                        type = "Windows";
//                        version = version.replace("Windows","").trim();
//                    }
//
//
//
//
//                    if(type != null){
//                        if(type.equals("Windows") || type.equals("Ubuntu") || type.equals("Fedora") || type.equals("Debian") || type.equals("CentOS")){
//                            log.info("{} - {} - {}",type,version,id);
//
//                            NodeImage nodeImage = nodeImageMapper.selectNodeParam(nodeInfo.getId(),id);
//                            if(nodeImage == null){
//                                nodeImage = new NodeImage();
//                                nodeImage.setNodeId(nodeInfo.getId());
//                                nodeImage.setImageType(type);
//                                nodeImage.setImageVersion(version);
//                                nodeImage.setImageParam(id);
//                                if(type.equals("Windows")){
//                                    nodeImage.setStatus(0);
//                                }else{
//                                    nodeImage.setStatus(0);
//                                }
//                                nodeImage.setCreateTime(new Date());
//                                nodeImage.setUpdateTime(new Date());
//                                nodeImageMapper.insertSelective(nodeImage);
//                            }else{
//                                if(nodeImage.getStatus() == 1){
//                                    nodeImage.setStatus(0);
//                                    nodeImage.setUpdateTime(new Date());
//                                    nodeImageMapper.updateByPrimaryKeySelective(nodeImage);
//                                }
//                            }
//
//                        }
//
//                        linMap.put(id,"Y");
//                    }
//
//
//                }
//
//                List<NodeImage> images = nodeImageMapper.selectByNode(nodeInfo.getId());
//                for(NodeImage nodeImage : images){
//                    String val = linMap.get(nodeImage.getImageParam());
//                    if(val == null){
//                        nodeImage.setStatus(1);
//                        nodeImage.setUpdateTime(new Date());
//                        nodeImageMapper.updateByPrimaryKeySelective(nodeImage);
//                    }
//                }
//
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
//
//    }
//
//
//}
