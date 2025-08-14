package com.core.manycloudtimer.job;

import com.core.manycloudcommon.caller.AkamaiCaller;
import com.core.manycloudcommon.caller.RcloudCaller;
import com.core.manycloudcommon.caller.UcloudCaller;
import com.core.manycloudcommon.entity.NodeImage;
import com.core.manycloudcommon.entity.NodeInfo;
import com.core.manycloudcommon.entity.PlatformAccount;
import com.core.manycloudcommon.enums.PlatformLabelEnum;
import com.core.manycloudcommon.mapper.NodeImageMapper;
import com.core.manycloudcommon.mapper.NodeInfoMapper;
import com.core.manycloudcommon.mapper.PlatformAccountMapper;
import com.core.manycloudcommon.model.AccountApi;
import com.core.manycloudcommon.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 定时任务 定时去更新不同平台的地域
 */
@Slf4j
@Component
@EnableScheduling
public class UpdateRegionTimer {


    @Autowired
    private NodeImageMapper nodeImageMapper;
    @Autowired
    private PlatformAccountMapper platformAccountMapper;

    @Autowired
    private NodeInfoMapper nodeInfoMapper;


    // 定义需要去除的常规系统前缀
    private static final List<String> SYSTEM_PREFIXES = Arrays.asList(
            "Windows ", "CentOS ", "Ubuntu ", "Debian ", "Server " // 常规前缀
    );
    /**
     * Ucloud每天凌晨三点触发定时任务
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void updateUcloudImageInfo() {
        try {
            log.info("开始执行镜像信息更新任务");

            //获取所有 UCLOUD 节点
            List<NodeInfo> nodeInfos = nodeInfoMapper.selectByLabel(PlatformLabelEnum.UCLOUD.getLabel());
            log.info("通过 UCLOUD 标签获取到 {} 个节点", nodeInfos.size());

            for (NodeInfo nodeInfo : nodeInfos) {
                log.info("处理节点: ID={}, 名称={}, 区域={}",
                        nodeInfo.getId(), nodeInfo.getNodeName(), nodeInfo.getNodeVal());

                // 获取平台账号
                PlatformAccount platformAccount = platformAccountMapper.selectDefault(nodeInfo.getLabel());
                if (platformAccount == null) {
                    log.warn("未找到节点 {} 的平台账号", nodeInfo.getNodeName());
                    continue;
                }

                // 构建账户 API 信息
                String projectId = null;
                if (StringUtils.isNotEmpty(nodeInfo.getNodeParam())) {
                    JSONObject param = JSONObject.fromObject(nodeInfo.getNodeParam());
                    //projectId 字段存在性判断
                    if (param.containsKey("projectId")) {
                        projectId = param.getString("projectId");
                    }
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

                // 获取 UCloud Caller 实例
                UcloudCaller caller = UcloudCaller.getClient(accountApi);

                // 获取 UCloud 镜像信息
                String imageInfo = caller.getImage();

                // 打印数据
                try {
                    JSONObject rawJson = JSONObject.fromObject(imageInfo);
                    log.info("UCloud 镜像原始数据: \n{}", rawJson.toString(2));
                } catch (Exception e) {
                    log.info("UCloud 镜像原始数据（非 JSON 格式）: \n{}", imageInfo);
                }

                // 解析 JSON 数据
                JSONObject imageJson = JSONObject.fromObject(imageInfo);

                // 检查 RetCode 是否为 0（表示成功）
                if (!imageJson.containsKey("RetCode") || imageJson.getInt("RetCode") != 0) {
                    log.error("UCloud API 调用失败，错误信息: \n{}", imageJson.toString(2));
                    continue;
                }

                // 检查是否有 ImageSet 字段且为 JSONArray
                if (!imageJson.containsKey("ImageSet")) {
                    log.warn("返回数据中没有找到 ImageSet 字段，完整返回数据: \n{}", imageJson.toString(2));
                    continue;
                }

                Object infosObj = imageJson.get("ImageSet");
                if (!(infosObj instanceof JSONArray)) {
                    log.warn("ImageSet 字段不是 JSONArray 类型，实际类型: {}，值: \n{}",
                            infosObj.getClass().getName(), formatJsonObject(infosObj));
                    continue;
                }

                JSONArray images = (JSONArray) infosObj;
                log.info("共解析到 {} 条镜像数据", images.size());

                // 根据节点 ID 查询不同节点的多个版本
                List<NodeImage> nodeImages = nodeImageMapper.selectByNode(nodeInfo.getId());
                log.info("根据节点 ID {} 查询到 {} 个镜像版本", nodeInfo.getId(), nodeImages.size());

                //根据这些版本获取对应的镜像 ID 去修改节点镜像 ID
                for (NodeImage nodeImage : nodeImages) {
                    if (nodeImage == null) {
                        log.warn("发现空的 NodeImage 对象，跳过处理");
                        continue;
                    }

                    String imageVersion = nodeImage.getImageVersion();
                    log.info("处理数据库镜像版本: {}", imageVersion);

                    if (StringUtils.isEmpty(imageVersion)) {
                        log.warn("镜像版本为空，跳过处理");
                        continue;
                    }

                    // 遍历 API 镜像，匹配版本
                    JSONObject targetImage = null;
                    for (int i = 0; i < images.size(); i++) {
                        JSONObject image = images.getJSONObject(i);
                        String apiOsName = image.getString("OsName");

                        // 调用工具方法，提取版本
                        String apiVersion = extractVersion(apiOsName);
                        log.debug("API原始OsName: [{}]，提取后版本: [{}]，数据库版本: [{}]",
                                apiOsName, apiVersion, imageVersion);

                        if (imageVersion.equals(apiVersion)) {
                            targetImage = image;
                            break;
                        }
                    }

                    if (targetImage == null) {
                        log.warn("未找到镜像版本 {} 的记录", imageVersion);
                        continue;
                    }

                    // 获取新镜像 ID 并清理
                    String newImageId = targetImage.getString("ImageId");
                    String dbImageId = nodeImage.getImageParam();

                    //去除首尾空格，处理 null 情况
                    String cleanDbId = (dbImageId != null) ? dbImageId.trim() : "";
                    String cleanApiId = (newImageId != null) ? newImageId.trim() : "";

                    log.info("镜像ID对比 - 数据库值: [{}]（长度: {}），API值: [{}]（长度: {}）",
                            cleanDbId, cleanDbId.length(),
                            cleanApiId, cleanApiId.length());

                    // 比较清理后的字符串
                    if (!cleanDbId.equals(cleanApiId)) {
                        log.info("准备更新镜像记录 - SQL: UPDATE t_node_image SET image_param = '{}' WHERE id = {}",
                                cleanApiId, nodeImage.getId());
                        nodeImage.setImageParam(cleanApiId); // 存储清理后的值
                        nodeImageMapper.updateByPrimaryKeySelective(nodeImage);
                        log.info("已更新镜像版本 {}，新镜像 ID：{}", imageVersion, cleanApiId);
                    } else {
                        log.info("镜像版本 {} 已是最新，无需更新", imageVersion);
                    }
                }
            }
            log.info("镜像信息更新任务执行完成");
        } catch (Exception e) {
            log.error("更新镜像信息失败", e);
        }
    }
    /**
     *Rcloud每天凌晨4点执行触发
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void updateRcloudImageInfo() {
        try {
            log.info("开始执行 Rcloud 镜像信息更新任务");

            //获取所有 Rcloud 节点
            List<NodeInfo> nodeInfos = nodeInfoMapper.selectByLabel(PlatformLabelEnum.RCLOUD.getLabel());
            log.info("通过 RCLOUD 标签获取到 {} 个节点", nodeInfos.size());

            for (NodeInfo nodeInfo : nodeInfos) {
                log.info("处理节点: ID={}, 名称={}, 区域={}",
                        nodeInfo.getId(), nodeInfo.getNodeName(), nodeInfo.getNodeVal());

                // 获取平台账号
                PlatformAccount platformAccount = platformAccountMapper.selectDefault(nodeInfo.getLabel());
                if (platformAccount == null) {
                    log.warn("未找到节点 {} 的平台账号，跳过处理", nodeInfo.getNodeName());
                    continue;
                }

                // 构建 Rcloud 账户 API 信息（Rcloud 无 projectId，直接设为 null）
                String projectId = null;

                AccountApi accountApi = AccountApi.builder()
                        .regionId(nodeInfo.getNodeVal())
                        .label(nodeInfo.getLabel())
                        .account(platformAccount.getAccount())
                        .keyNo(platformAccount.getKeyNo())
                        .keySecret(platformAccount.getKeySecret())
                        .baseUrl(platformAccount.getUrl())
                        .projectId(projectId)
                        .build();

                // 获取 Rcloud Caller 实例并调用 API
                RcloudCaller caller = RcloudCaller.getClient(accountApi);
                String imageInfo = caller.getImage();

                //打印数据
                try {
                    JSONObject rawJson = JSONObject.fromObject(imageInfo);
                    log.info("Rcloud 镜像原始数据: \n{}", rawJson.toString(2));
                } catch (Exception e) {
                    log.info("Rcloud 镜像原始数据（非 JSON 格式）: \n{}", imageInfo);
                }

                // 解析 JSON 响应
                JSONObject imageJson = JSONObject.fromObject(imageInfo);

                // 检查 API 调用是否成功（RetCode=0 为成功）
                if (!imageJson.containsKey("RetCode") || imageJson.getInt("RetCode") != 0) {
                    log.error("Rcloud API 调用失败，错误信息: \n{}", imageJson.toString(2));
                    continue;
                }

                // 验证 ImageSet 字段是否为 JSONArray
                if (!imageJson.containsKey("ImageSet")) {
                    log.warn("返回数据中无 ImageSet 字段，完整数据: \n{}", imageJson.toString(2));
                    continue;
                }

                Object infosObj = imageJson.get("ImageSet");
                if (!(infosObj instanceof JSONArray)) {
                    log.warn("ImageSet 不是 JSONArray，实际类型: {}，值: \n{}",
                            infosObj.getClass().getName(), formatJsonObject(infosObj));
                    continue;
                }

                JSONArray images = (JSONArray) infosObj;
                log.info("共解析到 {} 条镜像数据", images.size());

                // 查询节点关联的镜像版本
                List<NodeImage> nodeImages = nodeImageMapper.selectByNode(nodeInfo.getId());
                log.info("节点 ID {} 关联 {} 个镜像版本", nodeInfo.getId(), nodeImages.size());

                // 遍历版本，匹配并更新镜像 ID
                for (NodeImage nodeImage : nodeImages) {
                    if (nodeImage == null) {
                        log.warn("空的 NodeImage 对象，跳过处理");
                        continue;
                    }

                    String imageVersion = nodeImage.getImageVersion();
                    log.info("处理数据库镜像版本: {}", imageVersion);

                    if (StringUtils.isEmpty(imageVersion)) {
                        log.warn("镜像版本为空，跳过处理");
                        continue;
                    }

                    // 遍历 API 镜像，匹配版本
                    JSONObject targetImage = null;
                    for (int i = 0; i < images.size(); i++) {
                        JSONObject image = images.getJSONObject(i);
                        String apiOsName = image.getString("OsName");

                        String apiVersion = extractVersion(apiOsName);
                        log.debug("API原始OsName: [{}]，提取后版本: [{}]，数据库版本: [{}]",
                                apiOsName, apiVersion, imageVersion);

                        if (imageVersion.equals(apiVersion)) {
                            targetImage = image;
                            break;
                        }
                    }

                    if (targetImage == null) {
                        log.warn("未找到与版本 {} 匹配的镜像记录", imageVersion);
                        continue;
                    }

                    // 获取新镜像 ID 并清理
                    String newImageId = targetImage.getString("ImageId");
                    String dbImageId = nodeImage.getImageParam();

                    // 去除首尾空格，处理 null 情况
                    String cleanDbId = (dbImageId != null) ? dbImageId.trim() : "";
                    String cleanApiId = (newImageId != null) ? newImageId.trim() : "";

                    log.info("镜像ID对比 - 数据库值: [{}]（长度: {}），API值: [{}]（长度: {}）",
                            cleanDbId, cleanDbId.length(),
                            cleanApiId, cleanApiId.length());

                    // 比较清理后的字符串
                    if (!cleanDbId.equals(cleanApiId)) {
                        log.info("准备更新：UPDATE t_node_image SET image_param = '{}' WHERE id = {}",
                                cleanApiId, nodeImage.getId());
                        nodeImage.setImageParam(cleanApiId); // 存储清理后的值，避免后续差异
                        nodeImageMapper.updateByPrimaryKeySelective(nodeImage);
                        log.info("已更新镜像版本 {}，新镜像 ID：{}", imageVersion, cleanApiId);
                    } else {
                        log.info("镜像版本 {} 已是最新，无需更新", imageVersion);
                    }
                }
            }
            log.info("Rcloud 镜像信息更新任务执行完成");
        } catch (Exception e) {
            log.error("Rcloud 镜像更新失败", e);
        }
    }

    //静态变量缓存首次获取的镜像数据
    private static String cachedImageInfo = null;
    private static JSONArray cachedImages = null;
    private static boolean isFirstLoad = true; // 标记是否首次加载

    /**
     * AKM平台定时更新镜像
     * @return
     */
    @Scheduled(cron = "0 0 5 * * ?")
    public void updateAKMImageInfo() {
        try {
            log.info("开始执行 AKMPLA 镜像信息更新任务");

            //获取所有 AKMPLA 节点
            List<NodeInfo> nodeInfos = nodeInfoMapper.selectByLabel(PlatformLabelEnum.AKMPLA.getLabel());
            log.info("通过 AKMPLA 标签获取到 {} 个节点", nodeInfos.size());

            // 首次加载时获取一次数据并缓存，后续直接使用缓存
            if (isFirstLoad && !nodeInfos.isEmpty()) {
                // 使用第一个节点的账号信息获取数据
                NodeInfo firstNode = nodeInfos.get(0);
                PlatformAccount firstAccount = platformAccountMapper.selectDefault(firstNode.getLabel());
                if (firstAccount != null) {
                    AccountApi firstApi = AccountApi.builder()
                            .regionId(firstNode.getNodeVal())
                            .label(firstNode.getLabel())
                            .account(firstAccount.getAccount())
                            .keyNo(firstAccount.getKeyNo())
                            .keySecret(firstAccount.getKeySecret())
                            .baseUrl(firstAccount.getUrl())
                            .projectId(null)
                            .build();

                    AkamaiCaller firstCaller = AkamaiCaller.getClient(firstApi);
                    cachedImageInfo = firstCaller.getImage();

                    // 解析并缓存images数据
                    try {
                        JSONObject firstResponse = JSONObject.fromObject(cachedImageInfo);
                        if (firstResponse.containsKey("data") && firstResponse.get("data") instanceof JSONArray) {
                            cachedImages = (JSONArray) firstResponse.get("data");
                            log.info("已缓存AKM镜像数据，共 {} 条", cachedImages.size());
                        }
                    } catch (Exception e) {
                        log.error("缓存AKM镜像数据失败", e);
                    }
                }
                isFirstLoad = false; // 标记为已加载
            }

            for (NodeInfo nodeInfo : nodeInfos) {
                log.info("处理节点: ID={}, 名称={}, 区域={}",
                        nodeInfo.getId(), nodeInfo.getNodeName(), nodeInfo.getNodeVal());

                // 获取平台账号
                PlatformAccount platformAccount = platformAccountMapper.selectDefault(nodeInfo.getLabel());
                if (platformAccount == null) {
                    log.warn("未找到节点 {} 的平台账号，跳过处理", nodeInfo.getNodeName());
                    continue;
                }

                // 直接使用缓存的镜像数据，不再调用API
                String imageInfo = cachedImageInfo;
                JSONArray images = cachedImages;

                // 打印数
                try {
                    if (imageInfo != null) {
                        Object rawObj = JSONObject.fromObject(imageInfo);
                        log.info("AKMPLA 镜像数据（来自缓存）: \n{}", formatJsonObject(rawObj));
                    }
                } catch (Exception e) {
                    if (imageInfo != null) {
                        log.info("AKMPLA 镜像数据（来自缓存，非JSON格式）: \n{}", imageInfo);
                    }
                }

                // 解析 JSON 响应（使用缓存数据）
                if (images == null && imageInfo != null) {
                    try {
                        JSONObject responseObj = JSONObject.fromObject(imageInfo);
                        if (responseObj.containsKey("data") && responseObj.get("data") instanceof JSONArray) {
                            images = (JSONArray) responseObj.get("data");
                        } else {
                            log.warn("响应中未找到 data 字段或 data 不是 JSONArray，完整响应: \n{}", responseObj.toString(2));
                            continue;
                        }
                    } catch (Exception e) {
                        log.error("解析 AKMPLA 镜像数据失败", e);
                        continue;
                    }
                } else if (images == null) {
                    log.warn("无有效镜像数据，跳过节点处理");
                    continue;
                }

                log.info("共解析到 {} 条镜像数据", images.size());

                // 查询节点关联的镜像版本
                List<NodeImage> nodeImages = nodeImageMapper.selectByNode(nodeInfo.getId());
                log.info("节点 ID {} 关联 {} 个镜像版本", nodeInfo.getId(), nodeImages.size());

                // 遍历版本，匹配并更新镜像 ID
                for (NodeImage nodeImage : nodeImages) {
                    if (nodeImage == null) {
                        log.warn("空的 NodeImage 对象，跳过处理");
                        continue;
                    }

                    String imageVersion = nodeImage.getImageVersion();
                    log.info("处理数据库镜像版本: {}", imageVersion);

                    if (StringUtils.isEmpty(imageVersion)) {
                        log.warn("镜像版本为空，跳过处理");
                        continue;
                    }

                    // 遍历 API 镜像，匹配版本
                    JSONObject targetImage = null;
                    for (int i = 0; i < images.size(); i++) {
                        JSONObject image = images.getJSONObject(i);
                        String apiLabel = image.getString("label"); // AKM 的版本信息在 label 字段

                        // 打印原始 label 和提取后的值
                        log.debug("API label 原始值: [{}]", apiLabel);
                        String apiVersion = extractVersion(apiLabel);
                        log.debug("extractVersion 处理后: [{}]", apiVersion);

                        log.debug("API label原始值: [{}]，提取后版本: [{}]，数据库版本: [{}]",
                                apiLabel, apiVersion, imageVersion);

                        // 转小写 + 去重空格
                        String dbVersionNormalized = imageVersion.toLowerCase().replaceAll("\\s+", " ");
                        String apiVersionNormalized = apiVersion.toLowerCase().replaceAll("\\s+", " ");

                        if (dbVersionNormalized.equals(apiVersionNormalized)) {
                            targetImage = image;
                            break;
                        }
                    }

                    if (targetImage == null) {
                        log.warn("未找到与版本 {} 匹配的镜像记录", imageVersion);
                        continue;
                    }

                    // 获取新镜像 ID
                    String newImageId = targetImage.getString("id");
                    String dbImageId = nodeImage.getImageParam();

                    // 去除首尾空格，处理 null 情况
                    String cleanDbId = (dbImageId != null) ? dbImageId.trim() : "";
                    String cleanApiId = (newImageId != null) ? newImageId.trim() : "";


                    log.info("镜像ID对比 - 数据库值: [{}]（长度: {}），API值: [{}]（长度: {}）",
                            cleanDbId, cleanDbId.length(),
                            cleanApiId, cleanApiId.length());

                    // 比较清理后的字符串，执行更新
                    if (!cleanDbId.equals(cleanApiId)) {
                        log.info("准备更新：UPDATE t_node_image SET image_param = '{}' WHERE id = {}",
                                cleanApiId, nodeImage.getId());
                        nodeImage.setImageParam(cleanApiId); // 存储清理后的值
                        nodeImageMapper.updateByPrimaryKeySelective(nodeImage); // 确保数据库更新执行
                        log.info("已更新镜像版本 {}，新镜像 ID：{}", imageVersion, cleanApiId);
                    } else {
                        log.info("镜像版本 {} 已是最新，无需更新", imageVersion);
                    }
                }
            }
            log.info("AKMPLA 镜像信息更新任务执行完成");
        } catch (Exception e) {
            log.error("更新 AKMPLA 镜像信息失败", e);
        }
    }

    // 提取版本兼容高内核和常规格式
    private String extractVersion(String osName) {
        if (osName == null) {
            return "";
        }
        // 优先处理高内核开头的格式
        if (osName.startsWith("高内核")) {
            // 正则匹配：高内核 + 可选系统名 + 版本
            Pattern pattern = Pattern.compile("^高内核(?:CentOS|Ubuntu|Windows|Debian|Server)?\\s+(.*)$");
            Matcher matcher = pattern.matcher(osName);
            if (matcher.find()) {
                return "高内核 " + matcher.group(1).trim();
            }
        }

        // 处理 Kubernetes 格式
        Pattern kubePattern = Pattern.compile("^(Kubernetes\\s+[\\d.]+\\s+on)\\s+\\w+\\s+([\\d.]+)$");
        Matcher kubeMatcher = kubePattern.matcher(osName);
        if (kubeMatcher.find()) {
            return kubeMatcher.group(1) + " " + kubeMatcher.group(2);
        }

        // 处理常规系统前缀如 Windows、CentOS 等
        for (String prefix : SYSTEM_PREFIXES) {
            if (osName.startsWith(prefix)) {
                return osName.substring(prefix.length());
            }
        }

        return osName;
    }

    /**
     * 格式化JSON对象
     *
     * @param obj JSON对象
     * @return 格式化后的字符串
     */
    private String formatJsonObject(Object obj) {
        try {
            if (obj == null) {
                return "null";
            }
            if (obj instanceof JSONObject) {
                return ((JSONObject) obj).toString(2);
            } else if (obj instanceof JSONArray) {
                return ((JSONArray) obj).toString(2);
            } else {
                return obj.toString();
            }
        } catch (Exception e) {
            return obj.toString();
        }
    }
}