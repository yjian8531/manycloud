package com.core.manycloudservice.service.impl;

import com.core.manycloudcommon.entity.InstanceInfo;
import com.core.manycloudcommon.entity.OrderExce;
import com.core.manycloudcommon.mapper.OrderExceMapper;
import com.core.manycloudcommon.utils.StringUtils;
import com.core.manycloudservice.service.OrderExceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 订单异常表(t_order_exce)写入服务
 */
@Slf4j
@Service
public class OrderExceServiceImpl implements OrderExceService {

    @Autowired
    private OrderExceMapper orderExceMapper;

    /***
     * 创建失败写入订单异常表 t_order_exce
     * 独立事务(REQUIRES_NEW):失败记录立即提交,不随调用方大事务回滚,
     * 保证"云厂商创建报错→结算整体回滚"这类孤儿机场景的线索能留在库里可查
     * @param instanceInfo
     * @param content 失败原因
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void save(InstanceInfo instanceInfo, String content){
        try{
            OrderExce oe = new OrderExce();
            oe.setUserId(instanceInfo.getUserId());
            oe.setLabel(instanceInfo.getLabel());
            /** 主机编号（创建失败时尚无主机编号时用实例ID兜底） **/
            oe.setInstanceNo(StringUtils.isNotEmpty(instanceInfo.getServiceNo()) ? instanceInfo.getServiceNo() : instanceInfo.getInstanceId());
            oe.setStatus(0);//待处理
            oe.setAuthor("SYSTEM");
            oe.setContent(content);
            oe.setCreateTime(new Date());
            oe.setUpdateTime(new Date());
            orderExceMapper.insertSelective(oe);
        }catch (Exception e){
            //异常表写入失败不影响主流程
            log.info("订单异常记录写入失败：{}",e.getMessage());
        }
    }
}
