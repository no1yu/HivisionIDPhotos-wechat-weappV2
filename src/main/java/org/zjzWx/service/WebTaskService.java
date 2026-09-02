package org.zjzWx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zjzWx.entity.WebTask;

public interface WebTaskService extends IService<WebTask> {

    //清空定时任务日志并重置自增ID
    void truncateTable();
}
