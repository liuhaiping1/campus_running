package com.example.backend.service;

import com.example.backend.dto.request.AddressSaveRequest;
import com.example.backend.vo.AddressVO;

import java.util.List;

/**
 * 用户地址服务接口
 * <p>
 * 提供当前登录用户地址的查询、创建、修改、删除和默认地址设置能力。
 * </p>
 */
public interface AddressService {

    /**
     * 查询当前用户启用状态的地址列表
     *
     * @param userId 当前用户ID
     * @return 地址列表
     */
    List<AddressVO> list(Long userId);

    /**
     * 创建当前用户地址
     *
     * @param userId  当前用户ID
     * @param request 地址保存请求
     * @return 创建后的地址信息
     */
    AddressVO create(Long userId, AddressSaveRequest request);

    /**
     * 修改当前用户地址
     *
     * @param userId  当前用户ID
     * @param id      地址ID
     * @param request 地址保存请求
     * @return 修改后的地址信息
     */
    AddressVO update(Long userId, Long id, AddressSaveRequest request);

    /**
     * 删除当前用户地址
     *
     * @param userId 当前用户ID
     * @param id     地址ID
     */
    void delete(Long userId, Long id);

    /**
     * 设置当前用户默认地址
     *
     * @param userId 当前用户ID
     * @param id     地址ID
     * @return 默认地址信息
     */
    AddressVO setDefault(Long userId, Long id);
}
