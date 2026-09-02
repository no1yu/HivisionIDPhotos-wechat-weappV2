package org.zjzWx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.zjzWx.entity.UserRecord;

public interface UserRecordService extends IService<UserRecord> {

    //创建用户操作记录并保存接口开始时间
    UserRecord createUserRecord(Integer appId,String name,Integer userId,Integer photoId,long startTime);

    //新增用户操作结果
    void insertUserRecord(UserRecord userRecord,int status,String errorMessage);
}
