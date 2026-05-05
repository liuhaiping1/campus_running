package com.example.backend.vo;

import com.example.backend.entity.CampusLocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 校园常用地点视图对象
 * <p>
 * 供前端下单时选择常用校园地点，不包含审计字段。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampusLocationVO {

    /** 主键ID */
    private Long id;

    /** 地点名称 */
    private String locationName;

    /** 高德POI ID */
    private String amapPoiId;

    /** 高德行政区划编码 */
    private String amapAdcode;

    /** 高德城市编码 */
    private String amapCityCode;

    /** 地点类型：1快递点 2外卖取餐点 3商超 4教学楼 5宿舍楼 6打印店 7其他 */
    private Integer locationType;

    /** 校区名称 */
    private String campusName;

    /** 楼栋/区域名称 */
    private String buildingName;

    /** 详细地址 */
    private String detailAddress;

    /** 经度 */
    private BigDecimal longitude;

    /** 纬度 */
    private BigDecimal latitude;

    /** 坐标系：GCJ02/WGS84/BD09 */
    private String coordType;

    /** 排序 */
    private Integer sortNo;

    /**
     * 根据实体构建视图对象
     *
     * @param entity 校园地点实体
     * @return 视图对象
     */
    public static CampusLocationVO from(CampusLocation entity) {
        return CampusLocationVO.builder()
                .id(entity.getId())
                .locationName(entity.getLocationName())
                .amapPoiId(entity.getAmapPoiId())
                .amapAdcode(entity.getAmapAdcode())
                .amapCityCode(entity.getAmapCityCode())
                .locationType(entity.getLocationType())
                .campusName(entity.getCampusName())
                .buildingName(entity.getBuildingName())
                .detailAddress(entity.getDetailAddress())
                .longitude(entity.getLongitude())
                .latitude(entity.getLatitude())
                .coordType(entity.getCoordType())
                .sortNo(entity.getSortNo())
                .build();
    }
}
