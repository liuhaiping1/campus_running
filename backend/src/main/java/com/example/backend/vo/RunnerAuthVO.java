package com.example.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 跑腿员认证信息VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RunnerAuthVO {

    /** 认证记录ID */
    private Long id;

    /** 申请人用户ID */
    private Long userId;

    /** 申请人姓名 */
    private String realName;

    /** 申请人手机号 */
    private String phone;

    /** 认证批次号 */
    private String authBatchNo;

    /** 学校名称 */
    private String schoolName;

    /** 校区名称 */
    private String campusName;

    /** 证件类型 */
    private Integer certType;

    /** 证件号码 */
    private String certNo;

    /** 证件正面图片URL */
    private String certFrontUrl;

    /** 证件背面图片URL */
    private String certBackUrl;

    /** 认证状态：0待审 1通过 2驳回 3失效 */
    private Integer authStatus;

    /** 驳回原因 */
    private String rejectReason;

    /** 审核管理员ID */
    private Long reviewAdminId;

    /** 审核时间 */
    private LocalDateTime reviewTime;

    /** 申请时间 */
    private LocalDateTime createTime;
}
