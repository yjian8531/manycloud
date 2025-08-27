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

       //创建订单接口
        String requestUrl = this.url + "/client/order/addVps";
        String response = HttpRequest.postJson(requestUrl, JSONObject.fromObject(params).toString(), headers);
        JSONObject jsonResponse = JSONObject.fromObject(response);

        // 解析订单创建结果
        if (jsonResponse.getInt("code") == 200) {
            String orderId = jsonResponse.getString("data");
            try {
                //创建订单成功后，立即调用支付接口（
                PayVO payVO = orderPay(orderId);
                // 判断支付接口返回的code是否为200
                if ("200".equals(payVO.getCode())) {
                    log.info("订单{}创建成功，且支付成功", orderId);
                    return CreateVO.builder()
                            .code("200")
                            .msg("下单成功，且支付成功")
                            .data(orderId)
                            .build();
                } else {
                    log.info("订单{}创建成功，但支付失败，失败原因：{}", orderId, payVO.getMsg());
                    return CreateVO.builder()
                            .code("200")
                            .msg("下单成功，但支付失败：" + payVO.getMsg())
                            .data(orderId)
                            .build();
                }
            } catch (Exception e) {
                log.error("订单{}创建成功，但支付异常：{}", orderId, e.getMessage(), e);
                return CreateVO.builder()
                        .code("200")
                        .msg("下单成功，但支付过程异常")
                        .data(orderId)
                        .build();
            }
        } else {
            String errorMsg = jsonResponse.getString("msg");
            log.error("购买VPS失败: {}", errorMsg);
            return CreateVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(errorMsg)
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
        // 校验订单编号必填
        if (querySO == null || querySO.getOrderNo() == null) {
            throw new IllegalArgumentException("订单编号（orderNo）为必填参数，不能为空");
        }
        // 创建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-merchant-token", pubKey);
        headers.put("x-merchant-code", pivKey);

        StringBuilder urlBuilder = new StringBuilder(this.url + "/client/order/vpsOrderList");
        boolean isFirstParam = true;
        if (querySO != null) {
            urlBuilder.append(isFirstParam ? "?" : "&").append("orderNo=").append(querySO.getOrderNo());
            isFirstParam = false;
            if (querySO.getPageNum() != null) {
                urlBuilder.append(isFirstParam ? "?" : "&").append("pageNum=").append(querySO.getPageNum());
                isFirstParam = false;
            }
            if (querySO.getPageSize() != null) {
                urlBuilder.append(isFirstParam ? "?" : "&").append("pageSize=").append(querySO.getPageSize());
                isFirstParam = false;
            }
        }
        String requestUrl = urlBuilder.toString();

        try {
            // 发送 GET 请求
            String response = HttpRequest.get(requestUrl, headers);

            log.info("VPS订单列表查询接口返回原始响应：{}", response);

            // 解析响应结果
            JSONObject jsonResponse = JSONObject.fromObject(response);

            if (!jsonResponse.has("code")) {
                String errorMsg = "响应缺少 code 字段，响应：" + response;
                log.error(errorMsg);
                throw new Exception(errorMsg);
            }

            int code = jsonResponse.getInt("code");
            String msg = jsonResponse.getString("msg");
            if (code == 200) {
                int total = jsonResponse.getInt("total");
                JSONArray rowsArray = jsonResponse.getJSONArray("rows");
                Map<String, QueryDetailVO> queryDetailMap = new HashMap<>();
                for (int i = 0; i < rowsArray.size(); i++) {
                    JSONObject row = rowsArray.getJSONObject(i);
                    JSONArray orderItemsArray = row.getJSONArray("orderItems");
                    for (int j = 0; j < orderItemsArray.size(); j++) {
                        JSONObject orderItem = orderItemsArray.getJSONObject(j);
                        if (orderItem.has("vpsCode")) {
                            QueryDetailVO queryDetailVO = new QueryDetailVO();
                            // 映射实例ID，使用vpsCode
                            queryDetailVO.setServiceNo(orderItem.getString("vpsCode"));
                            // 公网IP地址，假设从orderItem中获取vpsIp
                            queryDetailVO.setPublicIp(orderItem.getString("vpsIp"));
                            queryDetailVO.setPrivateIp(orderItem.getString("hostIp"));
                            queryDetailMap.put(orderItem.getString("vpsCode"), queryDetailVO);
                        } else {
                            log.warn("orderItem 中不存在 vpsCode 字段，当前 orderItem 数据：{}", orderItem.toString());
                        }
                    }
                }
                return QueryVO.builder()
                        .code("200")
                        .msg("查询成功")
                        .queryDetailMap(queryDetailMap)
                        .total(total)
                        .build();
            } else {
                log.error("查询VPS订单列表失败: {}", msg);
                return QueryVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(msg)
                        .build();
            }
        } catch (Exception e) {
            log.error("查询VPS订单列表异常: {}", e.getMessage(), e);
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
        // 校验vpsCode必填
        if (querySO == null || querySO.getVpsCode() == null) {
            throw new IllegalArgumentException("vpsCode 为必填参数，不能为空");
        }
        // 创建请求头
        Map<String, String> headers = new HashMap<>();
        headers.put("x-merchant-token", pubKey);
        headers.put("x-merchant-code", pivKey);


        StringBuilder urlBuilder = new StringBuilder(this.url + "/client/vps/list");
        boolean isFirstParam = true;
        if (querySO != null) {
            // 因为vpsCode必填，所以直接拼接
            urlBuilder.append(isFirstParam ? "?" : "&").append("vpsCode=").append(querySO.getVpsCode());
            isFirstParam = false;
            if (querySO.getPageNum() != null) {
                urlBuilder.append(isFirstParam ? "?" : "&").append("pageNum=").append(querySO.getPageNum());
                isFirstParam = false;
            }
            if (querySO.getPageSize() != null) {
                urlBuilder.append(isFirstParam ? "?" : "&").append("pageSize=").append(querySO.getPageSize());
                isFirstParam = false;
            }
        }

        String requestUrl = urlBuilder.toString();

        try {
            // 发送 GET 请求
            String response = HttpRequest.get(requestUrl, headers);

            log.info("VPS列表查询接口返回原始响应：{}", response);

            // 解析响应结果
            JSONObject jsonResponse = JSONObject.fromObject(response);

            if (!jsonResponse.has("code")) {
                String errorMsg = "响应缺少 code 字段，响应：" + response;
                log.error(errorMsg);
                throw new Exception(errorMsg);
            }

            int code = jsonResponse.getInt("code");
            String msg = jsonResponse.getString("msg");

            if (code == 200) {
                int total = jsonResponse.getInt("total");
                JSONArray rowsArray = jsonResponse.getJSONArray("rows");

                Map<String, QueryDetailVO> queryDetailMap = new HashMap<>();
                for (int i = 0; i < rowsArray.size(); i++) {
                    JSONObject row = rowsArray.getJSONObject(i);
                    QueryDetailVO queryDetailVO = new QueryDetailVO();
                    // 映射实例ID，使用vpsCode
                    queryDetailVO.setServiceNo(row.getString("vpsCode"));
                    // 公网IP地址
                    queryDetailVO.setPublicIp(row.getString("vpsIp"));
                    // 账号，使用sshAccount
                    queryDetailVO.setAccount(row.getString("sshAccount"));
                    // 端口，使用sshPort
                    queryDetailVO.setPort(row.getInt("sshPort"));
                    // 密码，使用sshPwd
                    queryDetailVO.setPwd(row.getString("sshPwd"));

                    // 处理状态转换
                    int serverStatus = row.getInt("status");
                    if (serverStatus == 20) {
                        queryDetailVO.setStatus(1); // 成功
                    } else if (serverStatus == 0) {
                        queryDetailVO.setStatus(0); // 待定
                    } else {
                        queryDetailVO.setStatus(2); // 失败
                    }

                    // 根据status设置powerState
                    if (serverStatus == 20) {
                        queryDetailVO.setPowerState("running");
                    } else if (serverStatus == 0) {
                        queryDetailVO.setPowerState("execution");
                    } else {
                        queryDetailVO.setPowerState("halted");
                    }

                    queryDetailMap.put(row.getString("vpsCode"), queryDetailVO);
                }

                return QueryVO.builder()
                        .code("200")
                        .msg("查询成功")
                        .queryDetailMap(queryDetailMap)
                        .total(total)
                        .build();
            } else {
                log.error("查询VPS列表失败: {}", msg);
                return QueryVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(msg)
                        .build();
            }
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
        // 返回响应结果
        return RenewVO.builder()
                .code(responseJson.getString("code"))
                .msg(responseJson.getString("msg"))
                .data(responseJson.get("data"))
                .build();
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
            JSONObject jsonResponse = JSONObject.fromObject(response);

            // 从响应中获取code和msg字段
            String code = jsonResponse.getString("code");
            String msg = jsonResponse.getString("msg");

            // 封装并返回结果
            return StartVO.builder()
                    .code(code)
                    .msg(msg)
                    .build();
        } catch (IOException e) {
            log.error("启动VPS接口调用失败：{}", e.getMessage(), e);
            return StartVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg("启动VPS失败：" + e.getMessage())
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
            log.info("启动VPS接口返回原始响应：{}", response);

            // 解析响应结果
            JSONObject jsonResponse = JSONObject.fromObject(response);

            // 从响应中获取code和msg字段
            String code = jsonResponse.getString("code");
            String msg = jsonResponse.getString("msg");

            // 封装并返回结果
            return StopVO.builder()
                    .code(code)
                    .msg(msg)
                    .build();
        } catch (IOException e) {
            log.error("关闭VPS接口调用失败：{}", e.getMessage(), e);
            // 异常时返回失败信息
            return StopVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg("关闭VPS失败：" + e.getMessage())
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
            log.info("启动VPS接口返回原始响应：{}", response);

            // 解析响应结果
            JSONObject jsonResponse = JSONObject.fromObject(response);

            // 从响应中获取code和msg字段
            String code = jsonResponse.getString("code");
            String msg = jsonResponse.getString("msg");

            // 封装并返回结果
            return DestroyVO.builder()
                    .code(code)
                    .msg(msg)
                    .build();
        } catch (IOException e) {
            log.error("关闭VPS接口调用失败：{}", e.getMessage(), e);
            // 异常时返回失败信息
            return DestroyVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg("关闭VPS失败：" + e.getMessage())
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
                    .code("200")
                    .msg("查询成功")
                    .rows(rows)
                    .total(total)
                    .build();
        } else {
            String errorMsg = jsonResponse.getString("msg");
            log.error("查询集群列表失败: {}", errorMsg);
            return ClusterVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(errorMsg)
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
                    .code("200")
                    .msg("查询成功")
                    .total(total)
                    .rows(rows)
                    .build();
        } else {
            String errorMsg = jsonResponse.getString("msg");
            log.error("查询集群模板列表失败: {}", errorMsg);
            return TemplateListVO.builder()
                    .code(CommonUtil.FAIL_CODE)
                    .msg(errorMsg)
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
                        .code("200")
                        .msg("操作成功")
                        .data(payData)
                        .build();
            } else {
                log.error("支付订单失败: {}", msg);
                return PayVO.builder()
                        .code(CommonUtil.FAIL_CODE)
                        .msg(msg)
                        .build();
            }
        } catch (Exception e) {
            log.error("订单支付异常: {}", e.getMessage(), e);
            throw e;
        }
    }
}