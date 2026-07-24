package cc.mrbird.febs.cos.dao;

import cc.mrbird.febs.cos.entity.ParkRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.LinkedHashMap;

/**
 * @author Fank gmail - fan1ke2ke@gmail.com
 */
public interface ParkRecordMapper extends BaseMapper<ParkRecord> {

    /**
     * 分页获取停车记录信息
     *
     * @param page       分页对象
     * @param parkRecord 停车记录信息
     * @return 结果
     */
    IPage<LinkedHashMap<String, Object>> queryPage(Page<ParkRecord> page, @Param("parkRecord") ParkRecord parkRecord);
}
