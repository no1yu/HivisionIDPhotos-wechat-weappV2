package org.zjzWx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.zjzWx.entity.*;
import org.zjzWx.model.dto.ChangeClothesDto;
import org.zjzWx.model.dto.CreatePhotoDto;
import org.zjzWx.model.dto.UpdatePhotoDto;
import org.zjzWx.model.vo.PicVo;
import org.zjzWx.service.*;
import org.zjzWx.util.HivisionIDPhotosApiUtil;
import org.zjzWx.util.PicUtil;
import org.zjzWx.util.R;

import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;


@Service
public class ApiServiceImpl implements ApiService {

    @Autowired
    private CustomService customService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private PhotoService photoService;
    @Autowired
    private UserRecordService userRecordService;
    @Autowired
    private WebSetService webSetService;
    @Autowired
    private AppSetService appSetService;
    @Autowired
    private ClothesSetService clothesSetService;

    @Override
    public R createIdPhoto(CreatePhotoDto createPhotoDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(3,"生成证件照",createPhotoDto.getUserId(),createPhotoDto.getPhotoId(),startTime);

        //当照片不存在/不属于当前用户/没有上传原图/已经进入其它流程时，停止制作
        Photo photo = photoService.getById(createPhotoDto.getPhotoId());
        if(photo==null || !createPhotoDto.getUserId().equals(photo.getUserId()) || photo.getOriginalPath()==null || photo.getStandardPath()!=null || photo.getNImg()!=null){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        PicVo picVo = new PicVo();

        //如果是定制规格
        if(createPhotoDto.getType()==2){
            QueryWrapper<Custom> customQueryWrapper = new QueryWrapper<>();
            customQueryWrapper.eq("id",createPhotoDto.getItemId());
            customQueryWrapper.eq("user_id",createPhotoDto.getUserId());
            Custom custom = customService.getOne(customQueryWrapper);
            if(custom==null){
                userRecordService.insertUserRecord(userRecord,2,"非法请求");
                return R.no("非法请求");
            }


            picVo.setCategory(4);
            photo.setName("用户自定义尺寸");
            photo.setWidth(custom.getWidthPx());
            photo.setHeight(custom.getHeightPx());
            photo.setDpi(custom.getDpi());

        }else {
            //如果是系统规格
            Item item = itemService.getById(createPhotoDto.getItemId());

            photo.setName(item.getName());
            picVo.setCategory(item.getCategory());
            photo.setWidth(item.getWidthPx());
            photo.setHeight(item.getHeightPx());
            photo.setDpi(item.getDpi());
        }

        //先保持未解锁，点击下载时再按后台下载模式处理
        photo.setItemId(createPhotoDto.getItemId());
        photo.setAppId(3);
        photo.setType(createPhotoDto.getType());
        photo.setIsBeautyOn(createPhotoDto.getIsBeautyOn());
        photo.setSize(photo.getWidth() + "x" + photo.getHeight());
        photo.setClothesCategory(0);
        photo.setClothesId(0);


        try {

            QueryWrapper<WebSet> qw = new QueryWrapper<>();
            qw.eq("id",1);
            qw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key","human_matting_model","face_detect_model","brightness_strength","contrast_strength","sharpen_strength","saturation_strength");
            WebSet webSet = webSetService.getOne(qw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
            body.add("height",photo.getHeight());
            body.add("width",photo.getWidth());
            body.add("dpi",photo.getDpi());
            body.add("human_matting_model",webSet.getHumanMattingModel());
            body.add("face_detect_model",webSet.getFaceDetectModel());
            body.add("hd",false);
            body.add("face_align",true);

            //当用户开启美颜时
            if(photo.getIsBeautyOn()==1){
                body.add("brightness_strength",webSet.getBrightnessStrength());
                body.add("contrast_strength",webSet.getContrastStrength());
                body.add("sharpen_strength",webSet.getSharpenStrength());
                body.add("saturation_strength",webSet.getSaturationStrength());
            }


            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),webSet.getDirectory()).toFile()));

            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(webSet.getPicApiUrl(),webSet.getPicApiType(),webSet.getPicApiKey(),"idphoto",body);
            photo.setStandardPath(PicUtil.saveTempImage(photo,imageBytes,webSet.getDirectory(),"png"));
            photoService.updateById(photo);

            //封装前端数据
            picVo.setPhotoId(photo.getId());
            picVo.setPreviewUrl(PicUtil.getPublicUrl(photo.getStandardPath(),webSet.getPicDomain()));
            picVo.setDpi(photo.getDpi());
            picVo.setName(photo.getName());
            picVo.setItemId(photo.getItemId());

            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(picVo);


        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }


    }


    @Override
    public R createIdHdPhoto(Integer photoId,Integer userId) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(3,"生成高清证件照",userId,photoId,startTime);

        //当照片不存在/不属于当前用户/普通证件照还没有生成时，停止制作
        Photo photo = photoService.getById(photoId);
        if(photo==null || !userId.equals(photo.getUserId()) || photo.getStandardPath()==null){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //当高清照片已经生成过时，直接返回当前编辑数据
        if(photo.getHdPath()!=null){
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());
        }

        //当高清照片功能已经关闭时，停止制作
        AppSet appSet = appSetService.getById(3);
        if(appSet.getStatus()==0){
            userRecordService.insertUserRecord(userRecord,2,"当前功能维护中，请稍后再试");
            return R.no("当前功能维护中，请稍后再试");
        }

        try {

            QueryWrapper<WebSet> qw = new QueryWrapper<>();
            qw.eq("id",1);
            qw.select("directory","pic_api_type","pic_api_url","pic_api_key","human_matting_model","face_detect_model","brightness_strength","contrast_strength","sharpen_strength","saturation_strength");
            WebSet webSet = webSetService.getOne(qw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
            body.add("height",photo.getHeight());
            body.add("width",photo.getWidth());
            body.add("dpi",photo.getDpi());
            body.add("human_matting_model",webSet.getHumanMattingModel());
            body.add("face_detect_model",webSet.getFaceDetectModel());
            body.add("hd",true);
            body.add("face_align",true);

            //当用户开启美颜时
            if(photo.getIsBeautyOn()==1){
                body.add("brightness_strength",webSet.getBrightnessStrength());
                body.add("contrast_strength",webSet.getContrastStrength());
                body.add("sharpen_strength",webSet.getSharpenStrength());
                body.add("saturation_strength",webSet.getSaturationStrength());
            }

            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),webSet.getDirectory()).toFile()));

            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(webSet.getPicApiUrl(),webSet.getPicApiType(),webSet.getPicApiKey(),"idphoto",body);
            photo.setHdPath(PicUtil.saveTempImage(photo,imageBytes,webSet.getDirectory(),"png"));
            photoService.updateById(photo);

            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());
        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }


    @Override
    public R updateIdPhoto(UpdatePhotoDto updatePhotoDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(3,"换背景",updatePhotoDto.getUserId(),updatePhotoDto.getPhotoId(),startTime);

        PicVo picVo = new PicVo();

        //当照片不存在/不属于当前用户/透明证件照尚未生成时，停止制作
        Photo photo = photoService.getById(updatePhotoDto.getPhotoId());
        if(photo==null || !updatePhotoDto.getUserId().equals(photo.getUserId()) || photo.getStandardPath()==null){
            picVo.setMsg("非法请求");
            userRecordService.insertUserRecord(userRecord,2,picVo.getMsg());
            return R.no(picVo.getMsg());
        }

        try {

            photo.setBackgroundColor(updatePhotoDto.getColors());
            photo.setBackgroundRender(updatePhotoDto.getRender());
            picVo = renderEditedPhoto(photo,updatePhotoDto.getDpi(),updatePhotoDto.getKb(),updatePhotoDto.getHd());
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(picVo);
        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }


    @Override
    public R changeClothes(ChangeClothesDto changeClothesDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(3,"换装",changeClothesDto.getUserId(),changeClothesDto.getPhotoId(),startTime);

        PicVo picVo = new PicVo();

        //换装功能关闭时停止制作
        if(appSetService.getClothesSwitch()==0){
            picVo.setMsg("换装功能已关闭");
            userRecordService.insertUserRecord(userRecord,2,picVo.getMsg());
            return R.no(picVo.getMsg());
        }

        //换装参数不完整或超出当前素材范围时
        if(changeClothesDto.getPhotoId()==null || changeClothesDto.getClothesCategory()==null || changeClothesDto.getClothesId()==null || changeClothesDto.getDpi()==null || changeClothesDto.getKb()==null || changeClothesDto.getHd()==null){
            picVo.setMsg("换装参数无效");
            userRecordService.insertUserRecord(userRecord,2,picVo.getMsg());
            return R.no(picVo.getMsg());
        }

        //检查换装参数是否正常
        boolean cancelClothes = changeClothesDto.getClothesCategory()==0 && changeClothesDto.getClothesId()==0;
        boolean selectClothes = clothesSetService.isClothesAvailable(changeClothesDto.getClothesCategory(),changeClothesDto.getClothesId());
        if((!cancelClothes && !selectClothes) || changeClothesDto.getDpi()<72 || changeClothesDto.getKb()<0 || (changeClothesDto.getHd()!=0 && changeClothesDto.getHd()!=1)){
            picVo.setMsg("换装参数无效");
            userRecordService.insertUserRecord(userRecord,2,picVo.getMsg());
            return R.no(picVo.getMsg());
        }

        //当照片不存在/不属于当前用户/透明证件照尚未生成时，停止制作
        Photo photo = photoService.getById(changeClothesDto.getPhotoId());
        if(photo==null || !changeClothesDto.getUserId().equals(photo.getUserId()) || photo.getStandardPath()==null){
            picVo.setMsg("非法请求");
            userRecordService.insertUserRecord(userRecord,2,picVo.getMsg());
            return R.no(picVo.getMsg());
        }

        try {

            photo.setClothesCategory(changeClothesDto.getClothesCategory());
            photo.setClothesId(changeClothesDto.getClothesId());

            //当用户没有选择背景时就默认白色
            if(photo.getBackgroundColor()==null){
                photo.setBackgroundColor("#ffffff");
                photo.setBackgroundRender(0);
            }

            picVo = renderEditedPhoto(photo,changeClothesDto.getDpi(),changeClothesDto.getKb(),changeClothesDto.getHd());
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(picVo);
        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }


    private PicVo renderEditedPhoto(Photo photo,Integer dpi,Integer kb,Integer hd) throws Exception {


        QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
        storageQw.eq("id",1);
        storageQw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key","clothes_face_detect_model","clothes_parsing_model");
        WebSet storageSet = webSetService.getOne(storageQw);

        String sourcePath = photo.getStandardPath();

        //如果请求是高清编辑改用高清透明照作为合成源
        if(hd==1 && photo.getHdPath()!=null){
            sourcePath = photo.getHdPath();
        }

        MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
        body.add("input_image",new FileSystemResource(PicUtil.getFile(sourcePath,storageSet.getDirectory()).toFile()));
        body.add("color",photo.getBackgroundColor());
        body.add("render",photo.getBackgroundRender());
        body.add("dpi",dpi);

        //当用户填写了kb时
        if(kb>0){
            body.add("kb",kb);
        }

        String endpoint = "add_background";

        //照片已经选择服装时，调用换装接口同时重新合成当前背景
        if(photo.getClothesCategory()!=0){
            endpoint = "change_clothes";
            body.add("clothes_category",photo.getClothesCategory());
            body.add("clothes_id",photo.getClothesId());
            body.add("clothes_face_detect_model",storageSet.getClothesFaceDetectModel());
            body.add("clothes_parsing_model",storageSet.getClothesParsingModel());
        }


        //发起请求
        String resultPath = PicUtil.saveTempImage(photo,HivisionIDPhotosApiUtil.requestImage(storageSet.getPicApiUrl(),storageSet.getPicApiType(),storageSet.getPicApiKey(),endpoint,body),storageSet.getDirectory(),kb>0 ? "jpg" : "png");

        //新旧编辑结果不是同一个文件时，删除已经被替换的旧临时文件
        if(photo.getResultPath()!=null && !photo.getResultPath().equals(resultPath)){
            PicUtil.deleteFile(photo.getResultPath(),storageSet.getDirectory());
        }
        photo.setResultPath(resultPath);
        photoService.updateById(photo);

        PicVo picVo = new PicVo();
        picVo.setPhotoId(photo.getId());
        picVo.setPreviewUrl(PicUtil.getPublicUrl(resultPath,storageSet.getPicDomain()));
        return picVo;
    }


    @Override
    public R updateUserPhonto(Integer userId, Integer photoId) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(3,"下载证件照",userId,photoId,startTime);

        PicVo picVo = new PicVo();

        //当照片不存在/不属于当前用户/普通证件照还没有生成时，停止保存
        Photo photo = photoService.getById(photoId);
        if(photo==null || !userId.equals(photo.getUserId()) || photo.getStandardPath()==null){
            picVo.setMsg("非法请求");
            userRecordService.insertUserRecord(userRecord,2,picVo.getMsg());
            return R.no(picVo.getMsg());
        }

        //当用户还没有选择背景色时，停止保存
        if(photo.getBackgroundColor()==null){
            picVo.setMsg("您还没有选择背景色哦~");
            userRecordService.insertUserRecord(userRecord,2,picVo.getMsg());
            return R.no(picVo.getMsg());
        }

        try {

            QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
            storageQw.eq("id",1);
            storageQw.select("pic_domain","directory");
            WebSet storageSet = webSetService.getOne(storageQw);

            //高清下载权已经永久解锁时，普通预览只返回当前低清临时图，不覆盖作品里的高清成片
            if(photo.getDownloadStatus()==3){
                picVo.setPhotoId(photoId);
                picVo.setPicUrl(PicUtil.getPublicUrl(photo.getResultPath(),storageSet.getPicDomain()));
                userRecordService.insertUserRecord(userRecord,1,null);
                return R.ok(picVo);
            }

            String sourcePath = photo.getStandardPath();

            //当用户修改过背景或服装时，最终保存当前完整编辑结果
            if(photo.getResultPath()!=null){
                sourcePath = photo.getResultPath();
            }
            String imageExtension = sourcePath.substring(sourcePath.lastIndexOf(".")+1);
            String oldImagePath = photo.getNImg();
            photo.setNImg(PicUtil.savePermanentImage(
                    new SimpleDateFormat("yyyyMMdd").format(new Date()),
                    Files.readAllBytes(PicUtil.getFile(sourcePath,storageSet.getDirectory())),
                    storageSet.getDirectory(),
                    storageSet.getPicDomain(),
                    imageExtension
            ));
            photo.setDownloadStatus(2);
            photoService.updateById(photo);

            //当数据库中原来有另一张成片时，删除已经被替换的旧文件
            if(oldImagePath!=null && !oldImagePath.equals(photo.getNImg())){
                PicUtil.deleteImage(oldImagePath,storageSet.getDirectory());
            }
            picVo.setPhotoId(photoId);
            picVo.setPicUrl(photo.getNImg());
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(picVo);
        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }

}
