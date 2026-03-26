package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 地址保存请求
 * <p>
 * 用于创建和修改当前登录用户的地址。
 * </p>
 */
@Data
public class AddressSaveRequest {

    @NotBlank(message = "联系人不能为空")
    @Size(max = 32, message = "联系人长度不能超过32位")
    private String contactName;

    @NotBlank(message = "联系电话不能为空")
    @Size(max = 20, message = "联系电话长度不能超过20位")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    private String contactPhone;

    @NotBlank(message = "校区不能为空")
    @Size(max = 64, message = "校区长度不能超过64位")
    private String campusName;

    @NotBlank(message = "楼栋不能为空")
    @Size(max = 64, message = "楼栋长度不能超过64位")
    private String buildingName;

    @NotBlank(message = "详细地址不能为空")
    @Size(max = 255, message = "详细地址长度不能超过255位")
    private String detailAddress;

    private BigDecimal longitude;

    private BigDecimal latitude;

    private Integer isDefault = 0;
}
