package com.core.manycloudcommon.caller.so;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryFirewallSO {
   // 名称
   private String name;
   // 防火墙id
   private String fwId;
   // 组id
   private String groupId;
}