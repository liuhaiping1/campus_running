package com.example.backend.service;

import com.example.backend.vo.CampusLocationVO;
import com.example.backend.vo.CategoryVO;
import com.example.backend.vo.DictDataVO;
import com.example.backend.vo.NoticePageVO;

import java.util.List;

/**
 * 系统基础数据服务接口
 * <p>
 * 提供字典、公告、任务分类和校园地点等前台基础数据查询能力。
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

    /**
     * 查询启用状态的校园常用地点
     *
     * @param locationType 地点类型筛选，可为空
     * @param keyword      关键词模糊搜索（地点名称/校区/楼栋/详细地址），可为空
     * @return 校园地点列表，按排序号升序排列
     */
    List<CampusLocationVO> listCampusLocations(Integer locationType, String keyword);
}
