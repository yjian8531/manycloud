package com.core.manycloudcommon.caller;

import com.core.manycloudcommon.caller.Item.*;
import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.*;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.HttpRequest;
import com.core.manycloudcommon.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;


import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class IpLightCaller implements BaseCaller {

    private static Map<String, IpLightCaller> IpLightCallerMap = new HashMap<>();

    private String pubKey;
    private String pivKey;
    private String regionId;

    private String url = "https://api.iplight.net/api/iplt";

    private IpLightCaller(String pubKey, String pivKey, String regionId) {
        this.pivKey = pivKey;
        this.pubKey = pubKey;
        this.regionId = regionId;
    }

    /**
     * 获取客户端
     *
     * @param regionId
     * @return
     * @throws Exception
     */
    public static IpLightCaller getClient(String regionId, AccountApi accountApi) {
        String cacheKey = regionId + ":" + accountApi.getAccount();
        if (IpLightCallerMap.get(cacheKey) == null) {
            synchronized (IpLightCaller.class) {
                if (IpLightCallerMap.get(cacheKey) == null) {
                    IpLightCaller caller = new IpLightCaller(accountApi.getKeyNo(), accountApi.getKeySecret(), regionId);
                    IpLightCallerMap.put(cacheKey, caller);
                }
            }
        }
        return IpLightCallerMap.get(cacheKey);
    }

    public static IpLightCaller getClient(AccountApi accountApi) {
        // 从AccountApi获取regionId
        String regionId = accountApi.getRegionId();
        String cacheKey = regionId + ":" + accountApi.getAccount();

        // 双重检查锁保持不变
        if (IpLightCallerMap.get(cacheKey) == null) {
            synchronized (IpLightCaller.class) {
                if (IpLightCallerMap.get(cacheKey) == null) {
                    IpLightCaller caller = new IpLightCaller(
                            accountApi.getKeyNo(),
                            accountApi.getKeySecret(),
                            regionId  // 使用从AccountApi获取的regionId
                    );
                    IpLightCallerMap.put(cacheKey, caller);
                }
            }
        }
        return IpLightCallerMap.get(cacheKey);
    }

    /**
     * 创建订单
     *
     * @param createSO
     * @return 创建订单结果 JSON 字符串
     * @throws Exception
     */
    @Override
    public CreateVO create(CreateSO createSO) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x-merchant-token", pubKey);
        headers.put("x-merchant-code", pivKey);
        Map<String, Object> params = new HashMap<>();
        params.put("quantity", createSO.getNum());
        params.put("currency", "USD");
        params.put("payMethod", "BALANCE");
        params.put("payMode", "PAY_FIRST");
        params.put("activationDurationType", String.valueOf(createSO.getPeriod()));

        List<Map<String, Object>> orderItems = new ArrayList<>();
        Map<String, Object> orderItem = new HashMap<>();
        orderItem.put("hostTemplateId", createSO.getBundleId());
        orderItems.add(orderItem);
        params.put("orderItems", orderItems);

        // 调用“创建订单”接口
        String requestUrl = this.url + "/client/order/addVps";
        String response = HttpRequest.postJson(requestUrl, JSONObject.fromObject(params).toString(), headers);
        JSONObject jsonResponse = JSONObject.fromObject(response);

        // 解析“创建订单”结果
        if (jsonResponse.getInt("code") == 200) {
            String orderId = jsonResponse.getString("data");
            try {
                // 调用“订单支付”接口
                PayVO payVO = orderPay(orderId);
                if (CommonUtil.SUCCESS_CODE.equals(payVO.getCode())) {
                    log.info("订单{}创建成功，且支付成功", orderId);

                    // 构造实例ID集合，放入订单号
                    List<String> instanceIds = new ArrayList<>();
                    instanceIds.add(orderId);

                    // 封装结果：将订单号放入instanceIds返回
                    return CreateVO.builder()
                            .code(CommonUtil.SUCCESS_CODE)
                            .msg(CommonUtil.SUCCESS_MSG)
                            .instanceIds(instanceIds)
                            .build();
                } else {
                    log.info("订单{}创建成功，但支付失败，失败原因：{}", orderId, payVO.getMsg());
                    return CreateVO.builder()
                            .code(CommonUtil.FAIL_CODE)
                            .msg(CommonUtil.FAIL_MSG)
                            .build();
                }
            } catch (Exception e) {
                log.error("订单{}创建成功，但支付/查询异常：{}", orderId, e.getMessage(), e);
                return CreateVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(CommonUtil.FAIL_MSG)
                        .build();
            }
        } else {
            String errorMsg = jsonResponse.getString("msg");
            log.error("购买VPS失败: {}", errorMsg);
            return CreateVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 创建查询
     *
     * @param querySO 查询参数
     * @return 创建查询结果 JSON 字符串
     * @throws Exception
     */
    @Override
    public QueryVO createQuery(QuerySO querySO) throws Exception {
        if (querySO == null || querySO.getInstanceIds() == null || querySO.getInstanceIds().isEmpty()) {
            throw new IllegalArgumentException("实例ID集合（instanceIds）为必填参数，且至少包含一个订单号");
        }
        String orderId = querySO.getInstanceIds().get(0);
        log.info("开始处理订单{}的查询：订单号→vpsCode→主机信息", orderId);

        // 调用订单接口：用订单号查询vpsCode、公网IP
        Map<String, String> headers = new HashMap<>();
        headers.put("x-merchant-token", pubKey);
        headers.put("x-merchant-code", pivKey);

        // 构建订单查询URL
        StringBuilder orderUrlBuilder = new StringBuilder(this.url + "/client/order/vpsOrderList");
        orderUrlBuilder.append("?orderNo=").append(orderId);
        String orderQueryUrl = orderUrlBuilder.toString();

        // 发送请求并获取响应
        String orderResponse = HttpRequest.get(orderQueryUrl, headers);
        log.info("订单{}查询接口返回原始响应：{}", orderId, orderResponse);
        JSONObject orderJson = JSONObject.fromObject(orderResponse);

        // 第一步：判断订单接口返回code是否成功
        if (orderJson.getInt("code") != 200) {
            String errorMsg = "订单" + orderId + "查询失败：" + orderJson.getString("msg");
            log.error(errorMsg);
            throw new Exception(errorMsg);
        }

        // 解析订单数据获取vpsCode及IP信息
        JSONArray orderRows = orderJson.getJSONArray("rows");
        if (orderRows.isEmpty()) {
            throw new Exception("订单" + orderId + "未查询到关联实例");
        }
        JSONObject orderRow = orderRows.getJSONObject(0);
        JSONArray orderItems = orderRow.getJSONArray("orderItems");
        if (orderItems.isEmpty()) {
            throw new Exception("订单" + orderId + "未查询到实例详情（orderItems为空）");
        }
        JSONObject item = orderItems.getJSONObject(0);
        if (!item.has("vpsCode")) {
            throw new Exception("订单" + orderId + "未查询到vpsCode（orderItem中无该字段）");
        }

        // 提取订单查询关键信息
        String vpsCode = item.getString("vpsCode");
        String publicIp = item.getString("vpsIp");
        log.info("订单{}查询到vpsCode：{}，公网IP：{}", orderId, vpsCode, publicIp);

        //调用 /client/vps/list 接口查询主机详细信息
        StringBuilder vpsUrlBuilder = new StringBuilder(this.url + "/client/vps/list");
        vpsUrlBuilder.append("?vpsCode=").append(vpsCode);
        String vpsQueryUrl = vpsUrlBuilder.toString();

        try {
            // 发送 GET 请求
            String vpsResponse = HttpRequest.get(vpsQueryUrl, headers);
            log.info("VPS列表查询接口返回原始响应：{}", vpsResponse);

            // 解析响应结果
            JSONObject vpsJsonResponse = JSONObject.fromObject(vpsResponse);
            // 判断主机信息接口返回code是否成功
            int code = vpsJsonResponse.getInt("code");
            String msg = vpsJsonResponse.getString("msg");
            if (code != 200) {
                log.error("查询VPS列表失败: {}", msg);
                throw new Exception("查询VPS列表失败: " + msg);
            }
            // 接口调用成功，处理返回数据
            JSONArray rowsArray = vpsJsonResponse.getJSONArray("rows");
            if (rowsArray.isEmpty()) {
                throw new Exception("未查询到vpsCode=" + vpsCode + "的主机信息");
            }
            JSONObject row = rowsArray.getJSONObject(0);

            // 获取并判断主机状态
            int serverStatus = row.getInt("status");
            log.info("vpsCode={}的主机当前状态码：{}", vpsCode, serverStatus);

            // 创建查询详情对象
            QueryDetailVO queryDetailVO = new QueryDetailVO();
            queryDetailVO.setServiceNo(vpsCode);
            queryDetailVO.setPublicIp(publicIp);

            // 仅当状态为运行中(20)时，才获取完整数据
            if (serverStatus == 20) {
                // 运行中状态：设置所有信息，状态为成功（1）
                queryDetailVO.setAccount(row.getString("sshAccount")); // 账号
                queryDetailVO.setPort(row.getInt("sshPort"));         // 端口
                queryDetailVO.setPwd(row.getString("sshPwd"));        // 密码
                queryDetailVO.setStatus(1);                           // 成功
                queryDetailVO.setPowerState("running");               // 运行中状态描述
            } else {
                // 非运行中状态：只设置状态信息
                if (serverStatus == 0 || serverStatus == 10 || serverStatus == 21 || serverStatus == 25) {
                    queryDetailVO.setStatus(0); // 待定（对应启动中状态）
                    queryDetailVO.setPowerState("execution");
                } else if (serverStatus == 30 || serverStatus == 35) {
                    queryDetailVO.setStatus(2); // 失败（对应关机中状态）
                    queryDetailVO.setPowerState("halted");
                } else if (serverStatus == 40 || serverStatus == 50 || serverStatus == 55 || serverStatus == 60 || serverStatus == 70) {
                    queryDetailVO.setStatus(2); // 失败（对应关机状态）
                    queryDetailVO.setPowerState("halted");
                }
            }

            // 构建最终返回结果
            Map<String, QueryDetailVO> finalResultMap = new HashMap<>();
            finalResultMap.put(orderId, queryDetailVO);

            return QueryVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .queryDetailMap(finalResultMap)
                    .build();

        } catch (Exception e) {
            log.error("查询VPS列表异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 查询
     *
     * @param querySO 查询参数
     * @return 查询结果 JSON 字符串
     * @throws Exception
     */
    @Override
    public QueryVO query(QuerySO querySO) throws Exception {
        if (querySO == null || querySO.getInstanceIds() == null || querySO.getInstanceIds().isEmpty()) {
            throw new IllegalArgumentException("实例ID集合（instanceIds）为必填参数，且至少包含一个实例ID");
        }
        // 创建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-merchant-token", pubKey);
        headers.put("x-merchant-code", pivKey);

        StringBuilder urlBuilder = new StringBuilder(this.url + "/client/vps/list");
        boolean isFirstParam = true;
        if (querySO != null) {
            // 从instanceIds列表取第一个元素作为vpsCode参数值
            String vpsCode = querySO.getInstanceIds().get(0);
            urlBuilder.append(isFirstParam ? "?" : "&").append("vpsCode=").append(vpsCode);
            isFirstParam = false;
        }

        String requestUrl = urlBuilder.toString();

        try {
            // 发送 GET 请求
            String response = HttpRequest.get(requestUrl, headers);

            log.info("VPS列表查询接口返回原始响应：{}", response);

            // 解析响应结果
            JSONObject jsonResponse = JSONObject.fromObject(response);

            // 判断响应是否包含code字段
            if (!jsonResponse.has("code")) {
                String errorMsg = "响应缺少 code 字段，响应：" + response;
                log.error(errorMsg);
                throw new Exception(errorMsg);
            }

            //判断接口返回code是否为200
            int code = jsonResponse.getInt("code");
            String msg = jsonResponse.getString("msg");
            if (code != 200) {
                log.error("查询VPS列表失败: {}", msg);
                return QueryVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(CommonUtil.FAIL_MSG)
                        .build();
            }

            // 接口调用成功，继续处理数据
            JSONArray rowsArray = jsonResponse.getJSONArray("rows");

            Map<String, QueryDetailVO> queryDetailMap = new HashMap<>();
            for (int i = 0; i < rowsArray.size(); i++) {
                JSONObject row = rowsArray.getJSONObject(i);
                QueryDetailVO queryDetailVO = new QueryDetailVO();
                queryDetailVO.setServiceNo(row.getString("vpsCode"));
                queryDetailVO.setPublicIp(row.getString("vpsIp"));
                // 获取主机状态码
                int serverStatus = row.getInt("status");
                //仅当状态为运行中(20)时，才获取完整数据
                if (serverStatus == 20) {
                    // 运行中状态：设置所有信息，状态为成功（1）
                    queryDetailVO.setAccount(row.getString("sshAccount")); // 账号
                    queryDetailVO.setPort(row.getInt("sshPort"));         // 端口
                    queryDetailVO.setPwd(row.getString("sshPwd"));        // 密码
                    queryDetailVO.setStatus(1);                           // 成功
                    queryDetailVO.setPowerState("running");               // 运行中状态描述
                } else {
                    // 非运行中状态：只设置状态信息，
                    if (serverStatus == 0 || serverStatus == 10 || serverStatus == 21 || serverStatus == 25) {
                        queryDetailVO.setStatus(0); // 待定（对应启动中状态）
                        queryDetailVO.setPowerState("execution");
                    } else if (serverStatus == 30 || serverStatus == 35) {
                        queryDetailVO.setStatus(2); // 失败（对应关机中状态）
                        queryDetailVO.setPowerState("halted");
                    } else if (serverStatus == 40 || serverStatus == 50 || serverStatus == 55 || serverStatus == 60 || serverStatus == 70) {
                        queryDetailVO.setStatus(2); // 失败（对应关机状态）
                        queryDetailVO.setPowerState("halted");
                    }
                }
                queryDetailMap.put(row.getString("vpsCode"), queryDetailVO);
            }

            return QueryVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .queryDetailMap(queryDetailMap)
                    .build();

        } catch (Exception e) {
            log.error("查询VPS列表异常: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 续费
     *
     * @param renewSO 续费参数
     * @return 续费结果 JSON 字符串
     * @throws Exception
     */
    @Override
    public RenewVO renew(RenewSO renewSO) throws Exception {
        // 创建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-merchant-token", pubKey);
        headers.put("x-merchant-code", pivKey);
        headers.put("Content-Type", "application/json");

        // 构建请求参数
        Map<String, Object> params = new HashMap<>();
        params.put("quantity", 1);
        params.put("currency", "USD");
        params.put("payMethod", "BALANCE");
        params.put("payMode", "PAY_FIRST");
        params.put("activationDurationType", "" + renewSO.getNum() + "");

        // 构建orderItems
        List<Map<String, String>> orderItems = new ArrayList<>();
        Map<String, String> orderItem = new HashMap<>();
        orderItem.put("vpsCode", renewSO.getInstanceId());
        orderItems.add(orderItem);
        params.put("orderItems", orderItems);

        // 构建完整URL
        String url = this.url + "/client/order/addVps";

        // 发送POST请求
        String response = HttpRequest.postJson(url, JSONObject.fromObject(params).toString(), headers);
        JSONObject responseJson = JSONObject.fromObject(response);

        // 解析code和msg
        int code = responseJson.getInt("code");
        if (code == 200) {
            // code为200，返回成功
            return RenewVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .data(responseJson.get("data"))
                    .build();
        } else {
            // code不为200，返回失败
            return RenewVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .data(null)
                    .build();
        }
    }

    /**
     * 开机
     *
     * @param startSO
     * @return
     * @throws Exception
     */
    @Override
    public StartVO start(StartSO startSO) throws Exception {
        // 创建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-merchant-token", pubKey);
        headers.put("x-merchant-code", pivKey);
        headers.put("Content-Type", "application/json");

        // 构建请求参数
        Map<String, String> params = new HashMap<>();
        params.put("vpsCode", startSO.getInstanceId());

        // 构建完整URL
        String url = this.url + "/client/vps/start";

        try {
            // 发送POST请求
            String response = HttpRequest.postJson(url, JSONObject.fromObject(params).toString(), headers);
            log.info("启动VPS接口返回原始响应：{}", response);

            // 解析响应结果
            JSONObject responseJson = JSONObject.fromObject(response);

            int code = responseJson.getInt("code");

            if (code == 200) {
                // code为200，返回成功
                return StartVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            } else {
                // code不为200，返回失败
                return StartVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(CommonUtil.FAIL_MSG)
                        .build();
            }
        } catch (IOException e) {
            log.error("启动VPS接口调用失败：{}", e.getMessage(), e);
            return StartVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }


    /**
     * 重启
     *
     * @param rebootSO
     * @return
     * @throws Exception
     */
    @Override
    public RebootVO reboot(RebootSO rebootSO) throws Exception {
        return RebootVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("IpLight-不支持此功能")
                .build();
    }

    /**
     * 关机
     *
     * @param stopSO
     * @return
     * @throws Exception
     */
    @Override
    public StopVO stop(StopSO stopSO) throws Exception {
        // 创建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-merchant-token", pubKey);
        headers.put("x-merchant-code", pivKey);
        headers.put("Content-Type", "application/json");

        // 构建请求参数
        Map<String, String> params = new HashMap<>();
        params.put("vpsCode", stopSO.getInstanceId());

        // 构建完整URL
        String url = this.url + "/client/vps/stop";

        try {
            // 发送POST请求
            String response = HttpRequest.postJson(url, JSONObject.fromObject(params).toString(), headers);
            log.info("关闭VPS接口返回原始响应：{}", response);

            // 解析响应结果
            JSONObject responseJson = JSONObject.fromObject(response);

            int code = responseJson.getInt("code");

            if (code == 200) {
                // code为200，返回成功
                return StopVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            } else {
                // code不为200，返回失败
                return StopVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(CommonUtil.FAIL_MSG)
                        .build();
            }
        } catch (IOException e) {
            log.error("关闭VPS接口调用失败：{}", e.getMessage(), e);
            // 异常时返回失败信息
            return StopVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 重装系统
     *
     * @param reinstallSO
     * @return
     * @throws Exception
     */
    @Override
    public ReinstallVO reinstall(ReinstallSO reinstallSO) throws Exception {
        return ReinstallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("IpLight-不支持此功能")
                .build();
    }

    /**
     * 销毁
     *
     * @param destroySO
     * @return
     * @throws Exception
     */
    @Override
    public DestroyVO destroy(DestroySO destroySO) throws Exception {
        // 创建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-merchant-token", pubKey);
        headers.put("x-merchant-code", pivKey);
        headers.put("Content-Type", "application/json");

        // 构建请求参数
        Map<String, String> params = new HashMap<>();
        params.put("unsubscribeVpsCodes", destroySO.getInstanceId());

        // 构建完整URL
        String url = this.url + "/client/order/unsubscribeVps";

        try {
            // 发送POST请求
            String response = HttpRequest.postJson(url, JSONObject.fromObject(params).toString(), headers);
            log.info("销毁VPS接口返回原始响应：{}", response); // 修正日志描述

            // 解析响应结果
            JSONObject responseJson = JSONObject.fromObject(response);

            // 提取响应中的状态码和消息
            int code = responseJson.getInt("code");
            if (code == 200) {
                // 响应成功时返回成功结果
                return DestroyVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .build();
            } else {
                // 响应失败时返回接口返回的错误信息
                return DestroyVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(CommonUtil.FAIL_MSG)
                        .build();
            }
        } catch (IOException e) {
            log.error("销毁VPS接口调用失败：{}", e.getMessage(), e); // 修正日志描述
            // 异常时返回失败信息
            return DestroyVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 设置主机自动续费标识
     *
     * @param updateAuteRenewSO
     * @return
     */
    @Override
    public UpdateAuteRenewVO updateAuteRenew(UpdateAuteRenewSO updateAuteRenewSO) throws Exception {
        return UpdateAuteRenewVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("IpLight-不支持此功能")
                .build();
    }

    /**
     * 修改密码
     *
     * @param updatePwdSO
     * @return
     * @throws Exception
     */
    @Override
    public UpdatePwdVO updatePwd(UpdatePwdSO updatePwdSO) throws Exception {
        return UpdatePwdVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("IpLight-不支持此功能")
                .build();
    }

    /**
     * 创建防火墙
     *
     * @param createSecuritySO
     * @return
     * @throws Exception
     */
    @Override
    public CreateSecurityVO createFirewallTo(CreateSecuritySO createSecuritySO) throws Exception {
        return CreateSecurityVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("IpLight-不支持此功能")
                .build();
    }

    /**
     * 查询防火墙
     *
     * @param queryFirewallSO
     * @return
     * @throws Exception
     */
    @Override
    public QueryFirewallVO queryFirewall(QueryFirewallSO queryFirewallSO) throws Exception {
        return QueryFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("IpLight-不支持此功能")
                .build();
    }

    /**
     * 绑定防火墙
     *
     * @param grantFirewallSO
     * @return
     * @throws Exception
     */
    @Override
    public GrantFirewallVO grantFirewall(GrantFirewallSO grantFirewallSO) throws Exception {
        return GrantFirewallVO.builder()
                .code(CommonUtil.FAIL_CODE)
                .msg("IpLight-不支持此功能")
                .build();
    }

    /**
     * 查询集群列表
     *
     * @param clusterListSO
     * @return
     * @throws Exception
     */
    @Override
    public ClusterVO queryClusterList(ClusterListSO clusterListSO) throws Exception {
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-merchant-token", pubKey);
        headers.put("x-merchant-code", pivKey);
        headers.put("Content-Type", "application/json");

        // 构建请求 URL
        StringBuilder urlBuilder = new StringBuilder(this.url + "/client/cluster/clusterList");
        if (clusterListSO != null) {
            if (clusterListSO.getPageNum() != null) {
                urlBuilder.append("?pageNum=").append(clusterListSO.getPageNum());
            }
            if (clusterListSO.getPageSize() != null) {
                urlBuilder.append("&pageSize=").append(clusterListSO.getPageSize());
            }
            if (clusterListSO.getContinent() != null) {
                urlBuilder.append("&continent=").append(clusterListSO.getContinent());
            }
            if (clusterListSO.getCountryCode() != null) {
                urlBuilder.append("&countryCode=").append(clusterListSO.getCountryCode());
            }
        }

        String requestUrl = urlBuilder.toString();

        // 发送 GET 请求
        String response = HttpRequest.get(requestUrl, headers);

        // 解析响应结果
        JSONObject jsonResponse = JSONObject.fromObject(response);
        if (jsonResponse.getInt("code") == 200) {
            int total = jsonResponse.getInt("total");
            JSONArray rowsArray = jsonResponse.getJSONArray("rows");

            List<ClusterItem> rows = new ArrayList<>();
            for (int i = 0; i < rowsArray.size(); i++) {
                JSONObject row = rowsArray.getJSONObject(i);
                ClusterItem item = new ClusterItem();
                item.setId(row.getInt("id"));
                item.setClusterName(row.getString("clusterName"));
                item.setClusterType(row.getString("clusterType"));
                item.setContinent(row.getString("continent"));
                item.setCountryCode(row.getString("countryCode"));
                item.setStateCode(row.getString("stateCode"));
                rows.add(item);
            }
            return ClusterVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .rows(rows)
                    .total(total)
                    .build();
        } else {
            String errorMsg = jsonResponse.getString("msg");
            log.error("查询集群列表失败: {}", errorMsg);
            return ClusterVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 查询模板列表
     *
     * @param templateListSO
     * @return
     * @throws Exception
     */
    @Override
    public TemplateListVO queryTemplateList(TemplateListSO templateListSO) throws Exception {
        if (templateListSO == null || templateListSO.getClusterId() == null) {
            throw new IllegalArgumentException("clusterId是必填参数，不能为空");
        }
        // 构建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-merchant-token", pubKey);
        headers.put("x-merchant-code", pivKey);
        headers.put("Content-Type", "application/json");

        // 构建请求 URL（带查询参数）
        StringBuilder urlBuilder = new StringBuilder(this.url + "/client/cluster/templateList");
        // 必传参数clusterId，直接拼接
        urlBuilder.append("?clusterId=").append(templateListSO.getClusterId());
        if (templateListSO != null) {
            if (templateListSO.getTemplateId() != null) {
                urlBuilder.append("&templateId=").append(templateListSO.getTemplateId());
            }
            if (templateListSO.getPageNum() != null) {
                urlBuilder.append("&pageNum=").append(templateListSO.getPageNum());
            }
            if (templateListSO.getPageSize() != null) {
                urlBuilder.append("&pageSize=").append(templateListSO.getPageSize());
            }
        }

        String requestUrl = urlBuilder.toString();

        // 发送 GET 请求
        String response = HttpRequest.get(requestUrl, headers);

        // 解析响应结果
        JSONObject jsonResponse = JSONObject.fromObject(response);
        if (jsonResponse.getInt("code") == 200) {
            int total = jsonResponse.getInt("total");
            JSONArray rowsArray = jsonResponse.getJSONArray("rows");

            List<TemplateItem> rows = new ArrayList<>();
            for (int i = 0; i < rowsArray.size(); i++) {
                JSONObject row = rowsArray.getJSONObject(i);
                TemplateItem item = new TemplateItem();
                item.setId(row.getInt("id"));
                item.setTemplateId(row.getString("templateId"));
                item.setClusterId(row.getInt("clusterId"));
                item.setModel(row.getString("model"));
                item.setCpuCores(row.getInt("cpuCores"));
                item.setCpuModel(row.getString("cpuModel"));
                item.setMemory(row.getInt("memory"));
                item.setDiskCapacity(row.getInt("diskCapacity"));
                item.setDiskModel(row.getString("diskModel"));
                item.setIoWrite(row.getInt("ioWrite"));
                item.setIoRead(row.getInt("ioRead"));
                item.setBandwidth(row.getInt("bandwidth"));
                item.setInternetTraffic(row.getInt("internetTraffic"));
                item.setIpV4(row.getString("ipV4"));
                item.setIpV6(row.getString("ipV6"));
                item.setRefId(row.getString("refId"));
                // 修改第519行及类似代码
                item.setPriceMonthly(new BigDecimal(row.getString("priceMonthly")));
                item.setPriceQuarterly(new BigDecimal(row.getString("priceQuarterly")));
                item.setPriceSemiAnnually(new BigDecimal(row.getString("priceSemiAnnually")));
                item.setPriceAnnually(new BigDecimal(row.getString("priceAnnually")));
                rows.add(item);
            }

            return TemplateListVO.builder()
                    .code(CommonUtil.SUCCESS_CODE)
                    .msg(CommonUtil.SUCCESS_MSG)
                    .total(total)
                    .rows(rows)
                    .build();
        } else {
            String errorMsg = jsonResponse.getString("msg");
            log.error("查询集群模板列表失败: {}", errorMsg);
            return TemplateListVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(CommonUtil.FAIL_MSG)
                    .build();
        }
    }

    /**
     * 订单支付
     *
     * @param orderId
     * @return
     * @throws Exception
     */
    @Override
    public PayVO orderPay(String orderId) throws Exception {
        // 参数校验
        if (StringUtils.isEmpty(orderId)) {
            throw new IllegalArgumentException("订单ID不能为空");
        }

        // 创建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-merchant-token", pubKey);
        headers.put("x-merchant-code", pivKey);
        headers.put("Content-Type", "application/json");

        // 构建请求体
        Map<String, String> params = new HashMap<>();
        params.put("payment", "BALANCE");

        // 构建完整URL
        String url = this.url + "/client/payment/order/pay/" + orderId;

        try {
            // 发送POST请求
            String response = HttpRequest.postJson(url, JSONObject.fromObject(params).toString(), headers);

            log.info("订单支付接口返回原始响应：{}", response);

            // 解析响应结果
            JSONObject jsonResponse = JSONObject.fromObject(response);

            // 先判断是否有 code 字段
            if (!jsonResponse.has("code")) {
                String errorMsg = "支付响应缺少 code 字段，响应：" + response;
                log.error(errorMsg);
                throw new Exception(errorMsg);
            }

            int code = jsonResponse.getInt("code");
            String msg = jsonResponse.getString("msg");

            if (code == 200) {
                JSONObject dataObj = jsonResponse.getJSONObject("data");
                PayData payData = new PayData();
                payData.setPaymentType(dataObj.getString("paymentType"));
                payData.setPaymentName(dataObj.getString("paymentName"));
                payData.setOrderId(dataObj.getInt("orderId"));
                payData.setStatus(dataObj.getInt("status"));
                payData.setPayMessage(dataObj.getString("payMessage"));

                return PayVO.builder()
                        .code(CommonUtil.SUCCESS_CODE)
                        .msg(CommonUtil.SUCCESS_MSG)
                        .data(payData)
                        .build();
            } else {
                log.error("支付订单失败: {}", msg);
                return PayVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(CommonUtil.FAIL_MSG)
                        .build();
            }
        } catch (Exception e) {
            log.error("订单支付异常: {}", e.getMessage(), e);
            throw e;
        }
    }
}