package org.zjzWx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.zjzWx.dao.WebTaskDao;
import org.zjzWx.entity.WebTask;
import org.zjzWx.service.WebTaskService;

@Service
public class WebTaskServiceImpl extends ServiceImpl<WebTaskDao, WebTask> implements WebTaskService {

    @Override
    public void truncateTable() {
        baseMapper.truncateTable();
    }
}
