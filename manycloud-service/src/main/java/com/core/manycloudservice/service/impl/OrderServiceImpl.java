package com.core.manycloudservice.service.impl;

import com.core.manycloudcommon.caller.BaseCaller;
import com.core.manycloudcommon.caller.UcloudCaller;
import com.core.manycloudcommon.caller.so.CreateSO;
import com.core.manycloudcommon.caller.vo.CreateVO;
import com.core.manycloudcommon.caller.vo.RenewVO;
import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.entity.TimerTask;
import com.core.manycloudcommon.enums.MainEnum;
import com.core.manycloudcommon.enums.PlatformLabelEnum;
import com.core.manycloudcommon.enums.SyslogTypeEnum;
import com.core.manycloudcommon.enums.TaskTypeEnum;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.model.SyslogModel;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.DateUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.utils.StringUtils;
import com.core.manycloudcommon.vo.order.ShoppingListVO;
import com.core.manycloudservice.service.FinanceService;
import com.core.manycloudservice.service.OrderService;
import com.core.manycloudservice.so.order.*;
import com.core.manycloudservice.util.WeiXinCaller;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderInfoMapper orderInfoMapper;

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
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserFinanceMapper userFinanceMapper;

    @Autowired
    private PlatformAccountMapper platformAccountMapper;

    @Autowired
    private InstanceInfoMapper instanceInfoMapper;

    @Autowired
    private BalanceLogMapper balanceLogMapper;

    @Autowired
    private PlatformInfoMapper platformInfoMapper;

    @Autowired
    private TimerTaskMapper timerTaskMapper;

    @Autowired
    private FinanceDetailMapper financeDetailMapper;

    @Autowired
    private FinanceService financeService;

    @Autowired
    private LevelInfoMapper levelInfoMapper;

    @Autowired
    private SysLogMapper sysLogMapper;

    @Autowired
    private WeiXinCaller weiXinCaller;

    @Autowired
    private PeriodTypeMapper periodTypeMapper;


    /**
     * 查询节点的周期类型
     * @param queryPeriodTypeSelectSO
     * @return
     */
    public ResultMessage queryPeriodTypeSelect(QueryPeriodTypeSelectSO queryPeriodTypeSelectSO){
        List<PeriodType>  list = periodTypeMapper.selectByNodeId(queryPeriodTypeSelectSO.getNodeId());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
    }


    /**
     * 查询购买价格
     * @param orderSO
     * @return
     */
    public BigDecimal queryOrderPrice(OrderSO orderSO){
        /** 配置数据 **/
        NodeModel model;
        if(orderSO.getModelId() == null){
            model = nodeModelMapper.selectByConfig(orderSO.getNodeId(),orderSO.getCpu(),orderSO.getRam());
        }else{
            model = nodeModelMapper.selectByPrimaryKey(orderSO.getModelId());
        }

        /** 磁盘数据 **/
        NodeDisk nodeDisk;
        if("Y".equals(model.getRegular())){
            nodeDisk = nodeDiskMapper.selectByNode(orderSO.getNodeId(),model.getId());
        }else{
            nodeDisk = nodeDiskMapper.selectByNode(orderSO.getNodeId(),null);
        }
        /** 带宽数据 **/
        NodeNetwork bandwidth;
        /** 流量数据 **/
        NodeNetwork flow;

        if("Y".equals(model.getRegular())){
            bandwidth = nodeNetworkMapper.selectByNode(orderSO.getNodeId(),0,model.getId());
            flow = nodeNetworkMapper.selectByNode(orderSO.getNodeId(),1,model.getId());
        }else{
            bandwidth = nodeNetworkMapper.selectByNode(orderSO.getNodeId(),0,null);
            flow = nodeNetworkMapper.selectByNode(orderSO.getNodeId(),1,null);
        }

        /** 购买价格 **/
        BigDecimal totalPrice = BigDecimal.valueOf(0);

        /** 计算基础配置价格 **/
        NodePrice modelPrice = nodePriceMapper.selectConfigPrice(orderSO.getNodeId(),"model",model.getId(),orderSO.getPeriod());
        if(modelPrice != null){
            totalPrice = totalPrice.add(modelPrice.getPrice());
        }


        /** 计算磁盘配置价格 **/
        NodePrice diskPrice = nodePriceMapper.selectConfigPrice(orderSO.getNodeId(),"disk",nodeDisk.getId(),orderSO.getPeriod());
        if(diskPrice != null){
            Integer diskNum = orderSO.getSysDisk().subtract(nodeDisk.getGiveNum()).intValue();
            if(orderSO.getDataDisk() != null){
                diskNum = diskNum + orderSO.getDataDisk().intValue();
            }
            Integer diskItem = diskPrice.getItem().intValue();
            BigDecimal singleDiskPrice = diskPrice.getPrice();

            BigDecimal p = diskNum == 0 || diskItem == 0? BigDecimal.valueOf(0) : BigDecimal.valueOf(diskNum).divide( BigDecimal.valueOf(diskItem),2,BigDecimal.ROUND_DOWN);
            totalPrice =  totalPrice.add(p.multiply(singleDiskPrice));
        }

        /** 计算带宽默认配置价格 **/
        NodePrice bandwidthPrice = nodePriceMapper.selectConfigPrice(orderSO.getNodeId(),"network",bandwidth.getId(),orderSO.getPeriod());
        if(bandwidthPrice != null){
            Integer bandwidthNum = orderSO.getBandwidth().intValue();
            Integer bandwidthItem = bandwidthPrice.getItem().intValue();
            BigDecimal singleBandwidthPrice = bandwidthPrice.getPrice();
            BigDecimal p = bandwidthNum == 0 || bandwidthItem == 0? BigDecimal.valueOf(0) : BigDecimal.valueOf(bandwidthNum).divide( BigDecimal.valueOf(bandwidthItem),2,BigDecimal.ROUND_DOWN);
            totalPrice =  totalPrice.add(p.multiply(singleBandwidthPrice));
        }


        /** 计算流量默认配置价格 **/
        NodePrice flowPrice = nodePriceMapper.selectConfigPrice(orderSO.getNodeId(),"network",flow.getId(),orderSO.getPeriod());
        if(flowPrice != null && orderSO.getFlow() != null){
            Integer flowNum = orderSO.getFlow().intValue();
            Integer flowItem = flowPrice.getItem().intValue();
            BigDecimal singleFlowPrice = flowPrice.getPrice();
            BigDecimal p = flowNum == 0 || flowItem == 0? BigDecimal.valueOf(0) : BigDecimal.valueOf(flowNum).divide( BigDecimal.valueOf(flowItem),2,BigDecimal.ROUND_DOWN);
            totalPrice =  totalPrice.add(p.multiply(singleFlowPrice));
        }

        /** 计算当前用户折扣 **/
        if(StringUtils.isNotEmpty(orderSO.getUserId())){
            /** 获取用户当前VIP等级 **/
            LevelInfo levelInfo = levelInfoMapper.selectByUser(orderSO.getUserId());
            if(levelInfo != null){
                BigDecimal discountPrice = totalPrice.multiply(levelInfo.getDiscount().divide(BigDecimal.valueOf(100))).setScale(2,BigDecimal.ROUND_UP);

                totalPrice = totalPrice.subtract(discountPrice);
            }
        }


        /** 单价 乘以 数量  乘以时长 **/
        totalPrice = totalPrice.multiply(BigDecimal.valueOf(orderSO.getNum())).multiply(BigDecimal.valueOf(orderSO.getDuration()));

        return totalPrice;
    }


    /**
     * 添加订单
     * @param userId
     * @param orderSO
     * @return
     */
    public ResultMessage add(String userId,OrderSO orderSO){
        orderSO.setUserId(userId);
        /** 获取订单价格 **/
        BigDecimal price = queryOrderPrice(orderSO);

        if(price.compareTo(BigDecimal.valueOf(0)) < 1){
            return new ResultMessage(ResultMessage.FAILED_CODE,"无效金额，请尝试刷新页面！");
        }

        /** 配置数据 **/
        NodeModel model = nodeModelMapper.selectByPrimaryKey(orderSO.getModelId());
        NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(orderSO.getNodeId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(CommonUtil.getOnlyNo(MainEnum.ORDER));
        orderInfo.setUserId(userId);
        orderInfo.setNum(orderSO.getNum());
        orderInfo.setPeriod(orderSO.getPeriod());
        orderInfo.setDuration(orderSO.getDuration());
        orderInfo.setModelId(model.getId());
        orderInfo.setPrice(price);
        orderInfo.setOnlyPrice(price.divide(BigDecimal.valueOf(orderSO.getNum()),2,BigDecimal.ROUND_DOWN));
        orderInfo.setDiscount(BigDecimal.valueOf(0));//折扣
        orderInfo.setType(0);//默认普通类型
        orderInfo.setNodeId(orderSO.getNodeId());
        orderInfo.setLabel(nodeInfo.getLabel());
        orderInfo.setCpu(orderSO.getCpu());
        orderInfo.setRam(orderSO.getRam());
        orderInfo.setSysDisk(orderSO.getSysDisk());
        orderInfo.setDataDisk(orderSO.getDataDisk());
        orderInfo.setBandwidth(orderSO.getBandwidth());
        orderInfo.setFlow(orderSO.getFlow());
        orderInfo.setImage(orderSO.getImage());
        orderInfo.setImageId(orderSO.getImageId());

        orderInfo.setStatus(0);// 待支付状态
        orderInfo.setCreateTime(new Date());
        orderInfo.setUpdateTime(new Date());

        int i = orderInfoMapper.insertSelective(orderInfo);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }

    }


    /***
     * 查询购物车列表
     * @param queryShoppingListSO
     * @return
     */
    public ResultMessage queryShoppingList(String userId,QueryShoppingListSO queryShoppingListSO){
        PageHelper.startPage(queryShoppingListSO.getPage(), queryShoppingListSO.getPageSize());
        Page<ShoppingListVO> page = (Page<ShoppingListVO>)orderInfoMapper.selectShoppingList(userId);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }

    /**
     * 查询购物车订单详细
     * @param queryShoppingDetailSO
     * @return
     */
    public ResultMessage queryShoppingDetail(QueryShoppingDetailSO queryShoppingDetailSO){

        OrderInfo orderInfo = orderInfoMapper.selectByNo(queryShoppingDetailSO.getOrderNo());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,orderInfo);
    }


    /**
     * 删除购物车信息
     * @param delShoppingSO
     * @return
     */
    public ResultMessage delShopping(DelShoppingSO delShoppingSO){

        String[] idStr = delShoppingSO.getIds().split(",");
        List<Integer> idList = new ArrayList<>();
        for(String str : idStr){
            if(StringUtils.isNotEmpty(str)){
                idList.add(Integer.parseInt(str));
            }
        }

        int i = 0;
        if(idList.size() > 0){
            i = orderInfoMapper.deleteBatch(idList);
        }

        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"操作成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,ResultMessage.FAILED_MSG);
        }
    }

    /**
     * 结算订单(购买)
     * @param orderNos
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage buy(String userId, List<String> orderNos,BigDecimal amount){

        UserFinance uf = userFinanceMapper.selectByUserId(userId);
        if(uf.getValidNum().compareTo(amount) < 0){
            return new ResultMessage(ResultMessage.FAILED_CODE,"余额不足");
        }

        BigDecimal totalAmount = BigDecimal.valueOf(0);
        int totalNum = 0;
        int failNum = 0;

        for(String orderNo : orderNos){

            OrderInfo orderInfo = orderInfoMapper.selectByNo(orderNo);
            NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(orderInfo.getNodeId());
            totalNum = totalNum + orderInfo.getNum();
            List<InstanceInfo> instanceInfoList = new ArrayList<>();

            for(int i = 0 ; i < orderInfo.getNum() ; i++){

                InstanceInfo instanceInfo = new InstanceInfo();
                String instanceId = CommonUtil.getOnlyNo(MainEnum.MAIN);
                instanceInfo.setInstanceId(instanceId);
                instanceInfo.setOrderNo(orderInfo.getOrderNo());
                instanceInfo.setUserId(orderInfo.getUserId());
                instanceInfo.setType(0);
                instanceInfo.setNodeId(orderInfo.getNodeId());
                instanceInfo.setLabel(orderInfo.getLabel());
                PlatformAccount platformAccount = platformAccountMapper.selectDefault(orderInfo.getLabel());
                instanceInfo.setAccountId(platformAccount.getId());
                instanceInfo.setModelId(orderInfo.getModelId());
                instanceInfo.setCpu(orderInfo.getCpu());
                instanceInfo.setRam(orderInfo.getRam());
                instanceInfo.setConnectPwd(CommonUtil.getConnectPwd(PlatformLabelEnum.getByLabel(orderInfo.getLabel())));
                instanceInfo.setSysDisk(orderInfo.getSysDisk());
                instanceInfo.setDataDisk(orderInfo.getDataDisk());
                instanceInfo.setBandwidth(orderInfo.getBandwidth());
                instanceInfo.setFlow(orderInfo.getFlow());
                instanceInfo.setImage(orderInfo.getImage());
                instanceInfo.setImageId(orderInfo.getImageId());
                instanceInfo.setPeriod(orderInfo.getPeriod());
                instanceInfo.setStatus(0);
                instanceInfo.setCreateTime(new Date());
                Date endTime = null;
                if(orderInfo.getPeriod() == 0){//按天购买
                    endTime = DateUtil.addDateDays(new Date(),orderInfo.getDuration());
                }else if(orderInfo.getPeriod() == 1){//按月购买
                    endTime = DateUtil.daysBeMonth(new Date(),orderInfo.getDuration());
                }else if(orderInfo.getPeriod() == 2){//按固定月购买(30天)
                    endTime = DateUtil.addDateDays(new Date(),(orderInfo.getDuration() * 30));

                }
                instanceInfo.setEndTime(endTime);
                instanceInfo.setUpdateTime(new Date());
                int r = instanceInfoMapper.insertSelective(instanceInfo);
                if(r > 0){
                    instanceInfoList.add(instanceInfo);

                    /** 累计创建主机请求成功-冻结金额 **/
                    totalAmount = totalAmount.add(orderInfo.getOnlyPrice());

                    /** 添加财务账单明细 **/
                    FinanceDetail financeDetail = new FinanceDetail();
                    financeDetail.setUserId(orderInfo.getUserId());
                    financeDetail.setFinanceNo(CommonUtil.getRandomStr(12));
                    financeDetail.setProductNo(instanceInfo.getInstanceId());
                    financeDetail.setType(1);//消费类型
                    financeDetail.setMoneyNum(orderInfo.getOnlyPrice());
                    financeDetail.setPeriod(orderInfo.getPeriod());
                    financeDetail.setTag("buy");//购买
                    financeDetail.setDirection(1);//支出
                    financeDetail.setWay(2);//交易方式(0:支付宝,1:微信,2:账号余额)
                    financeDetail.setStatus(0);//未完成
                    financeDetail.setCreateTime(new Date());
                    financeDetail.setUpdateTime(new Date());
                    financeDetailMapper.insertSelective(financeDetail);

                }else{

                    /** 累计创建主机请求失败-主机数量 **/
                    failNum ++;
                    instanceInfo.setStatus(7);//创建失败状态
                    instanceInfo.setUpdateTime(new Date());
                    instanceInfoMapper.updateByPrimaryKeySelective(instanceInfo);

                }


            }
            boolean bl = false;
            //创建主机
            Map<String,Boolean> result = createInstance(instanceInfoList);
            for(InstanceInfo instanceInfo : instanceInfoList){
                boolean rs = result.get(instanceInfo.getInstanceId());
                if(rs){
                    bl = true;
                }else{
                    /** 累计创建主机请求失败-主机数量 **/
                    failNum += 1;
                    /** 减少创建主机请求成功-冻结金额 **/
                    totalAmount = totalAmount.subtract(orderInfo.getOnlyPrice());
                    instanceInfoMapper.deleteByPrimaryKey(instanceInfo.getId());
                }
            }
            if(bl){
                orderInfo.setStatus(2);//交付中状态
                orderInfo.setUpdateTime(new Date());
                orderInfoMapper.updateByPrimaryKeySelective(orderInfo);
            }

        }

        if(totalNum > failNum){ //下单总数量 大于 失败数量
            /** 冻结金额 **/
            int i = userFinanceMapper.updateBalanceByUserId(userId,"seal",totalAmount);
            if(i > 0){
                //用户余额更新记录
                balanceLogMapper.insertChange(userId,"seal",totalAmount,uf.getValidNum(),"下单成功冻结金额");
                if(failNum > 0){
                    return new ResultMessage(ResultMessage.SUCCEED_CODE,"下单成功："+(totalNum - failNum)+" 台,扣除金额："+totalAmount.toPlainString());
                }else{
                    return new ResultMessage(ResultMessage.SUCCEED_CODE,"下单成功");
                }
            }else{
                return new ResultMessage(ResultMessage.SUCCEED_CODE,"余额异常");
            }
        }else{
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"下单失败");
        }

    }


    /***
     * 创建实例入口
     * @param instanceInfoList
     */
    public Map<String,Boolean> createInstance(List<InstanceInfo> instanceInfoList){
        Map<String,Boolean> result = new HashMap<>();

        for(InstanceInfo instanceInfo : instanceInfoList){

            try{
                /** 节点可用区 **/
                NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(instanceInfo.getNodeId());
                /** 平台信息 **/
                PlatformInfo platformInfo = platformInfoMapper.selectByLabel(instanceInfo.getLabel());
                /** 平台账号 **/
                PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(instanceInfo.getAccountId());

                /** 基础数据 **/
                NodeModel nodeModel = nodeModelMapper.selectByPrimaryKey(instanceInfo.getModelId());
                NodeImage nodeImage = nodeImageMapper.selectByPrimaryKey(instanceInfo.getImageId());
                OrderInfo orderInfo = orderInfoMapper.selectByNo(instanceInfo.getOrderNo());
                /** 磁盘数据 **/
                NodeDisk nodeDisk;
                /** 带宽数据 **/
                NodeNetwork bandwidth;
                /** 流量数据 **/
                NodeNetwork flow;
                if("Y".equals(nodeModel.getRegular())){
                    nodeDisk = nodeDiskMapper.selectByNode(nodeInfo.getId(),nodeModel.getId());
                    bandwidth = nodeNetworkMapper.selectByNode(nodeInfo.getId(),0,nodeModel.getId());
                    flow = nodeNetworkMapper.selectByNode(nodeInfo.getId(),1,nodeModel.getId());
                }else{
                    nodeDisk = nodeDiskMapper.selectByNode(nodeInfo.getId(),null);
                    bandwidth = nodeNetworkMapper.selectByNode(nodeInfo.getId(),0,null);
                    flow = nodeNetworkMapper.selectByNode(nodeInfo.getId(),1,null);
                }

                //项目ID
                String projectId = null;
                if(StringUtils.isNotEmpty(nodeInfo.getNodeParam())){
                    JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
                    projectId = param.get("projectId") == null ? null:param.getString("projectId");
                }
                String zone = null;
                if(StringUtils.isNotEmpty(nodeInfo.getNodeParam())){
                    JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
                    zone = param.get("zone") == null ? null:param.getString("zone");
                }
                String machineType = null;
                if(StringUtils.isNotEmpty(nodeInfo.getNodeParam())){
                    JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
                    machineType = param.get("machineType") == null ? null:param.getString("machineType");
                }

                String securityGroupId = null;
                if(StringUtils.isNotEmpty(nodeInfo.getNodeParam())){
                    JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
                    securityGroupId = param.get("securityGroupId") == null ? null:param.getString("securityGroupId");
                }

                AccountApi accountApi = AccountApi.builder()
                        .regionId(nodeInfo.getNodeVal())
                        .label(instanceInfo.getLabel())
                        .account(platformAccount.getAccount())
                        .keyNo(platformAccount.getKeyNo())
                        .keySecret(platformAccount.getKeySecret())
                        .baseUrl(platformAccount.getUrl())
                        .projectId(projectId)
                        .build();
                //获取客户端
                BaseCaller caller = BaseCaller.getCaller(accountApi);

                CreateSO createSO = CreateSO.builder()
                        .pwd(instanceInfo.getConnectPwd())
                        .bundleId(nodeModel.getModelParam())
                        .imageId(nodeImage.getImageParam())
                        .period(orderInfo.getDuration())
                        .num(1)
                        .disksType(nodeDisk.getDiskType())
                        .disksSize(orderInfo.getSysDisk().intValue())
                        .cpu(nodeModel.getCpuVal())
                        .ram(nodeModel.getRamVal())
                        .zone(zone)
                        .machineType(machineType)
                        .securityGroupId(securityGroupId)
                        .build();
                CreateVO createVO = caller.create(createSO);
                if(CommonUtil.SUCCESS_CODE.equals(createVO.getCode())){
                    String serviceNo = createVO.getInstanceIds().get(0);

                    InstanceInfo ist = new InstanceInfo();
                    ist.setId(instanceInfo.getId());
                    ist.setServiceNo(serviceNo);
                    ist.setStatus(1);//实例创建中
                    ist.setUpdateTime(new Date());
                    instanceInfoMapper.updateByPrimaryKeySelective(ist);

                    TimerTask timerTask = new TimerTask();
                    timerTask.setLabel(instanceInfo.getLabel());
                    timerTask.setTaskNo(instanceInfo.getInstanceId());//实例ID
                    timerTask.setOrderNo(serviceNo);//主机编号
                    timerTask.setType(TaskTypeEnum.AHZ_BUY.getType());//异步创建主机
                    timerTask.setStatus(0);//待处理
                    timerTask.setUpdateTime(new Date());
                    timerTask.setCreateTime(new Date());
                    timerTaskMapper.insertSelective(timerTask);

                    result.put(instanceInfo.getInstanceId(),true);
                }else{
                    result.put(instanceInfo.getInstanceId(),false);
                    log.info("[{}]创建实例失败：{}",instanceInfo.getLabel(),createVO.getMsg());
                }

            }catch (Exception e){
                log.info("订单[{}]结算实例[{}]创建失败------>",instanceInfo.getOrderNo(),instanceInfo.getInstanceId());
                e.printStackTrace();
            }

        }

        return result;
    }

    /***
     * 查询续费价格
     * @param renewSO
     * @return
     */
    public BigDecimal queryRenewPrice(RenewSO renewSO){
        InstanceInfo instanceInfo = instanceInfoMapper.selectById(renewSO.getInstanceId());
        OrderSO orderSO = OrderSO.builder()
                .nodeId(instanceInfo.getNodeId())
                .modelId(instanceInfo.getModelId())
                .sysDisk(instanceInfo.getSysDisk())
                .dataDisk(instanceInfo.getDataDisk())
                .bandwidth(instanceInfo.getBandwidth())
                .flow(instanceInfo.getFlow())
                .num(1)
                .period(instanceInfo.getPeriod())
                .duration(renewSO.getDuration())
                .userId(instanceInfo.getUserId())
                .build();

        /** 获取续费价格 **/
        BigDecimal price = queryOrderPrice(orderSO);
        return price;
    }


    /***
     * 续费
     * @param renewSO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage renew(RenewSO renewSO){
        InstanceInfo instanceInfo = instanceInfoMapper.selectById(renewSO.getInstanceId());

        /** 获取续费价格 **/
        BigDecimal price = queryRenewPrice(renewSO);

        if(price.compareTo(BigDecimal.valueOf(0)) < 1){
            return new ResultMessage(ResultMessage.FAILED_CODE,"无效金额，不支持续费。");
        }

        String userId = instanceInfo.getUserId();
        UserFinance userFinance = userFinanceMapper.selectByUserId(userId);
        if(price.compareTo(userFinance.getValidNum()) > 0){
            return new ResultMessage(ResultMessage.FAILED_CODE,"余额不足");
        }else{
            /** 冻结金额 **/
            int i = userFinanceMapper.updateBalanceByUserId(instanceInfo.getUserId(),"seal",price);
            if(i > 0){
                //用户余额更新记录
                UserFinance uf = userFinanceMapper.selectByUserId(userId);
                balanceLogMapper.insertChange(userId,"seal",price,uf.getValidNum(),"续费冻结金额");
            }
        }

        //获取默认资源平台账号
        PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(instanceInfo.getAccountId());
        /** 平台信息 **/
        PlatformInfo platformInfo = platformInfoMapper.selectByLabel(instanceInfo.getLabel());
        /** 节点可用区 **/
        NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(instanceInfo.getNodeId());
        //项目ID
        String projectId = null;
        if(StringUtils.isNotEmpty(nodeInfo.getNodeParam())){
            JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
            projectId = param.get("projectId") == null ? null:param.getString("projectId");
        }

        AccountApi accountApi = AccountApi.builder()
                .regionId(nodeInfo.getNodeVal())
                .label(instanceInfo.getLabel())
                .account(platformAccount.getAccount())
                .keyNo(platformAccount.getKeyNo())
                .keySecret(platformAccount.getKeySecret())
                .baseUrl(platformAccount.getUrl())
                .projectId(projectId)
                .build();

       BaseCaller caller = BaseCaller.getCaller(accountApi);
        com.core.manycloudcommon.caller.so.RenewSO rs = com.core.manycloudcommon.caller.so.RenewSO.builder()
                .instanceId(instanceInfo.getServiceNo())
                .num(renewSO.getDuration())
                .build();
        try {
            RenewVO renewVO = caller.renew(rs);
            if(CommonUtil.SUCCESS_CODE.equals(renewVO.getCode())){
                /** 扣除金额 **/
                int i = userFinanceMapper.updateBalanceByUserId(instanceInfo.getUserId(),"minus",price);
                if(i > 0){
                    //用户余额更新记录
                    UserFinance uf = userFinanceMapper.selectByUserId(userId);
                    balanceLogMapper.insertChange(userId,"minus",price,uf.getValidNum(),"续费成功扣除金额");

                    OrderInfo orderInfo = orderInfoMapper.selectByNo(instanceInfo.getOrderNo());

                    //计算续费后的到期时间
                    Date newEndTime = null;
                    if(orderInfo.getPeriod() == 0){//按天购买
                        newEndTime = DateUtil.addDateDays(instanceInfo.getEndTime(),renewSO.getDuration());
                    }else if(orderInfo.getPeriod() == 1){//按月购买
                        newEndTime = DateUtil.daysBeMonth(instanceInfo.getEndTime(),renewSO.getDuration());

                    }else if(orderInfo.getPeriod() == 2){//按月购买
                        newEndTime = DateUtil.addDateDays(instanceInfo.getEndTime(),(renewSO.getDuration() * 30));
                    }

                    InstanceInfo entity = new InstanceInfo();
                    entity.setId(instanceInfo.getId());
                    entity.setEndTime(newEndTime);
                    entity.setStatus(3);//使用中状态
                    entity.setUpdateTime(new Date());
                    instanceInfoMapper.updateByPrimaryKeySelective(entity);

                    /** 添加财务账单明细 **/
                    FinanceDetail financeDetail = new FinanceDetail();
                    financeDetail.setUserId(instanceInfo.getUserId());
                    financeDetail.setFinanceNo(CommonUtil.getRandomStr(12));
                    financeDetail.setProductNo(instanceInfo.getInstanceId());
                    financeDetail.setType(1);//消费类型
                    financeDetail.setMoneyNum(price);
                    financeDetail.setPeriod(renewSO.getDuration());
                    financeDetail.setTag("renew");//续费
                    financeDetail.setDirection(1);//支出
                    financeDetail.setWay(2);//交易方式(0:支付宝,1:微信,2:账号余额)
                    financeDetail.setStatus(1);//未完成
                    financeDetail.setCreateTime(new Date());
                    financeDetail.setUpdateTime(new Date());
                    financeDetailMapper.insertSelective(financeDetail);

                    String info = instanceInfo.getNike() == null ? instanceInfo.getPublicIp() : instanceInfo.getPublicIp()+"-"+ instanceInfo.getNike();
                    String endTimeStr = DateUtil.dateStr4(newEndTime);
                    String content = "尊敬的用户，您的实例["+info+"]已经续费成功，最新到期时间为 ["+endTimeStr+"]。";
                    SysLog sysLog = SyslogModel.output(instanceInfo.getUserId(), SyslogTypeEnum.RECHARGE,content);
                    sysLogMapper.insertSelective(sysLog);


                    /** 续费成功公众号推送 **/
                    String productName = instanceInfo.getNike() == null ? instanceInfo.getPublicIp() : instanceInfo.getPublicIp()+"-"+ instanceInfo.getNike();
                    weiXinCaller.sendRenewSuccess(instanceInfo.getUserId(),price,"云主机 - "+productName,newEndTime,renewSO.getDuration());


                    /** 消费推广结算 **/
                    financeService.promotionCount(instanceInfo.getUserId(),instanceInfo.getInstanceId(),price,1);

                }
                return new ResultMessage(ResultMessage.SUCCEED_CODE,"续费成功");
            }else{
                /** 解冻金额 **/
                int i = userFinanceMapper.updateBalanceByUserId(instanceInfo.getUserId(),"unbind",price);
                if(i > 0){
                    //用户余额更新记录
                    UserFinance uf = userFinanceMapper.selectByUserId(userId);
                    balanceLogMapper.insertChange(userId,"unbind",price,uf.getValidNum(),"续费失败解冻金额");
                }
                return new ResultMessage(ResultMessage.SUCCEED_CODE,"续费失败");
            }
        }catch (Exception e){
            e.printStackTrace();
            return new ResultMessage(ResultMessage.FAILED_CODE,"续费错误");
        }
    }

}
