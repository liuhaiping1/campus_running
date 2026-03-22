package com.example.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 跑腿员认证申请请求DTO
 */
@Data
public class RunnerAuthApplyRequest {

    /** 学校名称 */
    @NotBlank(message = "学校名称不能为空")
    private String schoolName;

    /** 校区名称 */
    private String campusName;

    /** 证件类型：1学生证 2校园卡 3身份证 */
    @NotNull(message = "证件类型不能为空")
    private Integer certType;

    /** 证件号码 */
    private String certNo;

    /** 证件正面图片URL */
    @NotBlank(message = "证件正面图片不能为空")
    private String certFrontUrl;

    /** 证件背面图片URL */
    private String certBackUrl;
}
