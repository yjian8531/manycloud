package com.core.manycloudadmin.service.impl;

import com.core.manycloudadmin.service.UserService;
import com.core.manycloudadmin.so.user.QueryUserListSO;
import com.core.manycloudadmin.so.user.UpdateUserFinanceSO;
import com.core.manycloudadmin.so.user.UpdateUserRemarkSO;
import com.core.manycloudadmin.so.util.DateUtil;
import com.core.manycloudadmin.util.WeiXinCaller;
import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.QueryCommissionStatisticsVO;

import com.core.manycloudcommon.caller.vo.QueryUserProListVOTO;
import com.core.manycloudcommon.caller.vo.UserNumVO;
import com.core.manycloudcommon.entity.FinanceDetail;
import com.core.manycloudcommon.entity.PowerBinding;
import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.vo.finance.QueryCommissionDetailListVO;
import com.core.manycloudcommon.vo.finance.UserProductNumVO;
import com.core.manycloudcommon.vo.user.UserListVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.utils.StringUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserFinanceMapper userFinanceMapper;

    @Autowired
    private FinanceDetailMapper financeDetailMapper;

    @Autowired
    private InstanceInfoMapper instanceInfoMapper;

    @Autowired
    private WeiXinCaller weiXinCaller;

    @Autowired
    private UserProMapper userProMapper;


    @Autowired
    private CommissionDetailMapper commissionDetailMapper;

    @Autowired
    private AdminInfoMapper adminInfoMapper;

    /**
     * 查询用户列表
     * @param queryUserListSO
     * @return
     */
    public ResultMessage queryList(QueryUserListSO queryUserListSO){

        PageHelper.startPage(queryUserListSO.getPage(), queryUserListSO.getPageSize());
        Page<UserListVO> page = (Page<UserListVO>)userInfoMapper.selectList(queryUserListSO.getAccount(),queryUserListSO.getNick(),queryUserListSO.getStartTime(),queryUserListSO.getEndTime());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);

    }


    /**
     * 更新用户备注信息
     * @param updateUserRemarkSO
     * @return
     */
    public ResultMessage updateUserRemark(UpdateUserRemarkSO updateUserRemarkSO){
        UserInfo userInfo = userInfoMapper.selectById(updateUserRemarkSO.getUserId());
        userInfo.setRemark(updateUserRemarkSO.getRemar());
        userInfo.setUpdateTime(new Date());
        int i = userInfoMapper.updateByPrimaryKeySelective(userInfo);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"更新成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"更新失败");
        }
    }


    /**
     * 管理员更新用户余额
     * @param updateUserFinanceSO
     * @return
     */
    public ResultMessage updateUserFinance(UpdateUserFinanceSO updateUserFinanceSO){

        if("minus".equals(updateUserFinanceSO.getTad())){ //扣除金额 先冻结
            userFinanceMapper.updateBalanceByUserId(updateUserFinanceSO.getUserId(), "seal",updateUserFinanceSO.getMoneyNum());
        }
        int i = userFinanceMapper.updateBalanceByUserId(updateUserFinanceSO.getUserId(), updateUserFinanceSO.getTad(),updateUserFinanceSO.getMoneyNum());
        if(i > 0){
            //添加账单记录
            FinanceDetail financeDetail = new FinanceDetail();
            financeDetail.setUserId(updateUserFinanceSO.getUserId());
            financeDetail.setFinanceNo(CommonUtil.getRandomStr(12));
            if("add".equals(updateUserFinanceSO.getTad())){
                financeDetail.setType(0);//类型操作
                financeDetail.setDirection(0);//收入
            }else if("minus".equals(updateUserFinanceSO.getTad())){
                financeDetail.setType(1);//类型消费
                financeDetail.setDirection(1);//支出
            }else{
                return new ResultMessage(ResultMessage.SUCCEED_CODE,"操作成功");
            }
            financeDetail.setMoneyNum(updateUserFinanceSO.getMoneyNum());
            financeDetail.setTag("manage");//人工操作
            financeDetail.setWay(2);//交易方式(0:支付宝,1:微信,2:账号余额)
            financeDetail.setStatus(CommonUtil.STATUS_1);//交易完成
            financeDetail.setRemarks(updateUserFinanceSO.getRemark());
            financeDetail.setCreateTime(new Date());
            financeDetail.setUpdateTime(new Date());
            financeDetailMapper.insertSelective(financeDetail);


            if("add".equals(updateUserFinanceSO.getTad())){
                UserInfo userInfo = userInfoMapper.selectById(updateUserFinanceSO.getUserId());
                /** 充值到账微信公众号通知 **/
                weiXinCaller.sendRechargeSuccess(userInfo.getUserId(),userInfo.getAccount(),updateUserFinanceSO.getMoneyNum(),1,financeDetail.getUpdateTime());
            }

            return new ResultMessage(ResultMessage.SUCCEED_CODE,"操作成功");

        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"操作失败");
        }
    }

    /**
     * 用户统计曲线图数据（新增、活跃、失活）
     * @return 统计结果
     */
    public ResultMessage queryTotalUser(UserStatsSo userStatsSo) {
        String formatStr = null;
        String dbFormatStr = null;

        // 从实体类获取时间粒度参数
        if ("year".equals(userStatsSo.getTimeUnit())) {
            dbFormatStr = "%Y";
            formatStr = "yyyy";
        } else if ("month".equals(userStatsSo.getTimeUnit())) {
            dbFormatStr = "%Y-%m";
            formatStr = "yyyy-MM";
        } else if ("day".equals(userStatsSo.getTimeUnit())) {
            dbFormatStr = "%Y-%m-%d";
            formatStr = "yyyy-MM-dd";
        } else {
            return new ResultMessage("400", "时间粒度必须是 year/month/day", null);
        }

        // 处理结束时间
        String endTime = userStatsSo.getEndTime();
        if (endTime == null || endTime.trim().isEmpty()) {
            endTime = DateUtil.format(new Date(), formatStr);
        }

        // 处理开始时间
        String startTime = userStatsSo.getStartTime();
        if (startTime == null || startTime.trim().isEmpty()) {
            int offset = "day".equals(userStatsSo.getTimeUnit()) ? 30 :
                    ("month".equals(userStatsSo.getTimeUnit()) ? 12 : 1);
            try {
                Date endDate = DateUtil.parse(endTime, formatStr);
                Date startDate = "day".equals(userStatsSo.getTimeUnit()) ?
                        DateUtil.offsetDay(endDate, -offset) :
                        "month".equals(userStatsSo.getTimeUnit()) ?
                                DateUtil.offsetMonth(endDate, -offset) :
                                DateUtil.offsetYear(endDate, -offset);
                startTime = DateUtil.format(startDate, formatStr);
            } catch (ParseException e) {
                return new ResultMessage("500", "日期解析失败", null);
            }
        }

        // 生成时间区间列表
        List<String> dateList;
        try {
            dateList = DateUtil.rangeToList(
                    DateUtil.parse(startTime, formatStr),
                    DateUtil.parse(endTime, formatStr),
                    userStatsSo.getTimeUnit(),
                    formatStr
            );
        } catch (ParseException e) {
            return new ResultMessage("500", "生成时间区间失败", null);
        }
        if (dateList == null || dateList.isEmpty()) {
            return new ResultMessage("400", "开始时间不能晚于结束时间", null);
        }

        // 结果数据结构
        List<Integer> newUsersList = new ArrayList<>();
        List<Integer> activeUsersList = new ArrayList<>();
        List<Integer> inactiveUsersList = new ArrayList<>();

        // 汇总统计
        int totalNewUsers = 0;
        int totalActiveUsers = 0;
        int totalInactiveUsers = 0;
        int cumulativeActiveUsers = 0; // 累计活跃用户计数器

        for (String dateStr : dateList) {
            // 查询新增用户
            Integer createNum = userInfoMapper.queryCreateNum(dbFormatStr, dateStr);
            createNum = createNum == null ? 0 : createNum;
            newUsersList.add(createNum);
            totalNewUsers += createNum;

            // 查询活跃用户
            Integer activeNum = userInfoMapper.queryActiveNumNEW(dateStr);
            activeNum = activeNum == null ? 0 : activeNum;
            activeUsersList.add(activeNum);
            totalActiveUsers += activeNum;

            // 查询总用户数并据此计算失活用户数
            Integer totalUsersToDate = userInfoMapper.queryTotalUsers();  // 新增的查询
            totalUsersToDate = totalUsersToDate == null ? 0 : totalUsersToDate;
            Integer inactiveNum = totalUsersToDate - activeNum;  // 计算失活
            if (inactiveNum < 0) inactiveNum = 0;  // 防止负数
            inactiveUsersList.add(inactiveNum);

        }

        // 区间内去重 活跃用户总数
        Integer activeTotal = userInfoMapper.queryActiveUsersCount(startTime, endTime);
        //用户总数
        Integer totalUsers = userInfoMapper.queryTotalUsers();
        //失活用户总数
        Integer inactiveTotal = totalUsers - activeTotal;

        // 构建响应数据
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dates", dateList);
        data.put("newUsers", newUsersList);
        data.put("activeUsers", activeUsersList);
        data.put("inactiveUsers", inactiveUsersList);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("newUsers", totalNewUsers);//新增用户总数
        summary.put("activeTotal", activeTotal); //活跃用户总数
        summary.put("inactiveUsers", inactiveTotal); //失活用户总数
        data.put("summary", summary);

        return new ResultMessage(ResultMessage.SUCCEED_CODE, ResultMessage.SUCCEED_MSG, data);
    }

    /**
     * 分页查询推广统计列表(管理后台)
     * @param
     * @return
     */
    @Override
    public ResultMessage queryCommissionStatistics(QueryCommissionStatisticsSO queryCommissionStatisticsSO) {
        // 1. 分页初始化
        PageHelper.startPage(queryCommissionStatisticsSO.getPageNum(), queryCommissionStatisticsSO.getPageSize());

       // 2. 查询推广用户基础信息
        Page<QueryCommissionStatisticsVO> page = (Page<QueryCommissionStatisticsVO>) userProMapper.selectListByStatistics(queryCommissionStatisticsSO.getAccount());
        List<QueryCommissionStatisticsVO> list = page.getResult();
        Map<String, Object> resultMap = new HashMap<>();

        if (list.size() > 0) {
            List<String> promoterIds = list.stream()
                    .map(QueryCommissionStatisticsVO::getUserId)
                    .collect(Collectors.toList());

            //通过推广人ID查询其下属的被推广人ID列表
            List<String> userIds = userProMapper.selectSubUserIdsByPromoterIds(promoterIds);

            // 4. 查询被推广用户的消费、返佣总额（现在用被推广人ID查询）
            List<QueryCommissionStatisticsVO> commissionStatisticsList = commissionDetailMapper.selectStatisticsByUserIds(userIds);
            Map<String, QueryCommissionStatisticsVO> commissionStatisticsMap = commissionStatisticsList.stream()
                    .collect(Collectors.toMap(QueryCommissionStatisticsVO::getUserId, vo -> vo));

            // 5. 查询被推广用户的产品数量
            List<UserNumVO> mainStatisticsList = instanceInfoMapper.selectNumByProUsers(userIds);
            Map<String, Integer> mainStatisticsMap = mainStatisticsList.stream()
                    .collect(Collectors.toMap(UserNumVO::getUserId, UserNumVO::getNum));

            // 6. 合并统计结果到每个推广人
            for (QueryCommissionStatisticsVO csv : list) {
                String promoterId = csv.getUserId();
                //获取当前推广人的下属被推广人ID
                List<String> subUserIds = userProMapper.selectSubUserIdsByPromoterId(promoterId);

                // 累加下属的消费返佣和产品数量
                BigDecimal totalConsumption = BigDecimal.ZERO;
                BigDecimal totalCommission = BigDecimal.ZERO;
                int totalProductNum = 0;

                for (String subUserId : subUserIds) {
                    // 累加消费返佣
                    QueryCommissionStatisticsVO s = commissionStatisticsMap.get(subUserId);
                    if (s != null) {
                        totalConsumption = totalConsumption.add(s.getConsumptionTotal());
                        totalCommission = totalCommission.add(s.getCommissionTotal());
                    }
                    // 累加产品数量
                    totalProductNum += mainStatisticsMap.getOrDefault(subUserId, 0);
                }

                // 给推广人赋值（不再是直接匹配，而是累加结果）
                csv.setCommissionTotal(totalCommission);
                csv.setConsumptionTotal(totalConsumption);
                csv.setProductNum(totalProductNum);
            }
        }

       // 7. 封装分页结果
        resultMap.put("total", page.getTotal());
        resultMap.put("list", list);

        return new ResultMessage(ResultMessage.SUCCEED_CODE, ResultMessage.SUCCEED_MSG, resultMap);
    }

    /**
     * 查询用户推广明细列表(客户端)
     *
     * @param queryUserProListSO
     * @return
     */
    @Override
    public ResultMessage queryUserProList(QueryUserProListSO queryUserProListSO) {
        // 1. 分页初始化
        PageHelper.startPage(queryUserProListSO.getPageNum(), queryUserProListSO.getPageSize());
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("userId", queryUserProListSO.getUserId());
        paramMap.put("account", queryUserProListSO.getAccount());
        paramMap.put("startTime", queryUserProListSO.getStartTime());
        paramMap.put("endTime", queryUserProListSO.getEndTime());

        // 2. 查询推广明细列表
        Page<QueryUserProListVOTO> page = (Page<QueryUserProListVOTO>) userProMapper.selectListUser(paramMap);
        List<QueryUserProListVOTO> list = page.getResult();

        // 3. 超级管理员标记判断（type=1 为超级管理员）
        boolean isSuperAdmin = false;
        if (queryUserProListSO.getAdminId() != null) {
            List<PowerBinding> groupingBindings = adminInfoMapper.selectGroupingByAdmin(queryUserProListSO.getAdminId());
            for (PowerBinding powerBinding : groupingBindings) {
                // 假设 type=1 对应超级管理员权限
                if (powerBinding.getType() == 1) {
                    isSuperAdmin = true;
                    break;
                }
            }
        } else {
            isSuperAdmin = false;
        }

        // 4. 补充产品数量统计
        if (list.size() > 0) {
            List<String> userIds = list.stream().map(QueryUserProListVOTO::getUserId).collect(Collectors.toList());
            List<UserProductNumVO> productNumList = instanceInfoMapper.selectListByUserSNum(userIds);
            Map<String, Integer> productNumMap = productNumList.stream()
                    .collect(Collectors.toMap(UserProductNumVO::getUserId, UserProductNumVO::getNum));

            for (QueryUserProListVOTO vo : list) {
                vo.setProductNum(productNumMap.getOrDefault(vo.getUserId(), 0));
                // 超级管理员且手机号不为空时，才进行脱敏处理
                if (isSuperAdmin && StringUtils.isNotBlank(vo.getPhone())) {
                    vo.setPhone(CommonUtil.hideMiddleFourDigits(vo.getPhone()));
                }
            }
        }

        // 5. 封装分页结果
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total", page.getTotal());
        resultMap.put("list", list);

        return new ResultMessage(ResultMessage.SUCCEED_CODE, ResultMessage.SUCCEED_MSG, resultMap);
    }

    /**
     * 查询返佣明细列表(客户端)
     * @param queryCommissionDetailListSO
     * @return
     */
    public ResultMessage queryCommissionDetailList(QueryCommissionDetailListSO queryCommissionDetailListSO){
        PageHelper.startPage(queryCommissionDetailListSO.getPageNum(), queryCommissionDetailListSO.getPageSize());
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("userId",queryCommissionDetailListSO.getUserId());
        paramMap.put("account",queryCommissionDetailListSO.getAccount());
        paramMap.put("startTime",queryCommissionDetailListSO.getStartTime());
        paramMap.put("endTime",queryCommissionDetailListSO.getEndTime());
        Page<QueryCommissionDetailListVO> page = (Page<QueryCommissionDetailListVO>)commissionDetailMapper.selectListUser(paramMap);

        /** 超级管理员标记：重新定义变量名，明确含义 */
        boolean isSuperAdmin = false; // 默认为非超级管理员

        if (queryCommissionDetailListSO.getAdminId() != null) {
            List<PowerBinding> groupingBindings = adminInfoMapper.selectGroupingByAdmin(queryCommissionDetailListSO.getAdminId());
            for (PowerBinding powerBinding : groupingBindings) {
                // 当type=1时，判定为超级管理员
                if (powerBinding.getType() == 1) {
                    isSuperAdmin = true; // 标记为超级管理员
                    break;
                }
            }
        }
        List<QueryCommissionDetailListVO> list = page.getResult();
        for (QueryCommissionDetailListVO acdv : list) {
            // 只有超级管理员且手机号有效时，才进行脱敏
            if (isSuperAdmin && acdv.getPhone() != null && !acdv.getPhone().trim().isEmpty()) {
                acdv.setPhone(CommonUtil.hideMiddleFourDigits(acdv.getPhone()));
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",list);
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);

    }

    /**
     * 用户等级列表
     * @param queryUserLevelListSO
     * @return
     */
    @Override
    public ResultMessage queryUserLevelList(QueryUserLevelListSO queryUserLevelListSO) {
        PageHelper.startPage(queryUserLevelListSO.getPageNum(), queryUserLevelListSO.getPageSize());

        // 执行查询
        List<UserListVO> list = userInfoMapper.selectListWithLevel(queryUserLevelListSO);

        // 封装分页结果
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total", ((Page<UserListVO>) list).getTotal());
        resultMap.put("list", list);

        return new ResultMessage(ResultMessage.SUCCEED_CODE, ResultMessage.SUCCEED_MSG, resultMap);
    }
}
