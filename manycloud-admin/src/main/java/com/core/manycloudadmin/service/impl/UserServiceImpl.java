package com.core.manycloudadmin.service.impl;

import com.core.manycloudadmin.service.UserService;
import com.core.manycloudadmin.so.user.QueryUserListSO;
import com.core.manycloudadmin.so.user.UpdateUserFinanceSO;
import com.core.manycloudadmin.so.user.UpdateUserRemarkSO;
import com.core.manycloudadmin.so.util.DateUtil;
import com.core.manycloudadmin.util.WeiXinCaller;

import com.core.manycloudcommon.caller.so.UserStatsSo;

import com.core.manycloudcommon.entity.FinanceDetail;
import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.vo.user.UserListVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.text.ParseException;
import java.util.*;


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
            financeDetail.setFinanceNo(CommonUtil.getRandomNumber(32));
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

        // 处理结束时间（直接使用实体类的getter方法）
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

        for (String dateStr : dateList) {
            // 查询新增用户
            Integer createNum = userInfoMapper.queryCreateNum(dbFormatStr, dateStr);
            createNum = createNum == null ? 0 : createNum;
            newUsersList.add(createNum);
            totalNewUsers += createNum;

            // 查询活跃用户
            Integer activeNum = userInfoMapper.queryActiveNum(dbFormatStr, dateStr);
            activeNum = activeNum == null ? 0 : activeNum;
            activeUsersList.add(activeNum);
            totalActiveUsers += activeNum;

            // 查询失活用户
            Integer inactiveNum = userInfoMapper.queryInactiveNum(dbFormatStr, dateStr);
            inactiveNum = inactiveNum == null ? 0 : inactiveNum;

            // 处理是否显示失活数
            boolean showInactive = userStatsSo.getIncludeInactive() == null ?
                    true : userStatsSo.getIncludeInactive();
            inactiveUsersList.add(showInactive ? inactiveNum : 0);
            totalInactiveUsers += showInactive ? inactiveNum : 0;
        }

        // 计算活跃用户平均值（四舍五入）
        int activeUsersAvg = dateList.isEmpty() ? 0 :
                (int) Math.round((double) totalActiveUsers / dateList.size());

        // 构建响应数据
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("dates", dateList);
        data.put("newUsers", newUsersList);
        data.put("activeUsers", activeUsersList);
        data.put("inactiveUsers", inactiveUsersList);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("newUsers", totalNewUsers);
        summary.put("activeUsersAvg", activeUsersAvg);
        summary.put("inactiveUsers", totalInactiveUsers);
        data.put("summary", summary);

        return new ResultMessage(ResultMessage.SUCCEED_CODE, ResultMessage.SUCCEED_MSG, data);
    }

}
