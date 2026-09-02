package org.zjzWx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.zjzWx.dao.UserRecordDao;
import org.zjzWx.entity.UserRecord;
import org.zjzWx.service.UserRecordService;

import java.util.Date;

@Service
public class UserRecordServiceImpl extends ServiceImpl<UserRecordDao, UserRecord> implements UserRecordService {

    @Override
    public UserRecord createUserRecord(Integer appId, String name, Integer userId, Integer photoId, long startTime) {

        UserRecord userRecord = new UserRecord();
        userRecord.setAppId(appId);
        userRecord.setName(name);
        userRecord.setUserId(userId);
        userRecord.setPhotoId(photoId);
        userRecord.setCreateTime(new Date(startTime));
        return userRecord;
    }

    @Override
    public void insertUserRecord(UserRecord userRecord, int status, String errorMessage) {

        userRecord.setStatus(status);
        userRecord.setDurationMs(System.currentTimeMillis()-userRecord.getCreateTime().getTime());
        userRecord.setErrorMessage(errorMessage);
        baseMapper.insert(userRecord);
    }
}
