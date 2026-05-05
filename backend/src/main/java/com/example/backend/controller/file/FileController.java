package com.example.backend.controller.file;

import com.example.backend.common.Result;
import com.example.backend.service.FileUploadService;
import com.example.backend.vo.FileUploadVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 * <p>
 * 提供本地文件上传接口，需要登录后访问。
 * </p>
 */
@RestController
@RequestMapping("/api")
public class FileController {

    private final FileUploadService fileUploadService;

    /**
     * 构造函数注入文件上传服务
     *
     * @param fileUploadService 文件上传服务
     */
    public FileController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    /**
     * 上传文件
     * <p>
     * 接收单个 MultipartFile，保存到本地文件系统并返回可访问 URL。
     * </p>
     *
     * @param file 上传的文件
     * @return 文件上传结果
     */
    @PostMapping("/file/upload")
    public Result<FileUploadVO> upload(@RequestParam("file") MultipartFile file) {
        FileUploadVO vo = fileUploadService.upload(file);
        return Result.success(vo);
    }
}
