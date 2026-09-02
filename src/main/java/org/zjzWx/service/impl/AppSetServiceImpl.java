package org.zjzWx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.zjzWx.dao.AppSetDao;
import org.zjzWx.entity.AppSet;
import org.zjzWx.service.AppSetService;


@Service
public class AppSetServiceImpl extends ServiceImpl<AppSetDao, AppSet> implements AppSetService {

    @Override
    public Integer getBeautySwitch() {

        return baseMapper.selectById(2).getStatus();
    }

    @Override
    public Integer getClothesSwitch() {

        return baseMapper.selectById(15).getStatus();
    }
}
