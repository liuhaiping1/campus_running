package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.AddressSaveRequest;
import com.example.backend.entity.UserAddress;
import com.example.backend.mapper.UserAddressMapper;
import com.example.backend.service.AddressService;
import com.example.backend.vo.AddressVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户地址服务实现类
 * <p>
 * 实现地址数量限制、默认地址互斥和地址所属权校验。
 * </p>
 */
@Service
public class AddressServiceImpl implements AddressService {

    private static final int ENABLED = 1;
    private static final int DISABLED = 2;
    private static final int DEFAULT = 1;
    private static final int NOT_DEFAULT = 0;
    private static final long MAX_ADDRESS_COUNT = 10;

    private final UserAddressMapper userAddressMapper;

    /**
     * 构造函数注入用户地址Mapper
     *
     * @param userAddressMapper 用户地址Mapper
     */
    public AddressServiceImpl(UserAddressMapper userAddressMapper) {
        this.userAddressMapper = userAddressMapper;
    }

    /**
     * 查询当前用户启用地址列表
     *
     * @param userId 当前用户ID
     * @return 地址列表
     */
    @Override
    public List<AddressVO> list(Long userId) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getAddressStatus, ENABLED)
                .orderByDesc(UserAddress::getIsDefault)
                .orderByDesc(UserAddress::getCreateTime);
        return userAddressMapper.selectList(wrapper).stream()
                .map(AddressVO::from)
                .collect(Collectors.toList());
    }

    /**
     * 创建当前用户地址
     * <p>
     * 地址最多允许10条；创建默认地址时会先取消其他默认地址。
     * </p>
     *
     * @param userId  当前用户ID
     * @param request 地址保存请求
     * @return 创建后的地址信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO create(Long userId, AddressSaveRequest request) {
        LambdaQueryWrapper<UserAddress> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getAddressStatus, ENABLED);
        if (userAddressMapper.selectCount(countWrapper) >= MAX_ADDRESS_COUNT) {
            throw new BusinessException(ErrorCode.ADDRESS_LIMIT_EXCEEDED);
        }

        if (DEFAULT == normalizedDefault(request)) {
            clearDefault(userId);
        }

        UserAddress address = new UserAddress();
        fill(address, userId, request);
        userAddressMapper.insert(address);
        return AddressVO.from(address);
    }

    /**
     * 修改当前用户地址
     * <p>
     * 修改前校验地址归属；请求设置默认地址时会先取消其他默认地址。
     * </p>
     *
     * @param userId  当前用户ID
     * @param id      地址ID
     * @param request 地址保存请求
     * @return 修改后的地址信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO update(Long userId, Long id, AddressSaveRequest request) {
        UserAddress address = requireOwnedAddress(userId, id);
        if (DEFAULT == normalizedDefault(request)) {
            clearDefault(userId);
        }
        fill(address, userId, request);
        userAddressMapper.updateById(address);
        return AddressVO.from(address);
    }

    /**
     * 删除当前用户地址
     * <p>
     * 通过停用地址实现业务删除，保留历史数据。
     * </p>
     *
     * @param userId 当前用户ID
     * @param id     地址ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        UserAddress address = requireOwnedAddress(userId, id);
        address.setAddressStatus(DISABLED);
        address.setIsDefault(NOT_DEFAULT);
        userAddressMapper.updateById(address);
    }

    /**
     * 设置当前用户默认地址
     *
     * @param userId 当前用户ID
     * @param id     地址ID
     * @return 默认地址信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AddressVO setDefault(Long userId, Long id) {
        UserAddress address = requireOwnedAddress(userId, id);
        clearDefault(userId);
        address.setIsDefault(DEFAULT);
        userAddressMapper.updateById(address);
        return AddressVO.from(address);
    }

    /**
     * 查询并校验地址归属
     *
     * @param userId 当前用户ID
     * @param id     地址ID
     * @return 地址实体
     */
    private UserAddress requireOwnedAddress(Long userId, Long id) {
        UserAddress address = userAddressMapper.selectById(id);
        if (address == null) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        if (!userId.equals(address.getUserId())) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_OWNED);
        }
        return address;
    }

    /**
     * 将请求字段填充到地址实体
     *
     * @param address 地址实体
     * @param userId  当前用户ID
     * @param request 地址保存请求
     */
    private void fill(UserAddress address, Long userId, AddressSaveRequest request) {
        address.setUserId(userId);
        address.setContactName(request.getContactName());
        address.setContactPhone(request.getContactPhone());
        address.setCampusName(request.getCampusName());
        address.setBuildingName(request.getBuildingName());
        address.setDetailAddress(request.getDetailAddress());
        address.setLongitude(request.getLongitude());
        address.setLatitude(request.getLatitude());
        address.setIsDefault(normalizedDefault(request));
        address.setAddressStatus(ENABLED);
    }

    /**
     * 规范化默认地址标记
     *
     * @param request 地址保存请求
     * @return 默认地址标记
     */
    private int normalizedDefault(AddressSaveRequest request) {
        return Integer.valueOf(DEFAULT).equals(request.getIsDefault()) ? DEFAULT : NOT_DEFAULT;
    }

    /**
     * 清除当前用户其他默认地址
     *
     * @param userId 当前用户ID
     */
    private void clearDefault(Long userId) {
        LambdaUpdateWrapper<UserAddress> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId)
                .eq(UserAddress::getIsDefault, DEFAULT)
                .set(UserAddress::getIsDefault, NOT_DEFAULT);
        userAddressMapper.update(null, wrapper);
    }
}
