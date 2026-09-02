package org.zjzWx.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.zjzWx.entity.Photo;

public interface PhotoService extends IService<Photo> {

    //我的作品
    Page<Photo> photoList(int pageNum, int pageSize, Integer userId);
    //删除我的作品
    void deletePhotoId(Integer id,Integer userId);

}
