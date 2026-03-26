package com.example.backend.controller.address;

import com.example.backend.common.Result;
import com.example.backend.dto.request.AddressSaveRequest;
import com.example.backend.security.LoginUser;
import com.example.backend.service.AddressService;
import com.example.backend.vo.AddressVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户地址控制器
 * <p>
 * 处理当前登录用户的地址管理请求。
 * </p>
 */
@RestController
@RequestMapping("/api/address")
public class AddressController {

    private final AddressService addressService;

    /**
     * 构造函数注入地址服务
     *
     * @param addressService 地址服务
     */
    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    /**
     * 查询当前用户地址列表
     *
     * @param loginUser 当前登录用户
     * @return 地址列表
     */
    @GetMapping
    public Result<List<AddressVO>> list(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(addressService.list(loginUser.getUserId()));
    }

    /**
     * 创建当前用户地址
     *
     * @param loginUser 当前登录用户
     * @param request   地址保存请求
     * @return 创建后的地址信息
     */
    @PostMapping
    public Result<AddressVO> create(@AuthenticationPrincipal LoginUser loginUser,
                                    @Valid @RequestBody AddressSaveRequest request) {
        return Result.success("地址创建成功", addressService.create(loginUser.getUserId(), request));
    }

    /**
     * 修改当前用户地址
     *
     * @param loginUser 当前登录用户
     * @param id        地址ID
     * @param request   地址保存请求
     * @return 修改后的地址信息
     */
    @PutMapping("/{id}")
    public Result<AddressVO> update(@AuthenticationPrincipal LoginUser loginUser,
                                    @PathVariable Long id,
                                    @Valid @RequestBody AddressSaveRequest request) {
        return Result.success("地址更新成功", addressService.update(loginUser.getUserId(), id, request));
    }

    /**
     * 删除当前用户地址
     *
     * @param loginUser 当前登录用户
     * @param id        地址ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        addressService.delete(loginUser.getUserId(), id);
        return Result.success();
    }

    /**
     * 设置当前用户默认地址
     *
     * @param loginUser 当前登录用户
     * @param id        地址ID
     * @return 默认地址信息
     */
    @PostMapping("/{id}/default")
    public Result<AddressVO> setDefault(@AuthenticationPrincipal LoginUser loginUser, @PathVariable Long id) {
        return Result.success("默认地址设置成功", addressService.setDefault(loginUser.getUserId(), id));
    }
}
