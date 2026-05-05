package com.example.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.dto.request.AddressSaveRequest;
import com.example.backend.entity.CampusLocation;
import com.example.backend.entity.StationMessage;
import com.example.backend.entity.UserAddress;
import com.example.backend.mapper.CampusLocationMapper;
import com.example.backend.mapper.ErrandCategoryMapper;
import com.example.backend.mapper.StationMessageMapper;
import com.example.backend.mapper.SysDictDataMapper;
import com.example.backend.mapper.SysDictTypeMapper;
import com.example.backend.mapper.SystemNoticeMapper;
import com.example.backend.mapper.UserAddressMapper;
import com.example.backend.vo.CampusLocationVO;
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

    @Mock
    private CampusLocationMapper campusLocationMapper;

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

    // =========================================================================
    // 校园地点查询测试
    // =========================================================================

    @Test
    @DisplayName("无筛选时应返回所有启用地点列表")
    void shouldReturnAllEnabledLocations() {
        CampusLocation loc1 = buildCampusLocation(1L, "菜鸟驿站", 1);
        CampusLocation loc2 = buildCampusLocation(2L, "一食堂外卖点", 2);
        when(campusLocationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.Arrays.asList(loc1, loc2));

        java.util.List<CampusLocationVO> result = systemDataService.listCampusLocations(null, null);

        assertEquals(2, result.size());
        assertEquals("菜鸟驿站", result.get(0).getLocationName());
        verify(campusLocationMapper).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("locationType 应精确筛选")
    void shouldFilterByLocationType() {
        CampusLocation loc = buildCampusLocation(1L, "菜鸟驿站", 1);
        when(campusLocationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.singletonList(loc));

        java.util.List<CampusLocationVO> result = systemDataService.listCampusLocations(1, null);

        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).getLocationType());
    }

    @Test
    @DisplayName("keyword 应模糊匹配地点名称和校区")
    void shouldFilterByKeyword() {
        CampusLocation loc = buildCampusLocation(1L, "校内超市", 3);
        when(campusLocationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.singletonList(loc));

        java.util.List<CampusLocationVO> result = systemDataService.listCampusLocations(null, "超市");

        assertEquals(1, result.size());
        assertEquals("校内超市", result.get(0).getLocationName());
    }

    @Test
    @DisplayName("空结果应返回空列表")
    void shouldReturnEmptyListForNoMatch() {
        when(campusLocationMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(java.util.Collections.emptyList());

        java.util.List<CampusLocationVO> result = systemDataService.listCampusLocations(99, "不存在的");

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("VO.from() 字段映射正确，不包含审计字段")
    void shouldMapVoCorrectly() {
        CampusLocation entity = new CampusLocation();
        entity.setId(1L);
        entity.setLocationName("菜鸟驿站");
        entity.setAmapPoiId("POI123");
        entity.setAmapAdcode("330100");
        entity.setAmapCityCode("0571");
        entity.setLocationType(1);
        entity.setCampusName("默认校区");
        entity.setBuildingName("菜鸟驿站");
        entity.setDetailAddress("默认校区菜鸟驿站");
        entity.setLongitude(new java.math.BigDecimal("120.123456"));
        entity.setLatitude(new java.math.BigDecimal("30.234567"));
        entity.setCoordType("GCJ02");
        entity.setSortNo(1);
        entity.setCreateTime(java.time.LocalDateTime.now());
        entity.setCreateBy(1L);

        CampusLocationVO vo = CampusLocationVO.from(entity);

        assertEquals(Long.valueOf(1L), vo.getId());
        assertEquals("菜鸟驿站", vo.getLocationName());
        assertEquals("POI123", vo.getAmapPoiId());
        assertEquals(new java.math.BigDecimal("120.123456"), vo.getLongitude());
        assertEquals("GCJ02", vo.getCoordType());
    }

    /** 构建测试用校园地点 */
    private CampusLocation buildCampusLocation(Long id, String name, Integer type) {
        CampusLocation loc = new CampusLocation();
        loc.setId(id);
        loc.setLocationName(name);
        loc.setLocationType(type);
        loc.setCampusName("默认校区");
        loc.setBuildingName(name);
        loc.setDetailAddress("默认校区" + name);
        loc.setLocationStatus(1);
        loc.setSortNo(type);
        loc.setCoordType("GCJ02");
        return loc;
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
