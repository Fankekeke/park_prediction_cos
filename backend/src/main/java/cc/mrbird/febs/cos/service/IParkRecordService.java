package cc.mrbird.febs.cos.service;

import cc.mrbird.febs.cos.entity.ParkRecord;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import java.util.LinkedHashMap;

/**
 * @author Fank gmail - fan1ke2ke@gmail.com
 */
public interface IParkRecordService extends IService<ParkRecord> {

    /**
     * 分页获取停车记录信息
     *
     * @param page       分页对象
     * @param parkRecord 停车记录信息
     * @return 结果
     */
    IPage<LinkedHashMap<String, Object>> queryPage(Page<ParkRecord> page, ParkRecord parkRecord);

    /**
     * 获取停车记录信息趋势
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryTrend(String date);

    /**
     * 获取停车记录信息峰值分析
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryPeakAnalysis(String date);

    /**
     * 获取停车记录信息停车场排名
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryParkNumRank(String date);

    /**
     * 获取停车记录信息收入排名
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryIncomeRank(String date);

    /**
     * 获取停车记录信息停车时长分析
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryParkingTime(String date);

    /**
     * 获取停车记录信息用户类型贡献和收入
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryUserTypeContributionAndIncome(String date);

    /**
     * 获取停车记录信息工作日与周末停车量比较
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryWorkdayVsWeekendParkingVolumeComparison(String date);

    /**
     * 获取停车记录信息节假日停车变化
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryHolidayParkingChanges(String date);

    /**
     * 获取停车记录信息停车模式
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryParkingPattern(String date);

    /**
     * 获取停车记录信息天气和风向对停车量的影响
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryWeatherAndWindImpactOnParkingVolume(String date);

    /**
     * 获取停车记录信息业主资料和行为分析
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryOwnerProfileAndBehaviorAnalysis(String date);

    /**
     * 获取停车记录信息长时间停车特殊监控
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryLongTimeParkingSpecialMonitoring(String date);

    /**
     * 获取停车记录信息车辆重复停车时间分析
     *
     * @param date 日期
     * @return 结果
     */
    LinkedHashMap<String, Object> queryCarRepeatParkingTimeAlysis(String date);

    /**
     * 获取停车记录信息LSTM预测
     *
     * @return 结果
     */
    LinkedHashMap<String, Object> queryLstmPredict();
}
