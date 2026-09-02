package org.zjzWx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zjzWx.dao.PhotoDao;
import org.zjzWx.entity.Photo;
import org.zjzWx.entity.WebSet;
import org.zjzWx.service.PhotoService;
import org.zjzWx.service.WebSetService;
import org.zjzWx.util.PicUtil;


@Service
public class PhotoServiceImpl extends ServiceImpl<PhotoDao,Photo> implements PhotoService {

    @Autowired
    private WebSetService webSetService;

    @Override
    public Page<Photo> photoList(int pageNum, int pageSize, Integer userId) {

        //作品列表只返回已经解锁的正式成片
        Page<Photo> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Photo> qw = new QueryWrapper<>();
        qw.eq("user_id",userId);
        qw.isNotNull("n_img");
        qw.gt("download_status",1);
        qw.select("id","name","n_img","size","create_time");
        qw.orderByDesc("create_time");
        return baseMapper.selectPage(page, qw);
    }

    @Override
    public void deletePhotoId(Integer id, Integer userId) {

        QueryWrapper<Photo> qw = new QueryWrapper<>();
        qw.eq("id",id);
        qw.eq("user_id",userId);
        Photo photo = baseMapper.selectOne(qw);

        //当照片属于当前用户时，同时删除正式图片、临时目录和数据库记录
        if(photo!=null){
            QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
            storageQw.eq("id",1);
            storageQw.select("directory");
            String directory = webSetService.getOne(storageQw).getDirectory();

            PicUtil.deleteImage(photo.getNImg(),directory);
            PicUtil.deleteTempDirectory(photo.getId(),directory);
            baseMapper.deleteById(photo);
        }
    }
}
