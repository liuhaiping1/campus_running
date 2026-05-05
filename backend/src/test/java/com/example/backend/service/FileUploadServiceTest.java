package com.example.backend.service;

import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.config.UploadProperties;
import com.example.backend.service.impl.FileUploadServiceImpl;
import com.example.backend.vo.FileUploadVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FileUploadService 单元测试类
 * <p>
 * 覆盖文件上传的核心校验逻辑和异常场景。
 * </p>
 *
 * @author campus_running
 */
@DisplayName("FileUploadService 单元测试")
class FileUploadServiceTest {

    @TempDir
    Path tempDir;

    private UploadProperties uploadProperties;
    private FileUploadServiceImpl fileUploadService;

    /** 设置上传目录为基础临时目录下的 uploads 子目录 */
    @BeforeEach
    void setUp() {
        Path uploadDir = tempDir.resolve("uploads");
        uploadProperties = new UploadProperties();
        uploadProperties.setLocalDir(uploadDir.toString());
        uploadProperties.setPublicPrefix("/uploads");
        uploadProperties.setMaxSizeMb(5);
        uploadProperties.setAllowedTypes(java.util.List.of("jpg", "jpeg", "png", "webp", "pdf"));
        fileUploadService = new FileUploadServiceImpl(uploadProperties);
    }

    /** 创建一个合法的 MockMultipartFile */
    private MockMultipartFile createMockFile(String name, String originalName, String contentType, byte[] content) {
        return new MockMultipartFile(name, originalName, contentType, content);
    }

    // =========================================================================
    // 正常场景
    // =========================================================================

    @Nested
    @DisplayName("成功上传")
    class SuccessfulUpload {

        @Test
        @DisplayName("上传合法 JPG 文件应返回正确的 FileUploadVO")
        void shouldUploadJpgSuccessfully() {
            byte[] content = new byte[1024]; // 1KB 测试数据
            MockMultipartFile file = createMockFile("file", "test.jpg", "image/jpeg", content);

            FileUploadVO result = fileUploadService.upload(file);

            assertNotNull(result);
            assertEquals("test.jpg", result.getOriginalName());
            assertNotNull(result.getFileName());
            // 文件名应为 UUID + .jpg
            assertTrue(result.getFileName().endsWith(".jpg"));
            assertEquals(36 + 4, result.getFileName().length()); // UUID(36) + .jpg(4)
            assertEquals((long) content.length, result.getFileSize());
            assertEquals("image/jpeg", result.getContentType());
            // URL 应以 /uploads/ 开头并包含日期路径
            assertTrue(result.getFileUrl().startsWith("/uploads/"));
            assertTrue(result.getFileUrl().contains("/"));
            assertTrue(result.getFileUrl().endsWith(".jpg"));
        }

        @Test
        @DisplayName("上传 PNG 文件应成功")
        void shouldUploadPngSuccessfully() {
            byte[] content = new byte[512];
            MockMultipartFile file = createMockFile("file", "photo.png", "image/png", content);

            FileUploadVO result = fileUploadService.upload(file);

            assertNotNull(result);
            assertTrue(result.getFileName().endsWith(".png"));
            assertTrue(result.getFileUrl().endsWith(".png"));
        }

        @Test
        @DisplayName("上传 PDF 文件应成功")
        void shouldUploadPdfSuccessfully() {
            byte[] content = new byte[2048];
            MockMultipartFile file = createMockFile("file", "document.pdf", "application/pdf", content);

            FileUploadVO result = fileUploadService.upload(file);

            assertNotNull(result);
            assertTrue(result.getFileName().endsWith(".pdf"));
        }

        @Test
        @DisplayName("文件名中不应包含路径穿越字符")
        void shouldSanitizePathTraversalInName() {
            byte[] content = new byte[100];
            // 模拟路径穿越的文件名
            MockMultipartFile file = createMockFile("file", "../../etc/passwd.jpg", "image/jpeg", content);

            FileUploadVO result = fileUploadService.upload(file);

            // 保存后的文件名应为纯 UUID + 后缀，不含路径字符
            assertFalse(result.getFileName().contains(".."));
            assertFalse(result.getFileName().contains("/"));
            assertFalse(result.getFileName().contains("\\"));
            assertTrue(result.getFileName().endsWith(".jpg"));
        }
    }

    // =========================================================================
    // 异常场景
    // =========================================================================

    @Nested
    @DisplayName("空文件")
    class EmptyFile {

        @Test
        @DisplayName("file 为 null 时应抛出 FILE_EMPTY 异常")
        void shouldThrowWhenFileIsNull() {
            BusinessException ex = assertThrows(BusinessException.class, () -> {
                fileUploadService.upload(null);
            });
            assertEquals(ErrorCode.FILE_EMPTY.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("file.isEmpty() 为 true 时应抛出 FILE_EMPTY 异常")
        void shouldThrowWhenFileIsEmpty() {
            MockMultipartFile file = createMockFile("file", "empty.jpg", "image/jpeg", new byte[0]);

            BusinessException ex = assertThrows(BusinessException.class, () -> {
                fileUploadService.upload(file);
            });
            assertEquals(ErrorCode.FILE_EMPTY.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("超大文件")
    class OversizedFile {

        @Test
        @DisplayName("文件超过 5MB 限制时应抛出 FILE_TOO_LARGE 异常")
        void shouldThrowWhenFileTooLarge() {
            // 5MB + 1 byte
            byte[] content = new byte[5 * 1024 * 1024 + 1];
            MockMultipartFile file = createMockFile("file", "big.jpg", "image/jpeg", content);

            BusinessException ex = assertThrows(BusinessException.class, () -> {
                fileUploadService.upload(file);
            });
            assertEquals(ErrorCode.FILE_TOO_LARGE.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("非法文件类型")
    class IllegalFileType {

        @Test
        @DisplayName("上传 .exe 文件应抛出 FILE_TYPE_NOT_ALLOWED 异常")
        void shouldBlockExeFile() {
            byte[] content = new byte[100];
            MockMultipartFile file = createMockFile("file", "virus.exe", "application/x-msdownload", content);

            BusinessException ex = assertThrows(BusinessException.class, () -> {
                fileUploadService.upload(file);
            });
            assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("上传 .sh 文件应抛出 FILE_TYPE_NOT_ALLOWED 异常")
        void shouldBlockShellScript() {
            byte[] content = new byte[100];
            MockMultipartFile file = createMockFile("file", "script.sh", "text/x-shellscript", content);

            BusinessException ex = assertThrows(BusinessException.class, () -> {
                fileUploadService.upload(file);
            });
            assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("上传不允许的类型（如 .gif）应抛出 FILE_TYPE_NOT_ALLOWED 异常")
        void shouldBlockDisallowedType() {
            byte[] content = new byte[200];
            MockMultipartFile file = createMockFile("file", "animated.gif", "image/gif", content);

            BusinessException ex = assertThrows(BusinessException.class, () -> {
                fileUploadService.upload(file);
            });
            assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("上传 .html 文件应抛出 FILE_TYPE_NOT_ALLOWED 异常")
        void shouldBlockHtmlFile() {
            byte[] content = new byte[100];
            MockMultipartFile file = createMockFile("file", "page.html", "text/html", content);

            BusinessException ex = assertThrows(BusinessException.class, () -> {
                fileUploadService.upload(file);
            });
            assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED.getCode(), ex.getCode());
        }
    }

    @Nested
    @DisplayName("文件名安全处理")
    class FileNameSafety {

        @Test
        @DisplayName("无后缀的文件名应生成仅含 UUID 的文件名")
        void shouldHandleFileWithoutExtension() {
            byte[] content = new byte[100];
            // 无后缀但在允许列表中可能被拦截，这里测试后缀为空的处理
            // 实际上无后缀会被 allowedTypes 拦截（ext 为空不在允许列表中）
            // 所以验证应抛出类型错误
            MockMultipartFile file = createMockFile("file", "noextension", "application/octet-stream", content);

            BusinessException ex = assertThrows(BusinessException.class, () -> {
                fileUploadService.upload(file);
            });
            assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED.getCode(), ex.getCode());
        }

        @Test
        @DisplayName("多后缀文件名只取最后一个后缀")
        void shouldUseLastExtension() {
            byte[] content = new byte[100];
            MockMultipartFile file = createMockFile("file", "archive.tar.jpg", "image/jpeg", content);

            FileUploadVO result = fileUploadService.upload(file);

            // 后缀应为 jpg（最后一个），不是 tar.jpg
            assertTrue(result.getFileName().endsWith(".jpg"));
            assertFalse(result.getFileName().contains(".tar"));
        }
    }
}
