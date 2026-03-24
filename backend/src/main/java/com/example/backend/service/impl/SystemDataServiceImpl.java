package com.example.backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.backend.common.ErrorCode;
import com.example.backend.common.enums.NoticeStatusEnum;
import com.example.backend.common.exception.BusinessException;
import com.example.backend.entity.ErrandCategory;
import com.example.backend.entity.SysDictData;
import com.example.backend.entity.SysDictType;
import com.example.backend.entity.SystemNotice;
import com.example.backend.mapper.ErrandCategoryMapper;
import com.example.backend.mapper.SysDictDataMapper;
import com.example.backend.mapper.SysDictTypeMapper;
import com.example.backend.mapper.SystemNoticeMapper;
import com.example.backend.service.SystemDataService;
import com.example.backend.vo.CategoryVO;
import com.example.backend.vo.DictDataVO;
import com.example.backend.vo.NoticePageVO;
import com.example.backend.vo.NoticeVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统基础数据服务实现类
 * <p>
 * 查询字典、公告和任务分类等前台基础数据。
 * </p>
 */
@Service
public class SystemDataServiceImpl implements SystemDataService {

    private static final int ENABLED = 1;

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictDataMapper sysDictDataMapper;
    private final SystemNoticeMapper systemNoticeMapper;
    private final ErrandCategoryMapper errandCategoryMapper;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数注入系统基础数据相关Mapper
     *
     * @param sysDictTypeMapper    字典类型Mapper
     * @param sysDictDataMapper    字典数据Mapper
     * @param systemNoticeMapper   系统公告Mapper
     * @param errandCategoryMapper 任务分类Mapper
     * @param objectMapper         JSON处理器
     */
    public SystemDataServiceImpl(SysDictTypeMapper sysDictTypeMapper,
                                 SysDictDataMapper sysDictDataMapper,
                                 SystemNoticeMapper systemNoticeMapper,
                                 ErrandCategoryMapper errandCategoryMapper,
                                 ObjectMapper objectMapper) {
        this.sysDictTypeMapper = sysDictTypeMapper;
        this.sysDictDataMapper = sysDictDataMapper;
        this.systemNoticeMapper = systemNoticeMapper;
        this.errandCategoryMapper = errandCategoryMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 根据字典类型查询启用状态的字典数据
     *
     * @param dictType 字典类型编码
     * @return 字典数据列表
     */
    @Override
    public List<DictDataVO> listDictData(String dictType) {
        LambdaQueryWrapper<SysDictType> typeWrapper = new LambdaQueryWrapper<>();
        typeWrapper.eq(SysDictType::getDictType, dictType)
                .eq(SysDictType::getDictStatus, ENABLED);
        if (sysDictTypeMapper.selectCount(typeWrapper) == 0) {
            throw new BusinessException(ErrorCode.DICT_TYPE_NOT_FOUND);
        }

        LambdaQueryWrapper<SysDictData> dataWrapper = new LambdaQueryWrapper<>();
        dataWrapper.eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getDataStatus, ENABLED)
                .orderByAsc(SysDictData::getSortNo);
        return sysDictDataMapper.selectList(dataWrapper).stream()
                .map(DictDataVO::from)
                .collect(Collectors.toList());
    }

    /**
     * 分页查询已发布公告
     *
     * @param noticeType 公告类型，可为空
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @return 公告分页结果
     */
    @Override
    public NoticePageVO listNotices(Integer noticeType, int pageNum, int pageSize) {
        Page<SystemNotice> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SystemNotice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemNotice::getNoticeStatus, NoticeStatusEnum.PUBLISHED.getCode());
        if (noticeType != null) {
            wrapper.eq(SystemNotice::getNoticeType, noticeType);
        }
        wrapper.orderByDesc(SystemNotice::getPublishTime);

        Page<SystemNotice> result = systemNoticeMapper.selectPage(page, wrapper);
        return NoticePageVO.builder()
                .total(result.getTotal())
                .records(result.getRecords().stream().map(NoticeVO::from).collect(Collectors.toList()))
                .build();
    }

    /**
     * 查询启用状态的任务分类
     *
     * @return 分类列表
     */
    @Override
    public List<CategoryVO> listCategories() {
        LambdaQueryWrapper<ErrandCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ErrandCategory::getCategoryStatus, ENABLED)
                .orderByAsc(ErrandCategory::getSortNo)
                .orderByDesc(ErrandCategory::getCreateTime);
        return errandCategoryMapper.selectList(wrapper).stream()
                .map(category -> CategoryVO.from(category, objectMapper))
                .collect(Collectors.toList());
    }
}
