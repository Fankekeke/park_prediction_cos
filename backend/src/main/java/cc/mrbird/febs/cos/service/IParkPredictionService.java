
package cc.mrbird.febs.cos.service;

import java.util.LinkedHashMap;

/**
 * 停车场LSTM预测服务接口 *
 *
 * @author Fank gmail - fan1ke2ke@gmail.com
 */
public interface IParkPredictionService {

    /**
     * 对所有停车场进行未来30天车流量LSTM预测 *
     *
     * @return 各车场历史数据 +预测结果
     */
    LinkedHashMap<String, Object> predictNextMonth();
}