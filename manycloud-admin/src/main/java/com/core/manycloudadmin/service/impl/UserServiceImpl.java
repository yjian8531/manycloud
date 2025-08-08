package com.core.manycloudadmin.service.impl;

import com.core.manycloudadmin.service.UserService;
import com.core.manycloudadmin.so.user.QueryUserListSO;
import com.core.manycloudadmin.so.user.UpdateUserFinanceSO;
import com.core.manycloudadmin.so.user.UpdateUserRemarkSO;
import com.core.manycloudadmin.util.WeiXinCaller;
import com.core.manycloudcommon.entity.FinanceDetail;
import com.core.manycloudcommon.entity.UserInfo;
import com.core.manycloudcommon.mapper.FinanceDetailMapper;
import com.core.manycloudcommon.mapper.UserFinanceMapper;
import com.core.manycloudcommon.mapper.UserInfoMapper;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.vo.user.UserListVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private UserFinanceMapper userFinanceMapper;

    @Autowired
    private FinanceDetailMapper financeDetailMapper;

    @Autowired
    private WeiXinCaller weiXinCaller;

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

}
