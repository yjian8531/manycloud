package com.core.manycloudcommon.mapper;

import com.core.manycloudcommon.entity.CustomerService;

import java.util.List;

public interface CustomerServiceMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(CustomerService record);

    int insertSelective(CustomerService record);

    CustomerService selectByPrimaryKey(Integer id);

    List<CustomerService> selectValid();

    int updateByPrimaryKeySelective(CustomerService record);

    int updateByPrimaryKey(CustomerService record);
}