package com.core.manycloudcommon.caller.Item;

import lombok.Data;

@Data
public class ConfigItemVO {
    private String name; // 规格名称（如 1核1G）
    private Integer value; // 该规格的主机数量
}