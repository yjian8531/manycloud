package com.core.manycloudadmin.service.impl;

import com.core.manycloudadmin.service.FinanceService;
import com.core.manycloudadmin.so.finance.ExecWithdrawalSO;
import com.core.manycloudadmin.so.finance.QueryListSO;
import com.core.manycloudadmin.so.finance.QueryWithdrawalListSO;
import com.core.manycloudadmin.so.finance.QueyrSaleDetailSO;
import com.core.manycloudadmin.util.WeiXinCaller;
import com.core.manycloudcommon.entity.FinanceDetail;
import com.core.manycloudcommon.entity.FinanceWithdrawal;
import com.core.manycloudcommon.entity.UserFinance;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.ResultMessage;
import com.core.manycloudcommon.utils.StringUtils;
import com.core.manycloudcommon.vo.finance.FinanceDetailListVO;
import com.core.manycloudcommon.vo.finance.SaleDetailConunt;
import com.core.manycloudcommon.vo.finance.SaleDetailListVO;
import com.core.manycloudcommon.vo.user.UserSelectVO;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 财务service
 */
@Slf4j
@Service
public class FinanceServiceImpl implements FinanceService {


    @Autowired
    private FinanceDetailMapper financeDetailMapper;

    @Autowired
    private SaleDetailMapper saleDetailMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private FinanceWithdrawalMapper financeWithdrawalMapper;

    @Autowired
    private UserFinanceMapper userFinanceMapper;

    @Autowired
    private WeiXinCaller weiXinCaller;


    /**
     * 查询账单明细列表数据
     * @return
     */
    public ResultMessage queryList(QueryListSO queryListSO){
        PageHelper.startPage(queryListSO.getPage(), queryListSO.getPageSize());
        Page<FinanceDetailListVO> page = (Page<FinanceDetailListVO>)financeDetailMapper.selectList(null,queryListSO.getDirection(),queryListSO.getTag(),
                queryListSO.getStartTime(),queryListSO.getEndTime(),queryListSO.getProductNo(),queryListSO.getEmail());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 获取市场推广用户
     * @return
     */
    public ResultMessage querySaleUser(){
        List<UserSelectVO> list = userInfoMapper.selectSaleByType();
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,list);
    }


    /**
     * 查询市场人员提成数据
     * @param queyrSaleDetailSO
     * @return
     */
    public ResultMessage queyrSaleDetail(QueyrSaleDetailSO queyrSaleDetailSO){
        PageHelper.startPage(queyrSaleDetailSO.getPage(), queyrSaleDetailSO.getPageSize());
        Page<SaleDetailListVO> page = (Page<SaleDetailListVO>)saleDetailMapper.selectList(queyrSaleDetailSO.getUserId(),queyrSaleDetailSO.getMonthStr());

        SaleDetailConunt saleDetailConunt = saleDetailMapper.selectListCount(queyrSaleDetailSO.getUserId(),queyrSaleDetailSO.getMonthStr());

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("conunt",saleDetailConunt);
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 查询提现列表信息
     * @param queryWithdrawalListSO
     * @return
     */
    public ResultMessage queryWithdrawalList(QueryWithdrawalListSO queryWithdrawalListSO){
        PageHelper.startPage(queryWithdrawalListSO.getPage(), queryWithdrawalListSO.getPageSize());
        Page<FinanceWithdrawal> page = (Page<FinanceWithdrawal>)financeWithdrawalMapper.selectList(queryWithdrawalListSO.getAccount(),queryWithdrawalListSO.getStatus());
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }


    /**
     * 提现审核
     * @param execWithdrawalSO
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultMessage execWithdrawal(ExecWithdrawalSO execWithdrawalSO){
        Integer status;
        if("Y".equals(execWithdrawalSO.getTad())){
            status = 2;
        }else{
            status = 1;
        }

        FinanceWithdrawal financeWithdrawal = financeWithdrawalMapper.selectByPrimaryKey(execWithdrawalSO.getId());
        if(financeWithdrawal.getStatus() != 0){
            return new ResultMessage(ResultMessage.FAILED_CODE,"当前状态无法审核");
        }
        financeWithdrawal.setStatus(status);
        financeWithdrawal.setAccount(execWithdrawalSO.getAccount());
        financeWithdrawal.setName(execWithdrawalSO.getName());
        financeWithdrawal.setRemark(execWithdrawalSO.getRemark());
        financeWithdrawal.setUpdateTime(new Date());
        int i = financeWithdrawalMapper.updateByPrimaryKeySelective(financeWithdrawal);
        if(i > 0){

            if(status == 2){
                /** 审核通过扣除金额 **/
                userFinanceMapper.updateBalanceByUserId(financeWithdrawal.getUserId(),"minus",financeWithdrawal.getMoneyNum());

                FinanceDetail financeDetail = new FinanceDetail();
                financeDetail.setUserId(financeWithdrawal.getUserId());
                financeDetail.setFinanceNo(CommonUtil.getRandomStr(8));
                financeDetail.setProductNo(financeWithdrawal.getWithdrawalNo());
                financeDetail.setType(2);//提现类型
                financeDetail.setMoneyNum(financeWithdrawal.getMoneyNum());
                financeDetail.setTag("withdraw");//提现标签
                financeDetail.setDirection(1);//支出
                financeDetail.setStatus(1);
                financeDetail.setCreateTime(new Date());
                financeDetail.setUpdateTime(new Date());
                financeDetailMapper.insertSelective(financeDetail);
                String wayStr = "";
                if(financeWithdrawal.getWay() == 0){
                    wayStr = "支付宝";
                }else if(financeWithdrawal.getWay() == 1){
                    wayStr = "微信";
                }else if(financeWithdrawal.getWay() == 2){
                    wayStr = "银行卡";
                }
                String account = StringUtils.isEmpty(wayStr) ? financeWithdrawal.getAccount() : wayStr+" - "+ financeWithdrawal.getAccount();

                /** 微信公众号推送 **/
                weiXinCaller.sendWithdrawalSuccess(financeWithdrawal.getUserId(),financeWithdrawal.getUpdateTime(),
                        account,financeWithdrawal.getMoneyNum(),financeWithdrawal.getTaxRatio(),financeWithdrawal.getMoneyTax());

            }else if(status == 2){
                /** 审核未通过解冻金额 **/
                userFinanceMapper.updateBalanceByUserId(financeWithdrawal.getUserId(),"unbind",financeWithdrawal.getMoneyNum());
            }
        }

        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG);

    }


}
