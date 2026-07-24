
package cc.mrbird.febs.common.utils;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 天气工具类 -基于 itboy.net API
 */
public class WeatherUtil {

    private static final String WEATHER_API = "http://t.weather.itboy.net/api/weather/city/";

    /**
     * 默认城市编码：北京
     */
    private static final String DEFAULT_CITY_CODE = "101010100";

    /**
     * 缓存：cityCode -> {data, expireTime}
     */
    private static final java.util.Map<String, CacheEntry> CACHE = new java.util.HashMap<>();
    private static final long CACHE_DURATION = 30 * 60 * 1000L; //30分钟
    private static final Pattern NUM_PATTERN = Pattern.compile("(\\d+)");

    private static class CacheEntry {
        JSONObject data;
        long expireTime;

        CacheEntry(JSONObject data, long expireTime) {
            this.data = data;
            this.expireTime = expireTime;
        }
    }

    /**
     * 获取天气数据
     */
    private static JSONObject fetchWeather(String cityCode) {
        CacheEntry entry = CACHE.get(cityCode);
        if (entry != null && System.currentTimeMillis() < entry.expireTime) {
            return entry.data;
        }
        try {
            String result = HttpUtil.get(WEATHER_API + cityCode);
            JSONObject json = JSONUtil.parseObj(result);
            if (json.getInt("status") == 200) {
                JSONObject data = json.getJSONObject("data");
                CACHE.put(cityCode, new CacheEntry(data, System.currentTimeMillis() + CACHE_DURATION));
                return data;
            }
        } catch (Exception e) {
            //请求失败，返回 null }
            return null;
        }
        return null;
    }

    /**
     * 获取今日天气类型
     */
    public static String getWeatherType(String cityCode) {
        JSONObject today = getTodayForecast(cityCode);
        return today != null ? today.getStr("type") : null;
    }

    /**
     * 获取今日平均温度
     */
    public static Double getTempAvg(String cityCode) {
        JSONObject today = getTodayForecast(cityCode);
        if (today == null) {
            return null;
        }
        try {
            int high = extractNumber(today.getStr("high"));
            int low = extractNumber(today.getStr("low"));
            return (high + low) / 2.0;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取今日风力等级
     */
    public static Integer getWindLevel(String cityCode) {
        JSONObject today = getTodayForecast(cityCode);
        if (today == null) {
            return null;
        }
        try {
            return extractNumber(today.getStr("fl"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取今日天气预报
     */
    private static JSONObject getTodayForecast(String cityCode) {
        JSONObject data = fetchWeather(cityCode);
        if (data == null) {
            return null;
        }
        JSONArray forecast = data.getJSONArray("forecast");
        if (forecast == null || forecast.isEmpty()) {
            return null;
        }
        return forecast.getJSONObject(0);
    }

    /**
     * 从字符串中提取数字
     */
    private static int extractNumber(String str) {
        if (str == null) {
            return 0;
        }
        Matcher matcher = NUM_PATTERN.matcher(str);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    // ==========默认城市编码重载方法 ==========

    public static String getWeatherType() {
        return getWeatherType(DEFAULT_CITY_CODE);
    }

    public static Double getTempAvg() {
        return getTempAvg(DEFAULT_CITY_CODE);
    }

    public static Integer getWindLevel() {
        return getWindLevel(DEFAULT_CITY_CODE);
    }
}