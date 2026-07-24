
package cc.mrbird.febs.cos.service.impl;

import cc.mrbird.febs.common.utils.HolidayUtil;
import cc.mrbird.febs.cos.entity.ParkRecord;
import cc.mrbird.febs.cos.service.IParkPredictionService;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Deeplearning4j LSTM停车场车流量预测实现 *
 *
 * @author Fank gmail - fan1ke2ke@gmail.com
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ParkPredictionServiceImpl implements IParkPredictionService {

    private final ParkRecordServiceImpl parkRecordService;

    /**
     * 滑动窗口大小（天）
     */
    private static final int WINDOW_SIZE = 14;
    /**
     * LSTM隐藏层大小
     */
    private static final int HIDDEN_SIZE = 32;
    /**
     * 训练轮数
     */
    private static final int EPOCHS = 80;
    /**
     * 学习率
     */
    private static final double LEARNING_RATE = 0.005;
    /**
     * 随机种子
     */
    private static final int SEED = 123;
    /**
     * 最少需要的历史数据天数
     */
    private static final int MIN_DATA_DAYS = 30;

    /**
     * 天气编码映射
     */
    private static final Map<String, Double> WEATHER_MAP = new LinkedHashMap<>();

    static {
        WEATHER_MAP.put("晴", 0.0);
        WEATHER_MAP.put("多云", 1.0);
        WEATHER_MAP.put("阴", 2.0);
        WEATHER_MAP.put("雨", 3.0);
        WEATHER_MAP.put("雪", 4.0);
        WEATHER_MAP.put("未知", 5.0);
    }

    private static final int FEATURE_DIM = 13;

    @Override
    public LinkedHashMap<String, Object> predictNextMonth() {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();

        List<ParkRecord> allRecords = parkRecordService.list();
        if (allRecords == null || allRecords.isEmpty()) {
            result.put("status", "error");
            result.put("message", "暂无停车记录数据");
            return result;
        }

        Map<String, List<ParkRecord>> lotGroup = allRecords.stream()
                .filter(r -> StrUtil.isNotEmpty(r.getEntryTime()) && r.getEntryTime().length() >= 10)
                .collect(Collectors.groupingBy(ParkRecord::getLotId));

        Date today = new Date();
        List<String> futureDates = buildFutureDateList(today, 30);

        List<LinkedHashMap<String, Object>> lotPredictions = new ArrayList<>();
        LinkedHashMap<String, Object> summary = new LinkedHashMap<>();
        int successCount = 0;
        int failCount = 0;
        double totalPredictedAvg = 0;

        for (Map.Entry<String, List<ParkRecord>> entry : lotGroup.entrySet()) {
            String lotId = entry.getKey();
            List<ParkRecord> records = entry.getValue();
            String lotName = records.get(0).getLotName();

            try {
                List<DailyDataPoint> dailySeries = buildDailySeries(records);
                if (dailySeries.size() < MIN_DATA_DAYS) {
                    LinkedHashMap<String, Object> skipLot = new LinkedHashMap<>();
                    skipLot.put("lotId", lotId);
                    skipLot.put("lotName", lotName);
                    skipLot.put("status", "skip");
                    skipLot.put("message", "历史数据不足" + MIN_DATA_DAYS + "天，当前仅" + dailySeries.size() + "天");
                    lotPredictions.add(skipLot);
                    failCount++;
                    continue;
                }

                double countMax = dailySeries.stream().mapToDouble(d -> d.count).max().orElse(1.0);

                List<double[]> featureList = new ArrayList<>();
                for (DailyDataPoint dp : dailySeries) {
                    featureList.add(buildFeatureVector(dp, countMax));
                }

                INDArray trainInput;
                INDArray trainLabels;
                try {
                    List<INDArray> windows = buildSlidingWindows(featureList, countMax);
                    trainInput = windows.get(0);
                    trainLabels = windows.get(1);
                } catch (Exception e) {
                    LinkedHashMap<String, Object> errLot = new LinkedHashMap<>();
                    errLot.put("lotId", lotId);
                    errLot.put("lotName", lotName);
                    errLot.put("status", "error");
                    errLot.put("message", "构建训练数据失败: " + e.getMessage());
                    lotPredictions.add(errLot);
                    failCount++;
                    continue;
                }

                MultiLayerNetwork model = buildModel();
                try {
                    for (int epoch = 0; epoch < EPOCHS; epoch++) {
                        model.fit(trainInput, trainLabels);
                    }
                } catch (Exception e) {
                    LinkedHashMap<String, Object> errLot = new LinkedHashMap<>();
                    errLot.put("lotId", lotId);
                    errLot.put("lotName", lotName);
                    errLot.put("status", "error");
                    errLot.put("message", "模型训练失败: " + e.getMessage());
                    lotPredictions.add(errLot);
                    failCount++;
                    continue;
                }

                List<Double> predictions = iterativePredict(model, featureList, futureDates, countMax);

                List<LinkedHashMap<String, Object>> forecastList = new ArrayList<>();
                double sumPred = 0;
                for (int i = 0; i < futureDates.size(); i++) {
                    LinkedHashMap<String, Object> day = new LinkedHashMap<>();
                    day.put("date", futureDates.get(i));
                    day.put("predictedCount", Math.round(predictions.get(i) * 10.0) / 10.0);
                    forecastList.add(day);
                    sumPred += predictions.get(i);
                }
                double avgPred = sumPred / futureDates.size();

                List<LinkedHashMap<String, Object>> historyList = new ArrayList<>();
                for (DailyDataPoint dp : dailySeries) {
                    LinkedHashMap<String, Object> hd = new LinkedHashMap<>();
                    hd.put("date", dp.date);
                    hd.put("count", dp.count);
                    historyList.add(hd);
                }

                LinkedHashMap<String, Object> lotResult = new LinkedHashMap<>();
                lotResult.put("lotId", lotId);
                lotResult.put("lotName", lotName);
                lotResult.put("status", "success");
                lotResult.put("historyDays", dailySeries.size());
                lotResult.put("historyMax", countMax);
                lotResult.put("history", historyList);
                lotResult.put("forecast", forecastList);
                lotResult.put("avgDailyPredicted", Math.round(avgPred * 10.0) / 10.0);
                lotResult.put("totalPredicted", Math.round(sumPred * 10.0) / 10.0);

                lotPredictions.add(lotResult);
                successCount++;
                totalPredictedAvg += avgPred;
            } catch (Exception e) {
                log.error("车场 {}预测异常", lotId, e);
                LinkedHashMap<String, Object> errLot = new LinkedHashMap<>();
                errLot.put("lotId", lotId);
                errLot.put("lotName", StrUtil.isNotEmpty(records.get(0).getLotName()) ? records.get(0).getLotName() : "未知");
                errLot.put("status", "error");
                errLot.put("message", e.getMessage());
                lotPredictions.add(errLot);
                failCount++;
            }
        }

        summary.put("totalLots", lotGroup.size());
        summary.put("successLots", successCount);
        summary.put("failLots", failCount);
        summary.put("predictDays", 30);
        summary.put("predictStartDate", futureDates.get(0));
        summary.put("predictEndDate", futureDates.get(futureDates.size() - 1));
        if (successCount > 0) {
            summary.put("overallAvgDailyPredicted", Math.round(totalPredictedAvg / successCount * 10.0) / 10.0);
        }

        result.put("summary", summary);
        result.put("lotPredictions", lotPredictions);
        result.put("futureDates", futureDates);
        return result;
    }

    // ====================数据构建 ====================

    private List<String> buildFutureDateList(Date start, int days) {
        List<String> list = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Calendar cal = Calendar.getInstance();
        cal.setTime(start);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        for (int i = 0; i < days; i++) {
            list.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        return list;
    }

    private List<DailyDataPoint> buildDailySeries(List<ParkRecord> records) {
        Map<String, List<ParkRecord>> dayGroup = records.stream()
                .filter(r -> r.getEntryTime() != null && r.getEntryTime().length() >= 10)
                .collect(Collectors.groupingBy(r -> r.getEntryTime().substring(0, 10), TreeMap::new, Collectors.toList()));

        List<DailyDataPoint> series = new ArrayList<>();
        for (Map.Entry<String, List<ParkRecord>> entry : dayGroup.entrySet()) {
            String date = entry.getKey();
            List<ParkRecord> dayRecords = entry.getValue();

            DailyDataPoint dp = new DailyDataPoint();
            dp.date = date;
            dp.count = dayRecords.size();

            ParkRecord sample = dayRecords.get(0);
            dp.isHoliday = sample.getIsHoliday() != null && sample.getIsHoliday() == 1;
            dp.isWeekend = sample.getIsWeekend() != null && sample.getIsWeekend() == 1;
            dp.isWorkdayAdjust = sample.getIsWorkdayAdjust() != null && sample.getIsWorkdayAdjust() == 1;

            if (sample.getWeekday() != null) {
                dp.weekday = sample.getWeekday();
            } else {
                try {
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(new SimpleDateFormat("yyyy-MM-dd").parse(date));
                    int dow = cal.get(Calendar.DAY_OF_WEEK);
                    dp.weekday = (dow == Calendar.SUNDAY) ? 7 : dow - 1;
                } catch (Exception e) {
                    dp.weekday = 1;
                }
            }

            dp.weather = StrUtil.isNotEmpty(sample.getWeather()) ? normalizeWeather(sample.getWeather()) : "未知";
            dp.tempAvg = sample.getTempAvg() != null ? sample.getTempAvg() : 20.0;
            dp.windLevel = sample.getWindLevel() != null ? sample.getWindLevel().doubleValue() : 2.0;

            series.add(dp);
        }
        return series;
    }

    private double[] buildFeatureVector(DailyDataPoint dp, double countMax) {
        double[] vec = new double[FEATURE_DIM];
        int idx = 0;

        vec[idx + (dp.weekday - 1)] = 1.0;
        idx += 7;

        vec[idx++] = dp.isHoliday ? 1.0 : 0.0;
        vec[idx++] = dp.isWeekend ? 1.0 : 0.0;
        vec[idx++] = dp.isWorkdayAdjust ? 1.0 : 0.0;

        vec[idx++] = WEATHER_MAP.getOrDefault(dp.weather, 5.0) / 5.0;

        vec[idx++] = Math.max(0.0, Math.min(1.0, (dp.tempAvg + 20.0) / 60.0));

        vec[idx++] = Math.max(0.0, Math.min(1.0, dp.windLevel / 10.0));

        return vec;
    }

    /**
     * 构建滑动窗口训练数据 *输入: [1, FEATURE_DIM, WINDOW_SIZE] —每个样本是一段连续天数的特征序列 *标签: [1,1, WINDOW_SIZE] —对应每天的归一化车次（mask掉最后一个之外的所有）
     */
    private List<INDArray> buildSlidingWindows(List<double[]> featureList, double countMax) {
        int totalDays = featureList.size();
        int numWindows = totalDays - WINDOW_SIZE;

        INDArray input = Nd4j.create(new int[]{numWindows, FEATURE_DIM, WINDOW_SIZE}, 'f');
        INDArray labels = Nd4j.create(new int[]{numWindows, 1, WINDOW_SIZE}, 'f');

        for (int w = 0; w < numWindows; w++) {
            for (int t = 0; t < WINDOW_SIZE; t++) {
                double[] feat = featureList.get(w + t);
                for (int f = 0; f < FEATURE_DIM; f++) {
                    input.putScalar(new int[]{w, f, t}, feat[f]);
                }
                double normalizedCount = featureList.get(w + t + 1)[0] / countMax; //下一日的count labels.putScalar(new int[]{w,0, t}, t == WINDOW_SIZE -1 ? normalizedCount :0.0);
            }
        }

        return Arrays.asList(input, labels);
    }

    // ====================模型构建 ====================

    private MultiLayerNetwork buildModel() {
        MultiLayerNetwork model = new MultiLayerNetwork(new NeuralNetConfiguration.Builder()
                .seed(SEED)
                .weightInit(WeightInit.XAVIER)
                .updater(new Adam(LEARNING_RATE))
                .list()
                .layer(0, new LSTM.Builder()
                        .nIn(FEATURE_DIM)
                        .nOut(HIDDEN_SIZE)
                        .activation(Activation.TANH)
                        .build())
                .layer(1, new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .activation(Activation.IDENTITY)
                        .nIn(HIDDEN_SIZE)
                        .nOut(1)
                        .build())
                .build());
        model.init();
        return model;
    }

    // ====================迭代预测 ====================

    private List<Double> iterativePredict(MultiLayerNetwork model, List<double[]> historyFeatures,
                                          List<String> futureDates, double countMax) {
        List<Double> predictions = new ArrayList<>();

        int historyLen = historyFeatures.size();
        List<double[]> windowFeatures = new ArrayList<>();
        for (int i = historyLen - WINDOW_SIZE; i < historyLen; i++) {
            windowFeatures.add(historyFeatures.get(i).clone());
        }

        double lastCount = 0;
        for (int i = historyLen - 1; i >= 0; i--) {
            if (historyFeatures.get(i)[0] > 0) {
                lastCount = historyFeatures.get(i)[0];
                break;
            }
        }

        for (int step = 0; step < futureDates.size(); step++) {
            String futDate = futureDates.get(step);
            DailyDataPoint futDp = buildFutureDataPoint(futDate);
            double[] futFeat = buildFeatureVector(futDp, countMax);

            INDArray input = Nd4j.create(new int[]{1, FEATURE_DIM, WINDOW_SIZE}, 'f');
            for (int t = 0; t < WINDOW_SIZE; t++) {
                double[] feat = windowFeatures.get(t);
                for (int f = 0; f < FEATURE_DIM; f++) {
                    input.putScalar(new int[]{0, f, t}, feat[f]);
                }
            }

            INDArray output = model.rnnTimeStep(input);
            double predNormalized = output.getDouble(WINDOW_SIZE - 1);
            double predCount = Math.max(0, predNormalized * countMax);
            predictions.add(predCount);

            windowFeatures.remove(0);
            windowFeatures.add(futFeat.clone());
            lastCount = predCount;
        }

        return predictions;
    }

    private DailyDataPoint buildFutureDataPoint(String dateStr) {
        DailyDataPoint dp = new DailyDataPoint();
        dp.date = dateStr;

        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            int dow = cal.get(Calendar.DAY_OF_WEEK);
            dp.weekday = (dow == Calendar.SUNDAY) ? 7 : dow - 1;

            dp.isHoliday = HolidayUtil.isHoliday(date);
            dp.isWeekend = (dow == Calendar.SUNDAY || dow == Calendar.SATURDAY);
            dp.isWorkdayAdjust = HolidayUtil.isWorkdayAdjust(date);
            if (dp.isHoliday) {
                dp.isWeekend = false;
            }
        } catch (Exception e) {
            dp.weekday = 1;
        }

        dp.weather = "晴";
        dp.tempAvg = 22.0;
        dp.windLevel = 2.0;

        return dp;
    }

    // ====================辅助方法 ====================

    private String normalizeWeather(String weather) {
        if (StrUtil.isEmpty(weather)) {
            return "未知";
        }
        String w = weather.trim();
        if (w.contains("雪")) {
            return "雪";
        }
        if (w.contains("雨")) {
            return "雨";
        }
        if (w.contains("阴")) {
            return "阴";
        }
        if (w.contains("多云")) {
            return "多云";
        }
        if (w.contains("晴")) return "晴";
        return "未知";
    }

    // ====================内部数据类 ====================

    private static class DailyDataPoint {
        String date;
        double count;
        int weekday;
        boolean isHoliday;
        boolean isWeekend;
        boolean isWorkdayAdjust;
        String weather;
        double tempAvg;
        double windLevel;
    }
}