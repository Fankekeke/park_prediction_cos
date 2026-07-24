
package cc.mrbird.febs.common.utils;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.*;

/**
 * 节假日工具类 -基于 timor.tech API
 */
public class HolidayUtil {

    private static final String HOLIDAY_API = "https://timor.tech/api/holiday/year/";
    private static final Map<Integer, Map<String, JSONObject>> CACHE = new HashMap<>();

    /**
     * 加载指定年份的节假日数据（带缓存）
     */
    private static Map<String, JSONObject> loadHolidayData(int year) {
        if (CACHE.containsKey(year)) {
            return CACHE.get(year);
        }
        try {
            String result = HttpUtil.get(HOLIDAY_API + year);
            JSONObject json = JSONUtil.parseObj(result);
            if (json.getInt("code") == 0) {
                JSONObject holiday = json.getJSONObject("holiday");
                Map<String, JSONObject> map = new HashMap<>();
                for (String key : holiday.keySet()) {
                    map.put(key, holiday.getJSONObject(key));
                }
                CACHE.put(year, map);
                return map;
            }
        } catch (Exception e) {
            // API请求失败时返回空，避免影响业务 }
            return Collections.emptyMap();
        }
        return null;
    }

    /**
     * Date转 MM-dd格式
     */
    private static String toMmDd(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return String.format("%02d-%02d", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * 判断是否为节假日（holiday=true）
     */
    public static boolean isHoliday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        Map<String, JSONObject> data = loadHolidayData(cal.get(Calendar.YEAR));
        JSONObject info = data.get(toMmDd(date));
        return info != null && info.getBool("holiday", false);
    }

    /**
     * 判断是否为调休补班日（holiday=false）
     */
    public static boolean isWorkdayAdjust(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        Map<String, JSONObject> data = loadHolidayData(cal.get(Calendar.YEAR));
        JSONObject info = data.get(toMmDd(date));
        return info != null && !info.getBool("holiday", true);
    }

}