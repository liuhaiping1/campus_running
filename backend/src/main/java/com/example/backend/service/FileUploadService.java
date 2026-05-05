package com.example.backend.service;

import com.example.backend.vo.FileUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务接口
 */
public interface FileUploadService {

    /**
     * 上传文件到本地存储
     *
     * @param file 上传的文件
     * @return 文件上传结果 VO
     */
    FileUploadVO upload(MultipartFile file);
}
