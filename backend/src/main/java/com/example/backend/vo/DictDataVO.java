package com.example.backend.vo;

import com.example.backend.entity.SysDictData;
import lombok.Builder;
import lombok.Data;

/**
 * 字典数据响应视图对象
 */
@Data
@Builder
public class DictDataVO {
    private String dictType;
    private String dictValue;
    private String dictLabel;
    private Integer sortNo;
    private String cssClass;

    /**
     * 根据字典数据实体构建响应对象
     *
     * @param data 字典数据实体
     * @return 字典数据响应对象
     */
    public static DictDataVO from(SysDictData data) {
        return DictDataVO.builder()
                .dictType(data.getDictType())
                .dictValue(data.getDictValue())
                .dictLabel(data.getDictLabel())
                .sortNo(data.getSortNo())
                .cssClass(data.getCssClass())
                .build();
    }
}
