package org.zjzWx.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zjzWx.entity.Photo;
import org.zjzWx.entity.WebSet;
import org.zjzWx.entity.WebTask;
import org.zjzWx.service.PhotoService;
import org.zjzWx.service.WebTaskService;
import org.zjzWx.service.WebSetService;
import org.zjzWx.util.PicUtil;

import java.util.*;

@Component
public class PhotoTask {

    @Autowired
    private PhotoService photoService;
    @Autowired
    private WebSetService webSetService;
    @Autowired
    private WebTaskService webTaskService;



    //每天03:00执行    删除上传时间超过7天的照片记录和图片
    @Scheduled(cron = "0 0 3 * * ?", zone = "Asia/Shanghai")
    public void deleteExpirePhotoTask() {
        long startTime = System.currentTimeMillis();
        int deleteCount = 0;

        WebTask webTask = new WebTask();
        webTask.setType(1);
        webTask.setTaskName("过期照片清理");
        webTask.setStartTime(new Date(startTime));

        try {
            QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
            storageQw.eq("id",1);
            storageQw.select("directory");
            String directory = webSetService.getOne(storageQw).getDirectory();

            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
            calendar.add(Calendar.DAY_OF_MONTH, -7);
            Date expireTime = calendar.getTime();
            int batchSize = 500;

            while (true) {
                QueryWrapper<Photo> qw = new QueryWrapper<>();
                qw.select("id","n_img");
                qw.lt("create_time",expireTime);
                qw.orderByAsc("id");
                qw.last("limit " + batchSize);
                List<Photo> list = photoService.list(qw);
                if(list==null || list.size()==0){
                    break;
                }

                for (Photo photo : list) {
                    //删除前再次确认上传时间已经超过7天
                    QueryWrapper<Photo> removeQw = new QueryWrapper<>();
                    removeQw.eq("id",photo.getId());
                    removeQw.lt("create_time",expireTime);
                    if(photoService.remove(removeQw)){
                        deleteCount++;
                        PicUtil.deleteImage(photo.getNImg(),directory);

                        //同时删除这张照片的临时目录
                        PicUtil.deleteTempDirectory(photo.getId(),directory);
                    }
                }

                if(list.size()<batchSize){
                    break;
                }

            }
            webTask.setStatus(1);

        } catch (Exception e) {
            e.printStackTrace();
            webTask.setStatus(2);
            webTask.setErrorLog(e.toString());
        }

        webTask.setDeleteCount(deleteCount);
        webTask.setDurationMs((int)(System.currentTimeMillis()-startTime));
        webTask.setEndTime(new Date());
        webTaskService.save(webTask);
    }


    //每30分钟执行    清理已经超过有效期的临时编辑数据和物理目录
    @Scheduled(cron = "0 */30 * * * ?", zone = "Asia/Shanghai")
    public void deleteExpireTempPhotoTask() {
        long startTime = System.currentTimeMillis();
        int deleteCount = 0;

        WebTask webTask = new WebTask();
        webTask.setType(2);
        webTask.setTaskName("临时编辑数据清理");
        webTask.setStartTime(new Date(startTime));

        try {
            QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
            storageQw.eq("id",1);
            storageQw.select("directory");
            String directory = webSetService.getOne(storageQw).getDirectory();

            int batchSize = 500;
            Date cleanupTime = new Date();

            while (true) {
                QueryWrapper<Photo> qw = new QueryWrapper<>();
                qw.select("id","n_img");
                qw.isNotNull("expire_time");
                qw.le("expire_time",cleanupTime);
                qw.orderByAsc("id");
                qw.last("limit " + batchSize);
                List<Photo> list = photoService.list(qw);

                //当没有过期照片时，结束本次清理
                if(list==null || list.size()==0){
                    break;
                }

                for (Photo photo : list) {
                    //删除或清空时再次确认照片仍然处于过期状态
                    //当用户没有生成正式图片时，整条照片记录已经没有保留价值
                    if(photo.getNImg()==null){
                        QueryWrapper<Photo> removeQw = new QueryWrapper<>();
                        removeQw.eq("id",photo.getId());
                        removeQw.isNull("n_img");
                        removeQw.le("expire_time",cleanupTime);
                        if(photoService.remove(removeQw)){
                            deleteCount++;
                            PicUtil.deleteTempDirectory(photo.getId(),directory);
                        }
                    }else {
                        UpdateWrapper<Photo> updateQw = new UpdateWrapper<>();
                        updateQw.eq("id",photo.getId());
                        updateQw.isNotNull("n_img");
                        updateQw.le("expire_time",cleanupTime);
                        updateQw.set("width",null);
                        updateQw.set("height",null);
                        updateQw.set("dpi",null);
                        updateQw.set("is_beauty_on",null);
                        updateQw.set("original_path",null);
                        updateQw.set("standard_path",null);
                        updateQw.set("hd_path",null);
                        updateQw.set("result_path",null);
                        updateQw.set("expire_time",null);
                        if(photoService.update(updateQw)){
                            deleteCount++;
                            PicUtil.deleteTempDirectory(photo.getId(),directory);
                        }
                    }
                }

                //当本批数据不足500条时，说明没有下一批需要处理
                if(list.size()<batchSize){
                    break;
                }

            }
            webTask.setStatus(1);

        } catch (Exception e) {
            e.printStackTrace();
            webTask.setStatus(2);
            webTask.setErrorLog(e.toString());
        }

        webTask.setDeleteCount(deleteCount);
        webTask.setDurationMs((int)(System.currentTimeMillis()-startTime));
        webTask.setEndTime(new Date());
        webTaskService.save(webTask);
    }




}
