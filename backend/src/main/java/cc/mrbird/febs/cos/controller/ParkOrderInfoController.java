package cc.mrbird.febs.cos.controller;


import cc.mrbird.febs.common.exception.FebsException;
import cc.mrbird.febs.common.utils.HolidayUtil;
import cc.mrbird.febs.common.utils.R;
import cc.mrbird.febs.common.utils.WeatherUtil;
import cc.mrbird.febs.cos.entity.*;
import cc.mrbird.febs.cos.service.*;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wuwenze.poi.ExcelKit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Fank gmail - fan1ke2ke@gmail.com
 */
@RestController
@RequestMapping("/cos/park-order-info")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ParkOrderInfoController {

    private final IParkOrderInfoService parkOrderInfoService;

    private final IUserInfoService userInfoService;

    private final IMemberInfoService memberInfoService;

    private final ISpaceInfoService spaceInfoService;

    private final IReserveInfoService reserveInfoService;

    private final ISpaceStatusInfoService spaceStatusInfoService;

    private final IMailService mailService;

    private final TemplateEngine templateEngine;

    private final IMessageInfoService messageInfoService;

    private final IVehicleInfoService vehicleInfoService;

    private final IParkInfoService parkInfoService;

    private final IParkRecordService parkRecordService;

    /**
     * 分页获取订单信息
     *
     * @param page          分页对象
     * @param parkOrderInfo 订单信息
     * @return 结果
     */
    @GetMapping("/page")
    public R page(Page<ParkOrderInfo> page, ParkOrderInfo parkOrderInfo) {
        return R.ok(parkOrderInfoService.selectOrderPage(page, parkOrderInfo));
    }

    /**
     * 开始停车
     *
     * @return 结果
     */
    @GetMapping("/startOrder")
    @Transactional(rollbackFor = Exception.class)
    public R startOrder(Integer vehicleId, Integer spaceId) throws FebsException {
        // 校验车辆是否可以添加订单
        List<ParkOrderInfo> orderInfoList = parkOrderInfoService.list(Wrappers.<ParkOrderInfo>lambdaQuery().eq(ParkOrderInfo::getVehicleId, vehicleId).eq(ParkOrderInfo::getStatus, "0"));
        if (CollectionUtil.isNotEmpty(orderInfoList)) {
            throw new FebsException("此车辆正在停车中或有未缴费订单");
        }

        // 是否正在预约中
        List<ReserveInfo> reserveInfoList = reserveInfoService.list(Wrappers.<ReserveInfo>lambdaQuery().eq(ReserveInfo::getVehicleId, vehicleId).eq(ReserveInfo::getStatus, "1"));
        if (CollectionUtil.isNotEmpty(reserveInfoList)) {
            List<Integer> ids = reserveInfoList.stream().map(ReserveInfo::getId).collect(Collectors.toList());

            List<Integer> spaceIds = reserveInfoList.stream().map(ReserveInfo::getSpaceId).collect(Collectors.toList());
            reserveInfoService.update(Wrappers.<ReserveInfo>lambdaUpdate().set(ReserveInfo::getStatus, "0").in(ReserveInfo::getId, ids));
            // 更新预约车位状态
            spaceStatusInfoService.update(Wrappers.<SpaceStatusInfo>lambdaUpdate().set(SpaceStatusInfo::getStatus, "0").in(SpaceStatusInfo::getSpaceId, spaceIds));
        }

        // 车位信息
        SpaceInfo spaceInfo = spaceInfoService.getById(spaceId);

        // 更新车位状态
        spaceStatusInfoService.update(Wrappers.<SpaceStatusInfo>lambdaUpdate().set(SpaceStatusInfo::getStatus, "1").eq(SpaceStatusInfo::getSpaceId, spaceInfo.getId()));

        // 车辆信息
        VehicleInfo vehicleInfo = vehicleInfoService.getById(vehicleId);
        // 用户信息
        UserInfo userInfo = userInfoService.getById(vehicleInfo.getUserId());

        // 发送消息
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setUserId(userInfo.getId());
        messageInfo.setContent("您好，您的车辆 " + vehicleInfo.getVehicleNumber() + "订单已生成，请注意查看");
        messageInfo.setCreateDate(DateUtil.formatDateTime(new Date()));
        messageInfoService.save(messageInfo);
        if (StrUtil.isNotEmpty(userInfo.getEmail())) {
            Context context = new Context();
            context.setVariable("today", DateUtil.formatDate(new Date()));
            context.setVariable("custom", userInfo.getName() + " 您好。您的车辆 " + vehicleInfo.getVehicleNumber() + " 订单已生成，请注意查看");
            String emailContent = templateEngine.process("registerEmail", context);
            mailService.sendHtmlMail(userInfo.getEmail(), DateUtil.formatDate(new Date()) + "车位订单提示", emailContent);
        }

        // 订单信息
        ParkOrderInfo parkOrderInfo = new ParkOrderInfo();
        parkOrderInfo.setVehicleId(vehicleId);
        parkOrderInfo.setSpaceId(spaceId);
        parkOrderInfo.setUserId(vehicleInfo.getUserId());

        parkOrderInfo.setCode("ORD-" + System.currentTimeMillis());
        parkOrderInfo.setPrice(spaceInfo.getPrice());
        parkOrderInfo.setStatus("0");
        parkOrderInfo.setStartDate(DateUtil.formatDateTime(new Date()));
        return R.ok(parkOrderInfoService.save(parkOrderInfo));
    }

    /**
     * 订单导出
     *
     * @param response HTTP 响应对象
     * @return 结果
     */
    @PostMapping("/order/down")
    public void downOrder(HttpServletResponse response) throws FebsException {
        try {
            List<LinkedHashMap<String, Object>> result = parkOrderInfoService.queryOrderList();
            List<ParkOrderInfo> orderList = JSONUtil.toList(JSONUtil.toJsonStr(result), ParkOrderInfo.class);
            ExcelKit.$Export(ParkOrderInfo.class, response).downXlsx(orderList, false);
        } catch (Exception e) {
            String message = "导出 Excel 失败";
            throw new FebsException(message);
        }
    }

    /**
     * 订单驶出结算
     *
     * @param orderCode 订单编号
     * @return 结果
     */
    @GetMapping("/order/over")
    @Transactional(rollbackFor = Exception.class)
    public R orderOver(String orderCode) {
        // 获取订单信息
        ParkOrderInfo orderInfo = parkOrderInfoService.getOne(Wrappers.<ParkOrderInfo>lambdaQuery().eq(ParkOrderInfo::getCode, orderCode));

        // 车辆信息
        VehicleInfo vehicleInfo = vehicleInfoService.getById(orderInfo.getVehicleId());

        // 获取用户会员信息
        UserInfo userInfo = userInfoService.getOne(Wrappers.<UserInfo>lambdaQuery().eq(UserInfo::getId, vehicleInfo.getUserId()));
        List<MemberInfo> memberInfos = memberInfoService.list(Wrappers.<MemberInfo>lambdaQuery().eq(MemberInfo::getUserId, userInfo.getId()));
        boolean isMember = false;
        String memberType = "";
        if (CollectionUtil.isNotEmpty(memberInfos)) {
            for (MemberInfo memberInfo : memberInfos) {
                if (DateUtil.isIn(new Date(), DateUtil.parseDateTime(memberInfo.getStartDate()), DateUtil.parseDateTime(memberInfo.getEndDate()))) {
                    isMember = true;
                    memberType = memberInfo.getMemberLevel();
                }
            }
        }

        orderInfo.setEndDate(DateUtil.formatDateTime(new Date()));
        // 停车总时常
        long totalTime = DateUtil.between(DateUtil.parseDateTime(orderInfo.getStartDate()), DateUtil.parseDateTime(orderInfo.getEndDate()), DateUnit.MINUTE);
        orderInfo.setTotalTime(BigDecimal.valueOf(totalTime));
        // 总价格
        if (isMember) {
            orderInfo.setTotalPrice(BigDecimal.ZERO);
            orderInfo.setStatus("1");
        } else {
            BigDecimal unit = NumberUtil.div(orderInfo.getTotalTime(), new BigDecimal(60), 2);
            orderInfo.setTotalPrice(NumberUtil.mul(unit, orderInfo.getPrice()));
        }

        // 发送消息
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setUserId(userInfo.getId());
        messageInfo.setContent("您好，您的车辆 " + vehicleInfo.getVehicleNumber() + "已驶出，请前往订单缴费");
        messageInfo.setCreateDate(DateUtil.formatDateTime(new Date()));
        messageInfoService.save(messageInfo);
        if (StrUtil.isNotEmpty(userInfo.getEmail())) {
            Context context = new Context();
            context.setVariable("today", DateUtil.formatDate(new Date()));
            context.setVariable("custom", userInfo.getName() + " 您好，您的车辆 " + vehicleInfo.getVehicleNumber() + "已驶出，请前往订单缴费");
            String emailContent = templateEngine.process("registerEmail", context);
            mailService.sendHtmlMail(userInfo.getEmail(), DateUtil.formatDate(new Date()) + "车位订单提示", emailContent);
        }

        SpaceInfo spaceInfo = spaceInfoService.getById(orderInfo.getSpaceId());
        ParkInfo parkInfo = parkInfoService.getById(spaceInfo.getLotId());
        // 添加停车记录
        ParkRecord parkRecord = new ParkRecord();
        parkRecord.setRecordId("R" + System.currentTimeMillis());
        parkRecord.setPlateNo(vehicleInfo.getVehicleNumber());
        parkRecord.setLotId(spaceInfo.getLotId());
        parkRecord.setLotName(parkInfo.getLotName());
        parkRecord.setLotType(parkInfo.getLotType());
        parkRecord.setEntryTime(orderInfo.getStartDate());
        parkRecord.setExitTime(orderInfo.getEndDate());
        parkRecord.setDurationMin(orderInfo.getTotalTime().doubleValue());
        if (isMember) {
            parkRecord.setUserType("1".equals(memberType) ? "月租车" : "年租车");
        } else {
            parkRecord.setUserType("临时车");
        }
        parkRecord.setSpaceNo(spaceInfo.getName());
        parkRecord.setPayAmount(orderInfo.getTotalPrice().doubleValue());
        parkRecord.setPayMethod(isMember ? "ETC" : "支付宝");

        Date date = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        int weekday = (dayOfWeek == Calendar.SUNDAY) ? 7 : dayOfWeek - 1;
        parkRecord.setWeekday(weekday);
        parkRecord.setIsHoliday(HolidayUtil.isHoliday(date) ? 1 : 0);
        boolean isAdjust = HolidayUtil.isWorkdayAdjust(date);
        parkRecord.setIsWorkdayAdjust(isAdjust ? 1 : 0);
        boolean isWeekendFlag = (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) && !isAdjust;
        parkRecord.setIsWeekend(isWeekendFlag ? 1 : 0);

        parkRecord.setWeather(WeatherUtil.getWeatherType());
        parkRecord.setTempAvg(WeatherUtil.getTempAvg());
        parkRecord.setWindLevel(WeatherUtil.getWindLevel());
        parkRecordService.save(parkRecord);

        // 车位状态变更
        SpaceStatusInfo spaceStatusInfo = spaceStatusInfoService.getOne(Wrappers.<SpaceStatusInfo>lambdaQuery().eq(SpaceStatusInfo::getSpaceId, orderInfo.getSpaceId()));
        spaceStatusInfo.setStatus("0");
        spaceStatusInfoService.updateById(spaceStatusInfo);
        return R.ok(parkOrderInfoService.updateById(orderInfo));
    }

    /**
     * 查询主页信息
     *
     * @return 结果
     */
    @GetMapping("/home/data")
    public R homeData() {
        return R.ok(parkOrderInfoService.homeData());
    }

    /**
     * 数据统计
     *
     * @param checkDate 选择日期
     * @return 结果
     */
    @GetMapping("/statistics")
    public R selectRoomStatistics(@RequestParam(value = "checkDate", required = false) String checkDate) {
        return R.ok(parkOrderInfoService.selectStatistics(checkDate));
    }

    /**
     * 订单信息详情
     *
     * @param id 订单ID
     * @return 结果
     */
    @GetMapping("/{id}")
    public R detail(@PathVariable("id") Integer id) {
        return R.ok(parkOrderInfoService.getById(id));
    }

    /**
     * 订单信息列表
     *
     * @return 结果
     */
    @GetMapping("/list")
    public R list() {
        return R.ok(parkOrderInfoService.list());
    }

    /**
     * 新增订单信息
     *
     * @param parkOrderInfo 订单信息
     * @return 结果
     */
    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public R save(ParkOrderInfo parkOrderInfo) throws FebsException {
        // 校验车辆是否可以添加订单
        List<ParkOrderInfo> orderInfoList = parkOrderInfoService.list(Wrappers.<ParkOrderInfo>lambdaQuery().eq(ParkOrderInfo::getVehicleId, parkOrderInfo.getVehicleId()).eq(ParkOrderInfo::getStatus, "0"));
        if (CollectionUtil.isNotEmpty(orderInfoList)) {
            throw new FebsException("此车辆正在停车中或有未缴费订单");
        }

        // 是否正在预约中
        List<ReserveInfo> reserveInfoList = reserveInfoService.list(Wrappers.<ReserveInfo>lambdaQuery().eq(ReserveInfo::getVehicleId, parkOrderInfo.getVehicleId()).eq(ReserveInfo::getStatus, "1"));
        if (CollectionUtil.isNotEmpty(reserveInfoList)) {
            List<Integer> ids = reserveInfoList.stream().map(ReserveInfo::getId).collect(Collectors.toList());

            List<Integer> spaceIds = reserveInfoList.stream().map(ReserveInfo::getSpaceId).collect(Collectors.toList());
            reserveInfoService.update(Wrappers.<ReserveInfo>lambdaUpdate().set(ReserveInfo::getStatus, "0").in(ReserveInfo::getId, ids));
            // 更新预约车位状态
            spaceStatusInfoService.update(Wrappers.<SpaceStatusInfo>lambdaUpdate().set(SpaceStatusInfo::getStatus, "0").in(SpaceStatusInfo::getSpaceId, spaceIds));
        }

        // 车位信息
        SpaceInfo spaceInfo = spaceInfoService.getById(parkOrderInfo.getSpaceId());

        // 更新车位状态
        spaceStatusInfoService.update(Wrappers.<SpaceStatusInfo>lambdaUpdate().set(SpaceStatusInfo::getStatus, "1").eq(SpaceStatusInfo::getSpaceId, spaceInfo.getId()));

        // 车辆信息
        VehicleInfo vehicleInfo = vehicleInfoService.getById(parkOrderInfo.getVehicleId());
        // 用户信息
        UserInfo userInfo = userInfoService.getById(vehicleInfo.getUserId());

        // 发送消息
        MessageInfo messageInfo = new MessageInfo();
        messageInfo.setUserId(userInfo.getId());
        messageInfo.setContent("您好，您的车辆 " + vehicleInfo.getVehicleNumber() + "订单已生成，请注意查看");
        messageInfo.setCreateDate(DateUtil.formatDateTime(new Date()));
        messageInfoService.save(messageInfo);
        if (StrUtil.isNotEmpty(userInfo.getEmail())) {
            Context context = new Context();
            context.setVariable("today", DateUtil.formatDate(new Date()));
            context.setVariable("custom", userInfo.getName() + " 您好。您的车辆 " + vehicleInfo.getVehicleNumber() + " 订单已生成，请注意查看");
            String emailContent = templateEngine.process("registerEmail", context);
            mailService.sendHtmlMail(userInfo.getEmail(), DateUtil.formatDate(new Date()) + "车位订单提示", emailContent);
        }

        // 订单信息
        parkOrderInfo.setCode("ORD-" + System.currentTimeMillis());
        parkOrderInfo.setPrice(spaceInfo.getPrice());
        parkOrderInfo.setStatus("0");
        parkOrderInfo.setStartDate(DateUtil.formatDateTime(new Date()));
        return R.ok(parkOrderInfoService.save(parkOrderInfo));
    }

    /**
     * 修改订单信息
     *
     * @param parkOrderInfo 订单信息
     * @return 结果
     */
    @PutMapping
    public R edit(ParkOrderInfo parkOrderInfo) {
        return R.ok(parkOrderInfoService.updateById(parkOrderInfo));
    }

    /**
     * 删除订单信息
     *
     * @param ids ids
     * @return 订单信息
     */
    @DeleteMapping("/{ids}")
    public R deleteByIds(@PathVariable("ids") List<Integer> ids) {
        return R.ok(parkOrderInfoService.removeByIds(ids));
    }
}
