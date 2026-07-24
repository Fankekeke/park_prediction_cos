package cc.mrbird.febs.cos.service;

import cc.mrbird.febs.cos.entity.ParkInfo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

import java.util.LinkedHashMap;

/**
 * @author Fank gmail - fan1ke2ke@gmail.com
 */
public interface IParkInfoService extends IService<ParkInfo> {

    /**
     * 分页获取停车场信息
     *
     * @param page     分页对象
     * @param parkInfo 停车场信息
     * @return 结果
     */
    IPage<LinkedHashMap<String, Object>> queryPage(Page<ParkInfo> page, ParkInfo parkInfo);
}
