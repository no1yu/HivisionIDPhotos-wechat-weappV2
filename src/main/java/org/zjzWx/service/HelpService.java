package org.zjzWx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zjzWx.entity.Help;

import java.util.List;

public interface HelpService extends IService<Help> {

    //读取小程序常见问题
    List<Help> getHelpList();
}
