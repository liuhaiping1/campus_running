package com.example.backend.service.impl;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.config.UploadProperties;
import com.example.backend.service.FileUploadService;
import com.example.backend.vo.FileUploadVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传服务实现类
 * <p>
 * 将文件保存到本地磁盘，按日期分目录存储，文件名使用 UUID 防重名和路径穿越。
 * </p>
 */
@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    /** 日期路径格式化器，复用避免每次请求新建 */
    private static final DateTimeFormatter DATE_DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    /** 禁止上传的危险文件后缀 */
    private static final List<String> BLOCKED_EXTENSIONS = List.of(
            "exe", "bat", "cmd", "sh", "js", "html", "htm", "jsp", "php", "py", "rb", "pl"
    );

    /** MIME 类型与后缀的映射：只允许 image/* 和 application/pdf */
    private static boolean isMimeAllowedForExtension(String ext, String contentType) {
        if (contentType == null) return false;
        if (ext.equals("pdf")) return contentType.equals("application/pdf");
        return contentType.startsWith("image/");
    }

    private final UploadProperties uploadProperties;

    /**
     * 构造函数注入上传配置属性
     *
     * @param uploadProperties 上传配置属性
     */
    public FileUploadServiceImpl(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    /**
     * 上传文件到本地存储
     *
     * @param file 上传的文件
     * @return 文件上传结果 VO
     */
    @Override
    public FileUploadVO upload(MultipartFile file) {
        // 1. 校验文件非空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.FILE_EMPTY);
        }

        // 2. 获取并校验原始文件名
        String originalName = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "unknown";

        // 3. 提取后缀并校验类型
        String ext = extractExtension(originalName);
        validateFileType(ext, file.getContentType());

        // 4. 校验文件大小
        long maxBytes = uploadProperties.getMaxSizeBytes();
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        // 5. 生成安全文件名（UUID + 后缀），防止路径穿越和重名
        String safeFileName = UUID.randomUUID().toString() + "." + ext;

        // 6. 按日期创建子目录，例如 uploads/2026/05/05/
        String dateDir = LocalDate.now().format(DATE_DIR_FORMATTER);
        String relativePath = dateDir + "/" + safeFileName;

        // 7. 构建目标路径并保存文件（baseDir 先转绝对路径，确保安全检查有效）
        Path baseDir = Paths.get(uploadProperties.getLocalDir()).toAbsolutePath().normalize();
        Path targetPath = baseDir.resolve(relativePath).normalize();

        // 安全检查：确保最终路径在 baseDir 内，防止路径穿越
        if (!targetPath.startsWith(baseDir)) {
            log.error("文件保存路径异常，可能路径穿越攻击: {}", targetPath);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        try {
            Files.createDirectories(targetPath.getParent());
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            log.error("文件写入失败: {}", targetPath, e);
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        // 8. 构建可访问 URL
        String fileUrl = uploadProperties.getPublicPrefix() + "/" + relativePath;

        return FileUploadVO.builder()
                .originalName(originalName)
                .fileName(safeFileName)
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();
    }

    /**
     * 从文件名中提取后缀（小写）
     *
     * @param fileName 文件名
     * @return 小写后缀，不含点号；无后缀返回空字符串
     */
    private String extractExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * 校验文件类型是否允许上传
     *
     * @param ext         文件后缀（小写）
     * @param contentType MIME 类型
     */
    private void validateFileType(String ext, String contentType) {
        // 禁止危险文件后缀
        if (BLOCKED_EXTENSIONS.contains(ext)) {
            log.warn("拦截危险文件类型上传: ext={}, contentType={}", ext, contentType);
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        // 校验后缀是否在允许列表中
        List<String> allowedTypes = uploadProperties.getAllowedTypeList();
        if (allowedTypes.isEmpty()) {
            return;
        }
        if (!allowedTypes.contains(ext)) {
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }

        // 校验 MIME 类型与后缀是否匹配，防止后缀伪装攻击
        if (!isMimeAllowedForExtension(ext, contentType)) {
            log.warn("MIME类型与后缀不匹配: ext={}, contentType={}", ext, contentType);
            throw new BusinessException(ErrorCode.FILE_TYPE_NOT_ALLOWED);
        }
    }
}
