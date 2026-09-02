package org.zjzWx.file;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zjzWx.entity.AppSet;
import org.zjzWx.entity.Photo;
import org.zjzWx.entity.UserRecord;
import org.zjzWx.entity.WebSet;
import org.zjzWx.service.AppSetService;
import org.zjzWx.service.PhotoService;
import org.zjzWx.service.UploadService;
import org.zjzWx.service.UserRecordService;
import org.zjzWx.service.WebSetService;
import org.zjzWx.util.PicUtil;
import org.zjzWx.util.R;

import java.util.Date;

@RestController
public class ImageUpload {

    @Autowired
    private UploadService uploadService;
    @Autowired
    private UserRecordService userRecordService;
    @Autowired
    private AppSetService appSetService;
    @Autowired
    private PhotoService photoService;
    @Autowired
    private WebSetService webSetService;




    //证件照功能和探索中心功能的图片上传都会经过这个接口检查，通过后创建照片并返回photoId
    @PostMapping("/upload")
    public R uploadImage(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return R.no("图片不能为空");
        }


        //当图片上传功能已经关闭时，拒绝接收图片
        AppSet uploadAppSet = appSetService.getById(1);
        if(uploadAppSet.getStatus()==0){
            return R.no("系统维护中，请稍后再试");
        }

        //记录接口耗时
        long startTime = System.currentTimeMillis();


        //当文件名不是jpg、jpeg或png时
        if (!StringUtils.hasText(file.getOriginalFilename()) || (!file.getOriginalFilename().toLowerCase().endsWith(".png")
                && !file.getOriginalFilename().toLowerCase().endsWith(".jpg")) && !file.getOriginalFilename().toLowerCase().endsWith(".jpeg")) {
            return R.no("图片不合法，仅支持jpg/png/jpeg");
        }
        //超15M
        if (file.getSize() > 15 * 1024 * 1024) {
            return R.no("图片大小不能超过15M");
        }



        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (Exception e) {
            return R.no("图片读取失败，请重新上传");
        }


        String extension = PicUtil.getImageExtension(imageBytes);
        if(extension==null){
            return R.no("图片不合法，仅支持jpg/png/jpeg");
        }

        QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
        storageQw.eq("id",1);
        storageQw.select("directory","pic_api_type","pic_api_url","pic_api_key");
        WebSet webSet = webSetService.getOne(storageQw);

        //检查鉴黄
        AppSet nsfwAppSet = appSetService.getById(14);
        if(nsfwAppSet.getStatus()==1){
            String msg = uploadService.checkNsfw(imageBytes,file.getOriginalFilename(),nsfwAppSet.getSettingValue(),webSet.getPicApiUrl(),webSet.getPicApiType(),webSet.getPicApiKey());
            //图片违规
            if(msg!=null){
                return R.no(msg);
            }
        }

        UserRecord userRecord = userRecordService.createUserRecord(1,"上传图片",StpUtil.getLoginIdAsInt(),null,startTime);
        Photo photo = new Photo();
        String directory = webSet.getDirectory();

        try {
            photo.setUserId(StpUtil.getLoginIdAsInt());
            //临时图片过期时间2个小时
            photo.setExpireTime(new Date(System.currentTimeMillis()+7200*1000L));
            photo.setCreateTime(new Date());
            photoService.save(photo);
            photo.setOriginalPath(PicUtil.saveUpload(imageBytes,photo.getId(),directory,extension));
            photoService.updateById(photo);
            userRecord.setPhotoId(photo.getId());
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());
        } catch (Exception e) {
            userRecord.setPhotoId(photo.getId());
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());

            //异常时删除保存记录和临时目录
            if(photo.getId()!=null){
                PicUtil.deleteTempDirectory(photo.getId(),directory);
                photoService.removeById(photo.getId());
            }
            return R.no("图片上传失败，请重试");
        }
    }


}
