package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.AddressSaveRequest;
import com.example.backend.entity.StationMessage;
import com.example.backend.entity.UserAddress;
import com.example.backend.mapper.ErrandCategoryMapper;
import com.example.backend.mapper.StationMessageMapper;
import com.example.backend.mapper.SysDictDataMapper;
import com.example.backend.mapper.SysDictTypeMapper;
import com.example.backend.mapper.SystemNoticeMapper;
import com.example.backend.mapper.UserAddressMapper;
import com.example.backend.service.impl.AddressServiceImpl;
import com.example.backend.service.impl.MessageServiceImpl;
import com.example.backend.service.impl.SystemDataServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StageTwoServiceTest {

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), UserAddress.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), StationMessage.class);
    }

    @Mock
    private UserAddressMapper userAddressMapper;

    @Mock
    private StationMessageMapper stationMessageMapper;

    @Mock
    private SysDictTypeMapper sysDictTypeMapper;

    @Mock
    private SysDictDataMapper sysDictDataMapper;

    @Mock
    private SystemNoticeMapper systemNoticeMapper;

    @Mock
    private ErrandCategoryMapper errandCategoryMapper;

    @InjectMocks
    private AddressServiceImpl addressService;

    @InjectMocks
    private MessageServiceImpl messageService;

    @InjectMocks
    private SystemDataServiceImpl systemDataService;

    @Test
    @DisplayName("创建第 11 条地址时应抛出 ADDRESS_LIMIT_EXCEEDED")
    void shouldRejectAddressWhenLimitExceeded() {
        when(userAddressMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> addressService.create(1L, sampleAddressRequest(0)));

        assertEquals(ErrorCode.ADDRESS_LIMIT_EXCEEDED.getCode(), ex.getCode());
        verify(userAddressMapper, never()).insert(any(UserAddress.class));
    }

    @Test
    @DisplayName("创建默认地址时应先取消该用户其他默认地址")
    void shouldClearOtherDefaultsBeforeCreatingDefaultAddress() {
        when(userAddressMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(userAddressMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(userAddressMapper.insert(any(UserAddress.class))).thenAnswer(invocation -> {
            UserAddress address = invocation.getArgument(0);
            address.setId(100L);
            return 1;
        });

        addressService.create(1L, sampleAddressRequest(1));

        verify(userAddressMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
        verify(userAddressMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getUserId());
        assertEquals(1, captor.getValue().getIsDefault());
        assertEquals(1, captor.getValue().getAddressStatus());
    }

    @Test
    @DisplayName("用户不能修改别人的地址")
    void shouldRejectUpdatingOthersAddress() {
        UserAddress address = new UserAddress();
        address.setId(10L);
        address.setUserId(2L);
        when(userAddressMapper.selectById(10L)).thenReturn(address);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> addressService.update(1L, 10L, sampleAddressRequest(0)));

        assertEquals(ErrorCode.ADDRESS_NOT_OWNED.getCode(), ex.getCode());
        verify(userAddressMapper, never()).updateById(any(UserAddress.class));
    }

    @Test
    @DisplayName("删除自己的地址时应停用地址而不是调用删除")
    void shouldDisableAddressInsteadOfDeleting() {
        UserAddress address = new UserAddress();
        address.setId(10L);
        address.setUserId(1L);
        address.setAddressStatus(1);
        address.setIsDefault(1);
        when(userAddressMapper.selectById(10L)).thenReturn(address);
        when(userAddressMapper.updateById(any(UserAddress.class))).thenReturn(1);

        addressService.delete(1L, 10L);

        assertEquals(2, address.getAddressStatus());
        assertEquals(0, address.getIsDefault());
        verify(userAddressMapper).updateById(address);
        verify(userAddressMapper, never()).deleteById(any(Long.class));
    }

    @Test
    @DisplayName("消息只能由接收人读取")
    void shouldRejectReadingOthersMessage() {
        StationMessage message = new StationMessage();
        message.setId(11L);
        message.setReceiverUserId(2L);
        message.setIsRead(0);
        when(stationMessageMapper.selectById(11L)).thenReturn(message);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> messageService.markRead(1L, 11L));

        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        verify(stationMessageMapper, never()).updateById(any(StationMessage.class));
    }

    @Test
    @DisplayName("读取自己的未读消息时应设置已读时间")
    void shouldMarkOwnMessageAsRead() {
        StationMessage message = new StationMessage();
        message.setId(11L);
        message.setReceiverUserId(1L);
        message.setIsRead(0);
        when(stationMessageMapper.selectById(11L)).thenReturn(message);
        when(stationMessageMapper.updateById(any(StationMessage.class))).thenReturn(1);

        messageService.markRead(1L, 11L);

        assertEquals(1, message.getIsRead());
        assertNotNull(message.getReadTime());
        verify(stationMessageMapper).updateById(message);
    }

    @Test
    @DisplayName("不存在或停用的字典类型应抛出 DICT_TYPE_NOT_FOUND")
    void shouldRejectMissingDictType() {
        when(sysDictTypeMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> systemDataService.listDictData("missing"));

        assertEquals(ErrorCode.DICT_TYPE_NOT_FOUND.getCode(), ex.getCode());
        verify(sysDictDataMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    private AddressSaveRequest sampleAddressRequest(Integer isDefault) {
        AddressSaveRequest request = new AddressSaveRequest();
        request.setContactName("张三");
        request.setContactPhone("13800138000");
        request.setCampusName("主校区");
        request.setBuildingName("一号楼");
        request.setDetailAddress("101");
        request.setIsDefault(isDefault);
        return request;
    }
}
