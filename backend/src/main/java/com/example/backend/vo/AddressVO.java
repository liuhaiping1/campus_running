package com.example.backend.vo;

import com.example.backend.entity.UserAddress;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 地址响应视图对象
 */
@Data
@Builder
public class AddressVO {
    private Long id;
    private String contactName;
    private String contactPhone;
    private String campusName;
    private String buildingName;
    private String detailAddress;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer isDefault;

    /**
     * 根据地址实体构建响应对象
     *
     * @param address 地址实体
     * @return 地址响应对象
     */
    public static AddressVO from(UserAddress address) {
        return AddressVO.builder()
                .id(address.getId())
                .contactName(address.getContactName())
                .contactPhone(address.getContactPhone())
                .campusName(address.getCampusName())
                .buildingName(address.getBuildingName())
                .detailAddress(address.getDetailAddress())
                .longitude(address.getLongitude())
                .latitude(address.getLatitude())
                .isDefault(address.getIsDefault())
                .build();
    }
}
