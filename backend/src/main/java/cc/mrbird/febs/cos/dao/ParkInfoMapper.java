package cc.mrbird.febs.cos.dao;

import cc.mrbird.febs.cos.entity.ParkInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.LinkedHashMap;

/**
 * @author Fank gmail - fan1ke2ke@gmail.com
 */
public interface ParkInfoMapper extends BaseMapper<ParkInfo> {

    /**
     * 分页获取停车场信息
     *
     * @param page     分页对象
     * @param parkInfo 停车场信息
     * @return 结果
     */
    IPage<LinkedHashMap<String, Object>> queryPage(Page<ParkInfo> page, @Param("parkInfo") ParkInfo parkInfo);
}
