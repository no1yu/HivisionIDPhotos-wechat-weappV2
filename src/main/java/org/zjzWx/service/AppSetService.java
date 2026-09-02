package org.zjzWx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zjzWx.entity.AppSet;

public interface AppSetService extends IService<AppSet> {

    //获取是否开启美颜功能
    Integer getBeautySwitch();

    //获取是否开启换装功能
    Integer getClothesSwitch();

}
