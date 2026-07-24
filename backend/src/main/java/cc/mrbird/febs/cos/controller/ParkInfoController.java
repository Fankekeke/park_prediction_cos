package cc.mrbird.febs.cos.controller;


import cc.mrbird.febs.common.utils.R;
import cc.mrbird.febs.cos.entity.ParkInfo;
import cc.mrbird.febs.cos.service.IParkInfoService;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * @author Fank gmail - fan1ke2ke@gmail.com
 */
@RestController
@RequestMapping("/cos/park-info")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ParkInfoController {

    private final IParkInfoService parkInfoService;

    /**
     * 分页获取停车场信息
     *
     * @param page     分页对象
     * @param parkInfo 停车场信息
     * @return 结果
     */
    @GetMapping("/page")
    public R page(Page<ParkInfo> page, ParkInfo parkInfo) {
        return R.ok(parkInfoService.queryPage(page, parkInfo));
    }

    /**
     * 停车场信息详情
     *
     * @param id 停车场ID
     * @return 结果
     */
    @GetMapping("/{id}")
    public R detail(@PathVariable("id") Integer id) {
        return R.ok(parkInfoService.getById(id));
    }

    /**
     * 停车场信息列表
     *
     * @return 结果
     */
    @GetMapping("/list")
    public R list() {
        return R.ok(parkInfoService.list());
    }

    /**
     * 新增停车场信息
     *
     * @param parkInfo 停车场信息
     * @return 结果
     */
    @PostMapping
    public R save(ParkInfo parkInfo) {
        return R.ok(parkInfoService.save(parkInfo));
    }

    /**
     * 修改停车场信息
     *
     * @param parkInfo 停车场信息
     * @return 结果
     */
    @PutMapping
    public R edit(ParkInfo parkInfo) {
        return R.ok(parkInfoService.updateById(parkInfo));
    }

    /**
     * 删除停车场信息
     *
     * @param ids ids
     * @return 停车场信息
     */
    @DeleteMapping("/{ids}")
    public R deleteByIds(@PathVariable("ids") List<Integer> ids) {
        return R.ok(parkInfoService.removeByIds(ids));
    }
}
