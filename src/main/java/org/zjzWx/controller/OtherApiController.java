package org.zjzWx.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.zjzWx.entity.Photo;
import org.zjzWx.model.dto.ExploreDto;
import org.zjzWx.model.vo.PicVo;
import org.zjzWx.service.OtherApiService;
import org.zjzWx.service.PhotoService;
import org.zjzWx.util.R;

@RestController
@RequestMapping("/otherApi")
public class OtherApiController {

    @Autowired
    private OtherApiService otherApiService;
    @Autowired
    private PhotoService photoService;


    @GetMapping("/exploreIndex")
    public R exploreIndex(){
        return R.ok(otherApiService.exploreIndex());
    }

    @PostMapping("/colourize")
    public R colourize(@RequestBody ExploreDto exploreDto) {
        exploreDto.setUserId(StpUtil.getLoginIdAsInt());
        return otherApiService.colourize(exploreDto);
    }

    @PostMapping("/matting")
    public R matting(@RequestBody ExploreDto exploreDto) {
        exploreDto.setUserId(StpUtil.getLoginIdAsInt());
        return otherApiService.matting(exploreDto);
    }


    @PostMapping("/generateLayoutPhotos")
    public R generateLayoutPhotos(@RequestBody ExploreDto exploreDto) {
        exploreDto.setUserId(StpUtil.getLoginIdAsInt());
        return otherApiService.generateLayoutPhotos(exploreDto);
    }

    @PostMapping("/cartoon")
    public R cartoon(@RequestBody ExploreDto exploreDto) {
        exploreDto.setUserId(StpUtil.getLoginIdAsInt());
        return otherApiService.cartoon(exploreDto);
    }

    @PostMapping("/convertImageFormat")
    public R convertImageFormat(@RequestBody ExploreDto exploreDto) {
        exploreDto.setUserId(StpUtil.getLoginIdAsInt());
        return otherApiService.convertImageFormat(exploreDto);
    }

    @PostMapping("/americanIdPhoto")
    public R americanIdPhoto(@RequestBody ExploreDto exploreDto) {
        exploreDto.setUserId(StpUtil.getLoginIdAsInt());
        return otherApiService.americanIdPhoto(exploreDto);
    }

    @PostMapping("/generateTemplatePhotos")
    public R generateTemplatePhotos(@RequestBody ExploreDto exploreDto) {
        exploreDto.setUserId(StpUtil.getLoginIdAsInt());
        return otherApiService.generateTemplatePhotos(exploreDto);
    }

    @PostMapping("/watermark")
    public R watermark(@RequestBody ExploreDto exploreDto) {
        exploreDto.setUserId(StpUtil.getLoginIdAsInt());
        return otherApiService.watermark(exploreDto);
    }

    @PostMapping("/compressImage")
    public R compressImage(@RequestBody ExploreDto exploreDto) {
        exploreDto.setUserId(StpUtil.getLoginIdAsInt());
        return otherApiService.compressImage(exploreDto);
    }

    @PostMapping("/deblurImage")
    public R deblurImage(@RequestBody ExploreDto exploreDto) {
        exploreDto.setUserId(StpUtil.getLoginIdAsInt());
        return otherApiService.deblurImage(exploreDto);
    }

    @PostMapping("/coupleRedPhoto")
    public R coupleRedPhoto(@RequestBody ExploreDto exploreDto) {
        exploreDto.setUserId(StpUtil.getLoginIdAsInt());
        return otherApiService.coupleRedPhoto(exploreDto);
    }

    //photoId是上传图片接口返回的照片ID，用来读取探索成片
    @PostMapping("/getExplorePhoto")
    public R getExplorePhoto(@RequestParam("photoId") Integer photoId) {
        Photo photo = photoService.getById(photoId);

        if(photo==null || photo.getUserId()!=StpUtil.getLoginIdAsInt() || photo.getNImg()==null){
            return R.no("非法请求");
        }
        PicVo picVo = new PicVo();
        picVo.setPhotoId(photo.getId());
        picVo.setPicUrl(photo.getNImg());
        return R.ok(picVo);
    }



}
