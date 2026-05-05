package com.example.backend.vo;

import com.example.backend.entity.RunnerAuth;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 跑腿员认证信息响应视图对象
 */
@Data
@Builder
public class RunnerAuthProfileVO {

    /**
     * 认证记录ID
     */
    private Long id;

    /**
     * 认证状态：0待审 1通过 2驳回 3失效
     */
    private Integer authStatus;

    /**
     * 驳回原因
     */
    private String rejectReason;

    /**
     * 证件类型：1学生证 2校园卡 3身份证
     */
    private Integer certType;

    /**
     * 证件号码（脱敏）
     */
    private String certNoMasked;

    /**
     * 证件正面图片
     */
    private String certFrontUrl;

    /**
     * 证件背面图片
     */
    private String certBackUrl;

    /**
     * 申请时间
     */
    private LocalDateTime applyTime;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 根据认证实体构建响应对象，对证件号进行脱敏处理
     *
     * @param auth 认证实体
     * @return 认证信息响应对象
     */
    public static RunnerAuthProfileVO from(RunnerAuth auth) {
        return RunnerAuthProfileVO.builder()
                .id(auth.getId())
                .authStatus(auth.getAuthStatus())
                .rejectReason(auth.getRejectReason())
                .certType(auth.getCertType())
                .certNoMasked(maskCertNo(auth.getCertNo()))
                .certFrontUrl(auth.getCertFrontUrl())
                .certBackUrl(auth.getCertBackUrl())
                .applyTime(auth.getCreateTime())
                .reviewTime(auth.getReviewTime())
                .build();
    }

    /**
     * 证件号脱敏：保留前3后4，中间用****替换
     *
     * @param certNo 原始证件号
     * @return 脱敏后的证件号
     */
    private static String maskCertNo(String certNo) {
        if (certNo == null || certNo.length() <= 7) {
            return certNo;
        }
        return certNo.substring(0, 3) + "****" + certNo.substring(certNo.length() - 4);
    }
}
