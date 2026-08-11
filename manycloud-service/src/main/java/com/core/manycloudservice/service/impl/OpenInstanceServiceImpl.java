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
                }else{
                    return new ResultMessage(ResultMessage.SUCCEED_CODE, "下单成功但余额异常");
                }
            }else{
                return new ResultMessage(ResultMessage.FAILED_CODE, "下单失败");
            }

        } catch (Exception e) {
            log.error("[开放接口]下单异常：{}", e.getMessage(), e);
            return new ResultMessage(ResultMessage.FAILED_CODE, "下单异常：" + e.getMessage());
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


    // ============================== 查询 / 操作 ==============================

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

}
