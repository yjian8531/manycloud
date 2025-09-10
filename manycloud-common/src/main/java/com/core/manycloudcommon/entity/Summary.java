package com.core.manycloudcommon.entity;

import lombok.Data;

@Data
    public  class Summary {
        private Integer newUsers;      // 新增用户总数
        private Integer activeUsersAvg; // 活跃用户均值
        private Integer inactiveUsers; // 失活用户总数
    }