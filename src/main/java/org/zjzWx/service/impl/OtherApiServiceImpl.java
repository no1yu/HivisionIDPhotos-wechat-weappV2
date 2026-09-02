package org.zjzWx.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.zjzWx.entity.AppSet;
import org.zjzWx.entity.Photo;
import org.zjzWx.entity.UserRecord;
import org.zjzWx.entity.WebSet;
import org.zjzWx.model.dto.ExploreDto;
import org.zjzWx.model.vo.ExploreApplicationVo;
import org.zjzWx.service.AppSetService;
import org.zjzWx.service.OtherApiService;
import org.zjzWx.service.PhotoService;
import org.zjzWx.service.UserRecordService;
import org.zjzWx.service.WebSetService;
import org.zjzWx.util.HivisionIDPhotosApiUtil;
import org.zjzWx.util.PicUtil;
import org.zjzWx.util.R;

import java.util.ArrayList;
import java.util.List;


@Service
public class OtherApiServiceImpl implements OtherApiService {

    @Autowired
    private PhotoService photoService;
    @Autowired
    private UserRecordService userRecordService;
    @Autowired
    private AppSetService appSetService;
    @Autowired
    private WebSetService webSetService;


    @Override
    public List<ExploreApplicationVo> exploreIndex() {

        List<ExploreApplicationVo> applications = new ArrayList<>();
        List<AppSet> list = appSetService.list(new QueryWrapper<AppSet>().notIn("id",1,2,14,15).orderByAsc("sort"));
        for (AppSet appSet : list) {

            //已经关闭的应用不在探索中心显示
            if(appSet.getStatus()==0){
                continue;
            }
            ExploreApplicationVo application = new ExploreApplicationVo();
            application.setId(appSet.getId());
            application.setName(appSet.getName());
            application.setDescription(appSet.getDescription());
            application.setImage(appSet.getImage());
            Object useCount = userRecordService.getBaseMapper().selectObjs(new QueryWrapper<UserRecord>()
                    .eq("app_id",appSet.getId())
                    .eq("status",1)
                    .select("COUNT(DISTINCT photo_id)")).get(0);
            application.setUseCount(Long.parseLong(useCount.toString()));
            applications.add(application);
        }
        return applications;
    }

    @Override
    public R colourize(ExploreDto exploreDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(5,"生成老照片上色",exploreDto.getUserId(),exploreDto.getPhotoId(),startTime);

        //当照片不存在、不属于当前用户、没有原图或已经处理过时，拒绝再次上色
        Photo photo = photoService.getById(exploreDto.getPhotoId());
        if(photo==null || !exploreDto.getUserId().equals(photo.getUserId()) || photo.getOriginalPath()==null || photo.getNImg()!=null){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //当老照片上色功能已经关闭时，拒绝处理图片
        AppSet appSet = appSetService.getById(5);
        if(appSet.getStatus()==0){
            userRecordService.insertUserRecord(userRecord,2,"当前功能维护中，请稍后再试");
            return R.no("当前功能维护中，请稍后再试");
        }

        try {

            QueryWrapper<WebSet> qw = new QueryWrapper<>();
            qw.eq("id",1);
            qw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key","colourize_model");
            WebSet webSet = webSetService.getOne(qw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
            body.add("colourize_model",webSet.getColourizeModel());
            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),webSet.getDirectory()).toFile()));

            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(webSet.getPicApiUrl(),webSet.getPicApiType(),webSet.getPicApiKey(),"colourizeImg",body);
            photo.setName("老照片上色");
            photo.setNImg(PicUtil.savePermanentImage("colourize",imageBytes,webSet.getDirectory(),webSet.getPicDomain(),"png"));

            //探索成片先保持未解锁，点击保存时再按后台下载模式处理
            photo.setAppId(5);
            photo.setSize("无规格");
            photoService.updateById(photo);
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());


        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }

    @Override
    public R matting(ExploreDto exploreDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(6,"生成图片抠图",exploreDto.getUserId(),exploreDto.getPhotoId(),startTime);

        //当照片不存在、不属于当前用户、没有原图或已经处理过时，拒绝再次抠图
        Photo photo = photoService.getById(exploreDto.getPhotoId());
        if(photo==null || !exploreDto.getUserId().equals(photo.getUserId()) || photo.getOriginalPath()==null || photo.getNImg()!=null){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //当图片抠图功能已经关闭时，拒绝处理图片
        AppSet appSet = appSetService.getById(6);
        if(appSet.getStatus()==0){
            userRecordService.insertUserRecord(userRecord,2,"当前功能维护中，请稍后再试");
            return R.no("当前功能维护中，请稍后再试");
        }

        try {

            QueryWrapper<WebSet> qw = new QueryWrapper<>();
            qw.eq("id",1);
            qw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key","matting_model");
            WebSet webSet = webSetService.getOne(qw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
            body.add("human_matting_model",webSet.getMattingModel());

            //当用户填写了DPI时
            if(exploreDto.getDpi()!=null){
                body.add("dpi",exploreDto.getDpi());
            }
            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),webSet.getDirectory()).toFile()));
            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(webSet.getPicApiUrl(),webSet.getPicApiType(),webSet.getPicApiKey(),"human_matting",body);
            photo.setName("图片抠图");
            photo.setNImg(PicUtil.savePermanentImage("matting",imageBytes,webSet.getDirectory(),webSet.getPicDomain(),"png"));

            //探索成片先保持未解锁，点击保存时再按后台下载模式处理
            photo.setAppId(6);
            photo.setSize("无规格");
            photoService.updateById(photo);
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());


        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }

    @Override
    public R generateLayoutPhotos(ExploreDto exploreDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(4,"证件照排版",exploreDto.getUserId(),exploreDto.getPhotoId(),startTime);

        //当照片不存在、不属于当前用户、没有原图或已经处理过时，拒绝再次进行证件照排版
        Photo photo = photoService.getById(exploreDto.getPhotoId());
        if(photo==null || !exploreDto.getUserId().equals(photo.getUserId()) || photo.getOriginalPath()==null || photo.getNImg()!=null){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //当证件照排版功能已经关闭时，拒绝处理图片
        AppSet appSet = appSetService.getById(4);
        if(appSet.getStatus()==0){
            userRecordService.insertUserRecord(userRecord,2,"当前功能维护中，请稍后再试");
            return R.no("当前功能维护中，请稍后再试");
        }

        boolean hasLayoutSize = StringUtils.hasText(exploreDto.getLayoutSize());
        boolean hasLayoutHeight = exploreDto.getLayoutHeight()!=null;
        boolean hasLayoutWidth = exploreDto.getLayoutWidth()!=null;

        //画布预设和自定义画布尺寸同时存在时，拒绝处理图片
        if(hasLayoutSize && (hasLayoutHeight || hasLayoutWidth)){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //自定义画布高度和宽度没有同时填写时，拒绝处理图片
        if(hasLayoutHeight!=hasLayoutWidth){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //自定义画布尺寸小于1时，拒绝处理图片
        if(hasLayoutHeight && (exploreDto.getLayoutHeight()<1 || exploreDto.getLayoutWidth()<1)){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //画布预设不是图片接口支持的值时，拒绝处理图片
        if(hasLayoutSize
                && !"six_inch".equals(exploreDto.getLayoutSize())
                && !"five_inch".equals(exploreDto.getLayoutSize())
                && !"a4".equals(exploreDto.getLayoutSize())
                && !"three_r".equals(exploreDto.getLayoutSize())
                && !"four_r".equals(exploreDto.getLayoutSize())){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        try {

            QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
            storageQw.eq("id",1);
            storageQw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key");
            WebSet storageSet = webSetService.getOne(storageQw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();

            //当用户填写了高度时
            if(exploreDto.getHeight()!=null){
                body.add("height",exploreDto.getHeight());
            }
            //当用户填写了宽度时
            if(exploreDto.getWidth()!=null){
                body.add("width",exploreDto.getWidth());
            }

            //当用户选择了预设画布时
            if(hasLayoutSize){
                body.add("layout_size",exploreDto.getLayoutSize());
            }

            //当用户填写了自定义画布尺寸时
            if(hasLayoutHeight){
                body.add("layout_height",exploreDto.getLayoutHeight());
                body.add("layout_width",exploreDto.getLayoutWidth());
            }

            //当用户开启裁剪线时
            if(exploreDto.getCropLine()==1){
                body.add("crop_line",true);
            }

            //当用户填写了DPI时
            if(exploreDto.getDpi()!=null){
                body.add("dpi",exploreDto.getDpi());
            }

            //当用户填写了文件大小时
            if(exploreDto.getKb()!=null){
                body.add("kb",exploreDto.getKb());
            }

            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),storageSet.getDirectory()).toFile()));

            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(storageSet.getPicApiUrl(),storageSet.getPicApiType(),storageSet.getPicApiKey(),"generate_layout_photos",body);
            photo.setName("证件照排版");
            photo.setNImg(PicUtil.savePermanentImage("generateLayoutPhotos",imageBytes,storageSet.getDirectory(),storageSet.getPicDomain(),exploreDto.getKb()!=null ? "jpg" : "png"));

            //探索成片先保持未解锁，点击保存时再按后台下载模式处理
            photo.setAppId(4);

            //当用户使用自定义画布时，保存自定义画布尺寸
            if(hasLayoutHeight){
                photo.setSize(exploreDto.getLayoutWidth()+"x"+exploreDto.getLayoutHeight());
            //当用户使用预设画布时，保存预设画布名称
            }else if("five_inch".equals(exploreDto.getLayoutSize())){
                photo.setSize("五寸");
            }else if("a4".equals(exploreDto.getLayoutSize())){
                photo.setSize("A4");
            }else if("three_r".equals(exploreDto.getLayoutSize())){
                photo.setSize("3R");
            }else if("four_r".equals(exploreDto.getLayoutSize())){
                photo.setSize("4R");
            }else {
                photo.setSize("六寸");
            }
            photoService.updateById(photo);
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());


        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }

    @Override
    public R cartoon(ExploreDto exploreDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(8,"生成动漫风照",exploreDto.getUserId(),exploreDto.getPhotoId(),startTime);

        //当照片不存在、不属于当前用户、没有原图或已经处理过时，拒绝再次生成动漫风照
        Photo photo = photoService.getById(exploreDto.getPhotoId());
        if(photo==null || !exploreDto.getUserId().equals(photo.getUserId()) || photo.getOriginalPath()==null || photo.getNImg()!=null){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //当动漫风照功能已经关闭时，拒绝处理图片
        AppSet appSet = appSetService.getById(8);
        if(appSet.getStatus()==0){
            userRecordService.insertUserRecord(userRecord,2,"当前功能维护中，请稍后再试");
            return R.no("当前功能维护中，请稍后再试");
        }

        try {

            QueryWrapper<WebSet> qw = new QueryWrapper<>();
            qw.eq("id",1);
            qw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key","cartoon_model");
            WebSet webSet = webSetService.getOne(qw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
            body.add("cartoon_model",webSet.getCartoonModel());
            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),webSet.getDirectory()).toFile()));

            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(webSet.getPicApiUrl(),webSet.getPicApiType(),webSet.getPicApiKey(),"cartoon",body);
            photo.setName("动漫风照");
            photo.setNImg(PicUtil.savePermanentImage("cartoon",imageBytes,webSet.getDirectory(),webSet.getPicDomain(),"png"));

            //探索成片先保持未解锁，点击保存时再按后台下载模式处理
            photo.setAppId(8);
            photo.setSize("无规格");
            photoService.updateById(photo);
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());


        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }

    @Override
    public R convertImageFormat(ExploreDto exploreDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(7,"生成图片格式转换",exploreDto.getUserId(),exploreDto.getPhotoId(),startTime);

        //当照片不存在、不属于当前用户、没有原图、已经处理过或目标格式不支持时，拒绝转换
        Photo photo = photoService.getById(exploreDto.getPhotoId());
        if(photo==null || !exploreDto.getUserId().equals(photo.getUserId()) || photo.getOriginalPath()==null || photo.getNImg()!=null
                || !StringUtils.hasText(exploreDto.getTargetFormat())
                || !"jpg".equalsIgnoreCase(exploreDto.getTargetFormat()) && !"jpeg".equalsIgnoreCase(exploreDto.getTargetFormat())
                && !"png".equalsIgnoreCase(exploreDto.getTargetFormat()) && !"gif".equalsIgnoreCase(exploreDto.getTargetFormat())){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }
        String targetFormat = exploreDto.getTargetFormat().toLowerCase();

        //当图片格式转换功能已经关闭时，拒绝处理图片
        AppSet appSet = appSetService.getById(7);
        if(appSet.getStatus()==0){
            userRecordService.insertUserRecord(userRecord,2,"当前功能维护中，请稍后再试");
            return R.no("当前功能维护中，请稍后再试");
        }

        try {

            QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
            storageQw.eq("id",1);
            storageQw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key");
            WebSet storageSet = webSetService.getOne(storageQw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
            body.add("target_format",targetFormat);
            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),storageSet.getDirectory()).toFile()));

            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(storageSet.getPicApiUrl(),storageSet.getPicApiType(),storageSet.getPicApiKey(),"convert_image_format",body);
            photo.setName("图片格式转换");
            photo.setNImg(PicUtil.getPublicUrl("formatConversion/" + PicUtil.filesCopy("formatConversion",storageSet.getDirectory(),targetFormat,imageBytes),storageSet.getPicDomain()));

            //探索成片先保持未解锁，点击保存时再按后台下载模式处理
            photo.setAppId(7);
            photo.setSize(targetFormat.toUpperCase());
            photoService.updateById(photo);
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());


        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }

    @Override
    public R americanIdPhoto(ExploreDto exploreDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(9,"生成美式证件照",exploreDto.getUserId(),exploreDto.getPhotoId(),startTime);

        //当照片不存在、不属于当前用户、没有原图或已经处理过时，拒绝再次生成美式证件照
        Photo photo = photoService.getById(exploreDto.getPhotoId());
        if(photo==null || !exploreDto.getUserId().equals(photo.getUserId()) || photo.getOriginalPath()==null || photo.getNImg()!=null){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //当美式证件照功能已经关闭时，拒绝处理图片
        AppSet appSet = appSetService.getById(9);
        if(appSet.getStatus()==0){
            userRecordService.insertUserRecord(userRecord,2,"当前功能维护中，请稍后再试");
            return R.no("当前功能维护中，请稍后再试");
        }

        try {

            QueryWrapper<WebSet> qw = new QueryWrapper<>();
            qw.eq("id",1);
            qw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key","american_human_matting_model","american_face_detect_model");
            WebSet webSet = webSetService.getOne(qw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();

            //当用户没有填写高度时
            if(exploreDto.getHeight()==null){
                body.add("height",600);
            }else {
                body.add("height",exploreDto.getHeight());
            }
            //当用户没有填写宽度时
            if(exploreDto.getWidth()==null){
                body.add("width",600);
            }else {
                body.add("width",exploreDto.getWidth());
            }
            body.add("human_matting_model",webSet.getAmericanHumanMattingModel());
            body.add("face_detect_model",webSet.getAmericanFaceDetectModel());
            body.add("face_align",true);
            body.add("background","american");

            //当用户填写了DPI时
            if(exploreDto.getDpi()!=null){
                body.add("dpi",exploreDto.getDpi());
            }

            //当用户填写了文件大小时
            if(exploreDto.getKb()!=null){
                body.add("kb",exploreDto.getKb());
            }

            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),webSet.getDirectory()).toFile()));

            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(webSet.getPicApiUrl(),webSet.getPicApiType(),webSet.getPicApiKey(),"american_idphoto",body);
            photo.setName("美式证件照");
            photo.setNImg(PicUtil.savePermanentImage("americanIdPhoto",imageBytes,webSet.getDirectory(),webSet.getPicDomain(),exploreDto.getKb()!=null ? "jpg" : "png"));

            //探索成片先保持未解锁，点击保存时再按后台下载模式处理
            photo.setAppId(9);

            //当用户填写了宽度和高度时
            if(exploreDto.getWidth()!=null && exploreDto.getHeight()!=null){
                photo.setSize(exploreDto.getWidth()+"x"+exploreDto.getHeight());
            }else {
                photo.setSize("无规格");
            }


            photoService.updateById(photo);
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());


        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }


    @Override
    public R generateTemplatePhotos(ExploreDto exploreDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(10,"生成社交媒体模板照",exploreDto.getUserId(),exploreDto.getPhotoId(),startTime);

        //当照片不存在、不属于当前用户、没有原图或已经处理过时，拒绝再次生成模板照
        Photo photo = photoService.getById(exploreDto.getPhotoId());
        if(photo==null || !exploreDto.getUserId().equals(photo.getUserId()) || photo.getOriginalPath()==null || photo.getNImg()!=null){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //当社交媒体模板照功能已经关闭时，拒绝处理图片
        AppSet appSet = appSetService.getById(10);
        if(appSet.getStatus()==0){
            userRecordService.insertUserRecord(userRecord,2,"当前功能维护中，请稍后再试");
            return R.no("当前功能维护中，请稍后再试");
        }

        try {

            QueryWrapper<WebSet> qw = new QueryWrapper<>();
            qw.eq("id",1);
            qw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key","template_human_matting_model","template_face_detect_model");
            WebSet webSet = webSetService.getOne(qw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();

            //当用户没有填写高度时
            if(exploreDto.getHeight()==null){
                body.add("height",413);
            }else {
                body.add("height",exploreDto.getHeight());
            }
            //当用户没有填写宽度时
            if(exploreDto.getWidth()==null){
                body.add("width",295);
            }else {
                body.add("width",exploreDto.getWidth());
            }
            body.add("human_matting_model",webSet.getTemplateHumanMattingModel());
            body.add("face_detect_model",webSet.getTemplateFaceDetectModel());
            body.add("face_align",true);

            //当用户填写了背景颜色时
            if(StringUtils.hasText(exploreDto.getColor())){
                body.add("color",exploreDto.getColor());
            }else {
                body.add("color","#438edb");
            }
            body.add("template_name",exploreDto.getTemplateName());

            //当用户填写了DPI时
            if(exploreDto.getDpi()!=null){
                body.add("dpi",exploreDto.getDpi());
            }

            //当用户填写了文件大小时
            if(exploreDto.getKb()!=null){
                body.add("kb",exploreDto.getKb());
            }
            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),webSet.getDirectory()).toFile()));

            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(webSet.getPicApiUrl(),webSet.getPicApiType(),webSet.getPicApiKey(),"generate_template_photos",body);
            photo.setName("社交媒体模板照");
            photo.setNImg(PicUtil.savePermanentImage("templatePhotos",imageBytes,webSet.getDirectory(),webSet.getPicDomain(),exploreDto.getKb()!=null ? "jpg" : "png"));

            //探索成片先保持未解锁，点击保存时再按后台下载模式处理
            photo.setAppId(10);


            //当用户填写了宽度和高度时
            if(exploreDto.getWidth()!=null && exploreDto.getHeight()!=null){
                photo.setSize(exploreDto.getWidth()+"x"+exploreDto.getHeight());
            }else {
                photo.setSize("无规格");
            }


            photoService.updateById(photo);
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());


        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }

    @Override
    public R watermark(ExploreDto exploreDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(11,"给图片添加水印",exploreDto.getUserId(),exploreDto.getPhotoId(),startTime);

        //当照片不存在、不属于当前用户、没有原图、已经处理过或水印文字为空时，拒绝添加水印
        Photo photo = photoService.getById(exploreDto.getPhotoId());
        if(photo==null || !exploreDto.getUserId().equals(photo.getUserId()) || photo.getOriginalPath()==null || photo.getNImg()!=null || !StringUtils.hasText(exploreDto.getWatermarkText())){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //当水印参数超出页面允许的范围时，拒绝把异常参数发给图片接口
        if(exploreDto.getWatermarkText().length()>30
                || StringUtils.hasText(exploreDto.getWatermarkStyle()) && !"striped".equals(exploreDto.getWatermarkStyle()) && !"central".equals(exploreDto.getWatermarkStyle())
                || exploreDto.getWatermarkAngle()!=null && (exploreDto.getWatermarkAngle()<0 || exploreDto.getWatermarkAngle()>360)
                || exploreDto.getWatermarkOpacity()!=null && (exploreDto.getWatermarkOpacity()<1 || exploreDto.getWatermarkOpacity()>80)
                || exploreDto.getWatermarkSize()!=null && (exploreDto.getWatermarkSize()<10 || exploreDto.getWatermarkSize()>100)
                || (!StringUtils.hasText(exploreDto.getWatermarkStyle()) || "striped".equals(exploreDto.getWatermarkStyle()))
                && exploreDto.getWatermarkSpace()!=null && (exploreDto.getWatermarkSpace()<10 || exploreDto.getWatermarkSpace()>200)
                || StringUtils.hasText(exploreDto.getColor()) && !exploreDto.getColor().matches("^#[0-9a-fA-F]{6}$")){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //当图片加水印功能已经关闭时，拒绝处理图片
        AppSet appSet = appSetService.getById(11);
        if(appSet.getStatus()==0){
            userRecordService.insertUserRecord(userRecord,2,"当前功能维护中，请稍后再试");
            return R.no("当前功能维护中，请稍后再试");
        }

        try {

            QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
            storageQw.eq("id",1);
            storageQw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key");
            WebSet storageSet = webSetService.getOne(storageQw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
            body.add("text",exploreDto.getWatermarkText().trim());
            body.add("style",StringUtils.hasText(exploreDto.getWatermarkStyle()) ? exploreDto.getWatermarkStyle() : "striped");
            body.add("angle",exploreDto.getWatermarkAngle()==null ? 30 : exploreDto.getWatermarkAngle());
            body.add("color",StringUtils.hasText(exploreDto.getColor()) ? exploreDto.getColor() : "#171717");
            body.add("opacity",(exploreDto.getWatermarkOpacity()==null ? 30 : exploreDto.getWatermarkOpacity())/100D);
            body.add("size",exploreDto.getWatermarkSize()==null ? 40 : exploreDto.getWatermarkSize());
            body.add("space",exploreDto.getWatermarkSpace()==null ? 120 : exploreDto.getWatermarkSpace());
            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),storageSet.getDirectory()).toFile()));

            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(storageSet.getPicApiUrl(),storageSet.getPicApiType(),storageSet.getPicApiKey(),"watermark",body);
            photo.setName("图片加水印");
            photo.setNImg(PicUtil.savePermanentImage("watermark",imageBytes,storageSet.getDirectory(),storageSet.getPicDomain(),"png"));

            //探索成片先保持未解锁，点击保存时再按后台下载模式处理
            photo.setAppId(11);
            photo.setSize("无规格");
            photoService.updateById(photo);
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());


        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }

    @Override
    public R compressImage(ExploreDto exploreDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(12,"生成图片压缩",exploreDto.getUserId(),exploreDto.getPhotoId(),startTime);

        //当照片不存在、不属于当前用户、没有原图、已经处理过或目标KB无效时，拒绝压缩
        Photo photo = photoService.getById(exploreDto.getPhotoId());
        if(photo==null || !exploreDto.getUserId().equals(photo.getUserId()) || photo.getOriginalPath()==null || photo.getNImg()!=null
                || exploreDto.getKb()==null || exploreDto.getKb()<1
                || exploreDto.getDpi()!=null && exploreDto.getDpi()<72){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //当图片压缩功能已经关闭时，拒绝处理图片
        AppSet appSet = appSetService.getById(12);
        if(appSet.getStatus()==0){
            userRecordService.insertUserRecord(userRecord,2,"当前功能维护中，请稍后再试");
            return R.no("当前功能维护中，请稍后再试");
        }

        try {

            QueryWrapper<WebSet> storageQw = new QueryWrapper<>();
            storageQw.eq("id",1);
            storageQw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key");
            WebSet storageSet = webSetService.getOne(storageQw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
            body.add("kb",exploreDto.getKb());
            body.add("dpi",exploreDto.getDpi()==null ? 300 : exploreDto.getDpi());
            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),storageSet.getDirectory()).toFile()));

            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(storageSet.getPicApiUrl(),storageSet.getPicApiType(),storageSet.getPicApiKey(),"set_kb",body);
            photo.setName("图片压缩");
            photo.setNImg(PicUtil.savePermanentImage("compressImage",imageBytes,storageSet.getDirectory(),storageSet.getPicDomain(),"jpg"));

            //探索成片先保持未解锁，点击保存时再按后台下载模式处理
            photo.setAppId(12);
            photo.setSize("无规格");
            photo.setDpi(exploreDto.getDpi()==null ? 300 : exploreDto.getDpi());
            photoService.updateById(photo);
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());


        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }

    @Override
    public R deblurImage(ExploreDto exploreDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(13,"生成模糊图片变清晰",exploreDto.getUserId(),exploreDto.getPhotoId(),startTime);

        //当照片不存在、不属于当前用户、没有原图或已经处理过时，拒绝再次修复
        Photo photo = photoService.getById(exploreDto.getPhotoId());
        if(photo==null || !exploreDto.getUserId().equals(photo.getUserId()) || photo.getOriginalPath()==null || photo.getNImg()!=null){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //当模糊图片变清晰功能已经关闭时，拒绝处理图片
        AppSet appSet = appSetService.getById(13);
        if(appSet.getStatus()==0){
            userRecordService.insertUserRecord(userRecord,2,"当前功能维护中，请稍后再试");
            return R.no("当前功能维护中，请稍后再试");
        }

        try {

            QueryWrapper<WebSet> qw = new QueryWrapper<>();
            qw.eq("id",1);
            qw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key","deblur_model");
            WebSet webSet = webSetService.getOne(qw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
            body.add("deblur_model",webSet.getDeblurModel());
            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),webSet.getDirectory()).toFile()));

            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(webSet.getPicApiUrl(),webSet.getPicApiType(),webSet.getPicApiKey(),"deblur",body);
            photo.setName("模糊图片变清晰");
            photo.setNImg(PicUtil.savePermanentImage("deblurImage",imageBytes,webSet.getDirectory(),webSet.getPicDomain(),"png"));

            //探索成片先保持未解锁，点击保存时再按后台下载模式处理
            photo.setAppId(13);
            photo.setSize("无规格");
            photoService.updateById(photo);
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());


        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }

    @Override
    public R coupleRedPhoto(ExploreDto exploreDto) {

        //记录接口耗时
        long startTime = System.currentTimeMillis();
        UserRecord userRecord = userRecordService.createUserRecord(16,"生成情侣红底照",exploreDto.getUserId(),exploreDto.getPhotoId(),startTime);

        //当照片不存在、不属于当前用户、没有原图或已经处理过时，拒绝再次生成情侣红底照
        Photo photo = photoService.getById(exploreDto.getPhotoId());
        if(photo==null || !exploreDto.getUserId().equals(photo.getUserId()) || photo.getOriginalPath()==null || photo.getNImg()!=null){
            userRecordService.insertUserRecord(userRecord,2,"非法请求");
            return R.no("非法请求");
        }

        //当情侣红底照功能已经关闭时，拒绝处理图片
        AppSet appSet = appSetService.getById(16);
        if(appSet.getStatus()==0){
            userRecordService.insertUserRecord(userRecord,2,"当前功能维护中，请稍后再试");
            return R.no("当前功能维护中，请稍后再试");
        }

        try {

            QueryWrapper<WebSet> qw = new QueryWrapper<>();
            qw.eq("id",1);
            qw.select("pic_domain","directory","pic_api_type","pic_api_url","pic_api_key","couple_human_matting_model","couple_face_detect_model");
            WebSet webSet = webSetService.getOne(qw);

            MultiValueMap<String,Object> body = new LinkedMultiValueMap<>();
            body.add("color",exploreDto.getColor());
            body.add("human_matting_model",webSet.getCoupleHumanMattingModel());
            body.add("face_detect_model",webSet.getCoupleFaceDetectModel());
            body.add("input_image",new FileSystemResource(PicUtil.getFile(photo.getOriginalPath(),webSet.getDirectory()).toFile()));

            //发起请求
            byte[] imageBytes = HivisionIDPhotosApiUtil.requestImage(webSet.getPicApiUrl(),webSet.getPicApiType(),webSet.getPicApiKey(),"couple_red_photo",body);
            photo.setName("情侣红底照");
            photo.setNImg(PicUtil.savePermanentImage("coupleRedPhoto",imageBytes,webSet.getDirectory(),webSet.getPicDomain(),"png"));

            //探索成片先保持未解锁，点击保存时再按后台下载模式处理
            photo.setAppId(16);
            photo.setSize("无规格");
            photo.setBackgroundColor(exploreDto.getColor());
            photoService.updateById(photo);
            userRecordService.insertUserRecord(userRecord,1,null);
            return R.ok(photo.getId());


        } catch (Exception e) {
            userRecordService.insertUserRecord(userRecord,2,e.getMessage());
            return R.no(e.getMessage());
        }
    }

}
