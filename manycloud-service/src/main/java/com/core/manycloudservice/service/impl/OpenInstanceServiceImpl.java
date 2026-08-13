package com.core.manycloudservice.service.impl;

import com.core.manycloudcommon.caller.BaseCaller;
import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.entity.*;
import com.core.manycloudcommon.entity.TimerTask;
import com.core.manycloudcommon.enums.MainEnum;
import com.core.manycloudcommon.enums.PlatformLabelEnum;
import com.core.manycloudcommon.enums.TaskTypeEnum;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.DateUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.utils.StringUtils;
import com.core.manycloudservice.service.InstanceService;
import com.core.manycloudservice.service.OpenInstanceService;
import com.core.manycloudservice.service.OrderService;
import com.core.manycloudservice.so.order.ApiOrderSO;
import com.core.manycloudservice.so.order.MultiRegionOrderSO;
import com.core.manycloudservice.so.order.MultiRegionResultVO;
import com.core.manycloudservice.so.order.OrderSO;
import com.core.manycloudservice.vo.ApiFirewallRuleVO;
import com.core.manycloudservice.vo.ApiQueryFirewallVO;
import com.core.manycloudservice.so.instance.QueryDetailSO;
import com.core.manycloudservice.so.order.RenewSO;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * 特殊用户开放接口实现
 * 下单流程复制自 OrderServiceImpl.add/buy/createInstance（直接购买、无购物车、走余额扣款）
 */
@Slf4j
@Service
public class OpenInstanceServiceImpl implements OpenInstanceService {

    @Autowired
    private OrderService orderService;
    @Autowired
    private InstanceService instanceService;

    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private InstanceInfoMapper instanceInfoMapper;
    @Autowired
    private FinanceDetailMapper financeDetailMapper;
    @Autowired
    private UserFinanceMapper userFinanceMapper;
    @Autowired
    private BalanceLogMapper balanceLogMapper;
    @Autowired
    private PlatformAccountMapper platformAccountMapper;
    @Autowired
    private PlatformInfoMapper platformInfoMapper;
    @Autowired
    private NodeInfoMapper nodeInfoMapper;
    @Autowired
    private NodeModelMapper nodeModelMapper;
    @Autowired
    private NodeImageMapper nodeImageMapper;
    @Autowired
    private NodeDiskMapper nodeDiskMapper;
    @Autowired
    private NodeNetworkMapper nodeNetworkMapper;
    @Autowired
    private TimerTaskMapper timerTaskMapper;


   /**
     * 创建/购买
     * @param userId
     * @param apiOrderSO
     * @return
     */

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResultMessage create(String userId, ApiOrderSO apiOrderSO) {
        try {
            // 1. 参数校验
            if (apiOrderSO.getNodeId() == null || apiOrderSO.getModelId() == null) {
                return new ResultMessage(ResultMessage.FAILED_CODE, "缺少必要参数：nodeId 或 modelId");
            }
            if (StringUtils.isEmpty(apiOrderSO.getImageId())) {
                return new ResultMessage(ResultMessage.FAILED_CODE, "缺少必要参数：imageId");
            }
            if (apiOrderSO.getPeriod() == null || apiOrderSO.getPeriod() < 0) {
                return new ResultMessage(ResultMessage.FAILED_CODE, "参数错误：period 必须大于等于 0");
            }
            if (apiOrderSO.getQuantity() == null || apiOrderSO.getQuantity() < 1) {
                return new ResultMessage(ResultMessage.FAILED_CODE, "参数错误：quantity 必须大于等于 1");
            }

            // 2. 查询配置数据
            NodeModel nodeModel = nodeModelMapper.selectByPrimaryKey(apiOrderSO.getModelId());
            if (nodeModel == null) {
                return new ResultMessage(ResultMessage.FAILED_CODE, "模型配置不存在");
            }

            NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(apiOrderSO.getNodeId());
            if (nodeInfo == null) {
                return new ResultMessage(ResultMessage.FAILED_CODE, "节点配置不存在");
            }

            // 3. 查询磁盘、网络配置
            NodeDisk nodeDisk;
            NodeNetwork bandwidth;
            NodeNetwork flow;
            if ("Y".equals(nodeModel.getRegular())) {
                nodeDisk = nodeDiskMapper.selectByNode(apiOrderSO.getNodeId(), nodeModel.getId());
                bandwidth = nodeNetworkMapper.selectByNode(apiOrderSO.getNodeId(), 0, nodeModel.getId());
                flow = nodeNetworkMapper.selectByNode(apiOrderSO.getNodeId(), 1, nodeModel.getId());
            } else {
                nodeDisk = nodeDiskMapper.selectByNode(apiOrderSO.getNodeId(), null);
                bandwidth = nodeNetworkMapper.selectByNode(apiOrderSO.getNodeId(), 0, null);
                flow = nodeNetworkMapper.selectByNode(apiOrderSO.getNodeId(), 1, null);
            }

            if (nodeDisk == null) return new ResultMessage(ResultMessage.FAILED_CODE, "磁盘配置不存在");
            if (bandwidth == null) return new ResultMessage(ResultMessage.FAILED_CODE, "带宽配置不存在");
            if (flow == null) return new ResultMessage(ResultMessage.FAILED_CODE, "流量配置不存在");

            // 4. 查询镜像
            NodeImage nodeImage = nodeImageMapper.selectByParam(apiOrderSO.getNodeId(), apiOrderSO.getImageId());
            if (nodeImage == null) {
                return new ResultMessage(ResultMessage.FAILED_CODE, "镜像不存在");
            }

            // 5. 获取默认配置
            BigDecimal sysDiskSize = nodeDisk.getGiveNum() != null ? nodeDisk.getGiveNum() : BigDecimal.valueOf(50);
            BigDecimal dataDiskSize = nodeDisk.getMinNum() != null && nodeDisk.getGiveNum() != null
                    ? nodeDisk.getMinNum().subtract(nodeDisk.getGiveNum())
                    : BigDecimal.ZERO;
            if (dataDiskSize.compareTo(BigDecimal.ZERO) <= 0) dataDiskSize = null;

            BigDecimal bandwidthSize = bandwidth.getMinNum() != null ? bandwidth.getMinNum() : BigDecimal.ZERO;
            BigDecimal flowSize = flow.getMinNum() != null ? flow.getMinNum() : BigDecimal.ZERO;

            // 6. 构建OrderSO计算价格
            OrderSO tempOrderSO = OrderSO.builder()
                    .nodeId(apiOrderSO.getNodeId())
                    .modelId(apiOrderSO.getModelId())
                    .cpu(nodeModel.getCpuVal())
                    .ram(nodeModel.getRamVal())
                    .sysDisk(sysDiskSize)
                    .dataDisk(dataDiskSize)
                    .bandwidth(bandwidthSize)
                    .flow(flowSize)
                    .image(nodeImage.getImageVersion())
                    .imageId(nodeImage.getId())
                    .num(apiOrderSO.getQuantity())
                    .period(apiOrderSO.getPeriod())
                    .duration(apiOrderSO.getPeriod())
                    .userId(userId)
                    .build();

            BigDecimal price = orderService.queryOrderPrice(tempOrderSO);
            if(price == null || price.compareTo(BigDecimal.valueOf(0)) < 1){
                return new ResultMessage(ResultMessage.FAILED_CODE, "无效金额");
            }

            // 7. 余额校验
            UserFinance uf = userFinanceMapper.selectByUserId(userId);
            if(uf == null || uf.getValidNum() == null || price.compareTo(uf.getValidNum()) > 0){
                return new ResultMessage(ResultMessage.FAILED_CODE, "余额不足");
            }

            // 8. 创建订单
            OrderInfo orderInfo = new OrderInfo();
            orderInfo.setOrderNo(CommonUtil.getOnlyNo(MainEnum.ORDER));
            orderInfo.setUserId(userId);
            orderInfo.setNum(apiOrderSO.getQuantity());
            orderInfo.setPeriod(apiOrderSO.getPeriod());
            orderInfo.setDuration(apiOrderSO.getPeriod());
            orderInfo.setModelId(apiOrderSO.getModelId());
            orderInfo.setPrice(price);
            orderInfo.setOnlyPrice(price.divide(BigDecimal.valueOf(apiOrderSO.getQuantity()), 2, BigDecimal.ROUND_DOWN));
            orderInfo.setDiscount(BigDecimal.valueOf(0));
            orderInfo.setType(0);
            orderInfo.setNodeId(apiOrderSO.getNodeId());
            orderInfo.setLabel(nodeInfo.getLabel());
            orderInfo.setCpu(nodeModel.getCpuVal());
            orderInfo.setRam(nodeModel.getRamVal());
            orderInfo.setSysDisk(sysDiskSize);
            orderInfo.setDataDisk(dataDiskSize);
            orderInfo.setBandwidth(bandwidthSize);
            orderInfo.setFlow(flowSize);
            orderInfo.setImage(nodeImage.getImageVersion());
            orderInfo.setImageId(nodeImage.getId());
            orderInfo.setStatus(0);
            orderInfo.setCreateTime(new Date());
            orderInfo.setUpdateTime(new Date());
            orderInfoMapper.insertSelective(orderInfo);

            // 9. 创建实例和财务明细
            int totalNum = orderInfo.getNum();
            BigDecimal totalAmount = BigDecimal.valueOf(0);
            int failNum = 0;
            List<InstanceInfo> instanceInfoList = new ArrayList<>();

            for(int i = 0; i < totalNum; i++){
                InstanceInfo instanceInfo = new InstanceInfo();
                instanceInfo.setInstanceId(CommonUtil.getOnlyNo(MainEnum.MAIN));
                instanceInfo.setOrderNo(orderInfo.getOrderNo());
                instanceInfo.setUserId(userId);
                instanceInfo.setType(0);
                instanceInfo.setNodeId(apiOrderSO.getNodeId());
                instanceInfo.setLabel(orderInfo.getLabel());
                // 特殊用户使用del=2的特殊账号
                PlatformAccount platformAccount = platformAccountMapper.selectByLabelAndDel(orderInfo.getLabel(), 2);
                instanceInfo.setAccountId(platformAccount.getId());
                instanceInfo.setModelId(apiOrderSO.getModelId());
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
                instanceInfo.setEndTime(calcEndTime(orderInfo));
                instanceInfo.setUpdateTime(new Date());
                int r = instanceInfoMapper.insertSelective(instanceInfo);
                if(r > 0){
                    instanceInfoList.add(instanceInfo);
                    totalAmount = totalAmount.add(orderInfo.getOnlyPrice());

                    FinanceDetail financeDetail = new FinanceDetail();
                    financeDetail.setUserId(userId);
                    financeDetail.setFinanceNo(CommonUtil.getRandomStr(12));
                    financeDetail.setProductNo(instanceInfo.getInstanceId());
                    financeDetail.setType(1);
                    financeDetail.setMoneyNum(orderInfo.getOnlyPrice());
                    financeDetail.setPeriod(orderInfo.getPeriod());
                    financeDetail.setTag("buy");
                    financeDetail.setDirection(1);
                    financeDetail.setWay(2);
                    financeDetail.setStatus(0);
                    financeDetail.setCreateTime(new Date());
                    financeDetail.setUpdateTime(new Date());
                    financeDetailMapper.insertSelective(financeDetail);
                }else{
                    failNum++;
                }
            }

            // 10. 调用云厂商API
            Map<String, Boolean> result = doCreateInstance(instanceInfoList);
            for(InstanceInfo instanceInfo : instanceInfoList){
                Boolean rs = result.get(instanceInfo.getInstanceId());
                if(rs != null && rs){
                    // 成功
                }else{
                    failNum += 1;
                    totalAmount = totalAmount.subtract(orderInfo.getOnlyPrice());
                    instanceInfoMapper.deleteByPrimaryKey(instanceInfo.getId());
                }
            }

            // 11. 更新订单状态和冻结金额
            if(totalNum > failNum){
                orderInfo.setStatus(2);
                orderInfo.setUpdateTime(new Date());
                orderInfoMapper.updateByPrimaryKeySelective(orderInfo);

                int i = userFinanceMapper.updateBalanceByUserId(userId, "seal", totalAmount);
                if(i > 0){
                    UserFinance latest = userFinanceMapper.selectByUserId(userId);
                    balanceLogMapper.insertChange(userId, "seal", totalAmount, latest.getValidNum(), "下单冻结金额");
                    Map<String, Object> data = new HashMap<>();
                    data.put("orderNo", orderInfo.getOrderNo());
                    data.put("successNum", totalNum - failNum);
                    data.put("amount", totalAmount.toPlainString());
                    return new ResultMessage(ResultMessage.SUCCEED_CODE, "下单成功", data);
                } else {
                    // 扣款失败（余额不足），抛出异常让事务回滚
                    throw new RuntimeException("余额不足，下单失败");
                }
            } else {
                return new ResultMessage(ResultMessage.FAILED_CODE, "下单失败");
            }

        } catch (Exception e) {
            log.error("[开放接口]下单异常：{}", e.getMessage(), e);
            return new ResultMessage(ResultMessage.FAILED_CODE, "平台下单失败");
        }
    }

    /** 复制自 OrderServiceImpl.createInstance：调云厂商开户 + 回填serviceNo + 写轮询任务 **/
    private Map<String, Boolean> doCreateInstance(List<InstanceInfo> instanceInfoList){
        Map<String, Boolean> result = new HashMap<>();
        for(InstanceInfo instanceInfo : instanceInfoList){
            try{
                NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(instanceInfo.getNodeId());
                PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(instanceInfo.getAccountId());
                NodeModel nodeModel = nodeModelMapper.selectByPrimaryKey(instanceInfo.getModelId());

                // 查询镜像配置获取云平台镜像值
                NodeImage nodeImage = null;
                if(instanceInfo.getImageId() != null){
                    nodeImage = nodeImageMapper.selectByPrimaryKey(instanceInfo.getImageId());
                }
                if(nodeImage == null){
                    log.error("实例{}镜像配置不存在，imageId：{}", instanceInfo.getInstanceId(), instanceInfo.getImageId());
                    result.put(instanceInfo.getInstanceId(), false);
                    continue;
                }

                OrderInfo orderInfo = orderInfoMapper.selectByNo(instanceInfo.getOrderNo());

                NodeDisk nodeDisk;
                if("Y".equals(nodeModel.getRegular())){
                    nodeDisk = nodeDiskMapper.selectByNode(nodeInfo.getId(), nodeModel.getId());
                }else{
                    nodeDisk = nodeDiskMapper.selectByNode(nodeInfo.getId(), null);
                }

                String projectId = null, zone = null, machineType = null, securityGroupId = null;
                if(StringUtils.isNotEmpty(nodeInfo.getNodeParam())){
                    JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
                    projectId = param.get("projectId") == null ? null : param.getString("projectId");
                    zone = param.get("zone") == null ? null : param.getString("zone");
                    machineType = param.get("machineType") == null ? null : param.getString("machineType");
                    securityGroupId = param.get("securityGroupId") == null ? null : param.getString("securityGroupId");
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

                CreateSO createSO = CreateSO.builder()
                        .pwd(instanceInfo.getConnectPwd())
                        .bundleId(nodeModel.getModelParam())
                        .imageId(nodeImage.getImageParam())  // 使用查询到的云平台镜像值
                        .period(orderInfo.getDuration())
                        .num(1)
                        .disksType(nodeDisk.getDiskType())
                        .disksSize(orderInfo.getSysDisk().intValue())
                        .cpu(nodeModel.getCpuVal())
                        .ram(nodeModel.getRamVal())
                        .zone(zone)
                        .machineType(machineType)
                        .securityGroupId(securityGroupId)
                        // AWS Lightsail特殊用户标识：跳过EIP创建
                        .awsSpecialFlag("AWSLS".equals(instanceInfo.getLabel()) ? 1 : null)
                        .build();
                CreateVO createVO = caller.create(createSO);

                if(CommonUtil.SUCCESS_CODE.equals(createVO.getCode())){
                    String serviceNo = createVO.getInstanceIds().get(0);
                    InstanceInfo ist = new InstanceInfo();
                    ist.setId(instanceInfo.getId());
                    ist.setServiceNo(serviceNo);
                    ist.setStatus(1); // 创建中
                    ist.setUpdateTime(new Date());
                    instanceInfoMapper.updateByPrimaryKeySelective(ist);

//                    com.core.manycloudcommon.entity.TimerTask timerTask = new com.core.manycloudcommon.entity.TimerTask();
                    TimerTask timerTask = new TimerTask();
                    timerTask.setLabel(instanceInfo.getLabel());
                    timerTask.setTaskNo(instanceInfo.getInstanceId());
                    timerTask.setOrderNo(serviceNo);
                    timerTask.setType(TaskTypeEnum.AHZ_BUY.getType());
                    timerTask.setStatus(0);
                    timerTask.setUpdateTime(new Date());
                    timerTask.setCreateTime(new Date());
                    timerTaskMapper.insertSelective(timerTask);

                    result.put(instanceInfo.getInstanceId(), true);
                }else{
                    result.put(instanceInfo.getInstanceId(), false);
                    log.info("创建实例失败：{}", instanceInfo.getLabel(), createVO.getMsg());
                }
            }catch (Exception e){
                log.info("订单[{}]实例[{}]创建异常：{}", instanceInfo.getOrderNo(), instanceInfo.getInstanceId(), e.getMessage());
                result.put(instanceInfo.getInstanceId(), false);
            }
        }
        return result;
    }


    //查询操作

    @Override
    public ResultMessage detail(String userId, String instanceId) {
        InstanceInfo info = checkOwnership(userId, instanceId);
        if(info == null){
            return new ResultMessage(ResultMessage.FAILED_CODE, "主机不存在或无权访问");
        }
        // 复用现有详情逻辑
        QueryDetailSO so = new QueryDetailSO();
        so.setInstanceId(instanceId);
        return instanceService.queryDetail(so);
    }

    @Override
    public ResultMessage status(String userId, String instanceId) {
        InstanceInfo info = checkOwnership(userId, instanceId);
        if(info == null){
            return new ResultMessage(ResultMessage.FAILED_CODE, "主机不存在或无权访问");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("instanceId", info.getInstanceId());
        data.put("serviceNo", info.getServiceNo());
        data.put("status", info.getStatus());          // 0待创建 1创建中 3使用中 7失败
        data.put("statusText", statusText(info.getStatus()));
        data.put("powerState", info.getPowerState());
        data.put("publicIp", info.getPublicIp());
        data.put("label", info.getLabel());
        return new ResultMessage(ResultMessage.SUCCEED_CODE, ResultMessage.SUCCEED_MSG, data);
    }

    @Override
    public ResultMessage destroy(String userId, String instanceId) {
        InstanceInfo info = checkOwnership(userId, instanceId);
        if(info == null){
            return new ResultMessage(ResultMessage.FAILED_CODE, "主机不存在或无权访问");
        }
        try{
            BaseCaller caller = buildCaller(info);
            DestroyVO vo = caller.destroy(DestroySO.builder().instanceId(info.getServiceNo()).build());
            if(CommonUtil.SUCCESS_CODE.equals(vo.getCode())){
                info.setStatus(6); // 已销毁
                info.setUpdateTime(new Date());
                instanceInfoMapper.updateByPrimaryKeySelective(info);
                return new ResultMessage(ResultMessage.SUCCEED_CODE, "销毁成功");
            }
            return new ResultMessage(ResultMessage.FAILED_CODE, "销毁失败：" + vo.getMsg());
        }catch (Exception e){
            log.info("[开放接口]销毁[{}]异常：{}", instanceId, e.getMessage());
            return new ResultMessage(ResultMessage.FAILED_CODE, "销毁异常");
        }
    }

    @Override
    public ResultMessage renew(String userId, RenewSO renewSO) {
        InstanceInfo info = checkOwnership(userId, renewSO.getInstanceId());
        if(info == null){
            return new ResultMessage(ResultMessage.FAILED_CODE, "主机不存在或无权访问");
        }
        // 复用现有续费流程（内部按 instanceInfo.userId 处理余额，与特殊用户一致）
        return orderService.renew(renewSO);
    }

    @Override
    public ResultMessage createFirewall(String userId, CreateSecuritySO createSecuritySO) {
        InstanceInfo info = checkOwnership(userId, createSecuritySO.getInstanceId());
        if(info == null){
            return new ResultMessage(ResultMessage.FAILED_CODE, "主机不存在或无权访问");
        }
        try{
            BaseCaller caller = buildCaller(info);
            // 将系统内部ID替换为真实的云平台实例ID（serviceNo）
            createSecuritySO.setInstanceId(info.getServiceNo());
            CreateSecurityVO vo = caller.createFirewallTo(createSecuritySO);
            if(vo != null && CommonUtil.SUCCESS_CODE.equals(vo.getCode())){
                Map<String, Object> data = new HashMap<>();
                data.put("fwId", vo.getFwId());
                return new ResultMessage(ResultMessage.SUCCEED_CODE, "安全组规则创建成功", data);
            }
            return new ResultMessage(ResultMessage.FAILED_CODE, "安全组规则创建失败");
        }catch (Exception e){
            log.info("[开放接口]创建安全组[{}]异常：{}", createSecuritySO.getInstanceId(), e.getMessage());
            return new ResultMessage(ResultMessage.FAILED_CODE, "安全组规则创建异常");
        }
    }

    @Override
    public ResultMessage queryFirewall(String userId, QueryFirewallSO queryFirewallSO) {
        // 优先使用 instanceId 字段，其次尝试 fwId 或 name
        String instanceId = StringUtils.isNotEmpty(queryFirewallSO.getInstanceId())
                ? queryFirewallSO.getInstanceId()
                : (StringUtils.isNotEmpty(queryFirewallSO.getFwId())
                        ? queryFirewallSO.getFwId()
                        : queryFirewallSO.getName());

        InstanceInfo info = checkOwnership(userId, instanceId);
        if(info == null){
            return new ResultMessage(ResultMessage.FAILED_CODE, "主机不存在或无权访问");
        }
        try{
            BaseCaller caller = buildCaller(info);
            // 将真实的云平台实例ID设置回 queryFirewallSO，供底层 caller 使用
            queryFirewallSO.setFwId(info.getServiceNo());
            QueryFirewallVO callerVo = caller.queryFirewall(queryFirewallSO);

            // 转换为友好的API响应格式
            ApiQueryFirewallVO apiVo = convertToApiFormat(callerVo, instanceId);
            return new ResultMessage(ResultMessage.SUCCEED_CODE, ResultMessage.SUCCEED_MSG, apiVo);
        }catch (Exception e){
            log.info("[开放接口]查询安全组[{}]异常：{}", instanceId, e.getMessage());
            return new ResultMessage(ResultMessage.FAILED_CODE, "安全组规则查询异常");
        }
    }


    // ============================== 工具 ==============================

    /** 校验主机归属：存在且属于该特殊用户才返回，否则 null */
    private InstanceInfo checkOwnership(String userId, String instanceId){
        if(StringUtils.isEmpty(instanceId)){
            return null;
        }
        InstanceInfo info = instanceInfoMapper.selectById(instanceId);
        if(info == null || !userId.equals(info.getUserId())){
            return null;
        }
        return info;
    }

    /** 由实例信息构建对应平台的 Caller（复制自 InstanceServiceImpl 的通用模式） */
    private BaseCaller buildCaller(InstanceInfo info) {
        PlatformAccount platformAccount = platformAccountMapper.selectByPrimaryKey(info.getAccountId());
        NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(info.getNodeId());
        String projectId = null;
        if(StringUtils.isNotEmpty(nodeInfo.getNodeParam())){
            JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
            projectId = param.get("projectId") == null ? null : param.getString("projectId");
        }
        AccountApi accountApi = AccountApi.builder()
                .regionId(nodeInfo.getNodeVal())
                .label(info.getLabel())
                .account(platformAccount.getAccount())
                .keyNo(platformAccount.getKeyNo())
                .keySecret(platformAccount.getKeySecret())
                .baseUrl(platformAccount.getUrl())
                .projectId(projectId)
                .build();
        return BaseCaller.getCaller(accountApi);
    }

    private String statusText(Integer status){
        if(status == null) return "未知";
        switch (status){
            case 0: return "待创建";
            case 1: return "创建中";
            case 3: return "使用中";
            case 4: return "待续费";
            case 5: return "已过期";
            case 6: return "已销毁";
            case 7: return "创建失败";
            default: return "状态(" + status + ")";
        }
    }

    /**
     * 将 QueryFirewallVO 转换为友好的API响应格式
     */
    private ApiQueryFirewallVO convertToApiFormat(QueryFirewallVO callerVo, String instanceId) {
        if (callerVo == null || callerVo.getRules() == null) {
            return ApiQueryFirewallVO.builder()
                    .instanceId(instanceId)
                    .rules(new ArrayList<>())
                    .total(0)
                    .build();
        }

        List<ApiFirewallRuleVO> apiRules = new ArrayList<>();
        for (FirewallRule rule : callerVo.getRules()) {
            // 只转换有实际意义的字段，忽略null值
            ApiFirewallRuleVO apiRule = ApiFirewallRuleVO.builder()
                    .protocol(rule.getProtocol())
                    .port(rule.getPort())
                    .source(rule.getIpAddress())
                    .action(rule.getAction())
                    .description(rule.getRemark())
                    .build();
            apiRules.add(apiRule);
        }

        return ApiQueryFirewallVO.builder()
                .instanceId(instanceId)
                .rules(apiRules)
                .total(apiRules.size())
                .build();
    }

    /** 到期时间计算 */
    private Date calcEndTime(OrderInfo orderInfo) {
        if (orderInfo.getPeriod() == 0) {
            return DateUtil.addDateDays(new Date(), orderInfo.getDuration());
        } else if (orderInfo.getPeriod() == 1) {
            return DateUtil.daysBeMonth(new Date(), orderInfo.getDuration());
        } else {
            return DateUtil.addDateDays(new Date(), orderInfo.getDuration() * 30);
        }
    }

    /** 多地区下单（支持不同地区同时下单） */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResultMessage createMultiRegion(String userId, MultiRegionOrderSO orderSO) {
        try {
            //参数校验
            if(orderSO == null || orderSO.getOrders() == null || orderSO.getOrders().isEmpty()){
                return new ResultMessage(ResultMessage.FAILED_CODE, "订单信息不能为空");
            }

            // 先计算所有地区的总金额，检查余额是否足够
            BigDecimal totalEstimatedAmount = BigDecimal.valueOf(0);
            List<MultiRegionOrderSO.RegionOrder> validOrders = new ArrayList<>();
            Set<Integer> uniqueNodeIds = new HashSet<>();

            for(MultiRegionOrderSO.RegionOrder regionOrder : orderSO.getOrders()){
                // 跳过重复的地区
                if(uniqueNodeIds.contains(regionOrder.getNodeId())){
                    continue;
                }
                uniqueNodeIds.add(regionOrder.getNodeId());

                //查询该地区的价格
                try {
                    NodeModel nodeModel = nodeModelMapper.selectByPrimaryKey(regionOrder.getModelId());
                    if(nodeModel == null){
                        continue; // 配置不存在，跳过
                    }

                    NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(regionOrder.getNodeId());
                    if(nodeInfo == null){
                        continue; // 节点不存在，跳过
                    }

                    // 构建简化的价格查询对象
                    OrderSO tempOrderSO = OrderSO.builder()
                            .nodeId(regionOrder.getNodeId())
                            .modelId(regionOrder.getModelId())
                            .cpu(nodeModel.getCpuVal())
                            .ram(nodeModel.getRamVal())
                            .num(regionOrder.getQuantity())
                            .period(regionOrder.getPeriod())
                            .duration(regionOrder.getPeriod())
                            .userId(userId)
                            .build();

                    BigDecimal regionPrice = orderService.queryOrderPrice(tempOrderSO);
                    if(regionPrice != null && regionPrice.compareTo(BigDecimal.ZERO) > 0){
                        totalEstimatedAmount = totalEstimatedAmount.add(regionPrice);
                        validOrders.add(regionOrder);
                    }
                } catch(Exception e){
                    log.warn("[多地区下单]计算地区{}价格失败：{}", regionOrder.getNodeId(), e.getMessage());
                }
            }

            // 检查用户余额是否足够支付总金额
            UserFinance uf = userFinanceMapper.selectByUserId(userId);
            if(uf == null || uf.getValidNum() == null || totalEstimatedAmount.compareTo(uf.getValidNum()) > 0){
                return new ResultMessage(ResultMessage.FAILED_CODE,
                    "余额不足 需要：" + totalEstimatedAmount + "元，当前余额：" + (uf != null ? uf.getValidNum() : 0) + "元");
            }

            // 3. 按地区分别处理订单（余额已确认足够）
            List<MultiRegionResultVO.InstanceInfo> successInstances = new ArrayList<>();
            List<MultiRegionResultVO.FailedOrder> failedOrders = new ArrayList<>();
            OrderInfo globalOrderInfo = null; // 全局订单记录（只在第一个地区创建）
            BigDecimal totalAmount = BigDecimal.valueOf(0);
            String globalOrderNo = CommonUtil.getOnlyNo(MainEnum.ORDER); // 创建一个总订单号
            Set<Integer> processedNodeIds = new HashSet<>(); // 记录已处理的地区ID

            for(MultiRegionOrderSO.RegionOrder regionOrder : validOrders){
                try {
                    // 检查nodeId是否重复
                    if(processedNodeIds.contains(regionOrder.getNodeId())){
                        failedOrders.add(createFailedOrder(regionOrder, "该地区已在订单中，请合并数量"));
                        continue;
                    }
                    processedNodeIds.add(regionOrder.getNodeId());
                    // 2.1 查询配置数据
                    NodeModel nodeModel = nodeModelMapper.selectByPrimaryKey(regionOrder.getModelId());
                    if(nodeModel == null){
                        failedOrders.add(createFailedOrder(regionOrder, "模版配置不存在"));
                        continue;
                    }

                    NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(regionOrder.getNodeId());
                    if(nodeInfo == null){
                        failedOrders.add(createFailedOrder(regionOrder, "节点配置不存在"));
                        continue;
                    }

                    //查询磁盘、网络配置
                    NodeDisk nodeDisk;
                    NodeNetwork bandwidth;
                    NodeNetwork flow;
                    if("Y".equals(nodeModel.getRegular())){
                        nodeDisk = nodeDiskMapper.selectByNode(regionOrder.getNodeId(), nodeModel.getId());
                        bandwidth = nodeNetworkMapper.selectByNode(regionOrder.getNodeId(), 0, nodeModel.getId());
                        flow = nodeNetworkMapper.selectByNode(regionOrder.getNodeId(), 1, nodeModel.getId());
                    }else{
                        nodeDisk = nodeDiskMapper.selectByNode(regionOrder.getNodeId(), null);
                        bandwidth = nodeNetworkMapper.selectByNode(regionOrder.getNodeId(), 0, null);
                        flow = nodeNetworkMapper.selectByNode(regionOrder.getNodeId(), 1, null);
                    }

                    if(nodeDisk == null){
                        failedOrders.add(createFailedOrder(regionOrder, "磁盘配置不存在"));
                        continue;
                    }
                    if(bandwidth == null){
                        failedOrders.add(createFailedOrder(regionOrder, "带宽配置不存在"));
                        continue;
                    }
                    if(flow == null){
                        failedOrders.add(createFailedOrder(regionOrder, "流量配置不存在"));
                        continue;
                    }

                    // 查询镜像
                    NodeImage nodeImage = nodeImageMapper.selectByParam(regionOrder.getNodeId(), regionOrder.getImageId());
                    if(nodeImage == null){
                        failedOrders.add(createFailedOrder(regionOrder, "镜像不存在"));
                        continue;
                    }

                    //获取默认配置并计算价格
                    BigDecimal sysDiskSize = nodeDisk.getGiveNum() != null ? nodeDisk.getGiveNum() : BigDecimal.valueOf(50);
                    BigDecimal dataDiskSize = nodeDisk.getMinNum() != null && nodeDisk.getGiveNum() != null
                            ? nodeDisk.getMinNum().subtract(nodeDisk.getGiveNum())
                            : BigDecimal.ZERO;
                    if(dataDiskSize.compareTo(BigDecimal.ZERO) <= 0) dataDiskSize = null;

                    BigDecimal bandwidthSize = bandwidth.getMinNum() != null ? bandwidth.getMinNum() : BigDecimal.ZERO;
                    BigDecimal flowSize = flow.getMinNum() != null ? flow.getMinNum() : BigDecimal.ZERO;

                    // 构建临时订单计算价格
                    OrderSO tempOrderSO = OrderSO.builder()
                            .nodeId(regionOrder.getNodeId())
                            .modelId(regionOrder.getModelId())
                            .cpu(nodeModel.getCpuVal())
                            .ram(nodeModel.getRamVal())
                            .sysDisk(sysDiskSize)
                            .dataDisk(dataDiskSize)
                            .bandwidth(bandwidthSize)
                            .flow(flowSize)
                            .image(nodeImage.getImageVersion())
                            .imageId(nodeImage.getId())
                            .num(regionOrder.getQuantity())
                            .period(regionOrder.getPeriod())
                            .duration(regionOrder.getPeriod())
                            .userId(userId)
                            .build();

                    BigDecimal regionTotalPrice = orderService.queryOrderPrice(tempOrderSO);

                    if(regionTotalPrice == null || regionTotalPrice.compareTo(BigDecimal.valueOf(0)) < 1){
                        failedOrders.add(createFailedOrder(regionOrder, "无效金额"));
                        continue;
                    }

                    // 只在第一个地区创建全局订单记录
                    if(globalOrderInfo == null){
                        globalOrderInfo = new OrderInfo();
                        globalOrderInfo.setOrderNo(globalOrderNo);
                        globalOrderInfo.setUserId(userId);
                        // 注意：数量和价格会在所有地区处理完后更新
                        globalOrderInfo.setNum(0); // 临时设为0，最后在方法末尾设置为成功实例总数
                        globalOrderInfo.setPeriod(regionOrder.getPeriod());
                        globalOrderInfo.setDuration(regionOrder.getPeriod());
                        globalOrderInfo.setModelId(regionOrder.getModelId());
                        globalOrderInfo.setPrice(BigDecimal.valueOf(0)); // 先设为0，后续累加
                        globalOrderInfo.setOnlyPrice(BigDecimal.valueOf(0)); // 先设为0，后续累加
                        globalOrderInfo.setDiscount(BigDecimal.valueOf(0));
                        globalOrderInfo.setType(0);
                        globalOrderInfo.setNodeId(regionOrder.getNodeId());
                        globalOrderInfo.setLabel(nodeInfo.getLabel());
                        globalOrderInfo.setCpu(nodeModel.getCpuVal());
                        globalOrderInfo.setRam(nodeModel.getRamVal());
                        globalOrderInfo.setSysDisk(sysDiskSize);
                        globalOrderInfo.setDataDisk(dataDiskSize);
                        globalOrderInfo.setBandwidth(bandwidthSize);
                        globalOrderInfo.setFlow(flowSize);
                        globalOrderInfo.setImage(nodeImage.getImageVersion());
                        globalOrderInfo.setImageId(nodeImage.getId());
                        globalOrderInfo.setStatus(0);
                        globalOrderInfo.setCreateTime(new Date());
                        globalOrderInfo.setUpdateTime(new Date());
                        orderInfoMapper.insertSelective(globalOrderInfo);
                    }

                    // 创建该地区的所有实例
                    for(int i = 0; i < regionOrder.getQuantity(); i++){
                        InstanceInfo instanceInfo = new InstanceInfo();
                        instanceInfo.setInstanceId(CommonUtil.getOnlyNo(MainEnum.MAIN));
                        instanceInfo.setOrderNo(globalOrderNo); // 使用总订单号
                        instanceInfo.setUserId(userId);
                        instanceInfo.setType(0);
                        instanceInfo.setNodeId(regionOrder.getNodeId());
                        instanceInfo.setLabel(nodeInfo.getLabel());

                        // 特殊用户使用del=2的特殊账号
                        PlatformAccount platformAccount = platformAccountMapper.selectByLabelAndDel(nodeInfo.getLabel(), 2);
                        if(platformAccount == null){
                            failedOrders.add(createFailedOrder(regionOrder, "该平台暂不支持下单"));
                            break; // 跳出实例创建循环，继续处理下一个地区
                        }
                        instanceInfo.setAccountId(platformAccount.getId());
                        instanceInfo.setModelId(regionOrder.getModelId());
                        instanceInfo.setCpu(nodeModel.getCpuVal());
                        instanceInfo.setRam(nodeModel.getRamVal());
                        instanceInfo.setConnectPwd(CommonUtil.getConnectPwd(PlatformLabelEnum.getByLabel(nodeInfo.getLabel())));
                        instanceInfo.setSysDisk(sysDiskSize);
                        instanceInfo.setDataDisk(dataDiskSize);
                        instanceInfo.setBandwidth(bandwidthSize);
                        instanceInfo.setFlow(flowSize);
                        instanceInfo.setImage(nodeImage.getImageVersion());
                        instanceInfo.setImageId(nodeImage.getId());
                        instanceInfo.setPeriod(regionOrder.getPeriod());
                        instanceInfo.setStatus(0);
                        instanceInfo.setCreateTime(new Date());
                        // 计算到期时间（period就是月数，按月购买逻辑）
                        instanceInfo.setEndTime(DateUtil.daysBeMonth(new Date(), regionOrder.getPeriod()));
                        instanceInfo.setUpdateTime(new Date());

                        int insertResult = instanceInfoMapper.insertSelective(instanceInfo);
                        if(insertResult > 0){
                            // 添加到成功列表
                            MultiRegionResultVO.InstanceInfo instanceVO = new MultiRegionResultVO.InstanceInfo();
                            instanceVO.setInstanceId(instanceInfo.getInstanceId());
                            instanceVO.setRegion(nodeInfo.getNodeName());
                            instanceVO.setUnitPrice(regionTotalPrice.divide(BigDecimal.valueOf(regionOrder.getQuantity()), 2, BigDecimal.ROUND_DOWN).toPlainString());
                            successInstances.add(instanceVO);

                            totalAmount = totalAmount.add(regionTotalPrice.divide(BigDecimal.valueOf(regionOrder.getQuantity()), 2, BigDecimal.ROUND_DOWN));
                        }
                    }

                    // 调用云厂商API创建实例
                    List<InstanceInfo> regionInstances = new ArrayList<>();
                    List<MultiRegionResultVO.InstanceInfo> currentRegionInstVOs = new ArrayList<>();

                    // 一次遍历：收集当前地区的实例（避免重复查询数据库）
                    for(MultiRegionResultVO.InstanceInfo instVO : successInstances){
                        if(instVO.getRegion().equals(nodeInfo.getNodeName())){
                            InstanceInfo dbInfo = instanceInfoMapper.selectById(instVO.getInstanceId());
                            if(dbInfo != null){
                                regionInstances.add(dbInfo);
                                currentRegionInstVOs.add(instVO);
                            }
                        }
                    }

                    // 如果当前地区没有实例，跳过API调用
                    if(regionInstances.isEmpty()){
                        continue;
                    }

                    Map<String, Boolean> apiResult = doCreateInstance(regionInstances);

                    // 找出当前地区失败的实例（只遍历当前地区的实例，避免重复查询）
                    List<MultiRegionResultVO.InstanceInfo> failedInstances = new ArrayList<>();
                    for(MultiRegionResultVO.InstanceInfo instVO : currentRegionInstVOs){
                        Boolean result = apiResult.get(instVO.getInstanceId());
                        if(result == null || !result){
                            failedInstances.add(instVO);
                        }
                    }

                    // 为API调用成功的实例创建财务明细
                    for(MultiRegionResultVO.InstanceInfo instVO : successInstances){
                        // 只检查当前地区的实例
                        if(!instVO.getRegion().equals(nodeInfo.getNodeName())){
                            continue;
                        }
                        // 跳过失败的实例
                        if(failedInstances.stream().anyMatch(failed -> failed.getInstanceId().equals(instVO.getInstanceId()))){
                            continue;
                        }

                        InstanceInfo dbInfo = instanceInfoMapper.selectById(instVO.getInstanceId());
                        if(dbInfo != null){
                            FinanceDetail financeDetail = new FinanceDetail();
                            financeDetail.setUserId(userId);
                            financeDetail.setFinanceNo(CommonUtil.getRandomStr(12));
                            financeDetail.setProductNo(dbInfo.getInstanceId());
                            financeDetail.setType(1);
                            financeDetail.setMoneyNum(new BigDecimal(instVO.getUnitPrice()));
                            financeDetail.setPeriod(regionOrder.getPeriod());
                            financeDetail.setTag("buy");
                            financeDetail.setDirection(1);
                            financeDetail.setWay(2);
                            financeDetail.setStatus(0);
                            financeDetail.setCreateTime(new Date());
                            financeDetail.setUpdateTime(new Date());
                            financeDetailMapper.insertSelective(financeDetail);
                        }
                    }

                    // 处理失败的实例：扣减金额并删除记录
                    for(MultiRegionResultVO.InstanceInfo failedInst : failedInstances){
                        if(failedInst.getUnitPrice() != null){
                            try{
                                totalAmount = totalAmount.subtract(new BigDecimal(failedInst.getUnitPrice()));
                            } catch (NumberFormatException e){
                                log.warn("实例{}单价格式错误：{}", failedInst.getInstanceId(), failedInst.getUnitPrice());
                            }
                        }
                        InstanceInfo dbInfo = instanceInfoMapper.selectById(failedInst.getInstanceId());
                        if(dbInfo != null && dbInfo.getId() != null){
                            instanceInfoMapper.deleteByPrimaryKey(dbInfo.getId());
                        }
                    }

                    // 从成功列表中移除失败的实例
                    successInstances.removeIf(instVO ->
                        failedInstances.stream().anyMatch(failed -> failed.getInstanceId().equals(instVO.getInstanceId()))
                    );

                } catch(Exception e){
                    log.error("[开放接口]多地区下单-地区[{}]处理异常：{}", regionOrder.getNodeId(), e.getMessage(), e);
                    // 查询平台名称，给用户简化的错误信息
                    NodeInfo tmpNodeInfo = nodeInfoMapper.selectByPrimaryKey(regionOrder.getNodeId());
                    String platformName = (tmpNodeInfo != null && tmpNodeInfo.getLabel() != null) ? tmpNodeInfo.getLabel() : "云厂商";
                    failedOrders.add(createFailedOrder(regionOrder, platformName + "平台下单失败"));
                }
            }

            // 3. 更新订单状态和冻结金额
            if(successInstances.size() > 0){
                // 更新全局订单：设置正确的数量和价格
                OrderInfo updateOrder = new OrderInfo();
                updateOrder.setId(globalOrderInfo.getId());
                updateOrder.setNum(successInstances.size()); // 成功实例总数
                updateOrder.setPrice(totalAmount); // 总金额
                updateOrder.setOnlyPrice(null); // 多地区订单单价设为null（不同地区价格不同）
                updateOrder.setStatus(2); // 部分/全部成功
                updateOrder.setUpdateTime(new Date());
                orderInfoMapper.updateByPrimaryKeySelective(updateOrder);

                // 扣款
                int i = userFinanceMapper.updateBalanceByUserId(userId, "seal", totalAmount);
                if(i > 0){
                    UserFinance latest = userFinanceMapper.selectByUserId(userId);
                    balanceLogMapper.insertChange(userId, "seal", totalAmount, latest.getValidNum(), "多地区下单冻结金额");
                } else {
                    // 扣款失败（余额不足），抛出异常让事务回滚
                    throw new RuntimeException("余额不足，下单失败");
                }
            }

            // 4. 构建返回结果
            MultiRegionResultVO resultVO = new MultiRegionResultVO();
            resultVO.setOrderNo(globalOrderNo);
            resultVO.setSuccessNum(successInstances.size());
            resultVO.setTotalAmount(totalAmount.toPlainString());
            resultVO.setInstances(successInstances);
            // 只在有失败订单时才设置failed字段
            if(failedOrders != null && !failedOrders.isEmpty()){
                resultVO.setFailed(failedOrders);
            }

            if(successInstances.size() > 0){
                return new ResultMessage(ResultMessage.SUCCEED_CODE, "下单成功", resultVO);
            }else{
                return new ResultMessage(ResultMessage.FAILED_CODE, "下单失败", resultVO);
            }

        } catch (Exception e) {
            log.error("[开放接口]多地区下单异常：{}", e.getMessage(), e);
            return new ResultMessage(ResultMessage.FAILED_CODE, "平台处理失败");
        }
    }

    /** 辅助方法：创建失败订单对象 */
    private MultiRegionResultVO.FailedOrder createFailedOrder(MultiRegionOrderSO.RegionOrder regionOrder, String errorMsg){
        MultiRegionResultVO.FailedOrder failedOrder = new MultiRegionResultVO.FailedOrder();
        failedOrder.setNodeId(regionOrder.getNodeId());
        // 通过nodeId查询地区名称
        NodeInfo nodeInfo = nodeInfoMapper.selectByPrimaryKey(regionOrder.getNodeId());
        failedOrder.setRegion(nodeInfo != null ? nodeInfo.getNodeName() : "未知地区");
        failedOrder.setQuantity(regionOrder.getQuantity());
        failedOrder.setError(errorMsg);
        return failedOrder;
    }

}
