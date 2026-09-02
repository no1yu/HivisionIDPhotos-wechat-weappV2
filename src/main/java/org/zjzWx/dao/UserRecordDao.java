package org.zjzWx.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.zjzWx.entity.UserRecord;

@Mapper
public interface UserRecordDao extends BaseMapper<UserRecord> {
}
