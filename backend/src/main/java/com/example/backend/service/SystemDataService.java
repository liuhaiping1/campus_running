package com.example.backend.service;

import com.example.backend.vo.CategoryVO;
import com.example.backend.vo.DictDataVO;
import com.example.backend.vo.NoticePageVO;

import java.util.List;

/**
 * 系统基础数据服务接口
 * <p>
 * 提供字典、公告和任务分类等前台基础数据查询能力。
 * </p>
 */
public interface SystemDataService {

    /**
     * 根据字典类型查询启用状态的字典数据
     *
     * @param dictType 字典类型编码
     * @return 字典数据列表
     */
    List<DictDataVO> listDictData(String dictType);

    /**
     * 分页查询已发布公告
     *
     * @param noticeType 公告类型，可为空
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @return 公告分页结果
     */
    NoticePageVO listNotices(Integer noticeType, int pageNum, int pageSize);

    /**
     * 查询启用状态的任务分类
     *
     * @return 分类列表
     */
    List<CategoryVO> listCategories();
}
