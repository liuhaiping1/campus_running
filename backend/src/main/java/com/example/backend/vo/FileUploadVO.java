package com.example.backend.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件上传响应视图对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadVO {

    /** 原始文件名 */
    private String originalName;

    /** 保存后的文件名（UUID 重命名） */
    private String fileName;

    /** 文件可访问 URL，例如 /uploads/2026/05/05/xxx.jpg */
    private String fileUrl;

    /** 文件大小，单位字节 */
    private Long fileSize;

    /** 文件 MIME 类型，例如 image/jpeg */
    private String contentType;
}
