package com.core.manycloudcommon.caller.vo;
import com.core.manycloudcommon.caller.Item.TemplateItem;
import lombok.Data;
import lombok.Builder;

import java.util.List;

@Data
@Builder
public class TemplateListVO {
    private String code;
    private String msg;
    private Integer total;
    private List<TemplateItem> rows;
}
