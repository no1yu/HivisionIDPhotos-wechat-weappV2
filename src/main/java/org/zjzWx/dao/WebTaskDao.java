package org.zjzWx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;
import org.zjzWx.entity.WebTask;

@Mapper
public interface WebTaskDao extends BaseMapper<WebTask> {

    //定时任务日志没有外键，直接清空数据并重置自增ID
    @Update("TRUNCATE TABLE web_task")
    void truncateTable();
}
