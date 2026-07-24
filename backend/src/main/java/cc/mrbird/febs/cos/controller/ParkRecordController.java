package cc.mrbird.febs.cos.controller;


import cc.mrbird.febs.common.utils.R;
import cc.mrbird.febs.cos.entity.ParkRecord;
import cc.mrbird.febs.cos.service.IParkRecordService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author Fank gmail - fan1ke2ke@gmail.com
 */
@RestController
@RequestMapping("/cos/park-record")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ParkRecordController {

    private final IParkRecordService parkRecordService;

    /**
     * 分页获取停车记录信息
     *
     * @param page       分页对象
     * @param parkRecord 停车记录信息
     * @return 结果
     */
    @GetMapping("/page")
    public R page(Page<ParkRecord> page, ParkRecord parkRecord) {
        return R.ok(parkRecordService.queryPage(page, parkRecord));
    }

    /**
     * 获取停车记录信息趋势
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/trend")
    public R queryTrend(String date) {
        return R.ok(parkRecordService.queryTrend(date));
    }

    /**
     * 获取停车记录信息峰值分析
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/peakAnalysis")
    public R queryPeakAnalysis(String date) {
        return R.ok(parkRecordService.queryPeakAnalysis(date));
    }

    /**
     * 获取停车记录信息停车场排名
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/parkNumRank")
    public R queryParkNumRank(String date) {
        return R.ok(parkRecordService.queryParkNumRank(date));
    }

    /**
     * 获取停车记录信息收入排名
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/incomeRank")
    public R queryIncomeRank(String date) {
        return R.ok(parkRecordService.queryIncomeRank(date));
    }

    /**
     * 获取停车记录信息停车时长分析
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/parkingTime")
    public R queryParkingTime(String date) {
        return R.ok(parkRecordService.queryParkingTime(date));
    }

    /**
     * 获取停车记录信息用户类型贡献和收入
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/userTypeContributionAndIncome")
    public R queryUserTypeContributionAndIncome(String date) {
        return R.ok(parkRecordService.queryUserTypeContributionAndIncome(date));
    }

    /**
     * 获取停车记录信息工作日与周末停车量比较
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/workdayVsWeekendParkingVolumeComparison")
    public R queryWorkdayVsWeekendParkingVolumeComparison(String date) {
        return R.ok(parkRecordService.queryWorkdayVsWeekendParkingVolumeComparison(date));
    }

    /**
     * 获取停车记录信息节假日停车变化
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/holidayParkingChanges")
    public R queryHolidayParkingChanges(String date) {
        return R.ok(parkRecordService.queryHolidayParkingChanges(date));
    }

    /**
     * 获取停车记录信息停车模式
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/parkingPattern")
    public R queryParkingPattern(String date) {
        return R.ok(parkRecordService.queryParkingPattern(date));
    }

    /**
     * 获取停车记录信息天气和风向对停车量的影响
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/weatherAndWindImpactOnParkingVolume")
    public R queryWeatherAndWindImpactOnParkingVolume(String date) {
        return R.ok(parkRecordService.queryWeatherAndWindImpactOnParkingVolume(date));
    }

    /**
     * 获取停车记录信息业主资料和行为分析
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/ownerProfileAndBehaviorAnalysis")
    public R queryOwnerProfileAndBehaviorAnalysis(String date) {
        return R.ok(parkRecordService.queryOwnerProfileAndBehaviorAnalysis(date));
    }

    /**
     * 获取停车记录信息长时间停车特殊监控
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/longTimeParkingSpecialMonitoring")
    public R queryLongTimeParkingSpecialMonitoring(String date) {
        return R.ok(parkRecordService.queryLongTimeParkingSpecialMonitoring(date));
    }

    /**
     * 获取停车记录信息车辆重复停车时间分析
     *
     * @param date 日期
     * @return 结果
     */
    @GetMapping("/carRepeatParkingTimeAlysis")
    public R queryCarRepeatParkingTimeAlysis(String date) {
        return R.ok(parkRecordService.queryCarRepeatParkingTimeAlysis(date));
    }

    /**
     * 获取停车记录信息LSTM预测
     *
     * @return 结果
     */
    @GetMapping("/lstmPredict")
    public R queryLstmPredict() {
        return R.ok(parkRecordService.queryLstmPredict());
    }

    /**
     * 停车记录信息详情
     *
     * @param id 停车记录ID
     * @return 结果
     */
    @GetMapping("/{id}")
    public R detail(@PathVariable("id") Integer id) {
        return R.ok(parkRecordService.getById(id));
    }

    /**
     * 停车记录信息列表
     *
     * @return 结果
     */
    @GetMapping("/list")
    public R list() {
        return R.ok(parkRecordService.list());
    }

    /**
     * 新增停车记录信息
     *
     * @param parkRecord 停车记录信息
     * @return 结果
     */
    @PostMapping
    public R save(ParkRecord parkRecord) {
        return R.ok(parkRecordService.save(parkRecord));
    }

    /**
     * 修改停车记录信息
     *
     * @param parkRecord 停车记录信息
     * @return 结果
     */
    @PutMapping
    public R edit(ParkRecord parkRecord) {
        return R.ok(parkRecordService.updateById(parkRecord));
    }

    /**
     * 删除停车记录信息
     *
     * @param ids ids
     * @return 停车记录信息
     */
    @DeleteMapping("/{ids}")
    public R deleteByIds(@PathVariable("ids") List<Integer> ids) {
        return R.ok(parkRecordService.removeByIds(ids));
    }
}
