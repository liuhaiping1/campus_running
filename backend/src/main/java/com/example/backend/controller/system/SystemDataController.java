package com.example.backend.controller.system;

import com.example.backend.common.Result;
import com.example.backend.service.SystemDataService;
import com.example.backend.vo.CampusLocationVO;
import com.example.backend.vo.CategoryVO;
import com.example.backend.vo.DictDataVO;
import com.example.backend.vo.NoticePageVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统基础数据控制器
 * <p>
 * 提供字典、公告和任务分类等前台基础数据接口。
 * </p>
 */
@RestController
@RequestMapping("/api")
public class SystemDataController {

    private final SystemDataService systemDataService;

    /**
     * 构造函数注入系统基础数据服务
     *
     * @param systemDataService 系统基础数据服务
     */
    public SystemDataController(SystemDataService systemDataService) {
        this.systemDataService = systemDataService;
    }

    /**
     * 查询指定类型的字典数据
     *
     * @param dictType 字典类型编码
     * @return 字典数据列表
     */
    @GetMapping("/system/dict/{dictType}")
    public Result<List<DictDataVO>> listDict(@PathVariable String dictType) {
        return Result.success(systemDataService.listDictData(dictType));
    }

    /**
     * 分页查询已发布公告
     *
     * @param noticeType 公告类型，可为空
     * @param pageNum    页码
     * @param pageSize   每页数量
     * @return 公告分页结果
     */
    @GetMapping("/notice/list")
    public Result<NoticePageVO> listNotices(@RequestParam(required = false) Integer noticeType,
                                            @RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(systemDataService.listNotices(noticeType, pageNum, pageSize));
    }

    /**
     * 查询启用状态的任务分类
     *
     * @return 分类列表
     */
    @GetMapping("/category/list")
    public Result<List<CategoryVO>> listCategories() {
        return Result.success(systemDataService.listCategories());
    }

    /**
     * 查询启用状态的校园常用地点
     *
     * @param locationType 地点类型筛选，可为空
     * @param keyword      关键词模糊搜索，可为空
     * @return 校园地点列表
     */
    @GetMapping("/campus-location/list")
    public Result<List<CampusLocationVO>> listCampusLocations(
            @RequestParam(required = false) Integer locationType,
            @RequestParam(required = false) String keyword) {
        return Result.success(systemDataService.listCampusLocations(locationType, keyword));
    }
}
