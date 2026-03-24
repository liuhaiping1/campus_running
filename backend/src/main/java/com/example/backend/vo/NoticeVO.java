package com.example.backend.vo;

import com.example.backend.entity.SystemNotice;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统公告响应视图对象
 */
@Data
@Builder
public class NoticeVO {
    private Long id;
    private String noticeTitle;
    private String noticeContent;
    private Integer noticeType;
    private Integer noticeStatus;
    private LocalDateTime publishTime;

    /**
     * 根据公告实体构建响应对象
     *
     * @param notice 公告实体
     * @return 公告响应对象
     */
    public static NoticeVO from(SystemNotice notice) {
        return NoticeVO.builder()
                .id(notice.getId())
                .noticeTitle(notice.getNoticeTitle())
                .noticeContent(notice.getNoticeContent())
                .noticeType(notice.getNoticeType())
                .noticeStatus(notice.getNoticeStatus())
                .publishTime(notice.getPublishTime())
                .build();
    }
}
