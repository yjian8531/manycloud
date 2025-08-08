package com.core.manycloudcommon.vo.node;

import lombok.Builder;
import lombok.Data;

/**
 * 平台下拉选择
 */
@Data
@Builder
public class PlatformInfoSelectVO {

    /** 平台标签 **/
    private String label;

    /** 平台名称 **/
    private String name;

}
