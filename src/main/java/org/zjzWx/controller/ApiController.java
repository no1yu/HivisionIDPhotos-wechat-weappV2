package org.zjzWx.controller;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.zjzWx.entity.ClothesSet;
import org.zjzWx.model.dto.CreatePhotoDto;
import org.zjzWx.model.dto.ChangeClothesDto;
import org.zjzWx.model.dto.UpdatePhotoDto;
import org.zjzWx.service.ApiService;
import org.zjzWx.service.AppSetService;
import org.zjzWx.service.ClothesSetService;
import org.zjzWx.util.R;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {


    @Autowired
    private ApiService apiService;
    @Autowired
    private AppSetService appSetService;
    @Autowired
    private ClothesSetService clothesSetService;


    @PostMapping("/createIdPhoto")
    public R createIdPhoto(@RequestBody CreatePhotoDto createPhotoDto) {
        createPhotoDto.setUserId(StpUtil.getLoginIdAsInt());
        return apiService.createIdPhoto(createPhotoDto);
    }

    //photoId是上传图片接口返回的照片ID
    @PostMapping("/createIdHdPhoto")
    public R createIdHdPhoto(@RequestParam("photoId") Integer photoId) {
        return apiService.createIdHdPhoto(photoId,StpUtil.getLoginIdAsInt());
    }

    @PostMapping("/updateIdPhoto")
    public R updateIdPhoto(@RequestBody UpdatePhotoDto updatePhotoDto) {
        updatePhotoDto.setUserId(StpUtil.getLoginIdAsInt());
        return apiService.updateIdPhoto(updatePhotoDto);
    }

    @PostMapping("/changeClothes")
    public R changeClothes(@RequestBody ChangeClothesDto changeClothesDto) {
        changeClothesDto.setUserId(StpUtil.getLoginIdAsInt());
        return apiService.changeClothes(changeClothesDto);
    }

    //photoId是上传图片接口返回的照片ID
    @PostMapping("/updateUserPhonto")
    public R updateUserPhonto(@RequestParam("photoId") Integer photoId){
        return apiService.updateUserPhonto(StpUtil.getLoginIdAsInt(), photoId);
    }

    @PostMapping("/getBeautySwitch")
    public R getBeautySwitch(){
        return R.ok(appSetService.getBeautySwitch());
    }

    //读取状态正常的换装素材
    @PostMapping("/getClothesList")
    public R getClothesList(Integer category){
        List<ClothesSet> clothesList = clothesSetService.getClothesList(category);
        if(clothesList==null){
            return R.no();
        }
        return R.ok(clothesList);
    }





}
