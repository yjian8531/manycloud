package com.core.manycloudadmin.service.impl;

import com.core.manycloudadmin.service.FinanceService;
import com.core.manycloudadmin.so.finance.ExecWithdrawalSO;
import com.core.manycloudadmin.so.finance.QueryListSO;
import com.core.manycloudadmin.so.finance.QueryWithdrawalListSO;
import com.core.manycloudadmin.so.finance.QueyrSaleDetailSO;
import com.core.manycloudadmin.util.ExcelUtil;
import com.core.manycloudadmin.util.WeiXinCaller;
import com.core.manycloudcommon.caller.Item.FinanceStatsItem;
import com.core.manycloudcommon.caller.so.*;
import com.core.manycloudcommon.caller.vo.FinanceStatsVO;
import com.core.manycloudcommon.entity.FinanceDetail;
import com.core.manycloudcommon.entity.FinanceWithdrawal;
import com.core.manycloudcommon.entity.OfflineFinance;
import com.core.manycloudcommon.mapper.*;
import com.core.manycloudcommon.utils.CommonUtil;
import com.core.manycloudcommon.utils.DateUtil;
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

import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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


    @Autowired
    private OfflineFinanceTOMapper offlineFinanceTOMapper;


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
                financeDetail.setFinanceNo(CommonUtil.getRandomStr(12));
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

    @Override
    public ResultMessage getFinanceStats(FinanceStatsSO so) {
        String timeUnit = so.getTimeUnit();
        Date startTime = null;
        Date endTime = null;
        try {
            // 定义日期格式，根据实际传入的日期字符串格式调整，这里假设是 yyyy-MM
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            startTime = sdf.parse(so.getStartTime());
            endTime = sdf.parse(so.getEndTime());
        } catch (ParseException e) {
            // 日期解析异常时，返回错误信息
            return new ResultMessage(ResultMessage.FAILED_CODE, "日期格式错误");
        }

        // 验证时间范围
        if (startTime == null || endTime == null || startTime.after(endTime)) {
            return new ResultMessage(ResultMessage.FAILED_CODE, "时间参数错误");
        }

        // 查询数据
        List<FinanceStatsItem> items = financeDetailMapper.selectStatsByTimeUnit(timeUnit, startTime, endTime);

        // 构造返回数据
        FinanceStatsVO vo = new FinanceStatsVO();
        vo.setDates(items.stream().map(item -> item.getDateStr()).collect(Collectors.toList()));
        vo.setRecharge(items.stream().mapToInt(item -> item.getRecharge()).boxed().collect(Collectors.toList()));
        vo.setConsumption(items.stream().mapToInt(item -> item.getConsumption()).boxed().collect(Collectors.toList()));
        vo.setWithdrawal(items.stream().mapToInt(item -> item.getWithdrawal()).boxed().collect(Collectors.toList()));

        // 计算总额
        Map<String, Integer> total = new HashMap<>();
        total.put("recharge", items.stream().mapToInt(item -> item.getRecharge()).sum());
        total.put("consumption", items.stream().mapToInt(item -> item.getConsumption()).sum());
        total.put("withdrawal", items.stream().mapToInt(item -> item.getWithdrawal()).sum());
        vo.setTotal(total);

        return new ResultMessage(ResultMessage.SUCCEED_CODE, ResultMessage.SUCCEED_MSG, vo);
    }
    /**
     * 删除线下账单
     * @param delOfflineSO
     * @return
     */
    @Override
    public ResultMessage delOffline(DelOfflineSO delOfflineSO) {
        int i = offlineFinanceTOMapper.deleteByPrimaryKey(delOfflineSO.getId());
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"删除成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"删除失败");
        }
    }

    /**
     * 添加线下财务账单
     * @param addOfflineSO
     * @return
     */
    public ResultMessage addOffline(AddOfflineSO addOfflineSO){
        OfflineFinance offlineFinance = new OfflineFinance();
        offlineFinance.setAmountNum(addOfflineSO.getAmountNum());
        offlineFinance.setAssociation(addOfflineSO.getAssociation());
        offlineFinance.setDirection(addOfflineSO.getDirection());
        offlineFinance.setRemark(addOfflineSO.getRemark());
        offlineFinance.setTag(addOfflineSO.getTag());
        offlineFinance.setWay(addOfflineSO.getWay());
        offlineFinance.setOccurTime(addOfflineSO.getOccurTime() == null ? new Date(): DateUtil.StringToDate (addOfflineSO.getOccurTime()));
        offlineFinance.setCreateTime(new Date());
        int i = offlineFinanceTOMapper.insertSelective(offlineFinance);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"添加成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"添加失败");
        }
    }

    /**
     * 更新线下财务账单
     * @param updateOfflineSO
     * @return
     */
    public ResultMessage updateOffline(UpdateOfflineSO updateOfflineSO){
        OfflineFinance offlineFinance = new OfflineFinance();
        offlineFinance.setId(updateOfflineSO.getId());
        offlineFinance.setAmountNum(updateOfflineSO.getAmountNum());
        offlineFinance.setAssociation(updateOfflineSO.getAssociation());
        offlineFinance.setDirection(updateOfflineSO.getDirection());
        offlineFinance.setRemark(updateOfflineSO.getRemark());
        offlineFinance.setOccurTime(DateUtil.StringToDate(updateOfflineSO.getOccurTime()));
        offlineFinance.setTag(updateOfflineSO.getTag());
        offlineFinance.setWay(updateOfflineSO.getWay());
        int i = offlineFinanceTOMapper.updateByPrimaryKeySelective(offlineFinance);
        if(i > 0){
            return new ResultMessage(ResultMessage.SUCCEED_CODE,"更新成功");
        }else{
            return new ResultMessage(ResultMessage.FAILED_CODE,"更新失败");
        }
    }

    /**
     * 查询线下财务账单列表
     * @param selectOfflineListSO
     * @return
     */
    public ResultMessage selectOfflineList(SelectOfflineListSO selectOfflineListSO){
        PageHelper.startPage(selectOfflineListSO.getPageNum(), selectOfflineListSO.getPageSize());
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("association",selectOfflineListSO.getAssociation());
        paramMap.put("direction",selectOfflineListSO.getDirection());
        paramMap.put("remark",selectOfflineListSO.getRemark());
        paramMap.put("tag",selectOfflineListSO.getTag());
        paramMap.put("way",selectOfflineListSO.getWay());
        paramMap.put("startTime",selectOfflineListSO.getStartTime());
        paramMap.put("endTime",selectOfflineListSO.getEndTime());
        Page<OfflineFinance> page = (Page<OfflineFinance>) offlineFinanceTOMapper.selectList(paramMap);
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("total",page.getTotal());
        resultMap.put("list",page.getResult());
        return new ResultMessage(ResultMessage.SUCCEED_CODE,ResultMessage.SUCCEED_MSG,resultMap);
    }
    /**
     * 导出线下财务账单列表
     * @param selectOfflineListSO
     * @return
     */
    @Override
    public void deriveSelectOfflineList(HttpServletResponse response, SelectOfflineListSO selectOfflineListSO) {
            List<List<String>> excelData = new ArrayList<>();
            //表头数据
            List<String> header = new ArrayList<>();
            header.add("金额");
            header.add("收款方式");
            header.add("收支类型");
            header.add("标签");
            header.add("关联信息");
            header.add("交易时间");
            header.add("备注");
            header.add("创建时间");
            excelData.add(header);

            Map<String,Object> paramMap = new HashMap<>();
            paramMap.put("association",selectOfflineListSO.getAssociation());
            paramMap.put("direction",selectOfflineListSO.getDirection());
            paramMap.put("remark",selectOfflineListSO.getRemark());
            paramMap.put("tag",selectOfflineListSO.getTag());
            paramMap.put("way",selectOfflineListSO.getWay());
            paramMap.put("startTime",selectOfflineListSO.getStartTime());
            paramMap.put("endTime",selectOfflineListSO.getEndTime());
            List<OfflineFinance> list = offlineFinanceTOMapper.selectList(paramMap);
            for(OfflineFinance offlineFinance : list){
                List<String> content = new ArrayList<>();
                content.add(offlineFinance.getAmountNum().toPlainString());
                String way = "";
                if(offlineFinance.getWay() == 0){
                    way = "支付宝";
                }else if(offlineFinance.getWay() == 1){
                    way = "微信";
                }else{
                    way = "银行卡";
                }
                content.add(way);

                String direction = "";
                if(offlineFinance.getDirection() == 0){
                    direction = "收入";
                }else{
                    direction = "支出";
                }
                content.add(direction);

                String tag;
                if(offlineFinance.getTag() == null){
                    tag = "";
                }else if(offlineFinance.getTag() == 0){
                    tag = "渠道返佣";
                }else if(offlineFinance.getTag() == 1){
                    tag = "退款";
                }else{
                    tag = "采购支出";
                }
                content.add(tag);
                content.add(offlineFinance.getAssociation());
                if(offlineFinance.getOccurTime() != null){
                    content.add(DateUtil.dateStr4(offlineFinance.getOccurTime()));
                }else{
                    content.add(DateUtil.dateStr4(offlineFinance.getCreateTime()));
                }
                content.add(offlineFinance.getRemark());
                content.add(DateUtil.dateStr4(offlineFinance.getCreateTime()));
                excelData.add(content);
            }

            String sheetName = "线下财务账单";
            String fileName = "OfflineFinance.xls";
            try{
                ExcelUtil.exportExcel(response, excelData, sheetName, fileName, 15);
            }catch (Exception e){
                e.printStackTrace();
                log.info("导出线下财务账单Execl异常:{}",e.getMessage());
            }
        }

}
