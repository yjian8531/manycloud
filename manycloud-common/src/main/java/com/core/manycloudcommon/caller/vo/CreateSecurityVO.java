package com.core.manycloudcommon.caller.vo;

import lombok.Builder;
import lombok.Data;

/**
 * 安全组创建请求参数
 */
@Data
@Builder
public class CreateSecurityVO {
      private String code;

      private String msg;

      private String fwId;
}