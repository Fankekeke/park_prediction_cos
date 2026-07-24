package cc.mrbird.febs.cos.service.impl;

import cc.mrbird.febs.cos.entity.ParkInfo;
import cc.mrbird.febs.cos.dao.ParkInfoMapper;
import cc.mrbird.febs.cos.service.IParkInfoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

/**
 * @author Fank gmail - fan1ke2ke@gmail.com
 */
@Service
public class ParkInfoServiceImpl extends ServiceImpl<ParkInfoMapper, ParkInfo> implements IParkInfoService {

    /**
     * 分页获取停车场信息
     *
     * @param page     分页对象
     * @param parkInfo 停车场信息
     * @return 结果
     */
    @Override
    public IPage<LinkedHashMap<String, Object>> queryPage(Page<ParkInfo> page, ParkInfo parkInfo) {
        return baseMapper.queryPage(page, parkInfo);
    }
}
