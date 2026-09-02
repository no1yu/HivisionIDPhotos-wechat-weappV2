package org.zjzWx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zjzWx.entity.WebSet;

import java.util.Map;

public interface WebSetService extends IService<WebSet> {

    //读取个人中心公众号配置
    Map<String,Object> getMineSet();

}
